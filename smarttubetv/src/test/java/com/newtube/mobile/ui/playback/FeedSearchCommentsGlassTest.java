package com.newtube.mobile.ui.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import com.liskovsoft.smartyoutubetv2.tv.R;
import com.newtube.mobile.ui.search.SearchTagAdapter;

/**
 * Unit tests verifying Liquid Glass drawables and UI adapters for Feed, Search, and Comments.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class FeedSearchCommentsGlassTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
    }

    @Test
    public void feedAndCardDrawablesResolveSuccessfully() {
        Drawable badge = ContextCompat.getDrawable(mContext, R.drawable.bg_video_badge);
        assertNotNull("Liquid glass video duration badge drawable must resolve", badge);

        Drawable liveBadge = ContextCompat.getDrawable(mContext, R.drawable.bg_video_badge_live);
        assertNotNull("Liquid glass live badge drawable must resolve", liveBadge);
    }

    @Test
    public void searchExperienceDrawablesResolveSuccessfully() {
        Drawable searchField = ContextCompat.getDrawable(mContext, R.drawable.mobile_search_field_background);
        assertNotNull("Liquid glass search input field capsule drawable must resolve", searchField);
    }

    @Test
    public void commentsExperienceDrawablesResolveSuccessfully() {
        Drawable sortChip = ContextCompat.getDrawable(mContext, R.drawable.bg_comment_sort_chip);
        assertNotNull("Liquid glass comments sort chip drawable must resolve", sortChip);

        Drawable repliesChip = ContextCompat.getDrawable(mContext, R.drawable.bg_comment_replies_chip);
        assertNotNull("Liquid glass comment replies chip drawable must resolve", repliesChip);

        Drawable commentsMenu = ContextCompat.getDrawable(mContext, R.drawable.bg_comments_menu);
        assertNotNull("Liquid glass comments menu popup drawable must resolve", commentsMenu);

        Drawable composeField = ContextCompat.getDrawable(mContext, R.drawable.bg_comment_compose_field);
        assertNotNull("Liquid glass compose field pill drawable must resolve", composeField);

        Drawable composer = ContextCompat.getDrawable(mContext, R.drawable.bg_comment_composer);
        assertNotNull("Liquid glass docked composer sheet drawable must resolve", composer);
    }

    @Test
    public void searchTagAdapterInitialState() {
        SearchTagAdapter adapter = new SearchTagAdapter(null, null, null);
        assertTrue("SearchTagAdapter should initially be empty", adapter.isEmpty());
        assertEquals("Initial item count should be 0", 0, adapter.getItemCount());
    }
}
