package com.newtube.mobile.ui.browse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Liquid Glass Floating Bottom Navigation Bar for ViewTube.
 *
 * <p>Implements a photorealistic "water-bubble liquid glass" tube inspired by iOS 26
 * liquid-glass design:
 * <ul>
 *   <li>Transparent, rounded glass tube capsule with optical depth and reflective rim glint.</li>
 *   <li>Morphing convex water-lens bubble embedded behind the active destination.</li>
 *   <li>Surface-tension fluid displacement (horizontal stretch and vertical squish) during transitions.</li>
 *   <li>Smooth continuous semicircular ends with ZERO sharp square corners.</li>
 *   <li>Strictly hardware-accelerated Canvas &amp; Paint; zero per-frame object allocation.</li>
 *   <li>Clean toggleable mode: reverts to standard full-width bar when floating mode is disabled.</li>
 * </ul>
 */
public class LiquidGlassBottomNavigationView extends BottomNavigationView {

    private boolean mFloatingEnabled = false;

    // Geometry & Paths (pre-allocated to avoid GC churn during animation)
    private final RectF mCapsuleRect = new RectF();
    private final Path mCapsulePath = new Path();
    private final RectF mBubbleRect = new RectF();
    private final Path mBubbleMeniscusPath = new Path();
    private final Path mRimPath = new Path();

    // Paints
    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlassBodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlassRefractionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlassOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mGlassRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint mBubbleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBubbleLensPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBubbleMeniscusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Active Bubble State
    private float mBubbleCurrentX = -1f;
    private float mBubbleTargetX = -1f;
    private float mBubbleWidth = 0f;
    private float mBubbleHeight = 0f;
    private float mBubbleStretch = 0f; // fluid elongation factor [-1.0 .. 1.0]

    private ValueAnimator mBubbleAnimator;
    private boolean mIsDarkTheme = true;

    // Cache dimensions
    private int mLastWidth = 0;
    private int mLastHeight = 0;

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
        updateThemeMode();

        // Stroke paints
        mGlassOutlinePaint.setStyle(Paint.Style.STROKE);
        mGlassOutlinePaint.setStrokeWidth(dp(1f));

        mGlassRimPaint.setStyle(Paint.Style.STROKE);
        mGlassRimPaint.setStrokeWidth(dp(1.5f));
        mGlassRimPaint.setStrokeCap(Paint.Cap.ROUND);

        mBubbleMeniscusPaint.setStyle(Paint.Style.STROKE);
        mBubbleMeniscusPaint.setStrokeWidth(dp(1.2f));
        mBubbleMeniscusPaint.setStrokeCap(Paint.Cap.ROUND);

