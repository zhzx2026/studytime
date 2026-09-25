package com.aidemo.studytime;

import java.util.Arrays;

/**
 * 番茄钟状态机（纯 Java，无 Android 依赖，主机可测）。
 *
 * 时间全部由调用方传入的 now（epoch 毫秒）驱动，方便测试与「杀后台续存」：
 * 运行中的剩余时间永远是 endAt - now，落盘只存 endAt；暂停时存 remaining。
 *
 * 意外退出续存（tick 的补账规则，Agent.md 有背景）：
 *  · 回来时只把「已经到点的当前阶段」结算掉；
 *  · 休息阶段可以顺着链继续补完（休息不记账），并按原 endAt 串接时间轴；
 *  · 但最多只承认一个 WORK 的完成——再次到点的 WORK 一律停表拉满，
 *    避开几个小时回来凭空多出一排番茄（不诚实的统计比少记更糟）。
 */
public final class Pomo {
    /** 阶段 */
    public static final int IDLE = 0, WORK = 1, SHORT = 2, LONG = 3;
    /** tick 返回的事件 */
    public static final int EV_NONE = 0, EV_WORK = 1, EV_SHORT = 2, EV_LONG = 3;

    public Cfg cfg;
    public int state = IDLE;
    public boolean running = false;
    public long endAt = 0;      // running 时：本阶段结束时刻
    public long remaining = 0;  // 未 running 时：本阶段剩余毫秒
    public int cycle = 0;       // 本轮已做的番茄数（长休后归零）

    public Pomo() { this(new Cfg()); }
    public Pomo(Cfg cfg) { this.cfg = cfg; }

    /** 当前阶段总时长（IDLE 时按专注算，给按钮/进度条用） */
    public long phaseMs(int s) {
        if (s == WORK) return cfg.workMs();
        if (s == LONG) return cfg.longMs();
        if (s == SHORT) return cfg.shortMs();
        return cfg.workMs();
    }

    public long totalMs() { return phaseMs(state == IDLE ? WORK : state); }

    public long remainingAt(long now) {
        return running ? Math.max(0, endAt - now) : remaining;
    }

    public boolean isActive() { return state != IDLE; }
    public boolean isBreak() { return state == SHORT || state == LONG; }

    /** 空闲 → 开始一段专注 */
    public void start(long now) {
        if (state != IDLE) return;
        begin(WORK, now);
    }

    /** 暂停中 → 继续（IDLE 时等价 start） */
    public void resume(long now) {
        if (state == IDLE) { start(now); return; }
        if (running) return;
        running = true;
        endAt = now + Math.max(1, remaining);
        remaining = 0;
    }

    public void pause(long now) {
        if (!running) return;
        remaining = remainingAt(now);
        running = false;
        endAt = 0;
    }

    /**
     * 跳过当前休息，立刻进入下一段（返回被跳过的休息事件）。
     * 只在休息阶段有意义；running 时时间轴仍按「本该结束的时刻」链下去。
     */
    public int skip(long now) {
        if (state != SHORT && state != LONG) return EV_NONE;
        int ev = state == LONG ? EV_LONG : EV_SHORT;
        long anchor = running ? endAt : now;
        state = WORK;
        if (cfg.auto) {
            running = true;
            endAt = anchor + phaseMs(WORK);
            remaining = 0;
        } else {
            running = false;
            endAt = 0;
            remaining = phaseMs(WORK);
        }
        return ev;
    }

    /** 放弃本轮（回空闲，不记账） */
    public void reset() {
        state = IDLE;
        running = false;
        endAt = 0;
        remaining = 0;
        cycle = 0;
    }

    /**
     * 到点推进。返回本次实际发生的事件（0~2 个，按顺序）。
     * 前台每 250ms 调一次、onResume 时补调一次即可。
     */
    public int[] tick(long now) {
        if (state == IDLE || !running || now < endAt) return new int[0];
        int[] evs = new int[2];
        int n = 0;
        evs[n++] = complete();
        // 休息不记账：可以一路顺着补完（按原时间轴链式推进）
        while (running && (state == SHORT || state == LONG) && now >= endAt) {
            evs[n++] = complete();
        }
        // 兜底：又到下一个 WORK 的点了 —— 停表、剩余拉满，等人回来手动继续
        if (running && state == WORK && now >= endAt) {
            remaining = phaseMs(WORK);
            running = false;
            endAt = 0;
        }
        return Arrays.copyOf(evs, n);
    }

    /** 结算当前阶段并推进到下一阶段；返回刚完成的事件。 */
    private int complete() {
        int ev = state == WORK ? EV_WORK : state == SHORT ? EV_SHORT : EV_LONG;
        long anchor = endAt; // 下一阶段从「本该结束的时刻」接着算，补账不串轴
        int next;
        if (ev == EV_WORK) {
            cycle++;
            if (cycle >= cfg.longEvery) { cycle = 0; next = LONG; }
            else next = SHORT;
        } else if (ev == EV_LONG) {
            next = WORK;
        } else {
            next = WORK;
        }
        if (cfg.auto) {
            state = next;
            running = true;
            endAt = anchor + phaseMs(next);
            remaining = 0;
        } else {
            state = next;
            running = false;
            endAt = 0;
            remaining = phaseMs(next);
        }
        return ev;
    }

    private void begin(int s, long now) {
        state = s;
        running = true;
        endAt = now + phaseMs(s);
        remaining = 0;
    }

    // ── 落盘/恢复：st|run|endAt|remaining|cycle ─────────────────────────────
    public String save() {
        return state + "|" + (running ? 1 : 0) + "|" + endAt + "|" + remaining + "|" + cycle;
    }

    /** 载入快照；脏数据整份拒收（返回 false，调用方保持默认空闲态） */
    public boolean load(String s) {
        if (s == null) return false;
        String[] p = s.split("\\|", -1);
        if (p.length != 5) return false;
        try {
            int st = Integer.parseInt(p[0]);
            boolean run = p[1].equals("1");
            long end = Long.parseLong(p[2]);
            long rem = Long.parseLong(p[3]);
            int cy = Integer.parseInt(p[4]);
            if (st != IDLE && st != WORK && st != SHORT && st != LONG) return false;
            if (st == IDLE) return true; // 空闲快照即重置
            if (rem < 0 || end < 0 || cy < 0) return false;
            if (run && end == 0) return false;
            state = st;
            running = run;
            endAt = end;
            remaining = rem;
            cycle = cy;
            if (!running && rem == 0) remaining = phaseMs(st); // 容错：暂停态缺剩余就拉满
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
