package com.example.gitofy.view.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gitofy.R;

public class PlatformSelectorActivity extends AppCompatActivity {

    ImageButton gitlabButton, githubButton;
    private static final String CLIENT_ID = "Ov23lish7hRvDchbveEI"; // Replace with your real client_id
    private static final String REDIRECT_URI = "myapp://callback";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialize();
        setChosenPlatform();
    }


    public void initialize(){
        gitlabButton = findViewById(R.id.gitlabLogoImageButton);
        githubButton = findViewById(R.id.githubLogoImageButton);
    }

    public void setChosenPlatform(){
        gitlabButton.setOnClickListener(v -> {

        });

        githubButton.setOnClickListener(v -> {
            String authUrl = "https://github.com/login/oauth/authorize" +
                    "?client_id=" + CLIENT_ID +
                    "&redirect_uri=" + REDIRECT_URI +
                    "&scope=repo";

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            startActivity(intent);
        });
    }



}