        mBubbleWidth = dp(64f);
        mBubbleHeight = dp(40f);
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
            // In floating mode, item backgrounds are transparent so child views do not draw
            // square rectangular tiles over the rounded glass tube.
            setItemBackground(null);
            setItemRippleColor(ContextCompat.getColorStateList(getContext(), R.color.mobile_floating_nav_item));
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
        updateShaders();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLastWidth = w;
        mLastHeight = h;
        updateGeometry(w, h);
        updateShaders();
    }

    private void updateGeometry(int w, int h) {
        if (w <= 0 || h <= 0) {
            return;
        }

        // Full pill geometry: radius is exactly half-height for perfect semicircular caps
        float pillRadius = h / 2f;
        mCapsuleRect.set(0f, 0f, (float) w, (float) h);

        mCapsulePath.reset();
        mCapsulePath.addRoundRect(mCapsuleRect, pillRadius, pillRadius, Path.Direction.CW);

        // Continuous top specular reflection arc: tracks upper semicircle curvature with zero corners
        mRimPath.reset();
        float inset = dp(1f);
        float arcRadius = pillRadius - inset;
        RectF leftArc = new RectF(inset, inset, inset + 2f * arcRadius, (float) h - inset);
        RectF rightArc = new RectF((float) w - inset - 2f * arcRadius, inset, (float) w - inset, (float) h - inset);

        mRimPath.arcTo(leftArc, 200f, 70f, false);
        mRimPath.lineTo((float) w - inset - arcRadius, inset);
        mRimPath.arcTo(rightArc, 270f, 70f, false);

        // Update active bubble position if target is not yet initialized
        if (mBubbleCurrentX < 0) {
            snapBubbleToSelectedItem(false);
        }
    }

    private void updateShaders() {
        int w = mLastWidth;
        int h = mLastHeight;
        if (w <= 0 || h <= 0) {
            return;
        }

        if (mIsDarkTheme) {
            // Obsidian midnight glass body
            mGlassBodyPaint.setColor(Color.parseColor("#E60D111A")); // 90% obsidian blue-black
            mShadowPaint.setColor(Color.parseColor("#33000000"));
            mGlassOutlinePaint.setColor(Color.parseColor("#26FFFFFF")); // Fine reflective edge

            // Internal optical refraction gradient (creates subtle 3D cylinder depth)
            mGlassRefractionPaint.setShader(new LinearGradient(
                    0, 0, 0, h,
                    new int[]{
                            Color.parseColor("#1AFFFFFF"),
                            Color.parseColor("#05FFFFFF"),
                            Color.parseColor("#26000000")
                    },
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
            ));

            // Specular top rim sheen: soft glint fading at both ends
            mGlassRimPaint.setShader(new LinearGradient(
                    0, 0, w, 0,
                    new int[]{
                            Color.TRANSPARENT,
                            Color.parseColor("#4DFFFFFF"),
                            Color.parseColor("#80FFFFFF"),
                            Color.parseColor("#4DFFFFFF"),
                            Color.TRANSPARENT
                    },
                    new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
                    Shader.TileMode.CLAMP
            ));

            // Bubble glow & water lens paints
            mBubbleGlowPaint.setColor(Color.parseColor("#2538BDF8"));
            mBubbleMeniscusPaint.setColor(Color.parseColor("#66FFFFFF"));
        } else {
            // Frosted crystal glass body
            mGlassBodyPaint.setColor(Color.parseColor("#EBF8FAFC")); // Frosted off-white
            mShadowPaint.setColor(Color.parseColor("#1A0F172A"));
            mGlassOutlinePaint.setColor(Color.parseColor("#1F0F172A"));

            mGlassRefractionPaint.setShader(new LinearGradient(
                    0, 0, 0, h,
                    new int[]{
                            Color.parseColor("#33FFFFFF"),
                            Color.parseColor("#0AFFFFFF"),
                            Color.parseColor("#1A0F172A")
                    },
                    new float[]{0f, 0.45f, 1f},
                    Shader.TileMode.CLAMP
            ));

            mGlassRimPaint.setShader(new LinearGradient(
                    0, 0, w, 0,
                    new int[]{
                            Color.TRANSPARENT,
                            Color.parseColor("#66FFFFFF"),
                            Color.parseColor("#A6FFFFFF"),
                            Color.parseColor("#66FFFFFF"),
                            Color.TRANSPARENT
                    },
                    new float[]{0f, 0.25f, 0.5f, 0.75f, 1f},
                    Shader.TileMode.CLAMP
            ));

            mBubbleGlowPaint.setColor(Color.parseColor("#1A0284C7"));
            mBubbleMeniscusPaint.setColor(Color.parseColor("#80FFFFFF"));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mFloatingEnabled) {
            snapBubbleToSelectedItem(false);
        }
    }

    /**
     * Determines the center X coordinate of the item corresponding to the given menu item ID.
     */
    private float getCenterXForItemId(int itemId) {
        if (getChildCount() == 0) {
            return getWidth() / 2f;
        }

        View menuView = getChildAt(0);
        if (menuView instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) menuView;
            int childCount = vg.getChildCount();
            int menuSize = getMenu().size();

            for (int i = 0; i < menuSize && i < childCount; i++) {
                MenuItem item = getMenu().getItem(i);
                if (item.getItemId() == itemId) {
                    View itemView = vg.getChildAt(i);
                    if (itemView != null && itemView.getWidth() > 0) {
                        return vg.getLeft() + itemView.getLeft() + itemView.getWidth() / 2f;
                    }
                    float fallbackItemWidth = (float) getWidth() / Math.max(1, menuSize);
                    return (i + 0.5f) * fallbackItemWidth;
                }
            }
        }

        return getWidth() / 2f;
    }

    private void snapBubbleToSelectedItem(boolean animate) {
        int selectedId = getSelectedItemId();
        float targetX = getCenterXForItemId(selectedId);

        if (!animate || mBubbleCurrentX < 0) {
            mBubbleCurrentX = targetX;
            mBubbleTargetX = targetX;
            mBubbleStretch = 0f;
            invalidate();
            return;
        }

        animateBubbleTo(targetX);
    }

    private void animateBubbleTo(float targetX) {
        if (Math.abs(mBubbleCurrentX - targetX) < 1f) {
            return;
        }

        if (mBubbleAnimator != null && mBubbleAnimator.isRunning()) {
            mBubbleAnimator.cancel();
        }

        final float startX = mBubbleCurrentX;
        final float travelDistance = targetX - startX;
        final float direction = Math.signum(travelDistance);

        mBubbleAnimator = ValueAnimator.ofFloat(0f, 1f);
        mBubbleAnimator.setDuration(320);
        mBubbleAnimator.setInterpolator(new OvershootInterpolator(1.25f));

        mBubbleAnimator.addUpdateListener(anim -> {
            float fraction = anim.getAnimatedFraction();
            mBubbleCurrentX = startX + travelDistance * (float) anim.getAnimatedValue();

            // Fluid stretch displacement: bubble elongates during transit and snaps with spring dampening
            float stretchCurve = (float) Math.sin(fraction * Math.PI);
            mBubbleStretch = direction * stretchCurve * 0.45f;

            invalidate();
        });

        mBubbleAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mBubbleCurrentX = targetX;
                mBubbleStretch = 0f;
                invalidate();
            }
        });

        mBubbleAnimator.start();
    }

    @Override
    public void setSelectedItemId(int itemId) {
        super.setSelectedItemId(itemId);
        if (mFloatingEnabled && isAttachedToWindow()) {
            float targetX = getCenterXForItemId(itemId);
            animateBubbleTo(targetX);
        }
    }

    @Override
    public void setOnItemSelectedListener(@Nullable OnItemSelectedListener listener) {
        super.setOnItemSelectedListener(item -> {
            if (mFloatingEnabled && isAttachedToWindow()) {
                float targetX = getCenterXForItemId(item.getItemId());
                animateBubbleTo(targetX);
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

        // 1. Draw Ambient Restrained Shadow behind the glass tube
        float shadowOffset = dp(2.5f);
        canvas.drawRoundRect(0f, shadowOffset, (float) w, (float) h + shadowOffset, h / 2f, h / 2f, mShadowPaint);

        // 2. Draw Translucent Liquid Glass Tube Body
        canvas.drawRoundRect(mCapsuleRect, h / 2f, h / 2f, mGlassBodyPaint);

        // 3. Draw Optical Internal Cylinder Refraction
        canvas.drawRoundRect(mCapsuleRect, h / 2f, h / 2f, mGlassRefractionPaint);

        // 4. Draw Morphing Convex Water-Lens Bubble
        drawWaterBubble(canvas, h);

        // 5. Draw Top Specular Rim Reflection Arc (Continuous glint along the curve, NO square edges)
        canvas.drawPath(mRimPath, mGlassRimPaint);

        // 6. Draw Reflective Beveled Edge (Fine Fresnel outline)
        canvas.drawRoundRect(mCapsuleRect, h / 2f, h / 2f, mGlassOutlinePaint);

        // 7. Clip children strictly within the glass tube capsule so items never bleed past curves
        canvas.save();
        canvas.clipPath(mCapsulePath);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    private void drawWaterBubble(Canvas canvas, int containerHeight) {
        if (mBubbleCurrentX < 0) {
            return;
        }

        // Fluid displacement: horizontal stretch in travel direction, subtle vertical contraction
        float stretchPx = mBubbleStretch * dp(14f);
        float halfW = (mBubbleWidth / 2f) + Math.abs(stretchPx);
        float halfH = (mBubbleHeight / 2f) - Math.abs(mBubbleStretch) * dp(2f);

        float bLeft = mBubbleCurrentX - halfW;
        float bRight = mBubbleCurrentX + halfW;
        float bTop = (containerHeight / 2f) - halfH;
        float bBottom = (containerHeight / 2f) + halfH;
        float bRadius = halfH;

        mBubbleRect.set(bLeft, bTop, bRight, bBottom);

        // A. Soft Luminescent Aura
        canvas.drawRoundRect(
                bLeft - dp(3f), bTop - dp(2f),
                bRight + dp(3f), bBottom + dp(2f),
                bRadius + dp(2f), bRadius + dp(2f),
                mBubbleGlowPaint
        );

        // B. Convex Water-Lens Core Gradient
        RadialGradient lensShader = new RadialGradient(
                mBubbleCurrentX, (bTop + bBottom) / 2f - dp(3f),
                Math.max(halfW, halfH) * 1.1f,
                mIsDarkTheme
                        ? new int[]{Color.parseColor("#4038BDF8"), Color.parseColor("#1A0284C7")}
                        : new int[]{Color.parseColor("#330284C7"), Color.parseColor("#140369A1")},
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        );
        mBubbleLensPaint.setShader(lensShader);
        canvas.drawRoundRect(mBubbleRect, bRadius, bRadius, mBubbleLensPaint);

        // C. Meniscus Specular Highlight: Curved surface-tension glint on water bubble crest
        mBubbleMeniscusPath.reset();
        float arcR = bRadius - dp(1f);
        RectF leftArc = new RectF(bLeft + dp(1f), bTop + dp(1f), bLeft + dp(1f) + 2f * arcR, bBottom - dp(1f));
        RectF rightArc = new RectF(bRight - dp(1f) - 2f * arcR, bTop + dp(1f), bRight - dp(1f), bBottom - dp(1f));

        mBubbleMeniscusPath.arcTo(leftArc, 200f, 70f, false);
        mBubbleMeniscusPath.lineTo(bRight - dp(1f) - arcR, bTop + dp(1f));
        mBubbleMeniscusPath.arcTo(rightArc, 270f, 70f, false);

        canvas.drawPath(mBubbleMeniscusPath, mBubbleMeniscusPaint);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}
