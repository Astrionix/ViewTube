package com.newtube.mobile.ui.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Shared Liquid Glass design foundation helper for ViewTube.
 *
 * <p>Exposes safe styling defaults and background drawables for Liquid Glass capsule, card,
 * and bottom sheet surfaces. Compatible with Android 7.0+ (API 24+) without external dependencies.</p>
 */
public final class GlassMaterial {

    public enum Shape {
        CAPSULE,
        CARD,
        SHEET
    }

    private GlassMaterial() {}

    /**
     * Resolves the drawable resource ID for the requested glass shape.
     */
    @DrawableRes
    public static int getBackgroundRes(@NonNull Shape shape) {
        if (shape == Shape.CARD) {
            return R.drawable.bg_glass_card;
        } else if (shape == Shape.SHEET) {
            return R.drawable.bg_mobile_sheet;
        }
        return R.drawable.bg_glass_capsule;
    }

    /**
     * Resolves the drawable resource ID for a Liquid Glass capsule.
     */
    @DrawableRes
    public static int getCapsuleBackgroundRes() {
        return R.drawable.bg_glass_capsule;
    }

    /**
     * Resolves the drawable resource ID for a Liquid Glass card.
     */
    @DrawableRes
    public static int getCardBackgroundRes() {
        return R.drawable.bg_glass_card;
    }

    /**
     * Resolves the drawable resource ID for a Liquid Glass bottom sheet.
     */
    @DrawableRes
    public static int getSheetBackgroundRes() {
        return R.drawable.bg_mobile_sheet;
    }

    /**
     * Obtains the drawable instance for the requested glass shape.
     */
    @Nullable
    public static Drawable getDrawable(@NonNull Context context, @NonNull Shape shape) {
        return ContextCompat.getDrawable(context, getBackgroundRes(shape));
    }

    /**
     * Applies the Liquid Glass capsule material to the given view.
     */
    public static void applyCapsule(@Nullable View view) {
        apply(view, Shape.CAPSULE);
    }

    /**
     * Applies the Liquid Glass card material to the given view.
     */
    public static void applyCard(@Nullable View view) {
        apply(view, Shape.CARD);
    }

    /**
     * Applies the Liquid Glass sheet material to the given view.
     */
    public static void applySheet(@Nullable View view) {
        apply(view, Shape.SHEET);
    }

    /**
     * Applies the requested Liquid Glass material shape to the given view.
     */
    public static void apply(@Nullable View view, @NonNull Shape shape) {
        if (view == null) {
            return;
        }
        view.setBackgroundResource(getBackgroundRes(shape));
    }
}
