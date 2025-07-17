package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.gitofy.R;
import com.example.gitofy.view.util.CommitMarkerView;
import com.example.gitofy.view.util.GitHubService;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RepoStatsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private LinearLayout statsContainer;

    // Overview stats
    private TextView totalReposText;
    private TextView totalStarsText;
    private TextView totalForksText;
    private TextView totalSizeText;

    // Charts
    private BarChart repoSizeChart;
    private PieChart languageChart;
    private BarChart starsChart;
    private LineChart commitActivityChart;

    private GitHubService gitHubService;
    private String token;

    // Storing full repo names
    private String[] fullRepoNames;
    private String[] fullStarRepoNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repo_stats);

        initializeViews();
        setupCharts();

        gitHubService = new GitHubService();
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        token = prefs.getString("token", null);

        if (token != null) {
            loadRepositoryStats();
        } else {
            Toast.makeText(this, "Please authenticate first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        progressBar = findViewById(R.id.progressBar);
        statsContainer = findViewById(R.id.statsContainer);

        // Overview stats
        totalReposText = findViewById(R.id.totalReposText);
        totalStarsText = findViewById(R.id.totalStarsText);
        totalForksText = findViewById(R.id.totalForksText);
        totalSizeText = findViewById(R.id.totalSizeText);

        // Charts
        repoSizeChart = findViewById(R.id.repoSizeChart);
        languageChart = findViewById(R.id.languageChart);
        starsChart = findViewById(R.id.starsChart);
        commitActivityChart = findViewById(R.id.commitActivityChart);

        // Back button
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setupCharts() {
        // Setup Repository Size Chart
        repoSizeChart.getDescription().setEnabled(false);
        repoSizeChart.setDrawValueAboveBar(true);
        repoSizeChart.setPinchZoom(false);
        repoSizeChart.setDrawGridBackground(false);

        XAxis xAxis = repoSizeChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = repoSizeChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);

        repoSizeChart.getAxisRight().setEnabled(false);
        repoSizeChart.getLegend().setEnabled(false);

        // Setup Language Distribution Chart
        languageChart.getDescription().setEnabled(false);
        languageChart.setUsePercentValues(true);
        languageChart.setDrawHoleEnabled(true);
        languageChart.setHoleColor(Color.WHITE);
        languageChart.setTransparentCircleColor(Color.WHITE);
        languageChart.setTransparentCircleAlpha(110);
        languageChart.setHoleRadius(58f);
        languageChart.setTransparentCircleRadius(61f);
        languageChart.setDrawCenterText(true);
        languageChart.setCenterText("Languages");
        languageChart.setRotationEnabled(true);
        languageChart.setHighlightPerTapEnabled(true);

        // Setup Stars Chart
        starsChart.getDescription().setEnabled(false);
        starsChart.setDrawValueAboveBar(true);
        starsChart.setPinchZoom(false);
        starsChart.setDrawGridBackground(false);

        XAxis starsXAxis = starsChart.getXAxis();
        starsXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        starsXAxis.setDrawGridLines(false);
        starsXAxis.setGranularity(1f);

        starsChart.getAxisLeft().setDrawGridLines(false);
        starsChart.getAxisLeft().setAxisMinimum(0f);
        starsChart.getAxisRight().setEnabled(false);
        starsChart.getLegend().setEnabled(false);

        // Setup Commit Activity Chart
        commitActivityChart.getDescription().setEnabled(false);
        commitActivityChart.setTouchEnabled(true);
        commitActivityChart.setDragEnabled(true);
        commitActivityChart.setScaleEnabled(true);
        commitActivityChart.setPinchZoom(true);
        commitActivityChart.setDrawGridBackground(false);
        commitActivityChart.getLegend().setEnabled(false);
    }

    private void loadRepositoryStats() {
        progressBar.setVisibility(View.VISIBLE);
        statsContainer.setVisibility(View.GONE);

        gitHubService.fetchRepoStats(token, new GitHubService.RepoStatsCallback() {
            @Override
            public void onSuccess(JSONArray repos, Map<String, Object> aggregateStats) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statsContainer.setVisibility(View.VISIBLE);
                    displayStats(repos, aggregateStats);
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(RepoStatsActivity.this,
                            "Failed to load statistics: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void displayStats(JSONArray repos, Map<String, Object> aggregateStats) {
        try {
            // Display overview stats
            int totalRepos = (int) aggregateStats.get("totalRepos");
            int totalStars = (int) aggregateStats.get("totalStars");
            int totalForks = (int) aggregateStats.get("totalForks");
            long totalSize = (long) aggregateStats.get("totalSize");

            totalReposText.setText(String.valueOf(totalRepos));
            totalStarsText.setText(String.valueOf(totalStars));
            totalForksText.setText(String.valueOf(totalForks));
            totalSizeText.setText(formatSize(totalSize));

            // Prepare data for charts
            List<BarEntry> sizeEntries = new ArrayList<>();
            List<BarEntry> starsEntries = new ArrayList<>();
            List<String> repoNames = new ArrayList<>();
            List<String> truncatedRepoNames = new ArrayList<>();
            Map<String, Integer> languageMap = new HashMap<>();

            // Process top 10 repos by size and stars
            List<JSONObject> repoList = new ArrayList<>();
            for (int i = 0; i < repos.length(); i++) {
                repoList.add(repos.getJSONObject(i));
            }

            // Sort by size for size chart
            Collections.sort(repoList, (a, b) ->
                    Integer.compare(b.optInt("size", 0), a.optInt("size", 0)));

            for (int i = 0; i < Math.min(10, repoList.size()); i++) {
                JSONObject repo = repoList.get(i);
                String name = repo.getString("name");
                float size = repo.optInt("size", 0) / 1024f; // Convert to MB

                sizeEntries.add(new BarEntry(i, size));
                repoNames.add(name);
                truncatedRepoNames.add(truncateRepoName(name, 10));
            }

            // Store full names
            fullRepoNames = repoNames.toArray(new String[0]);

            // Sort by stars for stars chart
            Collections.sort(repoList, (a, b) ->
                    Integer.compare(b.optInt("stargazers_count", 0),
                            a.optInt("stargazers_count", 0)));

            List<String> starRepoNames = new ArrayList<>();
            List<String> truncatedStarRepoNames = new ArrayList<>();
            for (int i = 0; i < Math.min(10, repoList.size()); i++) {
                JSONObject repo = repoList.get(i);
                String name = repo.getString("name");
                int stars = repo.optInt("stargazers_count", 0);

                if (stars > 0) {
                    starsEntries.add(new BarEntry(starRepoNames.size(), stars));
                    starRepoNames.add(name);
                    truncatedStarRepoNames.add(truncateRepoName(name, 10));
                }
            }

            // Store full names
            fullStarRepoNames = starRepoNames.toArray(new String[0]);

            // Aggregate languages
            int nullLanguageCount = 0;
            for (JSONObject repo : repoList) {
                String language = repo.optString("language", "");
                if (language.isEmpty() || language.equals("null")) {
                    nullLanguageCount++;
                } else {
                    languageMap.put(language, languageMap.getOrDefault(language, 0) + 1);
                }
            }

            if (nullLanguageCount > 0) {
                languageMap.put("No Language", nullLanguageCount);
            }

            // Display charts with truncated names
            displaySizeChart(sizeEntries, truncatedRepoNames);
            displayLanguageChart(languageMap);
            displayStarsChart(starsEntries, truncatedStarRepoNames);

            // Load commit activity for the most active repos
            loadCommitActivity(repoList.subList(0, Math.min(5, repoList.size())));

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error displaying statistics", Toast.LENGTH_SHORT).show();
        }
    }

    private String truncateRepoName(String name, int maxLength) {
        if (name.length() <= maxLength) {
            return name;
        }
        return name.substring(0, maxLength - 2) + "..";
    }

    private void displaySizeChart(List<BarEntry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            repoSizeChart.setVisibility(View.GONE);
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Repository Size (MB)");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return new DecimalFormat("#.#").format(value) + " MB";
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        repoSizeChart.setData(data);

        // X-axis configuration
        XAxis xAxis = repoSizeChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-45);
        xAxis.setGranularity(1f);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setTextSize(8f);

        // Increase bottom offset to show labels properly
        repoSizeChart.setExtraBottomOffset(50f);
        repoSizeChart.setVisibleXRangeMaximum(5);
        repoSizeChart.moveViewToX(0);

        // Enable highlighting
        repoSizeChart.setHighlightPerTapEnabled(true);
        repoSizeChart.setHighlightPerDragEnabled(false);

        // Enable horizontal scrolling
        repoSizeChart.setDragEnabled(true);
        repoSizeChart.setScaleXEnabled(true);
        repoSizeChart.setScaleYEnabled(false);

        // Add click listener
        repoSizeChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < fullRepoNames.length) {
                    // Show full name in Toast
                    String fullName = fullRepoNames[index];
                    float size = e.getY();
                    String message = fullName + "\nSize: " + new DecimalFormat("#.#").format(size) + " MB";
                    Toast.makeText(RepoStatsActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });

        repoSizeChart.animateY(1000);
        repoSizeChart.invalidate();
    }

    private void displayLanguageChart(Map<String, Integer> languageMap) {
        if (languageMap.isEmpty()) {
            languageChart.setVisibility(View.GONE);
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : languageMap.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Languages");

        // Custom color palette including a color for "No Language"
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) {
            colors.add(c);
        }
        // Add a custom color for "No Language" if it exists
        if (languageMap.containsKey("No Language")) {
            colors.add(Color.parseColor("#808080")); // Gray color for no language
        }

        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueFormatter(new PercentFormatter(languageChart));

        PieData data = new PieData(dataSet);
        languageChart.setData(data);
        languageChart.animateY(1000);
        languageChart.invalidate();
    }

    private void displayStarsChart(List<BarEntry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            starsChart.setVisibility(View.GONE);
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Stars");
        dataSet.setColor(ContextCompat.getColor(this, R.color.gold));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        starsChart.setData(data);

        // X-axis configuration
        XAxis xAxis = starsChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelRotationAngle(-45);
        xAxis.setGranularity(1f);
        xAxis.setAvoidFirstLastClipping(true);

        // Set text size smaller
        xAxis.setTextSize(8f);

        // Increase bottom offset to show labels properly
        starsChart.setExtraBottomOffset(50f);

        // Set maximum visible range
        starsChart.setVisibleXRangeMaximum(5);
        starsChart.moveViewToX(0);

        // Enable highlighting
        starsChart.setHighlightPerTapEnabled(true);
        starsChart.setHighlightPerDragEnabled(false);

        // Enable horizontal scrolling
        starsChart.setDragEnabled(true);
        starsChart.setScaleXEnabled(true);
        starsChart.setScaleYEnabled(false);

        // Add click listener
        starsChart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                int index = (int) e.getX();
                if (index >= 0 && index < fullStarRepoNames.length) {
                    // Show full name in Toast
                    String fullName = fullStarRepoNames[index];
                    int stars = (int) e.getY();
                    String message = fullName + "\nStars: " + stars;
                    Toast.makeText(RepoStatsActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNothingSelected() {
                // Do nothing
            }
        });

        starsChart.animateY(1000);
        starsChart.invalidate();
    }

    private void loadCommitActivity(List<JSONObject> repos) {
        gitHubService.fetchRepoCommitActivity(token, repos,
                new GitHubService.CommitActivityCallback() {
                    @Override
                    public void onSuccess(Map<String, Integer> commitCountByDate) {
                        runOnUiThread(() -> displayCommitActivity(commitCountByDate));
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> commitActivityChart.setVisibility(View.GONE));
                    }
                });
    }

    private void displayCommitActivity(Map<String, Integer> commitData) {
        if (commitData.isEmpty()) {
            commitActivityChart.setVisibility(View.GONE);
            // Update the parent card to show a message
            View parent = (View)commitActivityChart.getParent();
            if (parent instanceof ViewGroup) {
                TextView noDataText = new TextView(this);
                noDataText.setText("No commit activity in the last 30 days");
                noDataText.setGravity(Gravity.CENTER);
                noDataText.setPadding(16, 16, 16, 16);
                noDataText.setTextColor(Color.GRAY);
                noDataText.setTextSize(16f);
                ((ViewGroup) parent).addView(noDataText);
            }
            return;
        }

        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>(commitData.keySet());
        Collections.sort(dates);

        // Show last 30 days with all dates (including zero commits)
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        List<String> allDates = new ArrayList<>();
        Map<String, Integer> completeData = new HashMap<>();

        // Fill in missing dates with 0 commits
        for (int i = 29; i >= 0; i--) {
            Calendar cal = (Calendar) calendar.clone();
            cal.add(Calendar.DAY_OF_MONTH, -i);
            String dateStr = dateFormat.format(cal.getTime());
            allDates.add(dateStr);
            completeData.put(dateStr, commitData.getOrDefault(dateStr, 0));
        }

        // Create entries for the chart
        for (int i = 0; i < allDates.size(); i++) {
            String date = allDates.get(i);
            entries.add(new Entry(i, completeData.get(date)));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Daily Commits");
        dataSet.setColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleRadius(3f);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.colorPrimary));
        dataSet.setFillAlpha(30);
        dataSet.setDrawValues(false); // Don't show values on the line itself

        // Use cubic lines for smoother appearance
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.2f);

        LineData data = new LineData(dataSet);
        commitActivityChart.setData(data);

        // Configure X-axis
        XAxis xAxis = commitActivityChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#E0E0E0"));
        xAxis.setGridLineWidth(0.5f);
        xAxis.setTextSize(11f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setLabelRotationAngle(-30);

        // Add proper date labels
        xAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < allDates.size()) {
                    try {
                        // Parse the date string and format it nicely
                        String fullDate = allDates.get(index);
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        Date date = inputFormat.parse(fullDate);
                        return dateFormat.format(date); // Returns "Jul 16" format
                    } catch (Exception e) {
                        return "";
                    }
                }
                return "";
            }
        });

        // Set label count to avoid crowding
        xAxis.setLabelCount(7, true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.BLACK);
        xAxis.setAxisLineWidth(1f);

        // Configure Y-axis
        YAxis leftAxis = commitActivityChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E0E0E0"));
        leftAxis.setGridLineWidth(0.5f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setGranularity(1f); // Show integer values only
        leftAxis.setTextSize(11f);
        leftAxis.setTextColor(Color.BLACK);
        leftAxis.setDrawAxisLine(true);
        leftAxis.setAxisLineColor(Color.BLACK);
        leftAxis.setAxisLineWidth(1f);

        // Set a reasonable maximum to avoid too much empty space
        int maxCommits = 0;
        for (Integer count : completeData.values()) {
            maxCommits = Math.max(maxCommits, count);
        }
        leftAxis.setAxisMaximum(Math.max(maxCommits + 2, 5));

        // Add axis label
        leftAxis.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // Disable right Y-axis
        commitActivityChart.getAxisRight().setEnabled(false);

        // Update chart description
        commitActivityChart.getDescription().setEnabled(false);

        // Add a title above the chart
        commitActivityChart.setNoDataText("Loading commit activity...");
        commitActivityChart.setNoDataTextColor(Color.GRAY);

        // Configure legend
        commitActivityChart.getLegend().setEnabled(false);

        // Enable touch interactions
        commitActivityChart.setTouchEnabled(true);
        commitActivityChart.setDragEnabled(true);
        commitActivityChart.setScaleEnabled(false);
        commitActivityChart.setPinchZoom(false);

        // Add marker view for when user taps on a point
        CommitMarkerView markerView = new CommitMarkerView(this, R.layout.marker_view_commit, allDates);
        markerView.setChartView(commitActivityChart);
        commitActivityChart.setMarker(markerView);

        commitActivityChart.setHighlightPerTapEnabled(true);
        commitActivityChart.setHighlightPerDragEnabled(false);

        // Animate the chart
        commitActivityChart.animateX(1500);
        commitActivityChart.invalidate();

        commitActivityChart.setExtraBottomOffset(15f);
        commitActivityChart.setExtraLeftOffset(10f);

        // Calculate and display statistics
        int totalCommits = 0;
        int activeDays = 0;
        for (Integer count : completeData.values()) {
            totalCommits += count;
            if (count > 0) activeDays++;
        }

        // Update the card title to show summary
        View parent = (View)commitActivityChart.getParent();
        if (parent instanceof ViewGroup && parent.getParent() instanceof ViewGroup) {
            ViewGroup cardContent = (ViewGroup) parent;
            TextView titleView = null;

            // Find the title TextView (assuming it's the first TextView in the card)
            for (int i = 0; i < cardContent.getChildCount(); i++) {
                View child = cardContent.getChildAt(i);
                if (child instanceof TextView) {
                    titleView = (TextView) child;
                    break;
                }
            }

            if (titleView != null) {
                String summaryText = String.format(Locale.getDefault(),
                        "Recent Commit Activity\n%d commits on %d days (last 30 days)",
                        totalCommits, activeDays);
                titleView.setText(summaryText);
            }
        }
    }

    private String formatSize(long sizeInKB) {
        if (sizeInKB < 1024) {
            return sizeInKB + " KB";
        } else if (sizeInKB < 1024 * 1024) {
            return String.format("%.1f MB", sizeInKB / 1024.0);
        } else {
            return String.format("%.1f GB", sizeInKB / (1024.0 * 1024.0));
        }
    }
}