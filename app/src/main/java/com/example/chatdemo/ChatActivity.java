package com.example.chatdemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest; // Import needed for the new method
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

    // Local storage key name for the auth token
    private static final String EC_TOKEN_KEY = "ec_token";

    // Placeholder for the token value to be injected
    private String ecTokenValue = "";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        webView = findViewById(R.id.webView);
        btnBack = findViewById(R.id.btnBack);

        Intent intent = getIntent();

        // --- 1. Get URL and EC_TOKEN from Intent ---
        String chatUrl = intent.getStringExtra(ChatUtil.EXTRA_CHAT_URL);
        // New: Retrieve the token passed separately via the helper method
        String incomingToken = intent.getStringExtra(ChatUtil.EXTRA_EC_TOKEN);

        if (incomingToken != null && !incomingToken.isEmpty()) {
            ecTokenValue = incomingToken;
        }

        Log.i(TAG, "Attempting to load chat URL: " + chatUrl);
        Log.i(TAG, "Token (ec_token) for injection: " + (ecTokenValue.isEmpty() ? "[Empty/None]" : "[Present]"));


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
        // This is necessary because we are now relying on a fresh, unauthenticated session
        // where we manually inject the token after page load.

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

        // Ensure no local caching conflict occurs by forcing a fresh network load
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Clear history (optional, for UI cleanliness)
        webView.clearHistory();

        // --- Clients (Updated to inject token on page finished) ---
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Check if we have a token to inject
                if (!ecTokenValue.isEmpty()) {
                    // JavaScript command to set the item in local storage
                    // Use the dynamic ecTokenValue retrieved from the Intent
                    String jsCommand = "localStorage.setItem('" + EC_TOKEN_KEY + "', '" + ecTokenValue + "');";

                    // Execute the script
                    // We can also use evaluateJavascript here which is preferred for modern APIs
                    view.evaluateJavascript(jsCommand, null);
                    Log.d(TAG, "Injected token into local storage: " + EC_TOKEN_KEY);
                } else {
                    Log.d(TAG, "No token to inject.");
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep all navigation within the WebView (default behavior)
                view.loadUrl(url);
                return true;
            }

            // New method for API 24+
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // For API 24+, use the request object to get the URL
                view.loadUrl(request.getUrl().toString());
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // 3. Load the URL
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put("ngrok-skip-browser-warning", "true");

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

    // --- OPTIONAL/REDUNDANT SESSION CLEANUP LOGIC on Activity Exit ---
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