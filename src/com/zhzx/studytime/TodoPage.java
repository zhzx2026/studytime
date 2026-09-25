package com.zhzx.studytime;

import android.graphics.Color;
import android.graphics.Paint;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 页签 2：待办（TODO）。 */
final class TodoPage extends Page {
    static final int F_TODAY = 0, F_OPEN = 1, F_DONE = 2;
    private static final String[] F_NAME = {"今天", "全部待办", "已完成"};
    static final String[] PRI_NAME = {"普通", "重要", "紧急"};

    private int filter = F_TODAY;
    private final TextView[] chips = new TextView[3];
    private LinearLayout list;
    private EditText quick;
    private TextView countTv;

    TodoPage(MainActivity a) { super(a); }

    @Override String title() { return "待办"; }

    /** 各筛选下的排序结果（番茄钟选任务也用它）。 */
    static List<Store.Task> sorted(Store s, int filter) {
        int today = Day.today();
        List<Store.Task> out = new ArrayList<>();
        for (Store.Task t : s.tasks) {
            boolean keep;
            if (filter == F_TODAY) keep = (!t.done && t.due != 0 && t.due <= today) || (t.done && t.doneDay == today);
            else if (filter == F_OPEN) keep = !t.done;
            else keep = t.done;
            if (keep) out.add(t);
        }
        Collections.sort(out, (a, b) -> {
            if (a.done != b.done) return a.done ? 1 : -1;
            if (filter == F_DONE) return Integer.compare(b.doneDay, a.doneDay);
            int da = a.due == 0 ? Integer.MAX_VALUE : a.due, db = b.due == 0 ? Integer.MAX_VALUE : b.due;
            if (da != db) return Integer.compare(da, db);
            if (a.pri != b.pri) return Integer.compare(b.pri, a.pri);
            return Long.compare(a.created, b.created);
        });
        return out;
    }

    @Override
    protected View build() {
        final MainActivity c = act;
        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.v(c);
        col.setPadding(Ui.dp(c, 16), Ui.dp(c, 4), Ui.dp(c, 16), Ui.dp(c, 24));
        sv.addView(col);

        LinearLayout chipRow = Ui.h(c);
        for (int i = 0; i < 3; i++) {
            final int f = i;
            chips[i] = Ui.chip(c, F_NAME[i]);
            chips[i].setOnClickListener(v -> { filter = f; refresh(); });
            chipRow.addView(chips[i], Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        }
        View sp = new View(c);
        chipRow.addView(sp, Ui.weight(1));
        countTv = Ui.tv(c, "", 13, Ui.col(c, R.color.sub));
        chipRow.addView(countTv);
        col.addView(chipRow, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 0, 0, 10));

        // 快速添加
        LinearLayout add = Ui.h(c);
        quick = Ui.field(c, "添加一件要做的事…");
        quick.setImeOptions(EditorInfo.IME_ACTION_DONE);
        quick.setOnEditorActionListener((v, actionId, e) -> {
            boolean enter = e != null && e.getKeyCode() == KeyEvent.KEYCODE_ENTER && e.getAction() == KeyEvent.ACTION_DOWN;
            if (actionId == EditorInfo.IME_ACTION_DONE || enter) { quickAdd(); return true; }
            return false;
        });
        add.addView(quick, Ui.weight(1));
        TextView addBtn = Ui.pill(c, "＋", Ui.col(c, R.color.brand), Color.WHITE);
        addBtn.setTextSize(20);
        addBtn.setPadding(Ui.dp(c, 16), Ui.dp(c, 6), Ui.dp(c, 16), Ui.dp(c, 6));
        addBtn.setOnClickListener(v -> quickAdd());
        addBtn.setOnLongClickListener(v -> { TaskDialog.show(c, null, filter == F_TODAY ? Day.today() : 0, t -> refresh()); return true; });
        add.addView(addBtn, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 8, 0, 0, 0));
        col.addView(add, Ui.lp(Ui.MATCH, Ui.WRAP));
        TextView hint = Ui.tv(c, "回车快速添加 · 长按 ＋ 填写详情 · 点任务编辑 · 长按删除", 11, Ui.col(c, R.color.sub));
        hint.setPadding(Ui.dp(c, 4), Ui.dp(c, 6), 0, Ui.dp(c, 6));
        col.addView(hint);

