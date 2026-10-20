package com.starlight.cameratuner;

import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

/** 功能页：功能开关 / 模式开关 / 高级 / 可见性编辑 */
public class FunctionPage {

    private final MainActivity a;
    final Map<String, CapsuleSwitch> switches = new LinkedHashMap<>();
    final Map<String, CapsuleSwitch> modeSwitches = new LinkedHashMap<>();
    final Map<String, android.widget.EditText> edits = new LinkedHashMap<>();

    FunctionPage(MainActivity a) { this.a = a; }

    ScrollView build() {
        ScrollView sv = new ScrollView(a);
        sv.setBackgroundColor(MainActivity.C_BG);
        sv.setClipToPadding(false);

        LinearLayout c = new LinearLayout(a);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(a.dp(16), a.dp(8), a.dp(16), a.dp(140));

        c.addView(Ui.pageTitle(a, "功能"));

        // 核心功能
        c.addView(Ui.sectionLabel(a, "核心功能"));
        LinearLayout card1 = Ui.card(a);
        Ui.switchRow(a, card1, "fake.enable", "机型伪装",
                "让相机认为自己是 18 Pro Max (madrid)", R.drawable.ic_phone, switches);
        Ui.divider(a, card1);
        Ui.switchRow(a, card1, "caps.force", "传奇一瞬能力门",
                "如果你不知道这是什么，请勿打开", R.drawable.ic_star, switches);
        Ui.divider(a, card1);
        Ui.switchRow(a, card1, "cvtype.natural", "原色风格",
                "顶栏风格多一档「原色」，共三档", R.drawable.ic_camera, switches);
        Ui.divider(a, card1);
        Ui.switchRow(a, card1, "opengate.enable", "片门全开",
                "专业录像可输出 4096×3072 全画幅", R.drawable.ic_film, switches);
        c.addView(card1);

        // 传奇色彩
        c.addView(Ui.sectionLabel(a, "传奇色彩"));
        LinearLayout cardColor = Ui.card(a);
        Ui.switchRow(a, cardColor, "legend.color", "色彩模拟",
                "让传奇一瞬正常出片", R.drawable.ic_film, switches);
        Ui.divider(a, cardColor);
        addLegendFilterRows(a, cardColor);
        c.addView(cardColor);

        // 水印
        c.addView(Ui.sectionLabel(a, "水印"));
        LinearLayout cardWm = Ui.card(a);
        Ui.switchRow(a, cardWm, "watermark.enable", "自定义水印机型",
                "修改照片水印显示的机型名称", R.drawable.ic_watermark, switches);
        Ui.divider(a, cardWm);
        Ui.editRow(a, cardWm, "watermark.name", "水印机型名称",
                "留空则显示 XIAOMI 18 Pro Max；填写时无需加 Xiaomi 前缀", edits);
        c.addView(cardWm);

        // 模式开关
        c.addView(Ui.sectionLabel(a, "模式"));
        LinearLayout card2 = Ui.card(a);
        int[] icons = {R.drawable.ic_ai, R.drawable.ic_water, R.drawable.ic_clapper,
                R.drawable.ic_moon, R.drawable.ic_night};
        for (int i = 0; i < MainActivity.MODE_PRESETS.length; i++) {
            String[] p = MainActivity.MODE_PRESETS[i];
            if (i > 0) Ui.divider(a, card2);
            int iconRes = (i < icons.length) ? icons[i] : R.drawable.ic_star;
            Ui.switchRow(a, card2, "mode:" + p[1], p[0], p[2],
                    iconRes, modeSwitches);
        }
        c.addView(card2);

        // 焦段
        c.addView(Ui.sectionLabel(a, "焦段"));
        LinearLayout cardZ = Ui.card(a);
        // ── 重映射组 ──
        Ui.switchRow(a, cardZ, "zoom.enable", "焦段重映射",
                "若已安装 XiaomiCamPort，请关闭此项以免冲突", R.drawable.ic_film, switches);
        Ui.divider(a, cardZ);
        Ui.editRow(a, cardZ, "zoom_remap", "档位重映射",
                "格式：旧:新，逗号分隔。例：0.7:0.6,3.2:5,6.4:10", edits);
        Ui.divider(a, cardZ);
        Ui.editRow(a, cardZ, "zoom_drop", "档位剔除",
                "需开启焦段重映射。格式：逗号分隔。例：4.3", edits);
        // ── 循环组 ──
        Ui.divider(a, cardZ);
        Ui.switchRow(a, cardZ, "legend.zoomcycle", "档位循环修正",
                "修复到最大档位后继续点击会乱跳的问题", R.drawable.ic_film, switches);
        Ui.divider(a, cardZ);
        Ui.editRow(a, cardZ, "legend.zoomcycle_stops", "循环档位表",
                "格式：逗号分隔。例：5,10,20", edits);
        Ui.divider(a, cardZ);
        Ui.editRow(a, cardZ, "legend.zoomcycle_min", "最低干预倍率",
                "低于此倍率不做循环修正。例：1.5", edits);

        // ── 长焦组 ──
        Ui.divider(a, cardZ);
        Ui.switchRow(a, cardZ, "legend.prolens", "专业模式长焦修复",
                "修复专业模式点 5x 无法调用长焦的问题", R.drawable.ic_film, switches);
        Ui.divider(a, cardZ);
        Ui.editRow(a, cardZ, "legend.prolens_modes", "生效模式",
                "默认 167（专业模式），一般无需修改", edits);
        c.addView(cardZ);

        // 可见性编辑
        c.addView(Ui.sectionLabel(a, "模式可见性编辑"));
        LinearLayout card4 = Ui.card(a);
        Ui.clickRow(a, card4, "自定义显示模式", "手动填写 enable_mode 列表", new Runnable() {
            public void run() { editList("enable_mode"); }
        });
        Ui.divider(a, card4);
        Ui.clickRow(a, card4, "自定义隐藏模式", "手动填写 disable_mode 列表", new Runnable() {
            public void run() { editList("disable_mode"); }
        });
        c.addView(card4);

        // ===== 高级参数（置底）=====
        // 高级参数（置底）
        c.addView(Ui.sectionLabel(a, "高级参数"));
        LinearLayout cardAdv = Ui.card(a);
        Ui.editRow(a, cardAdv, "level", "传奇一瞬设备等级",
                "模拟的设备等级，2 为最高。影响传奇一瞬模式可用性，一般无需修改。", edits);
        Ui.divider(a, cardAdv);
        Ui.editRow(a, cardAdv, "raw.type", "RAW 回调类型",
                "0x10 为默认输出，0x11 为 MultiRawHdr。改错可能导致传奇一瞬无法拍照。", edits);
        Ui.divider(a, cardAdv);
        Ui.editRow(a, cardAdv, "prolens.idx_tele", "长焦镜头索引",
                "专业模式长焦的 roleId。4 = ultraTele（5x 潜望），一般无需修改。", edits);
        Ui.divider(a, cardAdv);
        Ui.editRow(a, cardAdv, "prolens.idx_wide", "主摄镜头索引",
                "主摄默认 1。一般无需修改。", edits);
        Ui.divider(a, cardAdv);
        Ui.editRow(a, cardAdv, "prolens.idx_ultra", "超广角镜头索引",
                "超广角默认 0。一般无需修改。", edits);
        c.addView(cardAdv);


        sv.addView(c);
        applyValues();
        return sv;
    }

