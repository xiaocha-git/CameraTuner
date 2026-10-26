package com.starlight.cameratuner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;

/**
 * 底部真模糊遮罩（v2 · 2026-10-26）
 *
 * 原理：
 *   1. 把 pager 的底部区域「绘制」到一张缩小版 Bitmap（缩到 1/6，省性能）
 *   2. 用 RenderEffect 高斯模糊（API 31+）
 *   3. 放大贴回遮罩区域
 *   4. 顶部叠一层「透明→底color」渐变，让模糊带柔和浮现、底部实色压住
 *
 * 为什么不用 PixelCopy：
 *   PixelCopy 是异步 + 有截屏保护风险，小米 ROM 上可能返回黑图。
 *   直接 canvas 绘制 pager 更快，也完全可控。
 *
 * 更新策略：
 *   - 静止：不重绘
 *   - 滚动中：由外部节流调用 refresh()
 *   - 滚动停止：调用一次 refresh() 出高清版
 */
public class BottomFadeView extends View {

    public interface SourceProvider {
        /** 返回被遮挡的内容容器（通常是 SwipePager） */
        View getSource();
    }

    private static final float DOWNSCALE = 6f;    // 缩小倍数，越大越省性能、越糊
    private static final float BLUR_RADIUS = 20f; // 模糊半径（源图坐标，会被放大 6 倍）

    private final Paint bmpPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private SourceProvider provider;
    private Bitmap cache;
    private Canvas cacheCanvas;
    private LinearGradient fadeGradient;

    // 底部实色（与背景一致），用于压住导航栏区域
    private static final int C_FADE_SOLID = 0xFFF2F5FF;

    public BottomFadeView(Context c) {
        super(c);
        setWillNotDraw(false);
        setClickable(false);
        setFocusable(false);
    }

    public void setSourceProvider(SourceProvider p) {
        this.provider = p;
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        if (w <= 0 || h <= 0) return;
        ensureCache(w, h);
        // 覆盖层：顶部透明 -> 55% 处开始不透明 -> 底部实色
        fadeGradient = new LinearGradient(
                0, 0, 0, h,
                new int[]{0x00F2F5FF, 0x00F2F5FF, 0xB3F2F5FF, C_FADE_SOLID},
                new float[]{0f, 0.30f, 0.72f, 1f},
                Shader.TileMode.CLAMP);
        refresh();
    }

    private void ensureCache(int w, int h) {
        int cw = Math.max(1, (int) (w / DOWNSCALE));
        int ch = Math.max(1, (int) (h / DOWNSCALE));
        if (cache == null || cache.getWidth() != cw || cache.getHeight() != ch) {
            if (cache != null && !cache.isRecycled()) cache.recycle();
            cache = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
            cacheCanvas = new Canvas(cache);
        }
    }

    /** 重新抓取下方内容并模糊。可从 UI 线程直接调用。 */
    public void refresh() {
        if (provider == null) return;
        View src = provider.getSource();
        if (src == null || cache == null || cache.isRecycled()) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        int[] myLoc = new int[2];
        int[] srcLoc = new int[2];
        getLocationInWindow(myLoc);
        src.getLocationInWindow(srcLoc);

        float dx = myLoc[0] - srcLoc[0];
        float dy = myLoc[1] - srcLoc[1];

        cacheCanvas.save();
        cacheCanvas.scale(1f / DOWNSCALE, 1f / DOWNSCALE);
        cacheCanvas.translate(-dx, -dy);
        src.draw(cacheCanvas);
        cacheCanvas.restore();

        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(RenderEffect.createBlurEffect(
                    BLUR_RADIUS, BLUR_RADIUS, Shader.TileMode.CLAMP));
        }
        invalidate();
    }

    /** 清理缓存（页面销毁时调用） */
    public void release() {
        if (cache != null && !cache.isRecycled()) {
            cache.recycle();
        }
        cache = null;
        cacheCanvas = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 1) 模糊后的内容
        if (cache != null && !cache.isRecycled()) {
            Rect dst = new Rect(0, 0, w, h);
            canvas.drawBitmap(cache, null, dst, bmpPaint);
        }

        // 2) 顶部渐隐 -> 底部实色，让模糊带柔和浮现、底部压住
        if (fadeGradient != null) {
            fadePaint.setShader(fadeGradient);
            canvas.drawRect(0, 0, w, h, fadePaint);
        }
    }
}
