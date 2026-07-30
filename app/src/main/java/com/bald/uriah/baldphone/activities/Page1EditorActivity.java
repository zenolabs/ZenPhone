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
import android.view.View;

import androidx.annotation.Nullable;

import app.baldphone.neo.launcher.apps.ui.AppsActivity;
import app.baldphone.neo.launcher.home.HomeTilePickerActivity;
import app.baldphone.neo.settings.SettingsRows;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldPrefsUtils;
import com.bald.uriah.baldphone.views.home.HomePage1;

public class Page1EditorActivity extends BaldActivity implements HomePage1.TileDragListener {
    /** How long the removal bar takes to appear or go away. */
    private static final long TARGET_FADE_MS = 150L;
    /** The bar's opacity while a tile is merely being carried about. */
    private static final float TARGET_ALPHA_RESTING = 0.9f;
    /** How much it swells once the tile is over it. */
    private static final float TARGET_SCALE_ARMED = 1.06f;

    public BaldPrefsUtils baldPrefsUtils;
    private HomePage1 homePage1;
    private View removalTarget;
    /** Whether the carried tile is currently over the bar, so the look only changes on crossing. */
    private boolean overTarget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page_1_editor);
        baldPrefsUtils = BaldPrefsUtils.newInstance(this);
        homePage1 = findViewById(R.id.home_page_1);
        removalTarget = findViewById(R.id.tile_removal_target);
        homePage1.setTileDragListener(this);

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
    public void onTileDragStarted(boolean removable) {
        if (!removable) {
            // Nothing to offer: with one tile left there is nothing that could be given up.
            removalTarget.setVisibility(View.GONE);
            return;
        }
        overTarget = false;
        // A fade-out from the previous drag may still be running, and its end action would
        // otherwise hide the bar again a moment after this one raised it.
        removalTarget.animate().cancel();
        removalTarget.animate().withEndAction(null);
        removalTarget.setScaleX(1f);
        removalTarget.setScaleY(1f);
        removalTarget.setAlpha(0f);
        removalTarget.setVisibility(View.VISIBLE);
        removalTarget.animate().alpha(TARGET_ALPHA_RESTING).setDuration(TARGET_FADE_MS);
    }

    @Override
    public void onTileDragMoved(int screenX, int screenY) {
        if (removalTarget.getVisibility() != View.VISIBLE) return;

        final boolean inside = isOverTarget(screenX, screenY);
        // Only on crossing the edge, or every frame of the drag would start an animation.
        if (inside == overTarget) return;
        overTarget = inside;

        final float scale = inside ? TARGET_SCALE_ARMED : 1f;
        removalTarget
                .animate()
                .alpha(inside ? 1f : TARGET_ALPHA_RESTING)
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(TARGET_FADE_MS);
    }

    @Override
    public boolean onTileDropped(int screenX, int screenY) {
        final boolean remove =
                removalTarget.getVisibility() == View.VISIBLE && isOverTarget(screenX, screenY);
        hideRemovalTarget();
        return remove;
    }

    /**
     * Whether a point on the screen falls inside the removal bar.
     * <p>
     * Asked of the bar's position now rather than one remembered from when it appeared, since
     * it grows while armed and its edges move with it.
     */
    private boolean isOverTarget(int screenX, int screenY) {
        if (screenX < 0 || screenY < 0) return false;
        final int[] onScreen = new int[2];
        removalTarget.getLocationOnScreen(onScreen);
        return screenX >= onScreen[0]
                && screenX <= onScreen[0] + removalTarget.getWidth()
                && screenY >= onScreen[1]
                && screenY <= onScreen[1] + removalTarget.getHeight();
    }

    private void hideRemovalTarget() {
        overTarget = false;
        removalTarget
                .animate()
                .alpha(0f)
                .setDuration(TARGET_FADE_MS)
                .withEndAction(() -> removalTarget.setVisibility(View.GONE));
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