    void applyValues() {
        Map<String, String> conf = a.readConfig();
        for (Map.Entry<String, CapsuleSwitch> e : switches.entrySet()) {
            final String key = e.getKey();
            final CapsuleSwitch sw = e.getValue();
            String v = conf.get(key);
            boolean on = v == null || "1".equals(v) || "true".equalsIgnoreCase(v);
            sw.setCheckedImmediate(on);
            sw.setOnCheckedChangeListener(new CapsuleSwitch.OnCheckedChangeListener() {
                public void onCheckedChanged(CapsuleSwitch s, boolean checked) {
                    a.writeConfig(key, checked ? "1" : "0");
                }
            });
        }

        String enableRaw = a.readRawConfig("enable_mode");
        for (Map.Entry<String, CapsuleSwitch> e : modeSwitches.entrySet()) {
            final String key = e.getKey();
            final String cls = key.startsWith("mode:") ? key.substring(5) : key;
            final CapsuleSwitch sw = e.getValue();
            sw.setCheckedImmediate(enableRaw.contains(cls));
            sw.setOnCheckedChangeListener(new CapsuleSwitch.OnCheckedChangeListener() {
                public void onCheckedChanged(CapsuleSwitch s, boolean checked) {
                    a.setEnableMode(cls, checked);
                }
            });
        }

        // 刷新编辑行的"当前值"显示
        refreshValueLabels(conf);
    }

