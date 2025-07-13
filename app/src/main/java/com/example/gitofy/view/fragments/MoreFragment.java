package com.example.gitofy.view.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.example.gitofy.R;
import com.example.gitofy.view.activities.HeatmapActivity;

public class MoreFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);

        setupFeatureButtons(view);

        return view;
    }

    private void setupFeatureButtons(View view) {
        CardView heatmapCard = view.findViewById(R.id.card_heatmap);
        heatmapCard.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), HeatmapActivity.class);
            startActivity(intent);
        });
    }
}