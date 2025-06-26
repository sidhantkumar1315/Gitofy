package com.example.gitofy.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gitofy.R;

public class LoginActivity extends AppCompatActivity {

    TextView platformLogin, platformInstruction;
    EditText patInput;
    ImageView platformLogo;
    Button loginButton;
    String chosenPlatform;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        chosenPlatform = getIntent().getStringExtra("chosenPlatform");
        initialize();
        platformLoginUI();
    }

    public void initialize(){
        platformLogin = findViewById(R.id.loginTextView);
        platformInstruction = findViewById(R.id.instructionTextView);
        patInput = findViewById(R.id.PATEditText);
        platformLogo = findViewById(R.id.platformImageView);
        loginButton = findViewById(R.id.loginButton);
    }

    public void platformLoginUI(){
        platformInstruction.setText("Please enter your personal access token!");
        if("gitlab".equals(chosenPlatform)){
            platformLogin.setText("GITLAB LOGIN");
            platformLogo.setImageResource(R.drawable.gitlab_logo);
        }
        else if("github".equals(chosenPlatform)){
            platformLogin.setText("GITHUB LOGIN");
            platformLogo.setImageResource(R.drawable.github_logo);
        }
    }
}