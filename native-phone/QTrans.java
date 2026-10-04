package com.ishhf.aichat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

// مترجم الشاشة: زر عائم (نافذة وصول) ← تحديد منطقة ← ترجمة للعربية ← رسم الترجمة فوق النص الأصلي
public class QTrans {
    public static volatile boolean on = false;
    static boolean auto = false;
    static volatile boolean busy = false;
    static QAccService svc;
    static WindowManager wm;
    static TextView btn;
    static View selView, transView;
    static String url = "", dev = "";
    static final Handler H = new Handler(Looper.getMainLooper());
    static final Map<String, String> cache = new LinkedHashMap<String, String>();

    static class Item {
        String t;
        String tr;
        Rect r;
    }

    public static void start(QAccService s, String u, String d, boolean a) {
        svc = s;
        url = u;
        dev = d;
        auto = a;
        on = true;
        H.post(new Runnable() { public void run() { addButton(); } });
    }

    public static void setAuto(boolean a) { auto = a; }

    public static void stop() {
        on = false;
        H.post(new Runnable() {
            public void run() {
                closeSel();
                clearTrans();
                if (btn != null && wm != null) {
                    try { wm.removeView(btn); } catch (Exception e) {}
                }
                btn = null;
            }
        });
    }

    private static void toast(final String m) {
        H.post(new Runnable() {
            public void run() {
                try { Toast.makeText(svc, m, Toast.LENGTH_SHORT).show(); } catch (Exception e) {}
            }
        });
    }

    private static void addButton() {
        if (btn != null || svc == null) return;
        wm = (WindowManager) svc.getSystemService(Context.WINDOW_SERVICE);
        float d = svc.getResources().getDisplayMetrics().density;
        int sz = (int) (50 * d);
        btn = new TextView(svc);
        btn.setText("ع");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(22);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {0xFF5B8CFF, 0xFF8B5CF6});
        bg.setShape(GradientDrawable.OVAL);
        btn.setBackground(bg);
        btn.setAlpha(0.92f);
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams(sz, sz,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = (int) (12 * d);
        lp.y = (int) (260 * d);
        btn.setOnTouchListener(new View.OnTouchListener() {
            float dx, dy;
            int sx, sy;
            boolean moved;
            public boolean onTouch(View v, MotionEvent e) {
                int a = e.getAction();
                if (a == MotionEvent.ACTION_DOWN) {
                    dx = e.getRawX(); dy = e.getRawY(); sx = lp.x; sy = lp.y; moved = false;
                    return true;
                }
                if (a == MotionEvent.ACTION_MOVE) {
                    float mx = e.getRawX() - dx;
                    float my = e.getRawY() - dy;
                    if (Math.abs(mx) > 12 || Math.abs(my) > 12) moved = true;
                    if (moved) {
                        lp.x = sx + (int) mx;
                        lp.y = sy + (int) my;
                        try { wm.updateViewLayout(btn, lp); } catch (Exception x) {}
                    }
                    return true;
                }
                if (a == MotionEvent.ACTION_UP) {
                    if (!moved) onTap();
                    return true;
                }
                return false;
            }
        });
        try { wm.addView(btn, lp); } catch (Exception e) { btn = null; }
    }

    private static void onTap() {
        if (transView != null) { clearTrans(); return; }
        if (selView != null) { closeSel(); return; }
        openSel();
    }

    // ---------- تحديد المنطقة ----------
    static class SelView extends View {
        final Paint dim = new Paint();
        final Paint box = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint fill = new Paint();
        final Paint bp = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final RectF allBtn = new RectF();
        final float d;
        float x0, y0, x1, y1;
        boolean drag;

        SelView(Context c) {
            super(c);
            d = c.getResources().getDisplayMetrics().density;
            dim.setColor(0x66000000);
            fill.setColor(0x33FFFFFF);
            box.setStyle(Paint.Style.STROKE);
            box.setStrokeWidth(2 * d);
            box.setColor(0xFFFFFFFF);
            bp.setColor(0xFF5B8CFF);
            tp.setColor(Color.WHITE);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTextSize(15 * d);
            tp.setFakeBoldText(true);
        }

