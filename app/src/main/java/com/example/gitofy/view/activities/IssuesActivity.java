package com.example.gitofy.view.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.IssuesAdapter;
import com.example.gitofy.view.util.GitHubService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.json.JSONArray;

public class IssuesActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private IssuesAdapter adapter;
    private ProgressBar progressBar;
    private GitHubService gitHubService;
    private String accessToken;
    private String repoOwner;
    private String repoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issues);

        android.util.Log.d("IssuesActivity", "Activity created");

        // Get repo info from intent
        repoOwner = getIntent().getStringExtra("repo_owner");
        repoName = getIntent().getStringExtra("repo_name");

        android.util.Log.d("IssuesActivity", "Repo: " + repoOwner + "/" + repoName);

        setTitle(repoName + " - Issues");

        recyclerView = findViewById(R.id.recycler_view_issues);
        progressBar = findViewById(R.id.progress_bar);
        FloatingActionButton fabAddIssue = findViewById(R.id.fab_add_issue);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        gitHubService = new GitHubService();

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        accessToken = prefs.getString("token", null);

        fabAddIssue.setOnClickListener(v -> {
            Intent intent = new Intent(IssuesActivity.this, CreateIssueActivity.class);
            intent.putExtra("repo_owner", repoOwner);
            intent.putExtra("repo_name", repoName);
            startActivityForResult(intent, 1);
        });

        loadIssues();
    }

    private void loadIssues() {
        progressBar.setVisibility(View.VISIBLE);

        gitHubService.fetchIssues(accessToken, repoOwner, repoName, new GitHubService.IssuesCallback() {
            @Override
            public void onSuccess(JSONArray issues) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    adapter = new IssuesAdapter(issues, repoOwner, repoName);
                    recyclerView.setAdapter(adapter);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(IssuesActivity.this, "Failed to load issues: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            loadIssues();
        }
    }
}