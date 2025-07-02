package com.example.gitofy.view.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.gitofy.view.util.GitHubService;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthCallbackActivity extends AppCompatActivity {
    private static final String CLIENT_ID     = "Ov23lish7hRvDchbveEI";
    private static final String CLIENT_SECRET = "5dd1edb8e8e4d723e488971d760ed0214a4943a6"; // ⚠️ keep out of production APK!
    private static final String REDIRECT_URI  = "myapp://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show a blank screen
        setContentView(new View(this));

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith(REDIRECT_URI)) {
            String code = uri.getQueryParameter("code");
            if (code != null) {
                Log.d("OAuthCallback", "Code: " + code);
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                exchangeCodeForToken(code);
            } else {
                Toast.makeText(this, "Authorization failed", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void exchangeCodeForToken(String code) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            RequestBody body = new FormBody.Builder()
                    .add("client_id", CLIENT_ID)
                    .add("client_secret", CLIENT_SECRET)
                    .add("code", code)
                    .add("redirect_uri", REDIRECT_URI)
                    .build();

            Request request = new Request.Builder()
                    .url("https://github.com/login/oauth/access_token")
                    .header("Accept", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String json = response.body().string();
                JSONObject obj = new JSONObject(json);
                String token = obj.getString("access_token");
                SharedPreferences prefs = this.getSharedPreferences("auth", MODE_PRIVATE);
                prefs.edit().putString("token", token).apply();
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                finish();
            }
        }).start();
    }


    }



