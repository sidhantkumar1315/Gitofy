package com.example.gitofy.view.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.gitofy.R;

public class RepoDEtailsActivity extends AppCompatActivity {

    TextView nameTextView, ownerTextView;
    ImageView avatarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_details);

        nameTextView = findViewById(R.id.repoNameDetail);
        ownerTextView = findViewById(R.id.repoOwnerDetail);
        avatarImageView = findViewById(R.id.repoAvatarDetail);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String owner = getIntent().getStringExtra("owner");
        String avatarUrl = getIntent().getStringExtra("avatar");

        nameTextView.setText(name);
        ownerTextView.setText("Owner: " + owner);
        Glide.with(this).load(avatarUrl).circleCrop().into(avatarImageView);
    }
}
