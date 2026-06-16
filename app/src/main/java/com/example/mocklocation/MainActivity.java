package com.example.mocklocation;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int REQUEST_PERMISSIONS = 1001;
    private static final String PREFS = "mock_location_prefs";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_ACCURACY = "accuracy";

    private EditText latitudeInput;
    private EditText longitudeInput;
    private EditText accuracyInput;
    private TextView statusText;
    private SharedPreferences preferences;
    private final BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!MockLocationService.ACTION_STATUS.equals(intent.getAction())) {
                return;
            }
            String message = intent.getStringExtra(MockLocationService.EXTRA_MESSAGE);
            boolean ok = intent.getBooleanExtra(MockLocationService.EXTRA_OK, false);
            statusText.setText(message == null ? "No status" : message);
            statusText.setTextColor(ok ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        buildUi();
        loadSavedValues();
        requestNeededPermissions();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(MockLocationService.ACTION_STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(statusReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        unregisterReceiver(statusReceiver);
        super.onStop();
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Mock Location");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(17, 24, 39));
        title.setGravity(Gravity.START);
        title.setPadding(0, 0, 0, dp(8));
        root.addView(title, fullWidth());

        TextView subtitle = new TextView(this);
        subtitle.setText("Use Android Developer Options to allow this app as the mock location provider.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.rgb(75, 85, 99));
        subtitle.setPadding(0, 0, 0, dp(18));
        root.addView(subtitle, fullWidth());

        latitudeInput = addInput(root, "Latitude", "31.230416", true);
        longitudeInput = addInput(root, "Longitude", "121.473701", true);
        accuracyInput = addInput(root, "Accuracy meters", "5", false);

        LinearLayout presetRow = new LinearLayout(this);
        presetRow.setOrientation(LinearLayout.HORIZONTAL);
        presetRow.setPadding(0, dp(8), 0, dp(8));
        root.addView(presetRow, fullWidth());

        Button shanghai = addButton(presetRow, "Shanghai");
        shanghai.setOnClickListener(v -> setPreset(31.230416, 121.473701));

        Button beijing = addButton(presetRow, "Beijing");
        beijing.setOnClickListener(v -> setPreset(39.904200, 116.407396));

        Button shenzhen = addButton(presetRow, "Shenzhen");
        shenzhen.setOnClickListener(v -> setPreset(22.543096, 114.057865));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setPadding(0, dp(8), 0, dp(8));
        root.addView(actionRow, fullWidth());

        Button start = addButton(actionRow, "Start");
        start.setOnClickListener(v -> startMocking());

        Button stop = addButton(actionRow, "Stop");
        stop.setOnClickListener(v -> stopMocking());

        Button check = new Button(this);
        check.setText("Check System Location");
        check.setAllCaps(false);
        check.setOnClickListener(v -> checkSystemLocation());
        root.addView(check, fullWidthWithTopMargin(dp(6)));

        Button settings = new Button(this);
        settings.setText("Developer Options");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> openDeveloperOptions());
        root.addView(settings, fullWidthWithTopMargin(dp(6)));

        statusText = new TextView(this);
        statusText.setText("Ready");
        statusText.setTextSize(14);
        statusText.setTextColor(Color.rgb(37, 99, 235));
        statusText.setPadding(0, dp(18), 0, 0);
        root.addView(statusText, fullWidth());

        setContentView(scrollView);
    }

    private EditText addInput(LinearLayout root, String label, String hint, boolean signed) {
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(13);
        labelView.setTextColor(Color.rgb(55, 65, 81));
        labelView.setPadding(0, dp(12), 0, dp(4));
        root.addView(labelView, fullWidth());

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextSize(18);
        int inputType = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
        if (signed) {
            inputType |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        }
        input.setInputType(inputType);
        root.addView(input, fullWidth());
        return input;
    }

    private Button addButton(LinearLayout parent, String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(button, params);
        return button;
    }

    private void loadSavedValues() {
        latitudeInput.setText(preferences.getString(KEY_LATITUDE, "31.230416"));
        longitudeInput.setText(preferences.getString(KEY_LONGITUDE, "121.473701"));
        accuracyInput.setText(preferences.getString(KEY_ACCURACY, "5"));
    }

    private void setPreset(double latitude, double longitude) {
        latitudeInput.setText(String.valueOf(latitude));
        longitudeInput.setText(String.valueOf(longitude));
    }

    private void startMocking() {
        try {
            double latitude = parseDouble(latitudeInput, "Latitude");
            double longitude = parseDouble(longitudeInput, "Longitude");
            float accuracy = (float) parseDouble(accuracyInput, "Accuracy");

            if (latitude < -90 || latitude > 90) {
                show("Latitude must be between -90 and 90.");
                return;
            }
            if (longitude < -180 || longitude > 180) {
                show("Longitude must be between -180 and 180.");
                return;
            }
            if (accuracy <= 0) {
                show("Accuracy must be greater than 0.");
                return;
            }

            preferences.edit()
                    .putString(KEY_LATITUDE, latitudeInput.getText().toString().trim())
                    .putString(KEY_LONGITUDE, longitudeInput.getText().toString().trim())
                    .putString(KEY_ACCURACY, accuracyInput.getText().toString().trim())
                    .apply();

            Intent intent = new Intent(this, MockLocationService.class);
            intent.putExtra(MockLocationService.EXTRA_LATITUDE, latitude);
            intent.putExtra(MockLocationService.EXTRA_LONGITUDE, longitude);
            intent.putExtra(MockLocationService.EXTRA_ACCURACY, accuracy);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            statusText.setText("Mocking location: " + latitude + ", " + longitude);
            statusText.setTextColor(Color.rgb(37, 99, 235));
        } catch (NumberFormatException error) {
            show(error.getMessage());
        }
    }

    private void stopMocking() {
        Intent intent = new Intent(this, MockLocationService.class);
        intent.setAction(MockLocationService.ACTION_STOP);
        startService(intent);
        statusText.setText("Stopped");
    }

    private void checkSystemLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            show("Location permission is not granted.");
            return;
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Location gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location network = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        Location best = gps != null ? gps : network;

        if (best == null) {
            show("System has no last known mock location yet. Tap Start and wait 3 seconds.");
            return;
        }

        boolean mocked;
        if (Build.VERSION.SDK_INT >= 31) {
            mocked = best.isMock();
        } else {
            mocked = best.isFromMockProvider();
        }

        statusText.setText(
                "System location: "
                        + best.getLatitude()
                        + ", "
                        + best.getLongitude()
                        + " | provider="
                        + best.getProvider()
                        + " | mock="
                        + mocked
        );
        statusText.setTextColor(mocked ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));
    }

    private double parseDouble(EditText input, String name) {
        String value = input.getText().toString().trim();
        if (value.isEmpty()) {
            throw new NumberFormatException(name + " is required.");
        }
        return Double.parseDouble(value);
    }

    private void openDeveloperOptions() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (ActivityNotFoundException error) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        addPermissionIfMissing(permissions, Manifest.permission.ACCESS_FINE_LOCATION);
        addPermissionIfMissing(permissions, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            addPermissionIfMissing(permissions, Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    private void addPermissionIfMissing(List<String> permissions, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    private void show(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        statusText.setText(message);
        statusText.setTextColor(Color.rgb(185, 28, 28));
    }

    private LinearLayout.LayoutParams fullWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }

    private LinearLayout.LayoutParams fullWidthWithTopMargin(int topMargin) {
        LinearLayout.LayoutParams params = fullWidth();
        params.setMargins(0, topMargin, 0, 0);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
