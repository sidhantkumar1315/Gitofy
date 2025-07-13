package com.example.gitofy.view.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.example.gitofy.R;
import com.example.gitofy.view.fragments.MoreFragment;
import com.example.gitofy.view.fragments.RecentCommitsFragment;
import com.example.gitofy.view.fragments.ReposFragment;
import com.example.gitofy.view.fragments.WelcomeFragment;
import com.example.gitofy.view.util.GitHubService;
import org.json.JSONObject;

public class HomeActivity extends AppCompatActivity {

    private TextView userNameText;
    private LinearLayout navRepos, navRecent, navWatched, navMore;
    private LinearLayout currentSelected = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        userNameText = findViewById(R.id.userName);
        navRepos = findViewById(R.id.nav_repos);
        navRecent = findViewById(R.id.nav_recent);
        navWatched = findViewById(R.id.nav_watched);
        navMore = findViewById(R.id.nav_more);

        setupBottomNavigation();
        loadUserInfo();

        // Load welcome fragment by default (no selection)
        loadFragment(new WelcomeFragment());
    }

    private void setupBottomNavigation() {
        navRepos.setOnClickListener(v -> {
            selectNavItem(navRepos);
            loadFragment(new ReposFragment());
        });

        navRecent.setOnClickListener(v -> {
            selectNavItem(navRecent);
            loadFragment(new RecentCommitsFragment());
        });

        navWatched.setOnClickListener(v -> {
            selectNavItem(navWatched);
            // TODO: Implement WatchedFragment
            loadFragment(new WelcomeFragment());
        });

        navMore.setOnClickListener(v -> {
            selectNavItem(navMore);
            loadFragment(new MoreFragment());
        });
    }

    private void selectNavItem(LinearLayout selected) {
        // Reset all items
        resetNavItem(navRepos);
        resetNavItem(navRecent);
        resetNavItem(navWatched);
        resetNavItem(navMore);

        // Highlight selected item
        selected.setSelected(true);
        ImageView icon = (ImageView) selected.getChildAt(0);
        TextView text = (TextView) selected.getChildAt(1);

        int primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);
        icon.setColorFilter(primaryColor);
        text.setTextColor(primaryColor);

        currentSelected = selected;
    }

    private void resetNavItem(LinearLayout item) {
        item.setSelected(false);
        ImageView icon = (ImageView) item.getChildAt(0);
        TextView text = (TextView) item.getChildAt(1);

        int grayColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        icon.setColorFilter(grayColor);
        text.setTextColor(grayColor);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    private void loadUserInfo() {
        SharedPreferences prefs = getSharedPreferences("auth", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        GitHubService gitHubService = new GitHubService();
        gitHubService.fetchUser(token, new GitHubService.UserCallback() {
            @Override
            public void onSuccess(JSONObject user) {
                runOnUiThread(() -> {
                    String name = user.optString("name", user.optString("login"));
                    userNameText.setText(name);
                });
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}