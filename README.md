# DavinAutoSwipe — Android Auto-Scroll

A native, no-root Android app that **swipes UP after a user-chosen interval** (e.g., every **55 seconds**) while the Facebook/Facebook Lite or Shopee PH app is in the foreground. It shows a movable controller with a countdown, **PAUSE / RESUME**, and **STOP**.

**Important:** This package is an **Android Studio source project**, **not** an APK. The authoring environment did not have the Android SDK, so the project has not been compiled or device-tested. See the build options below.

## Functionality

- Set any whole interval from **5 to 3600 seconds**, presets 15/30/55/60.
- Check **Facebook Reels** and/or **Shopee PH Videos**.
- **Starts only after the user presses Start** in the main screen.
- Checks Android's visible foreground package before swiping; if you leave both apps, countdown resets and waits.
- Floating accessibility overlay allows pause/resume/stop; it can be dragged to another location.
- Screen-off / locked state pauses the countdown.
- Uses upward gestures, not app APIs. **Open the Reels / Shopee Videos feed yourself.** It **cannot know whether you're on a video screen** within the selected app; swiping on shopping pages or feed pages may scroll those pages instead. Stop before opening other screens in Shopee.
- Does not automatically like, follow, buy, redeem coins, or interact with payment controls.
- **No INTERNET permission**, no user login, and no third-party libraries.

## Recommended: Get an APK using GitHub Actions (no Android Studio needed)

1. Create a free new GitHub repository named `DavinAutoSwipe`.
2. Upload **all files from inside the DavinAutoSwipe-Android folder**, preserving `.github/workflows/build-apk.yml`. GitHub's web uploader can upload multiple files; for hidden `.github` folder, use Git/GitHub Desktop or create the workflow file directly in the GitHub web editor. Do not upload just the ZIP itself.
3. Open the repository's **Actions** tab, select **Build DavinAutoSwipe APK**, and click **Run workflow**.
4. After a successful run, download the **`app-debug.apk`** workflow artifact (direct APK download; no need to unzip).
5. Install the downloaded `app-debug.apk` on your Android phone, after reviewing and trusting the code/permissions. A debug APK can be installed for personal testing; it is not a Play Store production release.

## Build with Android Studio

- Install recent Android Studio, **Java 17**, Android API **35** SDK and Build Tools.
- This source project uses Android Gradle Plugin **8.9.2** and Gradle **8.11.1**.
- If your Android Studio does not automatically initialize Gradle for this source archive, use the Android Studio Terminal with Gradle 8.11.1 available on your computer to run `gradle wrapper --gradle-version 8.11.1` once, then re-sync.
- Open the folder `DavinAutoSwipe-Android` using **File > Open**.
- Use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
- The APK will be created at `app/build/outputs/apk/debug/app-debug.apk`.

## Setup on a phone (including Xiaomi Redmi)

1. Install and launch DavinAutoSwipe.
2. Choose an interval, e.g. `55`, and Facebook and/or Shopee.
3. Read the in-app disclosure; tick the consent checkbox if you agree.
4. Tap **Open Accessibility Settings**. Enable **DavinAutoSwipe Auto Scroll** in Android's Accessibility services, then return to DavinAutoSwipe.
5. Tap **START AUTO-SCROLL**, open Facebook Reels or Shopee Videos, and leave the phone on. At your selected interval, DavinAutoSwipe swipes upward to the next video.
6. Tap the floating **PAUSE / RESUME** or **STOP** when you wish.

### Android 13+ restricted settings

Android sometimes blocks accessibility services from sideloaded APKs. If you see "Restricted setting" after attempting to activate it, go to **Settings > Apps > Manage apps > DavinAutoSwipe > ⋮ > Allow restricted settings** (exact path may differ by manufacturer), then return to Accessibility settings and enable the service. Review the code and only allow this if you trust the app; accessibility access is powerful. See the official Android guidance: https://support.google.com/android/answer/12623953

### If auto-swipe stops unexpectedly

- Keep the phone awake and unlocked.
- Xiaomi/HyperOS may restrict background apps. Check DavinAutoSwipe's battery restrictions, but only change these settings if necessary.
- Check that you are viewing the **official Facebook, Facebook Lite, or Shopee PH Android app**, rather than a web browser or different regional Shopee app.
- The floating controller should be visible while Start is active; if it is absent, check Accessibility is enabled.

## Privacy and security

- The accessibility service is **not started automatically** when enabled and never resumes itself after reboot.
- It only reads the **foreground app's package name** to prevent swiping unrelated apps.
- Screen text, passwords, messages, screenshots, video files and account details are **not read or stored**. Android gives accessibility services the technical ability to interact broadly, so only enable it if you trust the source.
- Nothing is transmitted, since the app requests **no internet permission**.
- Users explicitly consent via the main interface, can stop at any time, and can disable the service entirely in Accessibility settings.

## Structure

- `app/src/main/java/ph/davin/autoswipe/MainActivity.java` — settings and consent UI.
- `app/src/main/java/ph/davin/autoswipe/AutoScrollService.java` — countdown, swipe gesture, foreground checking and floating controls.
- `app/src/main/res/xml/accessibility_service_config.xml` — Android gesture capability.
- `.github/workflows/build-apk.yml` — cloud APK build (free GitHub public repository actions quotas apply).
