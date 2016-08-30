package com.zaokea.cashier;

import android.annotation.SuppressLint;
import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewPresentation extends Presentation {
    private WebView webView;

    public WebViewPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(getContext());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadData("", "text/html", "utf-8");
        setContentView(webView);
    }

    public void loadUrl(final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }
}
