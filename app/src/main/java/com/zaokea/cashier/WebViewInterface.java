package com.zaokea.cashier;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.webkit.JavascriptInterface;

class WebViewInterface {
    private ConnectivityManager connectivityManager;
    private WebViewPresentation presentation;
    private Printer printer;

    WebViewInterface(Context context) {
        printer = new Printer(context);
        connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        presentation = new WebViewPresentation(context,
                ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE)).getDisplays()[1]);
    }

    void close() {
        printer.close();
        presentation.cancel();
    }

    @JavascriptInterface
    public void setClientUrl(String url) {
        Log.i(getClass().getSimpleName(), "loading url: " + url);
        presentation.setClientUrl(url);
    }

    @JavascriptInterface
    public boolean printTestPage() {
        Log.i(getClass().getSimpleName(), "printing test page");
        return printer.printTestPage();
    }

    @JavascriptInterface
    public boolean printerConnected() {
        return printer.connected();
    }

    @JavascriptInterface
    public boolean print(String data) {
        return printer.print(data);
    }

    @JavascriptInterface
    public boolean networkConnected() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
