package com.zhzx.studytime;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 检查更新：读 GitHub Releases 上的 update.json（由 CI 生成）。
 * 正式版 = releases/latest（只有转正才动）；测试版 = 预发布 Release `ci`（每次推分支 CI 覆盖）。
 * 找到新版 → 弹窗 → 浏览器下载 APK（系统安装器覆盖安装，数据保留）。
 */
final class Update {
    private Update() {}

    static final String REPO = "zhzx2026/studytime";

    static String url(int channel) {
        return channel == 1
                ? "https://github.com/" + REPO + "/releases/download/ci/update.json"
                : "https://github.com/" + REPO + "/releases/latest/download/update.json";
    }

    static String versionName(Context c) {
        try { return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName; }
        catch (Exception e) { return "?"; }
    }

    @SuppressWarnings("deprecation")
    static long versionCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return Build.VERSION.SDK_INT >= 28 ? pi.getLongVersionCode() : pi.versionCode;
        } catch (Exception e) { return 0; }
    }

    /** 装的是 dev 包（X.Y，Y≥1）就默认盯测试通道。 */
    static int defaultChannel(Context c) {
        String v = versionName(c);
        return v != null && v.matches("\\d+\\.0") ? 0 : 1;
    }

    /** 每天最多静默查一次，只在有新版时弹窗。 */
    static void autoCheck(Activity a) {
        Store s = Store.get(a);
        int today = Day.today();
        if (s.getInt("up_day", 0) == today) return;
        s.putInt("up_day", today);
        check(a, true);
    }

    static void check(final Activity a, final boolean quiet) {
        final int ch = Store.get(a).getInt(Store.K_CHANNEL, defaultChannel(a));
        final long mine = versionCode(a);
        if (!quiet) Ui.toast(a, "正在检查更新…");
        new Thread(() -> {
            String err = null;
            JSONObject o = null;
            try { o = new JSONObject(get(url(ch))); }
            catch (Exception e) { err = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : "：" + e.getMessage()); }
            final JSONObject res = o;
            final String error = err;
            a.runOnUiThread(() -> {
                if (a.isFinishing()) return;
                if (res == null) {
                    if (!quiet) Ui.dialog(a).setTitle("检查更新失败")
                            .setMessage((ch == 1 ? "测试通道" : "正式通道") + "暂时连不上 GitHub。\n" + error)
                            .setPositiveButton("好", null).show();
                    return;
                }
                long code = res.optLong("versionCode");
                String name = res.optString("versionName");
                final String apk = res.optString("apk");
                if (code <= mine || apk.isEmpty()) {
                    if (!quiet) Ui.toast(a, "已是最新版本（v" + versionName(a) + "）");
                    return;
                }
                Ui.dialog(a).setTitle("发现新版本 v" + name)
                        .setMessage(res.optString("notes", ""))
                        .setPositiveButton("下载安装", (d, w) -> {
                            try { a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(apk))); }
                            catch (RuntimeException e) { Ui.toast(a, "没有可用的浏览器"); }
                        })
                        .setNegativeButton("以后再说", null).show();
            });
        }).start();
    }

    /** GET（手动跟随跨域 302：github.com → objects.githubusercontent.com） */
    static String get(String u) throws Exception {
        for (int hop = 0; hop < 6; hop++) {
            HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
            c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(10000);
            c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "StudyTime-Android");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) throw new IllegalStateException("重定向缺 Location");
                u = new URL(new URL(u), loc).toString();
                continue;
            }
            if (code == 404) throw new IllegalStateException("该通道还没有发布过版本（404）");
            if (code != 200) throw new IllegalStateException("HTTP " + code);
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                return bo.toString("UTF-8");
            } finally {
                c.disconnect();
            }
        }
        throw new IllegalStateException("重定向次数过多");
    }
}
