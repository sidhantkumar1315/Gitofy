package com.example.gitofy.view.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.HeatmapActivity;
import com.example.gitofy.view.activities.IssuesActivity;
import com.example.gitofy.view.activities.RepoStatsActivity;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MoreFragment extends Fragment {

    private GitHubService gitHubService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        gitHubService = new GitHubService();
        setupFeatureButtons(view);

        return view;
    }

    private void setupFeatureButtons(View view) {
        // Heatmap Card
        CardView heatmapCard = view.findViewById(R.id.card_heatmap);
        heatmapCard.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), HeatmapActivity.class);
            startActivity(intent);
        });

        // Create Issue Card
        CardView createIssueCard = view.findViewById(R.id.card_create_issue);
        createIssueCard.setOnClickListener(v -> {
            showRepoSelectorDialog();
        });

        // Language Stats Card
        CardView languageStatsCard = view.findViewById(R.id.card_language_stats);
        languageStatsCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Language Stats coming soon!", Toast.LENGTH_SHORT).show();
        });

        CardView repoStatsCard = view.findViewById(R.id.card_filter_commits);
        repoStatsCard.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), RepoStatsActivity.class);
            startActivity(intent);
        });

        // Notifications Card
        CardView notificationsCard = view.findViewById(R.id.card_notifications);
        notificationsCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Notifications coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Milestones Card
        CardView milestonesCard = view.findViewById(R.id.card_milestones);
        milestonesCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Milestones coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Pull Requests Card
        CardView mergeRequestsCard = view.findViewById(R.id.card_merge_requests);
        mergeRequestsCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Pull Requests coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Profile Card
        CardView profileCard = view.findViewById(R.id.card_profile);
        profileCard.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Profile coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showRepoSelectorDialog() {
        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null || token.isEmpty()) {
            Toast.makeText(getContext(), "Please authenticate first", Toast.LENGTH_SHORT).show();
            return;
        }

        gitHubService.fetchRepos(token, new GitHubService.ReposCallback() {
            @Override
            public void onSuccess(JSONArray repos) {
                getActivity().runOnUiThread(() -> {
                    try {
                        if (repos.length() == 0) {
                            Toast.makeText(getContext(), "No repositories found", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create arrays for repo names and owners
                        String[] repoNames = new String[repos.length()];
                        String[] repoOwners = new String[repos.length()];

                        for (int i = 0; i < repos.length(); i++) {
                            JSONObject repo = repos.getJSONObject(i);
                            repoNames[i] = repo.getString("name");
                            repoOwners[i] = repo.getJSONObject("owner").getString("login");
                        }

                        new AlertDialog.Builder(getContext())
                                .setTitle("Select Repository")
                                .setItems(repoNames, (dialog, which) -> {
                                    Intent intent = new Intent(getContext(), IssuesActivity.class);
                                    intent.putExtra("repo_owner", repoOwners[which]);
                                    intent.putExtra("repo_name", repoNames[which]);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancel", null)
                                .show();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Error parsing repositories", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load repositories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}