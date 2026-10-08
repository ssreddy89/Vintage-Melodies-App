# Vintage Melodies Android

A polished Android WebView application that wraps the [Vintage Melodies](https://vintage-melodies-kappa.vercel.app/) web player in a native Android experience.

## Website

**Live:** https://vintage-melodies-kappa.vercel.app/

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI | Android Views / XML |
| Architecture | WebView wrapper |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 35 (Android 15) |
| Build System | Gradle (Kotlin DSL) |

## How to Open

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended).
2. Open Android Studio → **File → Open**.
3. Navigate to `D:\POC Projects\Vintage Melodies Android`.
4. Click **OK** and wait for Gradle sync to complete.

## How to Run

### On a Physical Android Phone

1. Enable **Developer Options** on your Android phone:
   - Go to **Settings → About Phone** → tap **Build Number** 7 times.
2. Enable **USB Debugging** in **Settings → Developer Options**.
3. Connect your phone via USB cable.
4. In Android Studio, select your device from the device dropdown.
5. Click the **Run ▶** button (or press `Shift+F10`).

### On an Emulator

1. In Android Studio, open **Device Manager** (right sidebar).
2. Create a virtual device (Pixel 6 or similar recommended).
3. Select the device and click **Run ▶**.

## How to Build Debug APK

### Via Android Studio

1. Open the project in Android Studio.
2. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
3. Wait for the build to complete.
4. Click **locate** in the notification to find the APK.

### Via Command Line

```bash
cd "D:\POC Projects\Vintage Melodies Android"
.\gradlew assembleDebug
```

## How to Build Release APK

### Step 1: Create a Keystore (One Time Only)

```bash
keytool -genkey -v -keystore vintage-melodies-release.keystore -alias vintage-melodies -keyalg RSA -keysize 2048 -validity 10000
```

Store the keystore file in a **safe location outside the project directory**.

### Step 2: Create `keystore.properties`

Create a file called `keystore.properties` in the project root (this file is gitignored):

```properties
storeFile=C:/path/to/your/vintage-melodies-release.keystore
storePassword=your_keystore_password
keyAlias=vintage-melodies
keyPassword=your_key_password
```

### Step 3: Build Release APK

```bash
.\gradlew assembleRelease
```

## APK Location

| Build Type | Location |
|------------|----------|
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/app-release.apk` |

## How Website Updates Work

The Android app loads the website from Vercel via WebView. When you deploy changes to the Vintage Melodies web project:

1. Push changes to the web project repository.
2. Vercel automatically deploys the updated website.
3. The Android app loads the latest version on next launch — **no APK rebuild needed**.

### Changes that propagate automatically (no new APK):
- HTML / CSS / JavaScript updates
- New images or artwork
- New playlists and songs
- UI redesigns
- New web pages
- Audio source changes
- API endpoint changes (server-side)

## When a New APK Is Required

A new APK build is needed only for **native Android changes**:

- Changing the app icon or splash screen
- Adding new Android permissions
- Changing the WebView URL
- Updating the Android SDK target version
- Modifying native Android behavior (back button, error handling)
- Changing the app name or package name
- Adding native Android features (notifications, services, etc.)

## Signing / Keystore

> **⚠️ IMPORTANT:** Never commit your keystore file or `keystore.properties` to version control.

- The `.gitignore` file excludes `*.keystore`, `*.jks`, and `keystore.properties`.
- Store your release keystore in a secure location.
- Back up the keystore — if lost, you cannot update the app with the same signing key.
- The debug APK uses Android Studio's auto-generated debug keystore.

## Troubleshooting

### App shows blank white screen
- Check your internet connection.
- Verify https://vintage-melodies-kappa.vercel.app/ is accessible in a browser.
- Clear the app's cache and data in Android Settings.

### Audio doesn't play
- Ensure your device volume is not muted.
- Check that the website's audio works in a mobile browser first.
- Some devices require a user interaction before audio can play.

### Gradle sync fails
- Ensure you have Android Studio Ladybug (2024.2) or newer.
- Check that your JDK version is 17 or higher.
- Try **File → Invalidate Caches / Restart**.

### Back button closes the app immediately
- This is expected behavior when there is no WebView navigation history.
- Navigate within the website first, then the back button will go back in history.

### Release build fails
- Verify `keystore.properties` exists in the project root.
- Verify the keystore file path in `keystore.properties` is correct.
- Verify the passwords are correct.

### Website looks zoomed in or out
- The website is responsive and should adapt automatically.
- If issues persist, check that the website's viewport meta tag is correct.

## Project Structure

```
Vintage Melodies Android/
├── app/
│   ├── src/main/
│   │   ├── kotlin/com/vintagemelodies/app/
│   │   │   ├── MainActivity.kt      # WebView host activity
│   │   │   ├── SplashActivity.kt     # Splash + network check
│   │   │   └── NetworkUtils.kt       # Connectivity helper
│   │   ├── res/
│   │   │   ├── drawable/             # Icons, backgrounds
│   │   │   ├── layout/               # XML layouts
│   │   │   ├── mipmap-anydpi-v26/    # Adaptive launcher icon
│   │   │   ├── values/               # Colors, strings, themes
│   │   │   └── xml/                  # Network security config
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/wrapper/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
└── README.md
```

## License

This Android wrapper is part of the Vintage Melodies project.
