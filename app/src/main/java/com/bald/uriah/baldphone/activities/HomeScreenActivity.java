/*
 * Copyright 2019 Uriah Shaul Mandel
 * Copyright 2026 Zenolabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bald.uriah.baldphone.activities;


import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.splashscreen.SplashScreen;

import app.baldphone.neo.extensions.SurfaceWorkaroundKt;
import app.baldphone.neo.extensions.ViewExtensions;
import app.baldphone.neo.launcher.apps.data.AppsRepository;
import app.baldphone.neo.launcher.topbar.TopBarView;
import app.baldphone.neo.permissions.PermissionManager;
import app.baldphone.neo.utils.HomeAppUtils;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.adapters.BaldPagerAdapter;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.D;
import com.bald.uriah.baldphone.utils.S;
import com.bald.uriah.baldphone.views.ViewPagerHolder;

public class HomeScreenActivity extends BaldActivity {
    private static final String TAG = HomeScreenActivity.class.getSimpleName();

    public BaldPagerAdapter baldPagerAdapter;

    private SharedPreferences sharedPreferences;
    private BaldPrefsUtils baldPrefsUtils;
    private ViewPagerHolder viewPagerHolder;

    private final Handler handler = new Handler();

    public enum LaunchSource {
        HOME, LAUNCHER, UNKNOWN
    }

    private final Runnable surfaceCheckRunnable = () ->
        SurfaceWorkaroundKt.ensureValidSurface(this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        final LaunchSource launchSource = detectLaunchSource(getIntent());
        Log.d(TAG, "launchSource: " + launchSource);

        sharedPreferences = BPrefs.get(this);
        if (!sharedPreferences.getBoolean(BPrefs.AFTER_TUTORIAL_KEY, false) && !testing) {
            startActivity(new Intent(this, TutorialActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.home_screen);
        viewPagerHolder = findViewById(R.id.view_pager_holder);

        View topBar = findViewById(R.id.top_bar);
        if (getResources().getConfiguration().orientation != android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            ViewExtensions.applyTopBarInsets(topBar);
        }
        setupTopBarButtons();

        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        viewPagerHandler();

        AppsRepository.getPinnedAppsLiveData().observe(this, pinnedApps -> {
            if (!isFinishing() && !isDestroyed()) {
                updateViewPager(false, false);
            }
        });
    }

    /**
     * Fills the top bar with whatever has been chosen for it.
     * <p>
     * Called again on resume, since the choice may have been changed in the settings while this
     * screen sat behind them.
     */
    private void setupTopBarButtons() {
        final TopBarView topBar = findViewById(R.id.top_bar);
        topBar.bind(this, onGranted -> {
            requestFlashlightPermission(onGranted);
            return kotlin.Unit.INSTANCE;
        });
    }

    private void requestFlashlightPermission(@NonNull Runnable onGranted) {
        PermissionManager.checkOrRequest(this, PermissionManager.CAMERA, result -> {
            if (result == PermissionManager.GRANTED) {
                onGranted.run();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "onStart");
    }

    @Override
    protected void onResume() { // remember to change in Page1EditorActivity.java too!
        super.onResume();
        Log.v(TAG, "onResume");

        if (baldPrefsUtils.hasChanged(this)) {
            viewPagerHolder.getViewPager().removeAllViews();//android auto saves fragments, not good for us in this case
            this.recreate();
            return;
        }
        // What the bar holds may have been changed in the settings while this screen waited
        // behind them, and the bar is built from that choice rather than from a layout.
        setupTopBarButtons();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        getWindow().getDecorView().removeCallbacks(surfaceCheckRunnable);
        getWindow().getDecorView().postDelayed(surfaceCheckRunnable, 500);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        handler.removeCallbacksAndMessages(null);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Starts the view pager - being called only in {@link #onCreate(Bundle)}
     */
    private void viewPagerHandler() {
        baldPagerAdapter = new BaldPagerAdapter(this);
        viewPagerHolder.setViewPagerAdapter(baldPagerAdapter);
        viewPagerHolder.setCurrentItem(baldPagerAdapter.startingPage);
    }

    /**
     * Updates {@link HomeScreenActivity#baldPagerAdapter} apps
     * Sets the page to {@link BaldPagerAdapter#startingPage}
     */
    private void updateViewPager(boolean animate, boolean resetToHome) {
        baldPagerAdapter.obtainAppList();
        if (resetToHome)
            viewPagerHolder.getViewPager().setCurrentItem(baldPagerAdapter.startingPage, animate);
        viewPagerHolder.onDataChanged();
    }

    @Override
    public void startActivity(Intent intent, @Nullable Bundle options) {
        try {
            super.startActivity(intent, options);
        } catch (Exception e) {
            Log.e(TAG, S.str(e.getMessage()));
            e.printStackTrace();
            BaldToast.error(this);
        }
    }

    @Override
    protected void onBackPressedCompat() {
        Log.v(TAG, "onBackPressed");
        if (vibrator != null)
            vibrator.vibrate(D.vibetime);

        if (viewPagerHolder.getViewPager().getCurrentItem() != baldPagerAdapter.startingPage) {
            viewPagerHolder.setCurrentItem(baldPagerAdapter.startingPage);
            // updateViewPager();
        } else {
            if (!HomeAppUtils.isDefaultLauncher(this)) {
                super.onBackPressedCompat();
            }
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // update the activity's intent
        final LaunchSource launchSource = detectLaunchSource(intent);
        Log.d(TAG, "onNewIntent: launchSource=" + launchSource);
        if (launchSource == LaunchSource.HOME) {
            updateViewPager(true, true);
        }
    }

    /**
     * Detects how the app was launched based on the provided intent.
     */
    @NonNull
    private LaunchSource detectLaunchSource(@Nullable Intent intent) {
        if (intent == null) {
            return LaunchSource.UNKNOWN;
        }

        // A Home button press
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            return LaunchSource.HOME;
        }

        // Launching by an app icon
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            return LaunchSource.LAUNCHER;
        }

        return LaunchSource.UNKNOWN;
    }
}
