package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.FileChangeAdapter;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommitDetailActivity extends AppCompatActivity {
    private static final String TAG = "CommitDetailActivity";

    private TextView commitMessageText, authorText, dateText, commitShaText;
    private TextView statsText, filesChangedText;
    private RecyclerView filesRecyclerView;
    private ProgressBar progressBar;
    private ScrollView contentLayout;
    private FileChangeAdapter adapter;
    private List<JSONObject> filesList = new ArrayList<>();

    private String repoName;
    private String commitSha;
    private String repoOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commit_detail);

        // Get data from intent with null checks
        repoName = getIntent().getStringExtra("repo_name");
        commitSha = getIntent().getStringExtra("commit_sha");
        repoOwner = getIntent().getStringExtra("repo_owner");
        String commitMessage = getIntent().getStringExtra("commit_message");
        String authorName = getIntent().getStringExtra("author_name");
        String commitDate = getIntent().getStringExtra("commit_date");

        // Log received data
        Log.d(TAG, "Received data:");
        Log.d(TAG, "repoName: " + repoName);
        Log.d(TAG, "repoOwner: " + repoOwner);
        Log.d(TAG, "commitSha: " + commitSha);
        Log.d(TAG, "commitMessage: " + commitMessage);
        Log.d(TAG, "authorName: " + authorName);

        // Validate required data
        if (repoName == null || commitSha == null || repoOwner == null || repoOwner.isEmpty()) {
            Log.e(TAG, "Missing required data - repoName: " + repoName + ", owner: " + repoOwner + ", sha: " + commitSha);
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            setupToolbar();
            initializeViews();

            // Set initial data with null safety
            commitMessageText.setText(commitMessage != null ? commitMessage : "No message");
            authorText.setText("by " + (authorName != null ? authorName : "Unknown"));
            dateText.setText(formatDate(commitDate));

            // Safely set SHA text
            if (commitSha != null && commitSha.length() >= 7) {
                commitShaText.setText(commitSha.substring(0, 7));
            } else {
                commitShaText.setText(commitSha != null ? commitSha : "");
            }

            // Setup RecyclerView
            filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new FileChangeAdapter(filesList);
            filesRecyclerView.setAdapter(adapter);

            loadCommitDetails();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading commit details", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(repoName != null ? repoName : "Commit Details");
        }
    }

    private void initializeViews() {
        commitMessageText = findViewById(R.id.commitMessage);
        authorText = findViewById(R.id.authorText);
        dateText = findViewById(R.id.dateText);
        commitShaText = findViewById(R.id.commitSha);
        statsText = findViewById(R.id.statsText);
        filesChangedText = findViewById(R.id.filesChangedText);
        filesRecyclerView = findViewById(R.id.filesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);  // This is the ScrollView

        // Initialize with default values
        statsText.setText("+0 / -0");
        filesChangedText.setText("0 files changed");
    }

    private void loadCommitDetails() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        Log.d(TAG, "Loading commit details for: " + repoOwner + "/" + repoName + "/" + commitSha);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchCommitDetails(token, repoOwner, repoName, commitSha,
                new GitHubService.CommitDetailCallback() {
                    @Override
                    public void onSuccess(JSONObject commitDetail) {
                        runOnUiThread(() -> {
                            try {
                                displayCommitDetails(commitDetail);
                                progressBar.setVisibility(View.GONE);
                                contentLayout.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                Log.e(TAG, "Error displaying commit details", e);
                                Toast.makeText(CommitDetailActivity.this,
                                        "Error displaying commit details", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.e(TAG, "Error loading commit details", e);
                        runOnUiThread(() -> {
                            Toast.makeText(CommitDetailActivity.this,
                                    "Failed to load commit details: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            progressBar.setVisibility(View.GONE);
                            finish();
                        });
                    }
                });
    }

    private void displayCommitDetails(JSONObject commitDetail) {
        try {
            // Get stats
            JSONObject stats = commitDetail.optJSONObject("stats");
            if (stats != null) {
                int additions = stats.optInt("additions", 0);
                int deletions = stats.optInt("deletions", 0);
                statsText.setText("+" + additions + " / -" + deletions);
            }

            // Get files
            JSONArray files = commitDetail.optJSONArray("files");
            if (files != null) {
                filesChangedText.setText(files.length() + " files changed");

                filesList.clear();
                for (int i = 0; i < files.length(); i++) {
                    filesList.add(files.getJSONObject(i));
                }
                adapter.notifyDataSetChanged();
            } else {
                filesChangedText.setText("0 files changed");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing commit details", e);
            e.printStackTrace();
        }
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "Unknown date";
        }

        try {
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            parser.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = parser.parse(dateStr);

            SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            return formatter.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date: " + dateStr, e);
            return dateStr;
        }
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