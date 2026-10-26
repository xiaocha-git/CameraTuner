package com.starlight.cameratuner;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    static final String CONF_DIR = "/sdcard/CameraTuner";
    static final String CONF_PATH = CONF_DIR + "/config.conf";
    static final String CAMERA_PKG = "com.android.camera";
    static final String EXPECTED_VERSION = "6.8.002090.2";
    static final String AUTHOR_KA_URL = "https://www.coolapk.com/u/16215423";

    static final int C_BG        = 0xFFF2F2F7;
    static final int C_CARD      = 0xFFFFFFFF;
    static final int C_TEXT_MAIN = 0xFF1C1C1E;
    static final int C_TEXT_SUB  = 0xFF8E8E93;
    static final int C_ACCENT    = 0xFFE8A33D;
    static final int C_GREEN     = 0xFF34C759;
    static final int C_RED       = 0xFFFF3B30;

    static final String[][] MODE_PRESETS = {
            {"AI 帮拍", "com.android.camera.features.mode.ai.AiModuleEntry",
                    "AI 拍摄助手，需联网"},
            {"AI 水印", "com.android.camera.features.mode.aiwatermark.AIWaterModuleEntry",
                    "在功能菜单中显示 AI 水印入口"},
            {"导演模式", "com.android.camera.features.mode.cinemaster.CinemasterModuleEntry",
                    "目前仅可进入录制视频，不可修改参数"},
            {"超级月亮", "com.android.camera.features.mode.moon.MoonModuleEntry",
                    "在菜单中选择月亮模式，自动识别并增强月球细节"},
            {"夜景", "com.android.camera.features.mode.night.photo.NightModuleEntry",
                    "在菜单中选择夜景模式，长曝光合成，适合暗光环境"},
    };

    static final String[] TAB_TITLES = {"状态", "功能", "关于"};
    static final int[] TAB_ICONS = {
            R.drawable.ic_tab_status, R.drawable.ic_tab_func, R.drawable.ic_tab_about
    };

    SwipePager pager;
    LinearLayout[] navItems = new LinearLayout[3];
    View navIndicator;
    ImageView[] navIcons = new ImageView[3];
    TextView[] navLabels = new TextView[3];
    ScrollView[] pages = new ScrollView[3];
    int currentTab = 0;

    final Map<String, CapsuleSwitch> switches = new LinkedHashMap<>();
    final Map<String, CapsuleSwitch> modeSwitches = new LinkedHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(C_BG);
        getWindow().setNavigationBarColor(C_BG);
        buildUi();
        switchTab(0, false);
        requestStoragePermissionIfNeeded();
        AutoUpdateCheck.run(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildPages();
        pager.setCurrentItem(currentTab, false);
    }

    // ==================== 主框架 ====================

    void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(C_BG);

        // 滑动页容器
        pager = new SwipePager(this);
        pager.setPageListener(new SwipePager.OnPageListener() {
            public void onPageSelected(int index) { updateNav(index); }
        });
        root.addView(pager, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 底部悬浮胶囊（FrameLayout 承载：指示器 + 三个 item）
        FrameLayout capsule = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(34));
        bg.setStroke(dp(1), 0xFFE0E0E5);
        capsule.setBackground(bg);
        capsule.setElevation(dp(6));

        // 内层：均分三个 item
        LinearLayout itemRow = new LinearLayout(this);
        itemRow.setOrientation(LinearLayout.HORIZONTAL);
        itemRow.setPadding(dp(6), dp(6), dp(6), dp(6));
        FrameLayout.LayoutParams rowLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        // 滑动指示器（先加，保证在 itemRow 下层）
        navIndicator = new View(this);
        GradientDrawable indBg = new GradientDrawable();
        indBg.setColor(0xFFEDEDF0);
        indBg.setCornerRadius(dp(24));
        navIndicator.setBackground(indBg);
        navIndicator.setAlpha(0f);
        navIndicator.setClickable(false);
        FrameLayout.LayoutParams indLp = new FrameLayout.LayoutParams(dp(60), dp(48));
        indLp.leftMargin = dp(6);
        indLp.topMargin = dp(10);
        capsule.addView(navIndicator, indLp);

        capsule.addView(itemRow, rowLp);

        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { switchTab(idx, true); }
            });

            ImageView icon = new ImageView(this);
            icon.setImageResource(TAB_ICONS[i]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(24), dp(24));
            ip.gravity = Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(ip);
            item.addView(icon);

            TextView label = new TextView(this);
            label.setText(TAB_TITLES[i]);
            label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            label.setIncludeFontPadding(false);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp2.gravity = Gravity.CENTER_HORIZONTAL;
            lp2.topMargin = dp(3);
            label.setLayoutParams(lp2);
            item.addView(label);

            navItems[i] = item;
            navIcons[i] = icon;
            navLabels[i] = label;

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(56), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            itemRow.addView(item, lp);
        }

        int navH = getSystemBarHeight("navigation_bar_height");
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.BOTTOM;
        clp.leftMargin = dp(22);
        clp.rightMargin = dp(22);
        clp.bottomMargin = navH + dp(12);
        root.addView(capsule, clp);

        setContentView(root);
    }

    int getSystemBarHeight(String name) {
        try {
            int resId = getResources().getIdentifier(name, "dimen", "android");
            if (resId > 0) return getResources().getDimensionPixelSize(resId);
        } catch (Throwable ignored) {}
        return dp(24);
    }

    void rebuildPages() {
        pages[0] = buildStatusPage();
        pages[1] = buildFunctionPage();
        pages[2] = buildAboutPage();
        pager.setPages(pages);
    }

    android.view.ViewGroup getContentRoot() { return pager; }

    /** 重新构建当前页（编辑保存后刷新显示值） */
    void refreshCurrentPage() {
        rebuildPages();
        pager.setCurrentItem(currentTab, false);
    }

    void switchTab(int idx, boolean animate) {
        currentTab = idx;
        rebuildPages();
        pager.setCurrentItem(idx, animate);
        updateNav(idx);
    }

    void updateNav(int idx) {
        for (int i = 0; i < 3; i++) {
            boolean sel = (i == idx);
            navIcons[i].setColorFilter(sel ? C_ACCENT : 0xFF1C1C1E);
            navLabels[i].setTextColor(sel ? C_ACCENT : 0xFF1C1C1E);
            navLabels[i].setTypeface(sel ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            navItems[i].setBackground(null);
        }
        moveIndicator(idx);
    }

    /** 让指示器平滑移动到第 idx 个 item */
    void moveIndicator(final int idx) {
        if (navIndicator == null) return;
        navItems[idx].post(new Runnable() {
            public void run() {
                LinearLayout target = navItems[idx];
                int tw = target.getWidth();
                // item.getLeft() 是相对 itemRow 的坐标；
                // 指示器的 leftMargin 已固定为 6dp，而 itemRow 也有 6dp padding，
                // 因此指示器目标 X = item.getLeft()（两者都从 itemRow 的 0 起算）
                int tx = target.getLeft();
                if (tw <= 0) return;

                // 首次显示：直接定位
                if (navIndicator.getAlpha() == 0f) {
                    ViewGroup.LayoutParams lp = navIndicator.getLayoutParams();
                    lp.width = tw;
                    navIndicator.setLayoutParams(lp);
                    navIndicator.setX(tx);
                    navIndicator.setAlpha(1f);
                    return;
                }

                ValueAnimator wa = ValueAnimator.ofInt(navIndicator.getWidth(), tw);
                ValueAnimator xa = ValueAnimator.ofInt(
                        (int) navIndicator.getX(), tx);
                wa.setDuration(240);
                xa.setDuration(240);
                wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator a) {
                        ViewGroup.LayoutParams lp = navIndicator.getLayoutParams();
                        lp.width = (Integer) a.getAnimatedValue();
                        navIndicator.setLayoutParams(lp);
                    }
                });
                xa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    public void onAnimationUpdate(ValueAnimator a) {
                        navIndicator.setX((Integer) a.getAnimatedValue());
                    }
                });
                wa.start();
                xa.start();
            }
        });
    }

    // ==================== 页面 ====================

    ScrollView buildStatusPage() { return new StatusPage(this).build(); }
    ScrollView buildFunctionPage() { return new FunctionPage(this).build(); }
    ScrollView buildAboutPage() { return new AboutPage(this).build(); }

    // ==================== 通用组件 ====================

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    boolean hasRoot() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader br = new BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            return line != null && line.contains("uid=0");
        } catch (Throwable t) { return false; }
    }

    boolean hasStoragePerm() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return true;
    }

    void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            new AlertDialog.Builder(this)
                    .setTitle("需要文件访问权限")
                    .setMessage("为了读写配置文件，需要授予「所有文件访问权限」。")
                    .setPositiveButton("去授权", new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int w) {
                            try {
                                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                i.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(i);
                            } catch (Throwable t) {
                                startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                            }
                        }
                    })
                    .setNegativeButton("稍后", null)
                    .show();
        }
    }

    // ==================== 配置读写 ====================

    Map<String, String> readConfig() {
        Map<String, String> map = new LinkedHashMap<>();
        File f = new File(CONF_PATH);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                map.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
            }
        } catch (Throwable ignored) {}
        return map;
    }

    String readRawConfig(String key) {
        StringBuilder sb = new StringBuilder();
        File f = new File(CONF_PATH);
        if (!f.exists()) return "";
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.startsWith(key + "=")) sb.append(t.substring(key.length() + 1)).append("\n");
            }
        } catch (Throwable ignored) {}
        return sb.toString();
    }

    void writeConfig(String key, String value) {
        File dir = new File(CONF_DIR);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(CONF_PATH);
        List<String> lines = new ArrayList<>();
        boolean found = false;
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.startsWith(key + "=")) { lines.add(key + "=" + value); found = true; }
                    else lines.add(line);
                }
            } catch (Throwable ignored) {}
        }
        if (!found) lines.add(key + "=" + value);
        try (FileWriter fw = new FileWriter(f)) {
            for (String l : lines) { fw.write(l); fw.write("\n"); }
        } catch (Throwable t) {
            Toast.makeText(this, "保存失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    void setEnableMode(String className, boolean enable) {
        File dir = new File(CONF_DIR);
        if (!dir.exists()) dir.mkdirs();
        File f = new File(CONF_PATH);
        List<String> lines = new ArrayList<>();
        boolean found = false;
        if (f.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (t.equals("enable_mode=" + className)) {
                        found = true;
                        if (enable) lines.add(line);
                    } else lines.add(line);
                }
            } catch (Throwable ignored) {}
        }
        if (enable && !found) lines.add("enable_mode=" + className);
        try (FileWriter fw = new FileWriter(f)) {
            for (String l : lines) { fw.write(l); fw.write("\n"); }
            Toast.makeText(this, (enable ? "已显示：" : "已隐藏：") + shortName(className),
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "保存失败：" + t, Toast.LENGTH_LONG).show();
        }
    }

    static String shortName(String cls) {
        int i = cls.lastIndexOf('.');
        return i >= 0 ? cls.substring(i + 1) : cls;
    }

    void execRootAsync(String cmd, final Runnable after) {
        final boolean[] ok = {false};
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            p.waitFor();
            ok[0] = true;
        } catch (Throwable ignored) {}
        runOnUiThread(new Runnable() {
            public void run() { if (after != null) after.run(); }
        });
    }
}
