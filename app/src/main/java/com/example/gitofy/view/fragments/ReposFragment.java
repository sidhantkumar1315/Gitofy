package com.example.gitofy.view.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.RepoAdapter;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ReposFragment extends Fragment {

    private RecyclerView repoRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_repos, container, false);

        repoRecyclerView = view.findViewById(R.id.repoRecyclerView);
        repoRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        DividerItemDecoration divider = new DividerItemDecoration(repoRecyclerView.getContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider));
        repoRecyclerView.addItemDecoration(divider);

        loadRepos();

        return view;
    }

    private void loadRepos() {
        SharedPreferences prefs = getActivity().getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchRepos(token, new GitHubService.ReposCallback() {
            @Override
            public void onSuccess(JSONArray repos) {
                getActivity().runOnUiThread(() -> {
                    displayRepos(repos);
                });
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void displayRepos(JSONArray reposJson) {
        try {
            List<JSONObject> repoList = new ArrayList<>();
            for (int i = 0; i < reposJson.length(); i++) {
                JSONObject repo = reposJson.getJSONObject(i);
                JSONObject owner = repo.getJSONObject("owner");
                String ownerLogin = owner.getString("login");
                Log.d("RepoOwner", "Owner: " + ownerLogin );
                repoList.add(repo); // If you're still passing the whole repo object to the adapter
            }


            RepoAdapter adapter = new RepoAdapter(repoList);
            repoRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load repos", Toast.LENGTH_SHORT).show();
        }
    }
}