package com.starlight.cameratuner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 传奇一瞬色彩 —— 滤镜选择弹窗。
 *
 * 42 个 CubeLut 滤镜分四组：胶片复古 / 人像日常 / 基础自然 / 美颜视频。
 * 每行左侧显示三段式渐变条（左→中→右），示意该滤镜的色调倾向。
 * 选择结果写回 config.conf 的 legend.filter.m3 / .m9 / .m10r。
 */
public final class FilterPicker {

    /**
     * {id, 英文名, 中文名, 分组, 左色, 中色, 右色, 效果说明}
     * 分组：0胶片 1人像 2基础 3美颜
     */
    private static final Object[][] FILTERS = {
            // ── 胶片复古 ──
            {60, "NEGATIVE_FILM", "负片", 0,
                    0xFF2E4A4F, 0xFFC77A6A, 0xFFF2D7B6, "暗部青绿、高光暖奶油，青橙负片感"},
            {71, "FILM", "胶片", 0,
                    0xFF3B3A2F, 0xFFB88A5A, 0xFFF2E3C6, "暖棕胶片、轻微褪色"},
            {72, "JAPANESE", "日系", 0,
                    0xFFA8C8D8, 0xFFE8DCCB, 0xFFF6F1E8, "低对比、冷蓝阴影、奶白高光"},
            {79, "N_600_F", "宝丽来600", 0,
                    0xFF7A8F8C, 0xFFD9B08C, 0xFFF4E8D0, "宝丽来褪色，青灰暗部配暖黄高光"},
            {80, "BF_70", "复古70", 0,
                    0xFF6B4F3A, 0xFFC89B6A, 0xFFEED9B0, "70年代棕黄复古，低饱和暖调"},
            {81, "R_600", "复古600", 0,
                    0xFF5E4A42, 0xFFB77A5E, 0xFFE7C9A8, "红棕复古，暗部褐、高光杏色"},
            {82, "P_100F", "富士100F", 0,
                    0xFF3E6B6A, 0xFF8FB7A8, 0xFFE5D9B8, "富士青绿，冷调偏青"},
            {83, "F_50", "富士50", 0,
                    0xFF4F7D86, 0xFFA7C4B7, 0xFFF0E6CC, "青蓝胶片，低饱和清冷"},
            {84, "KC_64", "柯达64", 0,
                    0xFF5A3F2E, 0xFFC46A3F, 0xFFF2C98E, "暖橙红黄，柯达复古浓郁"},
            {85, "V_250", "威士忌250", 0,
                    0xFF4A2E1E, 0xFFA85F2A, 0xFFE8B56B, "威士忌琥珀棕橙，暖暗调"},
            {86, "H_400", "黑白400", 0,
                    0xFF1E1E1E, 0xFF7A7A7A, 0xFFE6E6E6, "高对比黑白，颗粒纪实感"},
            {87, "KP_160", "柯达160", 0,
                    0xFF6B4A3A, 0xFFD18B63, 0xFFF3D9B5, "柯达人像暖调，蜜桃肤色"},
            {88, "FC_400", "富士400", 0,
                    0xFF3F5F5A, 0xFF8FAF9C, 0xFFE7D8B8, "富士绿青，日常胶片"},
            {89, "C_50D", "佳能50D", 0,
                    0xFF5A5F62, 0xFFB8A48C, 0xFFF0E8DA, "佳能中性偏暖，低饱和干净"},
            {90, "KG_200", "柯达200", 0,
                    0xFF5C3D2E, 0xFFC97A4A, 0xFFF0C98A, "暖橙红黄，阳光感"},

            // ── 人像日常 ──
            {57, "FLOWER_DREAM", "繁花如梦", 1,
                    0xFFE8935B, 0xFFF5C08A, 0xFFFBE9D2, "落日暖橙，柔和暖调"},
            {58, "WARM_BLUE", "暖蓝", 1,
                    0xFF6F8FA8, 0xFFA9C3D4, 0xFFF0E2D0, "蓝中带暖，冷背景暖肤色"},
            {59, "FOREST_GREEN", "森绿", 1,
                    0xFF3F5B45, 0xFF7E9B78, 0xFFE3D8B8, "森林绿低饱和，自然安静"},
            {63, "HOLIDAY", "假日", 1,
                    0xFFF28C5B, 0xFFF7D08A, 0xFF9AD1E3, "明快假日感，暖橙配天蓝"},
            {64, "OXYGEN", "氧气", 1,
                    0xFFA8D8C8, 0xFFD9F0E4, 0xFFF7FBF2, "清透氧气感，淡绿白"},
            {65, "MINT", "薄荷", 1,
                    0xFF7FD6C2, 0xFFC8F0E6, 0xFFF5FFFC, "清凉薄荷，干净透亮"},
            {66, "COLD_WHITE", "冷白", 1,
                    0xFFB8C9D9, 0xFFE6EEF5, 0xFFFFFFFF, "冷白清透，低饱和偏冷"},
            {67, "SHALLOTS", "葱绿", 1,
                    0xFF6F9A5A, 0xFFA8C68F, 0xFFE8EFD8, "葱绿清新，草本自然"},
            {68, "PINK_ORANGE", "粉橙", 1,
                    0xFFF08A8A, 0xFFF7B58A, 0xFFFBE0C4, "粉橙暖甜，肤色柔和"},
            {69, "LIVELY", "生动", 1,
                    0xFFE85D4A, 0xFFF2B84B, 0xFF4FB3D9, "高饱和生动，红黄蓝对比强"},
            {70, "DELICACY", "精致", 1,
                    0xFFC9A6A0, 0xFFE8D5C8, 0xFFF7F0E8, "精致低饱和，裸粉杏色"},
            {74, "PINK", "粉色", 1,
                    0xFFF2A6C0, 0xFFFAD1DF, 0xFFFFF0F5, "纯粉柔调，甜美低对比"},
            {75, "LILT", "轻快", 1,
                    0xFFF6C85F, 0xFFF6A6A6, 0xFF9BD7E8, "轻快糖果色，明亮活泼"},
            {76, "BLACKGOLD", "黑金", 1,
                    0xFF1C1A17, 0xFF8A6A2F, 0xFFE7C46A, "黑金暗调，高对比金属暖光"},

            // ── 基础自然 ──
            {61, "DISTINCT", "鲜明", 2,
                    0xFFD6453D, 0xFFF2C14E, 0xFF3E8ED0, "高饱和鲜明，色彩对比强"},
            {62, "ORIGINAL", "原色", 2,
                    0xFF7F8C8D, 0xFFC9B8A8, 0xFFF5F0E8, "原色中性，轻微暖灰"},
            {73, "NATURE", "自然", 2,
                    0xFF6B8E5A, 0xFFC9B98A, 0xFFE8E2D0, "自然绿黄，低饱和日常"},
            {77, "WHITEANDBLACK", "黑白", 2,
                    0xFF111111, 0xFF808080, 0xFFF2F2F2, "纯黑白，中性灰阶"},
            {78, "CLASSIC", "经典", 2,
                    0xFF3F4A5A, 0xFFB89B7A, 0xFFF0E6D2, "经典低饱和暖灰，稳重"},
            {91, "NEUTRAL", "中性", 2,
                    0xFF7A7F82, 0xFFC4BDB2, 0xFFF2EEE8, "中性灰低饱和，干净耐看"},
            {92, "SOFT", "柔和", 2,
                    0xFFD9C7D2, 0xFFE8E0D8, 0xFFF8F6F2, "柔和低对比，朦胧柔光"},

            // ── 美颜视频 ──
            {93, "WHITENING", "美白", 3,
                    0xFFE8D8C8, 0xFFF5EEE6, 0xFFFFFFFF, "提亮肤色、白皙柔光"},
            {94, "BRIGHT_SHINING", "闪耀", 3,
                    0xFFF6D98A, 0xFFFFF1C9, 0xFFFFFFFF, "闪耀亮金，高光通透"},
            {95, "FRESHNESS", "清新", 3,
                    0xFFA8D8C8, 0xFFD9F0E6, 0xFFFFFFFF, "清新透亮，淡青白"},
            {96, "CLEARNESS", "清晰", 3,
                    0xFFB8C9D9, 0xFFE6EEF5, 0xFFFFFFFF, "清晰冷白，干净锐利"},
            {102, "DOLBY", "杜比", 3,
                    0xFF0F1A24, 0xFF7A8A99, 0xFFFFFFFF, "高动态对比，暗部深、高光白"},
            {103, "DOLBY_BRIGHTNESS", "杜比明亮", 3,
                    0xFF2A2A2A, 0xFFB8C9D9, 0xFFFFFFFF, "提亮暗部，整体明亮通透"},
    };

