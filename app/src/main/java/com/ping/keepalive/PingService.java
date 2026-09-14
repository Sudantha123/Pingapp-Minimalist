package com.ping.keepalive;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class PingService extends Service {

    public static boolean isRunning = false;
    private PowerManager.WakeLock wakeLock;

    // Load the C library
    static {
        System.loadLibrary("native-engine");
    }

    // JNI C Functions
    public native void startNativePing(String host, int delay);
    public native void stopNativePing();

    @Override
    public void onCreate() {
        super.onCreate();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KeepAlive::NativeWakeLock");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("NativePing", "Native Ping", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, "NativePing")
                    .setContentTitle("Termux-Level Engine Active")
                    .setContentText("0% CPU | Zero Java Objects")
                    .setSmallIcon(android.R.drawable.ic_menu_upload);
            startForeground(1, builder.build());
        }

        if (!isRunning && intent != null) {
            isRunning = true;
            wakeLock.acquire();
            
            String host = intent.getStringExtra("HOST");
            int delay = intent.getIntExtra("DELAY", 15);
            
            // Start the Pure C Loop in background!
            startNativePing(host, delay);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        stopNativePing(); // Stop C Engine
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
