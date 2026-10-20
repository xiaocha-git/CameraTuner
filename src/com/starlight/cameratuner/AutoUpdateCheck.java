package com.starlight.cameratuner;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/**
 * 更新检查：
 * - 启动时静默请求 update.json
 * - 结果写入静态缓存，供状态页读取
 * - 不弹窗，由用户主动查看
 */
public class AutoUpdateCheck {

    private static final String UPDATE_URL = "https://update.xn--yet042g.cn/update.json";

    // 结果缓存（供 StatusPage 读取）
    public static volatile boolean checked = false;
    public static volatile boolean hasUpdate = false;
    public static volatile String latestVersion = null;
    public static volatile String latestNotes = null;
    public static volatile String latestUrl = null;

    private static boolean running = false;

    /** App 启动时调用 */
    public static void run(final MainActivity a) {
        if (running) return;
        running = true;
        new Thread(new Runnable() {
            public void run() {
                try {
                    String json = fetch();
                    if (json == null) {
                        checked = true;
                        return;
                    }
                    int remote = extractInt(json, "versionCode");
                    if (remote <= 0) {
                        checked = true;
                        return;
                    }
                    int local = getVersionCode(a);
                    if (remote > local) {
                        hasUpdate = true;
                        latestVersion = extractStr(json, "version");
                        latestNotes = extractStr(json, "notes");
                        latestUrl = extractStr(json, "url");
                    }
                    checked = true;

                    // 通知 UI 刷新（无论有无更新都刷新，让"已是最新版本"及时显示）
                    a.runOnUiThread(new Runnable() {
                        public void run() {
                            a.refreshCurrentPage();
                        }
                    });
                } catch (Throwable t) {
                    checked = true;
                } finally {
                    running = false;
                }
            }
        }).start();
    }

    /** 显示更新详情弹窗 */
    public static void showUpdateDialog(final MainActivity a) {
        String msg = "发现新版本 v" + (latestVersion == null ? "?" : latestVersion);
        if (latestNotes != null && !latestNotes.isEmpty()) {
            msg += "\n\n" + latestNotes;
        }
        TunerDialog.create(a)
                .title("有可用更新")
                .message(msg)
                .negative("稍后", null)
                .positive("立即更新", new TunerDialog.OnAction() {
                    public void onClick(TunerDialog d) {
                        if (latestUrl != null && !latestUrl.isEmpty()) {
                            try {
                                a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(latestUrl)));
                            } catch (Throwable t) {
                                Toast.makeText(a, "无法打开链接", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                })
                .show();
    }

    // ---------- 网络 ----------

    private static String fetch() {
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
            return sb.toString();
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) try { conn.disconnect(); } catch (Throwable ignored) {}
        }
    }

    private static int extractInt(String json, String key) {
        try {
            int i = json.indexOf("\"" + key + "\"");
            if (i < 0) return -1;
            int colon = json.indexOf(':', i);
            if (colon < 0) return -1;
            int end = colon + 1;
            while (end < json.length() && !Character.isDigit(json.charAt(end))
                    && json.charAt(end) != '-') end++;
            int start = end;
            while (end < json.length()
                    && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
            if (start >= end) return -1;
            return Integer.parseInt(json.substring(start, end));
        } catch (Throwable t) {
            return -1;
        }
    }

    private static String extractStr(String json, String key) {
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

    private static int getVersionCode(MainActivity a) {
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
}
