package com.starlight.cameratuner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** 关于页 */
public class AboutPage {

    private final MainActivity a;

    AboutPage(MainActivity a) { this.a = a; }

    ScrollView build() {
        ScrollView sv = new ScrollView(a);
        Ui.customScrollbar(a, sv);
        sv.setBackgroundColor(0x00000000);
        sv.setClipToPadding(false);

        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(a.dp(16), a.getStatusBarHeight() + a.dp(8), a.dp(16), a.dp(110));

        c.addView(Ui.pageTitle(a, "关于"));

        // ===== 应用信息 =====
        LinearLayout card1 = Ui.card(a);
        LinearLayout head = new LinearLayout(a);
        head.setOrientation(LinearLayout.VERTICAL);
        head.setPadding(a.dp(20), a.dp(20), a.dp(20), a.dp(16));

        // 图标 + 渐变标题
        LinearLayout titleRow = new LinearLayout(a);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        android.widget.ImageView appIcon = new android.widget.ImageView(a);
        appIcon.setImageResource(R.mipmap.ic_launcher);
        appIcon.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(a.dp(44), a.dp(44));
        iconLp.rightMargin = a.dp(14);
        appIcon.setLayoutParams(iconLp);
        titleRow.addView(appIcon);

        final TextView titleText = new TextView(a) {
            private android.graphics.LinearGradient gradient;

            @Override
            protected void onSizeChanged(int w, int h, int ow, int oh) {
                super.onSizeChanged(w, h, ow, oh);
                if (w > 0) {
                    gradient = new android.graphics.LinearGradient(
                            0, 0, w, 0,
                            new int[]{0xFFE88FA8, 0xFFF0B06A, 0xFF9E8FD0},
                            new float[]{0f, 0.55f, 1f},
                            android.graphics.Shader.TileMode.CLAMP);
                    getPaint().setShader(gradient);
                    invalidate();
                }
            }

            @Override
            protected void onDraw(android.graphics.Canvas c) {
                if (gradient != null) getPaint().setShader(gradient);
                super.onDraw(c);
            }
        };
        titleText.setText("Xiaomi CameraTuner");
        titleText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        titleText.setTextColor(0xFFE88FA8);
        titleText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titleText.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(titleText);

        head.addView(titleRow);

        card1.addView(head);
        Ui.divider(a, card1);
        Ui.clickRow(a, card1, "检查更新", "当前版本 v" + getVersionName() + " · 点击检查", new Runnable() {
            public void run() { checkUpdate(); }
        });
        Ui.divider(a, card1);
        Ui.clickRow(a, card1, "更新日志", "查看各版本更新内容", new Runnable() {
            public void run() { showChangelog(); }
        });
        c.addView(card1);

        // ===== 作者 =====
        c.addView(Ui.sectionLabel(a, "作者"));
        LinearLayout card2 = Ui.card(a);
        Ui.clickRow(a, card2, "小茶", "酷安主页 · 点击访问", new Runnable() {
            public void run() { openCoolApk(); }
        });
        c.addView(card2);

        // ===== 致谢 =====
        c.addView(Ui.sectionLabel(a, "致谢"));
        LinearLayout card3 = Ui.card(a);

        LinearLayout thanksBox = new LinearLayout(a);
        thanksBox.setOrientation(LinearLayout.VERTICAL);
        thanksBox.setPadding(a.dp(20), a.dp(18), a.dp(20), a.dp(18));

        TextView thanksText = new TextView(a);
        thanksText.setText("本模块部分功能参考自 XiaomiCamPort 的实现思路，包括焦段重映射、档位循环修正等。");
        thanksText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        thanksText.setTextColor(MainActivity.C_TEXT_MAIN);
        thanksText.setLineSpacing(a.dp(4), 1f);
        thanksBox.addView(thanksText);

        TextView thanksAuthor = new TextView(a);
        thanksAuthor.setText("酷安 @歪锅包肉RoddyWise");
        thanksAuthor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        thanksAuthor.setTextColor(MainActivity.C_ACCENT);
        thanksAuthor.setPadding(0, a.dp(10), 0, 0);
        thanksBox.addView(thanksAuthor);

        card3.addView(thanksBox);

        Ui.divider(a, card3);
        Ui.infoRow(a, card3, "模块框架", "LSPosed / Vector");

        c.addView(card3);

        // ===== 使用说明（分步）=====
        c.addView(Ui.sectionLabel(a, "使用说明"));
        String[] steps = {
                "授予「所有文件访问」权限",
                "在 LSPosed / Vector 中启用本模块",
                "作用域勾选「相机」(com.android.camera)",
                "修改配置后重启相机进程",
                "首次使用需启动一次相机以生成配置文件",
        };
        LinearLayout stepBox = Ui.card(a);
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) Ui.divider(a, stepBox);
            stepBox.addView(stepRow(i + 1, steps[i]));
        }
        c.addView(stepBox);

        // ===== 配置文件 =====
        c.addView(Ui.sectionLabel(a, "配置文件"));
        LinearLayout card4 = Ui.card(a);
        Ui.clickRow(a, card4, "编辑配置", MainActivity.CONF_PATH, new Runnable() {
            public void run() { editConfig(); }
        });
        Ui.divider(a, card4);
        Ui.clickRow(a, card4, "打开所在位置", "在文件管理器中定位", new Runnable() {
            public void run() { openLocation(); }
        });
        Ui.divider(a, card4);
        Ui.clickRow(a, card4, "复制配置内容", "把 config.conf 复制到剪贴板", new Runnable() {
            public void run() { copyConfig(); }
        });
        Ui.divider(a, card4);
        Ui.clickRow(a, card4, "备份当前配置", "保存为 config.conf.bak.<时间戳>", new Runnable() {
            public void run() { backupConfig(); }
        });
        c.addView(card4);

        // ===== 操作 =====
        c.addView(Ui.sectionLabel(a, "操作"));
        LinearLayout cardBtn = Ui.card(a);
        Ui.clickRow(a, cardBtn, "重新创建配置文件", "备份后删除，请立即强制停止相机再打开", new Runnable() {
            public void run() { recreateConfig(); }
        });
        c.addView(cardBtn);

        // ===== 调试 =====
        c.addView(Ui.sectionLabel(a, "调试"));
        LinearLayout card5 = Ui.card(a);
        Ui.clickRow(a, card5, "模块日志", "查看最近的 CameraTuner 日志", new Runnable() {
            public void run() { showLog(); }
        });
        Ui.divider(a, card5);
        Ui.clickRow(a, card5, "日志过滤", "只输出选中的日志标签，留空则全部输出", new Runnable() {
            public void run() { showLogFilterDialog(); }
        });
        c.addView(card5);

        // ===== 版权 =====
        TextView copyright = new TextView(a);
        copyright.setText("© 2026 小茶 · 保留所有权利");
        copyright.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        copyright.setTextColor(MainActivity.C_TEXT_SUB);
        copyright.setGravity(Gravity.CENTER);
        copyright.setPadding(0, a.dp(30), 0, a.dp(10));
        c.addView(copyright);

        sv.addView(c);
        return sv;
    }

    /** 带序号圆圈的单步行 */
    private LinearLayout stepRow(int num, String text) {
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(a.dp(20), a.dp(15), a.dp(20), a.dp(15));

        // 序号圆圈
        TextView badge = new TextView(a);
        badge.setText(String.valueOf(num));
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        badge.setTextColor(0xFFFFFFFF);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(MainActivity.C_ACCENT);
        badge.setBackground(circle);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(a.dp(22), a.dp(22));
        bp.rightMargin = a.dp(14);
        badge.setLayoutParams(bp);
        row.addView(badge);

        TextView tv = new TextView(a);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(MainActivity.C_TEXT_MAIN);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        return row;
    }

    private void openCoolApk() {
        try {
            a.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MainActivity.AUTHOR_KA_URL)));
        } catch (Throwable t) {
            Toast.makeText(a, "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyConfig() {
        StringBuilder sb = new StringBuilder();
        java.io.File f = new java.io.File(MainActivity.CONF_PATH);
        if (f.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            } catch (Throwable t) {
                Toast.makeText(a, "读取失败：" + t, Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            sb.append("# 配置文件尚未生成");
        }
        try {
            ClipboardManager cm = (ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("config.conf", sb.toString()));
            Toast.makeText(a, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(a, "复制失败：" + t, Toast.LENGTH_SHORT).show();
        }
    }

    /** 读取配置文件全文 */
    private String readConf() {
        StringBuilder sb = new StringBuilder();
        java.io.File f = new java.io.File(MainActivity.CONF_PATH);
        if (!f.exists()) return null;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        } catch (Throwable t) {
            return null;
        }
        return sb.toString();
    }

    /** 编辑配置：弹窗内可直接修改并保存 */
    private void editConfig() {
        final String content = readConf();
        if (content == null) {
            TunerDialog.create(a)
                    .title("配置文件不存在")
                    .message("配置文件尚未生成。\n启动一次相机后会自动创建。")
                    .positive("知道了", null)
                    .show();
            return;
        }

        final android.widget.EditText et = new android.widget.EditText(a);
        et.setText(content);
        et.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
        et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        et.setHorizontalScrollBarEnabled(true);
        et.setVerticalScrollBarEnabled(true);
        et.setSingleLine(false);
        et.setMinLines(12);

        // 限制最大高度（屏高 55%），超出在框内滚动，避免把按钮顶出屏幕
        int maxH = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f);
        et.setMaxHeight(maxH);
        et.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        android.widget.ScrollView sv = new android.widget.ScrollView(a);
        sv.setPadding(a.dp(16), a.dp(8), a.dp(16), 0);
        sv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        sv.addView(et,
                new android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        TunerDialog td = TunerDialog.create(a)
                .title("编辑 config.conf")
                .editable(content)
                .negative("取消", null)
                .neutral("复制", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        try {
                            android.content.ClipboardManager cm =
                                    (android.content.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText("config.conf", content));
                            Toast.makeText(a, "已复制", Toast.LENGTH_SHORT).show();
                        } catch (Throwable ignored) {}
                    }
                })
                .positive("保存", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        try {
                            android.widget.EditText et = d.getEditText();
                            java.io.FileWriter fw = new java.io.FileWriter(MainActivity.CONF_PATH);
                            fw.write(et == null ? "" : et.getText().toString());
                            fw.close();
                            Toast.makeText(a, "已保存，重启相机后生效", Toast.LENGTH_LONG).show();
                        } catch (Throwable t) {
                            Toast.makeText(a, "保存失败：" + t, Toast.LENGTH_LONG).show();
                        }
                    }
                });
        td.show();
    }

    /** 打开配置文件所在目录 */
    private void openLocation() {
        java.io.File dir = new java.io.File(MainActivity.CONF_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            Toast.makeText(a, "目录不存在且创建失败", Toast.LENGTH_SHORT).show();
            return;
        }
        // 尝试用系统文件管理器直接打开 CameraTuner 目录
        // 用 EXTERNAL_STORAGE_PROVIDER 的 tree/document URI，落到 primary:CameraTuner
        // 而不是 primary 根目录，避免用户还要自己一层层点进去
        boolean opened = false;
        String[][] candidates = new String[][] {
                {"content://com.android.externalstorage.documents/document/primary%3ACameraTuner",
                        "vnd.android.document/directory"},
                {"content://com.android.externalstorage.documents/tree/primary%3ACameraTuner",
                        "vnd.android.document/directory"},
                {"content://com.android.externalstorage.documents/root/primary",
                        "vnd.android.document/root"}
        };
        for (String[] c : candidates) {
            if (opened) break;
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse(c[0]), c[1]);
                a.startActivity(i);
                opened = true;
            } catch (Throwable ignored) {}
        }
        if (!opened) {
            Toast.makeText(a, "路径：" + MainActivity.CONF_DIR, Toast.LENGTH_LONG).show();
        }
    }

    /** 备份配置到带时间戳的文件 */
    private void backupConfig() {
        String content = readConf();
        if (content == null) {
            Toast.makeText(a, "配置文件不存在，无法备份", Toast.LENGTH_SHORT).show();
            return;
        }
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                java.util.Locale.getDefault()).format(new java.util.Date());
        String path = MainActivity.CONF_PATH + ".bak." + ts;
        try {
            java.io.FileWriter fw = new java.io.FileWriter(path);
            fw.write(content);
            fw.close();
            Toast.makeText(a, "已备份到：\n" + path, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Toast.makeText(a, "备份失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    /** 重新创建配置：备份 -> 删除 -> 重启相机后自动生成 */
    private void recreateConfig() {
        TunerDialog.create(a)
                .title("重新创建配置文件")
                .message("将备份当前配置并删除原文件。\n\n删除后请【立即强制停止相机】，\n再重新打开相机，才会生成完整的默认配置。\n\n若相机未被停止就做其他操作，可能只生成残缺配置。\n\n确定继续？")
                .negative("取消", null)
                .positive("确定", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        String content = readConf();
                        java.io.File f = new java.io.File(MainActivity.CONF_PATH);
                        String backupMsg = "";
                        if (content != null) {
                            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss",
                                    java.util.Locale.getDefault()).format(new java.util.Date());
                            String path = MainActivity.CONF_PATH + ".bak." + ts;
                            try {
                                java.io.FileWriter fw = new java.io.FileWriter(path);
                                fw.write(content);
                                fw.close();
                                backupMsg = "\n\n已备份到：\n" + path;
                            } catch (Throwable ignored) {}
                        }
                        boolean deleted = f.exists() && f.delete();
                        TunerDialog.create(a)
                                .title(deleted ? "已删除" : "文件不存在")
                                .message((deleted
                                        ? "配置文件已删除。\n请立即强制停止相机，再重新打开。"
                                        : "配置文件本来就不存在。")
                                        + backupMsg)
                                .positive("知道了", null)
                                .show();
                    }
                })
                .show();
    }

    // ==================== 版本与更新日志 ====================

    /** 读取真实版本名 */
    private String getVersionName() {
        try {
            return a.getPackageManager()
                    .getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    /** 读取真实 versionCode */
    private int getVersionCode() {
        try {
            android.content.pm.PackageInfo pi = a.getPackageManager()
                    .getPackageInfo(a.getPackageName(), 0);
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                return (int) pi.getLongVersionCode();
            }
            return pi.versionCode;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 本地兜底更新日志 */
    private static final String LOCAL_CHANGELOG =
            "v1.9\n"
          + "1. 新增 更多机型适配（目前 11 款）\n"
          + "2. 优化 滚动流畅度，修复部分场景卡顿\n"
          + "3. 优化 页面、导航栏、弹窗全部重构\n"
          + "4. 优化 适配全面屏界面\n"
          + "5. 修复 调色盘原图成片无法生效\n"
          + "6. 修复 手动滑动页面时导航栏胶囊不跟随\n"
          + "7. 修复 修改配置保存后页面跳回顶部\n"
          + "8. 修复 从后台回到应用时跳回状态页\n"
          + "9. 修复 竖向滚动列表时误触发翻页\n"
          + "10. 修复 部分提示弹窗多出一个「取消」按钮\n"
          + "\n"
          + "本次更新改动较大，如不生效建议重建配置文件\n"
          + "\n"
          + "v1.8\n"
          + "1. 新增移轴开关，相机左上角悬浮控制（圆形/平行）\n"
          + "2. 新增超级长焦和实况可以同时开启\n"
          + "3. 新增模块内日志过滤，按标签筛选输出\n"
          + "4. 新增模块内功能开关（移轴 / AI长焦实况）\n"
          + "5. 新增模块配置文件自动补齐（带分组与说明）\n"
          + "6. 新增模块配置路径统一\n"
          + "7. 修复相机轮盘内快捷焦段异常\n"
          + "8. 修复录像模式快捷焦段异常\n"
          + "9. 修复模块内编辑配置文件窗口异常\n"
          + "10. 修复模块打开配置文件路径异常\n"
          + "11. 修复模块配置文件分割线异常\n"
          + "\n"
          + "新增功能默认关闭，可在「核心功能」里按需开启\n"
          + "\n"
          + "v1.7\n"
          + "1. 新增智能构图修复，AI帮拍里的智能构图按钮可以用了\n"
          + "2. 新增传奇一瞬色彩模拟，让传奇一瞬正常出片\n"
          + "3. 新增 42 个滤镜可选，带渐变色条与效果说明\n"
          + "4. 新增原色风格开关，顶栏风格从 2 档变 3 档\n"
          + "5. 传奇色彩独立成区块，界面更清爽\n"
          + "6. 旧版本升级自动补齐缺失的配置项\n"
          + "7. 状态页运行状态判断改为按 hook 成功数\n"
          + "\n"
          + "原色风格默认关闭，可在「核心功能」里开启\n"
          + "\n"
          + "v1.6\n"
          + "1. 新增自定义水印机型\n"
          + "2. 水印设置独立分类\n"
          + "3. 修复编辑框过高导致按钮被遮挡\n"
          + "4. 默认剔除 4.3 倍焦段档位（修复人像模式焦段）\n"
          + "5. 修复配置文件横线排版\n"
          + "\n"
          + "v1.5\n"
          + "1. 新增 AI 帮拍模式解锁\n"
          + "2. 修复进入 AI 帮拍提示「机型不匹配」并自动退出的问题\n"
          + "3. 修复功能页图标越界导致的闪退\n"
          + "\n"
          + "v1.4\n"
          + "1. 修复专业模式长焦无法调用\n"
          + "2. 优化界面配色与布局\n"
          + "3. 新增配置编辑 / 备份 / 重建\n"
          + "4. 新增检查更新\n"
          + "\n"
          + "v1.3\n"
          + "1. 重构界面，白色主题 + 底部导航\n"
          + "2. 状态页显示设备信息\n"
          + "\n"
          + "v1.2\n"
          + "1. 新增片门全开\n"
          + "2. 新增模式可见性\n"
          + "\n"
          + "v1.1\n"
          + "1. 新增 AI 水印\n"
          + "2. 新增焦段重映射";

    /** 显示更新日志：云端优先，失败用本地 */
    private void showChangelog() {
        Toast.makeText(a, "正在获取更新日志…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final String cloud = fetchChangelog();
                a.runOnUiThread(new Runnable() {
                    public void run() {
                        String text = (cloud != null && !cloud.isEmpty())
                                ? cloud : LOCAL_CHANGELOG;
                        TunerDialog.create(a).title("更新日志")
                                .message(text)
                                .positive("关闭", null).show();
                    }
                });
            }
        }).start();
    }

    /** 从云端 JSON 提取 changelog 字段（字符串数组） */
    private String fetchChangelog() {
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(UPDATE_URL);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setUseCaches(false);
            conn.setRequestProperty("Cache-Control", "no-cache");
            if (conn.getResponseCode() != 200) return null;

            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            String body = sb.toString();

            // 找 "changelog" 数组，取出每个字符串元素
            int i = body.indexOf("\"changelog\"");
            if (i < 0) return null;
            int arrStart = body.indexOf('[', i);
            if (arrStart < 0) return null;
            int arrEnd = body.indexOf(']', arrStart);
            if (arrEnd < 0) return null;

            String arr = body.substring(arrStart + 1, arrEnd);
            StringBuilder out = new StringBuilder();
            boolean inStr = false;
            StringBuilder cur = new StringBuilder();
            for (int p = 0; p < arr.length(); p++) {
                char c = arr.charAt(p);
                if (!inStr) {
                    if (c == '"') { inStr = true; cur.setLength(0); }
                } else {
                    if (c == '\\' && p + 1 < arr.length()) {
                        char n = arr.charAt(p + 1);
                        if (n == 'n') { cur.append('\n'); p++; continue; }
                        if (n == 't') { cur.append('\t'); p++; continue; }
                        if (n == '"') { cur.append('"'); p++; continue; }
                        if (n == '\\') { cur.append('\\'); p++; continue; }
                        cur.append(c);
                    } else if (c == '"') {
                        inStr = false;
                        out.append(cur).append("\n");
                    } else {
                        cur.append(c);
                    }
                }
            }
            String result = out.toString().trim();
            return result.isEmpty() ? null : result;
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    // ==================== 检查更新 ====================

    private static final String UPDATE_URL = "https://update.xn--yet042g.cn/update.json";
    

    private void checkUpdate() {
        Toast.makeText(a, "正在检查更新…", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final String result = fetchUpdateJson();
                a.runOnUiThread(new Runnable() {
                    public void run() { handleUpdateResult(result); }
                });
            }
        }).start();
    }

    /** 拉 JSON，返回值：null=失败，"LATEST"=已最新，其他=JSON 原文 */
    private String fetchUpdateJson() {
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(UPDATE_URL);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setUseCaches(false);
            conn.setRequestProperty("Cache-Control", "no-cache");

            int code = conn.getResponseCode();
            if (code != 200) return null;

            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            String body = sb.toString().trim();
            if (body.isEmpty()) return null;

            int remoteCode = extractInt(body, "versionCode");
            if (remoteCode < 0) return null;
            if (remoteCode <= getVersionCode()) return "LATEST";
            return body;
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private void handleUpdateResult(String result) {
        if (result == null) {
            TunerDialog.create(a).title("检查更新")
                    .message("检查失败，请检查网络后重试。")
                    .positive("知道了", null).show();
            return;
        }
        if ("LATEST".equals(result)) {
            TunerDialog.create(a).title("检查更新")
                    .message("当前已是最新版本（v" + getVersionName() + "）")
                    .positive("知道了", null).show();
            return;
        }
        // 有新版本
        String ver = extractStr(result, "version");
        String notes = extractStr(result, "notes");
        final String url = extractStr(result, "url");

        String msg = "发现新版本 v" + (ver == null ? "?" : ver)
                + "\n\n" + (notes == null ? "" : notes.replace("\\n", "\n"));
        TunerDialog.create(a).title("发现新版本")
                .message(msg.trim())
                .negative("稍后", null)
                .positive("立即更新", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        if (url != null && !url.isEmpty()) {
                            try {
                                a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                            } catch (Throwable t) {
                                Toast.makeText(a, "无法打开链接", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .show();
    }

    /** 从 JSON 里提取 int 字段（简易解析） */
    private int extractInt(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\"");
            if (i < 0) return -1;
            int colon = json.indexOf(':', i);
            if (colon < 0) return -1;
            int end = colon + 1;
            while (end < json.length() && !Character.isDigit(json.charAt(end)) && json.charAt(end) != '-') end++;
            int start = end;
            while (end < json.length()
                    && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            return Integer.parseInt(json.substring(start, end));
        } catch (Throwable t) {
            return -1;
        }
    }

    /** 从 JSON 里提取字符串字段（简易解析） */
    private String extractStr(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\"");
            if (i < 0) return null;
            int colon = json.indexOf(':', i);
            if (colon < 0) return null;
            int q1 = json.indexOf('"', colon);
            if (q1 < 0) return null;
            StringBuilder sb = new StringBuilder();
            for (int p = q1 + 1; p < json.length(); p++) {
                char c = json.charAt(p);
                if (c == '\\' && p + 1 < json.length()) {
                    char n = json.charAt(p + 1);
                    if (n == 'n') { sb.append('\n'); p++; continue; }
                    if (n == 't') { sb.append('\t'); p++; continue; }
                    if (n == '"') { sb.append('"'); p++; continue; }
                    if (n == '\\') { sb.append('\\'); p++; continue; }
                    sb.append(c);
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            return null;
        }
    }

    /** 日志标签多选对话框 */
    /** 复选框样式：选中玫红填充 + 白勾，未选中描边空框 */
    private void applyBoxStyle(MainActivity a, TextView box, boolean on) {
        android.graphics.drawable.GradientDrawable bg =
                new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(a.dp(7));
        if (on) {
            bg.setColor(MainActivity.C_ACCENT);
            box.setText("✓");
        } else {
            bg.setColor(0x00000000);
            bg.setStroke((int) (a.dp(1) * 1.5f), 0x33000000);
            box.setText("");
        }
        box.setBackground(bg);
    }

    private void showLogFilterDialog() {
        final String[] TAGS = {
                "tilt", "zoom", "cycle", "telefix", "caps", "cvtype",
                "aiwm", "og", "raw", "rawfield", "rawsize", "opmode",
                "mode", "blockmsg", "wm", "level"
        };
        final String[] LABELS = {
                "tilt —— 移轴开关",
                "zoom —— 焦段重映射",
                "cycle —— 档位循环修正",
                "telefix —— 专业模式长焦修复（刷屏严重）",
                "caps —— 传奇一瞬能力门",
                "cvtype —— 原色风格",
                "aiwm —— AI水印",
                "og —— 片门全开",
                "raw —— RAW 回调类型",
                "rawfield —— RAW 字段强制",
                "rawsize —— RAW 尺寸补齐",
                "opmode —— 强制 operation mode",
                "mode —— 模式可见性",
                "blockmsg —— 拦截退出消息",
                "wm —— 自定义水印机型",
                "level —— 传奇设备等级"
        };

        java.util.Map<String, String> conf = a.readConfig();
        String cur = conf.get("log.filter");
        final boolean[] checked = new boolean[TAGS.length];
        if (cur != null && !cur.trim().isEmpty()) {
            for (String t : cur.split(",")) {
                String tt = t.trim();
                for (int i = 0; i < TAGS.length; i++) {
                    if (TAGS[i].equals(tt)) checked[i] = true;
                }
            }
        }

        // ---------- 自绘多选列表 ----------
        final TextView[] boxes = new TextView[TAGS.length];
        LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < TAGS.length; i++) {
            final int idx = i;

            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(a.dp(4), a.dp(9), a.dp(4), a.dp(9));

            TextView label = new TextView(a);
            label.setText(LABELS[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
            label.setTextColor(MainActivity.C_TEXT_MAIN);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(label);

            final TextView box = new TextView(a);
            box.setGravity(Gravity.CENTER);
            box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            box.setTextColor(0xFFFFFFFF);
            box.setTypeface(Typeface.DEFAULT_BOLD);
            box.setIncludeFontPadding(false);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(a.dp(22), a.dp(22));
            bp.leftMargin = a.dp(10);
            box.setLayoutParams(bp);
            applyBoxStyle(a, box, checked[i]);
            boxes[i] = box;
            row.addView(box);

            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    checked[idx] = !checked[idx];
                    applyBoxStyle(a, boxes[idx], checked[idx]);
                }
            });
            list.addView(row);

            if (i < TAGS.length - 1) {
                View line = new View(a);
                line.setBackgroundColor(0x0C000000);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, a.dp(1));
                lp.leftMargin = a.dp(4);
                lp.rightMargin = a.dp(4);
                list.addView(line, lp);
            }
        }

        ScrollView lsv = new ScrollView(a);
        Ui.customScrollbar(a, lsv);
        lsv.setClipToPadding(false);
        lsv.addView(list);
        int lh = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.5f);
        lsv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, lh));

        TunerDialog.create(a)
                .title("日志过滤")
                .customView(lsv)
                .neutral("全选", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        StringBuilder sb = new StringBuilder();
                        for (String t : TAGS) {
                            if (sb.length() > 0) sb.append(",");
                            sb.append(t);
                        }
                        a.writeConfig("log.filter", sb.toString());
                        Toast.makeText(a, "已全选，重启相机后生效", Toast.LENGTH_LONG).show();
                    }
                })
                .negative("清空", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        a.writeConfig("log.filter", "");
                        Toast.makeText(a, "已清空（全部输出）", Toast.LENGTH_LONG).show();
                    }
                })
                .positive("保存", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < TAGS.length; i++) {
                            if (checked[i]) {
                                if (sb.length() > 0) sb.append(",");
                                sb.append(TAGS[i]);
                            }
                        }
                        a.writeConfig("log.filter", sb.toString());
                        Toast.makeText(a,
                                sb.length() == 0 ? "已清空（全部输出）" : "已保存，重启相机后生效",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .show();
    }

    private void showLog() {
        final StringBuilder sb = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c",
                    "logcat -d -s CameraTuner:V | tail -80"});
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
        } catch (Throwable t) {
            sb.append("读取失败：").append(t);
        }
        final String logText = sb.length() == 0 ? "暂无日志" : sb.toString();

        // 只读日志框，限高 55% 屏高，框内滚动
        TextView tv = new TextView(a);
        tv.setText(logText);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        tv.setTextColor(MainActivity.C_TEXT_MAIN);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(a.dp(2), 1f);

        int maxH = (int) (a.getResources().getDisplayMetrics().heightPixels * 0.55f);

        ScrollView sv = new ScrollView(a);
        Ui.customScrollbar(a, sv);
        sv.setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(4));
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, maxH));
        sv.addView(tv, new android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TunerDialog.create(a)
                .title("模块日志")
                .customView(sv)
                .neutral("复制", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        try {
                            android.content.ClipboardManager cm =
                                    (android.content.ClipboardManager) a.getSystemService(Context.CLIPBOARD_SERVICE);
                            cm.setPrimaryClip(ClipData.newPlainText("CameraTuner log", logText));
                            Toast.makeText(a, "已复制", Toast.LENGTH_SHORT).show();
                        } catch (Throwable t) {
                            Toast.makeText(a, "复制失败：" + t, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .positive("关闭", null)
                .show();
    }
}
