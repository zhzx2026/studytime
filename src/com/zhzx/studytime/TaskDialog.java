package com.zhzx.studytime;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;

/** 新建 / 编辑待办（待办页、日历页、番茄钟共用）。 */
final class TaskDialog {
    private TaskDialog() {}

    interface Saved { void on(Store.Task t); }

    static void show(final MainActivity c, final Store.Task task, int defaultDue, final Saved cb) {
        final boolean isNew = task == null;
        final int[] due = {isNew ? defaultDue : task.due};
        final int[] pri = {isNew ? 0 : task.pri};
        final int[] est = {isNew ? 1 : Math.max(1, task.est)};

        ScrollView sv = new ScrollView(c);
        LinearLayout col = Ui.v(c);
        int p = Ui.dp(c, 20);
        col.setPadding(p, Ui.dp(c, 8), p, 0);
        sv.addView(col);

        final EditText title = Ui.field(c, "要做什么？");
        if (!isNew) title.setText(task.title);
        col.addView(title, Ui.lp(Ui.MATCH, Ui.WRAP));
        final EditText note = Ui.field(c, "备注（可选）");
        note.setSingleLine(false);
        note.setMinLines(2);
        note.setGravity(Gravity.TOP | Gravity.START);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        if (!isNew) note.setText(task.note);
        col.addView(note, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 10, 0, 0));

        // 优先级
        col.addView(Ui.section(c, "优先级"));
        LinearLayout priRow = Ui.h(c);
        final TextView[] pc = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int k = i;
            pc[i] = Ui.chip(c, TodoPage.PRI_NAME[i]);
            pc[i].setOnClickListener(v -> { pri[0] = k; for (int j = 0; j < 3; j++) Ui.setChip(c, pc[j], j == k); });
            priRow.addView(pc[i], Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
            Ui.setChip(c, pc[i], i == pri[0]);
        }
        col.addView(priRow);

        // 截止日
        col.addView(Ui.section(c, "截止日"));
        LinearLayout dueRow = Ui.h(c);
        final TextView dueTv = Ui.chip(c, "");
        final TextView today = Ui.chip(c, "今天");
        final TextView tmr = Ui.chip(c, "明天");
        final TextView none = Ui.chip(c, "不设");
        final Runnable showDue = () -> {
            int t = Day.today();
            dueTv.setText(due[0] == 0 ? "📅 选日期" : "📅 " + Day.rel(due[0], t));
            Ui.setChip(c, dueTv, due[0] != 0 && due[0] != t && due[0] != Day.add(t, 1));
            Ui.setChip(c, today, due[0] == t);
            Ui.setChip(c, tmr, due[0] == Day.add(t, 1));
            Ui.setChip(c, none, due[0] == 0);
        };
        today.setOnClickListener(v -> { due[0] = Day.today(); showDue.run(); });
        tmr.setOnClickListener(v -> { due[0] = Day.add(Day.today(), 1); showDue.run(); });
        none.setOnClickListener(v -> { due[0] = 0; showDue.run(); });
        dueTv.setOnClickListener(v -> {
            LocalDate d = due[0] == 0 ? LocalDate.now() : Day.of(due[0]);
            new DatePickerDialog(c, (dp, y, m, dd) -> { due[0] = y * 10000 + (m + 1) * 100 + dd; showDue.run(); },
                    d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        dueRow.addView(today, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        dueRow.addView(tmr, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        dueRow.addView(dueTv, Ui.lpm(c, Ui.WRAP, Ui.WRAP, 0, 0, 8, 0));
        dueRow.addView(none);
        col.addView(dueRow);
        showDue.run();

        // 预估番茄
        col.addView(Ui.section(c, "预估番茄数"));
        LinearLayout estRow = Ui.h(c);
        TextView minus = Ui.pill(c, "−", Ui.col(c, R.color.field), Ui.col(c, R.color.text));
        final TextView estTv = Ui.bold(Ui.tv(c, "", 18, Ui.col(c, R.color.text)));
        estTv.setGravity(Gravity.CENTER);
        TextView plus = Ui.pill(c, "＋", Ui.col(c, R.color.field), Ui.col(c, R.color.text));
        final Runnable showEst = () -> estTv.setText("🍅 × " + est[0]);
        minus.setOnClickListener(v -> { if (est[0] > 1) est[0]--; showEst.run(); });
        plus.setOnClickListener(v -> { if (est[0] < 20) est[0]++; showEst.run(); });
        estRow.addView(minus);
        estRow.addView(estTv, Ui.lp(Ui.dp(c, 96), Ui.WRAP));
        estRow.addView(plus);
        if (!isNew) {
            TextView doneTv = Ui.tv(c, "   已完成 " + task.pomos + " 个", 13, Ui.col(c, R.color.sub));
            estRow.addView(doneTv);
        }
        col.addView(estRow, Ui.lpm(c, Ui.MATCH, Ui.WRAP, 0, 0, 0, 12));
        showEst.run();

        AlertDialog.Builder b = Ui.dialog(c)
                .setTitle(isNew ? "新建待办" : "编辑待办")
                .setView(sv)
                .setPositiveButton("保存", null)
                .setNegativeButton("取消", null);
        if (!isNew) {
            b.setNeutralButton("删除", (d, w) -> {
                Store.get(c).deleteTask(task);
                if (cb != null) cb.on(task);
            });
        }
        final AlertDialog dlg = b.create();
        dlg.setOnShowListener(di -> dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String s = title.getText().toString().trim();
            if (s.isEmpty()) { title.setError("写点什么吧"); return; }
            Store st = Store.get(c);
            Store.Task t = isNew ? st.addTask(s, due[0]) : task;
            t.title = s;
            t.note = note.getText().toString().trim();
            t.due = due[0];
            t.pri = pri[0];
            t.est = est[0];
            st.saveTasks();
            dlg.dismiss();
            if (cb != null) cb.on(t);
        }));
        dlg.show();
        if (isNew) {
            title.requestFocus();
            if (dlg.getWindow() != null) {
                dlg.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
            }
        }
    }
}
