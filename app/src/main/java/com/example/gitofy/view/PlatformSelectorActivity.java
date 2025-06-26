package com.example.gitofy.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gitofy.R;

public class PlatformSelectorActivity extends AppCompatActivity {

    ImageButton gitlabButton, githubButton;

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
    }


    public void initialize(){
        gitlabButton = findViewById(R.id.gitlabLogoImageButton);
        githubButton = findViewById(R.id.githubLogoImageButton);
    }

    public void setChosenPlatform(){
        gitlabButton.setOnClickListener(v -> {
            moveToSelectedPlatformLogin("gitlab");
        });

        githubButton.setOnClickListener(v -> {
            moveToSelectedPlatformLogin("github");
        });
    }
    public void moveToSelectedPlatformLogin(String platform){
        Intent intent = new Intent(PlatformSelectorActivity.this, LoginActivity.class);
        intent.putExtra("chosenPlatform", platform);
        startActivity(intent);
    }

}