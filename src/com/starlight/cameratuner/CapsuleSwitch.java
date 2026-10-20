package com.starlight.cameratuner;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * 胶囊开关（仿 MIUI 风格）
 * 关闭：浅灰轨道 + 白圆点
 * 打开：主色轨道 + 白圆点（靠右）
 */
public class CapsuleSwitch extends View {

    public interface OnCheckedChangeListener {
        void onCheckedChanged(CapsuleSwitch sw, boolean checked);
    }

    private boolean checked = false;
    private float progress = 0f;  // 0..1 动画进度
    private int accentColor = 0xFFE8A33D;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();
    private ValueAnimator animator;
    private OnCheckedChangeListener listener;

    public CapsuleSwitch(Context c) { super(c); init(); }
    public CapsuleSwitch(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setClickable(true);
        trackPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setStyle(Paint.Style.FILL);
        thumbPaint.setColor(Color.WHITE);
        thumbPaint.setShadowLayer(2f, 0f, 1f, 0x33000000);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        invalidate();
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener l) {
        this.listener = l;
    }

    public boolean isChecked() { return checked; }

    public void setChecked(boolean v) {
        if (checked == v) return;
        checked = v;
        animateTo(v ? 1f : 0f);
    }

    public void setCheckedImmediate(boolean v) {
        checked = v;
        progress = v ? 1f : 0f;
        invalidate();
    }

    private void animateTo(float target) {
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(progress, target);
        animator.setDuration(180);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                progress = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = (int) dp(52);
        int h = (int) dp(32);
        setMeasuredDimension(resolveSize(w, wSpec), resolveSize(h, hSpec));
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth();
        float h = getHeight();
        float r = h / 2f;

        // 轨道颜色：关闭浅灰 -> 打开主色
        int offColor = 0xFFE3E3E8;
        int color = blend(offColor, accentColor, progress);
        trackPaint.setColor(color);
        trackRect.set(0, 0, w, h);
        c.drawRoundRect(trackRect, r, r, trackPaint);

        // 圆点
        float pad = dp(3);
        float thumbR = r - pad;
        float leftX = r;
        float rightX = w - r;
        float cx = leftX + (rightX - leftX) * progress;
        c.drawCircle(cx, r, thumbR, thumbPaint);
    }

    private int blend(int from, int to, float t) {
        int a = (int) (Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = (int) (Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = (int) (Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = (int) (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }

    @Override
    public boolean performClick() {
        setChecked(!checked);
        if (listener != null) listener.onCheckedChanged(this, checked);
        return super.performClick();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
