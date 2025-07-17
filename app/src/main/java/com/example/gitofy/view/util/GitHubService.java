package com.example.gitofy.view.util;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

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

                // First, get all repos
                Request reposRequest = new Request.Builder()
                        .url("https://api.github.com/user/repos?sort=pushed&per_page=10")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response reposResponse = client.newCall(reposRequest).execute();

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

    // In GitHubService.java, update the fetchCommitDetails method:

    public void fetchCommitDetails(String token, String owner, String repo, String sha,
                                   CommitDetailCallback callback) {
        new Thread(() -> {
            try {
                // First, get the commit details
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

                // Check if we have all files
                JSONArray files = commitDetail.optJSONArray("files");
                int totalFiles = commitDetail.optJSONObject("stats").optInt("total", 0);

                Log.d(TAG, "Initial files count: " + (files != null ? files.length() : 0) + ", Total expected: " + totalFiles);

                // GitHub API limits files to 300 in the commit endpoint
                // If there are more files or if files array is smaller than expected, fetch using compare API
                if (files == null || files.length() < totalFiles || totalFiles > 300) {
                    Log.d(TAG, "Fetching additional file details using compare API");
                    fetchAllFilesUsingCompare(token, owner, repo, sha, commitDetail, callback);
                } else {
                    callback.onSuccess(commitDetail);
                }

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    // Add this new method to fetch all files using the compare API
    private void fetchAllFilesUsingCompare(String token, String owner, String repo, String sha,
                                           JSONObject originalCommitDetail, CommitDetailCallback callback) {
        try {
            // Get the parent SHA
            JSONArray parents = originalCommitDetail.optJSONArray("parents");
            String baseSha = (parents != null && parents.length() > 0)
                    ? parents.getJSONObject(0).getString("sha")
                    : sha + "~1"; // Use parent notation if no parent found

            // Use compare API to get all files
            Request request = new Request.Builder()
                    .url("https://api.github.com/repos/" + owner + "/" + repo +
                            "/compare/" + baseSha + "..." + sha)
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                JSONObject compareResult = new JSONObject(response.body().string());
                JSONArray allFiles = compareResult.optJSONArray("files");

                if (allFiles != null && allFiles.length() > 0) {
                    // Replace the files array with the complete list
                    originalCommitDetail.put("files", allFiles);
                    Log.d(TAG, "Updated files count: " + allFiles.length());
                }
            } else {
                Log.e(TAG, "Failed to fetch files using compare API: " + response.code());
            }

            callback.onSuccess(originalCommitDetail);

        } catch (Exception e) {
            Log.e(TAG, "Error in fetchAllFilesUsingCompare", e);
            // Fall back to original commit detail
            callback.onSuccess(originalCommitDetail);
        }
    }

    // Also add a method to fetch file contents if patches are truncated
    public interface FileContentCallback {
        void onSuccess(String content);
        void onError(Exception e);
    }

    public void fetchFileContent(String token, String owner, String repo, String path, String ref,
                                 FileContentCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo +
                                "/contents/" + path + "?ref=" + ref)
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3.raw")
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch file content");
                }

                String content = response.body().string();
                callback.onSuccess(content);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public interface CommitActivityCallback {
        void onSuccess(Map<String, Integer> commitCountByDate);
        void onError(Exception e);
    }

    public void fetchUserCommitActivity(String token, CommitActivityCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Integer> commitCountMap = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                Log.d(TAG, "Fetching user commit activity");


                Request userRequest = new Request.Builder()
                        .url("https://api.github.com/user")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response userResponse = client.newCall(userRequest).execute();
                if (!userResponse.isSuccessful()) {
                    throw new IOException("Failed to fetch user info");
                }

                JSONObject user = new JSONObject(userResponse.body().string());
                String username = user.getString("login");


                Request eventsRequest = new Request.Builder()
                        .url("https://api.github.com/users/" + username + "/events/public?per_page=100")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response eventsResponse = client.newCall(eventsRequest).execute();
                if (!eventsResponse.isSuccessful()) {
                    Log.e(TAG, "Failed to fetch events: " + eventsResponse.code());
                    fetchCommitsFromRepos(token, commitCountMap, callback);
                    return;
                }

                JSONArray events = new JSONArray(eventsResponse.body().string());

                // Process push events
                for (int i = 0; i < events.length(); i++) {
                    JSONObject event = events.getJSONObject(i);
                    String type = event.optString("type", "");

                    if ("PushEvent".equals(type)) {
                        String createdAt = event.optString("created_at", "");
                        if (!createdAt.isEmpty()) {
                            String date = createdAt.substring(0, 10); // Extract date part
                            int commitCount = event.optJSONObject("payload")
                                    .optJSONArray("commits")
                                    .length();
                            commitCountMap.put(date, commitCountMap.getOrDefault(date, 0) + commitCount);
                        }
                    }
                }

                // Also fetch recent commits from repositories
                fetchCommitsFromRepos(token, commitCountMap, callback);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private void fetchCommitsFromRepos(String token, Map<String, Integer> commitCountMap,
                                       CommitActivityCallback callback) {
        try {
            // Get user's repos
            Request reposRequest = new Request.Builder()
                    .url("https://api.github.com/user/repos?sort=pushed&per_page=10")
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            Response reposResponse = client.newCall(reposRequest).execute();
            if (!reposResponse.isSuccessful()) {
                callback.onSuccess(commitCountMap);
                return;
            }

            JSONArray repos = new JSONArray(reposResponse.body().string());

            // For each repo, get recent commits
            for (int i = 0; i < Math.min(5, repos.length()); i++) {
                JSONObject repo = repos.getJSONObject(i);
                String fullName = repo.getString("full_name");

                // Get commits from the last year
                Calendar oneYearAgo = Calendar.getInstance();
                oneYearAgo.add(Calendar.YEAR, -1);
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

                Request commitsRequest = new Request.Builder()
                        .url("https://api.github.com/repos/" + fullName + "/commits?since=" +
                                isoFormat.format(oneYearAgo.getTime()) + "&per_page=100")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                try {
                    Response commitsResponse = client.newCall(commitsRequest).execute();
                    if (commitsResponse.isSuccessful()) {
                        JSONArray commits = new JSONArray(commitsResponse.body().string());

                        for (int j = 0; j < commits.length(); j++) {
                            JSONObject commit = commits.getJSONObject(j);
                            JSONObject author = commit.optJSONObject("commit")
                                    .optJSONObject("author");
                            if (author != null) {
                                String date = author.optString("date", "").substring(0, 10);
                                if (!date.isEmpty()) {
                                    commitCountMap.put(date, commitCountMap.getOrDefault(date, 0) + 1);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching commits for repo: " + fullName, e);
                }
            }

            callback.onSuccess(commitCountMap);

        } catch (Exception e) {
            Log.e(TAG, "Error in fetchCommitsFromRepos", e);
            callback.onSuccess(commitCountMap);
        }
    }

    // Add this interface
    public interface ContributionsCallback {
        void onSuccess(Map<String, Integer> contributions, int totalContributions);
        void onError(Exception e);
    }

    public void fetchYearlyContributions(String token, String username, int year, ContributionsCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Integer> contributions = new HashMap<>();
                Set<String> processedDates = new HashSet<>();

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

                Calendar startCal = Calendar.getInstance();
                startCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);

                Calendar endCal = Calendar.getInstance();
                endCal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);

                Log.d(TAG, "Fetching contributions for " + username + " in year " + year);


                Request reposRequest = new Request.Builder()
                        .url("https://api.github.com/users/" + username + "/repos?per_page=100&type=owner")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response reposResponse = client.newCall(reposRequest).execute();
                if (reposResponse.isSuccessful()) {
                    JSONArray repos = new JSONArray(reposResponse.body().string());

                    for (int i = 0; i < repos.length(); i++) {
                        JSONObject repo = repos.getJSONObject(i);
                        String fullName = repo.getString("full_name");


                        Request commitsRequest = new Request.Builder()
                                .url("https://api.github.com/repos/" + fullName +
                                        "/commits?author=" + username +
                                        "&since=" + isoFormat.format(startCal.getTime()) +
                                        "&until=" + isoFormat.format(endCal.getTime()) +
                                        "&per_page=100")
                                .header("Authorization", "token " + token)
                                .header("Accept", "application/vnd.github.v3+json")
                                .build();

                        try {
                            Response commitsResponse = client.newCall(commitsRequest).execute();
                            if (commitsResponse.isSuccessful()) {
                                JSONArray commits = new JSONArray(commitsResponse.body().string());

                                for (int j = 0; j < commits.length(); j++) {
                                    JSONObject commit = commits.getJSONObject(j);
                                    JSONObject author = commit.getJSONObject("commit").getJSONObject("author");

                                    String dateStr = author.getString("date");
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(isoFormat.parse(dateStr));

                                    if (cal.get(Calendar.YEAR) == year) {
                                        String date = dateFormat.format(cal.getTime());
                                        processedDates.add(date);
                                        contributions.put(date, contributions.getOrDefault(date, 0) + 1);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error fetching commits for repo: " + fullName, e);
                        }
                    }
                }

                int totalContributions = 0;
                for (Map.Entry<String, Integer> entry : contributions.entrySet()) {
                    totalContributions += entry.getValue();
                }

                Log.d(TAG, "Total contributions for year " + year + ": " + totalContributions);
                callback.onSuccess(contributions, totalContributions);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    private void fetchCommitsForYear(String token, String username, int year,
                                     Map<String, Integer> contributions, int totalContributions,
                                     ContributionsCallback callback) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

            Calendar startCal = Calendar.getInstance();
            startCal.set(year, Calendar.JANUARY, 1, 0, 0, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);

            // Get user's repos
            Request reposRequest = new Request.Builder()
                    .url("https://api.github.com/user/repos?per_page=100&type=owner")
                    .header("Authorization", "token " + token)
                    .header("Accept", "application/vnd.github.v3+json")
                    .build();

            Response reposResponse = client.newCall(reposRequest).execute();
            if (reposResponse.isSuccessful()) {
                JSONArray repos = new JSONArray(reposResponse.body().string());

                for (int i = 0; i < Math.min(repos.length(), 20); i++) { // Limit to 20 repos
                    JSONObject repo = repos.getJSONObject(i);
                    String fullName = repo.getString("full_name");

                    // Get commits for this repo in the specified year
                    Request commitsRequest = new Request.Builder()
                            .url("https://api.github.com/repos/" + fullName +
                                    "/commits?author=" + username +
                                    "&since=" + isoFormat.format(startCal.getTime()) +
                                    "&until=" + isoFormat.format(endCal.getTime()) +
                                    "&per_page=100")
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.v3+json")
                            .build();

                    try {
                        Response commitsResponse = client.newCall(commitsRequest).execute();
                        if (commitsResponse.isSuccessful()) {
                            JSONArray commits = new JSONArray(commitsResponse.body().string());

                            for (int j = 0; j < commits.length(); j++) {
                                JSONObject commit = commits.getJSONObject(j);
                                JSONObject author = commit.getJSONObject("commit").getJSONObject("author");
                                String dateStr = author.getString("date");
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(isoFormat.parse(dateStr));

                                if (cal.get(Calendar.YEAR) == year) {
                                    String date = dateFormat.format(cal.getTime());
                                    contributions.put(date, contributions.getOrDefault(date, 0) + 1);
                                    totalContributions++;
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error fetching commits for repo: " + fullName, e);
                    }
                }
            }

            callback.onSuccess(contributions, totalContributions);

        } catch (Exception e) {
            Log.e(TAG, "Error in fetchCommitsForYear", e);
            callback.onSuccess(contributions, totalContributions);
        }
    }


    public interface IssuesCallback {
        void onSuccess(JSONArray issues);
        void onError(Exception e);
    }

    public interface CreateIssueCallback {
        void onSuccess(JSONObject issue);
        void onError(Exception e);
    }

    public void fetchIssues(String token, String owner, String repo, IssuesCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/issues?state=all")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch issues: " + response.code());
                }

                JSONArray issues = new JSONArray(response.body().string());
                callback.onSuccess(issues);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void createIssue(String token, String owner, String repo, String title,
                            String body, CreateIssueCallback callback) {
        new Thread(() -> {
            try {
                JSONObject issueBody = new JSONObject();
                issueBody.put("title", title);
                issueBody.put("body", body);

                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(JSON, issueBody.toString());

                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/issues")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to create issue: " + response.code());
                }

                JSONObject createdIssue = new JSONObject(response.body().string());
                callback.onSuccess(createdIssue);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    // Add these methods to your existing GitHubService class

    public interface LabelsCallback {
        void onSuccess(JSONArray labels);
        void onError(Exception e);
    }

    public interface AssigneesCallback {
        void onSuccess(JSONArray assignees);
        void onError(Exception e);
    }

    public interface MilestonesCallback {
        void onSuccess(JSONArray milestones);
        void onError(Exception e);
    }

    public void fetchLabels(String token, String owner, String repo, LabelsCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/labels")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch labels: " + response.code());
                }

                JSONArray labels = new JSONArray(response.body().string());
                callback.onSuccess(labels);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void fetchAssignees(String token, String owner, String repo, AssigneesCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/assignees")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch assignees: " + response.code());
                }

                JSONArray assignees = new JSONArray(response.body().string());
                callback.onSuccess(assignees);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void fetchMilestones(String token, String owner, String repo, MilestonesCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/milestones")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch milestones: " + response.code());
                }

                JSONArray milestones = new JSONArray(response.body().string());
                callback.onSuccess(milestones);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void createIssue(String token, String owner, String repo, String title,
                            String body, List<String> assignees, List<String> labels,
                            Integer milestone, CreateIssueCallback callback) {
        new Thread(() -> {
            try {
                JSONObject issueBody = new JSONObject();
                issueBody.put("title", title);
                issueBody.put("body", body);

                if (assignees != null && !assignees.isEmpty()) {
                    JSONArray assigneesArray = new JSONArray(assignees);
                    issueBody.put("assignees", assigneesArray);
                }

                if (labels != null && !labels.isEmpty()) {
                    JSONArray labelsArray = new JSONArray(labels);
                    issueBody.put("labels", labelsArray);
                }

                if (milestone != null) {
                    issueBody.put("milestone", milestone);
                }

                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(JSON, issueBody.toString());

                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/issues")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to create issue: " + response.code());
                }

                JSONObject createdIssue = new JSONObject(response.body().string());
                callback.onSuccess(createdIssue);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public interface IssueDetailCallback {
        void onSuccess(JSONObject issue);
        void onError(Exception e);
    }

    public interface UpdateIssueCallback {
        void onSuccess(JSONObject issue);
        void onError(Exception e);
    }

    public void fetchIssueDetail(String token, String owner, String repo, int issueNumber, IssueDetailCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + issueNumber)
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch issue detail: " + response.code());
                }

                JSONObject issue = new JSONObject(response.body().string());
                callback.onSuccess(issue);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void updateIssueState(String token, String owner, String repo, int issueNumber, String state, UpdateIssueCallback callback) {
        new Thread(() -> {
            try {
                JSONObject updateBody = new JSONObject();
                updateBody.put("state", state);

                okhttp3.MediaType JSON = okhttp3.MediaType.parse("application/json; charset=utf-8");
                okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(JSON, updateBody.toString());

                Request request = new Request.Builder()
                        .url("https://api.github.com/repos/" + owner + "/" + repo + "/issues/" + issueNumber)
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .patch(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to update issue: " + response.code());
                }

                JSONObject updatedIssue = new JSONObject(response.body().string());
                callback.onSuccess(updatedIssue);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    // Add to GitHubService.java
    public interface CompareCallback {
        void onSuccess(JSONObject comparison);
        void onError(Exception e);
    }

    public void compareCommits(String token, String owner, String repo,
                               String base, String head, CompareCallback callback) {
        new Thread(() -> {
            try {
                String url = "https://api.github.com/repos/" + owner + "/" + repo +
                        "/compare/" + base + "..." + head;

                Request request = new Request.Builder()
                        .url(url)
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    throw new IOException("Failed to compare commits: " + response.code());
                }

                JSONObject comparison = new JSONObject(response.body().string());
                callback.onSuccess(comparison);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public interface RepoStatsCallback {
        void onSuccess(JSONArray repos, Map<String, Object> aggregateStats);
        void onError(Exception e);
    }

    public void fetchRepoStats(String token, RepoStatsCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url("https://api.github.com/user/repos?per_page=100&type=owner")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to fetch repositories: " + response.code());
                }

                JSONArray repos = new JSONArray(response.body().string());

                // Calculating aggregate statistics
                Map<String, Object> aggregateStats = new HashMap<>();
                int totalRepos = repos.length();
                int totalStars = 0;
                int totalForks = 0;
                long totalSize = 0;

                for (int i = 0; i < repos.length(); i++) {
                    JSONObject repo = repos.getJSONObject(i);
                    totalStars += repo.optInt("stargazers_count", 0);
                    totalForks += repo.optInt("forks_count", 0);
                    totalSize += repo.optLong("size", 0);
                }

                aggregateStats.put("totalRepos", totalRepos);
                aggregateStats.put("totalStars", totalStars);
                aggregateStats.put("totalForks", totalForks);
                aggregateStats.put("totalSize", totalSize);

                callback.onSuccess(repos, aggregateStats);

            } catch (Exception e) {
                callback.onError(e);
            }
        }).start();
    }

    public void fetchRepoCommitActivity(String token, List<JSONObject> repos,
                                        CommitActivityCallback callback) {
        new Thread(() -> {
            try {
                Map<String, Integer> commitCountMap = new HashMap<>();
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                // Get commits from the last 30 days
                Calendar thirtyDaysAgo = Calendar.getInstance();
                thirtyDaysAgo.add(Calendar.DAY_OF_MONTH, -30);

                // Get current user info
                Request userRequest = new Request.Builder()
                        .url("https://api.github.com/user")
                        .header("Authorization", "token " + token)
                        .header("Accept", "application/vnd.github.v3+json")
                        .build();

                Response userResponse = client.newCall(userRequest).execute();
                if (!userResponse.isSuccessful()) {
                    callback.onError(new IOException("Failed to get user info"));
                    return;
                }

                JSONObject user = new JSONObject(userResponse.body().string());
                String username = user.getString("login");

                boolean foundEventsData = false;
                try {
                    Request eventsRequest = new Request.Builder()
                            .url("https://api.github.com/users/" + username + "/events?per_page=100")
                            .header("Authorization", "token " + token)
                            .header("Accept", "application/vnd.github.v3+json")
                            .build();

                    Response eventsResponse = client.newCall(eventsRequest).execute();
                    if (eventsResponse.isSuccessful()) {
                        JSONArray events = new JSONArray(eventsResponse.body().string());

                        for (int i = 0; i < events.length(); i++) {
                            JSONObject event = events.getJSONObject(i);
                            String type = event.optString("type", "");

                            if ("PushEvent".equals(type)) {
                                String createdAt = event.optString("created_at", "");
                                if (!createdAt.isEmpty()) {
                                    Date eventDate = isoFormat.parse(createdAt);

                                    if (eventDate.after(thirtyDaysAgo.getTime())) {
                                        String date = dateFormat.format(eventDate);
                                        JSONObject payload = event.optJSONObject("payload");
                                        if (payload != null) {
                                            JSONArray commits = payload.optJSONArray("commits");
                                            int commitCount = commits != null ? commits.length() : 1;
                                            commitCountMap.put(date,
                                                    commitCountMap.getOrDefault(date, 0) + commitCount);
                                            foundEventsData = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Events API failed, will fallback to repository scanning", e);
                }

                if (!foundEventsData || commitCountMap.isEmpty()) {
                    Log.d(TAG, "No events data found, scanning repositories directly");

                    // Sort repos by updated date to get most recently active ones
                    Collections.sort(repos, (a, b) -> {
                        String dateA = a.optString("updated_at", "");
                        String dateB = b.optString("updated_at", "");
                        return dateB.compareTo(dateA);
                    });

                    // Check repositories for commits
                    int reposToCheck = Math.min(repos.size(), 20); // Check up to 20 repos
                    Log.d(TAG, "Checking " + reposToCheck + " repositories for commits");

                    for (int i = 0; i < reposToCheck; i++) {
                        JSONObject repo = repos.get(i);
                        String fullName = repo.getString("full_name");

                        // Skip if repo was updated before our time window
                        String updatedAt = repo.optString("updated_at", "");
                        if (!updatedAt.isEmpty()) {
                            try {
                                Date updateDate = isoFormat.parse(updatedAt);
                                if (updateDate.before(thirtyDaysAgo.getTime())) {
                                    Log.d(TAG, "Skipping " + fullName + " - not updated recently");
                                    continue;
                                }
                            } catch (Exception e) {
                                // Continue anyway if date parsing fails
                            }
                        }

                        // Get commits for this specific user from this repo
                        Request commitsRequest = new Request.Builder()
                                .url("https://api.github.com/repos/" + fullName +
                                        "/commits?author=" + username +
                                        "&since=" + isoFormat.format(thirtyDaysAgo.getTime()) +
                                        "&per_page=100")
                                .header("Authorization", "token " + token)
                                .header("Accept", "application/vnd.github.v3+json")
                                .build();

                        try {
                            Response commitsResponse = client.newCall(commitsRequest).execute();
                            if (commitsResponse.isSuccessful()) {
                                JSONArray commits = new JSONArray(commitsResponse.body().string());
                                Log.d(TAG, "Found " + commits.length() + " commits in " + fullName);

                                for (int j = 0; j < commits.length(); j++) {
                                    JSONObject commit = commits.getJSONObject(j);
                                    JSONObject commitData = commit.getJSONObject("commit");
                                    JSONObject author = commitData.getJSONObject("author");

                                    // Verify it's by the current user (double-check)
                                    String authorEmail = author.optString("email", "");
                                    String authorName = author.optString("name", "");

                                    String dateStr = author.getString("date");
                                    Date commitDate = isoFormat.parse(dateStr);
                                    String date = dateFormat.format(commitDate);

                                    commitCountMap.put(date,
                                            commitCountMap.getOrDefault(date, 0) + 1);
                                }
                            } else {
                                Log.w(TAG, "Failed to fetch commits for " + fullName + ": " + commitsResponse.code());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error fetching commits for repo: " + fullName, e);
                        }
                    }
                }


                int totalCommits = 0;
                for (Map.Entry<String, Integer> entry : commitCountMap.entrySet()) {
                    totalCommits += entry.getValue();
                    Log.d(TAG, "Date: " + entry.getKey() + ", Commits: " + entry.getValue());
                }
                Log.d(TAG, "Total commits in period: " + totalCommits);

                callback.onSuccess(commitCountMap);

            } catch (Exception e) {
                Log.e(TAG, "Error in fetchRepoCommitActivity", e);
                callback.onError(e);
            }
        }).start();
    }

}
