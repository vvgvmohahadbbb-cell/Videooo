package com.ishhf.aichat;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.concurrent.TimeUnit;

// قراءة النص من الصور (ML Kit، لاتيني: إنجليزي وغيره)
public class QOcr {
    public static class Blk {
        public String t;
        public Rect r;
    }

    private static List<Blk> collect(TextRecognizer rec, InputImage img) throws Exception {
        List<Blk> out = new ArrayList<Blk>();
        try {
            Text t = Tasks.await(rec.process(img), 20, TimeUnit.SECONDS);
            for (Text.TextBlock b : t.getTextBlocks()) {
                Rect r = b.getBoundingBox();
                String s = b.getText();
                if (r != null && s != null && s.trim().length() > 0) {
                    Blk k = new Blk();
                    k.t = s.replace("\n", " ").trim();
                    k.r = r;
                    out.add(k);
                }
            }
        } finally {
            try { rec.close(); } catch (Exception e) {}
        }
        return out;
    }

    public static List<Blk> read(Bitmap bmp) throws Exception {
        InputImage img = InputImage.fromBitmap(bmp, 0);
        return collect(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS), img);
    }
}
