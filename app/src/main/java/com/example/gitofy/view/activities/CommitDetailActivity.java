package com.example.gitofy.view.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.FileChangeAdapter;
import com.example.gitofy.view.util.GitHubService;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CommitDetailActivity extends AppCompatActivity {
    private static final String TAG = "CommitDetailActivity";

    // Views
    private TextView commitMessageText, authorText, dateText, commitShaText;
    private TextView statsText, filesChangedText;
    private RecyclerView filesRecyclerView;
    private ProgressBar progressBar;
    private ScrollView contentLayout;
    private EditText searchEditText;
    private Spinner filterSpinner;
    private View searchContainer;

    // Data
    private FileChangeAdapter adapter;
    private List<JSONObject> filesList = new ArrayList<>();
    private List<JSONObject> filteredFilesList = new ArrayList<>();
    private JSONObject commitDetail;
    private String repoName;
    private String commitSha;
    private String repoOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_commit_detail);

        // Get data from intent
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

        // Validate required data
        if (repoName == null || commitSha == null || repoOwner == null || repoOwner.isEmpty()) {
            Log.e(TAG, "Missing required data");
            Toast.makeText(this, "Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        try {
            setupToolbar();
            initializeViews();
            setupSearchAndFilter();

            // Set initial data
            commitMessageText.setText(commitMessage != null ? commitMessage : "No message");
            authorText.setText("by " + (authorName != null ? authorName : "Unknown"));
            dateText.setText(formatDate(commitDate));

            // Set SHA text with click to copy
            if (commitSha != null && commitSha.length() >= 7) {
                commitShaText.setText(commitSha.substring(0, 7));
            } else {
                commitShaText.setText(commitSha != null ? commitSha : "");
            }

            commitShaText.setOnClickListener(v -> copyCommitSha());

            // Setup RecyclerView
            setupRecyclerView();

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
        contentLayout = findViewById(R.id.contentLayout);

        // Search views - these will be null if not in layout
        searchContainer = findViewById(R.id.searchContainer);
        searchEditText = findViewById(R.id.searchFiles);
        filterSpinner = findViewById(R.id.filterSpinner);

        // Initialize with default values
        statsText.setText("+0 / -0");
        filesChangedText.setText("0 files changed");
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        filesRecyclerView.setLayoutManager(layoutManager);

        // Add divider
        DividerItemDecoration divider = new DividerItemDecoration(this, layoutManager.getOrientation());
        filesRecyclerView.addItemDecoration(divider);

        // Optimizations
        filesRecyclerView.setHasFixedSize(true);
        filesRecyclerView.setItemViewCacheSize(20);
        filesRecyclerView.setDrawingCacheEnabled(true);
        filesRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        adapter = new FileChangeAdapter(filteredFilesList);
        filesRecyclerView.setAdapter(adapter);
    }

    private void setupSearchAndFilter() {
        if (searchEditText == null || filterSpinner == null) {
            return; // Views not available in current layout
        }

        // Setup filter spinner
        String[] filterOptions = {"All Files", "Modified", "Added", "Removed", "Renamed"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        // Search listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterFiles();
            }
        });

        // Filter listener
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterFiles();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filterFiles() {
        if (searchEditText == null || filterSpinner == null) {
            return;
        }

        String searchQuery = searchEditText.getText().toString().toLowerCase();
        String selectedFilter = filterSpinner.getSelectedItem().toString();

        filteredFilesList.clear();

        for (JSONObject file : filesList) {
            try {
                String filename = file.optString("filename", "").toLowerCase();
                String status = file.optString("status", "");

                // Apply search filter
                if (!searchQuery.isEmpty() && !filename.contains(searchQuery)) {
                    continue;
                }

                // Apply status filter
                if (!selectedFilter.equals("All Files")) {
                    if (!status.equalsIgnoreCase(selectedFilter)) {
                        continue;
                    }
                }

                filteredFilesList.add(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        adapter.notifyDataSetChanged();

        // Update count
        if (searchQuery.isEmpty() && selectedFilter.equals("All Files")) {
            filesChangedText.setText(filesList.size() + " files changed");
        } else {
            filesChangedText.setText(filteredFilesList.size() + " of " + filesList.size() + " files");
        }
    }

    private void loadCommitDetails() {
        progressBar.setVisibility(View.VISIBLE);
        contentLayout.setVisibility(View.GONE);

        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchCommitDetails(token, repoOwner, repoName, commitSha,
                new GitHubService.CommitDetailCallback() {
                    @Override
                    public void onSuccess(JSONObject commitDetail) {
                        runOnUiThread(() -> {
                            try {
                                CommitDetailActivity.this.commitDetail = commitDetail;
                                displayCommitDetails(commitDetail);
                                progressBar.setVisibility(View.GONE);
                                contentLayout.setVisibility(View.VISIBLE);

                                // Show search container if many files and it exists
                                if (searchContainer != null && filesList.size() > 10) {
                                    searchContainer.setVisibility(View.VISIBLE);
                                }
                            } catch (Exception e) {
                                Toast.makeText(CommitDetailActivity.this,
                                        "Error displaying commit details", Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
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
                int total = stats.optInt("total", 0);
                statsText.setText("+" + additions + " / -" + deletions);

                // Show warning if there are many changes
                if (total > 100) {
                    Toast.makeText(this, "Large commit with " + total + " changes",
                            Toast.LENGTH_SHORT).show();
                }
            }

            // Get files
            JSONArray files = commitDetail.optJSONArray("files");
            if (files != null) {
                filesChangedText.setText(files.length() + " files changed");

                filesList.clear();

                // Show warning if there are many files
                if (files.length() > 50) {
                    Toast.makeText(this, "Showing " + files.length() + " files. This may take a moment.",
                            Toast.LENGTH_LONG).show();
                }

                for (int i = 0; i < files.length(); i++) {
                    JSONObject file = files.getJSONObject(i);

                    // Check if patch is available
                    String patch = file.optString("patch", "");
                    if (patch.isEmpty() && file.has("additions") && file.has("deletions")) {
                        // File might be binary or too large
                        file.put("patch", "[File content not available - might be binary or too large]");
                    }

                    filesList.add(file);
                }

                // Initially show all files
                filteredFilesList.clear();
                filteredFilesList.addAll(filesList);
                adapter.notifyDataSetChanged();

                Log.d(TAG, "Displaying " + filesList.size() + " files");

            } else {
                filesChangedText.setText("0 files changed");
                Log.w(TAG, "No files array in commit detail");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error parsing commit details", e);
            e.printStackTrace();
            Toast.makeText(this, "Error displaying some file changes", Toast.LENGTH_SHORT).show();
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

    private void copyCommitSha() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Commit SHA", commitSha);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Commit SHA copied", Toast.LENGTH_SHORT).show();
    }

    private void showCommitStats() {
        if (commitDetail == null) return;

        try {
            BottomSheetDialog dialog = new BottomSheetDialog(this);
            View statsView = getLayoutInflater().inflate(R.layout.bottom_sheet_commit_stats, null);

            // Get views
            TextView statsDetails = statsView.findViewById(R.id.statsDetails);
            TextView totalFilesCount = statsView.findViewById(R.id.totalFilesCount);
            TextView additionsCount = statsView.findViewById(R.id.additionsCount);
            TextView deletionsCount = statsView.findViewById(R.id.deletionsCount);

            // Calculate statistics
            Map<String, Integer> fileTypes = new HashMap<>();
            int maxChanges = 0;
            JSONObject largestFile = null;
            int totalAdditions = 0;
            int totalDeletions = 0;

            for (JSONObject file : filesList) {
                String filename = file.optString("filename", "");
                String ext = filename.contains(".") ?
                        filename.substring(filename.lastIndexOf(".") + 1).toUpperCase() : "OTHER";
                fileTypes.put(ext, fileTypes.getOrDefault(ext, 0) + 1);

                int changes = file.optInt("changes", 0);
                int additions = file.optInt("additions", 0);
                int deletions = file.optInt("deletions", 0);

                totalAdditions += additions;
                totalDeletions += deletions;

                if (changes > maxChanges) {
                    maxChanges = changes;
                    largestFile = file;
                }
            }

            // Set counts
            totalFilesCount.setText(String.valueOf(filesList.size()));
            additionsCount.setText("+" + totalAdditions);
            deletionsCount.setText("-" + totalDeletions);

            // Build details text
            StringBuilder sb = new StringBuilder();
            sb.append("File Types:\n");
            for (Map.Entry<String, Integer> entry : fileTypes.entrySet()) {
                sb.append("• ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" files\n");
            }

            if (largestFile != null) {
                sb.append("\nLargest Change:\n");
                sb.append(largestFile.optString("filename")).append(" (");
                sb.append("+").append(largestFile.optInt("additions")).append(" ");
                sb.append("-").append(largestFile.optInt("deletions")).append(")");
            }

            statsDetails.setText(sb.toString());

            dialog.setContentView(statsView);
            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing stats", e);
            Toast.makeText(this, "Error displaying statistics", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_commit_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share) {
            shareCommitDetails();
            return true;
        } else if (id == R.id.action_view_stats) {
            showCommitStats();
            return true;
        } else if (id == R.id.action_compare_parent) {
            compareWithParent();
            return true;
        } else if (id == R.id.action_view_on_github) {
            viewOnGitHub();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareCommitDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("Commit: ").append(commitSha.substring(0, Math.min(7, commitSha.length()))).append("\n");
        sb.append("Repository: ").append(repoOwner).append("/").append(repoName).append("\n");
        sb.append("Author: ").append(authorText.getText().toString()).append("\n");
        sb.append("Date: ").append(dateText.getText().toString()).append("\n");
        sb.append("Message: ").append(commitMessageText.getText().toString()).append("\n\n");
        sb.append("Changes: ").append(statsText.getText().toString()).append("\n");
        sb.append("Files Changed: ").append(filesList.size()).append("\n\n");

        // Add top 10 files
        sb.append("Files:\n");
        for (int i = 0; i < Math.min(10, filesList.size()); i++) {
            try {
                JSONObject file = filesList.get(i);
                String filename = file.optString("filename", "");
                int additions = file.optInt("additions", 0);
                int deletions = file.optInt("deletions", 0);
                sb.append("• ").append(filename).append(" (+").append(additions).append(" -").append(deletions).append(")\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (filesList.size() > 10) {
            sb.append("... and ").append(filesList.size() - 10).append(" more files\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Commit Details - " + commitSha.substring(0, Math.min(7, commitSha.length())));
        startActivity(Intent.createChooser(shareIntent, "Share Commit Details"));
    }

    private void compareWithParent() {
        if (commitDetail == null) {
            Toast.makeText(this, "Commit details not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray parents = commitDetail.optJSONArray("parents");
            if (parents != null && parents.length() > 0) {
                String parentSha = parents.getJSONObject(0).getString("sha");

                // Launch CompareActivity
                Intent intent = new Intent(this, CompareActivity.class);
                intent.putExtra("repo_owner", repoOwner);
                intent.putExtra("repo_name", repoName);
                intent.putExtra("base_sha", parentSha);
                intent.putExtra("head_sha", commitSha);
                startActivity(intent);
            } else {
                Toast.makeText(this, "No parent commit found (this might be the initial commit)",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error comparing with parent", e);
            Toast.makeText(this, "Error comparing commits", Toast.LENGTH_SHORT).show();
        }
    }

    private void viewOnGitHub() {
        String url = "https://github.com/" + repoOwner + "/" + repoName + "/commit/" + commitSha;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(url));

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No browser found", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToParentCommit() {
        if (commitDetail == null) {
            Toast.makeText(this, "Commit details not loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONArray parents = commitDetail.optJSONArray("parents");
            if (parents != null && parents.length() > 0) {
                JSONObject parent = parents.getJSONObject(0);
                String parentSha = parent.getString("sha");

                // Create new intent with parent commit data
                Intent intent = new Intent(this, CommitDetailActivity.class);
                intent.putExtra("repo_name", repoName);
                intent.putExtra("repo_owner", repoOwner);
                intent.putExtra("commit_sha", parentSha);
                intent.putExtra("commit_message", "Loading...");
                intent.putExtra("author_name", "Loading...");
                intent.putExtra("commit_date", "");

                startActivity(intent);
            } else {
                Toast.makeText(this, "This is the initial commit", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading parent commit", Toast.LENGTH_SHORT).show();
        }
    }
}