package com.example.gitofy.view.adpaters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.FileDiffActivity;
import org.json.JSONObject;
import java.util.List;

public class FileChangeAdapter extends RecyclerView.Adapter<FileChangeAdapter.FileChangeViewHolder> {
    private final List<JSONObject> filesList;

    public FileChangeAdapter(List<JSONObject> filesList) {
        this.filesList = filesList;
    }

    public static class FileChangeViewHolder extends RecyclerView.ViewHolder {
        TextView fileName, fileStatus, changeStats;
        View statusIndicator;

        public FileChangeViewHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            fileStatus = itemView.findViewById(R.id.fileStatus);
            changeStats = itemView.findViewById(R.id.changeStats);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
        }
    }

    @Override
    public FileChangeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file_change, parent, false);
        return new FileChangeViewHolder(view);
    }

    // In FileChangeAdapter.java, update the onBindViewHolder method:

    @Override
    public void onBindViewHolder(FileChangeViewHolder holder, int position) {
        try {
            JSONObject file = filesList.get(position);

            String filename = file.optString("filename", "Unknown file");
            String status = file.optString("status", "modified");
            int additions = file.optInt("additions", 0);
            int deletions = file.optInt("deletions", 0);
            int changes = file.optInt("changes", 0);
            String patch = file.optString("patch", "");

            holder.fileName.setText(filename);
            holder.fileStatus.setText(status.toUpperCase());

            // Show total changes if very large
            if (changes > 1000) {
                holder.changeStats.setText("+" + additions + " -" + deletions + " (" + changes + " total)");
            } else {
                holder.changeStats.setText("+" + additions + " -" + deletions);
            }

            // Set status color
            int color;
            switch (status) {
                case "added":
                    color = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark);
                    break;
                case "removed":
                    color = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark);
                    break;
                case "renamed":
                    color = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_dark);
                    break;
                default:
                    color = ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark);
                    break;
            }
            holder.statusIndicator.setBackgroundColor(color);
            holder.fileStatus.setTextColor(color);

            // Click listener to view diff
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), FileDiffActivity.class);
                intent.putExtra("file_name", filename);
                intent.putExtra("patch", patch);
                intent.putExtra("additions", additions);
                intent.putExtra("deletions", deletions);
                intent.putExtra("status", status);
                intent.putExtra("changes", changes);

                // Add a flag if patch might be truncated
                if (patch.isEmpty() && changes > 0) {
                    intent.putExtra("patch_truncated", true);
                }

                v.getContext().startActivity(intent);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return filesList.size();
    }
}