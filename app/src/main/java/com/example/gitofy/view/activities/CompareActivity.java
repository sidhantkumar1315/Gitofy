// Create CompareActivity.java
package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
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

import java.util.ArrayList;
import java.util.List;

public class CompareActivity extends AppCompatActivity {
    private static final String TAG = "CompareActivity";

    private TextView baseShaText, headShaText;
    private TextView statsText, filesChangedText;
    private TextView aheadBehindText;
    private RecyclerView filesRecyclerView;
    private ProgressBar progressBar;
    private View contentLayout;

    private FileChangeAdapter adapter;
    private List<JSONObject> filesList = new ArrayList<>();

    private String repoOwner, repoName, baseSha, headSha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare);

        // Get data from intent
        repoOwner = getIntent().getStringExtra("repo_owner");
        repoName = getIntent().getStringExtra("repo_name");
        baseSha = getIntent().getStringExtra("base_sha");
        headSha = getIntent().getStringExtra("head_sha");

        if (repoOwner == null || repoName == null || baseSha == null || headSha == null) {
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        loadComparison();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Compare Commits");
        }
    }

    private void initializeViews() {
        baseShaText = findViewById(R.id.baseSha);
        headShaText = findViewById(R.id.headSha);
        statsText = findViewById(R.id.statsText);
        filesChangedText = findViewById(R.id.filesChangedText);
        aheadBehindText = findViewById(R.id.aheadBehindText);
        filesRecyclerView = findViewById(R.id.filesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);

        // Set SHA texts
        baseShaText.setText(baseSha.substring(0, Math.min(7, baseSha.length())));
        headShaText.setText(headSha.substring(0, Math.min(7, headSha.length())));

        // Setup RecyclerView
        filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileChangeAdapter(filesList);
        filesRecyclerView.setAdapter(adapter);
    }

    private void loadComparison() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.compareCommits(token, repoOwner, repoName, baseSha, headSha,
                new GitHubService.CompareCallback() {
                    @Override
                    public void onSuccess(JSONObject comparison) {
                        runOnUiThread(() -> {
                            try {
                                displayComparison(comparison);
                                progressBar.setVisibility(View.GONE);
                                contentLayout.setVisibility(View.VISIBLE);
                            } catch (Exception e) {
                                Log.e(TAG, "Error displaying comparison", e);
                                Toast.makeText(CompareActivity.this,
                                        "Error displaying comparison", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Error loading comparison", e);
                            Toast.makeText(CompareActivity.this,
                                    "Failed to load comparison", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            finish();
                        });
                    }
                });
    }

    private void displayComparison(JSONObject comparison) {
        try {
            // Display stats
            int aheadBy = comparison.optInt("ahead_by", 0);
            int behindBy = comparison.optInt("behind_by", 0);
            aheadBehindText.setText("↑ " + aheadBy + " ahead, ↓ " + behindBy + " behind");

            // Get total changes
            int totalAdditions = 0;
            int totalDeletions = 0;

            JSONArray files = comparison.optJSONArray("files");
            if (files != null) {
                filesChangedText.setText(files.length() + " files changed");

                filesList.clear();
                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);
                    filesList.add(file);

                    totalAdditions += file.optInt("additions", 0);
                    totalDeletions += file.optInt("deletions", 0);
                }

                statsText.setText("+" + totalAdditions + " / -" + totalDeletions);
                adapter.notifyDataSetChanged();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing comparison", e);
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