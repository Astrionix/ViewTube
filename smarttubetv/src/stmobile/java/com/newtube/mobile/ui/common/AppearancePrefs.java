package com.newtube.mobile.ui.common;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages appearance and navigation presentation preferences, including the optional
 * Floating Bottom Navigation Bar.
 */
public final class AppearancePrefs {
    private static final String PREFS_NAME = "newtube_appearance";
    private static final String KEY_FLOATING_BOTTOM_BAR = "floating_bottom_bar";
    private static final String KEY_FLOATING_HEADER = "floating_header";

    public interface OnAppearanceChangeListener {
        void onAppearanceChanged();
    }

    private static final List<WeakReference<OnAppearanceChangeListener>> sListeners = new ArrayList<>();

    private AppearancePrefs() {
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Default behavior: false (preserve conventional full-width header).
     */
    public static boolean isFloatingHeaderEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_FLOATING_HEADER, false);
    }

    public static void setFloatingHeaderEnabled(Context context, boolean enabled) {
        if (isFloatingHeaderEnabled(context) == enabled) {
            return;
        }
        getPrefs(context).edit().putBoolean(KEY_FLOATING_HEADER, enabled).apply();
        notifyListeners();
    }

    /**
     * Default behavior: false (preserve conventional, full-width YouTube-style bottom bar).
     * The floating design is independently controlled by this setting.
     */
    public static boolean isFloatingBottomBarEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_FLOATING_BOTTOM_BAR, false);
    }

    public static void setFloatingBottomBarEnabled(Context context, boolean enabled) {
        if (isFloatingBottomBarEnabled(context) == enabled) {
            return;
        }
        getPrefs(context).edit().putBoolean(KEY_FLOATING_BOTTOM_BAR, enabled).apply();
        notifyListeners();
    }

    public static synchronized void addListener(OnAppearanceChangeListener listener) {
        if (listener == null) {
            return;
        }
        // Clean dead references and avoid duplicate registrations
        sListeners.removeIf(ref -> ref.get() == null || ref.get() == listener);
        sListeners.add(new WeakReference<>(listener));
    }

    public static synchronized void removeListener(OnAppearanceChangeListener listener) {
        if (listener == null) {
            return;
        }
        sListeners.removeIf(ref -> ref.get() == null || ref.get() == listener);
    }

    private static synchronized void notifyListeners() {
        sListeners.removeIf(ref -> {
            OnAppearanceChangeListener listener = ref.get();
            if (listener != null) {
                listener.onAppearanceChanged();
                return false;
            }
            return true;
        });
    }
}
