package com.newtube.mobile.ui.browse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import com.liskovsoft.smartyoutubetv2.tv.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class LiquidGlassBottomNavigationViewTest {

    private Context mContext;
    private LiquidGlassBottomNavigationView mNav;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mContext.setTheme(R.style.Theme_NewTube);
        mNav = new LiquidGlassBottomNavigationView(mContext);
    }

    @Test
    public void floatingModeIsDisabledByDefault() {
        assertFalse("Floating mode should be disabled by default", mNav.isFloatingEnabled());
    }

    @Test
    public void floatingModeCanBeToggled() {
        mNav.setFloatingEnabled(true);
        assertTrue("Floating mode should be enabled", mNav.isFloatingEnabled());

        mNav.setFloatingEnabled(false);
        assertFalse("Floating mode should be disabled", mNav.isFloatingEnabled());
    }

    @Test
    public void navigationRendersSafelyInFloatingMode() {
        mNav.setFloatingEnabled(true);
        mNav.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(180, android.view.View.MeasureSpec.EXACTLY)
        );
        mNav.layout(0, 0, 1080, 180);

        Bitmap bitmap = Bitmap.createBitmap(1080, 180, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        mNav.draw(canvas);

        assertNotNull("Bitmap should be drawn", bitmap);
    }

    @Test
    public void selectionAnimatesWithoutExceptions() {
        mNav.getMenu().add(0, 101, 0, "Home");
        mNav.getMenu().add(0, 102, 1, "Subscriptions");
        mNav.getMenu().add(0, 103, 2, "Downloads");

        mNav.setFloatingEnabled(true);
        mNav.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(180, android.view.View.MeasureSpec.EXACTLY)
        );
        mNav.layout(0, 0, 1080, 180);

        mNav.setSelectedItemId(102);
        mNav.setSelectedItemId(103);
        assertTrue("Selection completes cleanly", true);
    }
}
