package com.serenegiant.utils;

import android.os.Build.VERSION;

public final class BuildCheck {
    public BuildCheck() {
    }

    private static final boolean check(int value) {
        return VERSION.SDK_INT >= value;
    }

    public static boolean isLollipop() {
        return VERSION.SDK_INT >= 21;
    }

    public static boolean isAndroid5() {
        return check(21);
    }

    public static boolean isMarshmallow() {
        return check(23);
    }

}

