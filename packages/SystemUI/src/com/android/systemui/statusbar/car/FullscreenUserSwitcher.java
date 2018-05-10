/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.systemui.statusbar.car;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.car.user.CarUserManagerHelper;
import android.content.Context;
import android.view.View;
import android.view.ViewStub;

import android.support.v7.widget.GridLayoutManager;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBar;

/**
 * Manages the fullscreen user switcher.
 */
public class FullscreenUserSwitcher {
    private final View mContainer;
    private final View mParent;
    private final UserGridRecyclerView mUserGridView;
    private final int mShortAnimDuration;
    private final StatusBar mStatusBar;
    private final CarUserManagerHelper mCarUserManagerHelper;
    private int mCurrentForegroundUserId;
    private boolean mShowing;

    public FullscreenUserSwitcher(StatusBar statusBar, ViewStub containerStub, Context context) {
        mStatusBar = statusBar;
        mParent = containerStub.inflate();
        mContainer = mParent.findViewById(R.id.container);
        mUserGridView = mContainer.findViewById(R.id.user_grid);
        GridLayoutManager layoutManager = new GridLayoutManager(context,
                context.getResources().getInteger(R.integer.user_fullscreen_switcher_num_col));
        mUserGridView.getRecyclerView().setLayoutManager(layoutManager);
        mUserGridView.buildAdapter();
        mUserGridView.setUserSelectionListener(this::onUserSelected);

        mCarUserManagerHelper = new CarUserManagerHelper(context);
        updateCurrentForegroundUser();

        mShortAnimDuration = mContainer.getResources()
            .getInteger(android.R.integer.config_shortAnimTime);
    }

    public void show() {
        if (mShowing) {
            return;
        }
        mShowing = true;
        mParent.setVisibility(View.VISIBLE);
    }

    public void hide() {
        mShowing = false;
        mParent.setVisibility(View.GONE);
    }

    public void onUserSwitched(int newUserId) {
        // The logic for foreground user change is needed here to exclude the reboot case. On
        // reboot, system fires ACTION_USER_SWITCHED change from -1 to 0 user. This is not an actual
        // user switch. We only want to trigger keyguard dismissal when foreground user changes.
        if (foregroundUserChanged()) {
            toggleSwitchInProgress(false);
            updateCurrentForegroundUser();
            mParent.post(this::dismissKeyguard);
        }
    }

    private boolean foregroundUserChanged() {
        return mCurrentForegroundUserId != mCarUserManagerHelper.getCurrentForegroundUserId();
    }

    private void updateCurrentForegroundUser() {
        mCurrentForegroundUserId = mCarUserManagerHelper.getCurrentForegroundUserId();
    }

    private void onUserSelected(UserGridRecyclerView.UserRecord record) {
        if (record.mIsForeground) {
            dismissKeyguard();
            return;
        }
        toggleSwitchInProgress(true);
    }

    // Dismisses the keyguard and shows bouncer if authentication is necessary.
    private void dismissKeyguard() {
        mStatusBar.executeRunnableDismissingKeyguard(null/* runnable */, null /* cancelAction */,
                true /* dismissShade */, true /* afterKeyguardGone */, true /* deferred */);
    }

    private void toggleSwitchInProgress(boolean inProgress) {
        if (inProgress) {
            fadeOut(mContainer);
        } else {
            fadeIn(mContainer);
        }
    }

    private void fadeOut(View view) {
        view.animate()
                .alpha(0.0f)
                .setDuration(mShortAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        view.setVisibility(View.GONE);
                    }
                });
    }

    private void fadeIn(View view) {
        view.animate()
                .alpha(1.0f)
                .setDuration(mShortAnimDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animator) {
                        view.setAlpha(0.0f);
                        view.setVisibility(View.VISIBLE);
                    }
                });
    }
}
