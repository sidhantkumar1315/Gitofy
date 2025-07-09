package com.example.gitofy.view.adpaters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class CommitAdapter extends RecyclerView.Adapter<CommitAdapter.CommitViewHolder> {
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

            // Limit message to first line
            if (message.contains("\n")) {
                message = message.substring(0, message.indexOf("\n")) + "...";
            }
            holder.commitMessage.setText(message);

            // Get repo name
            String repoName = commitData.optString("repo_name", "Unknown repo");
            holder.repoName.setText(repoName);

            // Get branch name
            String branchName = commitData.optString("branch", "main");
            holder.branchName.setText(branchName);

            // Get author
            JSONObject author = commit.optJSONObject("author");
            if (author != null) {
                holder.commitAuthor.setText(author.optString("name", "Unknown"));

                // Parse and format date
                String dateStr = author.optString("date");
                if (!dateStr.isEmpty()) {
                    SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    parser.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date date = parser.parse(dateStr);
                    holder.commitTime.setText(dateFormat.format(date));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return commitList.size();
    }
}