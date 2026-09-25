package com.aidemo.studytime;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * 单 Activity 四个页签：专注 / 待办 / 日历 / 坚持。
 *
 * 计时用墙钟（endAt - now），进程被杀或锁屏都不丢：onResume 先补账再刷新。
 * 页签内容全部用代码拼（参考仓同思路），XML 只留主题与颜色。
 */
public class MainActivity extends Activity {
    static final int TAB_FOCUS = 0, TAB_TODO = 1, TAB_CAL = 2, TAB_STREAK = 3;

    Cfg cfg;
    Todo todo;
    Recs recs;
    Pomo pomo;
    int bindId;

    LinearLayout root;
    FrameLayout content;
    final TextView[] tabBtn = new TextView[4];
    final View[] tabViews = new View[4];
    int cur = TAB_FOCUS;

    // 专注页
    RingView ring;
    TextView focusTop, phaseHint, taskStrip, cycleLine, btnMain, btnSecond;
    // 待办页
    EditText input;
    TextView chipAll, chipOpen, chipDone;
    LinearLayout list;
    int filter = 0; // 0 全部 1 未完成 2 已完成
    // 日历页
    TextView calTitle, calDetail;
    CalView cal;
    int viewY, viewM;
    String selDay;
    // 坚持页
    TextView streakNum, streakCaption, todayGoalText, meterLabel, weekTotal, mottoLine;
    MeterView meter;
    WeekBarsView bars;
    TextView[] statPomos, statMinutes, statTasks, statBest;

    final Handler handler = new Handler(Looper.getMainLooper());
    boolean resumed = false;
    boolean ticking = false;
    boolean keepOnFlag = false;
    /** 页面活着的引用：AlarmReceiver 结算完通知 UI 刷新 */
    static MainActivity live;

    final Runnable ticker = new Runnable() {
        @Override public void run() {
            ticking = false;
            if (!resumed) return;
            long now = System.currentTimeMillis();
            boolean changed = handleTick(now);
            if (cur == TAB_FOCUS || changed) refreshFocus();
            if (changed) refreshCurrent();
            if (pomo.running) startTicker();
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        live = this;

        // 数据由 Ctl 进程内共享（页面/闹钟接收器同一份，避免两边各记一次）
        Ctl.ensure();
        pullCtl();

        // 13+ 通知运行时授权（到点提醒用；拒绝也不影响计时，receiver 会用声音兜底）
        if (android.os.Build.VERSION.SDK_INT >= 33 && !Notify.granted(this)) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        String today = Cal.todayKey();
        selDay = today;
        java.util.Calendar now = java.util.Calendar.getInstance();
        viewY = now.get(java.util.Calendar.YEAR);
        viewM = now.get(java.util.Calendar.MONTH);

        buildSkeleton();
        selectTab(TAB_FOCUS);
        refreshAll();
    }

    @Override protected void onDestroy() {
        if (live == this) live = null;
        super.onDestroy();
    }

    /** 从 Ctl 拉最新引用（设置页改完配置 / receiver 可能换过对象） */
    void pullCtl() {
        cfg = Ctl.cfg;
        todo = Ctl.todo;
        recs = Ctl.recs;
        pomo = Ctl.pomo;
        bindId = Ctl.bindId;
    }

    /** 改绑定任务的唯一入口：三处状态一起走 */
    void setBind(int id) {
        bindId = id;
        Ctl.bindId = id;
        Prefs.saveBind(id);
    }

    /** AlarmReceiver 结算完的回声：刷新页面；在前台就补一份即时反馈 */
    public void refreshFromAlarm(int[] evs) {
        if (isFinishing()) return;
        refreshAll();
        if (resumed) {
            for (int ev : evs) feedback(ev);
            startTicker();
        }
    }

    // ── 骨架：顶栏 + 内容 + 底部页签 ────────────────────────────────────────
    void buildSkeleton() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.col(this, R.color.bg));
        setContentView(root);

