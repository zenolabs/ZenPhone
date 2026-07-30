/*
 * Copyright 2019 Uriah Shaul Mandel
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

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import app.baldphone.neo.launcher.apps.ui.AppsActivity;
import app.baldphone.neo.launcher.home.HomeTilePickerActivity;
import app.baldphone.neo.settings.SettingsRows;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.views.home.HomePage1;

public class Page1EditorActivity extends BaldActivity {
    public BaldPrefsUtils baldPrefsUtils;
    private HomePage1 homePage1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_1_editor);
        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        homePage1 = findViewById(R.id.home_page_1);

        // INSTANCE because SettingsRows is a Kotlin object and this activity is still Java.
        SettingsRows.INSTANCE.bindAction(
                findViewById(R.id.row_choose_tiles),
                R.string.choose_tiles,
                R.drawable.ic_tabler_layout_grid,
                R.string.choose_tiles_subtext,
                () -> {
                    startActivity(new Intent(this, HomeTilePickerActivity.class));
                    return kotlin.Unit.INSTANCE;
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (baldPrefsUtils.hasChanged(this)) {
            recreate();
            return;
        }
        // Tiles may have been added or removed next door, and the grid is drawn from the saved
        // order, so it is asked to read it again rather than being told what changed.
        if (homePage1 != null) {
            homePage1.refreshTiles();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.hasExtra(AppsActivity.CHOOSE_MODE) && data.getComponent() != null) {
            BPrefs.get(this).edit().putString(data.getStringExtra(AppsActivity.CHOOSE_MODE), data.getComponent().flattenToString()).apply();
        }
    }

    @Override
    protected int requiredPermissions() {
        return PERMISSION_NONE;
    }
}
