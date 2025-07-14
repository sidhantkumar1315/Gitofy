package com.example.gitofy.view.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gitofy.R;

public class FileDiffActivity extends AppCompatActivity {

    private TextView diffTextView;
    private TextView statsTextView;
    private TextView truncatedMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_diff);

        // Get intent extras
        String fileName = getIntent().getStringExtra("file_name");
        String patch = getIntent().getStringExtra("patch");
        int additions = getIntent().getIntExtra("additions", 0);
        int deletions = getIntent().getIntExtra("deletions", 0);
        int changes = getIntent().getIntExtra("changes", 0);
        boolean patchTruncated = getIntent().getBooleanExtra("patch_truncated", false);
        String status = getIntent().getStringExtra("status");

        setupToolbar(fileName);
        initializeViews();

        // Display stats
        if (changes > 0 && changes != (additions + deletions)) {
            statsTextView.setText("+" + additions + " -" + deletions + " (" + changes + " changes)");
        } else {
            statsTextView.setText("+" + additions + " -" + deletions);
        }

        // Check if patch is truncated or missing
        if (patchTruncated || (patch == null || patch.isEmpty()) && changes > 0) {
            truncatedMessageView.setVisibility(View.VISIBLE);

            // Customize message based on status
            String message;
            if ("renamed".equals(status)) {
                message = "File was renamed. Full diff not available.";
            } else if (patch == null || patch.isEmpty()) {
                message = "Full diff not available. File might be binary or too large to display.";
            } else {
                message = "Diff might be truncated due to size limitations.";
            }
            truncatedMessageView.setText(message);

            // Still try to display whatever patch we have
            if (patch != null && !patch.isEmpty()) {
                displayDiff(patch);
            } else {
                diffTextView.setText("No preview available");
            }
        } else {
            truncatedMessageView.setVisibility(View.GONE);
            displayDiff(patch);
        }
    }

    private void setupToolbar(String fileName) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(fileName != null ? fileName : "File Diff");
        }
    }

    private void initializeViews() {
        diffTextView = findViewById(R.id.diffTextView);
        statsTextView = findViewById(R.id.statsTextView);
        truncatedMessageView = findViewById(R.id.truncatedMessage);
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
            } else if (line.startsWith("diff --git")) {
                // Diff header - bold
                spannableLine.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
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