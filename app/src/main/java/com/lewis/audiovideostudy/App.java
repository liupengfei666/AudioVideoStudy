package com.lewis.audiovideostudy;

import android.app.Application;

/**
 * author: lewis
 * create by: 19-2-20 下午2:54
 * description:
 */
public class App extends Application {

    private static App mInstance;

    public static App instance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
