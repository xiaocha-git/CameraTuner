package com.starlight.cameratuner;

/**
 * 模块启用状态 —— 由 Xposed 注入到本 App 进程时写入，App UI 读取。
 *
 * 原理：LSPosed 的「始终在作用域内」会把模块加载到自己的 App 进程。
 * 因此只要能走到 MainHook.handleLoadPackage 且 packageName 是自己的包名，
 * 就证明「框架已启用本模块」。
 *
 * 因为注入后与本 App 处于同一进程、同一 ClassLoader，
 * 直接写静态字段即可，无需文件或 IPC。
 */
public final class ModuleState {

    /** 框架已启用本模块（由 Xposed 侧写入） */
    public static volatile boolean frameworkActive = false;

    /** 最近一次被框架加载的时间戳 */
    public static volatile long lastInjectMs = 0L;

    /** 注入时拿到的 App 进程 pid */
    public static volatile int appPid = 0;

    private ModuleState() {
    }

    /** 由 MainHook 在检测到自身包名时调用 */
    public static void onInjectedIntoApp() {
        appPid = android.os.Process.myPid();
        lastInjectMs = System.currentTimeMillis();
        frameworkActive = true;
    }

    /**
     * 框架是否真的启用了模块。
     * 只有近期（10 秒内）被注入过才算，避免进程复用时读到陈旧值。
     */
    public static boolean isFrameworkActive() {
        if (!frameworkActive) {
            return false;
        }
        // 模块注入发生在 App 启动早期，距现在应该很短
        return System.currentTimeMillis() - lastInjectMs < 10_000L;
    }
}
