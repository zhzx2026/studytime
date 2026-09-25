package com.aidemo.studytime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * 番茄钟的 Android 胶水层：进程内唯一一份数据（页面和闹钟接收器共用，避免两边各记一次）。
 *
 * 三条铁律：
 *  1. 结算是**幂等**的——谁先 tick 掉 endAt 谁记账，后到的那位看到的已是新状态，空手而归；
 *     前台走页面心跳、后台走 AlarmReceiver，两条路共用 recordEvents，永远只记一笔。
 *  2. 每次状态变化后 savePomoArm()：落盘 + 排精确闹钟 + 刷新常驻倒计时通知，一步不能少。
 *  3. 闹钟**常在**（前台也在）：正点时谁先 tick 到谁反馈——页面先手就音效+Toast，
 *     接收器先手就发通知；幂等保证不会两边都记账。
 */
public final class Ctl {
    private Ctl() {}

    public static Cfg cfg;
    public static Todo todo;
    public static Recs recs;
    public static Pomo pomo;
    public static int bindId;

    private static boolean loaded;

    /** 进程内懒加载：进程活着用内存里的（页面改的），死了才从 Prefs 重建 */
    public static synchronized void ensure() {
        if (loaded) return;
        cfg = Prefs.cfg();
        todo = Prefs.todo();
        recs = Prefs.recs();
        pomo = new Pomo(cfg);
        Prefs.loadPomo(pomo);
        bindId = Prefs.bindId();
        loaded = true;
    }

    /** 设置页改了配置：两处（Ctl.cfg 与 pomo.cfg）都要跟上 */
    public static synchronized void cfgChanged() {
        ensure();
        cfg = Prefs.cfg();
        pomo.cfg = cfg;
    }

    /**
     * 结算到点阶段并落账（纯记账，不发声——前台音效/后台通知各自负责）。
     * 返回是否有事件。evs 来自 pomo.tick(now)。
     */
    public static synchronized boolean recordEvents(Context c, int[] evs, long now) {
        if (evs == null || evs.length == 0) return false;
        ensure();
        boolean rec = false;
        String today = Cal.todayKey(now);
        for (int ev : evs) {
            if (ev == Pomo.EV_WORK) {
                recs.addPomo(today, cfg.workMin);
                Todo.Task t = bindId != 0 ? todo.byId(bindId) : null;
                if (t != null && !t.done) {
                    t.donePomos++;
                    Prefs.saveTodo(todo);
                }
                rec = true;
            }
        }
        if (rec) Prefs.saveRecs(recs);
        return true;
    }

    /** 落盘 + 排/撤闹钟 + 常驻通知。改完 pomo 状态必须调这个（别直接 Prefs.savePomo）。 */
    public static synchronized void savePomoArm(Context c) {
        ensure();
        Prefs.savePomo(pomo);
        arm(c);
        Notify.running(c, pomo, cfg, todo, bindId);
    }

    /** 后台到点入口：结算 + 记账 + 提醒 + 排下一段的闹钟 */
    public static synchronized int[] settleFromAlarm(Context c, long now) {
        ensure();
        int[] evs = pomo.tick(now);
        if (evs.length == 0) return evs;
        boolean rec = recordEvents(c, evs, now);
        savePomoArm(c);
        boolean wasWork = false;
        for (int ev : evs) if (ev == Pomo.EV_WORK) wasWork = true;
        if (!Notify.granted(c)) Notify.fallbackDing(c, cfg); // 没通知权限：声音振动兜底
        Notify.done(c, wasWork, pomo);
        return evs;
    }

    // ── 精确闹钟 ────────────────────────────────────────────────────────────
    private static PendingIntent alarmIntent(Context c) {
        Intent i = new Intent(c, AlarmReceiver.class);
        return PendingIntent.getBroadcast(c, 7, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** 重排闹钟（savePomoArm 内部用；也给将来「切后台补排」留口子） */
    public static synchronized void arm(Context c) {
        ensure();
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = alarmIntent(c);
        try { am.cancel(pi); } catch (RuntimeException ignored) {}
        if (!pomo.running || pomo.state == Pomo.IDLE || pomo.endAt <= 0) return;
        try {
            if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, pomo.endAt, pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, pomo.endAt, pi);
            }
        } catch (SecurityException e) {
            try { am.set(AlarmManager.RTC_WAKEUP, pomo.endAt, pi); } catch (RuntimeException ignored) {}
        } catch (RuntimeException ignored) {
        }
    }
}
