package com.starlight.cameratuner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.view.View;

/**
 * 背景层：斜向渐变 + 四个超大模糊光斑。
 *
 * 性能关键（2026-10-26 重构）：
 *   模糊（BlurMaskFilter）在 Android 上是 CPU 密集操作，
 *   如果每次 onDraw 都画 4 个半径 210px 的模糊圆，
 *   任何上层变化都会触发重算 → 掉帧。
 *
 *   所以这里把整层背景**预渲染到一张 Bitmap**，
 *   只在尺寸变化时重画一次，onDraw 只做一次贴图。
 */
public class GlassBackground extends View {

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);

    private Bitmap cache;

    // 光斑颜色（ARGB）
    private static final int C_BLOB_PINK   = 0x8CFFB3CC;
    private static final int C_BLOB_PURPLE = 0x6BC9B8FF;
    private static final int C_BLOB_BLUE   = 0x61AFD8FF;
    private static final int C_BLOB_ORANGE = 0x52FFD1B3;

    private static final int C_BG_TOP    = 0xFFFFF6F9;
    private static final int C_BG_MID    = 0xFFFDF3F8;
    private static final int C_BG_BOTTOM = 0xFFF2F5FF;

    public GlassBackground(Context c) {
        super(c);
        setWillNotDraw(false);
        blobPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rebuildCache(w, h);
    }

    /** 把整层背景（渐变 + 4 个模糊光斑）画进一张 Bitmap */
    private void rebuildCache(int w, int h) {
        if (w <= 0 || h <= 0) return;

        if (cache != null && !cache.isRecycled()) cache.recycle();
        cache = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(cache);

        // 1) 斜向渐变
        basePaint.setShader(new LinearGradient(
                0, 0, w * 0.75f, h * 0.55f,
                new int[]{C_BG_TOP, C_BG_MID, C_BG_BOTTOM},
                new float[]{0f, 0.38f, 1f},
                Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, basePaint);

        // 2) 四个模糊光斑（只在这里算一次）
        float density = getResources().getDisplayMetrics().density;
        float blurRadius = 70f * density;

        drawBlob(c, w * 0.02f, h * 0.02f, 340 * density / 2f,
                blurRadius, C_BLOB_PINK);
        drawBlob(c, w * 1.02f, h * 0.20f, 300 * density / 2f,
                blurRadius * 0.95f, C_BLOB_PURPLE);
        drawBlob(c, w * 0.10f, h * 1.02f, 380 * density / 2f,
                blurRadius * 1.1f, C_BLOB_BLUE);
        drawBlob(c, w * 1.00f, h * 0.86f, 260 * density / 2f,
                blurRadius * 0.9f, C_BLOB_ORANGE);

        blobPaint.setMaskFilter(null);
    }

    private void drawBlob(Canvas canvas, float cx, float cy, float radius,
                          float blur, int color) {
        blobPaint.setColor(color);
        blobPaint.setMaskFilter(new BlurMaskFilter(blur, BlurMaskFilter.Blur.NORMAL));
        canvas.drawCircle(cx, cy, radius, blobPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cache != null && !cache.isRecycled()) {
            canvas.drawBitmap(cache, 0, 0, bitmapPaint);
        } else {
            canvas.drawColor(C_BG_TOP);
        }
    }

    /** 释放缓存 */
    public void release() {
        if (cache != null && !cache.isRecycled()) {
            cache.recycle();
        }
        cache = null;
    }
}
