package com.newtube.mobile.ui.playback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import com.newtube.mobile.ui.common.GlassMaterial;

/**
 * Unit tests verifying Liquid Glass mini-player and bottom sheet drawable foundation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class MiniPlayerGlassTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
    }

    @Test
    public void miniPlayerDrawablesResolveSuccessfully() {
        Drawable card = ContextCompat.getDrawable(mContext, R.drawable.bg_glass_card);
        assertNotNull("Liquid glass card background must resolve", card);

        Drawable highlight = ContextCompat.getDrawable(mContext, R.drawable.bg_glass_card_highlight);
        assertNotNull("Liquid glass card top highlight must resolve", highlight);

        Drawable metaScrim = ContextCompat.getDrawable(mContext, R.drawable.bg_mini_meta_scrim);
        assertNotNull("Mini-player metadata scrim must resolve", metaScrim);

        Drawable buttonScrim = ContextCompat.getDrawable(mContext, R.drawable.bg_mini_scrim_circle);
        assertNotNull("Mini-player button scrim must resolve", buttonScrim);
    }

    @Test
    public void bottomSheetDrawablesResolveSuccessfully() {
        Drawable sheet = ContextCompat.getDrawable(mContext, R.drawable.bg_mobile_sheet);
        assertNotNull("Liquid glass bottom sheet background must resolve", sheet);

        Drawable handle = ContextCompat.getDrawable(mContext, R.drawable.bg_mobile_sheet_handle);
        assertNotNull("Liquid glass bottom sheet handle must resolve", handle);

        assertEquals("GlassMaterial SHEET shape should match bg_mobile_sheet",
                R.drawable.bg_mobile_sheet,
                GlassMaterial.getSheetBackgroundRes());
    }

    @Test
    public void miniPlayerBridgeInitialStateIsInactive() {
        assertFalse("Mini player bridge should be inactive initially", MiniPlayerBridge.isActive());
        assertNull("Mini player player should be null initially", MiniPlayerBridge.getPlayer());
        assertNull("Mini player video metadata should be null initially", MiniPlayerBridge.getVideo());
    }
}
