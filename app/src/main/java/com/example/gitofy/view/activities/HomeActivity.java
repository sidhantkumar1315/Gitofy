package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gitofy.R;
import com.example.gitofy.view.adpaters.RepoAdapter;
import com.example.gitofy.view.util.GitHubService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    RecyclerView repoRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        repoRecyclerView = findViewById(R.id.repoRecyclerView);
        repoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String reposJson = getIntent().getStringExtra("repos_json");
        SharedPreferences prefs = this.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);
        GitHubService gitHubService = new GitHubService();

        gitHubService.fetchRepos(token, new GitHubService.ReposCallback() {
            @Override
            public void onSuccess(JSONArray repos) {
                // Use this list in your RecyclerView
                runOnUiThread(() -> {
                    repoArray(repos);
                });
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
    private void repoArray(JSONArray reposJson){
        try {
            JSONArray repoArray = new JSONArray(reposJson);
            List<JSONObject> repoList = new ArrayList<>();
            for (int i = 0; i < repoArray.length(); i++) {
                repoList.add(repoArray.getJSONObject(i));
            }

            RepoAdapter adapter = new RepoAdapter(repoList);
            repoRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load repos", Toast.LENGTH_SHORT).show();
        }
    }
}


