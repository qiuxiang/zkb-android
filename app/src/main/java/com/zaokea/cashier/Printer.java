package com.zaokea.cashier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.io.PortParameters;

import java.util.Date;
import java.util.Vector;

public class Printer {
    private GpService service;
    private Context context;
    static final int pageSize = 380;
    static final int printerId = 0;
    static final int CONNECTED = 3;

    class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Printer.this.service = GpService.Stub.asInterface(service);
            connect();
        }
    }

    Printer(Context context) {
        PrinterServiceConnection serviceConnection = new PrinterServiceConnection();
        Intent intent = new Intent("com.gprinter.service.GpPrintService");
        intent.setPackage(context.getPackageName());
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE); // bindService
        this.context = context;
    }

    public boolean connect() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getProductId() == 1536) {
                int result = GpCom.ERROR_CODE.FAILED.ordinal();
                try {
                    result = service.openPort(printerId, PortParameters.USB, device.getDeviceName(), 0);
                } catch (RemoteException e) {
                    Log.e(getClass().getSimpleName(), "open port fail");
                    e.printStackTrace();
                }
                if (result == GpCom.ERROR_CODE.SUCCESS.ordinal()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean connected() {
        boolean status = false;
        try {
            status = service.getPrinterConnectStatus(printerId) == CONNECTED;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return status;
    }

    public void printTestPage() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addText(new Date() + "\n");
//        esc.addRastBitImage(toQRCode("http://bing.com", pageSize), pageSize, 0);
        try {
            service.sendEscCommand(printerId, toBase64(esc.getCommand()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private String toBase64(Vector<Byte> vector) {
        byte[] bytes = new byte[vector.size()];
        for (int i = 0; i < bytes.length; i += 1) {
            bytes[i] = vector.get(i);
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap toQRCode(String content, int size) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
            for (int x = 0; x < size; x += 1) {
                for (int y = 0; y < size; y += 1) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Log.e(getClass().getSimpleName(), "QRCodeWriter encode error");
            return null;
        }
    }
}
