package com.newtube.mobile.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

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
public class GlassMaterialTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
    }

    @Test
    public void resolvesCorrectBackgroundResources() {
        assertEquals("Capsule shape should resolve to bg_glass_capsule",
                R.drawable.bg_glass_capsule,
                GlassMaterial.getBackgroundRes(GlassMaterial.Shape.CAPSULE));

        assertEquals("Card shape should resolve to bg_glass_card",
                R.drawable.bg_glass_card,
                GlassMaterial.getBackgroundRes(GlassMaterial.Shape.CARD));

        assertEquals("Sheet shape should resolve to bg_mobile_sheet",
                R.drawable.bg_mobile_sheet,
                GlassMaterial.getBackgroundRes(GlassMaterial.Shape.SHEET));

        assertEquals("Capsule accessor should match",
                R.drawable.bg_glass_capsule,
                GlassMaterial.getCapsuleBackgroundRes());

        assertEquals("Card accessor should match",
                R.drawable.bg_glass_card,
                GlassMaterial.getCardBackgroundRes());

        assertEquals("Sheet accessor should match",
                R.drawable.bg_mobile_sheet,
                GlassMaterial.getSheetBackgroundRes());
    }

    @Test
    public void resolvesDrawablesFromContext() {
        Drawable capsule = GlassMaterial.getDrawable(mContext, GlassMaterial.Shape.CAPSULE);
        assertNotNull("Capsule drawable should load successfully", capsule);

        Drawable card = GlassMaterial.getDrawable(mContext, GlassMaterial.Shape.CARD);
        assertNotNull("Card drawable should load successfully", card);

        Drawable sheet = GlassMaterial.getDrawable(mContext, GlassMaterial.Shape.SHEET);
        assertNotNull("Sheet drawable should load successfully", sheet);
    }

    @Test
    public void appliesBackgroundToView() {
        View view = new View(mContext);

        GlassMaterial.applyCapsule(view);
        assertNotNull("View should have background drawable after applyCapsule", view.getBackground());

        GlassMaterial.applyCard(view);
        assertNotNull("View should have background drawable after applyCard", view.getBackground());

        GlassMaterial.applySheet(view);
        assertNotNull("View should have background drawable after applySheet", view.getBackground());
    }

    @Test
    public void applyHandlesNullViewSafely() {
        // Must not throw NullPointerException
        GlassMaterial.apply(null, GlassMaterial.Shape.CAPSULE);
        GlassMaterial.apply(null, GlassMaterial.Shape.SHEET);
        GlassMaterial.applyCapsule(null);
        GlassMaterial.applyCard(null);
        GlassMaterial.applySheet(null);
    }
}