        @Override
        protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            c.drawRect(0, 0, w, h, dim);
            c.drawText("اسحب لتحديد النص المراد ترجمته", w / 2f, 90 * d, tp);
            if (drag) {
                RectF r = new RectF(Math.min(x0, x1), Math.min(y0, y1), Math.max(x0, x1), Math.max(y0, y1));
                c.drawRect(r, fill);
                c.drawRect(r, box);
            }
            float bw = 220 * d, bh = 48 * d;
            allBtn.set((w - bw) / 2f, h - 140 * d, (w + bw) / 2f, h - 140 * d + bh);
            c.drawRoundRect(allBtn, 24 * d, 24 * d, bp);
            c.drawText("ترجمة كل الشاشة", w / 2f, allBtn.centerY() + 5 * d, tp);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            int a = e.getAction();
            if (a == MotionEvent.ACTION_DOWN) {
                if (allBtn.contains(x, y)) {
                    closeSel();
                    runTranslate(null, false);
                    return true;
                }
                x0 = x; y0 = y; x1 = x; y1 = y; drag = true;
                invalidate();
                return true;
            }
            if (a == MotionEvent.ACTION_MOVE) {
                x1 = x; y1 = y;
                invalidate();
                return true;
            }
            if (a == MotionEvent.ACTION_UP) {
                drag = false;
                if (Math.abs(x1 - x0) > 30 * d && Math.abs(y1 - y0) > 14 * d) {
                    int[] loc = new int[2];
                    getLocationOnScreen(loc);
                    Rect sel = new Rect((int) Math.min(x0, x1) + loc[0], (int) Math.min(y0, y1) + loc[1], (int) Math.max(x0, x1) + loc[0], (int) Math.max(y0, y1) + loc[1]);
                    closeSel();
                    runTranslate(sel, false);
                } else {
                    closeSel();
                }
                return true;
            }
            return true;
        }
    }

    private static void openSel() {
        if (svc == null || wm == null) return;
        selView = new SelView(svc);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        try { wm.addView(selView, lp); } catch (Exception e) { selView = null; }
        raiseButton();
    }

    private static void closeSel() {
        if (selView != null && wm != null) {
            try { wm.removeView(selView); } catch (Exception e) {}
        }
        selView = null;
    }

    private static void raiseButton() {
        // يعيد الزر فوق النوافذ الأخرى
        if (btn == null || wm == null) return;
        try {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) btn.getLayoutParams();
            wm.removeView(btn);
            wm.addView(btn, lp);
        } catch (Exception e) {}
    }

    // ---------- جمع النصوص ----------
    private static boolean mostlyArabic(String t) {
        int ar = 0, le = 0;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (Character.isLetter(ch)) {
                le++;
                if (ch >= 0x0600 && ch <= 0x06FF) ar++;
            }
        }
        return le == 0 || ar * 2 > le;
    }

    private static void gather(AccessibilityNodeInfo n, Rect sel, List<Item> out, int depth) {
        if (n == null || out.size() >= 70 || depth > 40) return;
        if (n.isVisibleToUser()) {
            CharSequence cs = n.getText();
            if (cs != null && cs.length() > 0 && !n.isPassword() && !n.isEditable()) {
                String t = cs.toString().trim();
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                if (t.length() >= 2 && !r.isEmpty() && !mostlyArabic(t)) {
                    boolean ok = true;
                    if (sel != null) {
                        Rect in = new Rect();
                        if (!in.setIntersect(sel, r)) ok = false;
                        else {
                            long ia = (long) in.width() * in.height();
                            long ra = (long) r.width() * r.height();
                            if (ra > 0 && ia * 2 < ra) ok = false;
                        }
                    }
                    if (ok) {
                        Item it = new Item();
                        it.t = t.length() > 400 ? t.substring(0, 400) : t;
                        it.r = r;
                        out.add(it);
                    }
                }
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) gather(n.getChild(i), sel, out, depth + 1);
    }

    // ---------- الترجمة ----------
    private static void translateItems(List<Item> items) throws Exception {
        List<String> need = new ArrayList<String>();
        int chars = 0;
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            String c;
            synchronized (cache) { c = cache.get(it.t); }
            if (c != null) it.tr = c;
            else if (!need.contains(it.t) && chars + it.t.length() < 3500) {
                need.add(it.t);
                chars += it.t.length();
            }
        }
        if (!need.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (int i = 0; i < need.size(); i++) arr.put(need.get(i));
            JSONArray msgs = new JSONArray();
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", "أنت مترجم محترف. ترجم كل عنصر في المصفوفة إلى العربية الفصحى المبسطة الطبيعية والمختصرة قدر الإمكان، وأبقِ الأرقام والأسماء والروابط والرموز كما هي. أعد مصفوفة JSON من نصوص بنفس الطول والترتيب فقط، بدون أي شرح.");
            JSONObject usr = new JSONObject();
            usr.put("role", "user");
            usr.put("content", arr.toString());
            msgs.put(sys);
            msgs.put(usr);
            String out = QAgent.chat(url, dev, msgs, "openai/gpt-oss-20b", 3000);
            int a = out.indexOf('['), b = out.lastIndexOf(']');
            if (a < 0 || b <= a) throw new Exception("رد غير مفهوم من المترجم");
            JSONArray res = new JSONArray(out.substring(a, b + 1));
            synchronized (cache) {
                for (int i = 0; i < need.size() && i < res.length(); i++) {
                    String tr = res.optString(i, "");
                    if (tr.length() > 0) cache.put(need.get(i), tr);
                }
                while (cache.size() > 400) {
                    Iterator<String> itr = cache.keySet().iterator();
                    itr.next();
                    itr.remove();
                }
            }
        }
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it.tr == null) {
                synchronized (cache) { it.tr = cache.get(it.t); }
            }
        }
    }

    static void runTranslate(final Rect sel, final boolean silent) {
        if (busy || svc == null) return;
        busy = true;
        if (!silent) toast("جاري الترجمة…");
        new Thread(new Runnable() {
            public void run() {
                try {
                    List<Item> items = new ArrayList<Item>();
                    gather(svc.getRootInActiveWindow(), sel, items, 0);
                    if (items.isEmpty()) {
                        if (!silent) toast("لم أجد نصاً للترجمة هنا");
                        return;
                    }
                    translateItems(items);
                    final List<Item> fin = items;
                    H.post(new Runnable() { public void run() { showTrans(fin); } });
                } catch (Throwable e) {
                    if (!silent) toast("تعذرت الترجمة: " + e.getMessage());
                } finally {
                    busy = false;
                }
            }
        }).start();
    }

    // ---------- رسم الترجمة فوق النص الأصلي ----------
    static class TransView extends View {
        final List<Item> items;
        final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
        final TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final float d;

        TransView(Context c, List<Item> its) {
            super(c);
            items = its;
            d = c.getResources().getDisplayMetrics().density;
            bg.setColor(0xF2141B2E);
            tp.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas c) {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            c.translate(-loc[0], -loc[1]);
            for (int i = 0; i < items.size(); i++) {
                Item it = items.get(i);
                if (it.tr == null) continue;
                Rect r = it.r;
                float pad = 2 * d;
                RectF rf = new RectF(r.left - pad, r.top - pad, r.right + pad, r.bottom + pad);
                c.drawRoundRect(rf, 4 * d, 4 * d, bg);
                int w = Math.max(10, (int) rf.width() - (int) (8 * d));
                float size = Math.min(18 * d, Math.max(10 * d, r.height() * 0.62f));
                StaticLayout sl = null;
                while (size >= 9 * d) {
                    tp.setTextSize(size);
                    sl = new StaticLayout(it.tr, tp, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false);
                    if (sl.getHeight() <= rf.height()) break;
                    size -= d;
                }
                if (sl == null) continue;
                c.save();
                c.clipRect(rf);
                c.translate(rf.left + 4 * d, rf.top + Math.max(0f, (rf.height() - sl.getHeight()) / 2f));
                sl.draw(c);
                c.restore();
            }
        }
    }

    static void showTrans(List<Item> items) {
        clearTrans();
        if (svc == null || wm == null) return;
        transView = new TransView(svc, items);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        try { wm.addView(transView, lp); } catch (Exception e) { transView = null; }
        raiseButton();
    }

    static void clearTrans() {
        if (transView != null && wm != null) {
            try { wm.removeView(transView); } catch (Exception e) {}
        }
        transView = null;
    }

    // أحداث النظام: التمرير أو تغيّر النافذة يمسح الترجمة، والوضع التلقائي يترجم من جديد
    private static final Runnable AUTO = new Runnable() {
        public void run() {
            if (on && auto && selView == null && transView == null) runTranslate(null, true);
        }
    };

    public static void onEvent(AccessibilityEvent e) {
        try {
            CharSequence pk = e.getPackageName();
            if (pk != null && svc != null && pk.toString().equals(svc.getPackageName())) return;
            int t = e.getEventType();
            if (t == AccessibilityEvent.TYPE_VIEW_SCROLLED || t == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                if (transView != null) {
                    H.post(new Runnable() { public void run() { clearTrans(); } });
                }
                if (auto) {
                    H.removeCallbacks(AUTO);
                    H.postDelayed(AUTO, 1100);
                }
            }
        } catch (Exception x) {}
    }
}
