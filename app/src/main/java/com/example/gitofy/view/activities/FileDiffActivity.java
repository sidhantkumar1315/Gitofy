package com.example.gitofy.view.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gitofy.R;

public class FileDiffActivity extends AppCompatActivity {

    private TextView diffTextView;
    private TextView statsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_diff);

        String fileName = getIntent().getStringExtra("file_name");
        String patch = getIntent().getStringExtra("patch");
        int additions = getIntent().getIntExtra("additions", 0);
        int deletions = getIntent().getIntExtra("deletions", 0);

        setupToolbar(fileName);
        initializeViews();

        statsTextView.setText("+" + additions + " -" + deletions);
        displayDiff(patch);
    }

    private void setupToolbar(String fileName) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(fileName);
        }
    }

    private void initializeViews() {
        diffTextView = findViewById(R.id.diffTextView);
        statsTextView = findViewById(R.id.statsTextView);
    }

    private void displayDiff(String patch) {
        if (patch == null || patch.isEmpty()) {
            diffTextView.setText("No changes to display");
            return;
        }

        SpannableStringBuilder builder = new SpannableStringBuilder();
        String[] lines = patch.split("\n");

        for (String line : lines) {
            SpannableString spannableLine = new SpannableString(line + "\n");

            if (line.startsWith("+") && !line.startsWith("+++")) {
                // Addition - green background
                spannableLine.setSpan(new BackgroundColorSpan(Color.parseColor("#E6FFED")),
                        0, line.length(), 0);
                spannableLine.setSpan(new ForegroundColorSpan(Color.parseColor("#24292E")),
                        0, line.length(), 0);
            } else if (line.startsWith("-") && !line.startsWith("---")) {
                // Deletion - red background
                spannableLine.setSpan(new BackgroundColorSpan(Color.parseColor("#FFEEF0")),
                        0, line.length(), 0);
                spannableLine.setSpan(new ForegroundColorSpan(Color.parseColor("#24292E")),
                        0, line.length(), 0);
            } else if (line.startsWith("@@")) {
                // Line numbers - blue
                spannableLine.setSpan(new ForegroundColorSpan(Color.parseColor("#0366D6")),
                        0, line.length(), 0);
            }

            builder.append(spannableLine);
        }

        diffTextView.setText(builder);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}