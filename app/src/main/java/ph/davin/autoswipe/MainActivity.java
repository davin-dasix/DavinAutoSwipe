package ph.davin.autoswipe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Launcher/settings activity. Uses native Android views: no subscriptions, ads or libraries. */
public class MainActivity extends Activity {

    private static final int BG = 0xFF0B1020;
    private static final int CARD = 0xFF171F35;
    private static final int MUTED = 0xFFACB8D4;
    private static final int ACCENT = 0xFF8586FF;
    private SharedPreferences prefs;
    private EditText secondsInput;
    private CheckBox fbCheck;
    private CheckBox shopeeCheck;
    private CheckBox consentCheck;
    private TextView status;
    private Button startButton;
    private Button accessButton;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("davinautoswipe", MODE_PRIVATE);
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        scroll.setClipToPadding(false);
        // Targeting modern Android: preserve space for status/navigation bars
        // while keeping the content scrollable on smaller screens.
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            scroll.setPadding(0, insets.getSystemWindowInsetTop(), 0,
                    insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(38));
        scroll.addView(root);
        setContentView(scroll);

        TextView wordmark = text("↟  DAVINAUTOSWIPE", 14, 0xFFA7A9FF, true);
        wordmark.setLetterSpacing(0.12f);
        root.addView(wordmark);
        space(root, 24);

        TextView hero = text("Keep watching.\nWe'll do the scrolling.", 32, Color.WHITE, true);
        hero.setLineSpacing(dp(3), 1f);
        root.addView(hero);
        space(root, 12);
        root.addView(text("Automatic next-video swipes for Facebook Reels and Shopee Videos — at your own pace.", 15, MUTED, false));
        space(root, 24);

        LinearLayout timerCard = card();
        root.addView(timerCard);
        timerCard.addView(text("01   SCROLL INTERVAL", 12, 0xFF9DA9D3, true));
        space(timerCard, 13);
        LinearLayout timerRow = horizontal();
        timerRow.setGravity(Gravity.CENTER_VERTICAL);
        timerCard.addView(timerRow);
        secondsInput = new EditText(this);
        secondsInput.setSingleLine(true);
        secondsInput.setSelectAllOnFocus(true);
        secondsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        secondsInput.setText(String.valueOf(prefs.getInt("interval", 55)));
        secondsInput.setTextColor(Color.WHITE);
        secondsInput.setTextSize(40);
        secondsInput.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        secondsInput.setPadding(dp(12), 0, dp(8), 0);
        secondsInput.setBackground(rounded(0xFF24304D, 13));
        timerRow.addView(secondsInput, new LinearLayout.LayoutParams(dp(137), dp(68)));
        TextView secondsUnit = text("seconds / video", 15, MUTED, true);
        LinearLayout.LayoutParams unitLp = new LinearLayout.LayoutParams(-2, -2);
        unitLp.leftMargin = dp(12);
        timerRow.addView(secondsUnit, unitLp);
        space(timerCard, 14);
        timerCard.addView(text("Quick presets", 12, MUTED, false));
        space(timerCard, 8);
        LinearLayout presets = horizontal();
        timerCard.addView(presets);
        for (int sec : new int[]{15, 30, 55, 60}) {
            TextView chip = text(sec + "s", 13, Color.WHITE, true);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(rounded(0xFF303B60, 10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
            lp.rightMargin = dp(6);
            presets.addView(chip, lp);
            chip.setOnClickListener(v -> secondsInput.setText(String.valueOf(sec)));
        }
        space(root, 14);

        LinearLayout appsCard = card();
        root.addView(appsCard);
        appsCard.addView(text("02   CHOOSE WHERE TO SCROLL", 12, 0xFF9DA9D3, true));
        space(appsCard, 10);
        fbCheck = check("Facebook Reels (includes Facebook Lite)", prefs.getBoolean("facebook", true));
        shopeeCheck = check("Shopee PH Videos", prefs.getBoolean("shopee", true));
        appsCard.addView(fbCheck);
        appsCard.addView(shopeeCheck);
        space(appsCard, 8);
        appsCard.addView(text("The countdown pauses when you leave your selected apps and restarts when you return.", 12, MUTED, false));
        space(root, 14);

        LinearLayout permissionCard = card();
        root.addView(permissionCard);
        permissionCard.addView(text("03   ENABLE SCREEN SWIPES", 12, 0xFF9DA9D3, true));
        space(permissionCard, 10);
        status = text("Checking accessibility...", 13, MUTED, false);
        permissionCard.addView(status);
        space(permissionCard, 12);
        accessButton = button("Open Accessibility Settings", 0xFF2E395A);
        permissionCard.addView(accessButton, fullWidth(46));
        accessButton.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        space(permissionCard, 10);
        permissionCard.addView(text("Find “DavinAutoSwipe Auto Scroll” and switch it on. Android requires you to grant this access manually.", 12, MUTED, false));
        space(root, 14);

        LinearLayout disclosureCard = card();
        root.addView(disclosureCard);
        disclosureCard.addView(text("ACCESSIBILITY DISCLOSURE & PRIVACY", 12, 0xFF9DA9D3, true));
        space(disclosureCard, 10);
        disclosureCard.addView(text("DavinAutoSwipe uses Android Accessibility access to identify whether Facebook or Shopee is the active app and perform an upward swipe at the interval you select. Android grants this permission broad screen interaction capabilities. DavinAutoSwipe does not inspect or save video contents, messages, passwords or other on-screen text; it does not send data anywhere. It only runs after you tap Start, and you can pause or stop it at any time.", 13, 0xFFD4DBEF, false));
        space(disclosureCard, 10);
        consentCheck = check("I understand and agree to this Accessibility use", prefs.getBoolean("consent", false));
        disclosureCard.addView(consentCheck);
        consentCheck.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean("consent", checked).apply());
        space(root, 20);

        startButton = button("▶   START AUTO-SCROLL", ACCENT);
        startButton.setTextColor(0xFF090F22);
        startButton.setTextSize(16);
        root.addView(startButton, fullWidth(58));
        startButton.setOnClickListener(v -> onStartStop());
        space(root, 12);
        root.addView(text("After Start, open Facebook Reels or Shopee Videos. A movable floating controller shows the countdown, PAUSE and STOP.", 13, MUTED, false));
        space(root, 16);
        root.addView(text("Works with the official Android apps • No root • No internet permission • No subscription", 12, 0xFF8D9AB7, false));
        updateStatus();
    }

