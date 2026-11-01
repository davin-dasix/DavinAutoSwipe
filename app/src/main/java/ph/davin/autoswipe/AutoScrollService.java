package ph.davin.autoswipe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A user-initiated, deterministic auto-scroll helper. Only dispatches an UP swipe
 * when Facebook or Shopee PH is visibly in the foreground.
 *
 * No network, telemetry, credentials, or screen-content storage.
 */
public class AutoScrollService extends AccessibilityService {

    public static volatile AutoScrollService instance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private WindowManager.LayoutParams overlayParams;
    private View overlay;
    private TextView countdownLabel;
    private TextView secondaryLabel;
    private TextView pauseButton;

    private boolean running = false;
    private boolean paused = false;
    private boolean swiping = false;
    private boolean enableFacebook = true;
    private boolean enableShopee = true;
    private int intervalSeconds = 55;
    private long deadline = 0L;
    private int swipeCount = 0;
    private float lastTouchX;
    private float lastTouchY;
    private int originX;
    private int originY;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            if (!running) return;

            if (paused) {
                deadline = 0L;
                updateOverlay("Paused", "Tap RESUME to continue");
            } else if (!screenReady()) {
                deadline = 0L;
                updateOverlay("Waiting for video", "Open the selected app");
            } else if (!targetForeground()) {
                deadline = 0L;
                updateOverlay("Waiting for video", "Open Facebook or Shopee videos");
            } else if (!swiping) {
                long now = SystemClock.elapsedRealtime();
                if (deadline == 0L) deadline = now + intervalSeconds * 1000L;
                long remainingMs = Math.max(0L, deadline - now);
                if (remainingMs == 0L) {
                    doSwipe();
                } else {
                    long secondsLeft = (remainingMs + 999L) / 1000L;
                    updateOverlay("Next in " + secondsLeft + "s", "Swipes: " + swipeCount);
                }
            }
            handler.postDelayed(this, 250L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        // Deliberately do not auto-start. Only the user can start it from the app.
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Event content is intentionally not read or retained.
        // Package is checked at swipe time via rootInActiveWindow.
    }

    @Override public void onInterrupt() {
        pauseAutomation();
    }

