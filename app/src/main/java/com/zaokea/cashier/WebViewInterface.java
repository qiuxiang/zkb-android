package com.zaokea.cashier;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;

public class WebViewInterface {
    private final WebViewPresentation presentation;

    WebViewInterface(Context context) {
        Display[] displays = ((DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE)).getDisplays();
        presentation = new WebViewPresentation(context, displays[1]);
        presentation.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        presentation.show();
    }

    @JavascriptInterface
    public void loadUrl(String url) {
        presentation.loadUrl(url);
    }
}
