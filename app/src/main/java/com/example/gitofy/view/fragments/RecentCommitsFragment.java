package com.example.gitofy.view.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.CommitAdapter;
import com.example.gitofy.view.util.BranchColorManager;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RecentCommitsFragment extends Fragment {
    private static final String TAG = "RecentCommitsFragment";

    private RecyclerView commitRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private TextView emptyView;
    private HorizontalScrollView branchLegend;
    private LinearLayout legendContainer;
    private CommitAdapter adapter;

    // Search components
    private CardView searchCard;
    private EditText searchEditText;
    private ImageButton clearSearchButton;

    // Data lists
    private List<JSONObject> allCommits = new ArrayList<>(); // All commits
    private List<JSONObject> filteredCommits = new ArrayList<>(); // Filtered commits for display
    private Map<String, String> uniqueBranches = new LinkedHashMap<>();
    private Map<String, View> legendItems = new LinkedHashMap<>();
    private View currentSelectedLegendItem = null;
    private Handler scrollHandler = new Handler();
    private Handler searchHandler = new Handler();
    private String currentSearchQuery = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_recent_commits_with_legend, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupSearch();
        loadRecentCommits();

        return view;
    }

    private void initializeViews(View view) {
        commitRecyclerView = view.findViewById(R.id.commitRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);
        emptyView = view.findViewById(R.id.emptyView);
        branchLegend = view.findViewById(R.id.branchLegend);
        legendContainer = view.findViewById(R.id.legendContainer);

        // Search views
        searchCard = view.findViewById(R.id.searchCard);
        searchEditText = view.findViewById(R.id.searchEditText);
        clearSearchButton = view.findViewById(R.id.clearSearchButton);
    }

    private void setupSearch() {
        // Text change listener with debouncing
        searchEditText.addTextChangedListener(new TextWatcher() {
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                currentSearchQuery = query;

                // Show/hide clear button
                clearSearchButton.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);

                // Debounce search
                searchRunnable = () -> performSearch(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });

        // Clear button click
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            currentSearchQuery = "";
            performSearch("");
        });

        // Handle search action from keyboard
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(currentSearchQuery);
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void performSearch(String query) {
        filteredCommits.clear();

        if (query.isEmpty()) {
            // Show all commits
            filteredCommits.addAll(allCommits);
        } else {
            // Filter commits
            String lowerQuery = query.toLowerCase();
            for (JSONObject commit : allCommits) {
                if (matchesSearch(commit, lowerQuery)) {
                    filteredCommits.add(commit);
                }
            }
        }

        // Update UI
        adapter.notifyDataSetChanged();

        // Update empty view
        if (filteredCommits.isEmpty() && !query.isEmpty()) {
            emptyView.setText("No commits found for \"" + query + "\"");
            emptyView.setVisibility(View.VISIBLE);
            commitRecyclerView.setVisibility(View.GONE);
        } else if (filteredCommits.isEmpty()) {
            emptyView.setText("No commits found");
            emptyView.setVisibility(View.VISIBLE);
            commitRecyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            commitRecyclerView.setVisibility(View.VISIBLE);
        }

        // Update legend based on filtered commits
        updateLegendForFilteredCommits();
    }

    private boolean matchesSearch(JSONObject commit, String query) {
        try {
            // Search in commit message
            JSONObject commitObj = commit.optJSONObject("commit");
            if (commitObj != null) {
                String message = commitObj.optString("message", "").toLowerCase();
                if (message.contains(query)) return true;

                // Search in author name
                JSONObject author = commitObj.optJSONObject("author");
                if (author != null) {
                    String authorName = author.optString("name", "").toLowerCase();
                    if (authorName.contains(query)) return true;
                }
            }

            // Search in SHA
            String sha = commit.optString("sha", "").toLowerCase();
            if (sha.contains(query)) return true;

            // Search in repo name
            String repoName = commit.optString("repo_name", "").toLowerCase();
            if (repoName.contains(query)) return true;

            // Search in branch name
            String branchName = commit.optString("branch", "").toLowerCase();
            if (branchName.contains(query)) return true;

        } catch (Exception e) {
            Log.e(TAG, "Error searching commit", e);
        }

        return false;
    }

    private void updateLegendForFilteredCommits() {
        // Clear and rebuild branches based on filtered commits
        Map<String, String> filteredBranches = new LinkedHashMap<>();

        for (JSONObject commit : filteredCommits) {
            String repoName = commit.optString("repo_name", "Unknown");
            String branchName = commit.optString("branch", "main");
            String key = repoName + "/" + branchName;
            filteredBranches.put(key, repoName + "|" + branchName);
        }

        // Update legend with filtered branches
        legendContainer.removeAllViews();
        legendItems.clear();
        currentSelectedLegendItem = null;

        BranchColorManager colorManager = BranchColorManager.getInstance(getContext());

        for (Map.Entry<String, String> entry : filteredBranches.entrySet()) {
            String key = entry.getKey();
            String[] parts = entry.getValue().split("\\|");
            String repoName = parts[0];
            String branchName = parts[1];

            View legendItem = createLegendItem(repoName, branchName,
                    colorManager.getColorForBranch(repoName, branchName));

            legendItems.put(key, legendItem);

            legendItem.setOnClickListener(v -> {
                for (int i = 0; i < filteredCommits.size(); i++) {
                    JSONObject commit = filteredCommits.get(i);
                    String commitRepo = commit.optString("repo_name", "");
                    String commitBranch = commit.optString("branch", "");
                    if (commitRepo.equals(repoName) && commitBranch.equals(branchName)) {
                        commitRecyclerView.smoothScrollToPosition(i);
                        break;
                    }
                }
            });

            legendContainer.addView(legendItem);
        }

        // Update legend visibility
        if (filteredBranches.isEmpty()) {
            ((View)branchLegend.getParent()).setVisibility(View.GONE);
        } else {
            ((View)branchLegend.getParent()).setVisibility(View.VISIBLE);
        }
    }

    private void hideKeyboard() {
        if (getActivity() != null) {
            View view = getActivity().getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        commitRecyclerView.setLayoutManager(layoutManager);
        adapter = new CommitAdapter(filteredCommits); // Use filtered list
        commitRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            searchEditText.setText(""); // Clear search on refresh
            loadRecentCommits();
        });

        commitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private final int SCROLL_DELAY = 150;
            private Runnable scrollRunnable;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (scrollRunnable != null) {
                    scrollHandler.removeCallbacks(scrollRunnable);
                }

                scrollRunnable = () -> updateLegendSelection();
                scrollHandler.postDelayed(scrollRunnable, SCROLL_DELAY);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    updateLegendSelection();
                }
            }
        });
    }

    private void updateLegendSelection() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) commitRecyclerView.getLayoutManager();
        if (layoutManager == null || legendItems.isEmpty()) return;

        int firstVisiblePosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (firstVisiblePosition == RecyclerView.NO_POSITION) {
            firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        }

        if (firstVisiblePosition >= 0 && firstVisiblePosition < filteredCommits.size()) {
            JSONObject topCommit = filteredCommits.get(firstVisiblePosition);
            String repoName = topCommit.optString("repo_name", "Unknown");
            String branchName = topCommit.optString("branch", "main");
            String key = repoName + "/" + branchName;

            selectLegendItem(key);
        }
    }

    private void selectLegendItem(String key) {
        View targetItem = legendItems.get(key);
        if (targetItem == null || targetItem == currentSelectedLegendItem) return;

        // Deselect previous item
        if (currentSelectedLegendItem != null) {
            currentSelectedLegendItem.setSelected(false);
        }

        // Select new item
        targetItem.setSelected(true);
        currentSelectedLegendItem = targetItem;

        // Smooth scroll to center the selected item
        scrollHandler.removeCallbacksAndMessages(null);
        scrollHandler.postDelayed(() -> {
            // Get the horizontal position of the item relative to its parent
            int itemLeft = targetItem.getLeft();
            int itemWidth = targetItem.getWidth();
            int itemCenter = itemLeft + (itemWidth / 2);

            // Get the scroll view dimensions
            int scrollViewWidth = branchLegend.getWidth();
            int scrollViewCenter = scrollViewWidth / 2;

            // Calculate the scroll position to center the item
            int scrollTo = itemCenter - scrollViewCenter;

            // Ensure we don't scroll beyond bounds
            scrollTo = Math.max(0, scrollTo);

            // Smooth scroll to the calculated position
            branchLegend.smoothScrollTo(scrollTo, 0);

            // Optional: Add some visual feedback
            animateSelection(targetItem);
        }, 100);
    }

    private void animateSelection(View view) {
        view.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void loadRecentCommits() {
        Log.d(TAG, "loadRecentCommits called");
        progressBar.setVisibility(View.VISIBLE);
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        // Clear previous data
        uniqueBranches.clear();
        legendItems.clear();
        currentSelectedLegendItem = null;
        BranchColorManager.getInstance(getContext()).clearColors();

        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        Log.d(TAG, "Token exists: " + (token != null));

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchRecentCommits(token, new GitHubService.CommitsCallback() {
            @Override
            public void onSuccess(List<JSONObject> commits) {
                Log.d(TAG, "onSuccess: Received " + commits.size() + " commits");
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    allCommits.clear();
                    allCommits.addAll(commits);

                    // Initially show all commits
                    filteredCommits.clear();
                    filteredCommits.addAll(allCommits);

                    // Collect unique branches
                    for (JSONObject commit : commits) {
                        String repoName = commit.optString("repo_name", "Unknown");
                        String branchName = commit.optString("branch", "main");
                        String key = repoName + "/" + branchName;
                        uniqueBranches.put(key, repoName + "|" + branchName);
                    }

                    // Update UI
                    updateLegend();
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);

                    // Show/hide views based on data
                    if (filteredCommits.isEmpty()) {
                        if (emptyView != null) {
                            emptyView.setVisibility(View.VISIBLE);
                            emptyView.setText("No commits found");
                        }
                        commitRecyclerView.setVisibility(View.GONE);
                        ((View)branchLegend.getParent()).setVisibility(View.GONE);
                    } else {
                        if (emptyView != null) emptyView.setVisibility(View.GONE);
                        commitRecyclerView.setVisibility(View.VISIBLE);
                        ((View)branchLegend.getParent()).setVisibility(View.VISIBLE);

                        // Select first item by default
                        scrollHandler.postDelayed(() -> updateLegendSelection(), 200);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "onError: Failed to load commits", e);
                if (getActivity() == null) return;

                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    ((View)branchLegend.getParent()).setVisibility(View.GONE);
                    if (emptyView != null) {
                        emptyView.setVisibility(View.VISIBLE);
                        emptyView.setText("Failed to load commits: " + e.getMessage());
                    }
                });
            }
        });
    }

    private void updateLegend() {
        legendContainer.removeAllViews();
        legendItems.clear();

        BranchColorManager colorManager = BranchColorManager.getInstance(getContext());

        for (Map.Entry<String, String> entry : uniqueBranches.entrySet()) {
            String key = entry.getKey();
            String[] parts = entry.getValue().split("\\|");
            String repoName = parts[0];
            String branchName = parts[1];

            // Create legend item
            View legendItem = createLegendItem(repoName, branchName,
                    colorManager.getColorForBranch(repoName, branchName));

            // Store reference for selection
            legendItems.put(key, legendItem);

            // Add click listener
            legendItem.setOnClickListener(v -> {
                // Find and scroll to the first commit with this repo/branch
                for (int i = 0; i < filteredCommits.size(); i++) {
                    JSONObject commit = filteredCommits.get(i);
                    String commitRepo = commit.optString("repo_name", "");
                    String commitBranch = commit.optString("branch", "");
                    if (commitRepo.equals(repoName) && commitBranch.equals(branchName)) {
                        commitRecyclerView.smoothScrollToPosition(i);
                        break;
                    }
                }
            });

            legendContainer.addView(legendItem);
        }
    }

    private View createLegendItem(String repoName, String branchName, int color) {
        View item = LayoutInflater.from(getContext()).inflate(R.layout.legend_item, null);

        View colorDot = item.findViewById(R.id.colorDot);
        TextView repoText = item.findViewById(R.id.repoText);
        TextView branchText = item.findViewById(R.id.branchText);

        // Set color
        GradientDrawable shape = (GradientDrawable) colorDot.getBackground();
        shape.setColor(color);

        // Set texts
        repoText.setText(repoName);
        branchText.setText(branchName);

        // Add margin between items
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        params.setMargins(4, 0, 4, 0);
        item.setLayoutParams(params);

        return item;
    }
}