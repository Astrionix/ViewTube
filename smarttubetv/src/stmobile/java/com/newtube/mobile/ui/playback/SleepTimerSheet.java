package com.newtube.mobile.ui.playback;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Bottom sheet dialog for Sleep Timer configuration, following YouTube's mobile interface.
 */
public final class SleepTimerSheet {

    public interface Listener {
        void onTimerUpdated(String message);
    }

    private static final int[] PRESET_MINUTES = new int[] {10, 15, 20, 30, 45, 60};

    private SleepTimerSheet() {
    }

    public static BottomSheetDialog create(@NonNull Activity activity,
                                           @NonNull SleepTimer timer,
                                           @NonNull Listener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        View content = LayoutInflater.from(activity).inflate(R.layout.sheet_mobile_sleep_timer, null);
        dialog.setContentView(content);

        LinearLayout statusContainer = content.findViewById(R.id.sleep_timer_status_container);
        TextView statusText = content.findViewById(R.id.sleep_timer_status_text);
        LinearLayout add15Button = content.findViewById(R.id.sleep_timer_add_15);
        View actionsDivider = content.findViewById(R.id.sleep_timer_actions_divider);
        LinearLayout list = content.findViewById(R.id.sleep_timer_sheet_list);

        Handler tickHandler = new Handler(Looper.getMainLooper());
        Runnable tickRunnable = new Runnable() {
            @Override
            public void run() {
                if (timer.isActive()) {
                    statusText.setText(timer.getFormattedStatus(activity));
                    tickHandler.postDelayed(this, 1000);
                } else {
                    statusContainer.setVisibility(View.GONE);
                    add15Button.setVisibility(View.GONE);
                    actionsDivider.setVisibility(View.GONE);
                }
            }
        };

        boolean isActive = timer.isActive();
        if (isActive) {
            statusContainer.setVisibility(View.VISIBLE);
            statusText.setText(timer.getFormattedStatus(activity));
            add15Button.setVisibility(View.VISIBLE);
            actionsDivider.setVisibility(View.VISIBLE);
            tickHandler.postDelayed(tickRunnable, 1000);
        } else {
            statusContainer.setVisibility(View.GONE);
            add15Button.setVisibility(View.GONE);
            actionsDivider.setVisibility(View.GONE);
        }

        com.newtube.mobile.ui.common.GlassMaterial.applyTouchScaleFeedback(add15Button);
        add15Button.setOnClickListener(v -> {
            com.newtube.mobile.ui.common.Haptics.click(v);
            timer.addMinutes(15);
            dialog.dismiss();
            listener.onTimerUpdated(activity.getString(R.string.mobile_sleep_timer_toast_added, 15));
        });

        // 1. Off option
        boolean isOff = !timer.isActive();
        addChoiceRow(activity, list, activity.getString(R.string.mobile_sleep_timer_off), isOff, () -> {
            timer.stop();
            dialog.dismiss();
            listener.onTimerUpdated(activity.getString(R.string.mobile_sleep_timer_toast_off));
        });

        // 2. Preset minutes (10, 15, 20, 30, 45, 60)
        for (int min : PRESET_MINUTES) {
            String label = activity.getResources().getQuantityString(
                    R.plurals.mobile_settings_minutes, min, min);
            boolean isSelected = timer.isActive() && !timer.isEndOfVideo() && timer.getInitialMinutes() == min;
            addChoiceRow(activity, list, label, isSelected, () -> {
                timer.start(min);
                dialog.dismiss();
                listener.onTimerUpdated(activity.getString(R.string.mobile_sleep_timer_toast_set, label));
            });
        }

        // 3. End of video option
        boolean isEndOfVideoSelected = timer.isEndOfVideo();
        addChoiceRow(activity, list, activity.getString(R.string.mobile_sleep_timer_end_of_video),
                isEndOfVideoSelected, () -> {
                    timer.startEndOfVideo();
                    dialog.dismiss();
                    listener.onTimerUpdated(activity.getString(R.string.mobile_sleep_timer_toast_end_of_video));
                });

        dialog.setOnDismissListener(d -> tickHandler.removeCallbacks(tickRunnable));

        return dialog;
    }

    private static void addChoiceRow(@NonNull Activity activity,
                                     @NonNull LinearLayout parent,
                                     @NonNull CharSequence label,
                                     boolean selected,
                                     @NonNull Runnable onClick) {
        View row = LayoutInflater.from(activity).inflate(R.layout.item_mobile_quality_row, parent, false);
        TextView labelView = row.findViewById(R.id.quality_row_label);
        labelView.setText(label);
        ImageView checkView = row.findViewById(R.id.quality_row_check);
        checkView.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        com.newtube.mobile.ui.common.GlassMaterial.applyTouchScaleFeedback(row);
        row.setOnClickListener(v -> {
            com.newtube.mobile.ui.common.Haptics.click(v);
            onClick.run();
        });
        parent.addView(row);
    }
}
