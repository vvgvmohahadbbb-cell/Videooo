package com.example.videoeditor;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 16, 24));
        getWindow().setNavigationBarColor(Color.rgb(16, 16, 24));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(16, 16, 24));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(24), dp(22), dp(24));
        scroll.addView(page);

        TextView brand = text("FRAME  /  STUDIO", 13, Color.rgb(177, 156, 255), true);
        page.addView(brand);

        TextView title = text("Video Editor", 32, Color.WHITE, true);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(14);
        page.addView(title, titleLp);

        TextView subtitle = text("مساحة مونتاجك تبدأ من هنا", 17, Color.rgb(190, 190, 205), false);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(6);
        page.addView(subtitle, subLp);

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER);
        preview.setBackgroundColor(Color.rgb(31, 30, 46));
        preview.setPadding(dp(16), dp(36), dp(16), dp(36));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(220));
        previewLp.topMargin = dp(26);
        page.addView(preview, previewLp);

        TextView play = text("▶", 42, Color.rgb(177, 156, 255), true);
        preview.addView(play);
        TextView previewLabel = text("معاينة المشروع", 16, Color.WHITE, true);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(-2, -2);
        pl.topMargin = dp(12);
        preview.addView(previewLabel, pl);
        preview.addView(text("استورد مقطع فيديو لبدء التحرير", 13, Color.rgb(190, 190, 205), false));

        TextView timelineTitle = text("TIMELINE", 13, Color.rgb(177, 156, 255), true);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-1, -2);
        tlp.topMargin = dp(28);
        page.addView(timelineTitle, tlp);

        String[] tracks = {"VIDEO 1     ━━━━━━━━━━━━━━━━━", "VIDEO 2     ────────────────", "AUDIO          ♪ ━━━━━━━━━━━"};
        for (String track : tracks) {
            TextView row = text(track, 14, Color.rgb(220, 220, 235), true);
            row.setBackgroundColor(Color.rgb(35, 34, 49));
            row.setPadding(dp(12), dp(17), dp(12), dp(17));
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2);
            rp.topMargin = dp(8);
            page.addView(row, rp);
        }

        Button importButton = new Button(this);
        importButton.setText("اختيار فيديو");
        importButton.setAllCaps(false);
        importButton.setTextColor(Color.WHITE);
        importButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(124, 77, 255)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(54));
        bp.topMargin = dp(24);
        page.addView(importButton, bp);
        importButton.setOnClickListener(v -> Toast.makeText(this,
            "مرحلة أولى: واجهة المشروع جاهزة. ستتم إضافة استيراد الفيديو والتحرير في المرحلة التالية.",
            Toast.LENGTH_LONG).show());

        TextView note = text("المرحلة 1 • واجهة تأسيسية قابلة للبناء", 12, Color.rgb(145, 145, 165), false);
        LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(-1, -2);
        np.topMargin = dp(18);
        page.addView(note, np);

        setContentView(scroll);
    }
}
