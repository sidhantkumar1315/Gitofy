package com.example.gitofy.view.adpaters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.RepoDEtailsActivity;

import org.json.JSONObject;

import java.util.List;

public class RepoAdapter extends RecyclerView.Adapter<RepoAdapter.RepoViewHolder> {
    private final List<JSONObject> repoList;

    public RepoAdapter(List<JSONObject> repoList) {
        this.repoList = repoList;
    }

    public static class RepoViewHolder extends RecyclerView.ViewHolder {
        TextView repoName, repoOwner, repoLanguage;
        ImageView ownerAvatar;

        public RepoViewHolder(View itemView) {
            super(itemView);
            repoName = itemView.findViewById(R.id.repoName);
            repoOwner = itemView.findViewById(R.id.repoOwner);
            ownerAvatar = itemView.findViewById(R.id.ownerAvatar);
            repoLanguage = itemView.findViewById(R.id.repoLanguage);
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
            String language = !repo.isNull("language") ? repo.getString("language"): "";


            String avatarUrl = owner.getString("avatar_url");

            holder.repoName.setText(repoName);
            holder.repoOwner.setText( ownerLogin );
            holder.repoLanguage.setText(language);

            Glide.with(holder.itemView.getContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.avatar_placeholder) // Optional placeholder
                    .circleCrop() // Optional: make it circular
                    .into(holder.ownerAvatar);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(holder.itemView.getContext(), RepoDEtailsActivity.class);
                intent.putExtra("name", repoName);
                intent.putExtra("owner", ownerLogin);
                intent.putExtra("avatar", avatarUrl);
                holder.itemView.getContext().startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return repoList.size();
    }
}
