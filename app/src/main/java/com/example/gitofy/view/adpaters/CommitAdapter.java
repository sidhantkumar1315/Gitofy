package com.example.gitofy.view.adpaters;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.CommitDetailActivity;
import com.example.gitofy.view.util.BranchColorManager;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommitAdapter extends RecyclerView.Adapter<CommitAdapter.CommitViewHolder> {
    private static final String TAG = "CommitAdapter";
    private final List<JSONObject> commitList;
    private final SimpleDateFormat dateFormat;

    public CommitAdapter(List<JSONObject> commitList) {
        this.commitList = commitList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault());
    }

    public static class CommitViewHolder extends RecyclerView.ViewHolder {
        TextView commitMessage, repoName, branchName, commitAuthor, commitTime;

        public CommitViewHolder(View itemView) {
            super(itemView);
            commitMessage = itemView.findViewById(R.id.commitMessage);
            repoName = itemView.findViewById(R.id.repoName);
            branchName = itemView.findViewById(R.id.branchName);
            commitAuthor = itemView.findViewById(R.id.commitAuthor);
            commitTime = itemView.findViewById(R.id.commitTime);
        }
    }

    @Override
    public CommitViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_commit, parent, false);
        return new CommitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(CommitViewHolder holder, int position) {
        try {
            JSONObject commitData = commitList.get(position);

            // Get commit info
            JSONObject commit = commitData.getJSONObject("commit");
            String message = commit.optString("message", "No message");
            String fullMessage = message; // Store full message for detail view

            // Limit message to first line
            if (message.contains("\n")) {
                message = message.substring(0, message.indexOf("\n")) + "...";
            }
            holder.commitMessage.setText(message);

            // Get repo name
            String repoName = commitData.optString("repo_name", "Unknown repo");
            holder.repoName.setText(repoName);

            // Get branch name and set color
            String branchName = commitData.optString("branch", "main");
            holder.branchName.setText(branchName);

            // Set dynamic branch color
            BranchColorManager colorManager = BranchColorManager.getInstance(holder.itemView.getContext());
            int branchColor = colorManager.getColorForBranch(repoName, branchName);

            // Update background color
            GradientDrawable drawable = (GradientDrawable) holder.branchName.getBackground();
            if (drawable != null) {
                drawable.setColor(branchColor);
            }

            // Get author
            JSONObject author = commit.optJSONObject("author");
            String authorName = "Unknown";
            String commitDate = "";

            if (author != null) {
                authorName = author.optString("name", "Unknown");
                holder.commitAuthor.setText(authorName);

                // Parse and format date
                commitDate = author.optString("date", "");
                if (!commitDate.isEmpty()) {
                    try {
                        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date date = parser.parse(commitDate);
                        holder.commitTime.setText(dateFormat.format(date));
                    } catch (Exception e) {
                        holder.commitTime.setText(commitDate);
                    }
                }
            }

            // Store final values for click listener
            final String finalAuthorName = authorName;
            final String finalCommitDate = commitDate;
            final String finalFullMessage = fullMessage;

            // Add click listener for commit details
            holder.itemView.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(v.getContext(), CommitDetailActivity.class);

                    // Get the stored owner first
                    String owner = commitData.optString("repo_owner", "");

                    // If owner is not stored, try to extract from URL
                    if (owner.isEmpty()) {
                        String url = commitData.optString("url", "");
                        owner = extractOwnerFromUrl(url);
                    }

                    String sha = commitData.optString("sha", "");

                    intent.putExtra("repo_name", repoName);
                    intent.putExtra("repo_owner", owner);
                    intent.putExtra("commit_sha", sha);
                    intent.putExtra("commit_message", finalFullMessage);
                    intent.putExtra("author_name", finalAuthorName);
                    intent.putExtra("commit_date", finalCommitDate);

                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return commitList.size();
    }

    // Helper method to extract owner from URL
    private String extractOwnerFromUrl(String url) {
        // URL format: https://api.github.com/repos/OWNER/REPO/commits/SHA
        try {
            String[] parts = url.split("/");
            if (parts.length > 4) {
                return parts[4]; // The owner is the 5th element
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}