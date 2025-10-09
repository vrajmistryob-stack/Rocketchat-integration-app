package com.example.chatdemo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Build;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class ChatActivity extends AppCompatActivity {

    private WebView webView;
    private MaterialButton btnBack;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);

        // Clear HTML5 Web Storage (Removes the Meteor.loginToken from the previous user)
        WebStorage.getInstance().deleteAllData();

        // Get URL from intent
        String chatUrl = getIntent().getStringExtra("CHAT_URL");

        if (chatUrl != null && !chatUrl.isEmpty()) {
            setupWebView(chatUrl);
        } else {
            finish(); // Close activity if no URL provided
        }

        btnBack.setOnClickListener(v -> finish());

        // Handle back button with OnBackPressedDispatcher (modern approach)
        setupBackPressHandler();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(String url) {
        // --- Recommended WebView Settings ---
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);


        // Ensure no local caching conflict occurs
//        webView.getSettings().setAppCacheEnabled(false);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        // This setting is essential for Rocket.Chat to work
        webView.getSettings().setDatabaseEnabled(true);

        // Clear history to keep it clean, but cache/cookies are more important
        webView.clearHistory();

        // --- Clients ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // If you want to open ALL links in the webview (default behavior)
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // Load the URL
        webView.loadUrl(url);
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    // --- SESSION CLEANUP LOGIC ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {

            // 1. Clear General Cache (HTML assets, images)
            webView.clearCache(true);

            // 2. Clear HTML5 Web Storage (CRUCIAL: This removes the Meteor.loginToken)
            WebStorage.getInstance().deleteAllData();

            // 3. Clear Cookies (Session cookies)
            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // For API 21+
                cookieManager.removeAllCookies(null);
                cookieManager.flush(); // Ensure write to disk
            } else {
                // For older APIs (if supporting legacy devices)
                // cookieManager.removeAllCookie();
            }
        }
    }
}