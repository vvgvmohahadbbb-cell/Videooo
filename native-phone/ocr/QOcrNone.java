package com.ishhf.aichat;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.util.ArrayList;
import java.util.List;

// بدون OCR (احتياطي إذا فشل بناء مكتبة القراءة)
public class QOcr {
    public static class Blk {
        public String t;
        public Rect r;
    }

    public static List<Blk> read(Bitmap bmp) throws Exception {
        return new ArrayList<Blk>();
    }
}