    private void onStartStop() {
        AutoScrollService service = AutoScrollService.instance;
        if (service != null && service.isRunning()) {
            service.stopAutomation();
            updateStatus();
            Toast.makeText(this, "Auto-scroll stopped", Toast.LENGTH_SHORT).show();
            return;
        }
        int interval;
        try {
            interval = Integer.parseInt(secondsInput.getText().toString().trim());
        } catch (Exception e) {
            secondsInput.setError("Enter a number from 5 to 3600");
            secondsInput.requestFocus();
            return;
        }
        if (interval < 5 || interval > 3600) {
            secondsInput.setError("Choose 5–3600 seconds");
            secondsInput.requestFocus();
            return;
        }
        if (!fbCheck.isChecked() && !shopeeCheck.isChecked()) {
            Toast.makeText(this, "Choose Facebook or Shopee first", Toast.LENGTH_LONG).show();
            return;
        }
        if (!consentCheck.isChecked()) {
            new AlertDialog.Builder(this)
                    .setTitle("Permission disclosure")
                    .setMessage("Read the Accessibility disclosure and check the consent box to use auto-scroll.")
                    .setPositiveButton("OK", null).show();
            return;
        }
        prefs.edit().putInt("interval", interval)
                .putBoolean("facebook", fbCheck.isChecked())
                .putBoolean("shopee", shopeeCheck.isChecked())
                .apply();
        if (service == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Accessibility")
                    .setMessage("Please enable 'DavinAutoSwipe Auto Scroll' under Android Accessibility settings. Return here and tap Start again.")
                    .setPositiveButton("Open Settings", (dialog, which) ->
                            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                    .setNegativeButton("Cancel", null).show();
            return;
        }
        service.startAutomation(interval, fbCheck.isChecked(), shopeeCheck.isChecked());
        updateStatus();
        Toast.makeText(this, "Ready! Open your video feed. Use the floating STOP button to end.", Toast.LENGTH_LONG).show();
        moveTaskToBack(true);
    }

    private void updateStatus() {
        if (status == null || startButton == null) return;
        AutoScrollService service = AutoScrollService.instance;
        boolean enabled = accessibilityEnabled();
        if (service != null && service.isRunning()) {
            status.setText("●  Accessibility enabled · Auto-scroll active");
            status.setTextColor(0xFF8DF0CB);
            startButton.setText("■   STOP AUTO-SCROLL");
            startButton.setBackground(rounded(0xFFF0A4B9, 14));
        } else if (enabled) {
            status.setText("●  Accessibility enabled · Ready to start");
            status.setTextColor(0xFF8DF0CB);
            startButton.setText("▶   START AUTO-SCROLL");
            startButton.setBackground(rounded(ACCENT, 14));
        } else {
            status.setText("●  Accessibility is OFF · Enable it once");
            status.setTextColor(0xFFFFC68E);
            startButton.setText("▶   START AUTO-SCROLL");
            startButton.setBackground(rounded(ACCENT, 14));
        }
    }

    private boolean accessibilityEnabled() {
        if (AutoScrollService.instance != null) return true;
        try {
            String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String component = new ComponentName(this, AutoScrollService.class).flattenToString();
            return enabled != null && enabled.contains(component);
        } catch (Exception e) {
            return false;
        }
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(17), dp(18), dp(17), dp(18));
        c.setBackground(rounded(CARD, 20));
        return c;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private Button button(String label, int background) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(Color.WHITE);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setBackground(rounded(background, 14));
        b.setPadding(dp(10), 0, dp(10), 0);
        return b;
    }

    private CheckBox check(String label, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setTextColor(0xFFE8EAF6);
        c.setTextSize(14);
        c.setChecked(checked);
        c.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        c.setPadding(0, dp(5), 0, dp(5));
        return c;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private LinearLayout.LayoutParams fullWidth(int heightDp) {
        return new LinearLayout.LayoutParams(-1, dp(heightDp));
    }

    private void space(LinearLayout parent, int dpValue) {
        View spacer = new View(this);
        parent.addView(spacer, new LinearLayout.LayoutParams(1, dp(dpValue)));
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
