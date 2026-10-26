package com.starlight.cameratuner;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 统一样式的对话框  2026-10-27 改版
 *
 * 风格与主界面一致：
 *   - 玻璃白背景（95% 白）+ 细描边 + 大圆角
 *   - 主按钮：玫红实心胶囊
 *   - 次按钮：浅灰胶囊
 *   - 输入框：白底 + 描边，聚焦时描边变玫红
 *   - 背后有 45% 压暗遮罩
 */
public class TunerDialog extends Dialog {

    public interface OnAction {
        void onClick(TunerDialog d);
    }

    // ---------- 配色 ----------
    private static final int C_SURFACE      = 0xF7FFFFFF;  // 弹窗底：97% 白
    private static final int C_SURFACE_EDGE = 0xE6FFFFFF;  // 描边
    private static final int C_TEXT_MAIN    = 0xFF1C1C1E;
    private static final int C_TEXT_SUB     = 0xFF8E8A94;
    private static final int C_ACCENT       = 0xFFE0567C;  // 玫红
    private static final int C_ACCENT_SOFT  = 0x33E0567C;  // 玫红淡（聚焦描边）
    private static final int C_FIELD        = 0xFFFFFFFF;  // 输入框底
    private static final int C_FIELD_EDGE   = 0xFFE8DFE4;  // 输入框描边
    private static final int C_BTN_SECONDARY= 0xFFF1EFF2;  // 次按钮底
    private static final int C_BTN_SEC_TEXT = 0xFF4A4652;
    private static final int C_DIVIDER      = 0x0D000000;

    private String title;
    private String message;
    private EditText editText;
    private boolean editable;

    private String positiveText = "确定";
    // 默认不显示次按钮：提示类弹窗只需要一个按钮。
    // 需要取消的确认框请显式调用 .negative("取消", null)
    private String negativeText = null;
    private String neutralText;

    private OnAction positiveCb;
    private OnAction negativeCb;
    private OnAction neutralCb;

    private int contentMaxHeight;
    private boolean singleLine;
    private View customView;

    public TunerDialog(Context c) {
        super(c);
    }

