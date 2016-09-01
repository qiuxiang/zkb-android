package com.zaokea.cashier;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

public class WebViewInterface {
    private WebViewPresentation presentation;
    private Printer printer;

    WebViewInterface(Context context) {
        printer = new Printer(context);

        Display[] displays = ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE)).getDisplays();
        presentation = new WebViewPresentation(context, displays[1]);
        presentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        presentation.show();
    }

    @JavascriptInterface
    public void loadUrl(String url) {
        Log.i(getClass().getSimpleName(), "loading url: " + url);
        presentation.loadUrl(url);
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
}
