package com.zhzx.studytime;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 页签 3：坚持（习惯打卡 + 连续天数 + 目标天数）。 */
final class HabitPage extends Page {
    static final String[] EMOJI = {"📖", "✍️", "🏃", "💪", "🧘", "💧", "🛏️", "🍎", "🎹", "🗣️", "🧠", "📵", "🌅", "🧹", "💊", "✅"};
    static final int[] TARGETS = {7, 21, 30, 66, 100, 365};

    private LinearLayout list;
    private TextView sumTv, sumSub;
    private View sumBar;
    private LinearLayout sumBarBox;

    HabitPage(MainActivity a) { super(a); }

    @Override String title() { return "坚持"; }

    @Override
    protected View build() {
        final MainActivity c = act;
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.v(c);
        col.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 24));
        sv.addView(col);

        LinearLayout sum = Ui.card(c);
        LinearLayout sr = Ui.h(c);
        LinearLayout st = Ui.v(c);
        sumTv = Ui.bold(Ui.tv(c, "", 20, Ui.col(c, R.color.text)));
        sumSub = Ui.tv(c, "", 13, Ui.col(c, R.color.sub));
        st.addView(sumTv);
        st.addView(sumSub);
        sr.addView(st, Ui.weight(1));
        TextView add = Ui.pill(c, "＋ 新习惯", Ui.col(c, R.color.brand), Color.WHITE);
        add.setOnClickListener(v -> edit(null));
        sr.addView(add);
        sum.addView(sr);
        sumBarBox = Ui.h(c);
        sumBarBox.setBackground(Ui.rr(Ui.col(c, R.color.field), Ui.dp(c, 4)));
        sumBar = new View(c);
        sumBar.setBackground(Ui.rr(Ui.col(c, R.color.green), Ui.dp(c, 4)));
        sumBarBox.addView(sumBar, new LinearLayout.LayoutParams(0, Ui.dp(c, 8), 0));
        sum.addView(sumBarBox, Ui.lpm(c, Ui.MATCH, Ui.dp(c, 8), 0, 12, 0, 0));
        col.addView(sum, Ui.lp(Ui.MATCH, Ui.WRAP));

        list = Ui.v(c);
        col.addView(list, Ui.lp(Ui.MATCH, Ui.WRAP));
        return sv;
    }

    /** 两段权重条：done/total */
    static void setBar(LinearLayout box, View bar, float frac) {
        frac = Math.max(0f, Math.min(1f, frac));
        box.setWeightSum(1f);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) bar.getLayoutParams();
        lp.weight = frac;
        lp.width = 0;
        bar.setLayoutParams(lp);
        bar.setVisibility(frac <= 0 ? View.INVISIBLE : View.VISIBLE);
    }

    void refresh() {
        if (list == null) return;
        Store s = Store.get(act);
        int today = Day.today();
        int n = s.habits.size(), checked = s.habitsCheckedOn(today);
        sumTv.setText(n == 0 ? "养成一个好习惯" : "今日打卡 " + checked + " / " + n);
        int best = 0;
        for (Store.Habit h : s.habits) best = Math.max(best, Streak.current(h.days, today));
        sumSub.setText(n == 0 ? "每天打个卡，看着连续天数一点点涨 🔥"
                : checked == n ? "今天全部完成，太棒了 🎉" : "最长连续进行中：" + best + " 天");
        setBar(sumBarBox, sumBar, n == 0 ? 0 : (float) checked / n);

        list.removeAllViews();
        if (n == 0) {
            LinearLayout ideas = Ui.v(act);
            ideas.setPadding(0, Ui.dp(act, 18), 0, 0);
            ideas.addView(Ui.section(act, "试试这些"));
            String[][] sug = {{"📖", "阅读 20 分钟"}, {"🗣️", "背单词"}, {"🏃", "运动 30 分钟"}, {"🛏️", "11 点前睡觉"}, {"📵", "睡前不刷手机"}};
            for (final String[] x : sug) {
                TextView t = Ui.tv(act, x[0] + "  " + x[1] + "      ＋", 15, Ui.col(act, R.color.text));
                t.setPadding(Ui.dp(act, 14), Ui.dp(act, 12), Ui.dp(act, 14), Ui.dp(act, 12));
                t.setBackground(Ui.ripple(Ui.rr(Ui.col(act, R.color.card), Ui.dp(act, 12)), Ui.rr(Color.BLACK, Ui.dp(act, 12))));
                t.setOnClickListener(v -> {
                    Store st = Store.get(act);
                    Store.Habit h = new Store.Habit();
                    h.id = st.nextId(); h.emoji = x[0]; h.name = x[1]; h.target = 21; h.created = System.currentTimeMillis();
                    st.habits.add(h);
                    st.saveHabits();
                    refresh();
                });
                ideas.addView(t, Ui.lpm(act, Ui.MATCH, Ui.WRAP, 0, 0, 0, 8));
            }
            list.addView(ideas);
            return;
        }
        for (Store.Habit h : s.habits) list.addView(card(h, today), Ui.lpm(act, Ui.MATCH, Ui.WRAP, 0, 12, 0, 0));
    }

    private View card(final Store.Habit h, final int today) {
        final MainActivity c = act;
        LinearLayout card = Ui.card(c);
        boolean on = h.days.contains(today);
        int green = Ui.col(c, R.color.green);

        LinearLayout top = Ui.h(c);
        TextView emo = Ui.tv(c, h.emoji, 24, Ui.col(c, R.color.text));
        emo.setGravity(Gravity.CENTER);
        emo.setBackground(Ui.oval(Ui.col(c, R.color.field)));
        int es = Ui.dp(c, 46);
        top.addView(emo, Ui.lpm(c, es, es, 0, 0, 12, 0));
        LinearLayout mid = Ui.v(c);
        mid.addView(Ui.bold(Ui.tv(c, h.name, 17, Ui.col(c, R.color.text))));
        int cur = Streak.current(h.days, today);
        int best = Streak.best(h.days);
        mid.addView(Ui.tv(c, "🔥 连续 " + cur + " 天 · 最佳 " + best + " 天 · 累计 " + h.days.size() + " 天", 12,
                Ui.col(c, R.color.sub)));
        top.addView(mid, Ui.weight(1));

        TextView btn = Ui.bold(Ui.tv(c, on ? "✓" : "打卡", on ? 22 : 14, on ? Color.WHITE : green));
        btn.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable g = Ui.oval(on ? green : Color.TRANSPARENT);
        g.setStroke(Ui.dp(c, 2), green);
        btn.setBackground(Ui.ripple(g, Ui.oval(Color.BLACK)));
        btn.setOnClickListener(v -> toggle(h, today));
        int bs = Ui.dp(c, 52);
        top.addView(btn, Ui.lp(bs, bs));
        card.addView(top);

        // 最近 7 天（可补卡）
        LinearLayout week = Ui.h(c);
        week.setPadding(0, Ui.dp(c, 14), 0, 0);
        for (int i = 6; i >= 0; i--) {
            final int d = Day.add(today, -i);
            boolean hit = h.days.contains(d);
            LinearLayout cell = Ui.v(c);
            cell.setGravity(Gravity.CENTER);
            TextView wk = Ui.tv(c, i == 0 ? "今" : Day.week(d).substring(1), 11, Ui.col(c, R.color.sub));
            wk.setGravity(Gravity.CENTER);
            TextView dot = Ui.tv(c, hit ? "✓" : String.valueOf(Day.dom(d)), 12, hit ? Color.WHITE : Ui.col(c, R.color.sub));
            dot.setGravity(Gravity.CENTER);
            dot.setBackground(Ui.oval(hit ? green : Ui.col(c, R.color.field)));
            int ds = Ui.dp(c, 30);
            cell.addView(wk);
            cell.addView(dot, Ui.lpm(c, ds, ds, 0, 4, 0, 0));
            cell.setOnClickListener(v -> toggle(h, d));
            week.addView(cell, Ui.weight(1));
        }
        card.addView(week);

        // 目标进度
        int total = h.days.size();
        TextView goal = Ui.tv(c, total >= h.target ? "🏆 已达成 " + h.target + " 天目标！继续保持"
                : "🎯 目标 " + h.target + " 天 · 还差 " + (h.target - total) + " 天", 12, Ui.col(c, R.color.sub));
        goal.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 6));
        card.addView(goal);
        LinearLayout box = Ui.h(c);
        box.setBackground(Ui.rr(Ui.col(c, R.color.field), Ui.dp(c, 3)));
        View bar = new View(c);
        bar.setBackground(Ui.rr(green, Ui.dp(c, 3)));
        box.addView(bar, new LinearLayout.LayoutParams(0, Ui.dp(c, 6), 0));
        card.addView(box, Ui.lp(Ui.MATCH, Ui.dp(c, 6)));
        setBar(box, bar, (float) total / Math.max(1, h.target));

        card.setOnLongClickListener(v -> {
            Ui.dialog(c).setTitle(h.emoji + " " + h.name)
                    .setItems(new String[]{"编辑", "删除"}, (d, w) -> {
                        if (w == 0) edit(h);
                        else Ui.dialog(c).setTitle("删除「" + h.name + "」？").setMessage("打卡记录会一起删除。")
                                .setPositiveButton("删除", (d2, w2) -> {
                                    Store st = Store.get(c);
                                    st.habits.remove(h);
                                    st.saveHabits();
                                    refresh();
                                }).setNegativeButton("取消", null).show();
                    }).show();
            return true;
        });
        return card;
    }

    private void toggle(Store.Habit h, int day) {
        Store s = Store.get(act);
        boolean before = h.days.size() >= h.target;
        s.toggleHabit(h, day);
        Notify.tick(act);
        if (h.days.contains(day)) {
            if (!before && h.days.size() >= h.target) Ui.toast(act, "🏆 达成 " + h.target + " 天目标！");
            else if (day == Day.today()) Ui.toast(act, h.emoji + " 打卡成功，连续 " + Streak.current(h.days, day) + " 天");
        }
        refresh();
    }

    private void edit(final Store.Habit h) {
        final MainActivity c = act;
        final boolean isNew = h == null;
        final String[] emo = {isNew ? EMOJI[0] : h.emoji};
        final int[] target = {isNew ? 21 : h.target};

        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.v(c);
        int p = Ui.dp(c, 20);
        col.setPadding(p, Ui.dp(c, 8), p, Ui.dp(c, 8));
        sv.addView(col);
        final EditText name = Ui.field(c, "习惯名称，比如：阅读 20 分钟");
        if (!isNew) name.setText(h.name);
        col.addView(name, Ui.lp(Ui.MATCH, Ui.WRAP));

        col.addView(Ui.section(c, "图标"));
        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(8);
        final TextView[] cells = new TextView[EMOJI.length];
        for (int i = 0; i < EMOJI.length; i++) {
            final int k = i;
            cells[i] = Ui.tv(c, EMOJI[i], 20, Ui.col(c, R.color.text));
            cells[i].setGravity(Gravity.CENTER);
            cells[i].setOnClickListener(v -> {
                emo[0] = EMOJI[k];
                for (int j = 0; j < cells.length; j++) markEmoji(cells[j], EMOJI[j].equals(emo[0]));
            });
            markEmoji(cells[i], EMOJI[i].equals(emo[0]));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = Ui.dp(c, 36);
            lp.height = Ui.dp(c, 36);
            lp.setMargins(Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2));
            grid.addView(cells[i], lp);
        }
        col.addView(grid);

        col.addView(Ui.section(c, "目标天数"));
        LinearLayout tr = Ui.h(c);
        final TextView[] tc = new TextView[TARGETS.length];
        for (int i = 0; i < TARGETS.length; i++) {
            final int k = i;
            tc[i] = Ui.chip(c, String.valueOf(TARGETS[i]));
            tc[i].setPadding(Ui.dp(c, 10), Ui.dp(c, 7), Ui.dp(c, 10), Ui.dp(c, 7));
            tc[i].setOnClickListener(v -> { target[0] = TARGETS[k]; for (int j = 0; j < tc.length; j++) Ui.setChip(c, tc[j], j == k); });
            Ui.setChip(c, tc[i], TARGETS[i] == target[0]);
            tr.addView(tc[i], Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 6, 0));
        }
        col.addView(tr);

        final AlertDialog dlg = Ui.dialog(c).setTitle(isNew ? "新习惯" : "编辑习惯").setView(sv)
                .setPositiveButton("保存", null).setNegativeButton("取消", null).create();
        dlg.setOnShowListener(di -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = name.getText().toString().trim();
            if (s.isEmpty()) { name.setError("起个名字"); return; }
            Store st = Store.get(c);
            Store.Habit x = h;
            if (isNew) {
                x = new Store.Habit();
                x.id = st.nextId();
                x.created = System.currentTimeMillis();
                st.habits.add(x);
            }
            x.name = s;
            x.emoji = emo[0];
            x.target = target[0];
            st.saveHabits();
            dlg.dismiss();
            refresh();
        }));
        dlg.show();
    }

    private void markEmoji(TextView t, boolean sel) {
        int r = Ui.dp(act, 10);
        t.setBackground(sel ? Ui.rrs(Ui.col(act, R.color.brand_soft), r, Ui.col(act, R.color.brand), Ui.dp(act, 2))
                : Ui.rr(Color.TRANSPARENT, r));
    }

    @Override
    void onShow() { refresh(); }

}