    @Override public void onDestroy() {
        stopAutomation();
        instance = null;
        super.onDestroy();
    }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }

    public void startAutomation(int seconds, boolean facebook, boolean shopee) {
        if (!getSharedPreferences("davinautoswipe", MODE_PRIVATE).getBoolean("consent", false)) return;
        if (!facebook && !shopee) return;
        intervalSeconds = Math.max(5, Math.min(3600, seconds));
        enableFacebook = facebook;
        enableShopee = shopee;
        paused = false;
        swiping = false;
        swipeCount = 0;
        deadline = 0L;
        running = true;
        showOverlay();
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    public void stopAutomation() {
        running = false;
        paused = false;
        deadline = 0L;
        handler.removeCallbacks(ticker);
        removeOverlay();
    }

    private void pauseAutomation() {
        if (!running) return;
        paused = true;
        deadline = 0L;
        refreshPauseButton();
        updateOverlay("Paused", "Tap RESUME to continue");
    }

    private void togglePause() {
        if (!running) return;
        paused = !paused;
        deadline = 0L;
        refreshPauseButton();
    }

    private boolean screenReady() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return pm != null && pm.isInteractive() && (km == null || !km.isKeyguardLocked());
    }

    private boolean targetForeground() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null || root.getPackageName() == null) return false;
            String pkg = root.getPackageName().toString();
            return (enableFacebook && (pkg.equals("com.facebook.katana") || pkg.equals("com.facebook.lite")))
                    || (enableShopee && pkg.equals("com.shopee.ph"));
        } catch (Exception e) {
            return false; // On uncertainty, never swipe.
        }
    }

    private void doSwipe() {
        if (!running || paused || !screenReady() || !targetForeground() || swiping) {
            deadline = 0L;
            return;
        }
        swiping = true;
        deadline = 0L;
        updateOverlay("Swiping up...", "Moving to the next video");

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        float x = width * 0.52f;
        Path path = new Path();
        path.moveTo(x, height * 0.76f);
        path.lineTo(x, height * 0.27f);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0L, 340L);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke).build();

        boolean accepted = dispatchGesture(gesture, new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gestureDescription) {
                swiping = false;
                swipeCount++;
                if (running && !paused && screenReady() && targetForeground()) {
                    deadline = SystemClock.elapsedRealtime() + intervalSeconds * 1000L;
                }
            }
            @Override public void onCancelled(GestureDescription gestureDescription) {
                swiping = false;
                deadline = SystemClock.elapsedRealtime() + 1500L;
            }
        }, handler);

        if (!accepted) {
            swiping = false;
            deadline = SystemClock.elapsedRealtime() + 1500L;
        }
    }

    private void showOverlay() {
        if (overlay != null) {
            updateOverlay("Ready", "Open your selected video feed");
            refreshPauseButton();
            return;
        }
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        LinearLayout pane = new LinearLayout(this);
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setPadding(dp(12), dp(10), dp(12), dp(10));
        pane.setElevation(dp(9));
        pane.setBackground(rounded(0xF51D2741, dp(17)));

        TextView dragLabel = makeText("≡   DAVINAUTOSWIPE   •   drag to move", 10, 0xFF9EABE3, true);
        dragLabel.setPadding(0, 0, 0, dp(5));
        pane.addView(dragLabel);

        countdownLabel = makeText("Ready", 20, Color.WHITE, true);
        pane.addView(countdownLabel);
        secondaryLabel = makeText("Open your selected video feed", 11, 0xFFC1C8E0, false);
        pane.addView(secondaryLabel);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, dp(8), 0, 0);

        pauseButton = makeAction("PAUSE", 0xFF514EAB);
        TextView stopButton = makeAction("STOP", 0xFFB63C61);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(36), 1);
        lp.setMargins(0, 0, dp(6), 0);
        buttons.addView(pauseButton, lp);
        buttons.addView(stopButton, new LinearLayout.LayoutParams(0, dp(36), 1));
        pane.addView(buttons);

        pauseButton.setOnClickListener(v -> togglePause());
        stopButton.setOnClickListener(v -> stopAutomation());

        dragLabel.setOnTouchListener((v, event) -> {
            if (overlayParams == null || windowManager == null || overlay == null) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();
                    originX = overlayParams.x;
                    originY = overlayParams.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - lastTouchX);
                    int dy = (int) (event.getRawY() - lastTouchY);
                    int maxX = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(200));
                    int maxY = Math.max(0, getResources().getDisplayMetrics().heightPixels - dp(160));
                    overlayParams.x = Math.max(0, Math.min(maxX, originX + dx));
                    overlayParams.y = Math.max(0, Math.min(maxY, originY + dy));
                    windowManager.updateViewLayout(overlay, overlayParams);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return false;
            }
        });

        overlay = pane;
        overlayParams = new WindowManager.LayoutParams(
                dp(208), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayParams.x = Math.max(0, getResources().getDisplayMetrics().widthPixels - dp(225));
        overlayParams.y = dp(100);
        try {
            windowManager.addView(overlay, overlayParams);
        } catch (Exception ignored) {
            overlay = null;
            overlayParams = null;
            countdownLabel = null;
            secondaryLabel = null;
            pauseButton = null;
        }
    }

    private void removeOverlay() {
        if (overlay != null && windowManager != null) {
            try { windowManager.removeView(overlay); } catch (Exception ignored) { }
        }
        overlay = null;
        overlayParams = null;
        countdownLabel = null;
        secondaryLabel = null;
        pauseButton = null;
    }

    private void updateOverlay(String title, String sub) {
        if (countdownLabel != null) countdownLabel.setText(title);
        if (secondaryLabel != null) secondaryLabel.setText(sub);
    }

    private void refreshPauseButton() {
        if (pauseButton != null) pauseButton.setText(paused ? "RESUME" : "PAUSE");
    }

    private TextView makeText(String value, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextColor(color);
        v.setTextSize(sp);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private TextView makeAction(String text, int bg) {
        TextView v = makeText(text, 12, Color.WHITE, true);
        v.setGravity(Gravity.CENTER);
        v.setBackground(rounded(bg, dp(10)));
        return v;
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
