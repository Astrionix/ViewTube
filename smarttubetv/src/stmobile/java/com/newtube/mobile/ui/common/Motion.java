package com.newtube.mobile.ui.common;

import android.graphics.Path;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * NEWTUBE(motion): the app's motion tokens - Material 3 easing curves and the few durations the
 * phone UI shares, so every screen moves the same way. Things that arrive decelerate (EMPHASIZED_
 * DECELERATE), things that leave accelerate (EMPHASIZED_ACCELERATE), things that move from one
 * resting place to another use EMPHASIZED, and small fades use STANDARD. Nothing here is slower than
 * what it replaced: the point of the curves is that the first frames move a lot, so a transition
 * reads as fast even at 250-300 ms.
 */
public final class Motion {
    /** Material 3 emphasized: the two-segment path, not the (0.2, 0, 0, 1) approximation. */
    public static final Interpolator EMPHASIZED = new PathInterpolator(emphasizedPath());
    public static final Interpolator EMPHASIZED_DECELERATE = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    public static final Interpolator EMPHASIZED_ACCELERATE = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);
    public static final Interpolator STANDARD = new PathInterpolator(0.2f, 0f, 0f, 1f);
    public static final Interpolator STANDARD_DECELERATE = new PathInterpolator(0f, 0f, 0f, 1f);
    public static final Interpolator STANDARD_ACCELERATE = new PathInterpolator(0.3f, 0f, 1f, 1f);

    /** Apple Fluid Interface: Canonical natural deceleration curve (WWDC Designing Fluid Interfaces). */
    public static final Interpolator APPLE_DEFAULT = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    /** Apple Fluid Interface: Drawer and bottom sheet curve with slight momentum snap. */
    public static final Interpolator APPLE_SHEET = new PathInterpolator(0.32f, 0.72f, 0f, 1f);

    /**
     * Apple Fluid Interface: Projects initial release velocity into distance using exponential decay.
     * Deceleration rate is ~0.998 for standard scroll/flick inertia; 0.99 for snappier gestures.
     *
     * @param initialVelocity units per second (e.g. px/s)
     * @param decelerationRate decay constant (typically 0.998f)
     * @return projected travel distance in same units
     */
    public static float project(float initialVelocity, float decelerationRate) {
        return (initialVelocity / 1000f) * decelerationRate / (1f - decelerationRate);
    }

    public static float project(float initialVelocity) {
        return project(initialVelocity, 0.998f);
    }

    /**
     * Apple Fluid Interface: Continuous progressive resistance at a boundary.
     * The further past the boundary the finger pulls, the stiffer the resistance.
     *
     * @param overshoot distance dragged past boundary (positive or negative)
     * @param dimension width or height of the viewport/container
     * @param constant resistance factor (Apple standard is 0.55f)
     * @return resisted displacement
     */
    public static float rubberband(float overshoot, float dimension, float constant) {
        return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
    }

    public static float rubberband(float overshoot, float dimension) {
        return rubberband(overshoot, dimension, 0.55f);
    }

    /** Content that swaps in place (text, counts, a list replacing its skeleton). */
    public static final long FADE_IN_MS = 150;
    /** Content leaving before other content takes its place (the first half of a fade-through). */
    public static final long FADE_OUT_MS = 90;
    /** A surface arriving (a sheet sliding up). */
    public static final long ENTER_MS = 250;
    /** A surface leaving (a sheet sliding down). */
    public static final long EXIT_MS = 200;

    /** The like "pop": a quick press-in, overshoot and settle (the comments' thumb uses the same). */
    public static final long POP_MS = 260;

    private Motion() {
    }

    /**
     * NEWTUBE(motion): pop {@code view} - scale to 78%, overshoot to 112%, settle - for an action
     * that just took effect under the finger (a like, a dislike).
     */
    public static void pop(android.view.View view) {
        if (view == null) {
            return;
        }
        android.animation.Keyframe k0 = android.animation.Keyframe.ofFloat(0f, 1f);
        android.animation.Keyframe k1 = android.animation.Keyframe.ofFloat(0.35f, 0.78f);
        android.animation.Keyframe k2 = android.animation.Keyframe.ofFloat(0.75f, 1.12f);
        android.animation.Keyframe k3 = android.animation.Keyframe.ofFloat(1f, 1f);
        android.animation.ObjectAnimator pop = android.animation.ObjectAnimator.ofPropertyValuesHolder(view,
                android.animation.PropertyValuesHolder.ofKeyframe(android.view.View.SCALE_X, k0, k1, k2, k3),
                android.animation.PropertyValuesHolder.ofKeyframe(android.view.View.SCALE_Y, k0, k1, k2, k3));
        pop.setDuration(POP_MS);
        pop.setInterpolator(STANDARD);
        pop.start();
    }

    /**
     * NEWTUBE(haptics): a damped spring from one value to another, solved in closed form and played
     * as an interpolator, so an ordinary ValueAnimator runs it (its cancel and end handling stay as
     * they are). This is how the Pixel settles what a finger lets go of - the launcher's recents
     * flick and SystemUI's notification rows are springs that start at the finger's speed, and a
     * spring below critical damping lands with a small settle past its target.
     *
     * <p>{@link #durationMs} is when the spring stays within {@code precision} of its target.</p>
     */
    public static final class Spring implements Interpolator {
        public final long durationMs;
        private final float mFrom;
        private final float mTo;
        private final double mX0;
        private final double mV0;
        private final double mOmega;
        private final double mZeta;

        /** {@code velocity}: units per second, positive in the direction of increasing value. */
        public Spring(float from, float to, float velocity, float stiffness, float dampingRatio,
                float precision) {
            mFrom = from;
            mTo = to;
            mX0 = from - to;
            mV0 = velocity;
            mOmega = Math.sqrt(stiffness);
            mZeta = dampingRatio;
            long settled = 0;
            if (Math.abs(to - from) > 1e-6f) {
                for (int ms = 1; ms <= 2000; ms++) {
                    if (Math.abs(offset(ms / 1000.0)) > precision) {
                        settled = ms;
                    }
                }
            }
            durationMs = settled;
        }

        /** Displacement from the target after {@code t} seconds. */
        private double offset(double t) {
            if (mZeta < 1.0) {
                double damped = mOmega * Math.sqrt(1.0 - mZeta * mZeta);
                return Math.exp(-mZeta * mOmega * t) * (mX0 * Math.cos(damped * t)
                        + (mV0 + mZeta * mOmega * mX0) / damped * Math.sin(damped * t));
            }
            return (mX0 + (mV0 + mOmega * mX0) * t) * Math.exp(-mOmega * t);
        }

        @Override
        public float getInterpolation(float input) {
            if (input >= 1f || durationMs == 0) {
                return 1f;
            }
            double value = mTo + offset(input * durationMs / 1000.0);
            return (float) ((value - mFrom) / (mTo - mFrom));
        }
    }

    private static Path emphasizedPath() {
        Path path = new Path();
        path.moveTo(0f, 0f);
        path.cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f);
        path.cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f);
        return path;
    }
}
