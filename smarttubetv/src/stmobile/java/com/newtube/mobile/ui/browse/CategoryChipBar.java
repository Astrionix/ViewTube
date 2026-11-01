package com.newtube.mobile.ui.browse;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.common.Haptics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * YouTube-style Category Filter Chip Bar for the Home screen header.
 *
 * <p>Presents a horizontal, single-row scrolling strip of rounded pills:
 * All (Home), Gaming, Music, News, Live, Sports, Trending, Kids, Channels.
 * Tapping a pill triggers section navigation through {@link com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter}
 * and smoothly scrolls the chip into focus.</p>
 */
public class CategoryChipBar {

    public interface Listener {
        void onCategorySelected(int sectionId);
    }

    public static final class CategoryItem {
        public final int sectionId;
        public final int titleResId;

        public CategoryItem(int sectionId, int titleResId) {
            this.sectionId = sectionId;
            this.titleResId = titleResId;
        }
    }

    private final Activity mActivity;
    private final Listener mListener;
    private final HorizontalScrollView mScrollView;
    private final LinearLayout mChipsContainer;
    private final Map<Integer, TextView> mChipViews = new LinkedHashMap<>();
    private int mSelectedSectionId = MediaGroup.TYPE_HOME;

    private static final List<CategoryItem> CATEGORIES = new ArrayList<>();
    static {
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_HOME, R.string.mobile_category_all));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_GAMING, R.string.header_gaming));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_MUSIC, R.string.header_music));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_NEWS, R.string.header_news));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_LIVE, R.string.header_live));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_SPORTS, R.string.header_sports));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_TRENDING, R.string.header_trending));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_KIDS_HOME, R.string.header_kids_home));
        CATEGORIES.add(new CategoryItem(MediaGroup.TYPE_CHANNEL_UPLOADS, R.string.header_channels));
    }

    public CategoryChipBar(@NonNull Activity activity, @NonNull Listener listener) {
        mActivity = activity;
        mListener = listener;
        mScrollView = activity.findViewById(R.id.mobile_category_scroll);
        mChipsContainer = activity.findViewById(R.id.mobile_category_chips);

        initChips();
    }

    private void initChips() {
        if (mChipsContainer == null) {
            return;
        }
        mChipsContainer.removeAllViews();
        mChipViews.clear();
        LayoutInflater inflater = LayoutInflater.from(mActivity);

        for (CategoryItem item : CATEGORIES) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_mobile_category_chip, mChipsContainer, false);
            chip.setText(item.titleResId);
            chip.setOnClickListener(v -> {
                Haptics.click(v);
                if (mSelectedSectionId != item.sectionId) {
                    selectCategory(item.sectionId);
                    mListener.onCategorySelected(item.sectionId);
                }
            });
            mChipsContainer.addView(chip);
            mChipViews.put(item.sectionId, chip);
        }

        updateChipStyles();
    }

    public void selectCategory(int sectionId) {
        if (!mChipViews.containsKey(sectionId)) {
            return;
        }
        mSelectedSectionId = sectionId;
        updateChipStyles();

        TextView selectedChip = mChipViews.get(sectionId);
        if (selectedChip != null && mScrollView != null) {
            mScrollView.post(() -> {
                int targetX = selectedChip.getLeft() - dp(16);
                mScrollView.smoothScrollTo(Math.max(0, targetX), 0);
            });
        }
    }

    public int getSelectedSectionId() {
        return mSelectedSectionId;
    }

    public boolean isCategorySupported(int sectionId) {
        return mChipViews.containsKey(sectionId);
    }

    public void setVisibility(int visibility) {
        if (mScrollView != null) {
            mScrollView.setVisibility(visibility);
        }
    }

    public int getVisibility() {
        return mScrollView != null ? mScrollView.getVisibility() : View.GONE;
    }

    private void updateChipStyles() {
        int colorActiveText = ContextCompat.getColor(mActivity, R.color.mobile_color_on_inverse_surface);
        int colorInactiveText = ContextCompat.getColor(mActivity, R.color.mobile_color_on_surface);

        for (Map.Entry<Integer, TextView> entry : mChipViews.entrySet()) {
            boolean isSelected = entry.getKey() == mSelectedSectionId;
            TextView chip = entry.getValue();

            chip.setBackgroundResource(isSelected ? R.drawable.bg_category_chip_active : R.drawable.bg_category_chip_inactive);
            chip.setTextColor(isSelected ? colorActiveText : colorInactiveText);
            chip.setTypeface(null, isSelected ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, mActivity.getResources().getDisplayMetrics()));
    }
}
