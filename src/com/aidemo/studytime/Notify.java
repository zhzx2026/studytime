package com.aidemo.studytime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** 通知：到点提醒（HIGH 渠道，自带响铃振动）+ 计时中的常驻倒计时（LOW 渠道，无声）。 */
public final class Notify {
    private Notify() {}

    public static final String CH_DONE = "pomo_done", CH_RUN = "pomo_running";
    private static final int ID_RUN = 1, ID_DONE = 2;

    /** 建渠道（App.onCreate 调一次；receiver 复用同一进程） */
    public static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_DONE, "番茄钟到点提醒",
                NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("专注 / 休息结束时提醒");
        a.enableVibration(true);
        a.setVibrationPattern(new long[]{0, 300, 200, 300});
        nm.createNotificationChannel(a);
        NotificationChannel b = new NotificationChannel(CH_RUN, "计时中",
                NotificationManager.IMPORTANCE_LOW);
        b.setDescription("番茄钟运行时的常驻倒计时");
        b.setShowBadge(false);
        b.setSound(null, null);
        b.enableVibration(false);
        nm.createNotificationChannel(b);
    }

    /** 13+ 通知要运行时授权；没授权时通知会被系统静默丢弃（调用方用声音兜底） */
    public static boolean granted(Context c) {
        if (Build.VERSION.SDK_INT < 33) return true;
        return c.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static PendingIntent open(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String taskLine(Cfg cfg) {
        return String.format(java.util.Locale.CHINA, "专注 %d · 短休 %d · 长休 %d",
                cfg.workMin, cfg.shortMin, cfg.longMin);
    }

    /** 常驻倒计时通知：running 用系统 chronometer（锁屏状态栏自己走表），暂停显示剩余，空闲撤掉。 */
    public static void running(Context c, Pomo p, Cfg cfg, Todo todo, int bindId) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        try {
            if (p.state == Pomo.IDLE) { nm.cancel(ID_RUN); return; }
            Notification.Builder b = new Notification.Builder(c, CH_RUN)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentText(taskLine(cfg))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(open(c));
            if (p.running) {
                String phase = p.state == Pomo.WORK ? "专注" : p.state == Pomo.LONG ? "长休息" : "短休息";
                b.setContentTitle("🍅 " + phase + "中")
                        .setWhen(p.endAt).setShowWhen(true)
                        .setUsesChronometer(true).setChronometerCountDown(true);
            } else {
                String phase = p.state == Pomo.WORK ? "专注" : p.state == Pomo.LONG ? "长休息" : "短休息";
                b.setContentTitle("⏸ " + phase + "已暂停 · 剩余 "
                                + MainActivity.mmss(p.remainingAt(System.currentTimeMillis())))
                        .setShowWhen(false);
            }
            nm.notify(ID_RUN, b.build());
        } catch (RuntimeException ignored) {
            // 无权限 / ROM 限制：计时本身不受影响
        }
    }

    /** 到点提醒（后台/前台通用；通知自带响铃振动） */
    public static void done(Context c, boolean wasWork, Pomo p) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        String title = wasWork ? "🍅 番茄完成！记上一笔" : "⏰ 休息结束";
        String text = p.running
                ? (p.state == Pomo.WORK ? "下一轮专注已经就位，点开看看" : "休息进行中，点开查看进度")
                : "点开继续";
        try {
            Notification n = new Notification.Builder(c, CH_DONE)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .setContentIntent(open(c))
                    .build();
            nm.notify(ID_DONE, n);
        } catch (RuntimeException ignored) {
        }
    }

    public static void cancelDone(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(ID_DONE);
    }

    /** 没有通知权限时的兜底提醒（声音+振动，receiver 里用） */
    public static void fallbackDing(Context c, Cfg cfg) {
        SoundFx.ding(c, cfg.sound, cfg.vibrate);
    }
}
