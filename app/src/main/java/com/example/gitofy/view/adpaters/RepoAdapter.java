package com.example.gitofy.view.adpaters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gitofy.R;

import org.json.JSONObject;

import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {
    private final List<JSONObject> repoList;

    public RepoAdapter(List<JSONObject> repoList) {
        this.repoList = repoList;
    }

    public static class RepoViewHolder extends RecyclerView.ViewHolder {
        TextView repoName, repoOwner;
        ImageView ownerAvatar;

        public RepoViewHolder(View itemView) {
            super(itemView);
            repoName = itemView.findViewById(R.id.repoName);
            repoOwner = itemView.findViewById(R.id.repoOwner);
            ownerAvatar = itemView.findViewById(R.id.ownerAvatar);

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
        try {
            String repoName = repo.getString("name");

            JSONObject owner = repo.getJSONObject("owner");
            String ownerLogin = owner.getString("login");

            String avatarUrl = owner.getString("avatar_url");

            holder.repoName.setText(repoName);
            holder.repoOwner.setText("Owner: " + ownerLogin );

            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar_placeholder) // Optional placeholder
                    .circleCrop() // Optional: make it circular
                    .into(holder.ownerAvatar);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return repoList.size();
    }
}
