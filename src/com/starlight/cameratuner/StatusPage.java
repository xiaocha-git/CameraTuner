package com.starlight.cameratuner;

import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** 状态页 */
public class StatusPage {

    private final MainActivity a;

    StatusPage(MainActivity a) { this.a = a; }

    ScrollView build() {
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(MainActivity.C_BG);
        sv.setClipToPadding(false);

        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(150));

        c.addView(Ui.pageTitle(a, "Xiaomi CameraTuner"));

        // ===== 模块状态 =====
        String statusJson = readStatus();
        int hookCount = parseHookCount(statusJson);
        String lastTime = parseValue(statusJson, "time");

        // 判断：有 status.json 且成功了 ≥1 项 hook → 运行中
        boolean running = statusJson != null && hookCount > 0;

        LinearLayout card0 = Ui.card(a);
        LinearLayout stRow = new LinearLayout(a);
        stRow.setOrientation(LinearLayout.HORIZONTAL);
        stRow.setGravity(Gravity.CENTER_VERTICAL);
        stRow.setPadding(a.dp(20), a.dp(18), a.dp(20), a.dp(18));

        View dot = new View(a);
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(running ? MainActivity.C_GREEN : 0xFFCCCCCC);
        dot.setBackground(dotBg);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(a.dp(10), a.dp(10));
        dotLp.rightMargin = a.dp(10);
        dot.setLayoutParams(dotLp);
        stRow.addView(dot);

        LinearLayout stCol = new LinearLayout(a);
        stCol.setOrientation(LinearLayout.VERTICAL);
        stCol.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView stTitle = new TextView(a);
        stTitle.setText(running ? "模块运行中" : "模块未运行");
        stTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        stTitle.setTextColor(MainActivity.C_TEXT_MAIN);
        stTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        stCol.addView(stTitle);

        TextView stSub = new TextView(a);
        if (running) {
            stSub.setText("已加载 " + hookCount + " 项 hook" + (lastTime != null ? " · " + lastTime : ""));
        } else {
            stSub.setText("启动相机后写入状态，或检查 LSPosed 作用域");
        }
        stSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        stSub.setTextColor(MainActivity.C_TEXT_SUB);
        stSub.setPadding(0, a.dp(3), 0, 0);
        stCol.addView(stSub);

        stRow.addView(stCol);
        card0.addView(stRow);

        // 内嵌：版本框
        LinearLayout verBox = new LinearLayout(a);
        verBox.setOrientation(LinearLayout.VERTICAL);
        verBox.setPadding(a.dp(14), a.dp(12), a.dp(14), a.dp(12));
        GradientDrawable vbg = new GradientDrawable();
        vbg.setColor(0xFFF7F7FA);
        vbg.setCornerRadius(a.dp(12));
        verBox.setBackground(vbg);
        LinearLayout.LayoutParams vLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vLp.leftMargin = a.dp(16);
        vLp.rightMargin = a.dp(16);
        vLp.bottomMargin = a.dp(16);
        vLp.topMargin = a.dp(6);
        verBox.setLayoutParams(vLp);

        String myVer = "?";
        try {
            myVer = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Throwable ignored) {}

        // 当前版本行
        LinearLayout vrow1 = new LinearLayout(a);
        vrow1.setOrientation(LinearLayout.HORIZONTAL);
        vrow1.setGravity(Gravity.CENTER_VERTICAL);
        TextView vl = new TextView(a);
        vl.setText("当前版本");
        vl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        vl.setTextColor(MainActivity.C_TEXT_SUB);
        vl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        vrow1.addView(vl);
        TextView vv = new TextView(a);
        vv.setText("v" + myVer);
        vv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        vv.setTextColor(MainActivity.C_TEXT_MAIN);
        vrow1.addView(vv);
        verBox.addView(vrow1);

        // 更新状态行
        if (AutoUpdateCheck.hasUpdate) {
            View sep = new View(a);
            sep.setBackgroundColor(0xFFE8E8ED);
            LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, a.dp(1));
            sepLp.topMargin = a.dp(8);
            sepLp.bottomMargin = a.dp(8);
            verBox.addView(sep, sepLp);

