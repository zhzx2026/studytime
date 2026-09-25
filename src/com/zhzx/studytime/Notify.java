package com.zhzx.studytime;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** 通知 + 反馈（振动 / 提示音）。 */
final class Notify {
    private Notify() {}

    static final String CH_DONE = "pomo_done", CH_RUN = "pomo_running";
    private static final int ID_RUN = 1, ID_DONE = 2;

    static void channels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_DONE, "番茄钟到点提醒", NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("专注 / 休息结束时提醒");
        a.enableVibration(true);
        a.setVibrationPattern(new long[]{0, 300, 200, 300});
        nm.createNotificationChannel(a);
        NotificationChannel b = new NotificationChannel(CH_RUN, "计时中", NotificationManager.IMPORTANCE_LOW);
        b.setDescription("番茄钟运行时的常驻倒计时");
        b.setShowBadge(false);
        b.setSound(null, null);
        b.enableVibration(false);
        nm.createNotificationChannel(b);
    }

    private static PendingIntent open(Context c) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.putExtra(MainActivity.EXTRA_TAB, 0);
        return PendingIntent.getActivity(c, 0, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String taskLine(Context c, Pomo p) {
        if (p.taskId >= 0) {
            Store.Task t = Store.get(c).task(p.taskId);
            if (t != null) return "🎯 " + t.title;
        }
        return p.phase == Pomo.FOCUS ? "保持专注，手机放一边" : "起来走走，喝口水";
    }

    /** 运行中：常驻倒计时通知；暂停中：显示剩余；空闲：撤掉。 */
    static void running(Context c, Pomo p) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        try {
            if (!p.started) { nm.cancel(ID_RUN); return; }
            Notification.Builder b = new Notification.Builder(c, CH_RUN)
                    .setSmallIcon(R.drawable.ic_stat)
                    .setContentText(taskLine(c, p))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setContentIntent(open(c));
            if (p.running) {
                b.setContentTitle("🍅 " + Pomo.phaseName(p.phase) + "中")
                        .setWhen(p.endAt).setShowWhen(true)
                        .setUsesChronometer(true).setChronometerCountDown(true);
            } else {
                b.setContentTitle("⏸ " + Pomo.phaseName(p.phase) + "已暂停 · 剩余 " + Day.clock(p.remain))
                        .setShowWhen(false);
            }
            nm.notify(ID_RUN, b.build());
        } catch (RuntimeException ignored) {
            // 没有通知权限 / ROM 限制：计时本身不受影响
        }
    }

    static void done(Context c, Pomo.Done d, Pomo p) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        String title = d.phase == Pomo.FOCUS ? "🍅 番茄完成！专注了 " + d.minutes + " 分钟" : "⏰ 休息结束";
        String text = p.running
                ? "已自动开始" + Pomo.phaseName(p.phase)
                : "点开开始" + Pomo.phaseName(p.phase);
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

    static void cancelDone(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm != null) nm.cancel(ID_DONE);
    }

    /** 前台结算时的即时反馈（后台时由通知渠道负责响铃振动）。 */
    static void feedback(Context c) {
        Store s = Store.get(c);
        if (s.getBool(Store.K_VIBRATE, true)) {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                try {
                    v.vibrate(VibrationEffect.createWaveform(new long[]{0, 250, 150, 250}, -1));
                } catch (RuntimeException ignored) {}
            }
        }
        if (s.getBool(Store.K_SOUND, true)) {
            try {
                final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
                tg.startTone(ToneGenerator.TONE_PROP_ACK, 600);
                new Handler(Looper.getMainLooper()).postDelayed(tg::release, 900);
            } catch (RuntimeException ignored) {}
        }
    }

    static void tick(Context c) {
        Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator() || !Store.get(c).getBool(Store.K_VIBRATE, true)) return;
        try { v.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE)); } catch (RuntimeException ignored) {}
    }
}