    public static TunerDialog create(Context c) {
        TunerDialog d = new TunerDialog(c);
        d.contentMaxHeight = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.55f);
        return d;
    }

    public TunerDialog title(String t) { this.title = t; return this; }

    public TunerDialog message(String m) { this.message = m; return this; }

    public TunerDialog editable(String text) {
        this.editable = true;
        this.message = text;
        return this;
    }

    public TunerDialog singleLine() {
        this.singleLine = true;
        return this;
    }

    public EditText getEditText() { return editText; }

    /** 用自定义 View 作为内容区（优先级最高） */
    public TunerDialog customView(View v) { this.customView = v; return this; }

    public TunerDialog positive(String t, OnAction cb) {
        this.positiveText = t; this.positiveCb = cb; return this;
    }

    public TunerDialog negative(String t, OnAction cb) {
        this.negativeText = t; this.negativeCb = cb; return this;
    }

    public TunerDialog neutral(String t, OnAction cb) {
        this.neutralText = t; this.neutralCb = cb; return this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Context ctx = getContext();
        int d = dp(ctx, 1);

        // ---------- 根容器 ----------
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(ctx, 24), dp(ctx, 22), dp(ctx, 24), dp(ctx, 16));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(C_SURFACE);
        bg.setCornerRadius(dp(ctx, 24));
        bg.setStroke(dp(ctx, 1), C_SURFACE_EDGE);
        root.setBackground(bg);

        // ---------- 标题 ----------
        if (title != null && !title.isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText(title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tv.setTextColor(C_TEXT_MAIN);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setIncludeFontPadding(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(ctx, 14);
            root.addView(tv, lp);
        }

        // ---------- 内容 ----------
        if (customView != null) {
            // 保留传入 View 自己的 LayoutParams（例如 ScrollView 的固定高度）
            LinearLayout.LayoutParams clp;
            if (customView.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                clp = (LinearLayout.LayoutParams) customView.getLayoutParams();
            } else {
                clp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            root.addView(customView, clp);
        } else if (editable && singleLine) {
            editText = buildEditText(ctx, true);
            editText.setText(message == null ? "" : message);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 52));
            root.addView(editText, lp2);
            focusAndShowKeyboard(ctx, editText);
        } else if (editable) {
            editText = buildEditText(ctx, false);
            editText.setText(message == null ? "" : message);

            ScrollView sv = new ScrollView(ctx);
            GradientDrawable edThumb = new GradientDrawable();
            edThumb.setColor(0x3DE0567C);
            edThumb.setCornerRadius(dp(ctx, 4));
            edThumb.setSize(dp(ctx, 3), dp(ctx, 32));
            sv.setVerticalScrollbarThumbDrawable(edThumb);
            sv.setScrollbarFadingEnabled(false);
            sv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            GradientDrawable eb = fieldBg(ctx, false);
            sv.setBackground(eb);
            sv.setPadding(dp(ctx, 12), dp(ctx, 10), dp(ctx, 12), dp(ctx, 10));
            sv.addView(editText, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            sv.setFillViewport(false);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, contentMaxHeight);
            root.addView(sv, lp);

            // 焦点时高亮描边
            final ScrollView svRef = sv;
            editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    svRef.setBackground(fieldBg(v.getContext(), hasFocus));
                }
            });
        } else if (message != null) {
            ScrollView sv = new ScrollView(ctx);
            GradientDrawable thumb = new GradientDrawable();
            thumb.setColor(0x45E0567C);
            thumb.setCornerRadius(dp(ctx, 4));
            thumb.setSize(dp(ctx, 3), dp(ctx, 36));
            sv.setVerticalScrollbarThumbDrawable(thumb);
            sv.setScrollbarFadingEnabled(false);
            sv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            TextView tv = new TextView(ctx);
            tv.setText(message);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTextColor(0xFF3A3A3C);
            tv.setLineSpacing(dp(ctx, 5), 1f);
            sv.addView(tv);

            int h = Math.min(contentMaxHeight, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h);
            root.addView(sv, lp);
        }

        // ---------- 分隔线 ----------
        View line = new View(ctx);
        line.setBackgroundColor(C_DIVIDER);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1));
        lineLp.topMargin = dp(ctx, 20);
        root.addView(line, lineLp);

        // ---------- 按钮区 ----------
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.topMargin = dp(ctx, 14);

        if (neutralText != null) {
            View b = makeBtn(ctx, neutralText, false, false, new View.OnClickListener() {
                public void onClick(View v) {
                    if (neutralCb != null) neutralCb.onClick(TunerDialog.this);
                    else dismiss();
                }
            });
            btnRow.addView(b);
        }
        if (negativeText != null) {
            View b = makeBtn(ctx, negativeText, false, false, new View.OnClickListener() {
                public void onClick(View v) {
                    if (negativeCb != null) negativeCb.onClick(TunerDialog.this);
                    dismiss();
                }
            });
            btnRow.addView(b);
        }
        View pb = makeBtn(ctx, positiveText, true, true, new View.OnClickListener() {
            public void onClick(View v) {
                if (positiveCb != null) positiveCb.onClick(TunerDialog.this);
                dismiss();
            }
        });
        btnRow.addView(pb);

        root.addView(btnRow, rowLp);

        setContentView(root);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            // 背后压暗，让弹窗"浮"起来
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.45f;
            w.setAttributes(lp);

            int width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.86f);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    // ==================== 组件构建 ====================

    /** 输入框：白底 + 描边 + 圆角，聚焦时描边变玫红 */
    private EditText buildEditText(Context ctx, boolean single) {
        EditText et = new EditText(ctx);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, single ? 15 : 12.5f);
        et.setTextColor(C_TEXT_MAIN);
        et.setBackground(fieldBg(ctx, false));
        et.setSingleLine(single);

        if (single) {
            et.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            et.setPadding(dp(ctx, 16), 0, dp(ctx, 16), 0);
            et.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        } else {
            et.setGravity(Gravity.TOP | Gravity.START);
            et.setBackgroundColor(0x00000000);
            et.setPadding(dp(ctx, 2), dp(ctx, 2), dp(ctx, 2), dp(ctx, 2));
            et.setLineSpacing(dp(ctx, 3), 1f);
        }

        et.setHintTextColor(0xFFB9B3BE);

        if (single) {
            et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setBackground(fieldBg(v.getContext(), hasFocus));
                }
            });
        }
        return et;
    }

    /** 输入框背景（聚焦时描边变玫红） */
    private GradientDrawable fieldBg(Context ctx, boolean focused) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(C_FIELD);
        g.setCornerRadius(dp(ctx, 14));
        if (focused) {
            g.setStroke(dp(ctx, 2), C_ACCENT);
        } else {
            g.setStroke((int) (dp(ctx, 1) * 1.2f), C_FIELD_EDGE);
        }
        return g;
    }

    /** 胶囊按钮 */
    private View makeBtn(Context ctx, String text, boolean primary, boolean bold, View.OnClickListener l) {
        TextView b = new TextView(ctx);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTypeface(bold ? Typeface.create("sans-serif-medium", Typeface.NORMAL) : Typeface.DEFAULT);
        b.setGravity(Gravity.CENTER);
        b.setIncludeFontPadding(false);
        b.setPadding(dp(ctx, 20), dp(ctx, 11), dp(ctx, 20), dp(ctx, 11));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(ctx, 16));
        if (primary) {
            bg.setColor(C_ACCENT);
            b.setTextColor(0xFFFFFFFF);
        } else {
            bg.setColor(C_BTN_SECONDARY);
            b.setTextColor(C_BTN_SEC_TEXT);
        }
        b.setBackground(bg);

        // 按压缩放反馈
        b.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, android.view.MotionEvent e) {
                switch (e.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        break;
                }
                return false;
            }
        });

        b.setOnClickListener(l);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 8);
        b.setLayoutParams(lp);
        return b;
    }

    private void focusAndShowKeyboard(final Context ctx, final EditText et) {
        et.postDelayed(new Runnable() {
            public void run() {
                et.requestFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(et,
                        android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 150);
    }

    private int dp(Context ctx, float v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }
}
