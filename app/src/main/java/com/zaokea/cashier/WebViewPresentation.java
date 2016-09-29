package com.zaokea.cashier;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class WebViewPresentation extends Presentation {
    private WebView webView;

    WebViewPresentation(Context outerContext, Display display) {
        super(outerContext, display);
        if (getWindow() != null) {
            getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        }
        show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(getContext());
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("http://192.168.1.7:3000/client");
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setAppCachePath(getContext().getCacheDir().getAbsolutePath());
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
    }

    void setClientUrl(final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }
}
