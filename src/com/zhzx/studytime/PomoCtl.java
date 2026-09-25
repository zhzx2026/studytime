package com.zhzx.studytime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

/**
 * 番茄钟的 Android 胶水层：进程内唯一一份 {@link Pomo}（页面和闹钟接收器共用，避免两边各结算一次），
 * 负责读配置、落盘、结算记账、排闹钟、刷新常驻通知。
 */
final class PomoCtl {
    private PomoCtl() {}

    private static Pomo pomo;

    static synchronized Pomo get(Context c) {
        Store s = Store.get(c);
        if (pomo == null) pomo = Pomo.decode(s.getStr(Store.K_POMO, null));
        pomo.focusMin = s.getInt(Store.K_FOCUS, 25);
        pomo.shortMin = s.getInt(Store.K_SHORT, 5);
        pomo.longMin = s.getInt(Store.K_LONG, 15);
        pomo.longEvery = s.getInt(Store.K_EVERY, 4);
        if (!pomo.started) pomo.remain = pomo.full(pomo.phase);
        if (pomo.taskId >= 0) {
            Store.Task t = s.task(pomo.taskId);
            if (t == null || t.done) pomo.taskId = -1;
        }
        return pomo;
    }

    /** 落盘 + 排闹钟 + 刷新常驻通知。每次改完状态都要调。 */
    static void save(Context c, Pomo p) {
        Store.get(c).putStr(Store.K_POMO, p.encode());
        alarm(c, p);
        Notify.running(c, p);
    }

    /** 结算到点的阶段并记账（专注 → 写专注记录 + 关联待办番茄数 +1）。调用方随后 save。 */
    static List<Pomo.Done> settle(Context c, Pomo p) {
        Store s = Store.get(c);
        List<Pomo.Done> list = p.settle(System.currentTimeMillis(),
                s.getBool(Store.K_AUTO_BREAK, true), s.getBool(Store.K_AUTO_FOCUS, false));
        for (Pomo.Done d : list) {
            if (d.phase == Pomo.FOCUS) s.addSession(d.start, d.end, d.minutes, d.taskId);
        }
        return list;
    }

    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 7, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static void alarm(Context c, Pomo p) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        am.cancel(pi);
        if (!p.running) return;
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.endAt, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.endAt, pi);
            }
        } catch (SecurityException e) {
            am.set(AlarmManager.RTC_WAKEUP, p.endAt, pi);
        }
    }
}
