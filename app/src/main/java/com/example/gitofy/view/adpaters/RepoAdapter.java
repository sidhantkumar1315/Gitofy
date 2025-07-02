package com.example.gitofy.view.adpaters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.gitofy.R;

import org.json.JSONObject;

import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {
    private final List<JSONObject> repoList;

    public RepoAdapter(List<JSONObject> repoList) {
        this.repoList = repoList;
    }

    public static class RepoViewHolder extends RecyclerView.ViewHolder {
        TextView repoName, repoDesc;

        public RepoViewHolder(View itemView) {
            super(itemView);
            repoName = itemView.findViewById(R.id.repoName);
            repoDesc = itemView.findViewById(R.id.repoDesc);
        }
    }

    @Override
    public RepoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_repo, parent, false);
        return new RepoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RepoViewHolder holder, int position) {
        JSONObject repo = repoList.get(position);
        holder.repoName.setText(repo.optString("name"));
        holder.repoDesc.setText(repo.optString("description", "No description"));
    }

    @Override
    public int getItemCount() {
        return repoList.size();
    }
}
