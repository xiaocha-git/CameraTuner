package com.starlight.cameratuner;

import android.util.Log;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * 动态照片 × 超级长焦 共存修复。
 *
 * 【背景：完整的互斥链】
 *   1) CameraAppImpl 里注册静态互斥表（v3/d）：
 *        v3/d(0x302, "超级长焦", [动态照片 on->false, 动态照片 off->true])
 *   2) 开启超级长焦后，u3/l.n() 走 processTemporaryMutex
 *   3) 0x302 映射成位掩码 0x8
 *   4) 调用 r2/C.o(0x8, true)  —— trySetClose
 *   5) o() 内部：
 *        mMutexState |= 0x8
 *        a (mClose)   = (mMutexState != 0) = true
 *        b (mDisable) = 同上 = true
 *   6) isSwitchOn() 检查 mClose -> 返回 false（状态显示为关）
 *      X9/e3.invoke() 检查 mDisable -> 按钮走禁用分支（视觉变灰）
 *   7) 关闭超级长焦后 o(0x8, false) 清位，自动恢复
 *
 * 【第一版的问题】
 *   只 hook 了 isSwitchOn()，让状态返回 true。
 *   日志确认调用成功（mode=0xa3 反复放行），但按钮仍然变灰。
 *   原因：UI 层（X9/e3）读的是字段 b (mDisable)，不是 isSwitchOn 的返回值。
 *
 * 【本版做法】
 *   直接 hook r2/C.o(IZ)Z，拦截 mutexType == 0x8 的置位请求：
 *   把 set 参数改成 false，让这一位永远不被点亮。
 *   效果：mClose / mDisable 都不会被超级长焦置位，
 *        isSwitchOn 与 UI 使能都保持正常，无需再改返回值。
 *
 * 注意：
 *   本 hook 只解决「按钮可用」。两路流在 HAL 层是否冲突，
 *   代码层面无法判断，需要实拍验证。
 *
 * 配置：
 *   live.supertele=1   开启（默认 0 关闭）
 */
public class LiveSupTeleFix {

    private static final String TAG = "LiveSupTele";

    /** 超级长焦在互斥表里的 ID：0x302 */
    private static final int MUTEX_ID_SUPER_TELE = 0x302;

    /** 0x302 映射到的位掩码 */
    private static final int MUTEX_BIT_SUPER_TELE = 0x8;

    private MainHook host;

    public LiveSupTeleFix(MainHook host) {
        this.host = host;
    }

    private void logi(String msg) {
        host.logiTag("live", msg);
    }

    private void loge(String msg) {
        Log.e(TAG, msg);
        XposedBridge.log(TAG + ": " + msg);
    }

    /**
     * 安装 hook。
     *
     * @return 是否成功
     */
    public boolean install(ClassLoader cl) {
        try {
            Class<?> cls = XposedHelpers.findClassIfExists("r2.C", cl);
            if (cls == null) {
                loge("类 r2.C 不存在，跳过");
                return false;
            }

            // ── 主 hook：拦住 trySetClose 的置位 ──
            XposedHelpers.findAndHookMethod(cls, "o", int.class, boolean.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                int type = (Integer) param.args[0];
                                boolean set = (Boolean) param.args[1];

                                // 只关心「置位」操作，且只关心超级长焦那一位
                                if (!set) return;
                                if ((type & MUTEX_BIT_SUPER_TELE) == 0) return;

                                // 把这一位去掉，其余位保持原样
                                int newType = type & ~MUTEX_BIT_SUPER_TELE;
                                param.args[0] = newType;

                                logi("拦截超级长焦互斥：type=0x"
                                        + Integer.toHexString(type) + " -> 0x"
                                        + Integer.toHexString(newType));
                            } catch (Throwable t) {
                                loge("o() 处理异常: " + t);
                            }
                        }
                    });

            // ── 兜底 hook：万一 o() 不是唯一入口，isSwitchOn 也放行 ──
            XposedHelpers.findAndHookMethod(cls, "isSwitchOn", int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                if (Boolean.TRUE.equals(param.getResult())) return;

                                Object self = param.thisObject;
                                if (self == null) return;

                                int mode = param.args.length > 0
                                        && param.args[0] instanceof Integer
                                        ? (Integer) param.args[0] : -1;

                                String value = readComponentValue(self, mode);
                                if ("ON".equals(value)) {
                                    param.setResult(true);
                                    logi("兜底放行 isSwitchOn(mode=0x"
                                            + Integer.toHexString(mode) + ")");
                                }
                            } catch (Throwable t) {
                                loge("isSwitchOn 处理异常: " + t);
                            }
                        }
                    });

            logi("已 hook r2.C.o(IZ)Z —— 超级长焦不再置位动态照片互斥");
            return true;
        } catch (Throwable t) {
            loge("hook 失败: " + t);
            return false;
        }
    }

    /**
     * 调用 getComponentValue(int) 拿 pref 的真实值。
     * 注意：getComponentValue 本身不看 mClose，所以能拿到未受影响的原始值。
     */
    private String readComponentValue(Object obj, int mode) {
        try {
            Method m = findMethod(obj.getClass(), "getComponentValue", int.class);
            if (m == null) {
                return null;
            }
            m.setAccessible(true);
            Object r = m.invoke(obj, mode);
            return r instanceof String ? (String) r : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Method findMethod(Class<?> cls, String name, Class<?>... types) {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try {
                return c.getDeclaredMethod(name, types);
            } catch (Throwable ignore) {
                c = c.getSuperclass();
            }
        }
        return null;
    }
}