            LinearLayout vrow2 = new LinearLayout(a);
            vrow2.setOrientation(LinearLayout.HORIZONTAL);
            vrow2.setGravity(Gravity.CENTER_VERTICAL);
            vrow2.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { AutoUpdateCheck.showUpdateDialog(a); }
            });

            TextView ul = new TextView(a);
            ul.setText("更新状态");
            ul.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            ul.setTextColor(MainActivity.C_TEXT_SUB);
            ul.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            vrow2.addView(ul);

            TextView pill = new TextView(a);
            pill.setText("新版本 v" + (AutoUpdateCheck.latestVersion == null ? "?" : AutoUpdateCheck.latestVersion));
            pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            pill.setTextColor(0xFFFFFFFF);
            pill.setGravity(Gravity.CENTER);
            pill.setPadding(a.dp(10), a.dp(4), a.dp(10), a.dp(4));
            GradientDrawable pbg = new GradientDrawable();
            pbg.setColor(MainActivity.C_ACCENT);
            pbg.setCornerRadius(a.dp(10));
            pill.setBackground(pbg);
            LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pillLp.rightMargin = a.dp(6);
            pill.setLayoutParams(pillLp);
            vrow2.addView(pill);

            TextView arrow2 = new TextView(a);
            arrow2.setText("›");
            arrow2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            arrow2.setTextColor(0xFFC7C7CC);
            vrow2.addView(arrow2);

            verBox.addView(vrow2);
        } else if (AutoUpdateCheck.checked) {
            View sep = new View(a);
            sep.setBackgroundColor(0xFFE8E8ED);
            LinearLayout.LayoutParams sepLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, a.dp(1));
            sepLp.topMargin = a.dp(8);
            sepLp.bottomMargin = a.dp(8);
            verBox.addView(sep, sepLp);

            LinearLayout vrow2 = new LinearLayout(a);
            vrow2.setOrientation(LinearLayout.HORIZONTAL);
            vrow2.setGravity(Gravity.CENTER_VERTICAL);
            TextView ul = new TextView(a);
            ul.setText("更新状态");
            ul.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            ul.setTextColor(MainActivity.C_TEXT_SUB);
            ul.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            vrow2.addView(ul);
            TextView uv = new TextView(a);
            uv.setText("已是最新版本");
            uv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            uv.setTextColor(MainActivity.C_GREEN);
            vrow2.addView(uv);
            verBox.addView(vrow2);
        }

        card0.addView(verBox);
        c.addView(card0);

        // ===== 设备信息 =====
        c.addView(Ui.sectionLabel(a, "设备信息"));
        LinearLayout card1 = Ui.card(a);
        addRow(card1, "设备型号", Build.MODEL, 0);
        Ui.divider(a, card1);
        addRow(card1, "设备代号", Build.DEVICE, 0);
        Ui.divider(a, card1);
        String miOs = getProp("ro.mi.os.version.name");
        String sysVer = (miOs != null && !miOs.isEmpty()) ? miOs : Build.DISPLAY;
        addRow(card1, "系统版本", sysVer, 0);
        Ui.divider(a, card1);
        addRow(card1, "Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")", 0);
        c.addView(card1);

        // ===== 相机状态 =====
        c.addView(Ui.sectionLabel(a, "相机状态"));

        String ver = "未安装";
        boolean installed = false;
        try {
            PackageInfo pi = a.getPackageManager().getPackageInfo(MainActivity.CAMERA_PKG, 0);
            ver = pi.versionName;
            installed = true;
        } catch (Throwable ignored) {}
        boolean verOk = installed && MainActivity.EXPECTED_VERSION.equals(ver);

        LinearLayout card2 = Ui.card(a);
        addPillRow(card2, "相机版本", ver,
                installed ? (verOk ? MainActivity.C_GREEN : MainActivity.C_ACCENT) : MainActivity.C_RED,
                !installed ? "未安装" : (verOk ? "已验证" : "未验证"));
        Ui.divider(a, card2);
        addPillRow(card2, "Root 权限", null,
                a.hasRoot() ? MainActivity.C_GREEN : MainActivity.C_RED,
                a.hasRoot() ? "已授权" : "未授权");
        Ui.divider(a, card2);
        addPillRow(card2, "文件访问", null,
                a.hasStoragePerm() ? MainActivity.C_GREEN : MainActivity.C_RED,
                a.hasStoragePerm() ? "已授权" : "需要授权");
        Ui.divider(a, card2);
        boolean confOk = new File(MainActivity.CONF_PATH).exists();
        addPillRow(card2, "配置文件", null,
                confOk ? MainActivity.C_GREEN : MainActivity.C_ACCENT,
                confOk ? "已生成" : "未生成");
        c.addView(card2);

        if (installed && !verOk) {
            TextView warn = new TextView(a);
            warn.setText("当前相机版本与已验证版本（" + MainActivity.EXPECTED_VERSION
                    + "）不同，部分功能可能无法生效。");
            warn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            warn.setTextColor(MainActivity.C_ACCENT);
            warn.setPadding(a.dp(8), a.dp(12), a.dp(8), 0);
            c.addView(warn);
        }

        // ===== 快捷操作 =====
        c.addView(Ui.sectionLabel(a, "快捷操作"));
        LinearLayout card3 = Ui.card(a);
        Ui.clickRow(a, card3, "重启相机进程", "强制结束相机，使配置生效", new Runnable() {
            public void run() { confirmRestart(); }
        });
        c.addView(card3);

        TextView hint = new TextView(a);
        hint.setText("修改任何配置后，需重启相机进程才会生效。");
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hint.setTextColor(MainActivity.C_TEXT_SUB);
        hint.setPadding(a.dp(8), a.dp(10), a.dp(8), 0);
        c.addView(hint);

        sv.addView(c);
        return sv;
    }

    // ---------- 行组件 ----------

    private void addRow(LinearLayout card, String label, String value, int valueColor) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(a.dp(20), a.dp(15), a.dp(20), a.dp(15));

        TextView l = new TextView(a);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        l.setTextColor(MainActivity.C_TEXT_MAIN);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        TextView v = new TextView(a);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        v.setTextColor(valueColor == 0 ? MainActivity.C_TEXT_SUB : valueColor);
        row.addView(v);

        card.addView(row);
    }

    /** 带彩色胶囊的行 */
    private void addPillRow(LinearLayout card, String label, String extra,
                            int pillColor, String pillText) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(a.dp(20), a.dp(14), a.dp(20), a.dp(14));

        TextView l = new TextView(a);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        l.setTextColor(MainActivity.C_TEXT_MAIN);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(l);

        if (extra != null) {
            TextView ex = new TextView(a);
            ex.setText(extra);
            ex.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            ex.setTextColor(MainActivity.C_TEXT_SUB);
            ex.setPadding(0, 0, a.dp(10), 0);
            row.addView(ex);
        }

        TextView pill = new TextView(a);
        pill.setText(pillText);
        pill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        pill.setTextColor(0xFFFFFFFF);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(a.dp(10), a.dp(4), a.dp(10), a.dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(pillColor);
        bg.setCornerRadius(a.dp(10));
        pill.setBackground(bg);
        row.addView(pill);

        card.addView(row);
    }

    // ---------- 数据 ----------

    private String readStatus() {
        File f = new File(MainActivity.CONF_DIR + "/status.json");
        if (!f.exists()) return null;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    private int parseHookCount(String json) {
        if (json == null) return 0;
        try {
            int i = json.indexOf("\"hooks\":");
            if (i < 0) return 0;
            int start = i + 8;
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Throwable t) {
            return 0;
        }
    }

    private String parseValue(String json, String key) {
        if (json == null) return null;
        try {
            int i = json.indexOf("\"" + key + "\":");
            if (i < 0) return null;
            int q1 = json.indexOf('"', i + key.length() + 3);
            if (q1 < 0) return null;
            int q2 = json.indexOf('"', q1 + 1);
            if (q2 < 0) return null;
            return json.substring(q1 + 1, q2);
        } catch (Throwable t) {
            return null;
        }
    }

    private String getProp(String key) {
        try {
            Class<?> sp = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method m = sp.getMethod("get", String.class, String.class);
            return (String) m.invoke(null, key, "");
        } catch (Throwable t) {
            return "";
        }
    }

    private void confirmRestart() {
        new android.app.AlertDialog.Builder(a)
                .setTitle("重启相机")
                .setMessage("将强制结束相机进程，运行中的拍摄会中断。继续？")
                .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        a.execRootAsync("am force-stop " + MainActivity.CAMERA_PKG, new Runnable() {
                            public void run() {
                                android.widget.Toast.makeText(a, "相机已结束",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
