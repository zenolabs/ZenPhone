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

package com.bald.uriah.baldphone.views.home;

import static app.baldphone.neo.utils.IntentUtilsKt.startActivityWithNewTaskClear;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewTreeLifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import app.baldphone.neo.activities.DialerActivity;
import app.baldphone.neo.battery.BatteryRepository;
import app.baldphone.neo.launcher.ui.BatteryIconView;
import app.baldphone.neo.features.calls.ui.RecentCallsActivity;
import app.baldphone.neo.features.contacts.ui.ContactsActivity;
import app.baldphone.neo.features.gallery.MediaActivity;
import app.baldphone.neo.settings.ui.SettingsActivity;
import app.baldphone.neo.features.notifications.data.NotificationRepository;
import app.baldphone.neo.launcher.apps.AppIconBinder;
import app.baldphone.neo.launcher.apps.data.PredefinedApps;
import app.baldphone.neo.launcher.apps.data.AppsRepository;
import app.baldphone.neo.launcher.apps.data.db.AppEntry;
import app.baldphone.neo.launcher.apps.ui.AppsActivity;
import app.baldphone.neo.launcher.home.HomeTile;
import app.baldphone.neo.launcher.home.HomeTilesAdapter;
import app.baldphone.neo.permissions.PermissionManager;
import app.baldphone.neo.permissions.model.SpecialPermission;
import app.baldphone.neo.services.DeviceLock;
import app.baldphone.neo.ui.dialogs.BaldDialog;
import app.baldphone.neo.utils.messaging.WhatsAppHandler;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.activities.HomeScreenActivity;
import com.bald.uriah.baldphone.activities.Page1EditorActivity;
import com.bald.uriah.baldphone.activities.SOSActivity;
import com.bald.uriah.baldphone.activities.alarms.AlarmsActivity;
import com.bald.uriah.baldphone.activities.pills.PillsActivity;
import com.bald.uriah.baldphone.utils.BDB;
import com.bald.uriah.baldphone.utils.BDialog;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.S;
import com.bald.uriah.baldphone.views.FirstPageAppIcon;

import java.util.Map;
import java.util.Set;

public class HomePage1 extends HomeView {
    public static final String TAG = HomePage1.class.getSimpleName();
    /** Tiles per row. The grid has always been three wide; only the rows varied. */
    private static final int COLUMNS = 3;
    /** How much a tile grows while it is being carried. */
    private static final float DRAG_SCALE = 1.08f;
    private final NotificationRepository repo = NotificationRepository.INSTANCE;
    private Map<AppEntry, FirstPageAppIcon> viewsToApps;
    private RecyclerView tilesGrid;
    private HomeTilesAdapter tilesAdapter;
    /**
     * The view currently showing each tile. Rebuilt as the grid binds, and consulted by the
     * things that need to reach a particular tile - notification badges, mainly - which used to
     * hold a field per button.
     * <p>
     * Assigned in {@link #onCreateView}, not here: HomeView's constructor calls onCreateView,
     * and field initialisers only run once the superclass constructor has returned. A field
     * initialised inline would still be null by the time the grid is built.
     */
    private Map<HomeTile, FirstPageAppIcon> tileViews;
    private View notificationsArea;
    private BatteryIconView homeBattery;
    private TextView homeBatteryPercent;
    private View homeBatteryBlock;
    private boolean homeBatteryBound;
    private SharedPreferences sharedPreferences;
    @Nullable
    private TileDragListener tileDragListener;
    /**
     * Where the carried tile was last seen, in screen coordinates, or -1 before it has moved.
     * <p>
     * Kept because the decision to remove is taken when the tile is let go, and the callback
     * that hears about that is told which tile it was but not where the hand had got to.
     */
    private int lastDragX = -1, lastDragY = -1;

    /**
     * Told where a tile is being carried, so that something outside the grid can offer to take
     * it. The editor uses this to raise its removal bar and to know when a tile has been
     * dropped onto it.
     * <p>
     * Coordinates are on the screen rather than in this view, because the listener's own view
     * is somewhere else in the hierarchy and screen coordinates are the only frame both agree
     * on without either having to know where the other sits.
     */
    public interface TileDragListener {
        /**
         * A tile has been picked up.
         *
         * @param removable whether letting it go could remove it. False when it is the last
         *                  tile left, so that nothing is offered which would then be refused.
         */
        void onTileDragStarted(boolean removable);

