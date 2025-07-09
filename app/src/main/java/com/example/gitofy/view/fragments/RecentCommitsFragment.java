package com.example.gitofy.view.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.CommitAdapter;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class RecentCommitsFragment extends Fragment {

    private RecyclerView commitRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private CommitAdapter adapter;
    private List<JSONObject> commitList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent_commits, container, false);

        commitRecyclerView = view.findViewById(R.id.commitRecyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        progressBar = view.findViewById(R.id.progressBar);

        commitRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CommitAdapter(commitList);
        commitRecyclerView.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(this::loadRecentCommits);

        loadRecentCommits();

        return view;
    }

    private void loadRecentCommits() {
        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchRecentCommits(token, new GitHubService.CommitsCallback() {
            @Override
            public void onSuccess(List<JSONObject> commits) {
                getActivity().runOnUiThread(() -> {
                    commitList.clear();
                    commitList.addAll(commits);
                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                });
            }

            @Override
            public void onError(Exception e) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Failed to load commits", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                });
                e.printStackTrace();
            }
        });
    }
}