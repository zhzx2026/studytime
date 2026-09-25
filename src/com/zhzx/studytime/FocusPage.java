package com.zhzx.studytime;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** 页签 1：番茄钟。 */
final class FocusPage extends Page {
    private RingView ring;
    private TextView time, phaseTv, roundTv, taskTv, startBtn, resetBtn, skipBtn;
    private TextView statPomo, statMin, statStreak;
    private final TextView[] chips = new TextView[3];
    private LinearLayout todayList;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean visible;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!visible) return;
            step(true);
            h.postDelayed(this, 250);
        }
    };

    FocusPage(MainActivity a) { super(a); }

    @Override String title() { return "专注"; }

    @Override
    protected View build() {
        final MainActivity c = act;
        ScrollView sv = new ScrollView(c);
        sv.setFillViewport(true);
        LinearLayout col = Ui.v(c);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setPadding(Ui.dp(c, 16), Ui.dp(c, 6), Ui.dp(c, 16), Ui.dp(c, 24));
        sv.addView(col);

        // 阶段切换
        LinearLayout chipRow = Ui.h(c);
        chipRow.setGravity(Gravity.CENTER);
        for (int i = 0; i < 3; i++) {
            final int p = i;
            chips[i] = Ui.chip(c, Pomo.phaseName(i));
            chips[i].setOnClickListener(v -> {
                Pomo po = PomoCtl.get(c);
                if (po.started) { Ui.toast(c, "计时进行中，先「重置」再切换"); return; }
                po.setPhase(p);
                PomoCtl.save(c, po);
                step(false);
            });
            chipRow.addView(chips[i], Ui.lpm(c, Ui.WRAP, Ui.WRAP, 4, 0, 4, 0));
        }
        col.addView(chipRow, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 4, 0, 12));

        // 圆环 + 中央时间
        int sw = c.getResources().getDisplayMetrics().widthPixels;
        int size = Math.min(sw - Ui.dp(c, 72), Ui.dp(c, 300));
        FrameLayout ringBox = new FrameLayout(c);
        ring = new RingView(c);
        ringBox.addView(ring, new FrameLayout.LayoutParams(size, size));
        LinearLayout center = Ui.v(c);
        center.setGravity(Gravity.CENTER);
        phaseTv = Ui.tv(c, "", 15, Ui.col(c, R.color.sub));
        phaseTv.setGravity(Gravity.CENTER);
        time = Ui.bold(Ui.tv(c, "25:00", 58, Ui.col(c, R.color.text)));
        time.setGravity(Gravity.CENTER);
        time.setFontFeatureSettings("tnum");
        roundTv = Ui.tv(c, "", 13, Ui.col(c, R.color.sub));
        roundTv.setGravity(Gravity.CENTER);
        center.addView(phaseTv);
        center.addView(time);
        center.addView(roundTv);
        ringBox.addView(center, new FrameLayout.LayoutParams(size, size, Gravity.CENTER));
        col.addView(ringBox, Ui.lp(size, size));

        // 关联待办
        taskTv = Ui.tv(c, "", 15, Ui.col(c, R.color.text));
        taskTv.setGravity(Gravity.CENTER);
        taskTv.setSingleLine(true);
        taskTv.setEllipsize(android.text.TextUtils.TruncateAt.END);
        taskTv.setPadding(Ui.dp(c, 16), Ui.dp(c, 10), Ui.dp(c, 16), Ui.dp(c, 10));
        taskTv.setBackground(Ui.ripple(Ui.rr(Ui.col(c, R.color.field), Ui.dp(c, 20)), Ui.rr(Color.BLACK, Ui.dp(c, 20))));
        taskTv.setOnClickListener(v -> pickTask());
        col.addView(taskTv, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 14, 0, 0));

        // 按钮
        LinearLayout btns = Ui.h(c);
        btns.setGravity(Gravity.CENTER);
        resetBtn = Ui.pill(c, "重置", Ui.col(c, R.color.field), Ui.col(c, R.color.text));
        startBtn = Ui.pill(c, "开始专注", Ui.col(c, R.color.brand), Color.WHITE);
        startBtn.setTextSize(17);
        startBtn.setPadding(Ui.dp(c, 34), Ui.dp(c, 14), Ui.dp(c, 34), Ui.dp(c, 14));
        skipBtn = Ui.pill(c, "跳过", Ui.col(c, R.color.field), Ui.col(c, R.color.text));
        btns.addView(resetBtn, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 10, 0));
        btns.addView(startBtn);
        btns.addView(skipBtn, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 10, 0, 0, 0));
        col.addView(btns, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 18, 0, 18));
        startBtn.setOnClickListener(v -> toggle());
        resetBtn.setOnClickListener(v -> reset());
        skipBtn.setOnClickListener(v -> {
            Pomo p = PomoCtl.get(c);
            p.skip();
            PomoCtl.save(c, p);
            step(false);
        });

        // 今日统计
        LinearLayout stats = Ui.card(c);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        statPomo = stat(stats, "今日番茄");
        statMin = stat(stats, "今日专注");
        statStreak = stat(stats, "连续专注");
        col.addView(stats, Ui.lp(Ui.MATCH, Ui.WRAP));

        // 今日记录
        LinearLayout rec = Ui.card(c);
        rec.addView(Ui.bold(Ui.tv(c, "今日专注记录", 15, Ui.col(c, R.color.text))));
        todayList = Ui.v(c);
        rec.addView(todayList);
        col.addView(rec, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        return sv;
    }

    private TextView stat(LinearLayout parent, String label) {
        LinearLayout box = Ui.v(act);
        box.setGravity(Gravity.CENTER);
        TextView v = Ui.bold(Ui.tv(act, "0", 22, Ui.col(act, R.color.text)));
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.tv(act, label, 12, Ui.col(act, R.color.sub));
        l.setGravity(Gravity.CENTER);
        box.addView(v);
        box.addView(l);
        parent.addView(box, Ui.weight(1));
        return v;
    }

    private int phaseColor(int p) {
        return Ui.col(act, p == Pomo.FOCUS ? R.color.brand : p == Pomo.SHORT ? R.color.green : R.color.blue);
    }

    private int phaseSoft(int p) {
        return Ui.col(act, p == Pomo.FOCUS ? R.color.brand_soft : p == Pomo.SHORT ? R.color.green_soft : R.color.blue_soft);
    }

    /** 结算到点阶段 + 刷新显示。live=true 表示前台实时结算（给即时反馈）。 */
    private void step(boolean live) {
        Pomo p = PomoCtl.get(act);
        long now = System.currentTimeMillis();
        if (p.due(now)) {
            List<Pomo.Done> done = PomoCtl.settle(act, p);
            PomoCtl.save(act, p);
            if (!done.isEmpty()) {
                if (live) {
                    Notify.feedback(act);
                    Pomo.Done last = done.get(done.size() - 1);
                    Ui.toast(act, last.phase == Pomo.FOCUS ? "🍅 番茄完成！休息一下吧" : "⏰ 休息结束，继续加油");
                }
                refreshStats();
            }
        }
        render(p, now);
    }

    private void render(Pomo p, long now) {
        if (ring == null) return;
        int pc = phaseColor(p.phase);
        ring.setColors(pc, phaseSoft(p.phase));
        ring.setProgress(p.progress(now));
        time.setText(Day.clock(p.remaining(now)));
        phaseTv.setText(p.running ? Pomo.phaseName(p.phase) + "中" : p.started ? "已暂停" : Pomo.phaseName(p.phase));
        phaseTv.setTextColor(p.started ? pc : Ui.col(act, R.color.sub));
        int every = Math.max(1, p.longEvery);
        int inRound = p.done % every;
        roundTv.setText(p.phase == Pomo.FOCUS
                ? "本轮第 " + (inRound + 1) + " / " + every + " 个番茄"
                : "已完成 " + (inRound == 0 && p.phase == Pomo.LONG ? every : inRound) + " / " + every);
        for (int i = 0; i < 3; i++) Ui.setChip(act, chips[i], i == p.phase);

        String label = p.running ? "暂停" : p.started ? "继续" : "开始" + (p.phase == Pomo.FOCUS ? "专注" : "休息");
        startBtn.setText(label);
        int r = Ui.dp(act, 26);
        startBtn.setBackground(Ui.ripple(Ui.rr(pc, r), Ui.rr(Color.BLACK, r)));
        resetBtn.setAlpha(p.started ? 1f : 0.4f);
        resetBtn.setEnabled(p.started);

        Store.Task t = p.taskId >= 0 ? Store.get(act).task(p.taskId) : null;
        taskTv.setText(t != null ? "🎯 " + t.title + "  ·  🍅" + t.pomos + "/" + t.est + "  ▾" : "🎯 选择要专注的待办  ▾");

        boolean keep = p.running && visible && Store.get(act).getBool(Store.K_KEEP_ON, false);
        if (keep) act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void refreshStats() {
        if (statPomo == null) return;
        Store s = Store.get(act);
        int today = Day.today();
        statPomo.setText(String.valueOf(s.pomosOn(today)));
        statMin.setText(Ui.dur(s.minutesOn(today)));
        statStreak.setText(Streak.current(s.focusDays(), today) + "天");
        todayList.removeAllViews();
        List<Store.Session> list = new ArrayList<>(s.sessionsOn(today));
        if (list.isEmpty()) {
            TextView e = Ui.tv(act, "还没有完成的番茄。定个小目标：先专注 25 分钟 💪", 13, Ui.col(act, R.color.sub));
            e.setPadding(0, Ui.dp(act, 8), 0, 0);
            todayList.addView(e);
            return;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            Store.Session x = list.get(i);
            LinearLayout row = Ui.h(act);
            row.setPadding(0, Ui.dp(act, 8), 0, 0);
            TextView dot = Ui.tv(act, "🍅", 14, Ui.col(act, R.color.text));
            TextView when = Ui.tv(act, Day.hm(x.start) + " – " + Day.hm(x.end), 14, Ui.col(act, R.color.text));
            when.setFontFeatureSettings("tnum");
            TextView what = Ui.tv(act, (x.taskTitle.isEmpty() ? "自由专注" : x.taskTitle) + " · " + x.min + "分钟", 13,
                    Ui.col(act, R.color.sub));
            what.setSingleLine(true);
            what.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(dot, Ui.lpm(act, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
            row.addView(when, Ui.lpm(act, Ui.WRAP, Ui.WRAP, 0, 0, 10, 0));
            row.addView(what, Ui.weight(1));
            todayList.addView(row);
        }
    }

    private void toggle() {
        Pomo p = PomoCtl.get(act);
        long now = System.currentTimeMillis();
        if (p.running) p.pause(now); else p.start(now);
        Notify.tick(act);
        PomoCtl.save(act, p);
        step(false);
    }

    private void reset() {
        final Pomo p = PomoCtl.get(act);
        if (!p.started) return;
        if (p.phase == Pomo.FOCUS) {
            Ui.dialog(act).setTitle("放弃这个番茄？")
                    .setMessage("未完成的番茄不会被记录。")
                    .setPositiveButton("放弃", (d, w) -> { p.reset(); PomoCtl.save(act, p); step(false); })
                    .setNegativeButton("继续坚持", null)
                    .show();
        } else {
            p.reset();
            PomoCtl.save(act, p);
            step(false);
        }
    }

    private void pickTask() {
        final Store s = Store.get(act);
        final List<Store.Task> open = TodoPage.sorted(s, TodoPage.F_OPEN);
        final String[] items = new String[open.size() + 2];
        items[0] = "不关联待办（自由专注）";
        for (int i = 0; i < open.size(); i++) {
            Store.Task t = open.get(i);
            items[i + 1] = t.title + "  🍅" + t.pomos + "/" + t.est;
        }
        items[items.length - 1] = "＋ 新建待办…";
        Ui.dialog(act).setTitle("专注哪件事？")
                .setItems(items, (d, which) -> {
                    Pomo p = PomoCtl.get(act);
                    if (which == 0) p.taskId = -1;
                    else if (which == items.length - 1) {
                        TaskDialog.show(act, null, Day.today(), t -> {
                            Pomo q = PomoCtl.get(act);
                            q.taskId = t.id;
                            PomoCtl.save(act, q);
                            step(false);
                        });
                        return;
                    } else p.taskId = open.get(which - 1).id;
                    PomoCtl.save(act, p);
                    step(false);
                })
                .show();
    }

    @Override
    void onShow() {
        visible = true;
        Pomo p = PomoCtl.get(act);
        long now = System.currentTimeMillis();
        if (p.due(now)) {
            List<Pomo.Done> done = PomoCtl.settle(act, p);
            PomoCtl.save(act, p);
            int n = 0;
            for (Pomo.Done d : done) if (d.phase == Pomo.FOCUS) n++;
            if (n > 0) Ui.toast(act, "离开期间完成了 " + n + " 个番茄 🍅");
        }
        refreshStats();
        render(p, now);
        h.removeCallbacks(tick);
        h.post(tick);
    }

    @Override
    void onHide() {
        visible = false;
        h.removeCallbacks(tick);
        act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    void onAlarm() {
        refreshStats();
        render(PomoCtl.get(act), System.currentTimeMillis());
    }
}
