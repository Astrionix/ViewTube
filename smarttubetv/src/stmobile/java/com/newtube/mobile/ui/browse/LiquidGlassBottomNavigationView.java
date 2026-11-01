package com.newtube.mobile.ui.browse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.newtube.mobile.ui.common.Haptics;

/**
 * Apple Obsidian Black Floating Navigation Dock for ViewTube.
 *
 * <p>Implements a clean, solid Apple-grade dark obsidian navigation dock:
 * <ul>
 *   <li>Solid Obsidian Black surface (#141419) with restrained, subtle hairline outline.</li>
 *   <li>Zero liquid glass shaders, zero caustic reflections, zero water bubble distortion.</li>
 *   <li>Zero circular ripple bubbles on tap; flat and clean touch interaction.</li>
 *   <li>Discrete tactile Apple haptic tick on destination selection.</li>
 *   <li>Strictly hardware-accelerated Canvas &amp; Paint; zero per-frame object allocation.</li>
 *   <li>Clean toggleable mode: reverts to standard full-width bar when floating mode is disabled.</li>
 * </ul>
 */
public class LiquidGlassBottomNavigationView extends BottomNavigationView {

    private boolean mFloatingEnabled = false;

    // Geometry & Paths (pre-allocated to avoid GC churn)
    private final RectF mCapsuleRect = new RectF();
    private final Path mCapsulePath = new Path();

    // Paints
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDockBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDockOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean mIsDarkTheme = true;

    public LiquidGlassBottomNavigationView(@NonNull Context context) {
        this(context, null);
    }

    public LiquidGlassBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.bottomNavigationStyle);
    }

    public LiquidGlassBottomNavigationView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setWillNotDraw(false);
        setBackground(null);
        setBackgroundColor(Color.TRANSPARENT);
        updateThemeMode();

        mDockOutlinePaint.setStyle(Paint.Style.STROKE);
        mDockOutlinePaint.setStrokeWidth(dp(1f));

        // Disable circular ripple bubbles
        setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        setItemBackground(null);
    }

    private void updateThemeMode() {
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        mIsDarkTheme = (nightMode == Configuration.UI_MODE_NIGHT_YES);
    }

    public void setFloatingEnabled(boolean enabled) {
        if (mFloatingEnabled == enabled) {
            return;
        }
        mFloatingEnabled = enabled;
        updateThemeMode();

        if (mFloatingEnabled) {
            setBackground(null);
            setBackgroundColor(Color.TRANSPARENT);
            setItemBackground(null);
            // Disable circular ripple bubble effect when tapping
            setItemRippleColor(ColorStateList.valueOf(Color.TRANSPARENT));
        }
        invalidate();
    }

    public boolean isFloatingEnabled() {
        return mFloatingEnabled;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateThemeMode();
        updateColors();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGeometry(w, h);
        updateColors();
    }

    private void updateGeometry(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }

        float pillRadius = h / 2f;
        mCapsuleRect.set(0f, 0f, (float) w, (float) h);

        mCapsulePath.reset();
        mCapsulePath.addRoundRect(mCapsuleRect, pillRadius, pillRadius, Path.Direction.CW);
    }

    private void updateColors() {
        if (mIsDarkTheme) {
            // Apple Obsidian Black Palette: Translucent deep obsidian backdrop and surface
            mDockBodyPaint.setColor(Color.argb(235, 20, 20, 25));
            mDockOutlinePaint.setColor(Color.argb(80, 255, 255, 255));
            mShadowPaint.setColor(Color.argb(80, 0, 0, 0));
        } else {
            // Apple System Light Palette: Translucent frosted white glass surface
            mDockBodyPaint.setColor(Color.argb(238, 255, 255, 255));
            mDockOutlinePaint.setColor(Color.argb(35, 0, 0, 0));
            mShadowPaint.setColor(Color.argb(25, 0, 0, 0));
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Flat, clean touch dispatch without squish or liquid scale
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void setSelectedItemId(int itemId) {
        super.setSelectedItemId(itemId);
        if (mFloatingEnabled && isAttachedToWindow()) {
            Haptics.tick(this);
        }
    }

    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        super.setOnItemSelectedListener(item -> {
            if (mFloatingEnabled && isAttachedToWindow()) {
                Haptics.tick(this);
            }
            return listener == null || listener.onNavigationItemSelected(item);
        });
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (!mFloatingEnabled) {
            super.dispatchDraw(canvas);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            super.dispatchDraw(canvas);
            return;
        }

        float pillRadius = h / 2f;

        // 1. Ambient Apple Drop Shadow
        canvas.drawRoundRect(0f, dp(2f), (float) w, (float) h + dp(3f), pillRadius, pillRadius, mShadowPaint);

        // 2. Solid Apple Obsidian Body (pure obsidian black surface, no liquid, no shaders)
        canvas.drawRoundRect(mCapsuleRect, pillRadius, pillRadius, mDockBodyPaint);

        // 3. Hairline Outline
        canvas.drawRoundRect(mCapsuleRect, pillRadius, pillRadius, mDockOutlinePaint);

        // 4. Clip children strictly within the capsule
        canvas.save();
        canvas.clipPath(mCapsulePath);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
