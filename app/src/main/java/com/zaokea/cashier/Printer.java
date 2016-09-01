package com.zaokea.cashier;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

public class Printer {
    private GpService service;
    private Context context;
    static final int pageSize = 384;
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
            Log.e(getClass().getSimpleName(), "remote exception");
        }
        return status;
    }

    public boolean print(String json) {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i += 1) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                switch (jsonObject.getString("type")) {
                    case "text":
                        if (jsonObject.has("align")) {
                            switch (jsonObject.getString("align")) {
                                case "center":
                                    esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                                    break;
                                case "right":
                                    esc.addSelectJustification(EscCommand.JUSTIFICATION.RIGHT);
                                    break;
                                default:
                                    esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
                            }
                        }
                        esc.addText(jsonObject.getString("text") + "\n");
                        break;
                    case "barcode":
                        String barcode = jsonObject.getString("data");
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                        if (jsonObject.has("height")) {
                            esc.addSetBarcodeHeight((byte) jsonObject.getInt("height"));
                        }
                        if (barcode.length() > 8) {
                            esc.addEAN13(barcode);
                        } else {
                            esc.addEAN8(barcode);
                        }
                        break;
                    case "qrcode":
                        int imageSize = pageSize;
                        if (jsonObject.has("size")) {
                            imageSize = jsonObject.getInt("size");
                        }
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                        esc.addRastBitImage(toQRCode(jsonObject.getString("data"), imageSize), imageSize, 0);
                        break;
                    case "image":
                        byte[] bytes = Base64.decode(jsonObject.getString("data"), Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
                        esc.addRastBitImage(bitmap, bitmap.getWidth(), 0);
                }
            }
            return print(esc);
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "json parse error");
            return false;
        }
    }

    public boolean print(EscCommand esc) {
        boolean status = false;
        try {
            status = service.sendEscCommand(
                    printerId, toBase64(esc.getCommand())) == GpCom.ERROR_CODE.SUCCESS.ordinal();
        } catch (RemoteException e) {
            Log.e(getClass().getSimpleName(), "remote exception");
        }
        return status;
    }

    public boolean printTestPage() {
        EscCommand esc = new EscCommand();
        esc.addInitializePrinter();
        esc.addText(new Date() + "\n");
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        esc.addRastBitImage(toQRCode("http://bing.com", pageSize / 2), pageSize / 2, 0);
        return print(esc);
    }

    private String toBase64(Vector<Byte> vector) {
        byte[] bytes = new byte[vector.size()];
        for (int i = 0; i < bytes.length; i += 1) {
            bytes[i] = vector.get(i);
        }
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Bitmap toQRCode(String content, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 0);
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix;
        try {
            matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (WriterException e) {
            Log.e(getClass().getSimpleName(), "QRCodeWriter encode error");
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
