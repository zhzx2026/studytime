package com.zhzx.studytime;

import java.util.ArrayList;
import java.util.List;

/**
 * 番茄钟状态机（纯 Java，PomoTest 覆盖）。
 *
 * 关键设计：运行中只存「到点时刻 endAt」（墙钟毫秒），暂停时存「剩余 remain」。
 * 于是 App 被杀 / 闪退 / 锁屏都不影响计时 —— 重开后 {@link #settle} 按当前时间把错过的阶段补结算，
 * 专注记录不会丢（对应 wordsprint AGENT.md 坑 18「进度必须落盘，别指望 onPause」）。
 */
public final class Pomo {
    public static final int FOCUS = 0, SHORT = 1, LONG = 2;

    // ---- 配置（每次从设置读入，不参与编码）----
    public int focusMin = 25, shortMin = 5, longMin = 15, longEvery = 4;

    // ---- 状态（参与编码）----
    public int phase = FOCUS;
    public boolean running;
    /** 本阶段是否已开始（开始过、未结束 = 运行中或暂停中） */
    public boolean started;
    public long endAt;
    public long remain;
    /** 本阶段第一次按「开始」的时刻（专注记录的起点） */
    public long startedAt;
    /** 本轮已完成的专注个数（决定何时长休） */
    public int done;
    /** 关联的待办 id，-1 = 不关联 */
    public long taskId = -1;

    /** 一次阶段结束事件 */
    public static final class Done {
        public final int phase;
        public final long start, end, taskId;
        public final int minutes;
        Done(int phase, long start, long end, int minutes, long taskId) {
            this.phase = phase; this.start = start; this.end = end; this.minutes = minutes; this.taskId = taskId;
        }
    }

    public long full(int p) {
        int m = p == FOCUS ? focusMin : p == SHORT ? shortMin : longMin;
        if (m < 1) m = 1;
        return m * 60_000L;
    }

    public int minutesOf(int p) {
        return (int) (full(p) / 60_000L);
    }

    public boolean idle() { return !started; }

    public void start(long now) {
        if (running) return;
        if (!started) {
            started = true;
            startedAt = now;
            remain = full(phase);
        }
        running = true;
        endAt = now + Math.max(0, remain);
    }

    public void pause(long now) {
        if (!running) return;
        remain = Math.max(0, endAt - now);
        running = false;
    }

    /** 放弃当前阶段（不记录），回到本阶段满时长待开始。 */
    public void reset() {
        running = false;
        started = false;
        remain = full(phase);
    }

    /** 空闲时切换阶段。运行中/暂停中调用无效（先 reset）。 */
    public boolean setPhase(int p) {
        if (started || p < FOCUS || p > LONG) return false;
        phase = p;
        remain = full(p);
        return true;
    }

    public long remaining(long now) {
        if (running) return Math.max(0, endAt - now);
        if (started) return remain;
        return full(phase);
    }

    /** 0..1 已走过的比例 */
    public float progress(long now) {
        long f = full(phase);
        float p = 1f - (float) remaining(now) / f;
        return p < 0 ? 0 : p > 1 ? 1 : p;
    }

    public boolean due(long now) {
        return running && now >= endAt;
    }

    /** 本阶段自然结束：专注计数 +1，进入下一阶段（未开始）。返回刚结束的阶段。 */
    public int finish() {
        int p = phase;
        if (p == FOCUS) {
            done++;
            phase = (longEvery > 0 && done % longEvery == 0) ? LONG : SHORT;
        } else {
            phase = FOCUS;
            if (p == LONG) done = 0;
        }
        running = false;
        started = false;
        remain = full(phase);
        return p;
    }

    /** 跳过当前阶段：专注被跳过不计数、不记录。 */
    public void skip() {
        if (phase == FOCUS) {
            phase = (longEvery > 0 && (done + 1) % longEvery == 0) ? LONG : SHORT;
        } else {
            if (phase == LONG) done = 0;
            phase = FOCUS;
        }
        running = false;
        started = false;
        remain = full(phase);
    }

    /**
     * 按当前时间结算所有已到点的阶段（可能错过了好几个：锁屏久了 + 开了自动续）。
     * 自动续的下一阶段从上一阶段的 endAt 起算，时间线不漂移。
     */
    public List<Done> settle(long now, boolean autoBreak, boolean autoFocus) {
        List<Done> out = new ArrayList<>();
        int guard = 0;
        while (due(now) && guard++ < 64) {
            long end = endAt, st = startedAt;
            long tid = taskId;
            int mins = minutesOf(phase); // 暂停不计入：实际专注时长 = 本阶段设定时长
            int p = finish();
            out.add(new Done(p, st, end, mins, tid));
            boolean auto = phase == FOCUS ? autoFocus : autoBreak;
            if (auto) start(end); else break;
        }
        return out;
    }

    // ---- 持久化：一行文本，字段顺序固定；解析失败 → 全新状态 ----
    public String encode() {
        return "p1," + phase + "," + (running ? 1 : 0) + "," + (started ? 1 : 0) + "," + endAt + ","
                + remain + "," + startedAt + "," + done + "," + taskId;
    }

    public static Pomo decode(String s) {
        Pomo p = new Pomo();
        if (s == null || !s.startsWith("p1,")) { p.remain = p.full(FOCUS); return p; }
        try {
            String[] a = s.split(",");
            p.phase = Integer.parseInt(a[1]);
            p.running = "1".equals(a[2]);
            p.started = "1".equals(a[3]);
            p.endAt = Long.parseLong(a[4]);
            p.remain = Long.parseLong(a[5]);
            p.startedAt = Long.parseLong(a[6]);
            p.done = Integer.parseInt(a[7]);
            p.taskId = Long.parseLong(a[8]);
            if (p.phase < FOCUS || p.phase > LONG) throw new IllegalStateException("phase");
            if (p.running) p.started = true;
        } catch (RuntimeException e) {
            p = new Pomo();
            p.remain = p.full(FOCUS);
        }
        return p;
    }

    public static String phaseName(int p) {
        return p == FOCUS ? "专注" : p == SHORT ? "短休息" : "长休息";
    }
}
