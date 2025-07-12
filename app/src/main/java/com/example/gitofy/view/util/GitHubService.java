package com.example.gitofy.view.util;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GitHubService {
    private static final String TAG = "GitHubService";
    private final OkHttpClient client = new OkHttpClient();

    public interface UserCallback {
        void onSuccess(JSONObject user);
        void onError(Exception e);
    }

    public interface ReposCallback {
        void onSuccess(JSONArray repos);
        void onError(Exception e);
    }

    public interface CommitsCallback {
        void onSuccess(List<JSONObject> commits);
        void onError(Exception e);
    }

    public interface CommitDetailCallback {
        void onSuccess(JSONObject commitDetail);
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

    public void fetchRecentCommits(String token, CommitsCallback callback) {
        new Thread(() -> {
            try {
                List<JSONObject> allCommits = new ArrayList<>();

                Log.d(TAG, "Starting fetchRecentCommits with token: " + (token != null ? "exists" : "null"));

                // First, get all repos
                Request reposRequest = new Request.Builder()
                        .url("https://api.github.com/user/repos?sort=pushed&per_page=10")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response reposResponse = client.newCall(reposRequest).execute();
                Log.d(TAG, "Repos response code: " + reposResponse.code());

                if (!reposResponse.isSuccessful()) {
                    String errorBody = reposResponse.body() != null ? reposResponse.body().string() : "No error body";
                    Log.e(TAG, "Failed to fetch repos. Code: " + reposResponse.code() + ", Body: " + errorBody);
                    throw new IOException("Failed to fetch repos: " + reposResponse.code());
                }

                String reposJson = reposResponse.body().string();
                JSONArray repos = new JSONArray(reposJson);

                // For each repo, get all branches and their commits
                for (int i = 0; i < Math.min(5, repos.length()); i++) {
                    JSONObject repo = repos.getJSONObject(i);
                    String repoName = repo.getString("name");
                    String fullName = repo.getString("full_name");

                    // Extract owner information
                    String owner = "";
                    if (repo.has("owner")) {
                        JSONObject ownerObj = repo.getJSONObject("owner");
                        owner = ownerObj.getString("login");
                    }

                    // Get all branches for this repo
                    Request branchesRequest = new Request.Builder()
                            .url("https://api.github.com/repos/" + fullName + "/branches")
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.v3+json")
                            .build();

                    try {
                        Response branchesResponse = client.newCall(branchesRequest).execute();
                        if (branchesResponse.isSuccessful()) {
                            String branchesJson = branchesResponse.body().string();
                            JSONArray branches = new JSONArray(branchesJson);

                            // For each branch, get recent commits
                            for (int j = 0; j < branches.length(); j++) {
                                JSONObject branch = branches.getJSONObject(j);
                                String branchName = branch.getString("name");


                                // Get commits for this branch
                                Request commitsRequest = new Request.Builder()
                                        .url("https://api.github.com/repos/" + fullName + "/commits?sha=" + branchName + "&per_page=3")
                                        .header("Authorization", "token " + token)
                                        .header("Accept", "application/vnd.github.v3+json")
                                        .build();

                                try {
                                    Response commitsResponse = client.newCall(commitsRequest).execute();
                                    if (commitsResponse.isSuccessful()) {
                                        String commitsJson = commitsResponse.body().string();
                                        JSONArray commits = new JSONArray(commitsJson);

                                        for (int k = 0; k < commits.length(); k++) {
                                            JSONObject commit = commits.getJSONObject(k);
                                            // Add repo, branch, and owner info to commit object
                                            commit.put("repo_name", repoName);
                                            commit.put("branch", branchName);
                                            commit.put("repo_owner", owner);
                                            allCommits.add(commit);
                                        }
                                    }
                                } catch (Exception e) {
                                }
                            }
                        }
                    } catch (Exception e) {
                    }
                }


                // Sort by date (newest first)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    allCommits.sort((a, b) -> {
                        try {
                            String dateA = a.getJSONObject("commit").getJSONObject("author").getString("date");
                            String dateB = b.getJSONObject("commit").getJSONObject("author").getString("date");
                            return dateB.compareTo(dateA);
                        } catch (Exception e) {
                            return 0;
                        }
                    });
                }

                // Limit to most recent 50 commits to avoid too many items
                if (allCommits.size() > 50) {
                    allCommits = new ArrayList<>(allCommits.subList(0, 50));
                }

                callback.onSuccess(allCommits);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void fetchCommitDetails(String token, String owner, String repo, String sha,
                                   CommitDetailCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + sha)
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch commit details");
                }

                String json = response.body().string();
                JSONObject commitDetail = new JSONObject(json);

                callback.onSuccess(commitDetail);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }
}