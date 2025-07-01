package com.example.gitofy.view;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gitofy.R;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);

        webView = findViewById(R.id.oauthWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView = findViewById(R.id.oauthWebView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE); // Prevent cache miss

        webView.clearCache(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();

                // Check if GitHub is redirecting to your app
                if (url.toString().startsWith("myapp://callback")) {
                    String code = url.getQueryParameter("code");

                    if (code != null) {
                        Toast.makeText(getApplicationContext(), "Code: " + code, Toast.LENGTH_LONG).show();
                        Log.d("OAuth", "Code received: " + code);

                        // TODO: Exchange code for access_token here or pass it to another activity
                    } else {
                        Toast.makeText(getApplicationContext(), "Authorization failed", Toast.LENGTH_SHORT).show();
                    }

                    return true; // Prevent WebView from loading this redirect
                }

                // Otherwise continue loading
                return false;
            }
        });

        String authUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=Ov23lish7hRvDchbveEI" +
                "&redirect_uri=myapp://callback" +
                "&scope=repo";

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.clearCache(true);
        webView.loadUrl(authUrl);

    }
}
