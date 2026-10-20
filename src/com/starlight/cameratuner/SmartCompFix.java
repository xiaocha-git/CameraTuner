package com.starlight.cameratuner;

import android.hardware.camera2.CaptureResult;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * AI帮拍「智能构图」修复 —— 移植自 XiaomiCamPort (pudding) SmartCompFix。
 *
 * 根因（原作者结论，经实测复现）：
 *   智能构图依赖 vendor tag com.xiaomi.camera.autoCrop.*，
 *   本机 HAL 未注册该 section →
 *     l9.h.U3(caps) 读 autoCropVersion 恒 null → 四道门第一道就死 →
 *     u2.H 的 f17374a 恒 false → isSwitchOn() 恒 false →
 *     点击写进 pref 的 ON 永远读不回来 → 图标被拨回 → 「按了没反应」。
 *   另有一处独立卡点：FragmentAi.onClick 第一行 if(!f8958y0) return;
 *     f8958y0 只由 ft(...,true) 置位，AI 结果不回来时恒 false → 点击直接被吞。
 *
 * 两代实现：
 *   v1 = ASD 代（s6.r0 / SemanticScene），v2 = autoCrop 代（s6.s0 / autoCropData）
 *   本机两个数据源都没有，v2 是「跑起来但 100% 无数据」，v1 是「一次都没走到」。
 *   档 1~4 抬 U3 走 v2 = 死路；档 5~8 抬 V3 走 v1 = 活路。
 *
 * 配置（写在 /sdcard/CameraTuner/config.conf）：
 *   ai.smartcomp = 0..8
 *     0 关（默认，不干预）
 *     1 抬能力门 U3 + FragmentAi.ft 解锁点击
 *     2 +比例门 t0
 *     3 +下发门 l9.g.S0(autoCropEnable)
 *     4 +兜底门 f17374a 强制 true
 *     5 ★只抬 V3★ → 走 v1 分支
 *     6 +补齐 v1：F.w 改判 / isSwitchOn 出口改判
 *     7 +抬齐 v1 剩余三门：l9.g.S0(SemanticScene) / F.d0 / i5.g.M + 合成后门
 *     8 +取景引导环（ring.*，合成 fake 构图数据驱动 UI）
 *   ai.smartcomp.submit  = 0|1  A3.g.w() 是否放行
 *   ai.smartcomp.compose = 0|1  合成数据来源（档8）
 *   ai.smartcomp.cycle   = 0|1  每次合成后索引自增（演示用）
 *   ai.smartcomp.dbgindex= <int> 合成索引
 *   ai.smartcomp.dbgfrag = 0|1  fragment 处 debug TextView 放行（默认 0，有 NPE 风险）
 *   ai.smartcomp.ring    = 0|1  档8 是否合成
 *   ai.smartcomp.ring.x/.y/.w/.h = 0..1 固定矩形（previewSize 比例）
 *   ai.smartcomp.ring.zoom/.tip/.src/.idle/.pad/.fill/.autozoom/.zmax/.smooth
 *   ai.smartcomp.ring.zoomms/.exp/.probe/.iou/.med/.wband/.dead/.lock/.walpha/.wrate
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public final class SmartCompFix {

    private static final String TAG = "CameraTuner";
    private static final String HEAD = "构图fix: ";

    private static final String C_CAMERA_PKG = "com.android.camera";

    /** dex Ll9/h; —— U3 能力门 / t0 比例门 / V3 */
    private static final String C_H = "l9.h";
    /** dex Ll9/g; —— S0(String) vendor tag 自报表 */
    private static final String C_G = "l9.g";
    /** dex Lu2/H; —— ComponentGlobalSmartComposition */
    private static final String C_COMP = "u2.H";
    /** dex 字段真名；jadx 里叫 f17374a */
    private static final String F_SUPPORT = "a";
    /** dex Lw2/a; —— 构图项选择器（值 = off | ai | 创意图案 id） */
    private static final String C_SEL = "w2.a";
    /** dex Li5/g; —— SmartCompositionManager */
    private static final String C_MGR = "i5.g";
    /** dex LA3/g; —— AiFeatureSubmitHelper */
    private static final String C_SUBMIT = "A3.g";
    /** v2 拦截器 jadx p154s6.s0 */
    private static final String C_S0_V2 = "s6.s0";
    /** v1 拦截器 dex 真名 s6.r0 */
    private static final String C_R0_V1 = "s6.r0";
    private static final String C_AI_FRAG = "com.android.camera.features.mode.ai.FragmentAi";
    private static final String C_DATA_F = "com.android.camera.data.data.F";
    private static final String C_ITR_I = "com.android.camera.module.interceptor.base.i";
    private static final String C_CAPMOD = "com.android.camera.features.mode.capture.CaptureModule";
    private static final String C_KR_G = "Kr.g";
    private static final String C_JA_R = "ja.r";
    private static final String C_ZOOM = "R6.C0";

    /** 构图项选中 AI 的字面值 */
    private static final String V_AI = "ai";
    private static final int MODE_PHOTO = 163;
    private static final int MODE_AI_HELP = 168;

    private static final String TAG_ENABLE = "com.xiaomi.camera.autoCrop.autoCropEnable";

    private static final String[] RATIOS = {
            "4x3", "3x2", "16x9", "9x16", "1x1", "full", "18x9", "20x9", "21x9"};

    private static ClassLoader sCl;
    private static boolean sInstalled;
    private static boolean sSubmitLogDone;

    private static volatile boolean sInAcc;
    private static String sLastData;
    private static String sLastOut;
    private static String sLastItr;

    /** 用户在相机里那个「智能构图」开关：-1 未知 / 0 关 / 1 开 */
    private static volatile int sUserOn = -1;
    private static volatile String sUserOnWhy;

    private static volatile boolean sCfgDone;
    private static volatile int sLevel;

    private static volatile int sRing = 1;
    private static volatile float sRingX = 0.30f;
    private static volatile float sRingY = 0.32f;
    private static volatile float sRingW = 0.40f;
    private static volatile float sRingH = 0.36f;
    private static volatile float sRingZoom = 1.0f;
    private static volatile int sRingTip = 0;
    private static volatile int sRingSrc = 1;
    private static volatile int sRingIdle = 0;
    private static volatile float sRingPad = 1.30f;
    private static volatile float sRingFill = 0.45f;
    private static volatile int sRingAutoZoom = 1;
    private static volatile float sRingZMax = 6.0f;
    private static volatile float sRingSmooth = 0.30f;
    private static volatile long sRingZoomMs = 60;
    private static volatile int sRingExp = 2;
    private static volatile int sRingProbe = 0;
    private static volatile float sRingIou = 0.35f;
    private static volatile int sRingMed = 5;
    private static volatile float sRingWantBand = 0.10f;
    private static volatile float sRingDead = 0.06f;
    private static volatile int sRingLock = 1;
    private static volatile float sRingWantAlpha = 0.08f;
    private static volatile float sRingWantRate = 0.5f;

    private static volatile int sSubmit;
    private static volatile int sDbgIndex;
    private static volatile int sDbgFrag;
    private static volatile int sCompose;
    private static volatile int sCycle;

    private static volatile boolean sHideFrame;
    private static volatile long sIsThread = -1L;

    private static Object sMgr;
    private static Object sSel;
    private static Object sR0;

    private static String sCfgPath;

    private SmartCompFix() {
    }

    // ==================================================================
    // 日志
    // ==================================================================
    private static void log(String msg) {
        XposedBridge.log(TAG + ": " + HEAD + msg);
    }

    private static void logN(String key, String msg, int step) {
        log("[" + key + "] " + msg);
    }

    private static void err(Throwable th) {
        XposedBridge.log(TAG + ": " + HEAD + "!! " + Log.getStackTraceString(th));
    }

    // ==================================================================
    // 配置
    // ==================================================================
    private static void ensureCfg() {
        if (sCfgDone) {
            return;
        }
        sCfgDone = true;
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
        sCfgPath = chosen;
        if (chosen == null) {
            log("未找到配置文件，智能构图不启用");
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
                String k = t.substring(0, eq).trim();
                String v = t.substring(eq + 1).trim();
                try {
                    if ("ai.smartcomp".equals(k)) {
                        sLevel = Integer.parseInt(v);
                    } else if ("ai.smartcomp.submit".equals(k)) {
                        sSubmit = Integer.parseInt(v);
                    } else if ("ai.smartcomp.dbgindex".equals(k)) {
                        sDbgIndex = Integer.parseInt(v);
                    } else if ("ai.smartcomp.dbgfrag".equals(k)) {
                        sDbgFrag = Integer.parseInt(v);
                    } else if ("ai.smartcomp.compose".equals(k)) {
                        sCompose = Integer.parseInt(v);
                    } else if ("ai.smartcomp.cycle".equals(k)) {
                        sCycle = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring".equals(k)) {
                        sRing = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.x".equals(k)) {
                        sRingX = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.y".equals(k)) {
                        sRingY = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.w".equals(k)) {
                        sRingW = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.h".equals(k)) {
                        sRingH = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.zoom".equals(k)) {
                        sRingZoom = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.tip".equals(k)) {
                        sRingTip = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.src".equals(k)) {
                        sRingSrc = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.idle".equals(k)) {
                        sRingIdle = Integer.parseInt(v) != 0 ? 1 : 0;
                    } else if ("ai.smartcomp.ring.pad".equals(k)) {
                        sRingPad = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.fill".equals(k)) {
                        sRingFill = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.autozoom".equals(k)) {
                        sRingAutoZoom = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.zmax".equals(k)) {
                        sRingZMax = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.smooth".equals(k)) {
                        sRingSmooth = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.zoomms".equals(k)) {
                        sRingZoomMs = Long.parseLong(v);
                    } else if ("ai.smartcomp.ring.exp".equals(k)) {
                        sRingExp = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.probe".equals(k)) {
                        sRingProbe = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.iou".equals(k)) {
                        sRingIou = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.med".equals(k)) {
                        sRingMed = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.wband".equals(k)) {
                        sRingWantBand = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.dead".equals(k)) {
                        sRingDead = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.lock".equals(k)) {
                        sRingLock = Integer.parseInt(v);
                    } else if ("ai.smartcomp.ring.walpha".equals(k)) {
                        sRingWantAlpha = Float.parseFloat(v);
                    } else if ("ai.smartcomp.ring.wrate".equals(k)) {
                        sRingWantRate = Float.parseFloat(v);
                    }
                } catch (Throwable ignore) {
                }
            }
            if (sLevel < 0) {
                sLevel = 0;
            }
            if (sLevel > 8) {
                sLevel = 8;
            }
            if (sSubmit < 0) {
                sSubmit = 0;
            }
            if (sSubmit > 1) {
                sSubmit = 1;
            }
            if (sRingX < 0f) {
                sRingX = 0f;
            }
            if (sRingX > 0.95f) {
                sRingX = 0.95f;
            }
            if (sRingY < 0f) {
                sRingY = 0f;
            }
            if (sRingY > 0.95f) {
                sRingY = 0.95f;
            }
            if (sRingW < 0.05f) {
                sRingW = 0.05f;
            }
            if (sRingW > 1f) {
                sRingW = 1f;
            }
            if (sRingH < 0.05f) {
                sRingH = 0.05f;
            }
            if (sRingH > 1f) {
                sRingH = 1f;
            }
        } catch (Throwable th) {
            err(th);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    private static String cfgPath() {
        return sCfgPath;
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
        ensureCfg();
        log("installed ai.smartcomp=" + sLevel
                + " submit=" + sSubmit
                + " dbgindex=" + sDbgIndex
                + " dbgfrag=" + sDbgFrag
                + " cfg=" + cfgPath()
                + (sLevel <= 0 ? "  (关，不干预)" : ""));
        if (sLevel <= 0) {
            return;
        }
        // TODO 第 2 块：档 1~8 的 hook 注册

        // ============================================================
        // 先采用户开关：相机设置里的「智能构图」= u2.H / pref_smart_composition_key_<mode>
        // sUserOn 决定后续所有门是否抬：
        //   -1 未知（按开处理）
        //    0 用户关了 → 一律不抬、不合成、不驱动变焦 = 零干预
        //    1 用户开了 → 照常
        // ============================================================
        try {
            XposedHelpers.findAndHookMethod(C_COMP, sCl, "isSwitchOn", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                            try {
                                int mode = ((Integer) p.args[0]).intValue();
                                if (mode != MODE_PHOTO && mode != MODE_AI_HELP) {
                                    return;
                                }
                                boolean on = Boolean.TRUE.equals(p.getResult());
                                if (!on) {
                                    // false 有两种可能：存值就是 OFF，或被 f17374a 挡住。
                                    // 存值才是用户的意思，读出来区分。
                                    on = "ON".equals(String.valueOf(
                                            XposedHelpers.callMethod(p.thisObject,
                                                    "getComponentValue", p.args[0])));
                                }
                                setUserOn(on, "isSwitchOn(" + mode + ")");
                            } catch (Throwable th) {
                                err(th);
                            }
                        }
                    });
            log("hook u2.H.isSwitchOn ok（采开关）");
        } catch (Throwable th) {
            log("!! hook u2.H.isSwitchOn 失败 " + th);
        }

        // reInit 一结束就采开关。不能挂 before：R(C) 的参数字段在 dex 里
        // 是别的名字，取不到 mode。after 时 this.mCurrentMode 已经是本轮 mode。
        try {
            XposedHelpers.findAndHookMethod(C_COMP, sCl, "R", Object.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                            try {
                                int mode = XposedHelpers.getIntField(
                                        p.thisObject, "mCurrentMode");
                                Object v = XposedHelpers.callMethod(p.thisObject,
                                        "getComponentValue", Integer.valueOf(mode));
                                setUserOn("ON".equals(String.valueOf(v)),
                                        "R() 后 mode=" + mode + " 存值=" + v);
                            } catch (Throwable th) {
                                logN("swsamp", "采开关失败(R) " + th, 600);
                            }
                        }
                    });
            log("hook u2.H.R 后采开关 ok");
        } catch (Throwable th) {
            log("!! hook u2.H.R 后采开关 失败 " + th);
        }

        // ============================================================
        // ★ 点击被吞：FragmentAi.onClick 第一行 if(!f8958y0) return;
        //   f8958y0 = 「分析已结束」，只有 onAiEffectResult/onAiPoseResult
        //   回来时调 ft(...,true) 才置位。AI 结果不回来 → 恒 false →
        //   智能构图 / 效果推荐 / 姿势引导 三个按钮全被吞。
        //   ① ft(str,false) 一律跳过 —— 不许它把自己按回「分析中」
        //   ② UI 起来后补一次 ft(str,true) —— 把开机就卡住的状态拉起来
        //   ft() 本体只有一行 log + 一个字段赋值，跳过/多调一次都安全。
        // ============================================================
        final boolean[] aiFtOk = {false};
        try {
            XposedHelpers.findAndHookMethod(C_AI_FRAG, sCl, "ft",
                    String.class, boolean.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                            try {
                                if (!Boolean.TRUE.equals(p.args[1])) {
                                    p.setResult(null);   // void：跳过原方法体
                                    logN("aiunstick", "FragmentAi.ft(" + p.args[0]
                                            + ", false) 已跳过 → 不再卡 onClick", 120);
                                }
                            } catch (Throwable th) {
                                err(th);
                            }
                        }
                    });
            aiFtOk[0] = true;
            log("hook FragmentAi.ft ok（分析标志不会被按回 false）");
        } catch (Throwable th) {
            log("!! hook FragmentAi.ft 失败 " + th);
        }
        if (aiFtOk[0]) {
            // getFragmentId() 是 FragmentAi 自己 final 实现的（返回 3821），
            // 在 onResume 之后补一次 ft(...,true)，把开机就卡住的分析标志拉起来
            try {
                XposedHelpers.findAndHookMethod(C_AI_FRAG, sCl, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    XposedHelpers.callMethod(p.thisObject, "ft",
                                            String.valueOf(p.thisObject), true);
                                    logN("aiunstick", "onResume 后补 ft(...,true)", 300);
                                } catch (Throwable th) {
                                    logN("aiunstick", "onResume 补 ft 失败 " + th, 600);
                                }
                            }
                        });
                log("hook FragmentAi.onResume ok（补 ft true）");
            } catch (Throwable th) {
                log("!! hook FragmentAi.onResume 失败 " + th);
            }
        }

        // ============================================================
        // 档 1：抬能力门 U3  +  挡 submitAiComposition
        // ============================================================
        if (sLevel >= 1) {
            try {
                XposedHelpers.findAndHookMethod(C_H, sCl, "U3", C_G, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                            if (!gateOpen()) {
                                return;
                            }
                            Object orig = p.getResult();
                            p.setResult(Boolean.TRUE);
                            budget("U3 " + orig + " -> true  (抬 autoCropVersion 门)");
                        } catch (Throwable th) {
                            err(th);
                        }
                    }
                });
                log("hook l9.h.U3 ok");
            } catch (Throwable th) {
                log("!! hook l9.h.U3 失败 " + th);
            }

            // 构图请求闸门：A3.g.w() == submitAiComposition
            // 只被 x()(submitAiPose)/y()(submitAiTuning) 内部调用，无外部调用者。
            // U3 抬门后开关会锁住 → x.t0(mode) 变 true → w() 被触发，
            // 而 AI 分析不回来时 onClick 会被吞 → 退化成另一种「点了没反应」。
            if (sSubmit <= 0) {
                try {
                    XposedHelpers.findAndHookMethod(C_SUBMIT, sCl, "w", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                            try {
                                if (!gateOpen()) {
                                    return;
                                }
                                p.setResult(null);   // void：跳过原方法体
                                if (!sSubmitLogDone) {
                                    sSubmitLogDone = true;
                                    log("A3.g.w() 拦截 → submitAiComposition 不下发"
                                            + " (ai.smartcomp.submit=0，后续静音)");
                                }
                            } catch (Throwable th) {
                                err(th);
                            }
                        }
                    });
                    log("hook A3.g.w ok (submitAiComposition 已挡)");
                } catch (Throwable th) {
                    log("!! hook A3.g.w 失败 " + th);
                }
            } else {
                log("submitAiComposition 放行 (ai.smartcomp.submit=1)");
            }
        }

        // 档 2：比例门 t0 返回 原结果 ∪ 常见比例
        if (sLevel >= 2) {
            try {
                XposedHelpers.findAndHookMethod(C_H, sCl, "t0", C_G, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                            if (!gateOpen()) {
                                return;
                            }
                            List<String> out = new ArrayList<String>();
                            Object orig = p.getResult();
                            if (orig instanceof List) {
                                for (Object o : (List) orig) {
                                    if (o != null) {
                                        out.add(String.valueOf(o));
                                    }
                                }
                            }
                            StringBuilder added = new StringBuilder();
                            for (String ratio : RATIOS) {
                                if (!out.contains(ratio)) {
                                    out.add(ratio);
                                    if (added.length() > 0) {
                                        added.append(',');
                                    }
                                    added.append(ratio);
                                }
                            }
                            p.setResult(out);
                            budget("t0 " + out + "  (补入: " + added + ")");
                        } catch (Throwable th) {
                            err(th);
                        }
                    }
                });
                log("hook l9.h.t0 ok");
            } catch (Throwable th) {
                log("!! hook l9.h.t0 失败 " + th);
            }
        }

        // 档 3：下发门 —— 让 applySmartCompositionEnable 的 isTagDefined 变 true
        if (sLevel >= 3) {
            try {
                XposedHelpers.findAndHookMethod(C_G, sCl, "S0", String.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    if (!gateOpen()) {
                                        return;
                                    }
                                    Object arg = p.args[0];
                                    if (!(arg instanceof String)
                                            || !TAG_ENABLE.equals(arg)) {
                                        return;   // 只放过这一个 tag
                                    }
                                    Object orig = p.getResult();
                                    p.setResult(Boolean.TRUE);
                                    budget("S0 " + TAG_ENABLE + " " + orig
                                            + " -> true  (放行下发)");
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                log("hook l9.g.S0 ok (只认 autoCropEnable)");
            } catch (Throwable th) {
                log("!! hook l9.g.S0 失败 " + th);
            }
        }

        // 档 4：兜底门 —— reInit 结束后强制 f17374a = true（绕过 intent type / facing）
        if (sLevel >= 4) {
            try {
                // dex 真签名是 R(Ljava/lang/Object;) -> V
                XposedHelpers.findAndHookMethod(C_COMP, sCl, "R", Object.class,
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    boolean before = readSupport(p.thisObject);
                                    writeSupport(p.thisObject, true);
                                    boolean after = readSupport(p.thisObject);
                                    budget("R() 后 f17374a " + before + " -> " + after);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                log("hook u2.H.R ok (强制 f17374a=true)");
            } catch (Throwable th) {
                log("!! hook u2.H.R 失败 " + th);
            }
        }

        // ============================================================
        // 档 5~7：抬 V3 走 v1（ASD 代）
        // v1 与 v2 的分叉点：
        //   appendInterceptor:  if(!V3){ if(U3) add(v2 s6.s0); return; }
        //                       add(v1 s6.r0)
        //   updateSmartComposition: if(!U3){ if(V3) { v1 分支 } return; }
        //   → 只抬 V3、不抬 U3，两边一致走 v1，不会错配。
        // ============================================================
        if (sLevel >= 5 && sLevel <= 7) {
            try {
                XposedHelpers.findAndHookMethod(C_H, sCl, "V3", C_G, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                        try {
                            if (!gateOpen()) {
                                return;
                            }
                            Object orig = p.getResult();
                            p.setResult(Boolean.TRUE);
                            logN("V3", "V3 " + orig
                                    + " -> true  (抬 asd.aiComposition → 走 v1)", 0);
                        } catch (Throwable th) {
                            err(th);
                        }
                    }
                });
                log("hook l9.h.V3 ok (只抬 V3，U3 保持 false → v1)");
            } catch (Throwable th) {
                log("!! hook l9.h.V3 失败 " + th);
            }

            // ---- v1 通路诊断探针（不改结果）----
            try {
                XposedHelpers.findAndHookMethod(C_DATA_F, sCl, "d0",
                        int.class, C_G, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    Object[] a = p.args;
                                    logN("Fd0", "F.d0(mode=" + a[0]
                                            + ", caps=" + (a[1] != null) + ") = "
                                            + p.getResult(), 0);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                log("probe data.data.F.d0 ok");
            } catch (Throwable th) {
                log("!! probe F.d0 失败 " + th);
            }

            try {
                XposedHelpers.findAndHookMethod(C_R0_V1, sCl,
                        "initAndGetPriorCondition", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    logN("r0init", "r0.initAndGetPriorCondition = "
                                            + p.getResult(), 0);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                XposedHelpers.findAndHookMethod(C_R0_V1, sCl,
                        "getInTimeCondition", new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    logN("r0in", "r0.getInTimeCondition = "
                                            + p.getResult(), 0);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                XposedHelpers.findAndHookMethod(C_R0_V1, sCl,
                        "acceptResult", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    logN("r0acc", "★ r0.acceptResult 进入", 50);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                log("probe s6.r0 ok (initAndGetPriorCondition/getInTimeCondition/acceptResult)");
            } catch (Throwable th) {
                log("!! probe s6.r0 失败 " + th);
            }

            try {
                XposedHelpers.findAndHookMethod(C_CAPMOD, sCl,
                        "updateSmartComposition", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam p) throws Throwable {
                                try {
                                    logN("usc", "CaptureModule.updateSmartComposition() 进入", 0);
                                } catch (Throwable th) {
                                    err(th);
                                }
                            }
                        });
                log("probe CaptureModule.updateSmartComposition ok");
            } catch (Throwable th) {
                log("!! probe updateSmartComposition 失败 " + th);
            }

            // ============================================================
            // 档 6：v1 剩下两块
            //  ① F.w(mode) = "ai".equals(w2.a.getComponentValue(mode))
            //     w2.a.getKey() 返回 null → 取内存 map 的 null 键 → 从没写过
            //     就恒等于缺省 "off" → i5.g.M() 恒 false →
            //     getInTimeCondition 恒 false → acceptResult 永远不跑。
            //     ★ 只在 F.w 这一层改判，绝不改 w2.a 的存值 ★
            //       10-03 实测：直接改 getComponentValue 会让 w2.a.m() 走进
            //       AI 分支遍历未加载的图案列表 → NPE → 相机每开必闪退。
            //  ② isSwitchOn 出口按存值改判（f17374a 被 U3 门写死 false）
            //     ★ 只改出口，绝不抬 f17374a 字段 ★
            //       m() = isReconfigStreamCompositionInit() 也读同一字段，
            //       直接抬会把换流判断带翻。
            // ============================================================
            if (sLevel >= 6) {
                try {
                    XposedHelpers.findAndHookMethod(C_DATA_F, sCl, "w", int.class,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                    try {
                                        int mode = ((Integer) p.args[0]).intValue();
                                        boolean orig = Boolean.TRUE.equals(p.getResult());
                                        if (!orig && (mode == MODE_PHOTO
                                                || mode == MODE_AI_HELP)) {
                                            p.setResult(Boolean.TRUE);
                                            logN("FwFix", "F.w(" + mode
                                                    + ") false -> true（补判 AI 构图，不动 w2.a 存值）", 0);
                                        } else {
                                            logN("Fw", "F.w(" + mode + ") = " + orig, 120);
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook F.w ok (163/168 补判 true)");
                } catch (Throwable th) {
                    log("!! hook F.w 失败 " + th);
                }

                try {
                    XposedHelpers.findAndHookMethod(C_COMP, sCl, "isSwitchOn", int.class,
                            new XC_MethodHook() {
                                final Method[] gm = {null};

                                @Override
                                protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                                    try {
                                        if (Boolean.TRUE.equals(p.getResult())) {
                                            return;
                                        }
                                        int mode = ((Integer) p.args[0]).intValue();
                                        if (mode != MODE_PHOTO && mode != MODE_AI_HELP) {
                                            return;
                                        }
                                        Object self = p.thisObject;
                                        if (gm[0] == null) {
                                            gm[0] = self.getClass()
                                                    .getMethod("getComponentValue", int.class);
                                            gm[0].setAccessible(true);
                                        }
                                        Object v = gm[0].invoke(self, Integer.valueOf(mode));
                                        if ("ON".equals(v)) {
                                            p.setResult(Boolean.TRUE);
                                            logN("swFix", "u2.H.isSwitchOn(" + mode
                                                    + ") false -> true（f17374a 挡住存值 ON，"
                                                    + "只改判出口，未抬字段）", 20);
                                        } else {
                                            logN("sw", "u2.H.isSwitchOn(" + mode
                                                    + ") = false, 存值=" + v, 600);
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook u2.H.isSwitchOn ok (f17374a 门只在出口绕过)");
                } catch (Throwable th) {
                    log("!! hook u2.H.isSwitchOn 出口改判 失败 " + th);
                }
            }
            // ============================================================
            // 档 7：抬齐 v1 剩下 3 道门 + 合成数据后门
            // v1 通路共 5 道门，档 5/6 只解决了 2 道：
            //  ① l9.g.S0(SemanticScene) —— 最关键，档5/6 漏了它
            //     prepare() 里 z9 = name.startsWith("android") || M4(caps,name)，
            //     M4(g,s) = g.S0(s)。本机没注册 xiaomi.ai.misd.SemanticScene
            //     → z9=false → supportPrior=false → prepare() 返回 false
            //     → 拦截器根本不注册 → acceptResult 永远不会跑
            //  ② F.d0(mode, caps) —— 163/168 直接 true
            //     原式 = !g.N() && mode==163 && caps!=null && V3(caps)
            //     抬了 V3 之后 g.N() 仍可能挡路，这里按模式兜死
            //  ③ i5.g.M() —— 原 false 就改 true（nn() 兜底）
            //     M() = F.w && !L0.nn()，是 getInTimeCondition 的后半段
            //  ④ 合成总闸 r0.f17135a（dex 真名 a）
            //     在 interceptor.base.i.onCaptureResultNext 的 before 里
            //     对 s6.r0 直接 setBooleanField("a", true)
            //     ★ 不走 Kr.g.c ★ 实测栈上只有 Xposed 混淆帧和相机自己的
            //       AOP 责任链，拿不到业务类
            //     ★ 也绝不全局 setprop ★ 那会连带打开 fragment 的 debug
            //       TextView → layout 没那控件就 NPE 闪退（10-03 已踩过）
            //  ⑤ Kr.g.a("debug_composition_index") → 返回 ai.smartcomp.dbgindex
            //     r0.acceptResult 拿不到真实 ASD 数据时用它造一份
            // ============================================================
            if (sLevel >= 7) {

                // ---- ② tag 支持门：S0(name) 对 SemanticScene 放行 ----
                try {
                    XposedHelpers.findAndHookMethod(C_G, sCl, "S0", String.class,
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam p)
                                        throws Throwable {
                                    try {
                                        String n = (String) p.args[0];
                                        if (n != null && n.contains("SemanticScene")) {
                                            p.setResult(Boolean.TRUE);
                                            logN("S0v1", "l9.g.S0(" + n
                                                    + ") -> true（抬 v1 tag 支持门，"
                                                    + "prepare 才会注册拦截器）", 0);
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook l9.g.S0 ok (SemanticScene 支持门)");
                } catch (Throwable th) {
                    log("!! hook l9.g.S0 失败 " + th);
                }

                // ---- ① F.d0：r0.initAndGetPriorCondition 的门（按模式直判）----
                try {
                    XposedHelpers.findAndHookMethod(C_DATA_F, sCl, "d0",
                            int.class, C_G, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam p)
                                        throws Throwable {
                                    try {
                                        int mode = ((Integer) p.args[0]).intValue();
                                        if (mode == MODE_PHOTO || mode == MODE_AI_HELP) {
                                            if (!Boolean.TRUE.equals(p.getResult())) {
                                                p.setResult(Boolean.TRUE);
                                                logN("Fd0fix", "F.d0(" + mode
                                                        + ") false -> true（进 v1 拦截器）", 0);
                                            }
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook F.d0 ok (163/168 进 v1)");
                } catch (Throwable th) {
                    log("!! hook F.d0 失败 " + th);
                }

                // ---- ③ i5.g.M()：getInTimeCondition 的后半段（nn() 兜底）----
                try {
                    XposedHelpers.findAndHookMethod(C_MGR, sCl, "M", new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam p) throws Throwable {
                            try {
                                if (sMgr == null && p.thisObject != null) {
                                    sMgr = p.thisObject;
                                    log("缓存 i5.g 实例（供图案懒加载 Pn() 用）");
                                }
                                if (!Boolean.TRUE.equals(p.getResult())) {
                                    p.setResult(Boolean.TRUE);
                                    logN("mgrMfix", "i5.g.M() false -> true"
                                            + "（F.w 已抬，nn() 挡路时兜底）", 0);
                                }
                            } catch (Throwable th) {
                                err(th);
                            }
                        }
                    });
                    log("hook i5.g.M ok (兜底 getInTimeCondition)");
                } catch (Throwable th) {
                    log("!! hook i5.g.M 失败 " + th);
                }

                // ---- ④ 合成数据总闸：直接写 r0.f17135a（dex 真名 a）----
                // CaptureModule 装配完 r0 之后就没人再改这个字段，
                // 所以在拦截器每帧入口 onCaptureResultNext 的 before 里
                // 直接写 true —— 方法体里 z8 = this.a 在 acceptResult
                // 一开始就读，那时已经是 true，合成分支必走。
                try {
                    XposedHelpers.findAndHookMethod(C_ITR_I, sCl,
                            "onCaptureResultNext", CaptureResult.class,
                            new XC_MethodHook() {
                                final Field[] fA = {null};

                                @Override
                                protected void beforeHookedMethod(MethodHookParam p)
                                        throws Throwable {
                                    try {
                                        Object self = p.thisObject;
                                        if (self == null
                                                || !C_R0_V1.equals(
                                                        self.getClass().getName())) {
                                            return;
                                        }
                                        if (fA[0] == null) {
                                            Field f = self.getClass()
                                                    .getDeclaredField(F_SUPPORT);
                                            f.setAccessible(true);
                                            fA[0] = f;
                                        }
                                        if (!fA[0].getBoolean(self)) {
                                            fA[0].setBoolean(self, true);
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook r0 总闸 ok (interceptor.base.i.onCaptureResultNext)");
                } catch (Throwable th) {
                    log("!! hook r0 总闸 失败 " + th);
                }

                // ---- 诊断：Kr.g.c(debug_composition_enable) 的真实调用者 ----
                try {
                    XposedHelpers.findAndHookMethod(C_KR_G, sCl, "c",
                            String.class, boolean.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam p)
                                        throws Throwable {
                                    try {
                                        String k = (String) p.args[0];
                                        if (!"debug_composition_enable".equals(k)) {
                                            return;
                                        }
                                        String caller = dbgCaller();
                                        logN("dbgGate", "Kr.g.c(debug_composition_enable, "
                                                + p.args[1] + ") caller=" + caller, 60);
                                        // ai.smartcomp.dbgfrag=1 时才顺带放开 fragment 那处
                                        // 的 debug TextView（默认 0，有 NPE 风险）
                                        if (sDbgFrag == 1
                                                && caller.contains("smartComposition")
                                                && !Boolean.TRUE.equals(p.getResult())) {
                                            p.setResult(Boolean.TRUE);
                                        }
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("probe Kr.g.c ok (debug_composition_enable 调用者)");
                } catch (Throwable th) {
                    log("!! probe Kr.g.c 失败 " + th);
                }

                // ---- ⑤ 属性后门：debug_composition_index（合成数据）----
                try {
                    XposedHelpers.findAndHookMethod(C_KR_G, sCl, "a", String.class,
                            new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam p)
                                        throws Throwable {
                                    try {
                                        String k = (String) p.args[0];
                                        if (!"debug_composition_index".equals(k)) {
                                            return;
                                        }
                                        if (sCycle == 1) {
                                            sDbgIndex = nextPatIndex();
                                        }
                                        p.setResult(String.valueOf(sDbgIndex));
                                        logN("dbgIdx", "Kr.g.a(debug_composition_index) <- "
                                                + sDbgIndex + "  合成 r.a{a=11, b="
                                                + sDbgIndex + "}", 600);
                                    } catch (Throwable th) {
                                        err(th);
                                    }
                                }
                            });
                    log("hook Kr.g.a ok (debug_composition_index=" + sDbgIndex + ")");
                } catch (Throwable th) {
                    log("!! hook Kr.g.a 失败 " + th);
                }
            }
        }
    }

    // ==================================================================
    // 工具方法
    // ==================================================================
    /** 只允许在冷路径调用：抓栈很贵 */
    private static String dbgCaller() {
        try {
            StackTraceElement[] st = new Throwable().getStackTrace();
            for (StackTraceElement e : st) {
                String cn = e.getClassName();
                if (cn.startsWith("com.starlight") || cn.startsWith("de.robv")
                        || cn.startsWith("com.pudding")) {
                    continue;
                }
                return "[" + cn + "." + e.getMethodName() + "]";
            }
        } catch (Throwable ignore) {
        }
        return "[?]";
    }

    /** 读 w2.a 的 AI 图案列表（字段名踩过被改名的坑，找不到就按类型找） */
    private static Object readPatternList(Object sel) {
        if (sel == null) {
            return null;
        }
        try {
            return XposedHelpers.getObjectField(sel, "f18198a");
        } catch (Throwable ignore) {
            // 落到下面按类型找
        }
        try {
            for (Field f : sel.getClass().getDeclaredFields()) {
                if (f.getType() == ArrayList.class
                        && !java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                    f.setAccessible(true);
                    return f.get(sel);
                }
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static Object readListField(Object obj, String... names) {
        if (obj == null) {
            return null;
        }
        for (String n : names) {
            try {
                return XposedHelpers.getObjectField(obj, n);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }

    /**
     * 图案懒加载（w2.a.m() 列表空时调，最多 6 次）。三级：
     *   ① 官方：i5.g.Pn() —— if (d 空) { d.a(); w2.a.r(d.b, d.c); }
     *   ② DI：  g2.a.j().x(i5.g.class) 拿不到实例就跳过
     *   ③ 兜底：自己 new i5.d() 调 a() 解析 JSON → w2.a.r(b, c)
     */
    private static void ensurePatternLoaded(Object sel, int attempt) {
        try {
            Object mgr = sMgr;
            if (mgr == null) {
                try {
                    Object container = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("g2.a", sCl), "j");
                    mgr = XposedHelpers.callMethod(container, "x",
                            XposedHelpers.findClass(C_MGR, sCl));
                    if (mgr != null) {
                        sMgr = mgr;
                        log("懒加载#" + attempt + " DI 拿到 i5.g 实例");
                    }
                } catch (Throwable t2) {
                    log("懒加载#" + attempt + " DI 取 i5.g 失败: " + t2);
                }
            }
            if (mgr != null) {
                try {
                    XposedHelpers.callMethod(mgr, "Pn");
                    log("懒加载#" + attempt + " 调了 i5.g.Pn()");
                } catch (Throwable t) {
                    log("懒加载#" + attempt + " i5.g.Pn() 调用失败: " + t);
                }
            } else {
                log("懒加载#" + attempt + " i5.g 实例还没拿到，走③兜底自行解析 JSON");
            }

            Object list = readPatternList(sel);
            if (list != null && !((Collection) list).isEmpty()) {
                log("★ 图案懒加载成功（①/②路径）：" + ((Collection) list).size() + " 条");
                return;
            }

            if (attempt > 3) {
                log("懒加载#" + attempt + " 兜底已试满 3 次，不再重试");
                return;
            }
            try {
                Object d = XposedHelpers.findClass("i5.d", sCl)
                        .getDeclaredConstructor().newInstance();
                XposedHelpers.callMethod(d, "a");
                Object b = readListField(d, "b");
                Object c = readListField(d, "c", "f13213c");
                if (b == null || c == null) {
                    log("懒加载#" + attempt + " 兜底：i5.d 字段没读到 b=" + b + " c=" + c);
                    return;
                }
                XposedHelpers.callMethod(sel, "r", b, c);
                Object after = readPatternList(sel);
                log("★ 图案懒加载成功（③兜底自行解析 JSON）：AI图案="
                        + ((Collection) b).size() + " 条, 创意项="
                        + ((Collection) c).size() + " 条 → w2.a 里="
                        + (after == null ? "null" : ((Collection) after).size()) + " 条");
            } catch (Throwable t3) {
                log("懒加载#" + attempt + " 兜底解析失败: " + t3);
            }
        } catch (Throwable th) {
            err(th);
        }
    }

    /** 下一个 AI 模板索引（1..48 轮转） */
    private static int nextPatIndex() {
        int n = sDbgIndex + 1;
        if (n > 48) {
            n = 1;
        }
        return n;
    }
    private static void setUserOn(boolean on, String why) {
        int v = on ? 1 : 0;
        if (sUserOn != v) {
            sUserOn = v;
            sUserOnWhy = why;
            log("★ 用户开关 = " + (on ? "开" : "关") + "  (" + why + ")");
        } else {
            sUserOnWhy = why;
        }
    }

    private static boolean gateOpen() {
        return sUserOn != 0;
    }

    private static final long[] sBudgetT = {0L};

    private static synchronized void budget(String msg) {
        long now = System.currentTimeMillis();
        if (now - sBudgetT[0] < 800) {
            return;
        }
        sBudgetT[0] = now;
        log(msg);
    }

    private static boolean readSupport(Object o) {
        try {
            return XposedHelpers.getBooleanField(o, F_SUPPORT);
        } catch (Throwable th) {
            try {
                Field f = o.getClass().getDeclaredField(F_SUPPORT);
                f.setAccessible(true);
                return f.getBoolean(o);
            } catch (Throwable ignore) {
                return false;
            }
        }
    }

    private static void writeSupport(Object o, boolean v) {
        try {
            XposedHelpers.setBooleanField(o, F_SUPPORT, v);
        } catch (Throwable th) {
            try {
                Field f = o.getClass().getDeclaredField(F_SUPPORT);
                f.setAccessible(true);
                f.setBoolean(o, v);
            } catch (Throwable ignore) {
            }
        }
    }
}
