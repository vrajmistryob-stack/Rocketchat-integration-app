package com.example.chatdemo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
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

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private WebView webView;
    private MaterialButton btnBack;
    private static final String TAG = "ChatActivity";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);

        // NOTE: The WebStorage.getInstance().deleteAllData() call in your original onCreate
        // has been removed here, as the comprehensive cleanup is now centralized
        // in setupWebView() right before loading the URL.

        // Get URL from intent
        String chatUrl = getIntent().getStringExtra("CHAT_URL");
        Log.i(TAG, "Attempting to load chat URL: " + chatUrl);

        if (chatUrl != null && !chatUrl.isEmpty()) {
            setupWebView(chatUrl);
        } else {
            Log.e(TAG, "No chat URL provided. Finishing activity.");
            finish(); // Close activity if no URL provided
        }

        btnBack.setOnClickListener(v -> finish());

        // Handle back button with OnBackPressedDispatcher (modern approach)
        setupBackPressHandler();
    }

    private void setupWebView(String url) {

        // 1. 🛑 CRITICAL FIX: CONSOLIDATE ALL SESSION CLEANUP BEFORE URL LOAD 🛑

        // Clear HTML5 Web Storage (Removes the Meteor.loginToken from the previous user)
        WebStorage.getInstance().deleteAllData();
        Log.d(TAG, "WebStorage data cleared.");

        // Clear Cookies (Session IDs)
        CookieManager cookieManager = CookieManager.getInstance();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
            Log.d(TAG, "All cookies removed and flushed.");
        }

        // Clear General Cache (HTML assets, images)
        webView.clearCache(true);
        Log.d(TAG, "WebView cache cleared.");

        // 2. --- WEBVIEW CONFIGURATION ---
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Add this line to set a custom User-Agent to bypass the ngrok warning
//        settings.setUserAgentString("MyCustomRocketChatApp-Bypass-Client/1.0");

        // Ensure no local caching conflict occurs by forcing a fresh network load
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Clear history (optional, for UI cleanliness)
        webView.clearHistory();

        // --- Clients ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep all navigation within the WebView (default behavior)
                view.loadUrl(url);
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // 3. Load the URL
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("ngrok-skip-browser-warning", "true");

        // Use the two-parameter loadUrl method!
//        webView.loadUrl(url, extraHeaders);
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

    // --- OPTIONAL/REDUNDANT SESSION CLEANUP LOGIC on Activity Exit ---
    // This is safe to keep, ensuring that if the user closes the app mid-session,
    // the data is cleaned up for the *next* launch.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            WebStorage.getInstance().deleteAllData();
            webView.clearCache(true);

            CookieManager cookieManager = CookieManager.getInstance();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cookieManager.removeAllCookies(null);
                cookieManager.flush();
            }
        }
    }
}