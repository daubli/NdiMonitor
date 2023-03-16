package de.daubli.ndimonitor;

import android.app.Application;
import android.content.Context;

public class NdiMonitorApplication extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        NdiMonitorApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return NdiMonitorApplication.context;
    }
}
