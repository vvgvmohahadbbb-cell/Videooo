package com.ishhf.aichat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@CapacitorPlugin(name = "SavePlugin")
public class SavePlugin extends Plugin {
    @PluginMethod
    public void saveUrl(final PluginCall call) {
        final String u = call.getString("url");
        final String name = call.getString("name", "qasim.jpg");
        if (u == null) { call.reject("no url"); return; }
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
                    h.setConnectTimeout(30000);
                    h.setReadTimeout(120000);
                    h.setInstanceFollowRedirects(true);
                    h.setRequestProperty("User-Agent", "Mozilla/5.0");
                    int code = h.getResponseCode();
                    if (code != 200) { call.reject("HTTP " + code); return; }
                    String ct = h.getContentType();
                    String mime = "image/jpeg";
                    if (ct != null && ct.startsWith("image/")) mime = ct.split(";")[0];
                    InputStream in = h.getInputStream();
                    OutputStream os;
                    String where;
                    Uri uri = null;
                    ContentResolver cr = getContext().getContentResolver();
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
                        v.put(MediaStore.Images.Media.MIME_TYPE, mime);
                        v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Qasim");
                        v.put(MediaStore.Images.Media.IS_PENDING, 1);
                        uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
                        if (uri == null) { call.reject("insert failed"); return; }
                        os = cr.openOutputStream(uri);
                        where = "Pictures/Qasim";
                    } else {
                        File d = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                        File f = new File(d, name);
                        os = new FileOutputStream(f);
                        where = f.getAbsolutePath();
                    }
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                    os.close();
                    in.close();
                    if (uri != null) {
                        ContentValues v2 = new ContentValues();
                        v2.put(MediaStore.Images.Media.IS_PENDING, 0);
                        cr.update(uri, v2, null, null);
                    }
                    JSObject r = new JSObject();
                    r.put("path", where);
                    call.resolve(r);
                } catch (Exception e) {
                    call.reject(String.valueOf(e.getMessage()));
                }
            }
        }).start();
    }
}
