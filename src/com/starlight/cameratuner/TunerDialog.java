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
 * 统一样式的对话框：
 * 白色圆角卡片 + 标题 + 内容 + 底部按钮
 */
public class TunerDialog extends Dialog {

    public interface OnAction {
        void onClick(TunerDialog d);
    }

    private String title;
    private String message;
    private EditText editText;
    private boolean editable;

    private String positiveText = "确定";
    private String negativeText = "取消";
    private String neutralText;

    private OnAction positiveCb;
    private OnAction negativeCb;
    private OnAction neutralCb;

    private int contentMaxHeight;
    private boolean singleLine;

    public TunerDialog(Context c) {
        super(c);
    }

    public static TunerDialog create(Context c) {
        TunerDialog d = new TunerDialog(c);
        d.contentMaxHeight = (int) (c.getResources().getDisplayMetrics().heightPixels * 0.6f);
        return d;
    }

    public TunerDialog title(String t) { this.title = t; return this; }

    public TunerDialog message(String m) { this.message = m; return this; }

    /** 可编辑模式：显示多行输入框 */
    public TunerDialog editable(String text) {
        this.editable = true;
        this.message = text;
        return this;
    }

    /** 单行输入模式：用于短文本，小框 + 正常弹键盘 */
    public TunerDialog singleLine() {
        this.singleLine = true;
        return this;
    }

    public EditText getEditText() { return editText; }

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
        float density = ctx.getResources().getDisplayMetrics().density;
        int dp = (int) density;

        // 根容器
        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(14));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(20));
        root.setBackground(bg);

        // 标题
        if (title != null && !title.isEmpty()) {
            TextView tv = new TextView(ctx);
            tv.setText(title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            tv.setTextColor(0xFF1C1C1E);
            tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tv.setPadding(0, 0, 0, dp(14));
            root.addView(tv);
        }

        // 内容
        if (editable && singleLine) {
            editText = new EditText(ctx);
            editText.setText(message == null ? "" : message);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            editText.setTextColor(0xFF1C1C1E);
            editText.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            editText.setSingleLine(true);
            editText.setPadding(dp(4), dp(2), dp(4), dp(2));
            editText.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
            GradientDrawable eb2 = new GradientDrawable();
            eb2.setColor(0xFFF7F7FA);
            eb2.setCornerRadius(dp(12));
            editText.setBackground(eb2);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
            editText.setLayoutParams(lp2);
            editText.postDelayed(new Runnable() {
                public void run() {
                    editText.requestFocus();
                    android.view.inputmethod.InputMethodManager imm =
                            (android.view.inputmethod.InputMethodManager)
                                    ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showSoftInput(editText,
                            android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }, 150);
            root.addView(editText);
        } else if (editable) {
            editText = new EditText(ctx);
            editText.setText(message == null ? "" : message);
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            editText.setTextColor(0xFF1C1C1E);
            editText.setGravity(Gravity.TOP | Gravity.START);
            editText.setBackgroundColor(0x00000000);
            editText.setPadding(0, 0, 0, 0);
            editText.setSingleLine(false);

            ScrollView sv = new ScrollView(ctx);
            GradientDrawable eb = new GradientDrawable();
            eb.setColor(0xFFF7F7FA);
            eb.setCornerRadius(dp(12));
            sv.setBackground(eb);
            sv.setPadding(dp(14), dp(12), dp(14), dp(12));
            sv.addView(editText, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            sv.setFillViewport(false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, contentMaxHeight);
            sv.setLayoutParams(lp);
            root.addView(sv);
        } else if (message != null) {
            ScrollView sv = new ScrollView(ctx);
            TextView tv = new TextView(ctx);
            tv.setText(message);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            tv.setTextColor(0xFF3A3A3C);
            tv.setLineSpacing(dp(5), 1f);
            sv.addView(tv);

            int h = Math.min(contentMaxHeight, ViewGroup.LayoutParams.WRAP_CONTENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, h);
            sv.setLayoutParams(lp);
            root.addView(sv);
        }

        // 按钮区
        LinearLayout btnRow = new LinearLayout(ctx);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(Gravity.END);
        btnRow.setPadding(0, dp(18), 0, 0);

        if (neutralText != null) {
            btnRow.addView(makeBtn(ctx, neutralText, 0xFF8E8E93, false, new View.OnClickListener() {
                public void onClick(View v) {
                    if (neutralCb != null) neutralCb.onClick(TunerDialog.this);
                    else dismiss();
                }
            }));
        }
        if (negativeText != null) {
            btnRow.addView(makeBtn(ctx, negativeText, 0xFF8E8E93, false, new View.OnClickListener() {
                public void onClick(View v) {
                    if (negativeCb != null) negativeCb.onClick(TunerDialog.this);
                    dismiss();
                }
            }));
        }
        btnRow.addView(makeBtn(ctx, positiveText, 0xFFE8A33D, true, new View.OnClickListener() {
            public void onClick(View v) {
                if (positiveCb != null) positiveCb.onClick(TunerDialog.this);
                dismiss();
            }
        }));

        root.addView(btnRow);

        setContentView(root);

        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawableResource(android.R.color.transparent);
            int width = (int) (ctx.getResources().getDisplayMetrics().widthPixels * 0.88f);
            w.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dp(int v) {
        return (int) (v * getContext().getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView makeBtn(Context ctx, String text, int color, boolean bold, View.OnClickListener l) {
        int dp = (int) ctx.getResources().getDisplayMetrics().density;
        TextView b = new TextView(ctx);
        b.setText(text);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        b.setTextColor(color);
        b.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        b.setOnClickListener(l);
        return b;
    }
}
