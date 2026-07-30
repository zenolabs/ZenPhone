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

package com.bald.uriah.baldphone.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.R;
import com.bald.uriah.baldphone.utils.BPrefs;
import com.bald.uriah.baldphone.utils.BaldToast;
import com.bald.uriah.baldphone.utils.D;

/**
 * Simple Button, extends {@link androidx.appcompat.widget.AppCompatTextView}; adapted to App settings.
 * {@link BaldFrameLayoutButton#setOnLongClickListener(OnLongClickListener)} is deprecated,
 * use {@link BaldFrameLayoutButton#setOnClickListener(OnClickListener)} instead.
 */
public class BaldFrameLayoutButton extends FrameLayout implements BaldButtonInterface, View.OnLongClickListener, View.OnClickListener {
    private final SharedPreferences sharedPreferences;
    private final boolean longPresses, vibrationFeedback, longPressesShorter;
    private final Vibrator vibrator;
    private final BaldToast longer;
    private OnClickListener onClickListener;
    private BaldButtonTouchListener baldButtonTouchListener;
    /**
     * @see #setDirectTaps(boolean)
     */
    private boolean directTaps;

    public BaldFrameLayoutButton(Context context) {
        this(context, null);
    }

    public BaldFrameLayoutButton(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BaldFrameLayoutButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.sharedPreferences = context.getSharedPreferences(D.BALD_PREFS, Context.MODE_PRIVATE);
        this.longPresses = sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_KEY, BPrefs.LONG_PRESSES_DEFAULT_VALUE);
        this.longPressesShorter = sharedPreferences.getBoolean(BPrefs.LONG_PRESSES_SHORTER_KEY, BPrefs.LONG_PRESSES_SHORTER_DEFAULT_VALUE);
        this.vibrationFeedback = sharedPreferences.getBoolean(BPrefs.VIBRATION_FEEDBACK_KEY, BPrefs.VIBRATION_FEEDBACK_DEFAULT_VALUE);
        this.vibrator = this.vibrationFeedback ? (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE) : null;
        longer = longPresses ? BaldToast.from(context).setText(context.getText(R.string.press_longer)).setType(BaldToast.TYPE_DEFAULT).setLength(0).build() : null;
        applyPressBehaviour();
    }

    /**
     * Decides which gesture reaches {@link #onClickListener}.
     * <p>
     * Called again whenever {@link #setDirectTaps(boolean)} changes, so every listener it may
     * have installed before is cleared here rather than merely left in place: the three
     * arrangements below are alternatives, and two of them at once would fire twice.
     */
    private void applyPressBehaviour() {
        if (longPresses && !directTaps) {
            if (longPressesShorter) {
                if (baldButtonTouchListener == null)
                    baldButtonTouchListener = new BaldButtonTouchListener(this);
                super.setOnTouchListener(baldButtonTouchListener);
                super.setOnLongClickListener(null);
                setLongClickable(false);
                super.setOnClickListener(D.EMPTY_CLICK_LISTENER);
            } else {
                super.setOnTouchListener(null);
                super.setOnLongClickListener(this);
                super.setOnClickListener(this);
            }
        } else {
            super.setOnTouchListener(null);
            super.setOnLongClickListener(null);
            setLongClickable(false);
            super.setOnClickListener(this);
        }
    }

    /**
     * Makes this button answer a short tap, whatever the "long presses" accessibility setting
     * says.
     * <p>
     * That setting is there so a hand resting on the screen cannot start anything by accident,
     * and it stays in force on the home screen. In the layout editor, though, holding a tile is
     * how it is picked up and moved, and one tile cannot have "press and hold" mean two things.
     * The editor is used by whoever sets the phone up, not by whoever lives with it, so that is
     * where the plain tap can be given back.
     */
    public void setDirectTaps(boolean directTaps) {
        if (this.directTaps == directTaps) return;
        this.directTaps = directTaps;
        applyPressBehaviour();
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    /**
     * use {@link BaldButton#setOnLongClickListener(android.view.View.OnLongClickListener)} instead
     */
    @Deprecated
    @Override
    public void setOnLongClickListener(View.OnLongClickListener onLongClickListener) {
        throw new RuntimeException("use setOnClickListener(View.OnClickListener onClickListener) instead");
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        if (baldButtonTouchListener != null && !directTaps)
            baldButtonTouchListener.addListener(l);
        else
            super.setOnTouchListener(l);
    }

    @Override
    public void onClick(View v) {
        if (longPresses && !directTaps) {
            longer.show();
        } else {
            vibrate();
            if (onClickListener != null)
                onClickListener.onClick(v);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (longPresses && !directTaps) {
            vibrate();

            if (onClickListener != null)
                onClickListener.onClick(v);
            return true;
        }
        return false;

    }

    @Override
    public void baldPerformClick() {
        if (onClickListener != null)
            onClickListener.onClick(this);
    }

    @Override
    public void vibrate() {
        if (vibrationFeedback)
            vibrator.vibrate(D.vibetime);
    }
}