        /** The centre of the carried tile has moved. */
        void onTileDragMoved(int screenX, int screenY);

        /**
         * Whether a tile let go at this point would be taken off the grid.
         * <p>
         * A question and nothing more: it is asked twice, once to decide how long the tile
         * should take to settle and once to act on the answer, so it must leave things as it
         * found them.
         */
        boolean isOverRemovalTarget(int screenX, int screenY);

        /** The drag is over, however it ended. Time to put the bar away. */
        void onTileDragEnded();
    }

    public HomePage1(@NonNull Context context) {
        this(context, null);
    }

    /** Used by the layout inflater; the attributes carry the id the editor looks this up by. */
    public HomePage1(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(
                (context instanceof HomeScreenActivity) ? (HomeScreenActivity) context : null,
                (Activity) context,
                attributeSet);
        sharedPreferences = BPrefs.get(activity);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.fragment_home_page1, container, false);
        viewsToApps = new ArrayMap<>();
        tileViews = new ArrayMap<>();

        initViews(view);
        setupGrid();
        return view;
    }

    private void initViews(View rootView) {
        tilesGrid = rootView.findViewById(R.id.tiles_grid);
        notificationsArea = rootView.findViewById(R.id.notifications_area);
        homeBattery = rootView.findViewById(R.id.home_battery);
        homeBatteryPercent = rootView.findViewById(R.id.home_battery_percent);
        homeBatteryBlock = rootView.findViewById(R.id.home_battery_block);
    }

    /**
     * Feeds the battery indicator that sits beside the clock.
     * <p>
     * The icon keeps itself in step through the repository, but the percentage next to it is
     * plain text, so it is filled in here. The description is set on the block rather than on
     * either child, so a screen reader announces "battery, 45 percent" once instead of
     * reading an icon and a number as two separate things.
     */
    private void bindHomeBattery(@NonNull LifecycleOwner owner) {
        // onAttachedToWindow can fire again after a detach; binding twice would leave a second
        // collector running for the lifetime of the launcher.
        if (homeBattery == null || homeBatteryBound) return;
        homeBatteryBound = true;

        homeBattery.observeBatteryState(owner);
        BatteryRepository.get(getContext()).getBatteryLiveData().observe(owner, state -> {
            final Integer percentage = state.getPercentage();
            if (homeBatteryPercent != null) {
                homeBatteryPercent.setText(
                        percentage == null
                                ? ""
                                : getContext().getString(R.string.battery_percentage_short, percentage));
            }
            if (homeBatteryBlock != null) {
                homeBatteryBlock.setContentDescription(state.formatSimpleInfo(getContext()));
            }
        });
    }

    /** Builds the grid and fills it from the saved order. */
    private void setupGrid() {
        if (tilesGrid == null) return;
        tilesAdapter = new HomeTilesAdapter(this::bindTile);
        // A home screen does not scroll: every tile is on screen or it is not there at all. Said
        // here rather than trusted to arithmetic, so a pixel of rounding cannot turn into a drag.
        tilesGrid.setLayoutManager(new GridLayoutManager(getContext(), COLUMNS) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        });
        tilesGrid.setAdapter(tilesAdapter);

        // Tiles are rearranged in the editor and nowhere else. On the home screen itself a tile
        // must stay where it was put, however long it is held.
        if (homeScreen == null) attachTileDragging();

        // The tile height follows the grid's own height, so it is taken once the grid has been
        // laid out and again if that ever changes - rotation, or the fourth row being switched
        // on. A posted runnable would not do: it runs on attach, before the first layout.
        tilesGrid.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        updateTileHeight());

        submitTiles();
    }

    /**
     * Lets a tile be picked up and dropped somewhere else.
     * <p>
     * All four directions, because this is a grid and not a list; swiping is left switched off,
     * as there is nowhere for a tile to be swiped to. The order is written when the tile is let
     * go rather than at every swap: a drag across the grid passes through a dozen arrangements
     * nobody asked to keep, and each one would be a write.
     */
    private void attachTileDragging() {
        final int directions =
                ItemTouchHelper.UP
                        | ItemTouchHelper.DOWN
                        | ItemTouchHelper.LEFT
                        | ItemTouchHelper.RIGHT;

        new ItemTouchHelper(
                        new ItemTouchHelper.SimpleCallback(directions, 0) {
                            @Override
                            public boolean onMove(
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    @NonNull RecyclerView.ViewHolder target) {
                                if (tilesAdapter == null) return false;
                                final int from = viewHolder.getBindingAdapterPosition();
                                final int to = target.getBindingAdapterPosition();
                                if (from == RecyclerView.NO_POSITION
                                        || to == RecyclerView.NO_POSITION) {
                                    return false;
                                }
                                tilesAdapter.moveTile(from, to);
                                return true;
                            }

                            @Override
                            public void onSwiped(
                                    @NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                                // Nothing is swiped away here; the callback insists on the method.
                            }

                            @Override
                            public void onSelectedChanged(
                                    @Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
                                super.onSelectedChanged(viewHolder, actionState);
                                // ItemTouchHelper already raises the tile above its neighbours,
                                // but these are flat blocks of colour and the shadow alone is
                                // easy to miss. Growing it says plainly which one is in hand.
                                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                                        && viewHolder != null) {
                                    scaleTile(viewHolder.itemView, DRAG_SCALE);
                                    onTileLifted();
                                }
                            }

                            @Override
                            public void onChildDraw(
                                    @NonNull Canvas canvas,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX,
                                    float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {
                                super.onChildDraw(
                                        canvas,
                                        recyclerView,
                                        viewHolder,
                                        dX,
                                        dY,
                                        actionState,
                                        isCurrentlyActive);
                                // isCurrentlyActive tells the hand from the animation. Once the
                                // tile is let go it keeps being drawn, on its way back to a
                                // resting place, and those frames arrive before clearView does:
                                // taken as movement they would overwrite where it was released
                                // with where it landed, which is never the removal bar.
                                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG
                                        && isCurrentlyActive) {
                                    reportDragPosition(recyclerView, viewHolder, dX, dY);
                                }
                            }

                            @Override
                            public long getAnimationDuration(
                                    @NonNull RecyclerView recyclerView,
                                    int animationType,
                                    float animateDx,
                                    float animateDy) {
                                // A tile about to be thrown away should not first be seen
                                // gliding back to a place it will not keep. Removal happens in
                                // clearView, and clearView waits for this animation, so the
                                // wait is dropped when there is nothing to wait for.
                                if (isDropOnRemovalTarget()) return 0L;
                                return super.getAnimationDuration(
                                        recyclerView, animationType, animateDx, animateDy);
                            }

                            @Override
                            public void clearView(
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder) {
                                super.clearView(recyclerView, viewHolder);
                                scaleTile(viewHolder.itemView, 1f);
                                onTileDropped(viewHolder.getBindingAdapterPosition());
                            }
                        })
                .attachToRecyclerView(tilesGrid);
    }

    private static void scaleTile(@NonNull View tile, float scale) {
        tile.setScaleX(scale);
        tile.setScaleY(scale);
    }

    /** Hands the grid over to something that wants to know where tiles are being carried. */
    public void setTileDragListener(@Nullable TileDragListener listener) {
        this.tileDragListener = listener;
    }

    private void onTileLifted() {
        lastDragX = -1;
        lastDragY = -1;
        if (tileDragListener == null) return;
        // The last tile cannot be given up: an empty order reads as never configured, and the
        // defaults would come back as though the request had been ignored. Said now, by not
        // offering removal at all, rather than by refusing it once the tile has been dropped.
        final boolean removable = tilesAdapter != null && tilesAdapter.getItemCount() > 1;
        // The bar takes the clock's place, so the clock steps aside - but only when there is
        // going to be a bar. Invisible rather than gone: gone would give its height back to the
        // grid, every tile would be re-measured mid-drag, and the one in hand would jump.
        if (removable) setNotificationsAreaShown(false);
        tileDragListener.onTileDragStarted(removable);
    }

    private void setNotificationsAreaShown(boolean shown) {
        if (notificationsArea != null) {
            notificationsArea.setVisibility(shown ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Works out where the carried tile has got to and passes it on.
     * <p>
     * ItemTouchHelper reports a displacement from where the tile would sit at rest, so the two
     * are added and the result moved into screen coordinates. The tile's centre is used rather
     * than a corner: it is what someone aiming at the removal bar believes they are aiming with.
     */
    private void reportDragPosition(
            @NonNull RecyclerView grid, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY) {
        if (tileDragListener == null) return;
        final View tile = viewHolder.itemView;
        final int[] gridOnScreen = new int[2];
        grid.getLocationOnScreen(gridOnScreen);

        lastDragX = Math.round(gridOnScreen[0] + tile.getLeft() + dX + tile.getWidth() / 2f);
        lastDragY = Math.round(gridOnScreen[1] + tile.getTop() + dY + tile.getHeight() / 2f);
        tileDragListener.onTileDragMoved(lastDragX, lastDragY);
    }

    /**
     * Settles what a finished drag meant: a tile somewhere new, or one tile fewer.
     * <p>
     * The listener is asked even when the tile never moved, because it has a bar on screen that
     * has to come down either way.
     */
    /** Whether the tile, as it stands, would be given up rather than kept. */
    private boolean isDropOnRemovalTarget() {
        return tileDragListener != null
                && tileDragListener.isOverRemovalTarget(lastDragX, lastDragY);
    }

    private void onTileDropped(int position) {
        final boolean remove = isDropOnRemovalTarget();
        // Restored unconditionally: whether it was ever hidden or not, this is where the clock
        // belongs once nothing is in the air.
        setNotificationsAreaShown(true);
        if (tileDragListener != null) {
            tileDragListener.onTileDragEnded();
        }

        if (remove
                && position != RecyclerView.NO_POSITION
                && tilesAdapter != null
                && tilesAdapter.getItemCount() > 1) {
            tilesAdapter.removeTile(position);
            // A row may have gone with it, and the tiles left behind are owed its height.
            updateTileHeight();
        }
        persistTileOrder();
    }

    /**
     * Saves the order the tiles now stand in, once a drag has finished.
     * <p>
     * Ids rather than the tiles themselves, because that is what the preference holds: an id
     * written by a later version means nothing to an earlier one, which drops it and carries on.
     */
    private void persistTileOrder() {
        if (tilesAdapter == null) return;
        HomeTile.saveOrder(tilesAdapter.currentTiles());
    }

    /**
     * Draws the grid again from whatever is saved now.
     * <p>
     * For the editor to call when it has changed which tiles there are, which it does on its own
     * screen rather than on this one.
     */
    public void refreshTiles() {
        submitTiles();
    }

    /**
     * Hands the grid's measured height to the adapter, divided by the number of rows.
     * <p>
     * What is passed is the height of a row, margins included; the adapter takes each tile's own
     * margins off it. Always posted, never applied on the spot: this is called from a layout
     * callback, and telling a RecyclerView its items changed while it is laying out throws.
     */
    private void updateTileHeight() {
        if (tilesGrid == null || tilesAdapter == null) return;
        final int height = tilesGrid.getHeight();
        final int rows = tilesAdapter.getRowCount();
        if (height <= 0 || rows <= 0) return;

        final int rowHeight = height / rows;
        if (rowHeight == tilesAdapter.getRowHeight()) return;
        tilesGrid.post(() -> {
            if (tilesAdapter != null) tilesAdapter.setRowHeight(rowHeight);
        });
    }

    /**
     * Reads the saved order and hands it to the grid, falling back to the defaults the first
     * time round. Called again whenever the launcher returns to the foreground, because the
     * layout may have been changed from the settings in the meantime.
     */
    private void submitTiles() {
        if (tilesAdapter == null || tilesGrid == null) return;

        // Reached from a window-visibility change and from a couple of observers, any of which
        // may land mid-layout; rebuilding the list then throws.
        if (tilesGrid.isComputingLayout()) {
            tilesGrid.post(this::submitTiles);
            return;
        }

        tileViews.clear();
        tilesAdapter.submit(HomeTile.savedOrder(), COLUMNS);

        // The row count may have changed even though the grid's own height has not, so the
        // height is recomputed here as well as from the layout callback.
        updateTileHeight();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            submitTiles();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        LifecycleOwner owner = ViewTreeLifecycleOwner.get(this);
        if (owner != null) {
            bindHomeBattery(owner);
            repo.getPackages().observe(owner, this::refreshBadges);
            repo.getMissedCalls(activity).observe(owner, missedCalls -> {
                final FirstPageAppIcon recent = tileViews.get(HomeTile.RECENT);
                if (recent != null && !viewsToApps.containsValue(recent)) {
                    recent.setBadgeVisibility(!missedCalls.isEmpty());
                }
            });
            AppsRepository.getAllAppsLiveData().observe(owner, apps -> {
                viewsToApps.clear();
                submitTiles();
            });
        } else {
            Log.e(TAG, "LifecycleOwner is null. Cannot observe LiveData.");
        }
    }

    private Intent getCameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        ResolveInfo resolveInfo =
                activity.getPackageManager()
                        .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            Log.e(TAG, "No camera app found to handle IMAGE_CAPTURE action.");
            BaldToast.error(this.getContext());
            return null;
        }

        ActivityInfo activityInfo = resolveInfo.activityInfo;
        ComponentName name =
                new ComponentName(activityInfo.applicationInfo.packageName, activityInfo.name);
        return new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .setComponent(name);
    }

    /**
     * Applies what a tile actually shows, as the grid binds it.
     *
     * Anchored on the tile rather than on a view field, which is what the nine setupButton
     * calls used to do. Everything below - a tile pointed at an app of the user's choosing,
     * the lock tile behaving differently on older Android - is the same logic keyed differently.
     */
    private void bindTile(HomeTile tile, FirstPageAppIcon view) {
        sharedPreferences = BPrefs.get(activity);
        tileViews.put(tile, view);
        setupButton(tile, view);
    }

    /**
     * What a tile does when it has not been pointed at an app of the user's choosing.
     *
     * Returns null for the tiles that still live on the second page: they are in the catalogue
     * but their behaviour has not been moved across yet, so they are not offered on the grid.
     */
    @Nullable
    private OnClickListener defaultActionFor(HomeTile tile) {
        switch (tile) {
            case RECENT:
                return v -> homeScreen.startActivity(new Intent(homeScreen, RecentCallsActivity.class));
            case DIALER:
                return v -> homeScreen.startActivity(new Intent(homeScreen, DialerActivity.class));
            case CONTACTS:
                return v -> homeScreen.startActivity(new Intent(homeScreen, ContactsActivity.class));
            case WHATSAPP:
                return v -> {
                    try {
                        WhatsAppHandler.INSTANCE.launch(homeScreen);
                    } catch (Exception e) {
                        BaldToast.error(homeScreen, e.getLocalizedMessage());
                    }
                };
            case ASSISTANT:
                return v -> {
                    try {
                        homeScreen.startActivity(
                                new Intent(Intent.ACTION_VOICE_COMMAND)
                                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } catch (Exception e) {
                        BaldToast.from(homeScreen)
                                .setType(BaldToast.TYPE_ERROR)
                                .setText(R.string.your_phone_doesnt_have_assistant_installed)
                                .show();
                    }
                };
            case MESSAGES:
                return v -> {
                    try {
                        final ResolveInfo resolveInfo =
                                homeScreen
                                        .getPackageManager()
                                        .queryIntentActivities(
                                                new Intent("android.intent.action.MAIN", null)
                                                        .setPackage(
                                                                Telephony.Sms.getDefaultSmsPackage(
                                                                        homeScreen)),
                                                0)
                                        .iterator()
                                        .next();
                        S.startComponentName(
                                homeScreen,
                                new ComponentName(
                                        resolveInfo.activityInfo.packageName,
                                        resolveInfo.activityInfo.name));
                    } catch (Exception e) {
                        BaldToast.from(homeScreen)
                                .setType(BaldToast.TYPE_ERROR)
                                .setText(R.string.an_error_has_occurred)
                                .show();
                    }
                };
            case EMERGENCY:
                return v -> homeScreen.startActivity(new Intent(homeScreen, SOSActivity.class));
            case CAMERA:
                return v -> {
                    Intent intent = getCameraIntent();
                    if (intent != null) {
                        homeScreen.startActivity(intent);
                    }
                };
            case LOCK_SCREEN:
                return v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        requestDeviceLock();
                    } else {
                        homeScreen.startActivity(new Intent(homeScreen, AppsActivity.class));
                    }
                };
            case PILLS:
                return v -> homeScreen.startActivity(new Intent(homeScreen, PillsActivity.class));
            case APPS:
                return v -> homeScreen.startActivity(new Intent(homeScreen, AppsActivity.class));
            case ALARMS:
                return v -> homeScreen.startActivity(new Intent(homeScreen, AlarmsActivity.class));
            case SETTINGS:
                return v -> homeScreen.startActivity(new Intent(homeScreen, SettingsActivity.class));
            case PHOTOS:
                return v -> openGallery(MediaActivity.MODE_PHOTOS_ONLY);
            case VIDEOS:
                return v -> openGallery(MediaActivity.MODE_VIDEOS_ONLY);
            case INTERNET:
                return v -> openExternally(Uri.parse("https://www.google.com"));
            case MAPS:
                return v -> openExternally(Uri.parse("geo:0,0"));
            default:
                return null;
        }
    }

    /** The gallery, showing one kind of thing at a time; the modes are its own. */
    private void openGallery(int mode) {
        startActivityWithNewTaskClear(
                getContext(),
                new Intent(getContext(), MediaActivity.class)
                        .putExtra(MediaActivity.EXTRA_MODE, mode));
    }

    /**
     * Hands a web address or a map reference to whatever on the phone deals with such things.
     * <p>
     * Deliberately no chooser of our own. Where several apps can answer, Android asks in the
     * dialog the person already meets everywhere else on the phone, and remembers the answer if
     * they say to; a list built here would be a second thing to learn that forgets every time.
     */
    private void openExternally(@NonNull Uri uri) {
        try {
            homeScreen.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "nothing on this phone can open " + uri.getScheme(), e);
            BaldToast.from(homeScreen)
                    .setType(BaldToast.TYPE_ERROR)
                    .setText(R.string.an_error_has_occurred)
                    .show();
        }
    }

    private void setupButton(HomeTile tile, @NonNull FirstPageAppIcon button) {
        if (homeScreen != null) {
            setupButtonForHomeScreen(tile, button);
        } else {
            setupButtonForEditor(tile, button);
        }
    }

    private void setupButtonForHomeScreen(HomeTile tile, @NonNull FirstPageAppIcon button) {
        final int defaultTextRes = tile.getLabelRes();
        final int defaultIconRes = tile.getIconRes();
        final OnClickListener defaultListener = defaultActionFor(tile);
        AppEntry app = findAppByPreference(tile.getCustomAppKey());
        if (app != null) {
            button.setText(app.getLabel());
            AppIconBinder.loadPic(app, button.imageView);
            button.setOnClickListener(v -> S.startComponentName(homeScreen, app));
            viewsToApps.put(app, button);
        } else {
            setupDefault(tile, button, defaultTextRes, defaultIconRes, defaultListener);
        }
    }

    private void setupButtonForEditor(HomeTile tile, @NonNull FirstPageAppIcon bt) {
        final String bPrefsKey = tile.getCustomAppKey();
        final int defaultTextRes = tile.getLabelRes();
        final int defaultIconRes = tile.getIconRes();
        AppEntry app = findAppByPreference(bPrefsKey);

        // This is for Page1EditorActivity context
        final Page1EditorActivity page1EditorActivity = (Page1EditorActivity) activity;

        // Holding a tile is how it gets picked up and moved, so the dialog below has to answer
        // a short tap - even where the accessibility settings would normally require a hold.
        bt.setDirectTaps(true);

        // The dialog's first option clears the preference, so it is named after what the tile
        // will become, not after what it currently is. Naming it after the app already assigned
        // read as "keep AntennaPod" while it in fact discarded it.
        final CharSequence defaultName;

        if (tile == HomeTile.LOCK_SCREEN && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (app == null) {
                AppEntry appsActivityApp = PredefinedApps.getAppsActivityEntry(activity);
                if (appsActivityApp != null) {
                    bt.setText(appsActivityApp.getLabel());
                    AppIconBinder.loadPic(appsActivityApp, bt.imageView);
                }
            }
            defaultName = activity.getText(R.string.apps);
        } else {
            defaultName = activity.getText(defaultTextRes);
        }

        final BDB bdb =
                BDB.from(activity)
                        .setTitle(R.string.custom_app)
                        .setSubText(R.string.custom_app_subtext)
                        .addFlag(BDialog.FLAG_OK | BDialog.FLAG_CANCEL)
                        .setOptions(defaultName, activity.getText(R.string.custom))
                        .setOptionsStartingIndex(
                                () -> sharedPreferences.contains(bPrefsKey) ? 1 : 0)
                        .setPositiveButtonListener(
                                params -> {
                                    if (params[0].equals(0)) {
                                        sharedPreferences.edit().remove(bPrefsKey).apply();
                                    } else {
                                        activity.startActivityForResult(
                                                new Intent(activity, AppsActivity.class)
                                                        .putExtra(
                                                                AppsActivity.CHOOSE_MODE,
                                                                bPrefsKey),
                                                0);
                                    }
                                    return true;
                                });

        bt.setOnClickListener(
                v ->
                        bdb.show()
                                .setOnDismissListener(
                                        dialog -> {
                                            if (page1EditorActivity.baldPrefsUtils.hasChanged(
                                                    page1EditorActivity)) {
                                                page1EditorActivity.recreate();
                                            }
                                        }));

        if (app != null) {
            bt.setText(app.getLabel());
            AppIconBinder.loadPic(app, bt.imageView);
            viewsToApps.put(app, bt);
        } else {
            if (tile != HomeTile.LOCK_SCREEN || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bt.setText(defaultTextRes);
                bt.setImageResource(defaultIconRes);
            }
        }
    }

    private void setupDefault(HomeTile tile, @NonNull FirstPageAppIcon bt, int defaultTextRes, int defaultIconRes, OnClickListener onClickListener) {
        if (tile == HomeTile.LOCK_SCREEN) {
            // The lock screen button has a different behavior on older APIs
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                // On older APIs, this button opens the Apps screen
                AppEntry app = PredefinedApps.getAppsActivityEntry(getContext());
                if (app != null) {
                    bt.setText(app.getLabel());
                    AppIconBinder.loadPic(app, bt.imageView);
                }
            } else {
                bt.setText(defaultTextRes);
                bt.setImageResource(defaultIconRes);
            }
        } else {
            bt.setText(defaultTextRes);
            bt.setImageResource(defaultIconRes);
        }
        bt.setOnClickListener(onClickListener);
    }

    // Helper
    @Nullable
    private AppEntry findAppByPreference(@Nullable String bPrefsKey) {
        if (bPrefsKey != null && sharedPreferences.contains(bPrefsKey)) {
            String componentName = sharedPreferences.getString(bPrefsKey, null);
            if (componentName == null) return null;
            return AppsRepository.findByComponentName(componentName);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void requestDeviceLock() {
        DeviceLock.requestLock(homeScreen, result -> {
            switch (result) {
                case FAILURE:
                    // System reports failure despite the service being technically enabled.
                    // Prompt user to re-enable to fix the internal state.
                    new BaldDialog.Builder(homeScreen)
                        .setTitle(R.string.accessibility_permission_check_title)
                        .setMessage(R.string.accessibility_permission_check_message)
                        .setPositiveButton(R.string.dialog_button_enable, dialog -> {
                            openAccessibilitySettings();
                            return kotlin.Unit.INSTANCE;
                        })
                        .setNegativeButton(R.string.dialog_button_not_now, null)
                        .show();
                    break;
                case ACCESS_DENIED:
                    // Permission missing.
                    PermissionManager.checkOrRequest(
                        homeScreen, SpecialPermission.Accessibility.INSTANCE, result1 -> {}
                    );
                    break;
            }
        });
    }

    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            homeScreen.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            BaldToast.error(homeScreen, "Failed to open accessibility settings.");
        }
    }

    private void refreshBadges(Set<String> packagesSet) {
        Context viewContext = getContext(); // Use the view's context if available

        final FirstPageAppIcon whatsapp = tileViews.get(HomeTile.WHATSAPP);
        if (whatsapp != null && !viewsToApps.containsValue(whatsapp)) {
            whatsapp.setBadgeVisibility(packagesSet.contains(WhatsAppHandler.WHATSAPP_PACKAGE_NAME));
        }


        final FirstPageAppIcon messages = tileViews.get(HomeTile.MESSAGES);
        if (messages != null && !viewsToApps.containsValue(messages)) {
            String defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(viewContext);
            if (defaultSmsPackage != null) {
                messages.setBadgeVisibility(packagesSet.contains(defaultSmsPackage));
            } else {
                messages.setBadgeVisibility(false); // No default SMS app, hide badge
            }
        }

        for (Map.Entry<AppEntry, FirstPageAppIcon> app : viewsToApps.entrySet()) {
            if (app == null) continue;

            FirstPageAppIcon icon = app.getValue();
            if (icon != null) {
                ComponentName cn = app.getKey().getComponent();
                icon.setBadgeVisibility(
                    packagesSet.contains(cn.getPackageName()));
            }
        }
    }
}
