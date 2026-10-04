package com.ishhf.aichat;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "PhonePlugin")
public class PhonePlugin extends Plugin {
    @PluginMethod
    public void agentStart(PluginCall call) {
        String sys = call.getString("sys", "");
        String task = call.getString("task", "");
        String url = call.getString("url", "");
        String dev = call.getString("dev", "");
        int max = call.getInt("max", 12);
        boolean ok = QAgent.start(getContext().getApplicationContext(), sys, task, url, dev, max);
        JSObject r = new JSObject();
        r.put("ok", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void agentStatus(PluginCall call) {
        JSObject r = new JSObject();
        r.put("running", QAgent.running);
        r.put("log", QAgent.log);
        r.put("result", QAgent.result);
        call.resolve(r);
    }

    @PluginMethod
    public void agentStop(PluginCall call) {
        QAgent.stop = true;
        call.resolve();
    }

    @PluginMethod
    public void accEnabled(PluginCall call) {
        JSObject r = new JSObject();
        r.put("on", QAccService.inst != null);
        call.resolve(r);
    }

    @PluginMethod
    public void accSettings(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e));
        }
    }

    @PluginMethod
    public void appInfo(PluginCall call) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getContext().getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e));
        }
    }

    @PluginMethod
    public void transOn(PluginCall call) {
        boolean ok = QAccService.transOn(call.getString("url", ""), call.getString("dev", ""), call.getBoolean("auto", false));
        JSObject r = new JSObject();
        r.put("ok", ok);
        call.resolve(r);
    }

    @PluginMethod
    public void transOff(PluginCall call) {
        QAccService.transOff();
        call.resolve();
    }

    @PluginMethod
    public void transAuto(PluginCall call) {
        QAccService.transAuto(call.getBoolean("auto", false));
        call.resolve();
    }

    @PluginMethod
    public void transState(PluginCall call) {
        JSObject r = new JSObject();
        r.put("on", QAccService.transIsOn());
        call.resolve(r);
    }
}
