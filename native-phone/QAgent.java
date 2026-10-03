package com.ishhf.aichat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

public class QAgent {
    public static volatile boolean running = false;
    public static volatile boolean stop = false;
    public static volatile String log = "";
    public static volatile String result = "";

    private static final Pattern RISKY = Pattern.compile("(?i).*(send|إرسال|ارسال|pay|دفع|اشتر|شراء|buy|purchase|حذف|delete|remove|uninstall|إلغاء التثبيت|transfer|تحويل|confirm|تأكيد|order|اطلب|طلب|post|نشر|call|اتصال|اتصل|submit).*");
    private static final String[] SENSITIVE = {"bank", "wallet", "paypal", "stripe", "authenticator", "vault", "keepass", "bitwarden", "1password", "lastpass", "binance", "crypto", "payment"};

    public static synchronized boolean start(final Context ctx, final String sys, final String task, final String url, final String dev, final int max) {
        if (running) return false;
        running = true;
        stop = false;
        log = "";
        result = "";
        new Thread(new Runnable() {
            public void run() {
                try {
                    loop(ctx, sys, task, url, dev, max);
                } catch (Throwable e) {
                    finish("تعذر إكمال المهمة: " + e.getMessage());
                } finally {
                    running = false;
                }
            }
        }).start();
        return true;
    }

    private static void add(Context ctx, String s) {
        log = log + "- " + s + "\n";
        toast(ctx, s);
    }

    private static void finish(String s) {
        result = s;
    }

