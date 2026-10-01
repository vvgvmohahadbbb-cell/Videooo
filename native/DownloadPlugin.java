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

@CapacitorPlugin(name = "DownloadPlugin")
public class DownloadPlugin extends Plugin {
    @PluginMethod
    public void download(final PluginCall call) {
        final String u = call.getString("url");
        final String name = call.getString("name", "file");
        final String mime = call.getString("mime", "image/jpeg");
        final boolean apk = mime.contains("android.package");
        if (u == null) { call.reject("no url"); return; }
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection h = (HttpURLConnection) new URL(u).openConnection();
                    h.setConnectTimeout(30000);
                    h.setReadTimeout(120000);
                    h.setInstanceFollowRedirects(true);
                    h.setRequestProperty("User-Agent", "Mozilla/5.0");
                    if (h.getResponseCode() != 200) { call.reject("HTTP " + h.getResponseCode()); return; }
                    long total = h.getContentLengthLong();
                    InputStream in = h.getInputStream();
                    OutputStream os;
                    Uri uri = null;
                    String where;
                    ContentResolver cr = getContext().getContentResolver();
                    if (Build.VERSION.SDK_INT >= 29) {
                        ContentValues v = new ContentValues();
                        v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                        v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                        Uri col;
                        if (apk) {
                            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Qasim");
                            col = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                            where = "Download/Qasim";
                        } else {
                            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Qasim");
                            col = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            where = "Pictures/Qasim";
                        }
                        v.put(MediaStore.MediaColumns.IS_PENDING, 1);
                        uri = cr.insert(col, v);
                        if (uri == null) { call.reject("insert failed"); return; }
                        os = cr.openOutputStream(uri);
                    } else {
                        File d = getContext().getExternalFilesDir(apk ? Environment.DIRECTORY_DOWNLOADS : Environment.DIRECTORY_PICTURES);
                        File f = new File(d, name);
                        os = new FileOutputStream(f);
                        where = f.getAbsolutePath();
                    }
                    byte[] buf = new byte[16384];
                    int n;
                    long got = 0, last = 0;
                    while ((n = in.read(buf)) > 0) {
                        os.write(buf, 0, n);
                        got += n;
                        long now = System.currentTimeMillis();
                        if (now - last > 150) {
                            last = now;
                            JSObject e = new JSObject();
                            e.put("got", got);
                            e.put("total", total);
                            notifyListeners("dlprogress", e);
                        }
                    }
                    os.close();
                    in.close();
                    if (uri != null) {
                        ContentValues v2 = new ContentValues();
                        v2.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        cr.update(uri, v2, null, null);
                    }
                    JSObject r = new JSObject();
                    r.put("where", where);
                    call.resolve(r);
                } catch (Exception e) {
                    call.reject(String.valueOf(e.getMessage()));
                }
            }
        }).start();
    }
}
