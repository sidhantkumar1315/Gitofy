package com.example.gitofy.view.adpaters;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.IssueDetailActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IssuesAdapter extends RecyclerView.Adapter<IssuesAdapter.IssueViewHolder> {
    private JSONArray issues;
    private String repoOwner;
    private String repoName;

    public IssuesAdapter(JSONArray issues, String repoOwner, String repoName) {
        this.issues = issues;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_issue, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        try {
            JSONObject issue = issues.getJSONObject(position);
            holder.bind(issue, repoOwner, repoName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return issues.length();
    }

    static class IssueViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textNumber;
        TextView textState;
        TextView textCreatedAt;
        TextView textAuthor;

        IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_issue_title);
            textNumber = itemView.findViewById(R.id.text_issue_number);
            textState = itemView.findViewById(R.id.text_issue_state);
            textCreatedAt = itemView.findViewById(R.id.text_created_at);
            textAuthor = itemView.findViewById(R.id.text_author);
        }

        void bind(JSONObject issue, String repoOwner, String repoName) throws JSONException {
            textTitle.setText(issue.getString("title"));
            textNumber.setText("#" + issue.getInt("number"));

            String state = issue.getString("state");
            textState.setText(state.toUpperCase());


            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setCornerRadius(20);

            // Set state color
            if ("open".equals(state)) {
                drawable.setColor(Color.parseColor("#4CAF50")); // Green
                textState.setTextColor(Color.WHITE);
            } else {
                drawable.setColor(Color.parseColor("#F44336")); // Red
                textState.setTextColor(Color.WHITE);
            }

            textState.setBackground(drawable);
            textState.setPadding(16, 4, 16, 4);

            // Format date
            String createdAt = issue.getString("created_at");
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            try {
                Date date = inputFormat.parse(createdAt);
                textCreatedAt.setText(outputFormat.format(date));
            } catch (ParseException e) {
                textCreatedAt.setText(createdAt);
            }

            JSONObject user = issue.getJSONObject("user");
            textAuthor.setText("by " + user.getString("login"));

            // click listener to open issue details
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), IssueDetailActivity.class);
                intent.putExtra("repo_owner", repoOwner);
                intent.putExtra("repo_name", repoName);
                intent.putExtra("issue_number", issue.optInt("number"));
                v.getContext().startActivity(intent);
            });
        }
    }
}