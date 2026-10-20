package com.starlight.cameratuner;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 传奇一瞬色彩模拟 —— 移植自 XiaomiCamPort (pudding) LegendaryColor。
 *
 * 背景：18 Pro Max 的传奇色彩实现在 HAL 调参里（vendor tag legendMode/capsInfo
 * 本机 HAL 根本没注册），15 Pro 的 HAL 不可能实现；预览/成片都没有任何
 * 应用侧渲染路径，所以拍出来就是原图。
 *
 * 本 hook 的做法：把 M3/M9/M10R 三种传奇风格映射到相机已有的 CubeLut 滤镜管线
 * （这条管线「预览 + 成片」都验证过可用），让传奇一瞬正常出片。
 *
 * 配置（/sdcard/CameraTuner/config.conf）：
 *   legend.color=1            总开关（1=开 0=关）
 *   legend.filter.m3=77       默认 WHITEANDBLACK（黑白）
 *   legend.filter.m9=84       默认 KC_64（Kodachrome64，接近 CCD 鲜艳暖调）
 *   legend.filter.m10r=73     默认 NATURE（自然）
 *   legend.degree=100         滤镜强度 0-100
 *
 * 值可以写数字 id，也可以写 LUT 名（lut_normal_classic / N_CLASSIC / classic）。
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class LegendaryColor {

    private static final String TAG = "CameraTuner";
    private static final String HEAD = "传奇: ";

    private static final String C_CAMERA_PKG = "com.android.camera";
    /** i.Q() 读当前滤镜 id */
    private static final String C_FILTER_Q = "com.android.camera.data.data.i";
    /** LegendaryModuleDevice，d(k6.k) 会话参数重建 */
    private static final String C_T3 = "T3.c";
    private static final String C_SESSION_PARAM = "k6.k";
    /** EffectController：e0 写滤镜、s0 挂引擎 */
    private static final String C_EC = "com.xiaomi.camera.effect.EffectController";
    private static final String C_MODULE2 = "com.android.camera.module.Camera2Module";
    /** DI 容器 */
    private static final String C_REPO = "g2.a";
    /** 传奇风格组件 r2.B（读 M3/M9/M10R） */
    private static final String C_LEGEND = "r2.B";
    /** u2.Y.D(int) 把索引换成 mode */
    private static final String C_Y = "u2.W";
    /** 模块基类，onRenderEngineCreate/Destroy 声明在这 */
    private static final String C_BASEMOD = "com.android.camera.module.t";
    /** PhotoBase 也可能声明 onRenderEngineCreate（作者两个都挂） */
    private static final String C_PHOTOBASE = "com.xiaomi.camera.module.PhotoBase";
    private static final String C_LEGMODULE =
            "com.android.camera.features.mode.legendary.LegendaryModule";
    /** 渲染操作枚举，Ku.d.f == 滤镜 renderer（Tu.j） */
    private static final String C_KUD = "Ku.d";

    /** 传奇一瞬模式 ID = 0x100 */
    private static final int LEGEND_MODE = 256;

    /** LUT 后缀 / 枚举名 -> o3.d 序号（正常滤镜组，走 type=1 主 CubeLut 槽） */
    private static final Map<String, Integer> LUT_IDS = new HashMap<String, Integer>();

    static {
        put("flower_dream", 57, "FLOWER_DREAM");
        put("warm_blue", 58, "WARM_BLUE");
        put("forest_green", 59, "FOREST_GREEN");
        put("negative_film", 60, "NEGATIVE_FILM");
        put("distinct", 61, "DISTINCT");
        put("original", 62, "ORIGINAL");
        put("holiday", 63, "HOLIDAY");
        put("oxygen", 64, "OXYGEN");
        put("mint", 65, "MINT");
        put("cold_white", 66, "COLD_WHITE");
        put("shallots", 67, "SHALLOTS");
        put("pink_orange", 68, "PINK_ORANGE");
        put("lively", 69, "LIVELY");
        put("delicacy", 70, "DELICACY");
        put("film", 71, "FILM");
        put("japanese", 72, "JAPANESE");
        put("nature", 73, "NATURE");
        put("pink", 74, "PINK");
        put("lilt", 75, "LILT");
        put("blackgold", 76, "BLACKGOLD");
        put("whiteandblack", 77, "WHITEANDBLACK");
        put("classic", 78, "CLASSIC");
        put("600_f", 79, "N_600_F");
        put("bf_70", 80, "BF_70");
        put("r_600", 81, "R_600");
        put("p_100f", 82, "P_100F");
        put("f_50", 83, "F_50");
        put("c_64", 84, "KC_64");
        put("v_5207", 85, "V_250");
        put("h_400", 86, "H_400");
        put("p_160nc", 87, "KP_160");
        put("p_400h", 88, "FC_400");
        put("c_50d", 89, "C_50D");
        put("g_200", 90, "KG_200");
        put("neutral", 91, "NEUTRAL");
        put("soft", 92, "SOFT");
        put("whitening", 93, "WHITENING");
        put("brightshining", 94, "BRIGHT_SHINING");
        put("freshness", 95, "FRESHNESS");
        put("clearness", 96, "CLEARNESS");
        put("dolby", 102, "DOLBY");
        put("dolby_brightness", 103, "DOLBY_BRIGHTNESS");
    }

    private static void put(String lut, int id, String enumName) {
        LUT_IDS.put(lut, Integer.valueOf(id));
        LUT_IDS.put(enumName.toLowerCase(), Integer.valueOf(id));
    }

    private static ClassLoader sCl;
    private static boolean sInstalled;
    private static volatile boolean sEnabled;
    private static volatile int sDegree = 100;
    private static volatile int sFm3 = 86;
    private static volatile int sFm9 = 84;
    private static volatile int sFm10r = 73;

    private static Class<?> sClsY;
    private static Class<?> sClsRepo;
    private static Class<?> sClsLegend;
    private static Class<?> sClsEc;
    private static Method sMethodD;

    private static volatile String sLastVal = "";
    private static volatile String sLastLog = "";
    private static volatile String sLastError = "";
    private static volatile String sLastAdd = "";
    /** EffectController.s0(v) 挂上来的预览渲染引擎 F8.j */
    private static volatile Object sEngine;

    private LegendaryColor() {
    }

    // ==================================================================
    // 入口
    // ==================================================================
    public static void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (lpparam == null || !C_CAMERA_PKG.equals(lpparam.packageName)) {
            return;
        }
        sCl = lpparam.classLoader;
        if (sInstalled) {
            return;
        }
        sInstalled = true;
        loadConfig();
        log("loaded: enabled=" + sEnabled + " m3=" + sFm3 + " m9=" + sFm9
                + " m10r=" + sFm10r + " degree=" + sDegree);
        if (!sEnabled) {
            return;
        }

        // 1) i.Q() 返回滤镜 id：让 App 全程「以为」传奇模式选了滤镜，
        //    这样 AI 色彩修正 / setAiSceneEffect 等会绕过我们
        try {
            XposedHelpers.findAndHookMethod(C_FILTER_Q, sCl, "Q", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (currentMode() != LEGEND_MODE) {
                            return;
                        }
                        int id = mapped();
                        if (id > 0) {
                            param.setResult(Integer.valueOf(id));
                        }
                    } catch (Throwable th) {
                        err("Q", th);
                    }
                }
            });
            log("hook i.Q ok");
        } catch (Throwable th) {
            err("hook Q", th);
        }

        // 2) T3.c.d(k)：会话参数重建（开相机 / 切 M3-M9-M10R）的确定性时机
        try {
            XposedHelpers.findAndHookMethod(C_T3, sCl, "d",
                    XposedHelpers.findClass(C_SESSION_PARAM, sCl), new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                apply("T3.c.d");
                            } catch (Throwable th) {
                                err("T3.c.d", th);
                            }
                        }
                    });
            log("hook T3.c.d ok");
        } catch (Throwable th) {
            err("hook T3.c.d", th);
        }

        // 3) EffectController.e0(int,int)：滤镜状态的唯一写入口，统一强制成传奇滤镜
        try {
            XposedHelpers.findAndHookMethod(C_EC, sCl, "e0",
                    Integer.TYPE, Integer.TYPE, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                if (currentMode() != LEGEND_MODE) {
                                    return;
                                }
                                int id = mapped();
                                if (id > 0) {
                                    param.args[0] = Integer.valueOf(id);
                                }
                            } catch (Throwable th) {
                                err("e0", th);
                            }
                        }
                    });
            log("hook EffectController.e0 ok");
        } catch (Throwable th) {
            err("hook e0", th);
        }

        // 4) EffectController.s0(v)：渲染引擎挂载
        //    e0() 只在引擎已挂载时才把滤镜下发给预览渲染器，
        //    而传奇模式切会话会先重启预览（引擎解绑 → 重新挂载），
        //    apply 恰好落在这个空窗里 —— 这就是「成片有色、预览没色」的原因。
        //    所以引擎一挂上就补推一次。
        try {
            Class<?> ecCls = XposedHelpers.findClass(C_EC, sCl);
            Method s0 = null;
            Method[] methods = ecCls.getDeclaredMethods();
            for (Method m : methods) {
                if ("s0".equals(m.getName()) && m.getParameterTypes().length == 1) {
                    s0 = m;
                    break;
                }
            }
            if (s0 == null) {
                throw new NoSuchMethodException(C_EC + "#s0");
            }
            final Class<?> s0Param = s0.getParameterTypes()[0];
            XposedHelpers.findAndHookMethod(ecCls, "s0", s0Param, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    try {
                        if (param.args == null || param.args.length < 1
                                || param.args[0] == null) {
                            return; // 解绑 s0(null) 不用推
                        }
                        sEngine = param.args[0];
                        apply("s0.attach");
                    } catch (Throwable th) {
                        err("s0", th);
                    }
                }
            });
            log("hook EffectController.s0 ok");
        } catch (Throwable th) {
            err("hook s0", th);
        }

        // 5) Camera2Module.updateFilter(int,int)：同步 moduleState，避免每次拍照重算
        try {
            XposedHelpers.findAndHookMethod(C_MODULE2, sCl, "updateFilter",
                    Integer.TYPE, Integer.TYPE, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                if (param.thisObject == null) {
                                    return;
                                }
                                Object mode = XposedHelpers.callMethod(
                                        param.thisObject, "getModuleIndex");
                                if (mode == null
                                        || ((Integer) mode).intValue() != LEGEND_MODE) {
                                    return;
                                }
                                int id = mapped();
                                if (id > 0) {
                                    param.args[0] = Integer.valueOf(id);
                                }
                            } catch (Throwable th) {
                                err("updateFilter", th);
                            }
                        }
                    });
            log("hook Camera2Module.updateFilter ok");
        } catch (Throwable th) {
            err("hook updateFilter", th);
        }

        // 6) onRenderEngineCreate / onRenderEngineDestroy —— 预览无色的最终根因
        //    其余 13 个模块都覆写了 onRenderEngineCreate()，在里面调 V0(Ku.d.f)
        //    把滤镜 renderer Tu.j 加进预览绘制列表 H；
        //    LegendaryModule 没有覆写，走基类实现（只注册 engine，不加 renderer）。
        //    ★ 作者挂了两个类：com.android.camera.module.u 和
        //      com.xiaomi.camera.module.PhotoBase，二者谁声明了就挂谁。
        final XC_MethodHook createHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (!isLegendaryModule(param.thisObject)) {
                        return;
                    }
                    ensureFilterRenderer(param.thisObject, "onRenderEngineCreate");
                    apply("onRenderEngineCreate");
                } catch (Throwable th) {
                    err("onRenderEngineCreate", th);
                }
            }
        };
        final XC_MethodHook destroyHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                try {
                    if (!isLegendaryModule(param.thisObject)) {
                        return;
                    }
                    Object fi = renderEngineOf(param.thisObject);
                    if (fi == null) {
                        return;
                    }
                    XposedHelpers.callMethod(fi, "c1", kuField("f"));
                    String tag = "onRenderEngineDestroy#remove";
                    if (!tag.equals(sLastAdd)) {
                        sLastAdd = tag;
                        log("onRenderEngineDestroy: removeLocalRenderer(Ku.d.f)");
                    }
                } catch (Throwable th) {
                    err("onRenderEngineDestroy", th);
                }
            }
        };
        hookLifecycle(C_BASEMOD, "onRenderEngineCreate", createHook);
        hookLifecycle(C_BASEMOD, "onRenderEngineDestroy", destroyHook);
        hookLifecycle(C_PHOTOBASE, "onRenderEngineCreate", createHook);
        hookLifecycle(C_PHOTOBASE, "onRenderEngineDestroy", destroyHook);
    }

    // ==================================================================
    // 内部逻辑
    // ==================================================================
    /** 只在类确实声明了该方法时才挂钩（PhotoBase 可能并没有声明，避免误报错误）。 */
    private static void hookLifecycle(String clsName, String method, XC_MethodHook cb) {
        try {
            Class<?> cls = XposedHelpers.findClass(clsName, sCl);
            Method[] methods = cls.getDeclaredMethods();
            boolean declared = false;
            for (Method m : methods) {
                if (method.equals(m.getName())) {
                    declared = true;
                    break;
                }
            }
            if (!declared) {
                log("skip " + clsName + "#" + method + " (not declared)");
                return;
            }
            XposedHelpers.findAndHookMethod(cls, method, cb);
            log("hooked " + clsName + "#" + method);
        } catch (Throwable th) {
            err("hook " + clsName + "#" + method, th);
        }
    }

    private static boolean isLegendaryModule(Object obj) {
        if (obj == null || sCl == null) {
            return false;
        }
        try {
            return XposedHelpers.findClass(C_LEGMODULE, sCl).isInstance(obj);
        } catch (Throwable th) {
            return false;
        }
    }

    private static Object kuField(String name) {
        Class<?> kuCls = XposedHelpers.findClass(C_KUD, sCl);
        try {
            Field f = kuCls.getField(name);
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable th) {
            throw new IllegalStateException("Ku.d." + name + " not found", th);
        }
    }

    /** 取预览渲染引擎 F8.j：优先 mCallback.fi()，拿不到就用 s0 记下的。 */
    private static Object renderEngineOf(Object module) {
        if (module != null) {
            try {
                Object cb = XposedHelpers.getObjectField(module, "mCallback");
                if (cb != null) {
                    Object fi = XposedHelpers.callMethod(cb, "fi");
                    if (fi != null) {
                        return fi;
                    }
                }
            } catch (Throwable ignored) {
                // 走下面的兜底
            }
        }
        return sEngine;
    }

    /**
     * V0(Ku.d.f) = PreviewRenderEngine.addLocalRenderer，
     * 把滤镜 renderer Tu.j 加进预览绘制列表 H（内部有 contains 判断，重复调用无副作用）。
     */
    private static void ensureFilterRenderer(Object module, String where) {
        try {
            Object fi = renderEngineOf(module);
            if (fi == null) {
                log(where + ": render engine not ready");
                return;
            }
            XposedHelpers.callMethod(fi, "V0", kuField("f"));
            String tag = where + "#add";
            if (!tag.equals(sLastAdd)) {
                sLastAdd = tag;
                log(where + ": addLocalRenderer(Ku.d.f)");
            }
        } catch (Throwable th) {
            err("ensureFilterRenderer/" + where, th);
        }
    }

    /** 把传奇滤镜写进 EffectController（预览/出片实际读取的状态）。 */
    private static void apply(String where) {
        if (currentMode() != LEGEND_MODE) {
            return;
        }
        int id = mapped();
        if (id <= 0) {
            return;
        }
        try {
            if (sClsEc == null) {
                sClsEc = XposedHelpers.findClass(C_EC, sCl);
            }
            // 预览侧保险：先保证滤镜 renderer 已进绘制列表，再写滤镜状态
            if (sEngine != null) {
                ensureFilterRenderer(null, where);
            }
            Object u = XposedHelpers.callStaticMethod(sClsEc, "u");
            XposedHelpers.callMethod(u, "e0",
                    Integer.valueOf(id), Integer.valueOf(sDegree));
            String tag = where + "#" + id;
            if (!tag.equals(sLastLog)) {
                sLastLog = tag;
                log("apply via " + where + " -> filterId=" + id
                        + " degree=" + sDegree);
            }
        } catch (Throwable th) {
            err("apply/" + where, th);
        }
    }

    /** 当前模式（等价于 i.Q() 里那句 yG.D(yG.u)），非 256 时所有 hook 都不干预。 */
    private static int currentMode() {
        try {
            if (sClsY == null) {
                sClsY = XposedHelpers.findClass(C_Y, sCl);
            }
            if (sClsRepo == null) {
                sClsRepo = XposedHelpers.findClass(C_REPO, sCl);
            }
            if (sMethodD == null) {
                sMethodD = sClsY.getDeclaredMethod("D", Integer.TYPE);
            }
            Object y = XposedHelpers.callStaticMethod(sClsRepo, "g");
            int idx = XposedHelpers.getIntField(y, "u");
            Object mode = sMethodD.invoke(y, Integer.valueOf(idx));
            return mode == null ? -1 : ((Integer) mode).intValue();
        } catch (Throwable th) {
            err("currentMode", th);
            return -1;
        }
    }

    /** 读传奇风格组件值（M3 / M9 / M10R），和 T3.c.d 里的读法一致。 */
    private static String legendValue() {
        try {
            if (sClsRepo == null) {
                sClsRepo = XposedHelpers.findClass(C_REPO, sCl);
            }
            if (sClsLegend == null) {
                sClsLegend = XposedHelpers.findClass(C_LEGEND, sCl);
            }
            Object repo = XposedHelpers.callStaticMethod(sClsRepo, "a");
            Object comp = XposedHelpers.callMethod(repo, "x", sClsLegend);
            Object val = XposedHelpers.callMethod(comp, "getComponentValue",
                    Integer.valueOf(LEGEND_MODE));
            String v = val == null ? null : val.toString();
            if (v == null || v.length() == 0) {
                v = "M10R";
            }
            if (!v.equals(sLastVal)) {
                sLastVal = v;
                log("legend value = " + v + " -> filterId=" + mapped());
            }
            return v;
        } catch (Throwable th) {
            err("legendValue", th);
            return null;
        }
    }

    private static int mapped() {
        String v = legendValue();
        if (v == null) {
            return -1;
        }
        if ("M3".equals(v)) {
            return sFm3;
        }
        if ("M9".equals(v)) {
            return sFm9;
        }
        return sFm10r;
    }

    private static int resolveFilter(String raw) {
        if (raw == null) {
            return -1;
        }
        String s = raw.trim();
        if (s.length() == 0) {
            return -1;
        }
        try {
            int i = Integer.parseInt(s);
            if (i > 0 && i <= 65535) {
                return i;
            }
        } catch (NumberFormatException e) {
            // 不是数字，按名字查
        }
        String k = s.toLowerCase();
        if (k.startsWith("lut_normal_")) {
            k = k.substring("lut_normal_".length());
        } else if (k.startsWith("lut_")) {
            k = k.substring(4);
        } else if (k.startsWith("n_")) {
            k = k.substring(2);
        }
        Integer box = LUT_IDS.get(k);
        return box == null ? -1 : box.intValue();
    }

    // ==================================================================
    // 配置
    // ==================================================================
    private static void loadConfig() {
        String[] paths = {
                "/sdcard/CameraTuner/config.conf",
                "/storage/emulated/0/CameraTuner/config.conf",
                "/data/local/tmp/CameraTuner/config.conf"};
        String chosen = null;
        for (String p : paths) {
            File f = new File(p);
            if (f.isFile() && f.canRead()) {
                chosen = p;
                break;
            }
        }
        if (chosen == null) {
            log("config.conf not found, use defaults");
            return;
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(chosen));
            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.length() == 0 || t.startsWith("#")) {
                    continue;
                }
                int eq = t.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = t.substring(0, eq).trim();
                String val = t.substring(eq + 1).trim();
                if ("legend.color".equals(key) || "legend.enable".equals(key)) {
                    sEnabled = !"0".equals(val) && !"false".equalsIgnoreCase(val)
                            && !"off".equalsIgnoreCase(val);
                } else if ("observe_only".equals(key)) {
                    if ("1".equals(val) || "true".equalsIgnoreCase(val)) {
                        sEnabled = false;
                    }
                } else if ("legend.degree".equals(key)) {
                    try {
                        int d = Integer.parseInt(val);
                        if (d >= 0 && d <= 100) {
                            sDegree = d;
                        }
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                } else {
                    int id = resolveFilter(val);
                    if (id <= 0) {
                        if (key.startsWith("legend.filter.")) {
                            log("unknown filter value '" + val + "' for " + key + ", ignored");
                        }
                        continue;
                    }
                    if ("legend.filter.m3".equals(key)) {
                        sFm3 = id;
                    } else if ("legend.filter.m9".equals(key)) {
                        sFm9 = id;
                    } else if ("legend.filter.m10r".equals(key)) {
                        sFm10r = id;
                    }
                }
            }
            log("config " + chosen + " ok");
        } catch (Throwable th) {
            err("loadConfig", th);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    // ==================================================================
    // 日志
    // ==================================================================
    private static void log(String msg) {
        try {
            Log.i(TAG, HEAD + msg);
            XposedBridge.log(TAG + ": " + HEAD + msg);
        } catch (Throwable ignore) {
        }
    }

    private static void err(String where, Throwable th) {
        String msg = String.valueOf(where) + " -> " + th;
        if (msg.equals(sLastError)) {
            return;
        }
        sLastError = msg;
        log("ERROR " + msg);
    }
}