        list = Ui.v(c);
        col.addView(list, Ui.lp(Ui.MATCH, Ui.WRAP));
        return sv;
    }

    private void quickAdd() {
        String s = quick.getText().toString().trim();
        if (s.isEmpty()) { TaskDialog.show(act, null, filter == F_TODAY ? Day.today() : 0, t -> refresh()); return; }
        Store.get(act).addTask(s, filter == F_TODAY ? Day.today() : 0);
        quick.setText("");
        InputMethodManager imm = (InputMethodManager) act.getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(quick.getWindowToken(), 0);
        refresh();
    }

    void refresh() {
        if (list == null) return;
        for (int i = 0; i < 3; i++) Ui.setChip(act, chips[i], i == filter);
        Store s = Store.get(act);
        List<Store.Task> items = sorted(s, filter);
        int open = 0;
        for (Store.Task t : items) if (!t.done) open++;
        countTv.setText(filter == F_DONE ? items.size() + " 项" : open + " 项未完成");
        list.removeAllViews();
        if (items.isEmpty()) {
            TextView e = Ui.tv(act, filter == F_TODAY ? "今天没有到期的待办 🎉\n（没写截止日的任务在「全部待办」里）"
                    : filter == F_OPEN ? "清单是空的，加一件想做的事吧" : "还没有完成的事项", 14, Ui.col(act, R.color.sub));
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, Ui.dp(act, 40), 0, 0);
            list.addView(e, Ui.lp(Ui.MATCH, Ui.WRAP));
            return;
        }
        for (Store.Task t : items) list.addView(row(t), Ui.lpm(act, Ui.MATCH, Ui.WRAP, 0, 8, 0, 0));
    }

    static TextView checkCircle(MainActivity c, boolean done, int color) {
        TextView ck = Ui.bold(Ui.tv(c, done ? "✓" : "", 15, Color.WHITE));
        ck.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable g = Ui.oval(done ? color : Color.TRANSPARENT);
        g.setStroke(Ui.dp(c, 2), done ? color : Ui.col(c, R.color.sub));
        ck.setBackground(g);
        return ck;
    }

    private View row(final Store.Task t) {
        final MainActivity c = act;
        int today = Day.today();
        LinearLayout r = Ui.h(c);
        r.setBackground(Ui.ripple(Ui.rr(Ui.col(c, R.color.card), Ui.dp(c, 14)), Ui.rr(Color.BLACK, Ui.dp(c, 14))));
        r.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12));

        int priColor = t.pri == 2 ? Ui.col(c, R.color.brand) : t.pri == 1 ? Ui.col(c, R.color.amber) : Ui.col(c, R.color.green);
        TextView ck = checkCircle(c, t.done, Ui.col(c, R.color.green));
        if (!t.done && t.pri > 0) {
            android.graphics.drawable.GradientDrawable g = Ui.oval(Color.TRANSPARENT);
            g.setStroke(Ui.dp(c, 2), priColor);
            ck.setBackground(g);
        }
        ck.setOnClickListener(v -> {
            Store.get(c).setDone(t, !t.done);
            Notify.tick(c);
            if (t.done) Ui.toast(c, "完成 ✓ " + t.title);
            refresh();
        });
        int s = Ui.dp(c, 26);
        r.addView(ck, Ui.lpm(c, s, s, 0, 0, 12, 0));

        LinearLayout mid = Ui.v(c);
        TextView title = Ui.tv(c, t.title, 16, Ui.col(c, t.done ? R.color.sub : R.color.text));
        if (t.done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        mid.addView(title);
        StringBuilder meta = new StringBuilder();
        boolean overdue = !t.done && t.due != 0 && t.due < today;
        if (t.due != 0) meta.append(overdue ? "⚠ 已过期 · " : "📅 ").append(Day.rel(t.due, today)).append("   ");
        if (t.pri > 0) meta.append(PRI_NAME[t.pri]).append("   ");
        meta.append("🍅 ").append(t.pomos).append("/").append(t.est);
        if (!t.note.isEmpty()) meta.append("   📝 ").append(t.note.replace('\n', ' '));
        TextView mt = Ui.tv(c, meta.toString(), 12, overdue ? Ui.col(c, R.color.brand) : Ui.col(c, R.color.sub));
        mt.setSingleLine(true);
        mt.setEllipsize(android.text.TextUtils.TruncateAt.END);
        mt.setPadding(0, Ui.dp(c, 3), 0, 0);
        mid.addView(mt);
        r.addView(mid, Ui.weight(1));

        if (!t.done) {
            TextView play = Ui.tv(c, "▶", 14, Ui.col(c, R.color.brand));
            play.setGravity(Gravity.CENTER);
            play.setBackground(Ui.ripple(Ui.oval(Ui.col(c, R.color.brand_soft)), Ui.oval(Color.BLACK)));
            play.setOnClickListener(v -> c.focusOn(t.id));
            int ps = Ui.dp(c, 36);
            r.addView(play, Ui.lpm(c, ps, ps, 8, 0, 0, 0));
        }

        r.setOnClickListener(v -> TaskDialog.show(c, t, 0, x -> refresh()));
        r.setOnLongClickListener(v -> {
            Ui.dialog(c).setTitle("删除「" + t.title + "」？")
                    .setPositiveButton("删除", (d, w) -> { Store.get(c).deleteTask(t); refresh(); })
                    .setNegativeButton("取消", null).show();
            return true;
        });
        return r;
    }

    @Override
    void onShow() { refresh(); }
}
