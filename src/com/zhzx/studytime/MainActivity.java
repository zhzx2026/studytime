package com.zhzx.studytime;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 单 Activity + 四个页签：专注（番茄钟）/ 待办 / 坚持 / 日历。 */
public class MainActivity extends Activity {
    static final String EXTRA_TAB = "tab";
    private static final String[] TAB_ICON = {"🍅", "✅", "🔥", "📅"};
    private static final String[] TAB_NAME = {"专注", "待办", "坚持", "日历"};

    private static MainActivity live;

    static void pingFromAlarm() {
        MainActivity a = live;
        if (a != null && a.cur >= 0) a.pages[a.cur].onAlarm();
    }

    Page[] pages;
    private FrameLayout host;
    private TextView title, subtitle;
    private final LinearLayout[] tabs = new LinearLayout[4];
    int cur = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        pages = new Page[]{new FocusPage(this), new TodoPage(this), new HabitPage(this), new CalendarPage(this)};

        LinearLayout root = Ui.v(this);
        root.setBackgroundColor(Ui.col(this, R.color.bg));

        // ---- 顶栏 ----
        LinearLayout head = Ui.h(this);
        head.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 6));
        LinearLayout tcol = Ui.v(this);
        title = Ui.bold(Ui.tv(this, "", 26, Ui.col(this, R.color.text)));
        subtitle = Ui.tv(this, "", 13, Ui.col(this, R.color.sub));
        tcol.addView(title);
        tcol.addView(subtitle);
        head.addView(tcol, Ui.weight(1));
        TextView gear = Ui.tv(this, "⚙", 22, Ui.col(this, R.color.sub));
        gear.setGravity(Gravity.CENTER);
        gear.setBackground(Ui.ripple(null, Ui.oval(0xFF000000)));
        gear.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        head.addView(gear, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        root.addView(head, Ui.lp(Ui.MATCH, Ui.WRAP));

        host = new FrameLayout(this);
        root.addView(host, new LinearLayout.LayoutParams(Ui.MATCH, 0, 1));

        // ---- 底部导航 ----
        LinearLayout nav = Ui.h(this);
        nav.setBackgroundColor(Ui.col(this, R.color.card));
        nav.setElevation(Ui.dp(this, 8));
        nav.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout t = Ui.v(this);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
            t.setBackground(Ui.ripple(null, Ui.rr(0xFF000000, Ui.dp(this, 12))));
            TextView ic = Ui.tv(this, TAB_ICON[i], 20, Ui.col(this, R.color.text));
            ic.setGravity(Gravity.CENTER);
            TextView nm = Ui.tv(this, TAB_NAME[i], 12, Ui.col(this, R.color.sub));
            nm.setGravity(Gravity.CENTER);
            t.addView(ic);
            t.addView(nm);
            t.setOnClickListener(v -> select(idx));
            tabs[i] = t;
            nav.addView(t, Ui.weight(1));
        }
        root.addView(nav, Ui.lp(Ui.MATCH, Ui.WRAP));
        setContentView(root);

        int tab = 0;
        if (b != null) tab = b.getInt(EXTRA_TAB, 0);
        else if (getIntent() != null) tab = getIntent().getIntExtra(EXTRA_TAB, 0);
        select(tab);

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED
                && !Store.get(this).getBool("asked_notif", false)) {
            Store.get(this).putBool("asked_notif", true);
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 1);
        }
        Update.autoCheck(this);
    }

    void select(int i) {
        if (i < 0 || i > 3) i = 0;
        if (i == cur) { pages[i].onShow(); return; }
        if (cur >= 0) pages[cur].onHide();
        cur = i;
        host.removeAllViews();
        host.addView(pages[i].view(), new FrameLayout.LayoutParams(Ui.MATCH, Ui.MATCH));
        title.setText(pages[i].title());
        subtitle.setText(Day.label(Day.today()));
        for (int k = 0; k < 4; k++) {
            TextView nm = (TextView) tabs[k].getChildAt(1);
            boolean sel = k == i;
            nm.setTextColor(Ui.col(this, sel ? R.color.brand : R.color.sub));
            nm.setTypeface(sel ? android.graphics.Typeface.DEFAULT_BOLD : android.graphics.Typeface.DEFAULT);
            tabs[k].getChildAt(0).setAlpha(sel ? 1f : 0.55f);
        }
        pages[i].onShow();
    }

    /** 从待办/日历点「专注这个」→ 关联任务并切到番茄钟 */
    void focusOn(long taskId) {
        Pomo p = PomoCtl.get(this);
        p.taskId = taskId;
        PomoCtl.save(this, p);
        select(0);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.hasExtra(EXTRA_TAB)) select(intent.getIntExtra(EXTRA_TAB, 0));
    }

    @Override
    protected void onResume() {
        super.onResume();
        live = this;
        Notify.cancelDone(this);
        subtitle.setText(Day.label(Day.today()));
        if (cur >= 0) pages[cur].onShow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        live = null;
        if (cur >= 0) pages[cur].onHide();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(EXTRA_TAB, Math.max(0, cur));
    }

    @Override
    public void onBackPressed() {
        if (cur != 0) select(0);
        else super.onBackPressed();
    }

    /** 给子页用：隐藏的 View 参数 */
    static View gone(View v) {
        v.setVisibility(View.GONE);
        return v;
    }
}
