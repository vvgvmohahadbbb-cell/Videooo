package com.ishhf.aichat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class QAccService extends AccessibilityService {
    public static volatile QAccService inst;
    private final List<AccessibilityNodeInfo> nodes = new ArrayList<AccessibilityNodeInfo>();
    private final List<String> labels = new ArrayList<String>();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        inst = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {}

    @Override
    public void onInterrupt() {}

    @Override
    public boolean onUnbind(Intent i) {
        inst = null;
        return super.onUnbind(i);
    }

    @Override
    public void onDestroy() {
        inst = null;
        super.onDestroy();
    }

    private static String clip(CharSequence s, int n) {
        if (s == null) return "";
        String t = s.toString().replace("\n", " ").trim();
        return t.length() > n ? t.substring(0, n) : t;
    }

    public synchronized String dump() {
        nodes.clear();
        labels.clear();
        JSONObject out = new JSONObject();
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root == null) {
                out.put("pkg", "");
                out.put("nodes", new JSONArray());
                return out.toString();
            }
            out.put("pkg", String.valueOf(root.getPackageName()));
            JSONArray arr = new JSONArray();
            walk(root, arr, 0);
            out.put("nodes", arr);
        } catch (Exception e) {
            try { out.put("err", String.valueOf(e)); } catch (Exception x) {}
        }
        return out.toString();
    }

    private void walk(AccessibilityNodeInfo n, JSONArray arr, int depth) throws Exception {
        if (n == null || arr.length() >= 70 || depth > 30) return;
        if (n.isVisibleToUser()) {
            String label = clip(n.getText(), 40);
            if (label.length() == 0) label = clip(n.getContentDescription(), 40);
            boolean pw = n.isPassword();
            boolean useful = n.isClickable() || n.isEditable() || n.isScrollable() || label.length() > 0;
            if (useful) {
                if (pw) label = "[سري]";
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                StringBuilder f = new StringBuilder();
                if (n.isClickable()) f.append('c');
                if (n.isEditable()) f.append('e');
                if (n.isScrollable()) f.append('s');
                if (n.isChecked()) f.append('k');
                if (pw) f.append('p');
                JSONArray row = new JSONArray();
                row.put(nodes.size());
                row.put(label);
                row.put(f.toString());
                row.put(r.centerX());
                row.put(r.centerY());
                nodes.add(n);
                labels.add(label);
                arr.put(row);
            }
        }
        for (int i = 0; i < n.getChildCount(); i++) walk(n.getChild(i), arr, depth + 1);
    }

    public synchronized String labelOf(int i) {
        if (i < 0 || i >= labels.size()) return "";
        return labels.get(i);
    }

    public synchronized boolean click(int i) {
        if (i < 0 || i >= nodes.size()) return false;
        AccessibilityNodeInfo n = nodes.get(i);
        AccessibilityNodeInfo c = n;
        int guard = 0;
        while (c != null && guard < 6) {
            if (c.isClickable() && c.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            c = c.getParent();
            guard++;
        }
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        return tap(r.centerX(), r.centerY());
    }

    public synchronized boolean setText(int i, String t) {
        if (i < 0 || i >= nodes.size()) return false;
        AccessibilityNodeInfo n = nodes.get(i);
        if (n.isPassword()) return false;
        n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
        Bundle b = new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b);
    }

    public synchronized boolean scroll(boolean forward) {
        for (int i = 0; i < nodes.size(); i++) {
            AccessibilityNodeInfo n = nodes.get(i);
            if (n.isScrollable()) {
                return n.performAction(forward ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            }
        }
        return false;
    }

    public boolean tap(int x, int y) {
        if (Build.VERSION.SDK_INT < 24) return false;
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription.StrokeDescription sd = new GestureDescription.StrokeDescription(p, 0, 60);
        return dispatchGesture(new GestureDescription.Builder().addStroke(sd).build(), null, null);
    }

    public boolean global(String a) {
        int g = -1;
        if ("back".equals(a)) g = GLOBAL_ACTION_BACK;
        else if ("home".equals(a)) g = GLOBAL_ACTION_HOME;
        else if ("recents".equals(a)) g = GLOBAL_ACTION_RECENTS;
        return g >= 0 && performGlobalAction(g);
    }
}
