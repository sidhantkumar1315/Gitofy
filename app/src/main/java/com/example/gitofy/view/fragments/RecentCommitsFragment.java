package com.example.gitofy.view.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
    private List<JSONObject> commitList = new ArrayList<>();
    private Map<String, String> uniqueBranches = new LinkedHashMap<>(); // LinkedHashMap to maintain order
    private Map<String, View> legendItems = new LinkedHashMap<>();
    private View currentSelectedLegendItem = null;
    private Handler scrollHandler = new Handler();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_recent_commits_with_legend, container, false);

        initializeViews(view);
        setupRecyclerView();
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
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        commitRecyclerView.setLayoutManager(layoutManager);
        adapter = new CommitAdapter(commitList);
        commitRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadRecentCommits);


        commitRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private final int SCROLL_DELAY = 150;
            private Runnable scrollRunnable;

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Remove any pending scroll callbacks
                if (scrollRunnable != null) {
                    scrollHandler.removeCallbacks(scrollRunnable);
                }

                // Post a new callback with delay (debouncing)
                scrollRunnable = new Runnable() {
                    @Override
                    public void run() {
                        updateLegendSelection();
                    }
                };
                scrollHandler.postDelayed(scrollRunnable, SCROLL_DELAY);
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // Update immediately when scrolling stops
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
        // If no item is completely visible, use the first visible item
        if (firstVisiblePosition == RecyclerView.NO_POSITION) {
            firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
        }

        if (firstVisiblePosition >= 0 && firstVisiblePosition < commitList.size()) {
            JSONObject topCommit = commitList.get(firstVisiblePosition);
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
        scrollHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
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
            }
        }, 100);
    }

    // Optional: Add a subtle animation when selecting an item
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
                    commitList.clear();
                    commitList.addAll(commits);

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
                    if (commitList.isEmpty()) {
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
                for (int i = 0; i < commitList.size(); i++) {
                    JSONObject commit = commitList.get(i);
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