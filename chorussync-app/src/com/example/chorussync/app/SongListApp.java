package com.example.chorussync.app;

import org.searchordsmen.chorussync.lib.HttpUtils;

import roboguice.RoboGuice;
import android.app.Application;

import com.google.inject.Binder;
import com.google.inject.Module;

public class SongListApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        HttpUtils.setupCookieManager();
        
        RoboGuice.setBaseApplicationInjector(this, RoboGuice.DEFAULT_STAGE, 
                RoboGuice.newDefaultRoboModule(this),
                new Module() {
                    public void configure(Binder b) {
                    }
                });
    }
}
