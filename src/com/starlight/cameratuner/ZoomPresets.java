package com.starlight.cameratuner;

import android.os.Build;

/**
 * 焦段重映射预设。
 *
 * 底包（18 Pro Max 相机包 6.8.002090.2）的原始焦段固定为：
 *     0.7 / 1.0 / 2.0 / 3.2 / 6.4
 *
 * 各机型装上这个包后，需要把上面这组档位改成自己机型的实际光学焦段，
 * 否则会出现"点 3.2x 实际是 3x 镜头"这类错位，快捷焦段条也不对。
 *
 * 数据格式：机型名 | 设备代号(Build.DEVICE) | 目标焦段（5 档，逗号分隔）
 *
 * 特殊说明：
 *   - 小米 15 Ultra(xuanyuan)：第 4 档是 3.0（不是 3.2）
 *   - 小米 17 Ultra(nezha)：第 4 档是 3.2~4.3 的光学变焦区间，
 *     取上沿 4.3 作为档位值，用户可用轮盘回到 3.2。
 */
final class ZoomPresets {

    /** 底包原始焦段（未重映射时相机里的值） */
    static final float[] BASE = {0.7f, 1.0f, 2.0f, 3.2f, 6.4f};

    /** 一条预设：display=显示名, device=Build.DEVICE, target=目标焦段 */
    static final class Preset {
        final String display;
        final String device;
        final float[] target;
        /** 循环档位表：光学最高档 + 数字变焦递进 */
        final float[] cycle;

        Preset(String display, String device, float[] target, float[] cycle) {
            this.display = display;
            this.device = device;
            this.target = target;
            this.cycle = cycle;
        }

        /** 生成本机需要的 zoom_remap 字符串，例如 "0.7:0.6,3.2:5.0,6.4:10.0" */
        String toRemapString() {
            StringBuilder sb = new StringBuilder();
            int n = Math.min(BASE.length, target.length);
            for (int i = 0; i < n; i++) {
                if (Math.abs(BASE[i] - target[i]) < 0.0001f) continue;  // 相同的不用写
                if (sb.length() > 0) sb.append(',');
                sb.append(fmt(BASE[i])).append(':').append(fmt(target[i]));
            }
            return sb.toString();
        }

        /** 生成 legend.zoomcycle_stops 字符串，例如 "10,20,30" */
        String toCycleString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cycle.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(fmt(cycle[i]));
            }
            return sb.toString();
        }

        /**
         * 生成 zoom_drop 字符串。
         *
         * 底包在人像等模式会多出一个 4.3 档位，而很多机型（15 Pro/15s Pro/17 Pro 等）
         * 并没有这个光学焦段，留着会让用户切到不存在的镜头。
         * 规则：目标焦段里没有 4.3 的，就把它剔除。
         */
        String toDropString() {
            for (float v : target) {
                if (Math.abs(v - 4.3f) < 0.0001f) return "";   // 本机有 4.3，不剔
            }
            return "4.3";
        }

        private static String fmt(float v) {
            if (Math.abs(v - Math.round(v)) < 0.0001f) {
                return String.valueOf((int) Math.round(v));
            }
            return String.valueOf(v);
        }
    }

    static final Preset[] ALL = {
            // 循环表 = 光学最高档 + 数字变焦递进（写死，方便以后加机型）
            new Preset("小米 15",         "dada",
                    new float[]{0.6f, 1f, 2f, 2.6f, 5f},
                    new float[]{5f, 10f, 20f}),
            new Preset("小米 15 Pro",     "haotian",
                    new float[]{0.6f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 15s Pro",    "dojun",
                    new float[]{0.6f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 15 Ultra",   "xuanyuan",
                    new float[]{0.6f, 1f, 2f, 3f, 4.3f},
                    new float[]{4.3f, 10f, 20f}),
            new Preset("小米 17",         "pudding",
                    new float[]{0.7f, 1f, 2f, 2.6f, 5f},
                    new float[]{5f, 10f, 20f}),
            new Preset("小米 17 Pro",     "pandora",
                    new float[]{0.7f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 17 Pro Max", "popsicle",
                    new float[]{0.7f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 17 Max",     "byron",
                    new float[]{0.7f, 1f, 2f, 3f, 6f},
                    new float[]{6f, 10f, 20f}),
            new Preset("小米 17T",        "chagall",
                    new float[]{0.6f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 17T Pro",    "warhol",
                    new float[]{0.6f, 1f, 2f, 5f, 10f},
                    new float[]{10f, 20f, 30f}),
            new Preset("小米 17 Ultra",   "nezha",
                    new float[]{0.6f, 1f, 2f, 4.3f, 8.6f},
                    new float[]{8.6f, 12f, 20f}),
    };

    private ZoomPresets() {
    }

    /** 按设备代号找预设（用于 zoom.preset 配置项） */
    static Preset byDevice(String device) {
        if (device == null || device.trim().isEmpty()) return null;
        String d = device.trim();
        for (Preset p : ALL) {
            if (p.device.equalsIgnoreCase(d)) return p;
        }
        return null;
    }

    /** 按当前设备代号找匹配的预设，找不到返回 null */
    static Preset current() {
        String dev = Build.DEVICE;
        if (dev == null) return null;
        for (Preset p : ALL) {
            if (p.device.equalsIgnoreCase(dev)) return p;
        }
        return null;
    }

    /** 找一个 preset 的索引，找不到返回 -1 */
    static int indexOf(Preset p) {
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i] == p) return i;
        }
        return -1;
    }

    /** 判断当前配置的 zoom_remap 是否等于某个预设生成的值 */
    static boolean matches(String remap, Preset p) {
        if (remap == null || p == null) return false;
        String want = p.toRemapString();
        return want.equals(remap.replace(" ", "").trim());
    }

    /** 同时比对 remap 和 drop 是否都跟预设一致 */
    static boolean matchesBoth(String remap, String drop, Preset p) {
        if (p == null) return false;
        if (!matches(remap, p)) return false;
        String wantDrop = p.toDropString();
        String curDrop = (drop == null) ? "" : drop.replace(" ", "").trim();
        return wantDrop.equals(curDrop);
    }
}
