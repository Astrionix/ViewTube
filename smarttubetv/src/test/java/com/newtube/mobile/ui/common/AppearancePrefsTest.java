package com.newtube.mobile.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.ConscryptMode;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE, application = Application.class)
@ConscryptMode(ConscryptMode.Mode.OFF)
public class AppearancePrefsTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mContext.getSharedPreferences("newtube_appearance", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void floatingHeaderIsDisabledByDefault() {
        assertFalse("Floating header should be disabled by default",
                AppearancePrefs.isFloatingHeaderEnabled(mContext));
    }

    @Test
    public void floatingBottomBarIsDisabledByDefault() {
        assertFalse("Floating bottom bar should be disabled by default",
                AppearancePrefs.isFloatingBottomBarEnabled(mContext));
    }

    @Test
    public void headerPreferencePersistsAcrossReads() {
        AppearancePrefs.setFloatingHeaderEnabled(mContext, true);
        assertTrue("Floating header should be enabled after setting",
                AppearancePrefs.isFloatingHeaderEnabled(mContext));

        AppearancePrefs.setFloatingHeaderEnabled(mContext, false);
        assertFalse("Floating header should be disabled after setting to false",
                AppearancePrefs.isFloatingHeaderEnabled(mContext));
    }

    @Test
    public void bottomBarPreferencePersistsAcrossReads() {
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, true);
        assertTrue("Floating bottom bar should be enabled after setting",
                AppearancePrefs.isFloatingBottomBarEnabled(mContext));

        AppearancePrefs.setFloatingBottomBarEnabled(mContext, false);
        assertFalse("Floating bottom bar should be disabled after setting to false",
                AppearancePrefs.isFloatingBottomBarEnabled(mContext));
    }

    @Test
    public void headerAndBottomBarTogglesAreIndependent() {
        // Initially both false
        assertFalse(AppearancePrefs.isFloatingHeaderEnabled(mContext));
        assertFalse(AppearancePrefs.isFloatingBottomBarEnabled(mContext));

        // Enable header only
        AppearancePrefs.setFloatingHeaderEnabled(mContext, true);
        assertTrue("Header should be enabled", AppearancePrefs.isFloatingHeaderEnabled(mContext));
        assertFalse("Bottom bar must remain disabled when header is enabled",
                AppearancePrefs.isFloatingBottomBarEnabled(mContext));

        // Enable bottom bar too
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, true);
        assertTrue("Header should remain enabled", AppearancePrefs.isFloatingHeaderEnabled(mContext));
        assertTrue("Bottom bar should now be enabled", AppearancePrefs.isFloatingBottomBarEnabled(mContext));

        // Disable header only
        AppearancePrefs.setFloatingHeaderEnabled(mContext, false);
        assertFalse("Header should now be disabled", AppearancePrefs.isFloatingHeaderEnabled(mContext));
        assertTrue("Bottom bar must remain enabled when header is disabled",
                AppearancePrefs.isFloatingBottomBarEnabled(mContext));
    }

    @Test
    public void listenersAreNotifiedOnHeaderChange() {
        AtomicInteger notificationCount = new AtomicInteger(0);
        AppearancePrefs.OnAppearanceChangeListener listener = notificationCount::incrementAndGet;

        AppearancePrefs.addListener(listener);

        AppearancePrefs.setFloatingHeaderEnabled(mContext, true);
        assertEquals(1, notificationCount.get());

        // Same value should not re-trigger listener
        AppearancePrefs.setFloatingHeaderEnabled(mContext, true);
        assertEquals(1, notificationCount.get());

        // Value change should trigger listener again
        AppearancePrefs.setFloatingHeaderEnabled(mContext, false);
        assertEquals(2, notificationCount.get());

        AppearancePrefs.removeListener(listener);
        AppearancePrefs.setFloatingHeaderEnabled(mContext, true);
        assertEquals(2, notificationCount.get());
    }

    @Test
    public void listenersAreNotifiedOnBottomBarChange() {
        AtomicInteger notificationCount = new AtomicInteger(0);
        AppearancePrefs.OnAppearanceChangeListener listener = notificationCount::incrementAndGet;

        AppearancePrefs.addListener(listener);

        // Toggle from default false to true
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, true);
        assertEquals(1, notificationCount.get());

        // Same value should not re-trigger listener
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, true);
        assertEquals(1, notificationCount.get());

        // Value change should trigger listener again
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, false);
        assertEquals(2, notificationCount.get());

        AppearancePrefs.removeListener(listener);
        AppearancePrefs.setFloatingBottomBarEnabled(mContext, true);
        assertEquals(2, notificationCount.get());
    }
}
