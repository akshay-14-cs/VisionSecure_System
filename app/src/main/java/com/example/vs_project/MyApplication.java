package com.example.vs_project;

import android.app.Application;
import com.example.vs_project.utils.ThemeUtils;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ThemeUtils.applyTheme(this);
    }
}
