package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.gitofy.R;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HeatmapActivity extends AppCompatActivity {
    private static final String TAG = "HeatmapActivity";

    private LinearLayout heatmapContainer;
    private ProgressBar progressBar;
    private TextView totalCommitsText, currentStreakText, longestStreakText;
    private TextView contributionTitle;
    private Spinner yearSpinner;
    private Map<String, Integer> commitCountMap = new HashMap<>();
    private int maxCommitsPerDay = 0;
    private int selectedYear;
    private String username;
    private int userJoinYear = 2008; // Default, will be updated

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heatmap);

        selectedYear = Calendar.getInstance().get(Calendar.YEAR);

        setupToolbar();
        initializeViews();
        loadUserAndCommitData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Contribution Activity");
        }
    }

    private void initializeViews() {
        heatmapContainer = findViewById(R.id.heatmapContainer);
        progressBar = findViewById(R.id.progressBar);
        totalCommitsText = findViewById(R.id.totalCommitsText);
        currentStreakText = findViewById(R.id.currentStreakText);
        longestStreakText = findViewById(R.id.longestStreakText);
        contributionTitle = findViewById(R.id.contributionTitle);
        yearSpinner = findViewById(R.id.yearSpinner);
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // Add years from current year back to user's join year
        for (int year = currentYear; year >= userJoinYear; year--) {
            years.add(String.valueOf(year));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(adapter);

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedYear = Integer.parseInt(years.get(position));
                if (username != null) {
                    loadCommitDataForYear();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadUserAndCommitData() {
        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchUser(token, new GitHubService.UserCallback() {
            @Override
            public void onSuccess(JSONObject user) {
                username = user.optString("login");

                // Get user's join year
                String createdAt = user.optString("created_at", "");
                if (!createdAt.isEmpty()) {
                    try {
                        Calendar cal = Calendar.getInstance();
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                        cal.setTime(format.parse(createdAt));
                        userJoinYear = cal.get(Calendar.YEAR);
                    } catch (Exception e) {
                        userJoinYear = 2008;
                    }
                }

                runOnUiThread(() -> {
                    setupYearSpinner();
                    loadCommitDataForYear();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(HeatmapActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadCommitDataForYear() {
        if (username == null) return;

        progressBar.setVisibility(View.VISIBLE);
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchYearlyContributions(token, username, selectedYear,
                new GitHubService.ContributionsCallback() {
                    @Override
                    public void onSuccess(Map<String, Integer> contributions, int totalContributions) {
                        runOnUiThread(() -> {
                            commitCountMap = contributions;
                            calculateMaxCommits();
                            generateYearHeatmap();
                            updateStats(totalContributions);
                            progressBar.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(HeatmapActivity.this,
                                    "Failed to load contribution data", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                });
    }

    private void calculateMaxCommits() {
        maxCommitsPerDay = 0;
        for (int count : commitCountMap.values()) {
            if (count > maxCommitsPerDay) {
                maxCommitsPerDay = count;
            }
        }
        if (maxCommitsPerDay == 0) maxCommitsPerDay = 1;
    }

    private void generateYearHeatmap() {
        heatmapContainer.removeAllViews();

        // Create horizontal scroll view to fit all months
        HorizontalScrollView scrollView = new HorizontalScrollView(this);
        scrollView.setHorizontalScrollBarEnabled(false);

        // Create main container
        LinearLayout mainContainer = new LinearLayout(this);
        mainContainer.setOrientation(LinearLayout.VERTICAL);

        // Add month labels
        LinearLayout monthsRow = new LinearLayout(this);
        monthsRow.setOrientation(LinearLayout.HORIZONTAL);
        monthsRow.setPadding(dpToPx(25), 0, 0, dpToPx(2));

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        // Calculate width for each month (approximately 4.3 weeks per month)
        for (String month : months) {
            TextView monthLabel = new TextView(this);
            monthLabel.setText(month);
            monthLabel.setTextSize(8);
            monthLabel.setTextColor(getResources().getColor(android.R.color.darker_gray));
            monthLabel.setWidth(dpToPx(28));
            monthsRow.addView(monthLabel);
        }
        mainContainer.addView(monthsRow);

        // Create container for days and grid
        LinearLayout gridContainer = new LinearLayout(this);
        gridContainer.setOrientation(LinearLayout.HORIZONTAL);

        // Add day labels
        LinearLayout daysColumn = new LinearLayout(this);
        daysColumn.setOrientation(LinearLayout.VERTICAL);
        daysColumn.setPadding(0, 0, dpToPx(2), 0);

        String[] days = {"", "Mon", "", "Wed", "", "Fri", ""};
        for (String day : days) {
            TextView dayLabel = new TextView(this);
            dayLabel.setText(day);
            dayLabel.setTextSize(8);
            dayLabel.setTextColor(getResources().getColor(android.R.color.darker_gray));
            dayLabel.setHeight(dpToPx(9));
            daysColumn.addView(dayLabel);
        }
        gridContainer.addView(daysColumn);

        // Create the grid
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(53);
        grid.setRowCount(7);

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, Calendar.JANUARY, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Calculate first day of year position
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int totalDays = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

        // Fill the grid
        int currentDay = 1;
        for (int week = 0; week < 53; week++) {
            for (int day = 0; day < 7; day++) {
                View cell;

                if (week == 0 && day < firstDayOfWeek) {
                    // Empty cell before year starts
                    cell = new View(this);
                } else if (currentDay <= totalDays) {
                    // Create cell for this day
                    calendar.set(selectedYear, Calendar.JANUARY, 1);
                    calendar.add(Calendar.DAY_OF_YEAR, currentDay - 1);
                    String date = dateFormat.format(calendar.getTime());
                    cell = createDayCell(date);
                    currentDay++;
                } else {
                    // Empty cell after year ends
                    cell = new View(this);
                }

                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.rowSpec = GridLayout.spec(day);
                params.columnSpec = GridLayout.spec(week);
                params.width = dpToPx(7);  // Reduced from 9 to 7
                params.height = dpToPx(7); // Reduced from 9 to 7
                params.setMargins(dpToPx(1), dpToPx(1), dpToPx(1), dpToPx(1));
                cell.setLayoutParams(params);
                grid.addView(cell);
            }
        }

        gridContainer.addView(grid);
        mainContainer.addView(gridContainer);
        scrollView.addView(mainContainer);
        heatmapContainer.addView(scrollView);
    }

    private View createDayCell(String date) {
        View cell = new View(this);
        int commitCount = commitCountMap.getOrDefault(date, 0);

        cell.setBackgroundResource(R.drawable.heatmap_cell_background);
        cell.getBackground().setTint(getColorForCommitCount(commitCount));

        cell.setOnClickListener(v -> {
            SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            try {
                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String formattedDate = displayFormat.format(parseFormat.parse(date));
                String message = formattedDate + "\n" + commitCount + " contributions";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, date + ": " + commitCount + " contributions", Toast.LENGTH_SHORT).show();
            }
        });

        return cell;
    }

    private int getColorForCommitCount(int count) {
        if (count == 0) {
            return Color.parseColor("#EBEDF0");
        } else if (count == 1) {
            return Color.parseColor("#9BE9A8");
        } else if (count < 5) {
            return Color.parseColor("#40C463");
        } else if (count < 10) {
            return Color.parseColor("#30A14E");
        } else {
            return Color.parseColor("#216E39");
        }
    }

    private void updateStats(int totalContributions) {
        contributionTitle.setText(totalContributions + " contributions in " + selectedYear);

        // Calculate streaks
        int currentStreak = calculateCurrentStreak();
        int longestStreak = calculateLongestStreak();

        totalCommitsText.setText(String.valueOf(totalContributions));
        currentStreakText.setText(currentStreak + " days");
        longestStreakText.setText(longestStreak + " days");
    }

    private int calculateCurrentStreak() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int streak = 0;
        while (true) {
            String date = dateFormat.format(calendar.getTime());
            if (commitCountMap.getOrDefault(date, 0) > 0) {
                streak++;
                calendar.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateLongestStreak() {
        int longest = 0;
        int current = 0;

        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, Calendar.JANUARY, 1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        int daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR);

        for (int i = 0; i < daysInYear; i++) {
            String date = dateFormat.format(calendar.getTime());
            if (commitCountMap.getOrDefault(date, 0) > 0) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }

        return longest;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}