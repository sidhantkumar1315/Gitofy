package com.example.gitofy.view;



import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OAuthCallbackActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri != null && uri.toString().startsWith("myapp://callback")) {
            String code = uri.getQueryParameter("code");

            if (code != null) {
                Log.d("OAuthCallback", "Authorization code: " + code);
                Toast.makeText(this, "Code received: " + code, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Authorization failed or cancelled", Toast.LENGTH_SHORT).show();
            }
        }

        // Optionally close this activity and go back
        finish();
    }
}