        // 顶栏
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 6));
        TextView title = Ui.text(this, "番茄时光", 21, Ui.col(this, R.color.text));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView gear = Ui.text(this, "⚙", 22, Ui.col(this, R.color.text2));
        gear.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4));
        gear.setOnClickListener(v -> startActivity(new android.content.Intent(this, SettingsActivity.class)));
        top.addView(gear);
        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 内容区（四页共用，切页签换可见性）
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        tabViews[TAB_FOCUS] = buildFocusTab();
        tabViews[TAB_TODO] = buildTodoTab();
        tabViews[TAB_CAL] = buildCalTab();
        tabViews[TAB_STREAK] = buildStreakTab();
        for (View v : tabViews) {
            v.setVisibility(View.GONE);
            content.addView(v, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        // 底部页签条
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Ui.col(this, R.color.card));
        String[][] tabs = {{"⏱\n专注", "专注"}, {"☑\n待办", "待办"}, {"📅\n日历", "日历"}, {"🔥\n坚持", "坚持"}};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView t = Ui.text(this, tabs[i][0].replace('\n', ' '), 13, Ui.col(this, R.color.text2));
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 12));
            t.setOnClickListener(v -> selectTab(idx));
            nav.addView(t, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tabBtn[i] = t;
        }
        root.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    void selectTab(int i) {
        cur = i;
        for (int k = 0; k < 4; k++) {
            tabViews[k].setVisibility(k == i ? View.VISIBLE : View.GONE);
            TextView b = tabBtn[k];
            b.setTextColor(Ui.col(this, k == i ? R.color.accent : R.color.text2));
            b.setTypeface(null, k == i ? Typeface.BOLD : Typeface.NORMAL);
        }
        refreshCurrent();
        if (i == TAB_FOCUS) startTicker();
    }

    void refreshCurrent() {
        if (cur == TAB_FOCUS) refreshFocus();
        else if (cur == TAB_TODO) refreshTodos();
        else if (cur == TAB_CAL) refreshCal();
        else refreshStreak();
    }

    void refreshAll() {
        refreshFocus();
        refreshTodos();
        refreshCal();
        refreshStreak();
    }

    // ── 计时心跳 ────────────────────────────────────────────────────────────
    void startTicker() {
        if (ticking || !resumed || !pomo.running) return;
        ticking = true;
        handler.postDelayed(ticker, 250);
    }

    void stopTicker() {
        ticking = false;
        handler.removeCallbacks(ticker);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        Ctl.cfgChanged();   // 设置页可能改过配置
        pullCtl();
        // 杀后台/锁屏期间到点的账，回来一次补齐（补账规则见 Pomo.tick）
        boolean changed = handleTick(System.currentTimeMillis());
        Notify.cancelDone(this); // 人回来了，到点通知收掉
        refreshAll();
        if (changed || pomo.running) startTicker();
    }

    @Override protected void onPause() {
        resumed = false;
        stopTicker();
        setKeepOn(false);
        // 落盘 + 排好下一发闹钟（后台由 AlarmReceiver 接力）
        Ctl.savePomoArm(this);
        super.onPause();
    }

    /** 前台心跳：结算 + 落账 + 排闹钟 + 音效/Toast。返回是否动了数据。 */
    boolean handleTick(long now) {
        int[] evs = pomo.tick(now);
        if (!Ctl.recordEvents(this, evs, now)) return false;
        Ctl.savePomoArm(this);
        for (int ev : evs) feedback(ev);
        return true;
    }

    /** 即时反馈：提示音 + 振动 + 一句 Toast（通知渠道在后台负责响铃） */
    void feedback(int ev) {
        SoundFx.ding(this, cfg.sound, cfg.vibrate);
        Ui.toast(this, ev == Pomo.EV_WORK ? "🍅 番茄完成！休息一下吧" : "⏰ 休息结束，继续加油");
    }

    static String mmss(long ms) {
        long s = Math.max(0, ms) / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    // ── ① 专注页 ────────────────────────────────────────────────────────────
    View buildFocusTab() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 16));

        focusTop = Ui.text(this, "", 13, Ui.col(this, R.color.text2));
        focusTop.setGravity(Gravity.CENTER);
        v.addView(focusTop);

        ring = new RingView(this);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 320));
        rlp.topMargin = Ui.dp(this, 6);
        rlp.gravity = Gravity.CENTER_HORIZONTAL;
        v.addView(ring, rlp);

        phaseHint = Ui.text(this, "", 13, Ui.col(this, R.color.text2));
        phaseHint.setGravity(Gravity.CENTER);
        v.addView(phaseHint);

        taskStrip = new TextView(this);
        taskStrip.setTextSize(14);
        taskStrip.setGravity(Gravity.CENTER_VERTICAL);
        taskStrip.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        taskStrip.setBackground(Ui.round(this, Ui.col(this, R.color.chip), 12));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 14);
        v.addView(taskStrip, tlp);
        taskStrip.setOnClickListener(v1 -> openTaskPicker());

        cycleLine = Ui.text(this, "", 13, Ui.col(this, R.color.text2));
        cycleLine.setGravity(Gravity.CENTER);
        cycleLine.setPadding(0, Ui.dp(this, 12), 0, 0);
        v.addView(cycleLine);

        LinearLayout btnRow = Ui.row(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(this, 14);
        btnMain = Ui.btn(this, "开始专注", 16, Ui.col(this, R.color.accent), 0xFFFFFFFF);
        btnSecond = Ui.btn(this, "放弃", 15, Ui.col(this, R.color.chip), Ui.col(this, R.color.text));
        btnSecond.setPadding(Ui.dp(this, 14), Ui.dp(this, 11), Ui.dp(this, 14), Ui.dp(this, 11));
        LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        b2.leftMargin = Ui.dp(this, 10);
        btnRow.addView(btnMain, b1);
        btnRow.addView(btnSecond, b2);
        v.addView(btnRow, blp);

        btnMain.setOnClickListener(v1 -> onMainBtn());
        btnSecond.setOnClickListener(v1 -> onSecondBtn());

        return wrapScroll(v);
    }

    void onMainBtn() {
        long now = System.currentTimeMillis();
        if (pomo.state == Pomo.IDLE) {
            pomo.start(now);
        } else if (pomo.running) {
            pomo.pause(now);
        } else {
            pomo.resume(now);
        }
        Ctl.savePomoArm(this);
        refreshFocus();
        startTicker();
    }

    void onSecondBtn() {
        long now = System.currentTimeMillis();
        if (pomo.isBreak()) {
            // 休息中：第二键 = 跳过休息
            pomo.skip(now);
            Ctl.savePomoArm(this);
            refreshFocus();
            startTicker();
            return;
        }
        if (pomo.state == Pomo.IDLE) return;
        Ui.confirm(this, "放弃这一轮番茄？进度不计入今天的记录。", "放弃", () -> {
            pomo.reset();
            Ctl.savePomoArm(this);
            refreshFocus();
            refreshCurrent();
        });
    }

    void refreshFocus() {
        if (ring == null) return;
        long now = System.currentTimeMillis();
        long rem = pomo.remainingAt(now);
        long tot = pomo.totalMs();
        float frac = tot <= 0 ? 0 : rem / (float) tot;
        boolean isBreak = pomo.isBreak();
        String big;
        String sub;
        if (pomo.state == Pomo.IDLE) {
            big = mmss(cfg.workMs());
            sub = "准备开始";
        } else {
            big = mmss(rem);
            sub = pomo.running ? (isBreak ? (pomo.state == Pomo.LONG ? "长休息中" : "短休息中") : "专注中")
                               : (isBreak ? "休息已暂停" : "已暂停");
        }
        ring.setRing(pomo.state == Pomo.IDLE ? 1f : frac, big, sub,
                Ui.col(this, isBreak ? R.color.green : R.color.accent));

        int tp = recs.pomosOn(Cal.todayKey());
        setTxt(focusTop, String.format(Locale.CHINA, "今日番茄 %d/%d · 连续打卡 %d 天",
                tp, cfg.goalPomos, Streak.current(recs.days, Cal.todayKey(), cfg.goalPomos)));

        // 两个自动开关分开说，别让人误以为下一轮也会自己开跑
        String autoTxt = !cfg.auto ? " · 到点停下等我开始"
                : cfg.autoFocus ? " · 到点自动续" : " · 休息自动 · 下轮等我开始";
        setTxt(phaseHint, String.format(Locale.CHINA, "专注 %d 分钟 · 短休 %d · 长休 %d（每 %d 轮）%s",
                cfg.workMin, cfg.shortMin, cfg.longMin, cfg.longEvery, autoTxt));

        Todo.Task bind = bindId != 0 ? todo.byId(bindId) : null;
        if (bind == null) {
            if (bindId != 0) setBind(0);
            if (todo.countOpen() > 0) {
                setTxt(taskStrip, "选个待办当靶子 ›");
                taskStrip.setTextColor(Ui.col(this, R.color.text2));
            } else {
                setTxt(taskStrip, "去「待办」加一条，专注更有奔头 ›");
                taskStrip.setTextColor(Ui.col(this, R.color.text2));
            }
        } else {
            setTxt(taskStrip, "🎯 " + bind.title + (bind.est > 0 ? "（" + Math.min(bind.donePomos, bind.est) + "/" + bind.est + "）" : ""));
            taskStrip.setTextColor(Ui.col(this, R.color.text));
        }

        setTxt(cycleLine, String.format(Locale.CHINA, "本轮 %d/%d 个番茄", pomo.cycle, cfg.longEvery));

        if (pomo.state == Pomo.IDLE) {
            setTxt(btnMain, "开始专注");
            btnSecond.setVisibility(View.GONE);
        } else if (pomo.isBreak()) {
            setTxt(btnMain, pomo.running ? "暂停" : "继续");
            setTxt(btnSecond, "跳过休息");
            btnSecond.setVisibility(View.VISIBLE);
        } else {
            setTxt(btnMain, pomo.running ? "暂停" : "继续");
            setTxt(btnSecond, "放弃");
            btnSecond.setVisibility(View.VISIBLE);
        }
        // 屏幕常亮只在「计时中 + 专注页 + 你正开着」时生效
        setKeepOn(pomo.running && resumed && cur == TAB_FOCUS && cfg.keepOn);
    }

    /** 只在文案变化时才 setText：每 250ms 心跳不至于反复触发排版 */
    static void setTxt(android.widget.TextView v, CharSequence s) {
        CharSequence cur = v.getText();
        if (!android.text.TextUtils.equals(cur, s == null ? "" : s)) v.setText(s);
    }

    void setKeepOn(boolean on) {
        if (on == keepOnFlag) return;
        keepOnFlag = on;
        if (on) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    void openTaskPicker() {
        java.util.List<Todo.Task> open = new java.util.ArrayList<>();
        for (Todo.Task t : todo.tasks) if (!t.done) open.add(t);
        if (open.isEmpty()) {
            Ui.toast(this, "还没有未完成的待办，先去加一条");
            selectTab(TAB_TODO);
            return;
        }
        String[] names = new String[open.size() + 1];
        names[0] = "不绑定任务";
        for (int i = 0; i < open.size(); i++) {
            Todo.Task t = open.get(i);
            names[i + 1] = (bindId == t.id ? "🎯 " : "") + t.title;
        }
        int checked = 0;
        for (int i = 0; i < open.size(); i++) if (open.get(i).id == bindId) checked = i + 1;
        Ui.pick(this, "这次专注盯哪条？", names, checked, idx -> {
            setBind(idx == 0 ? 0 : open.get(idx - 1).id);
            refreshFocus();
        });
    }

    // ── ② 待办页 ────────────────────────────────────────────────────────────
    View buildTodoTab() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), 0);

        LinearLayout addRow = Ui.row(this);
        input = new EditText(this);
        input.setHint("加一条待办…");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setTextColor(Ui.col(this, R.color.text));
        input.setHintTextColor(Ui.col(this, R.color.text2));
        input.setBackground(null);
        addRow.addView(input, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView addBtn = Ui.btn(this, "＋ 加", 14, Ui.col(this, R.color.accent), 0xFFFFFFFF);
        addRow.addView(addBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        v.addView(addRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        input.setOnEditorActionListener((tv, actionId, ev) -> {
            addTask();
            return true;
        });
        addBtn.setOnClickListener(v1 -> addTask());

        // 过滤 chips
        LinearLayout chips = Ui.row(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(this, 10);
        chipAll = mkChip("全部");
        chipOpen = mkChip("未完成");
        chipDone = mkChip("已完成");
        chips.addView(chipAll, chipLp());
        chips.addView(chipOpen, chipLp());
        chips.addView(chipDone, chipLp());
        v.addView(chips, clp);
        chipAll.setOnClickListener(v1 -> { filter = 0; refreshTodos(); });
        chipOpen.setOnClickListener(v1 -> { filter = 1; refreshTodos(); });
        chipDone.setOnClickListener(v1 -> { filter = 2; refreshTodos(); });

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, Ui.dp(this, 8), 0, 0);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(list, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        v.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        return v;
    }

    LinearLayout.LayoutParams chipLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.rightMargin = Ui.dp(this, 8);
        return lp;
    }

    TextView mkChip(String s) {
        TextView t = Ui.text(this, s, 13, Ui.col(this, R.color.text2));
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 7), 0, Ui.dp(this, 7));
        t.setBackground(Ui.round(this, Ui.col(this, R.color.chip), 99));
        return t;
    }

    void chipState(TextView t, boolean on) {
        t.setBackground(Ui.round(this, Ui.col(this, on ? R.color.accent : R.color.chip), 99));
        t.setTextColor(on ? 0xFFFFFFFF : Ui.col(this, R.color.text2));
    }

    void addTask() {
        String s = input.getText() == null ? "" : input.getText().toString();
        Todo.Task t = todo.add(s, 0);
        if (t == null) {
            Ui.toast(this, "写点内容再加");
            return;
        }
        input.setText("");
        Prefs.saveTodo(todo);
        filter = 1;
        refreshTodos();
    }

    void refreshTodos() {
        if (list == null) return;
        chipState(chipAll, filter == 0);
        chipState(chipOpen, filter == 1);
        chipState(chipDone, filter == 2);
        int nAll = todo.tasks.size(), nOpen = todo.countOpen(), nDone = todo.countDone();
        chipAll.setText("全部 " + nAll);
        chipOpen.setText("未完成 " + nOpen);
        chipDone.setText("已完成 " + nDone);

        list.removeAllViews();
        boolean any = false;
        for (Todo.Task t : todo.tasks) {
            if (filter == 1 && t.done) continue;
            if (filter == 2 && !t.done) continue;
            any = true;
            list.addView(taskRow(t));
        }
        if (!any) {
            TextView empty = Ui.text(this,
                    filter == 2 ? "还没完成过任务" : filter == 1 ? "清单空了，休息一下？" : "还没有待办，先加一条吧",
                    14, Ui.col(this, R.color.text2));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 48), 0, 0);
            list.addView(empty);
        }
    }

    View taskRow(Todo.Task t) {
        LinearLayout row = Ui.row(this);
        row.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10));

        TextView mark = Ui.text(this, t.done ? "✓" : "○", 18,
                Ui.col(this, t.done ? R.color.green : R.color.text2));
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(Typeface.DEFAULT_BOLD);
        int mk = Ui.dp(this, 30);
        mark.setBackground(Ui.roundStroke(this, Ui.col(this, R.color.card),
                Ui.col(this, t.done ? R.color.green : R.color.text2), 99, 1.5f));
        row.addView(mark, new LinearLayout.LayoutParams(mk, mk));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        mlp.leftMargin = Ui.dp(this, 12);
        mlp.rightMargin = Ui.dp(this, 8);
        TextView title = Ui.text(this, t.title, 15,
                Ui.col(this, t.done ? R.color.text2 : R.color.text));
        if (t.done) title.setPaintFlags(title.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        if (bindId == t.id && !t.done) title.setTextColor(Ui.col(this, R.color.accent));
        // 纵排容器里的子项必须给全宽（width=0 是横排 weight 专用的，套进来会宽度归零看不见）
        mid.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        String meta;
        if (bindId == t.id && !t.done) meta = "🎯 当前专注 · ";
        else meta = "";
        if (t.est > 0) meta += "预计 " + t.est + " 个 · 已做 " + t.donePomos;
        else if (t.donePomos > 0) meta += "已做 " + t.donePomos + " 个番茄";
        else meta = meta.length() > 0 ? meta.substring(0, meta.length() - 3) : " ";
        TextView mt = Ui.text(this, meta, 12, Ui.col(this, R.color.text2));
        if (meta.trim().length() > 0 || bindId == t.id) {
            mid.addView(mt, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        row.addView(mid, mlp);

        TextView del = Ui.text(this, "✕", 15, Ui.col(this, R.color.text2));
        del.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        row.addView(del);

        View.OnClickListener toggle = v -> {
            boolean nowDone = !t.done;
            t.done = nowDone;
            if (nowDone && !t.counted) {
                t.counted = true;
                recs.addTask(Cal.todayKey());
                Prefs.saveRecs(recs);
                if (bindId == t.id) setBind(0);
            } else if (!nowDone && t.counted) {
                t.counted = false;
                String k = Cal.todayKey();
                if (recs.tasksOn(k) > 0) {
                    Recs.Day d = recs.at(k);
                    d.tasks--;
                    Prefs.saveRecs(recs);
                }
            } else if (nowDone && bindId == t.id) {
                setBind(0);
            }
            Prefs.saveTodo(todo);
            refreshTodos();
            refreshFocus();
        };
        mark.setOnClickListener(toggle);
        title.setOnClickListener(toggle);
        mid.setOnClickListener(toggle);
        del.setOnClickListener(v -> {
            if (bindId == t.id) setBind(0);
            todo.remove(t.id);
            Prefs.saveTodo(todo);
            refreshTodos();
            refreshFocus();
        });
        row.setOnLongClickListener(v -> {
            taskMenu(t);
            return true;
        });
        return row;
    }

    void taskMenu(Todo.Task t) {
        Ui.list(this, t.title, new String[]{"设为当前专注任务", "设预计番茄数（现在 " + t.est + "）", "删除这条"},
                idx -> {
                    if (idx == 0) {
                        if (t.done) { Ui.toast(this, "已完成的任务不用再设靶子"); return; }
                        setBind(t.id);
                        refreshFocus();
                        refreshTodos();
                    } else if (idx == 1) {
                        String[] nums = {"不设", "1", "2", "3", "4", "5", "6", "8", "10"};
                        int checked = 0;
                        String curEst = String.valueOf(t.est);
                        for (int i = 1; i < nums.length; i++) if (nums[i].equals(curEst)) checked = i;
                        Ui.pick(this, "预计几个番茄？", nums, checked, n -> {
                            t.est = n == 0 ? 0 : Integer.parseInt(nums[n]);
                            Prefs.saveTodo(todo);
                            refreshTodos();
                        });
                    } else {
                        if (bindId == t.id) setBind(0);
                        todo.remove(t.id);
                        Prefs.saveTodo(todo);
                        refreshTodos();
                        refreshFocus();
                    }
                });
    }

    // ── ③ 日历页 ────────────────────────────────────────────────────────────
    View buildCalTab() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));

        LinearLayout nav = Ui.row(this);
        TextView prev = Ui.text(this, "‹", 26, Ui.col(this, R.color.text2));
        TextView next = Ui.text(this, "›", 26, Ui.col(this, R.color.text2));
        prev.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 8), 0);
        next.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 16), 0);
        calTitle = Ui.text(this, "", 17, Ui.col(this, R.color.text));
        calTitle.setTypeface(Typeface.DEFAULT_BOLD);
        calTitle.setGravity(Gravity.CENTER);
        TextView backToday = Ui.text(this, "今天", 13, Ui.col(this, R.color.accent));
        backToday.setBackground(Ui.round(this, Ui.col(this, R.color.chip), 99));
        backToday.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        nav.addView(prev);
        nav.addView(calTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        nav.addView(next);
        nav.addView(backToday);
        v.addView(nav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        cal = new CalView(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(this, 6);
        cal.setOnCell(key -> {
            selDay = key;
            refreshCal();
        });
        v.addView(cal, clp);

        prev.setOnClickListener(v1 -> shiftMonth(-1));
        next.setOnClickListener(v1 -> shiftMonth(1));
        backToday.setOnClickListener(v1 -> {
            java.util.Calendar n = java.util.Calendar.getInstance();
            viewY = n.get(java.util.Calendar.YEAR);
            viewM = n.get(java.util.Calendar.MONTH);
            selDay = Cal.todayKey();
            refreshCal();
        });

        LinearLayout card = Ui.card(this, 14);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.topMargin = Ui.dp(this, 14);
        v.addView(card, plp);
        calDetail = Ui.text(this, "", 14, Ui.col(this, R.color.text));
        card.addView(calDetail);

        LinearLayout legend = Ui.row(this);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.topMargin = Ui.dp(this, 10);
        TextView lt = Ui.text(this, "少", 11, Ui.col(this, R.color.text2));
        legend.addView(lt);
        int[] heatIds = {R.color.heat0, R.color.heat1, R.color.heat2, R.color.heat3, R.color.heat4};
        for (int lvl = 0; lvl < heatIds.length; lvl++) {
            View sq = new View(this);
            sq.setBackground(Ui.round(this, Ui.col(this, heatIds[lvl]), 4));
            LinearLayout.LayoutParams sqp = new LinearLayout.LayoutParams(
                    Ui.dp(this, 16), Ui.dp(this, 16));
            sqp.leftMargin = Ui.dp(this, 4);
            sqp.rightMargin = Ui.dp(this, 4);
            legend.addView(sq, sqp);
        }
        legend.addView(Ui.text(this, "· 越深 = 离目标越近", 11, Ui.col(this, R.color.text2)));
        card.addView(legend, llp);

        // 小屏上整页可能超出一屏（网格 6 行 + 图例卡），给它兜一层滚动
        return wrapScroll(v);
    }

    void shiftMonth(int d) {
        viewM += d;
        if (viewM < 0) { viewM = 11; viewY--; }
        if (viewM > 11) { viewM = 0; viewY++; }
        refreshCal();
    }

    void refreshCal() {
        if (cal == null) return;
        calTitle.setText(Cal.monthTitle(viewY, viewM));
        cal.setMonth(viewY, viewM, recs, cfg.goalPomos, Cal.todayKey(), selDay);
        String k = selDay;
        int p = recs.pomosOn(k), m = recs.minutesOn(k), t = recs.tasksOn(k);
        String head = Cal.dayTitle(k);
        if (p == 0 && m == 0 && t == 0) {
            calDetail.setText(head + "\n这天没有学习记录");
        } else {
            calDetail.setText(String.format(Locale.CHINA,
                    "%s\n🍅 %d 个番茄 · 专注 %d 分钟 · 完成任务 %d 项", head, p, m, t));
        }
    }

    // ── ④ 坚持页 ────────────────────────────────────────────────────────────
    View buildStreakTab() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        v.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 20));

        // 卡一：连续打卡
        LinearLayout c1 = Ui.card(this, 16);
        LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        v.addView(c1, lp1);

        LinearLayout r1 = Ui.row(this);
        streakNum = Ui.mono(this, "🔥 0", 40, Ui.col(this, R.color.accent));
        r1.addView(streakNum);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        clp.leftMargin = Ui.dp(this, 14);
        streakCaption = Ui.text(this, "连续打卡（天）", 13, Ui.col(this, R.color.text2));
        col.addView(streakCaption);
        r1.addView(col, clp);
        c1.addView(r1);

        mottoLine = Ui.text(this, "", 14, Ui.col(this, R.color.text));
        mottoLine.setPadding(0, Ui.dp(this, 6), 0, 0);
        c1.addView(mottoLine);

        todayGoalText = Ui.text(this, "", 13, Ui.col(this, R.color.text2));
        todayGoalText.setPadding(0, Ui.dp(this, 12), 0, 0);
        c1.addView(todayGoalText);

        meter = new MeterView(this);
        c1.addView(meter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 12)));
        meterLabel = Ui.text(this, "", 12, Ui.col(this, R.color.text2));
        meterLabel.setPadding(0, Ui.dp(this, 6), 0, 0);
        c1.addView(meterLabel);

        // 卡二：本周
        LinearLayout c2 = Ui.card(this, 16);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = Ui.dp(this, 14);
        v.addView(c2, lp2);
        TextView h2 = Ui.text(this, "本周", 14, Ui.col(this, R.color.text));
        h2.setTypeface(Typeface.DEFAULT_BOLD);
        c2.addView(h2);
        bars = new WeekBarsView(this);
        c2.addView(bars, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));
        weekTotal = Ui.text(this, "", 13, Ui.col(this, R.color.text2));
        c2.addView(weekTotal);

        // 卡三：累计
        LinearLayout c3 = Ui.card(this, 16);
        LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp3.topMargin = Ui.dp(this, 14);
        v.addView(c3, lp3);
        TextView h3 = Ui.text(this, "累计", 14, Ui.col(this, R.color.text));
        h3.setTypeface(Typeface.DEFAULT_BOLD);
        c3.addView(h3);
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        c3.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout gr1 = Ui.row(this), gr2 = Ui.row(this);
        LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        cellLp.topMargin = Ui.dp(this, 10);
        statPomos = addStat(gr1, cellLp);
        statMinutes = addStat(gr1, copyLp(cellLp));
        statTasks = addStat(gr2, copyLp(cellLp));
        statBest = addStat(gr2, copyLp(cellLp));
        grid.addView(gr1);
        grid.addView(gr2);

        ScrollView wrap = new ScrollView(this);
        wrap.addView(v, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    LinearLayout.LayoutParams copyLp(LinearLayout.LayoutParams src) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(src.width, src.height, src.weight);
        lp.topMargin = src.topMargin;
        lp.leftMargin = src.leftMargin;
        return lp;
    }

    /** 累计格：大数字 + 小标签，返回 {数字, 标签} 两个 TextView */
    TextView[] addStat(LinearLayout parent, LinearLayout.LayoutParams lp) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        TextView val = Ui.mono(this, "0", 22, Ui.col(this, R.color.text));
        TextView lab = Ui.text(this, "", 12, Ui.col(this, R.color.text2));
        cell.addView(val);
        cell.addView(lab);
        parent.addView(cell, lp);
        return new TextView[]{val, lab};
    }

    void refreshStreak() {
        if (bars == null) return;
        String today = Cal.todayKey();
        int goal = cfg.goalPomos;
        int cur = Streak.current(recs.days, today, goal);
        int best = Streak.best(recs.days, goal);
        int tp = recs.pomosOn(today);
        boolean met = tp >= goal;

        streakNum.setText("🔥 " + cur);
        streakCaption.setText("连续打卡（天）· 最高 " + best + " 天");
        mottoLine.setText(Streak.motto(cur));
        if (met) {
            todayGoalText.setText("今天已达标 " + tp + "/" + goal + " 🎉 可以安心收工");
            meter.set(1f, Ui.col(this, R.color.green));
        } else {
            todayGoalText.setText(String.format(Locale.CHINA, "今天 %d/%d，还差 %d 个番茄达标",
                    tp, goal, goal - tp));
            meter.set(goal <= 0 ? 0 : tp / (float) goal, Ui.col(this, R.color.accent));
        }
        meterLabel.setText("今日进度 · 专注 " + recs.minutesOn(today) + " 分钟");

        // 本周七天
        String ws = Cal.weekStart(today);
        int[] vals = new int[7];
        int weekSum = 0;
        for (int i = 0; i < 7; i++) {
            vals[i] = recs.pomosOn(Cal.addDays(ws, i));
            weekSum += vals[i];
        }
        int todayCol = -1;
        for (int i = 0; i < 7; i++) if (Cal.addDays(ws, i).equals(today)) todayCol = i;
        bars.set(vals, goal, todayCol);
        weekTotal.setText(String.format(Locale.CHINA, "本周 %d 个番茄 · %.1f 小时",
                weekSum, weekSum * cfg.workMin / 60f));

        int[] tot = recs.totals();
        setStat(statPomos, tot[0] + "", "累计番茄");
        setStat(statMinutes, String.format(Locale.CHINA, "%.1f h", tot[1] / 60f), "累计专注");
        setStat(statTasks, tot[2] + "", "完成任务（次）");
        setStat(statBest, best + "", "最高连续（天）");
    }

    void setStat(TextView[] cell, String val, String lab) {
        cell[0].setText(val);
        cell[1].setText(lab);
    }

    // ── 小工具 ──────────────────────────────────────────────────────────────
    ScrollView wrapScroll(View child) {
        ScrollView s = new ScrollView(this);
        s.addView(child, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }
}
