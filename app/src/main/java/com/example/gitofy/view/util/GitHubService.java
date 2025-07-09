package com.example.gitofy.view.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GitHubService {
    private final OkHttpClient client = new OkHttpClient();

    public interface UserCallback {
        void onSuccess(JSONObject user);
        void onError(Exception e);
    }

    public interface ReposCallback {
        void onSuccess(JSONArray repos);
        void onError(Exception e);
    }

    public void fetchUser(String token, UserCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/user")
                        .header("Authorization", "token " + token)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                JSONObject user = new JSONObject(response.body().string());
                callback.onSuccess(user);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void fetchRepos(String token, ReposCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/user/repos")
                        .header("Authorization", "token " + token)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                JSONArray repos = new JSONArray(response.body().string());
                callback.onSuccess(repos);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public interface CommitsCallback {
        void onSuccess(List<JSONObject> commits);
        void onError(Exception e);
    }

    public void fetchRecentCommits(String token, CommitsCallback callback) {
        new Thread(() -> {
            try {
                List<JSONObject> allCommits = new ArrayList<>();

                // First, get all repos
                Request reposRequest = new Request.Builder()
                        .url("https://api.github.com/user/repos?sort=pushed&per_page=20")
                        .header("Authorization", "token " + token)
                        .build();

                Response reposResponse = client.newCall(reposRequest).execute();
                if (!reposResponse.isSuccessful()) throw new IOException("Failed to fetch repos");

                JSONArray repos = new JSONArray(reposResponse.body().string());

                // For each repo, get recent commits
                for (int i = 0; i < Math.min(5, repos.length()); i++) {
                    JSONObject repo = repos.getJSONObject(i);
                    String repoName = repo.getString("name");
                    String fullName = repo.getString("full_name");
                    String defaultBranch = repo.optString("default_branch", "main");

                    // Get commits for this repo
                    Request commitsRequest = new Request.Builder()
                            .url("https://api.github.com/repos/" + fullName + "/commits?per_page=5")
                            .header("Authorization", "token " + token)
                            .build();

                    try {
                        Response commitsResponse = client.newCall(commitsRequest).execute();
                        if (commitsResponse.isSuccessful()) {
                            JSONArray commits = new JSONArray(commitsResponse.body().string());

                            for (int j = 0; j < commits.length(); j++) {
                                JSONObject commit = commits.getJSONObject(j);
                                commit.put("repo_name", repoName);
                                commit.put("branch", defaultBranch);
                                allCommits.add(commit);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Sort by date (newest first)
                allCommits.sort((a, b) -> {
                    try {
                        String dateA = a.getJSONObject("commit").getJSONObject("author").getString("date");
                        String dateB = b.getJSONObject("commit").getJSONObject("author").getString("date");
                        return dateB.compareTo(dateA);
                    } catch (Exception e) {
                        return 0;
                    }
                });

                callback.onSuccess(allCommits);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}

