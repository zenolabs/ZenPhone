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

package com.bald.uriah.baldphone.views.home;

import android.app.Activity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.bald.uriah.baldphone.activities.HomeScreenActivity;

public abstract class HomeView extends FrameLayout {
    protected final HomeScreenActivity homeScreen;
    protected final Activity activity;

    /** For the subclasses that are only ever built in code, and so have no attributes to pass. */
    public HomeView(HomeScreenActivity homeScreen, Activity activity) {
        this(homeScreen, activity, null);
    }

    /**
     * @param attrs what the XML tag said, or null when built in code.
     *              <p>
     *              Must reach {@link FrameLayout}, or everything declared on the tag that belongs
     *              to the view itself is quietly dropped - the id among it. A subclass inflated
     *              from a layout would then answer to no findViewById at all, and the caller
     *              would get null back from an id it can see written in the file in front of it.
     */
    public HomeView(HomeScreenActivity homeScreen, Activity activity, @Nullable AttributeSet attrs) {
        super(homeScreen == null ? activity : homeScreen, attrs);
        this.homeScreen = homeScreen;
        this.activity = activity;
        addView(onCreateView(LayoutInflater.from(activity), this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    public abstract View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup);

}
