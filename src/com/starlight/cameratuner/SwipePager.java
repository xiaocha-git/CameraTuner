package com.starlight.cameratuner;

import android.content.Context;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Scroller;

/**
 * 简易横向滑动分页容器：
 * - 手指拖动切换
 * - 松手吸附
 * - 提供 onPageSelected 回调
 */
public class SwipePager extends ViewGroup {

    public interface OnPageListener {
        void onPageSelected(int index);
    }

    private final Scroller scroller;
    private final int touchSlop;
    private final int minFling;
    private VelocityTracker tracker;

    private int currentPage = 0;
    private float downX;
    private float lastX;
    private boolean dragging;
    private OnPageListener listener;

    public SwipePager(Context context) {
        super(context);
        scroller = new Scroller(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        minFling = ViewConfiguration.get(context).getScaledMinimumFlingVelocity();
    }

    public void setPageListener(OnPageListener l) {
        this.listener = l;
    }

    public int getCurrentItem() { return currentPage; }

    public void setPages(View[] pages) {
        removeAllViews();
        for (View p : pages) {
            if (p != null) addView(p);
        }
        requestLayout();
    }

    public void setCurrentItem(int index, boolean smooth) {
        if (index < 0 || index >= getChildCount()) return;
        boolean changed = index != currentPage;
        currentPage = index;
        int target = index * getWidth();
        if (smooth) {
            scroller.startScroll(getScrollX(), 0, target - getScrollX(), 0, 260);
            invalidate();
        } else {
            scrollTo(target, 0);
        }
        if (changed && listener != null) listener.onPageSelected(index);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int h = MeasureSpec.getSize(heightSpec);
        setMeasuredDimension(w, h);
        int childH = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), childH);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w = r - l;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.layout(i * w, 0, (i + 1) * w, b - t);
        }
        // 保证当前页位置正确
        if (!scroller.computeScrollOffset()) {
            scrollTo(currentPage * w, 0);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                lastX = downX;
                dragging = false;
                if (tracker == null) tracker = VelocityTracker.obtain();
                tracker.clear();
                tracker.addMovement(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                if (dx > touchSlop) {
                    dragging = true;
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (tracker == null) tracker = VelocityTracker.obtain();
        tracker.addMovement(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                lastX = downX;
                if (!scroller.isFinished()) scroller.abortAnimation();
                return true;

            case MotionEvent.ACTION_MOVE: {
                float x = ev.getX();
                float dx = lastX - x;
                lastX = x;

                int maxScroll = (getChildCount() - 1) * getWidth();
                int target = (int) (getScrollX() + dx);
                if (target < 0) target = 0;
                if (target > maxScroll) target = maxScroll;
                scrollTo(target, 0);
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                tracker.computeCurrentVelocity(1000);
                float vx = tracker.getXVelocity();
                int pageWidth = getWidth();

                int target = currentPage;
                if (Math.abs(vx) > minFling) {
                    target = vx < 0 ? currentPage + 1 : currentPage - 1;
                } else {
                    float offset = (float) getScrollX() / pageWidth;
                    target = Math.round(offset);
                }
                if (target < 0) target = 0;
                if (target > getChildCount() - 1) target = getChildCount() - 1;

                boolean changed = target != currentPage;
                currentPage = target;
                scroller.startScroll(getScrollX(), 0,
                        target * pageWidth - getScrollX(), 0, 240);
                invalidate();
                if (changed && listener != null) listener.onPageSelected(target);

                if (tracker != null) { tracker.recycle(); tracker = null; }
                return true;
            }
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), 0);
            postInvalidateOnAnimation();
        }
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return true;
    }
}
