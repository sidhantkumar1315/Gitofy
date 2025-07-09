package com.example.gitofy.view.util;

import android.content.Context;
import android.graphics.Color;
import androidx.core.content.ContextCompat;
import com.example.gitofy.R;
import java.util.HashMap;
import java.util.Map;

public class BranchColorManager {
    private static BranchColorManager instance;
    private final Map<String, Integer> branchColorMap = new HashMap<>();
    private final int[] colorPalette;
    private int colorIndex = 0;

    private BranchColorManager(Context context) {
        // Define a nice color palette
        colorPalette = new int[]{
                ContextCompat.getColor(context, R.color.branch_color_1),
                ContextCompat.getColor(context, R.color.branch_color_2),
                ContextCompat.getColor(context, R.color.branch_color_3),
                ContextCompat.getColor(context, R.color.branch_color_4),
                ContextCompat.getColor(context, R.color.branch_color_5),
                ContextCompat.getColor(context, R.color.branch_color_6),
                ContextCompat.getColor(context, R.color.branch_color_7),
                ContextCompat.getColor(context, R.color.branch_color_8),
                ContextCompat.getColor(context, R.color.branch_color_9),
                ContextCompat.getColor(context, R.color.branch_color_10)
        };
    }

    public static BranchColorManager getInstance(Context context) {
        if (instance == null) {
            instance = new BranchColorManager(context.getApplicationContext());
        }
        return instance;
    }

    public int getColorForBranch(String repoName, String branchName) {
        String key = repoName + "/" + branchName;

        // Special colors for main/master branches
        if (branchName.equals("main") || branchName.equals("master")) {
            return colorPalette[0]; // Always use first color for main/master
        }

        // Check if we already assigned a color to this branch
        if (branchColorMap.containsKey(key)) {
            return branchColorMap.get(key);
        }

        // Assign a new color
        int color = colorPalette[(colorIndex + 1) % (colorPalette.length - 1) + 1]; // Skip first color (reserved for main)
        branchColorMap.put(key, color);
        colorIndex++;

        return color;
    }

    public void clearColors() {
        branchColorMap.clear();
        colorIndex = 0;
    }
}