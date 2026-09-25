package com.zhzx.studytime;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 页签 4：日历（月视图热力 + 某天的专注 / 待办 / 打卡明细，可补卡、可往某天加待办）。 */
final class CalendarPage extends Page {
    private int year, month, selected;
    private MonthView mv;
    private TextView monthTv, sumA, sumB, sumC, sumD, dayTitle;
    private LinearLayout detail;

    CalendarPage(MainActivity a) {
        super(a);
        LocalDate d = LocalDate.now();
        year = d.getYear();
        month = d.getMonthValue();
        selected = Day.today();
    }

    @Override String title() { return "日历"; }

    @Override
    protected View build() {
        final MainActivity c = act;
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.v(c);
        col.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 24));
        sv.addView(col);

        LinearLayout cal = Ui.card(c);
        cal.setPadding(Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12));
        LinearLayout nav = Ui.h(c);
        TextView prev = navBtn("‹");
        TextView next = navBtn("›");
        monthTv = Ui.bold(Ui.tv(c, "", 18, Ui.col(c, R.color.text)));
        monthTv.setGravity(Gravity.CENTER);
        TextView todayBtn = Ui.chip(c, "今天");
        prev.setOnClickListener(v -> shift(-1));
        next.setOnClickListener(v -> shift(1));
        todayBtn.setOnClickListener(v -> {
            LocalDate d = LocalDate.now();
            year = d.getYear(); month = d.getMonthValue(); selected = Day.today();
            refresh();
        });
        nav.addView(prev);
        nav.addView(monthTv, Ui.weight(1));
        nav.addView(next);
        nav.addView(todayBtn, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 6, 0, 4, 0));
        cal.addView(nav);

        mv = new MonthView(c);
        mv.setOnDay(k -> {
            selected = k;
            if (Day.month(k) != month || Day.year(k) != year) { year = Day.year(k); month = Day.month(k); }
            refresh();
        });
        cal.addView(mv, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 8, 0, 4));

        // 图例
        LinearLayout legend = Ui.h(c);
        legend.setGravity(Gravity.CENTER);
        legend.addView(Ui.tv(c, "专注 少", 11, Ui.col(c, R.color.sub)));
        for (int i = 0; i <= 4; i++) {
            View sq = new View(c);
            int col0 = i == 0 ? Ui.col(c, R.color.field) : Ui.heatColor(c, i);
            sq.setBackground(Ui.rr(col0, Ui.dp(c, 3)));
            legend.addView(sq, Ui.lpm(c, Ui.dp(c, 12), Ui.dp(c, 12), 3, 0, 3, 0));
        }
        legend.addView(Ui.tv(c, "多    ", 11, Ui.col(c, R.color.sub)));
        TextView dots = Ui.tv(c, "● 待办   ", 11, Ui.col(c, R.color.brand));
        TextView dots2 = Ui.tv(c, "● 打卡", 11, Ui.col(c, R.color.green));
        legend.addView(dots);
        legend.addView(dots2);
        cal.addView(legend);
        col.addView(cal, Ui.lp(Ui.MATCH, Ui.WRAP));

        // 本月汇总
        LinearLayout sum = Ui.card(c);
        sum.setOrientation(LinearLayout.HORIZONTAL);
        sumA = stat(sum, "本月专注");
        sumB = stat(sum, "番茄");
        sumC = stat(sum, "完成待办");
        sumD = stat(sum, "打卡次数");
        col.addView(sum, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));

        // 当天明细
        LinearLayout day = Ui.card(c);
        LinearLayout dh = Ui.h(c);
        dayTitle = Ui.bold(Ui.tv(c, "", 17, Ui.col(c, R.color.text)));
        dh.addView(dayTitle, Ui.weight(1));
        TextView add = Ui.pill(c, "＋ 待办", Ui.col(c, R.color.brand_soft), Ui.col(c, R.color.brand));
        add.setTextSize(13);
        add.setPadding(Ui.dp(c, 12), Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6));
        add.setOnClickListener(v -> TaskDialog.show(c, null, selected, t -> refresh()));
        dh.addView(add);
        day.addView(dh);
        detail = Ui.v(c);
        day.addView(detail);
        col.addView(day, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
        return sv;
    }

    private TextView navBtn(String s) {
        TextView t = Ui.bold(Ui.tv(act, s, 24, Ui.col(act, R.color.text)));
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.ripple(null, Ui.oval(Color.BLACK)));
        t.setLayoutParams(Ui.lp(Ui.dp(act, 44), Ui.dp(act, 44)));
        return t;
    }

    private TextView stat(LinearLayout parent, String label) {
        LinearLayout box = Ui.v(act);
        box.setGravity(Gravity.CENTER);
        TextView v = Ui.bold(Ui.tv(act, "0", 17, Ui.col(act, R.color.text)));
        v.setGravity(Gravity.CENTER);
        TextView l = Ui.tv(act, label, 11, Ui.col(act, R.color.sub));
        l.setGravity(Gravity.CENTER);
        box.addView(v);
        box.addView(l);
        parent.addView(box, Ui.weight(1));
        return v;
    }

    private void shift(int delta) {
        LocalDate d = LocalDate.of(year, month, 1).plusMonths(delta);
        year = d.getYear();
        month = d.getMonthValue();
        refresh();
    }

    void refresh() {
        if (mv == null) return;
        Store s = Store.get(act);
        int today = Day.today();
        monthTv.setText(year + "年" + month + "月");
        int[] grid = Day.monthGrid(year, month);
        int[] heat = new int[42];
        boolean[] td = new boolean[42], hd = new boolean[42];
        Map<Integer, Integer> mins = s.minutesByDay();
        for (int i = 0; i < 42; i++) {
            int k = grid[i];
            Integer m = mins.get(k);
            heat[i] = Ui.heatLevel(m == null ? 0 : m);
            hd[i] = s.habitsCheckedOn(k) > 0;
        }
        for (Store.Task t : s.tasks) {
            for (int i = 0; i < 42; i++) {
                if ((!t.done && t.due == grid[i]) || (t.done && t.doneDay == grid[i])) td[i] = true;
            }
        }
        mv.setData(year, month, today, selected, heat, td, hd);

        // 本月汇总
        int mm = 0, pomos = 0, doneTasks = 0, checks = 0;
        for (Store.Session x : s.sessions) if (Day.year(x.day) == year && Day.month(x.day) == month) { mm += x.min; pomos++; }
        for (Store.Task t : s.tasks) if (t.done && Day.year(t.doneDay) == year && Day.month(t.doneDay) == month) doneTasks++;
        for (Store.Habit h : s.habits) for (int k : h.days) if (Day.year(k) == year && Day.month(k) == month) checks++;
        sumA.setText(mm >= 60 ? String.format(java.util.Locale.US, "%.1fh", mm / 60f) : mm + "m");
        sumB.setText(String.valueOf(pomos));
        sumC.setText(String.valueOf(doneTasks));
        sumD.setText(String.valueOf(checks));

        renderDay(s, today);
    }

    private void renderDay(final Store s, final int today) {
        final MainActivity c = act;
        dayTitle.setText(Day.label(selected) + (selected == today ? " · 今天" : ""));
        detail.removeAllViews();

        // 专注
        List<Store.Session> ss = s.sessionsOn(selected);
        int m = 0;
        for (Store.Session x : ss) m += x.min;
        detail.addView(Ui.section(c, "🍅 专注  " + (ss.isEmpty() ? "无" : ss.size() + " 个番茄 · " + Ui.dur(m))));
        for (Store.Session x : ss) {
            TextView t = Ui.tv(c, Day.hm(x.start) + " – " + Day.hm(x.end) + "   " + (x.taskTitle.isEmpty() ? "自由专注" : x.taskTitle),
                    13, Ui.col(c, R.color.text));
            t.setPadding(Ui.dp(c, 8), Ui.dp(c, 2), 0, Ui.dp(c, 2));
            detail.addView(t);
        }

        // 待办：当天截止 + 当天完成
        detail.addView(Ui.section(c, "✅ 待办"));
        boolean any = false;
        for (final Store.Task t : s.tasks) {
            boolean dueHere = t.due == selected, doneHere = t.done && t.doneDay == selected;
            if (!dueHere && !doneHere) continue;
            any = true;
            LinearLayout row = Ui.h(c);
            row.setPadding(Ui.dp(c, 4), Ui.dp(c, 6), 0, Ui.dp(c, 6));
            TextView ck = TodoPage.checkCircle(c, t.done, Ui.col(c, R.color.green));
            int cs = Ui.dp(c, 22);
            row.addView(ck, Ui.lpm(c, cs, cs, 0, 0, 10, 0));
            TextView tt = Ui.tv(c, t.title + (doneHere && !dueHere ? "（当天完成）" : ""), 14,
                    Ui.col(c, t.done ? R.color.sub : R.color.text));
            if (t.done) tt.setPaintFlags(tt.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            row.addView(tt, Ui.weight(1));
            ck.setOnClickListener(v -> { s.setDone(t, !t.done); Notify.tick(c); refresh(); });
            row.setOnClickListener(v -> TaskDialog.show(c, t, 0, x -> refresh()));
            detail.addView(row);
        }
        if (!any) detail.addView(empty("这天没有待办"));

        // 坚持：每个习惯当天的打卡（今天及以前可以补卡）
        detail.addView(Ui.section(c, "🔥 坚持"));
        if (s.habits.isEmpty()) detail.addView(empty("还没有习惯，去「坚持」页添加"));
        for (final Store.Habit h : s.habits) {
            boolean on = h.days.contains(selected);
            LinearLayout row = Ui.h(c);
            row.setPadding(Ui.dp(c, 4), Ui.dp(c, 6), 0, Ui.dp(c, 6));
            TextView ck = TodoPage.checkCircle(c, on, Ui.col(c, R.color.green));
            int cs = Ui.dp(c, 22);
            row.addView(ck, Ui.lpm(c, cs, cs, 0, 0, 10, 0));
            row.addView(Ui.tv(c, h.emoji + " " + h.name, 14, Ui.col(c, R.color.text)), Ui.weight(1));
            if (selected <= today) {
                row.setOnClickListener(v -> { s.toggleHabit(h, selected); Notify.tick(c); refresh(); });
            } else {
                row.setAlpha(0.5f);
            }
            detail.addView(row);
        }
        if (!s.habits.isEmpty() && selected <= today) {
            TextView tip = Ui.tv(c, "点一行可以给这天补卡 / 取消", 11, Ui.col(c, R.color.sub));
            tip.setPadding(Ui.dp(c, 4), Ui.dp(c, 4), 0, 0);
            detail.addView(tip);
        }
    }

    private TextView empty(String s) {
        TextView t = Ui.tv(act, s, 13, Ui.col(act, R.color.sub));
        t.setPadding(Ui.dp(act, 8), Ui.dp(act, 2), 0, Ui.dp(act, 2));
        return t;
    }

    @Override
    void onShow() { refresh(); }

    @Override
    void onAlarm() { refresh(); }
}