    /** 遍历视图树，刷新 tag=value:xxx 的 TextView */
    void refreshValueLabels(final Map<String, String> conf) {
        // 通过 Activity 的 contentArea 遍历
        android.view.ViewGroup content = a.getContentRoot();
        if (content == null) return;
        java.util.List<android.view.View> all = new java.util.ArrayList<>();
        collectViews(content, all);
        for (android.view.View v : all) {
            Object tag = v.getTag();
            if (tag instanceof String && ((String) tag).startsWith("value:")) {
                String key = ((String) tag).substring(6);
                String val = conf.get(key);
                ((android.widget.TextView) v).setText(val == null ? "未设置" : val);
            }
        }
    }

    private void collectViews(android.view.View v, java.util.List<android.view.View> out) {
        out.add(v);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) collectViews(g.getChildAt(i), out);
        }
    }

    void editList(final String key) {
        final android.widget.EditText et = new android.widget.EditText(a);
        et.setHint("每行一个类全名");
        et.setMinLines(6);
        et.setGravity(android.view.Gravity.TOP);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        et.setText(a.readRawConfig(key));

        final String key2 = key;
        TunerDialog td = TunerDialog.create(a)
                .title(key2)
                .editable(a.readRawConfig(key2))
                .positive("保存", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        android.widget.EditText e = d.getEditText();
                        saveList(key2, (e == null ? "" : e.getText().toString()).split("\n"));
                    }
                });
        td.show();
    }

    void saveList(String key, String[] values) {
        java.io.File dir = new java.io.File(MainActivity.CONF_DIR);
        if (!dir.exists()) dir.mkdirs();
        java.io.File f = new java.io.File(MainActivity.CONF_PATH);
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (f.exists()) {
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String t = line.trim();
                    if (!t.startsWith(key + "=")) lines.add(line);
                }
            } catch (Throwable ignored) {}
        }
        for (String v : values) {
            v = v.trim();
            if (!v.isEmpty()) lines.add(key + "=" + v);
        }
        try (java.io.FileWriter fw = new java.io.FileWriter(f)) {
            for (String l : lines) { fw.write(l); fw.write("\n"); }
            android.widget.Toast.makeText(a, "已保存", android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            android.widget.Toast.makeText(a, "保存失败：" + t, android.widget.Toast.LENGTH_LONG).show();
        }
    }




    /** 传奇一瞬三种风格对应的滤镜选择行 */
    private void addLegendFilterRows(final MainActivity a, LinearLayout card) {
        final java.util.Map<String, String> conf = a.readConfig();

        // 注意：相机里 UI 第 2 档绑定的是 m10r、第 3 档绑定的是 m9，
        // 这里按相机实际顺序排列，避免改一档影响到另一档。
        String[] keys = {"legend.filter.m3", "legend.filter.m10r", "legend.filter.m9"};
        String[] titles = {"黑白胶片", "当代经典", "传奇 CCD"};
        int[] defaults = {86, 73, 84};

        for (int i = 0; i < keys.length; i++) {
            final String key = keys[i];
            final int def = defaults[i];
            String raw = conf.get(key);
            int id = def;
            if (raw != null && !raw.isEmpty()) {
                try {
                    id = Integer.parseInt(raw.trim());
                } catch (Throwable ignored) {
                }
            }
            final int cur = id;
            Ui.clickRow(a, card, titles[i],
                    "当前：" + FilterPicker.nameOf(cur) + "（点击更换）",
                    new Runnable() {
                        public void run() {
                            FilterPicker.show(a, key, cur);
                        }
                    });
            if (i < keys.length - 1) {
                Ui.divider(a, card);
            }
        }
        Ui.divider(a, card);
        Ui.clickRow(a, card, "恢复默认滤镜",
                "三个风格恢复成 黑白400 / 自然 / 柯达64",
                new Runnable() {
                    public void run() {
                        a.writeConfig(FilterPicker.KEY_M3, String.valueOf(FilterPicker.DEF_M3));
                        a.writeConfig(FilterPicker.KEY_M9, String.valueOf(FilterPicker.DEF_M9));
                        a.writeConfig(FilterPicker.KEY_M10R, String.valueOf(FilterPicker.DEF_M10R));
                        a.refreshCurrentPage();
                        android.widget.Toast.makeText(a, "已恢复默认滤镜",
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