    private static final String[] GROUPS = {
            "胶片复古", "人像日常", "基础自然", "美颜视频"};

    /** 三个配置键的默认滤镜 id */
    public static final String KEY_M3 = "legend.filter.m3";
    public static final String KEY_M9 = "legend.filter.m9";
    public static final String KEY_M10R = "legend.filter.m10r";
    public static final int DEF_M3 = 86;
    public static final int DEF_M9 = 84;
    public static final int DEF_M10R = 73;

    private FilterPicker() {
    }

    /** 按 id 取中文名 */
    public static String nameOf(int id) {
        for (Object[] f : FILTERS) {
            if (((Integer) f[0]).intValue() == id) {
                return (String) f[2];
            }
        }
        return "滤镜 " + id;
    }

    /** 按 key 取默认 id */
    public static int defaultOf(String key) {
        if (KEY_M3.equals(key)) {
            return DEF_M3;
        }
        if (KEY_M9.equals(key)) {
            return DEF_M9;
        }
        if (KEY_M10R.equals(key)) {
            return DEF_M10R;
        }
        return -1;
    }

    /** 构造三段式渐变条 */
    private static GradientDrawable gradient(int left, int mid, int right, boolean highlight) {
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{left, mid, right});
        g.setCornerRadius(14f);
        int strokeW = highlight ? 4 : 2;
        int strokeC = highlight ? MainActivity.C_ACCENT : 0x33000000;
        g.setStroke(strokeW, strokeC);
        return g;
    }

    /** 弹出滤镜选择器 */
    public static void show(final MainActivity a, final String key, int current) {
        final LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = a.dp(12);
        root.setPadding(pad, a.dp(4), pad, a.dp(4));

        final ScrollView sv = new ScrollView(a);
        final LinearLayout list = new LinearLayout(a);
        list.setOrientation(LinearLayout.VERTICAL);
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, a.dp(500)));

        final AlertDialog[] holder = new AlertDialog[1];

        int lastGroup = -1;
        for (final Object[] f : FILTERS) {
            final int id = ((Integer) f[0]).intValue();
            String en = (String) f[1];
            String cn = (String) f[2];
            int grp = ((Integer) f[3]).intValue();
            int cL = ((Integer) f[4]).intValue();
            int cM = ((Integer) f[5]).intValue();
            int cR = ((Integer) f[6]).intValue();
            String desc = (String) f[7];

            if (grp != lastGroup) {
                lastGroup = grp;
                TextView h = new TextView(a);
                h.setText(GROUPS[grp]);
                h.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
                h.setTextColor(0xFF999999);
                h.setPadding(a.dp(6), a.dp(16), a.dp(6), a.dp(6));
                list.addView(h);
            }

            final boolean selected = id == current;

            LinearLayout row = new LinearLayout(a);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(a.dp(10), a.dp(10), a.dp(10), a.dp(10));
            if (selected) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(0x0F000000);
                bg.setCornerRadius(a.dp(10));
                row.setBackground(bg);
            }

            // 渐变条
            View swatch = new View(a);
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                    a.dp(56), a.dp(28));
            sp.rightMargin = a.dp(12);
            swatch.setLayoutParams(sp);
            swatch.setBackground(gradient(cL, cM, cR, selected));
            row.addView(swatch);

            // 文字
            LinearLayout col = new LinearLayout(a);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView t = new TextView(a);
            t.setText(cn);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            t.setTextColor(selected ? MainActivity.C_ACCENT : 0xFF222222);
            if (selected) {
                t.setTypeface(Typeface.DEFAULT_BOLD);
            }
            col.addView(t);

            TextView sub = new TextView(a);
            sub.setText(en + " · " + id + "　" + desc);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            sub.setTextColor(0xFF9A9A9A);
            sub.setPadding(0, a.dp(2), 0, 0);
            col.addView(sub);

            row.addView(col);

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    a.writeConfig(key, String.valueOf(id));
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                    a.refreshCurrentPage();
                }
            });
            list.addView(row);
        }

        final int defId = defaultOf(key);
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle("选择滤镜 · 当前 " + nameOf(current))
                .setView(root)
                .setNegativeButton("取消", null);
        if (defId > 0) {
            b.setNeutralButton("恢复默认", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface d, int which) {
                    a.writeConfig(key, String.valueOf(defId));
                    if (holder[0] != null) {
                        holder[0].dismiss();
                    }
                    a.refreshCurrentPage();
                }
            });
        }
        AlertDialog dlg = b.create();
        holder[0] = dlg;
        dlg.show();
    }
}
