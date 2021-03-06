package com.zaokea.cashier;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.gprinter.command.EscCommand;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class PrinterCommand {
    private EscCommand esc = new EscCommand();

    PrinterCommand(String json) throws JSONException {
        esc.addInitializePrinter();
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i += 1) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            switch (jsonObject.getString("type")) {
                case "text":
                    handleText(jsonObject);
                    break;
                case "barcode":
                    handleBarCode(jsonObject);
                    break;
                case "qrcode":
                    handleQRCode(jsonObject);
                    break;
                case "image":
                    handleImage(jsonObject);
                    break;
            }
        }
    }

    private void handleAlign(JSONObject jsonObject) throws JSONException {
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
        } else {
            esc.addSelectJustification(EscCommand.JUSTIFICATION.LEFT);
        }
    }

    private void handleText(JSONObject jsonObject) throws JSONException {
        handleAlign(jsonObject);
        EscCommand.ENABLE emphasized = EscCommand.ENABLE.OFF;
        if (jsonObject.has("emphasized") && jsonObject.getBoolean("emphasized")) {
            emphasized = EscCommand.ENABLE.ON;
        }
        EscCommand.ENABLE doubleSize = EscCommand.ENABLE.OFF;
        if (jsonObject.has("double") && jsonObject.getBoolean("double")) {
            doubleSize = EscCommand.ENABLE.ON;
        }
        EscCommand.ENABLE underline = EscCommand.ENABLE.OFF;
        if (jsonObject.has("underline") && jsonObject.getBoolean("underline")) {
            underline = EscCommand.ENABLE.ON;
        }
        esc.addSelectPrintModes(EscCommand.FONT.FONTA, emphasized, doubleSize, doubleSize, underline);
        esc.addText(jsonObject.getString("text") + "\n");
    }

    private void handleBarCode(JSONObject jsonObject) throws JSONException {
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
    }

    private void handleQRCode(JSONObject jsonObject) throws JSONException {
        int imageSize = Printer.PAGE_SIZE;
        if (jsonObject.has("size")) {
            imageSize = jsonObject.getInt("size");
        }
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        esc.addRastBitImage(Printer.toQRCode(jsonObject.getString("data"), imageSize), imageSize, 0);
    }

    private void handleImage(JSONObject jsonObject) throws JSONException {
        byte[] bytes = Base64.decode(jsonObject.getString("data"), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        esc.addSelectJustification(EscCommand.JUSTIFICATION.CENTER);
        esc.addRastBitImage(bitmap, bitmap.getWidth(), 0);
    }

    EscCommand getCommand() {
        return esc;
    }
}
