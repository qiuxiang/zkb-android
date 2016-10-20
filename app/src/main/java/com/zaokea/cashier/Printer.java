package com.zaokea.cashier;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Base64;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.io.PortParameters;
import com.gprinter.service.GpPrintService;

import org.json.JSONException;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

class Printer {
    private PrinterServiceConnection serviceConnection;
    private Receiver receiver;
    private GpService service;
    private Context context;
    private static final int PRINTER_ID = 0;
    private static final int CONNECTED = 3;
    static final int PAGE_SIZE = 384;

    private class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Printer.this.service = GpService.Stub.asInterface(service);
            connect();
        }
    }

    class Receiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(getClass().getSimpleName(), "usb connected");
            connect();
        }
    }

    Printer(Context context) {
        this.context = context;
        serviceConnection = new PrinterServiceConnection();
        receiver = new Receiver();
        context.bindService(new Intent(context, GpPrintService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        context.registerReceiver(receiver, new IntentFilter("android.hardware.usb.action.USB_DEVICE_ATTACHED"));
    }

    void close() {
        context.unregisterReceiver(receiver);
        context.unbindService(serviceConnection);
    }

    private boolean connect() {
        if (connected()) {
            return true;
        }
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getProductId() == 1536) {
                int result = GpCom.ERROR_CODE.FAILED.ordinal();
                try {
                    result = service.openPort(PRINTER_ID, PortParameters.USB, device.getDeviceName(), 0);
                } catch (RemoteException e) {
                    Log.e(getClass().getSimpleName(), "open port fail");
                }
                if (result == GpCom.ERROR_CODE.SUCCESS.ordinal()) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean connected() {
        boolean status = false;
        try {
            status = service.getPrinterConnectStatus(PRINTER_ID) == CONNECTED;
        } catch (RemoteException e) {
            Log.e(getClass().getSimpleName(), "remote exception");
        }
        return status;
    }

    boolean print(String json) {
        try {
            return print(new PrinterCommand(json).getCommand());
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "json parse error");
            return false;
        }
    }

    private boolean print(EscCommand esc) {
        boolean status = false;
        try {
            status = service.sendEscCommand(
                    PRINTER_ID, toBase64(esc.getCommand())) == GpCom.ERROR_CODE.SUCCESS.ordinal();
        } catch (RemoteException e) {
            Log.e(getClass().getSimpleName(), "remote exception");
        }
        return status;
    }

    boolean printTestPage() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addText(new Date() + "\n");
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        esc.addRastBitImage(toQRCode("http://zaokea.com", PAGE_SIZE / 2), PAGE_SIZE / 2, 0);
        return print(esc);
    }

    private static String toBase64(Vector<Byte> vector) {
        byte[] bytes = new byte[vector.size()];
        for (int i = 0; i < bytes.length; i += 1) {
            bytes[i] = vector.get(i);
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    static Bitmap toQRCode(String content, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix;
        try {
            matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (WriterException e) {
            Log.e(Printer.class.getSimpleName(), "QRCodeWriter encode error");
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x += 1) {
            for (int y = 0; y < size; y += 1) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
}
