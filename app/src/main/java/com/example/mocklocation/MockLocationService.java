package com.example.mocklocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;

public class MockLocationService extends Service {
    public static final String ACTION_STOP = "com.example.mocklocation.STOP";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_ACCURACY = "extra_accuracy";

    private static final String CHANNEL_ID = "mock_location";
    private static final int NOTIFICATION_ID = 2001;
    private static final long UPDATE_INTERVAL_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                pushMockLocation(LocationManager.GPS_PROVIDER);
                pushMockLocation(LocationManager.NETWORK_PROVIDER);
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            } catch (SecurityException error) {
                stopSelf();
            } catch (IllegalArgumentException error) {
                stopSelf();
            }
        }
    };

    private LocationManager locationManager;
    private double latitude = 31.230416;
    private double longitude = 121.473701;
    private float accuracy = 5f;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            latitude = intent.getDoubleExtra(EXTRA_LATITUDE, latitude);
            longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, longitude);
            accuracy = intent.getFloatExtra(EXTRA_ACCURACY, accuracy);
        }

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startForeground(NOTIFICATION_ID, buildNotification("Mocking " + latitude + ", " + longitude));

        try {
            prepareProvider(LocationManager.GPS_PROVIDER);
            prepareProvider(LocationManager.NETWORK_PROVIDER);
            handler.removeCallbacks(updateRunnable);
            updateRunnable.run();
        } catch (SecurityException error) {
            stopSelf();
        } catch (IllegalArgumentException error) {
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(updateRunnable);
        clearProvider(LocationManager.GPS_PROVIDER);
        clearProvider(LocationManager.NETWORK_PROVIDER);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void prepareProvider(String provider) {
        try {
            locationManager.addTestProvider(
                    provider,
                    false,
                    false,
                    false,
                    false,
                    true,
                    true,
                    true,
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
            );
        } catch (IllegalArgumentException ignored) {
            // The provider may already exist from a previous service run.
        }
        locationManager.setTestProviderEnabled(provider, true);
    }

    private void pushMockLocation(String provider) {
        Location location = new Location(provider);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        location.setAltitude(0d);
        location.setBearing(0f);
        location.setSpeed(0f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            location.setVerticalAccuracyMeters(accuracy);
            location.setSpeedAccuracyMetersPerSecond(0.1f);
            location.setBearingAccuracyDegrees(0.1f);
        }

        locationManager.setTestProviderLocation(provider, location);
    }

    private void clearProvider(String provider) {
        if (locationManager == null) {
            return;
        }
        try {
            locationManager.clearTestProviderLocation(provider);
        } catch (Exception ignored) {
        }
        try {
            locationManager.removeTestProvider(provider);
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification(String text) {
        createNotificationChannel();
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle("Mock Location Running")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Mock Location",
                NotificationManager.IMPORTANCE_LOW
        );
        manager.createNotificationChannel(channel);
    }
}
