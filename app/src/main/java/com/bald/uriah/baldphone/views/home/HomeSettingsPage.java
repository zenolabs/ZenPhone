/*
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

package com.bald.uriah.baldphone.views.home;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import app.baldphone.neo.activities.FeedbackActivity;
import app.baldphone.neo.settings.SettingId;
import app.baldphone.neo.settings.SettingsAdapter;
import app.baldphone.neo.settings.SettingsMenu;
import app.baldphone.neo.settings.ui.SettingsActivity;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.HomeScreenActivity;

/**
 * The settings menu, one swipe to the left of the tiles.
 *
 * It took the place of a page of eight fixed buttons - settings, internet, maps, photos,
 * videos, medication, apps, alarms - every one of which is now a tile that can be put on the
 * home screen or left off it. Seven of the eight were therefore saying twice what the tiles
 * already said, and the eighth was the settings themselves, which could not be a tile alone:
 * a tile can be removed, and removing the way into the settings takes with it the way to put
 * it back.
 *
 * Being a page rather than a tile is the point. No arrangement of the home screen can reach
 * it, so no arrangement can lock anyone out.
 */
public class HomeSettingsPage extends HomeView {
    public static final String TAG = HomeSettingsPage.class.getSimpleName();

    public HomeSettingsPage(@NonNull HomeScreenActivity homeScreen) {
        super(homeScreen, homeScreen);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
        final RecyclerView list =
                (RecyclerView) inflater.inflate(R.layout.fragment_home_settings_page, container, false);

        list.setAdapter(new SettingsAdapter(SettingsMenu.INSTANCE.getITEMS(), id -> {
            open(id);
            return kotlin.Unit.INSTANCE;
        }));

        final DividerItemDecoration divider =
                new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        final android.graphics.drawable.Drawable line =
                ContextCompat.getDrawable(getContext(), R.drawable.ll_divider);
        if (line != null) {
            divider.setDrawable(line);
            list.addItemDecoration(divider);
        }

        return list;
    }

    /**
     * Opens a section straight away rather than the menu with the section waiting to be
     * chosen: the row has already been chosen, and asking again would be asking twice.
     */
    private void open(SettingId id) {
        if (id instanceof SettingId.Feedback) {
            // The one entry that lives outside the settings' own navigation, so it cannot be
            // reached by asking the settings to go there.
            homeScreen.startActivity(new Intent(homeScreen, FeedbackActivity.class));
            return;
        }
        homeScreen.startActivity(
                new Intent(homeScreen, SettingsActivity.class)
                        .putExtra(SettingsActivity.EXTRA_SECTION, id.getKey()));
    }
}
