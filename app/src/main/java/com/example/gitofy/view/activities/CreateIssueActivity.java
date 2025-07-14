package com.example.gitofy.view.activities;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.gitofy.R;
import com.example.gitofy.view.util.GitHubService;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class CreateIssueActivity extends AppCompatActivity {
    private EditText editTextTitle;
    private EditText editTextDescription;
    private Button buttonCreate;
    private TextView textAssignees;
    private TextView textLabels;
    private TextView textMilestone;
    private ChipGroup chipGroupAssignees;
    private ChipGroup chipGroupLabels;

    private String accessToken;
    private String repoOwner;
    private String repoName;
    private GitHubService gitHubService;

    private List<String> selectedAssignees = new ArrayList<>();
    private List<String> selectedLabels = new ArrayList<>();
    private Integer selectedMilestoneNumber = null;
    private String selectedMilestoneTitle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_issue);

        repoOwner = getIntent().getStringExtra("repo_owner");
        repoName = getIntent().getStringExtra("repo_name");

        initializeViews();

        gitHubService = new GitHubService();

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        accessToken = prefs.getString("token", null);

        setupClickListeners();
    }

    private void initializeViews() {
        editTextTitle = findViewById(R.id.edit_text_title);
        editTextDescription = findViewById(R.id.edit_text_description);
        buttonCreate = findViewById(R.id.button_create);
        textAssignees = findViewById(R.id.text_assignees);
        textLabels = findViewById(R.id.text_labels);
        textMilestone = findViewById(R.id.text_milestone);
        chipGroupAssignees = findViewById(R.id.chip_group_assignees);
        chipGroupLabels = findViewById(R.id.chip_group_labels);

        ImageButton buttonBack = findViewById(R.id.button_back);
        buttonBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        findViewById(R.id.layout_assignees).setOnClickListener(v -> showAssigneesDialog());
        findViewById(R.id.layout_labels).setOnClickListener(v -> showLabelsDialog());
        findViewById(R.id.layout_milestone).setOnClickListener(v -> showMilestonesDialog());

        buttonCreate.setOnClickListener(v -> createIssue());
    }

    private void showAssigneesDialog() {
        gitHubService.fetchAssignees(accessToken, repoOwner, repoName, new GitHubService.AssigneesCallback() {
            @Override
            public void onSuccess(JSONArray assignees) {
                runOnUiThread(() -> {
                    try {
                        String[] assigneeNames = new String[assignees.length()];
                        boolean[] checkedItems = new boolean[assignees.length()];

                        for (int i = 0; i < assignees.length(); i++) {
                            JSONObject assignee = assignees.getJSONObject(i);
                            assigneeNames[i] = assignee.getString("login");
                            checkedItems[i] = selectedAssignees.contains(assigneeNames[i]);
                        }

                        new AlertDialog.Builder(CreateIssueActivity.this)
                                .setTitle("Select Assignees")
                                .setMultiChoiceItems(assigneeNames, checkedItems, (dialog, which, isChecked) -> {
                                    if (isChecked) {
                                        selectedAssignees.add(assigneeNames[which]);
                                    } else {
                                        selectedAssignees.remove(assigneeNames[which]);
                                    }
                                })
                                .setPositiveButton("OK", (dialog, which) -> updateAssigneesDisplay())
                                .setNegativeButton("Cancel", null)
                                .show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(CreateIssueActivity.this, "Error parsing assignees", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CreateIssueActivity.this, "Failed to load assignees", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showLabelsDialog() {
        gitHubService.fetchLabels(accessToken, repoOwner, repoName, new GitHubService.LabelsCallback() {
            @Override
            public void onSuccess(JSONArray labels) {
                runOnUiThread(() -> {
                    try {
                        String[] labelNames = new String[labels.length()];
                        boolean[] checkedItems = new boolean[labels.length()];

                        for (int i = 0; i < labels.length(); i++) {
                            JSONObject label = labels.getJSONObject(i);
                            labelNames[i] = label.getString("name");
                            checkedItems[i] = selectedLabels.contains(labelNames[i]);
                        }

                        new AlertDialog.Builder(CreateIssueActivity.this)
                                .setTitle("Select Labels")
                                .setMultiChoiceItems(labelNames, checkedItems, (dialog, which, isChecked) -> {
                                    if (isChecked) {
                                        selectedLabels.add(labelNames[which]);
                                    } else {
                                        selectedLabels.remove(labelNames[which]);
                                    }
                                })
                                .setPositiveButton("OK", (dialog, which) -> updateLabelsDisplay())
                                .setNegativeButton("Cancel", null)
                                .show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(CreateIssueActivity.this, "Error parsing labels", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CreateIssueActivity.this, "Failed to load labels", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showMilestonesDialog() {
        gitHubService.fetchMilestones(accessToken, repoOwner, repoName, new GitHubService.MilestonesCallback() {
            @Override
            public void onSuccess(JSONArray milestones) {
                runOnUiThread(() -> {
                    try {
                        if (milestones.length() == 0) {
                            Toast.makeText(CreateIssueActivity.this, "No milestones available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String[] milestoneNames = new String[milestones.length() + 1];
                        final Integer[] milestoneNumbers = new Integer[milestones.length() + 1];

                        milestoneNames[0] = "No milestone";
                        milestoneNumbers[0] = null;

                        for (int i = 0; i < milestones.length(); i++) {
                            JSONObject milestone = milestones.getJSONObject(i);
                            milestoneNames[i + 1] = milestone.getString("title");
                            milestoneNumbers[i + 1] = milestone.getInt("number");
                        }

                        new AlertDialog.Builder(CreateIssueActivity.this)
                                .setTitle("Select Milestone")
                                .setItems(milestoneNames, (dialog, which) -> {
                                    selectedMilestoneNumber = milestoneNumbers[which];
                                    selectedMilestoneTitle = milestoneNames[which];
                                    updateMilestoneDisplay();
                                })
                                .show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(CreateIssueActivity.this, "Error parsing milestones", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> Toast.makeText(CreateIssueActivity.this, "Failed to load milestones", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateAssigneesDisplay() {
        chipGroupAssignees.removeAllViews();
        if (selectedAssignees.isEmpty()) {
            textAssignees.setText("No one - Assign yourself");
            chipGroupAssignees.setVisibility(View.GONE);
        } else {
            textAssignees.setText("Assignees:");
            chipGroupAssignees.setVisibility(View.VISIBLE);
            for (String assignee : selectedAssignees) {
                Chip chip = new Chip(this);
                chip.setText(assignee);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    selectedAssignees.remove(assignee);
                    updateAssigneesDisplay();
                });
                chipGroupAssignees.addView(chip);
            }
        }
    }

    private void updateLabelsDisplay() {
        chipGroupLabels.removeAllViews();
        if (selectedLabels.isEmpty()) {
            textLabels.setText("No labels");
            chipGroupLabels.setVisibility(View.GONE);
        } else {
            textLabels.setText("Labels:");
            chipGroupLabels.setVisibility(View.VISIBLE);
            for (String label : selectedLabels) {
                Chip chip = new Chip(this);
                chip.setText(label);
                chip.setCloseIconVisible(true);
                chip.setOnCloseIconClickListener(v -> {
                    selectedLabels.remove(label);
                    updateLabelsDisplay();
                });
                chipGroupLabels.addView(chip);
            }
        }
    }

    private void updateMilestoneDisplay() {
        if (selectedMilestoneTitle == null || "No milestone".equals(selectedMilestoneTitle)) {
            textMilestone.setText("No milestone");
        } else {
            textMilestone.setText(selectedMilestoneTitle);
        }
    }

    private void createIssue() {
        String title = editTextTitle.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show();
            return;
        }

        buttonCreate.setEnabled(false);


        gitHubService.createIssue(accessToken, repoOwner, repoName, title, description,
                selectedAssignees, selectedLabels, selectedMilestoneNumber,
                new GitHubService.CreateIssueCallback() {
                    @Override
                    public void onSuccess(JSONObject issue) {
                        runOnUiThread(() -> {
                            Toast.makeText(CreateIssueActivity.this, "Issue created successfully", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            buttonCreate.setEnabled(true);
                            Toast.makeText(CreateIssueActivity.this, "Failed to create issue: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
}