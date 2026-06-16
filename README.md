# MockLocationApp

Android mock location app for internal testing. It feeds a chosen latitude and longitude into Android's `gps` and `network` location providers after the app is selected in Developer Options as the mock location app.

This does not hack or rewrite the phone's GNSS hardware position. Android still controls the real location stack, and apps may be able to detect that the location is mocked.

## Build APK on GitHub

1. Create a new GitHub repository.
2. Upload all files in this folder to the repository root.
3. Open the repository's **Actions** tab.
4. Run **Build Android APK** manually, or push to `main`.
5. Download the `mock-location-debug-apk` artifact.
6. Install `app-debug.apk` on your Android phone.

## Use on Android

1. Enable Developer Options.
2. Open **Developer Options**.
3. Set **Select mock location app** to **Mock Location**.
4. Open **Mock Location**.
5. Grant location permission.
6. Enter latitude and longitude.
7. Tap **Start**.

Tap **Stop** when you want Android to return to normal providers.

## Notes

- This app targets Android SDK 35 and uses a foreground location service.
- Some apps reject or flag mock locations. That is expected Android behavior.
- If you need system-level location virtualization for owned test devices, build it as a privileged/system app in your own ROM or device-management environment instead of a normal APK.
