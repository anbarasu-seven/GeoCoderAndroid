package com.example.mylocation;

import android.util.Log;

public class Logger {

    private static final boolean DEBUG = true;

    public static final void D(String TAG, String message) {
        if (DEBUG) {
            Log.d("" + TAG, message);
        }
    }

    public static final void E(String TAG, String message) {
        if (DEBUG) {
            Log.e("" + TAG, message);
        }
    }

}
