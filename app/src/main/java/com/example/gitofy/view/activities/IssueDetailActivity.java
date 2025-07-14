package com.example.gitofy.view.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.gitofy.R;
import com.example.gitofy.view.util.GitHubService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IssueDetailActivity extends AppCompatActivity {
    private TextView textTitle;
    private TextView textNumber;
    private TextView textState;
    private TextView textDescription;
    private TextView textAuthor;
    private TextView textCreatedAt;
    private TextView textAssignees;
    private TextView textLabels;
    private TextView textMilestone;
    private ChipGroup chipGroupAssignees;
    private ChipGroup chipGroupLabels;
    private Button buttonCloseIssue;
    private Button buttonReopenIssue;
    private ProgressBar progressBar;
    private ScrollView contentLayout;

    private GitHubService gitHubService;
    private String accessToken;
    private String repoOwner;
    private String repoName;
    private int issueNumber;
    private String currentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_detail);

        // Get data from intent
        repoOwner = getIntent().getStringExtra("repo_owner");
        repoName = getIntent().getStringExtra("repo_name");
        issueNumber = getIntent().getIntExtra("issue_number", 0);

        initializeViews();

        gitHubService = new GitHubService();

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        accessToken = prefs.getString("token", null);

        loadIssueDetails();
    }

    private void initializeViews() {
        textTitle = findViewById(R.id.text_issue_title);
        textNumber = findViewById(R.id.text_issue_number);
        textState = findViewById(R.id.text_issue_state);
        textDescription = findViewById(R.id.text_issue_description);
        textAuthor = findViewById(R.id.text_author);
        textCreatedAt = findViewById(R.id.text_created_at);
        textAssignees = findViewById(R.id.text_assignees);
        textLabels = findViewById(R.id.text_labels);
        textMilestone = findViewById(R.id.text_milestone);
        chipGroupAssignees = findViewById(R.id.chip_group_assignees);
        chipGroupLabels = findViewById(R.id.chip_group_labels);
        buttonCloseIssue = findViewById(R.id.button_close_issue);
        buttonReopenIssue = findViewById(R.id.button_reopen_issue);
        progressBar = findViewById(R.id.progress_bar);
        contentLayout = findViewById(R.id.content_layout);

        ImageButton buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());

        buttonCloseIssue.setOnClickListener(v -> updateIssueState("closed"));
        buttonReopenIssue.setOnClickListener(v -> updateIssueState("open"));
    }

    private void loadIssueDetails() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        gitHubService.fetchIssueDetail(accessToken, repoOwner, repoName, issueNumber,
                new GitHubService.IssueDetailCallback() {
                    @Override
                    public void onSuccess(JSONObject issue) {
                        runOnUiThread(() -> {
                            try {
                                displayIssueDetails(issue);
                                progressBar.setVisibility(View.GONE);
                                contentLayout.setVisibility(View.VISIBLE);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(IssueDetailActivity.this, "Error parsing issue details", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(IssueDetailActivity.this, "Failed to load issue details", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                });
    }

    private void displayIssueDetails(JSONObject issue) throws JSONException {
        // Title and number
        textTitle.setText(issue.getString("title"));
        textNumber.setText("#" + issue.getInt("number"));

        // State
        currentState = issue.getString("state");
        updateStateDisplay();

        // Description
        String body = issue.optString("body", "No description provided.");
        textDescription.setText(body);

        // Author and created date
        JSONObject user = issue.getJSONObject("user");
        textAuthor.setText("opened by " + user.getString("login"));

        String createdAt = issue.getString("created_at");
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());

        try {
            Date date = inputFormat.parse(createdAt);
            textCreatedAt.setText(outputFormat.format(date));
        } catch (ParseException e) {
            textCreatedAt.setText(createdAt);
        }

        // Assignees
        JSONArray assignees = issue.optJSONArray("assignees");
        if (assignees != null && assignees.length() > 0) {
            textAssignees.setText("Assignees");
            chipGroupAssignees.setVisibility(View.VISIBLE);
            chipGroupAssignees.removeAllViews();

            for (int i = 0; i < assignees.length(); i++) {
                JSONObject assignee = assignees.getJSONObject(i);
                Chip chip = new Chip(this);
                chip.setText(assignee.getString("login"));
                chipGroupAssignees.addView(chip);
            }
        } else {
            textAssignees.setText("No assignees");
            chipGroupAssignees.setVisibility(View.GONE);
        }

        // Labels
        JSONArray labels = issue.optJSONArray("labels");
        if (labels != null && labels.length() > 0) {
            textLabels.setText("Labels");
            chipGroupLabels.setVisibility(View.VISIBLE);
            chipGroupLabels.removeAllViews();

            for (int i = 0; i < labels.length(); i++) {
                JSONObject label = labels.getJSONObject(i);
                Chip chip = new Chip(this);
                chip.setText(label.getString("name"));

                // Set label color
                String color = "#" + label.getString("color");
                chip.setChipBackgroundColorResource(android.R.color.transparent);
                chip.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor(color)));

                // Determine text color based on background
                int bgColor = android.graphics.Color.parseColor(color);
                int textColor = (android.graphics.Color.red(bgColor) * 0.299 +
                        android.graphics.Color.green(bgColor) * 0.587 +
                        android.graphics.Color.blue(bgColor) * 0.114) > 186 ?
                        android.graphics.Color.BLACK : android.graphics.Color.WHITE;
                chip.setTextColor(textColor);

                chipGroupLabels.addView(chip);
            }
        } else {
            textLabels.setText("No labels");
            chipGroupLabels.setVisibility(View.GONE);
        }

        // Milestone
        JSONObject milestone = issue.optJSONObject("milestone");
        if (milestone != null) {
            textMilestone.setText("Milestone: " + milestone.getString("title"));
        } else {
            textMilestone.setText("No milestone");
        }
    }

    private void updateStateDisplay() {
        if ("open".equals(currentState)) {
            textState.setText("OPEN");
            textState.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.branch_color_2))); // Green
            buttonCloseIssue.setVisibility(View.VISIBLE);
            buttonReopenIssue.setVisibility(View.GONE);
        } else {
            textState.setText("CLOSED");
            textState.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.branch_color_3))); // Red
            buttonCloseIssue.setVisibility(View.GONE);
            buttonReopenIssue.setVisibility(View.VISIBLE);
        }
    }

    private void updateIssueState(String newState) {
        new AlertDialog.Builder(this)
                .setTitle(newState.equals("closed") ? "Close Issue" : "Reopen Issue")
                .setMessage("Are you sure you want to " + (newState.equals("closed") ? "close" : "reopen") + " this issue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    gitHubService.updateIssueState(accessToken, repoOwner, repoName, issueNumber, newState,
                            new GitHubService.UpdateIssueCallback() {
                                @Override
                                public void onSuccess(JSONObject updatedIssue) {
                                    runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        currentState = newState;
                                        updateStateDisplay();
                                        Toast.makeText(IssueDetailActivity.this,
                                                "Issue " + (newState.equals("closed") ? "closed" : "reopened") + " successfully",
                                                Toast.LENGTH_SHORT).show();
                                        setResult(RESULT_OK); // Notify parent to refresh
                                    });
                                }

                                @Override
                                public void onError(Exception e) {
                                    runOnUiThread(() -> {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(IssueDetailActivity.this,
                                                "Failed to update issue: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}