    private static void toast(final Context ctx, final String s) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                try { Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show(); } catch (Exception e) {}
            }
        });
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }

    private static JSONObject msg(String role, String content) throws Exception {
        JSONObject o = new JSONObject();
        o.put("role", role);
        o.put("content", content);
        return o;
    }

    private static void loop(Context ctx, String sys, String task, String url, String dev, int max) throws Exception {
        long t0 = System.currentTimeMillis();
        StringBuilder hs = new StringBuilder();
        int lines = 0;
        for (int step = 1; step <= max && !stop; step++) {
            if (System.currentTimeMillis() - t0 > 150000) { finish("انتهى الوقت المسموح للمهمة."); return; }
            QAccService s = QAccService.inst;
            String screen = (s != null) ? s.dump() : "{\"pkg\":\"\",\"nodes\":[],\"note\":\"الوصول غير مفعّل: استعمل أوامر النظام فقط (open, yt, url, maps, alarm, timer, dial, sms)\"}";
            String low = screen.toLowerCase();
            int pi = low.indexOf("\"pkg\":\"");
            if (pi >= 0) {
                String pk = low.substring(pi + 7, Math.min(low.length(), pi + 80));
                for (int k = 0; k < SENSITIVE.length; k++) {
                    if (pk.indexOf(SENSITIVE[k]) >= 0) { finish("توقفت: الشاشة الحالية حساسة (مصرف أو دفع)، أكمل أنت بنفسك."); return; }
                }
            }
            JSONArray msgs = new JSONArray();
            msgs.put(msg("system", sys));
            msgs.put(msg("user", "المهمة: " + task + "\nما نُفّذ سابقاً:\n" + (hs.length() == 0 ? "لا شيء" : hs.toString()) + "\nالشاشة الآن: " + screen));
            String out = llm(url, dev, msgs);
            JSONObject a = parse(out);
            if (a == null) {
                add(ctx, "رد غير مفهوم، أعيد المحاولة");
                hs.append(step).append(") (رد غير مفهوم)\n");
                continue;
            }
            String k = a.optString("a", "");
            if ("done".equals(k)) { finish(a.optString("say", "تمت المهمة.")); return; }
            String res = exec(ctx, a, s);
            add(ctx, step + ": " + describe(a));
            if (res.startsWith("STOP:")) { finish(res.substring(5)); return; }
            hs.append(step).append(") ").append(a.toString()).append(" -> ").append(res).append("\n");
            lines++;
            if (lines > 6) {
                String h = hs.toString();
                int cut = h.indexOf('\n');
                hs = new StringBuilder(h.substring(cut + 1));
                lines--;
            }
            if (a.optBoolean("end", false)) { finish(a.optString("say", "تمت المهمة.")); return; }
        }
        if (stop) finish("أوقفت المهمة.");
        else finish("وصلت للحد الأقصى من الخطوات دون إنهاء المهمة.");
    }

    private static String describe(JSONObject a) {
        String k = a.optString("a", "");
        if ("open".equals(k)) return "فتح " + a.optString("name", "");
        if ("yt".equals(k)) return "بحث يوتيوب: " + a.optString("q", "");
        if ("click".equals(k)) return "ضغط على عنصر " + a.optInt("i", -1);
        if ("type".equals(k)) return "كتابة نص";
        if ("scroll".equals(k)) return "تمرير";
        return k;
    }

    private static String llm(String url, String dev, JSONArray msgs) throws Exception {
        JSONObject body = new JSONObject();
        body.put("model", "openai/gpt-oss-120b");
        body.put("messages", msgs);
        body.put("max_completion_tokens", 900);
        body.put("reasoning_effort", "low");
        body.put("include_reasoning", false);
        JSONObject req = new JSONObject();
        req.put("path", "chat");
        req.put("dev", dev);
        req.put("body", body);
        HttpURLConnection h = (HttpURLConnection) new URL(url).openConnection();
        h.setRequestMethod("POST");
        h.setDoOutput(true);
        h.setConnectTimeout(20000);
        h.setReadTimeout(70000);
        h.setInstanceFollowRedirects(true);
        h.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
        OutputStream os = h.getOutputStream();
        os.write(req.toString().getBytes("UTF-8"));
        os.close();
        int code = h.getResponseCode();
        InputStream in = code >= 400 ? h.getErrorStream() : h.getInputStream();
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while (in != null && (n = in.read(buf)) > 0) bo.write(buf, 0, n);
        JSONObject j = new JSONObject(new String(bo.toByteArray(), "UTF-8"));
        int st = j.optInt("status", 200);
        JSONObject d = j.optJSONObject("data");
        if (st != 200 || d == null) {
            String m = "خطأ من الخادم";
            if (d != null && d.optJSONObject("error") != null) m = d.optJSONObject("error").optString("message", m);
            else if (j.optString("error").length() > 0) m = j.optString("error");
            throw new Exception(m);
        }
        return d.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "");
    }

    private static JSONObject parse(String s) {
        try {
            int a = s.indexOf('{');
            int b = s.lastIndexOf('}');
            if (a < 0 || b <= a) return null;
            return new JSONObject(s.substring(a, b + 1));
        } catch (Exception e) {
            return null;
        }
    }

    private static void go(Context ctx, Intent i) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(i);
    }

    private static boolean hasPkg(Context ctx, String p) {
        try { ctx.getPackageManager().getPackageInfo(p, 0); return true; } catch (Exception e) { return false; }
    }

    private static String exec(Context ctx, JSONObject a, QAccService s) {
        String k = a.optString("a", "");
        try {
            if ("open".equals(k)) {
                String p = findPkg(ctx, a.optString("name", ""));
                if (p == null) return "التطبيق غير موجود";
                Intent i = ctx.getPackageManager().getLaunchIntentForPackage(p);
                if (i == null) return "تعذر الفتح";
                go(ctx, i);
                sleep(2300);
                return "تم فتح " + p;
            }
            if ("yt".equals(k)) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(a.optString("q", ""))));
                if (hasPkg(ctx, "com.google.android.youtube")) i.setPackage("com.google.android.youtube");
                go(ctx, i);
                sleep(2500);
                return "تم البحث";
            }
            if ("url".equals(k)) {
                String u = a.optString("u", "");
                if (!u.startsWith("http")) return "رابط غير صالح";
                go(ctx, new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
                sleep(2300);
                return "تم فتح الرابط";
            }
            if ("maps".equals(k)) {
                go(ctx, new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a.optString("q", "")))));
                sleep(2300);
                return "تم فتح الخرائط";
            }
            if ("alarm".equals(k)) {
                Intent i = new Intent(AlarmClock.ACTION_SET_ALARM);
                int hh = a.optInt("h", 7);
                int mm = a.optInt("m", 0);
                i.putExtra(AlarmClock.EXTRA_HOUR, hh);
                i.putExtra(AlarmClock.EXTRA_MINUTES, mm);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label", ""));
                go(ctx, i);
                sleep(1800);
                return "تم فتح ضبط المنبه";
            }
            if ("timer".equals(k)) {
                Intent i = new Intent(AlarmClock.ACTION_SET_TIMER);
                int sec = a.optInt("s", 60);
                i.putExtra(AlarmClock.EXTRA_LENGTH, sec);
                i.putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label", ""));
                go(ctx, i);
                sleep(1800);
                return "تم فتح المؤقت";
            }
            if ("dial".equals(k)) {
                go(ctx, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(a.optString("n", "")))));
                return "STOP:فتحت لوحة الاتصال بالرقم، اضغط اتصال بنفسك.";
            }
            if ("sms".equals(k)) {
                Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(a.optString("n", ""))));
                i.putExtra("sms_body", a.optString("t", ""));
                go(ctx, i);
                return "STOP:جهّزت الرسالة، اضغط إرسال بنفسك.";
            }
            if ("wait".equals(k)) {
                sleep(1500);
                return "انتظرت";
            }
            if (s == null) return "الوصول غير مفعّل";
            if ("click".equals(k)) {
                int i = a.optInt("i", -1);
                String lbl = s.labelOf(i);
                if (RISKY.matcher(lbl).matches()) return "STOP:توقفت قبل الضغط على «" + lbl + "» لأنها خطوة حساسة، اضغطها بنفسك إذا أردت.";
                boolean ok = s.click(i);
                sleep(1300);
                return ok ? "تم الضغط" : "فشل الضغط";
            }
            if ("type".equals(k)) {
                boolean ok = s.setText(a.optInt("i", -1), a.optString("t", ""));
                sleep(900);
                return ok ? "تمت الكتابة" : "فشلت الكتابة";
            }
            if ("scroll".equals(k)) {
                boolean ok = s.scroll(!"up".equals(a.optString("d", "down")));
                sleep(1000);
                return ok ? "تم التمرير" : "لا يوجد ما يُمرَّر";
            }
            if ("back".equals(k) || "home".equals(k)) {
                boolean ok = s.global(k);
                sleep(1200);
                return ok ? "تم" : "فشل";
            }
            return "أمر غير معروف";
        } catch (Exception e) {
            return "خطأ: " + e.getMessage();
        }
    }

    private static String norm(String s) {
        String t = s == null ? "" : s.toLowerCase().trim();
        t = t.replace("أ", "ا").replace("إ", "ا").replace("آ", "ا").replace("ة", "ه").replace("ى", "ي").replace("ـ", "");
        if (t.startsWith("ال")) t = t.substring(2);
        return t;
    }

    private static final String[][] ALIAS = {
        {"حاسب", "calculator"}, {"calc", "calculator"}, {"يوتيوب", "youtube"}, {"واتس", "whatsapp"}, {"كروم", "chrome"},
        {"كاميرا", "camera"}, {"اعدادات", "settings"}, {"إعدادات", "settings"}, {"خرائط", "maps"}, {"ساعه", "clock"}, {"منبه", "clock"},
        {"جهات", "contacts"}, {"هاتف", "dialer"}, {"رسائل", "messag"}, {"صور", "photos"}, {"معرض", "gallery"},
        {"تيك", "tiktok"}, {"انستا", "instagram"}, {"فيس", "facebook"}, {"تلغرام", "telegram"}, {"تيليجرام", "telegram"},
        {"سناب", "snapchat"}, {"جيميل", "gmail"}, {"بلاي", "vending"}
    };

    private static String findPkg(Context ctx, String name) {
        String q = norm(name);
        if (q.length() == 0) return null;
        String key = q;
        for (int i = 0; i < ALIAS.length; i++) {
            if (q.indexOf(norm(ALIAS[i][0])) >= 0) { key = ALIAS[i][1]; break; }
        }
        PackageManager pm = ctx.getPackageManager();
        Intent qi = new Intent(Intent.ACTION_MAIN);
        qi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> l = pm.queryIntentActivities(qi, 0);
        String best = null;
        int bs = 0;
        for (int i = 0; i < l.size(); i++) {
            ResolveInfo r = l.get(i);
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(ctx.getPackageName())) continue;
            String lab = norm(String.valueOf(r.loadLabel(pm)));
            int sc = 0;
            if (lab.equals(q) || lab.equals(key)) sc = 4;
            else if (lab.indexOf(key) >= 0 || (lab.length() > 2 && key.indexOf(lab) >= 0)) sc = 3;
            else if (pkg.toLowerCase().indexOf(key) >= 0) sc = 2;
            if (sc > bs) { bs = sc; best = pkg; }
        }
        return best;
    }
}
