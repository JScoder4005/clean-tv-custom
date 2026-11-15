package dev.uday.luffy;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One screen: the Pi's vital signs on the left, the lamp's controls on the right. D-pad and OK only. */
public class LuffyActivity extends Activity {
    private static final long REFRESH_MS = 5000;
    private static final String LAMP_ACTION = "/api/home/devices/lamp-main/action";
    private static final int BG = Color.parseColor("#14111A");
    private static final int INK = Color.parseColor("#F2EDE6");
    private static final int DIM = Color.parseColor("#9A93A3");
    private static final int ACCENT = Color.parseColor("#E8A33D");
    private static final int KEY = Color.parseColor("#2A2433");

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView piStatus;
    private TextView piLines;
    private TextView bulbStatus;
    private BulbState bulb;
    private boolean resumed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(72), dp(56), dp(72), dp(40));

        LinearLayout left = column();
        left.addView(text("Raspberry Pi", 34, ACCENT, true));
        piStatus = text("Connecting…", 22, DIM, false);
        piStatus.setPadding(0, dp(6), 0, dp(24));
        left.addView(piStatus);
        piLines = text("", 30, INK, false);
        piLines.setLineSpacing(dp(10), 1f);
        left.addView(piLines);

        LinearLayout right = column();
        right.addView(text("Bulb", 34, ACCENT, true));
        bulbStatus = text("…", 22, DIM, false);
        bulbStatus.setPadding(0, dp(6), 0, dp(24));
        right.addView(bulbStatus);
        TextView power = key("Power", 0, () -> send(Actions.toggle()));
        row(right, new View[]{power});
        row(right, new View[]{
                key("Dimmer", 0, () -> send(Actions.brightness((bulb == null ? 50 : bulb.brightness()) - 10))),
                key("Brighter", 0, () -> send(Actions.brightness((bulb == null ? 50 : bulb.brightness()) + 10)))});
        row(right, new View[]{
                key("Warm", 0, () -> send(Actions.mode("warm"))),
                key("Cool", 0, () -> send(Actions.mode("cool"))),
                key("White", 0, () -> send(Actions.mode("white")))});
        row(right, new View[]{
                key("Red", Color.parseColor("#E5484D"), () -> send(Actions.color("#ff0000"))),
                key("Green", Color.parseColor("#30A46C"), () -> send(Actions.color("#00ff00"))),
                key("Blue", Color.parseColor("#3E63DD"), () -> send(Actions.color("#0000ff"))),
                key("Purple", Color.parseColor("#8E4EC6"), () -> send(Actions.color("#8000ff")))});

        root.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        root.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        setContentView(root);
        power.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        handler.post(refresh);
    }

    @Override
    protected void onPause() {
        resumed = false;
        handler.removeCallbacks(refresh);
        super.onPause();
    }

    private final Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if (!resumed) return;
            Api.get("/api/pi", body -> {
                PiStats s = PiStats.parse(body);
                piStatus.setText(s == null ? "Pi unreachable" : "Connected");
                piStatus.setTextColor(s == null ? Color.parseColor("#E5484D") : DIM);
                if (s != null) piLines.setText(String.join("\n", s.lines()));
            });
            Api.get("/api/home", body -> {
                BulbState s = BulbState.parseSnapshot(body);
                if (s != null) show(s);
            });
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    private void send(String action) {
        bulbStatus.setText("Sending…");
        Api.post(LAMP_ACTION, action, body -> {
            BulbState s = BulbState.parseDevice(body);
            if (s == null) bulbStatus.setText("Bulb did not answer");
            else show(s);
        });
    }

    private void show(BulbState s) {
        bulb = s;
        bulbStatus.setText(s.summary());
    }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(0, 0, dp(48), 0);
        return c;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    /** A focusable key; OK runs `onOk`. `tint` 0 = neutral. */
    private TextView key(String label, int tint, Runnable onOk) {
        TextView t = text(label, tint == 0 ? 26 : 21, INK, true);   // the four colour keys share one row
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(18), dp(8), dp(18));
        t.setSingleLine(true);
        t.setFocusable(true);
        t.setClickable(true);
        int base = tint == 0 ? KEY : tint;
        restyle(t, base, false);
        t.setOnFocusChangeListener((v, focused) -> restyle(t, base, focused));
        t.setOnClickListener(v -> onOk.run());
        return t;
    }

    private void row(LinearLayout into, View[] keys) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        for (View k : keys) r.addView(k, keyParams());   // equal widths: a row always fits its column
        into.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private LinearLayout.LayoutParams keyParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(0, 0, dp(12), dp(14));
        return p;
    }

    private void restyle(View v, int base, boolean focused) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(14));
        d.setColor(base);
        if (focused) d.setStroke(dp(4), ACCENT);
        v.setBackground(d);
        v.setScaleX(focused ? 1.06f : 1f);
        v.setScaleY(focused ? 1.06f : 1f);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
