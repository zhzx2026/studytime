package com.zhzx.studytime;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** 设置：番茄钟时长 / 自动续 / 提醒 / 备份恢复 / 检查更新 / 关于。 */
public class SettingsActivity extends Activity {
    private LinearLayout col;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.col(this, R.color.bg));
        col = Ui.v(this);
        col.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 32));
        sv.addView(col);

        LinearLayout head = Ui.h(this);
        TextView back = Ui.bold(Ui.tv(this, "‹", 30, Ui.col(this, R.color.text)));
        back.setGravity(Gravity.CENTER);
        back.setBackground(Ui.ripple(null, Ui.oval(Color.BLACK)));
        back.setOnClickListener(v -> finish());
        head.addView(back, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        head.addView(Ui.bold(Ui.tv(this, "设置", 24, Ui.col(this, R.color.text))));
        col.addView(head);

        final Store s = Store.get(this);

        col.addView(Ui.section(this, "番茄钟"));
        LinearLayout pomo = Ui.card(this);
        pomo.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 4));
        pomo.addView(stepper("专注时长", Store.K_FOCUS, 25, 5, 90, 5, "分钟"));
        pomo.addView(Ui.divider(this));
        pomo.addView(stepper("短休息", Store.K_SHORT, 5, 1, 30, 1, "分钟"));
        pomo.addView(Ui.divider(this));
        pomo.addView(stepper("长休息", Store.K_LONG, 15, 5, 60, 5, "分钟"));
        pomo.addView(Ui.divider(this));
        pomo.addView(stepper("每几个番茄长休一次", Store.K_EVERY, 4, 2, 8, 1, "个"));
        pomo.addView(Ui.divider(this));
        pomo.addView(toggle("专注结束自动开始休息", Store.K_AUTO_BREAK, true));
        pomo.addView(Ui.divider(this));
        pomo.addView(toggle("休息结束自动开始下一个番茄", Store.K_AUTO_FOCUS, false));
        col.addView(pomo);

        col.addView(Ui.section(this, "提醒"));
        LinearLayout rem = Ui.card(this);
        rem.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 4));
        rem.addView(toggle("振动", Store.K_VIBRATE, true));
        rem.addView(Ui.divider(this));
        rem.addView(toggle("提示音（App 在前台时）", Store.K_SOUND, true));
        rem.addView(Ui.divider(this));
        rem.addView(toggle("计时中保持屏幕常亮", Store.K_KEEP_ON, false));
        rem.addView(Ui.divider(this));
        rem.addView(link("通知设置（到点提醒的铃声 / 横幅）", v -> {
            Intent i = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            i.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            try { startActivity(i); } catch (RuntimeException e) { Ui.toast(this, "打不开系统通知设置"); }
        }));
        col.addView(rem);

        col.addView(Ui.section(this, "数据"));
        LinearLayout data = Ui.card(this);
        data.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 4));
        data.addView(link("复制备份到剪贴板", v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null) return;
            cm.setPrimaryClip(ClipData.newPlainText("studytime-backup", s.exportJson()));
            Ui.toast(this, "已复制：待办 " + s.tasks.size() + " · 习惯 " + s.habits.size() + " · 专注 " + s.sessions.size() + " 条");
        }));
        data.addView(Ui.divider(this));
        data.addView(link("从剪贴板恢复（覆盖本机数据）", v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            CharSequence txt = null;
            if (cm != null && cm.getPrimaryClip() != null && cm.getPrimaryClip().getItemCount() > 0) {
                txt = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            }
            final String text = txt == null ? "" : txt.toString();
            Ui.dialog(this).setTitle("用剪贴板里的备份覆盖本机数据？")
                    .setMessage("本机现有的待办、习惯、专注记录会被替换。")
                    .setPositiveButton("恢复", (d, w) -> Ui.toast(this, s.importJson(text) ? "恢复成功" : "剪贴板里不是学习时光的备份"))
                    .setNegativeButton("取消", null).show();
        }));
        col.addView(data);

        col.addView(Ui.section(this, "关于与更新"));
        LinearLayout about = Ui.card(this);
        about.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 4));
        about.addView(channelRow());
        about.addView(Ui.divider(this));
        about.addView(link("检查更新", v -> Update.check(this, false)));
        col.addView(about);

        TextView foot = Ui.tv(this, "学习时光 v" + Update.versionName(this) + "（code " + Update.versionCode(this) + "）\n"
                + "构建 " + BuildInfo.STAMP + "\n番茄钟 · 待办 · 坚持 · 日历 — 纯离线，数据只存在本机", 12, Ui.col(this, R.color.sub));
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, Ui.dp(this, 20), 0, 0);
        col.addView(foot, Ui.lp(Ui.MATCH, Ui.WRAP));

        setContentView(sv);
    }

    private View row(String label) {
        LinearLayout r = Ui.h(this);
        r.setMinimumHeight(Ui.dp(this, 52));
        r.addView(Ui.tv(this, label, 15, Ui.col(this, R.color.text)), Ui.weight(1));
        return r;
    }

    private View stepper(String label, final String key, int def, final int min, final int max, final int step, final String unit) {
        LinearLayout r = (LinearLayout) row(label);
        final Store s = Store.get(this);
        final TextView val = Ui.bold(Ui.tv(this, "", 15, Ui.col(this, R.color.text)));
        val.setGravity(Gravity.CENTER);
        final int[] cur = {s.getInt(key, def)};
        final Runnable show = () -> val.setText(cur[0] + " " + unit);
        TextView minus = Ui.pill(this, "−", Ui.col(this, R.color.field), Ui.col(this, R.color.text));
        TextView plus = Ui.pill(this, "＋", Ui.col(this, R.color.field), Ui.col(this, R.color.text));
        minus.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4));
        plus.setPadding(Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4));
        minus.setOnClickListener(v -> {
            int n = cur[0] - step;
            if (cur[0] > min && n < min) n = min;
            if (n >= min) { cur[0] = n; s.putInt(key, n); show.run(); }
        });
        plus.setOnClickListener(v -> {
            int n = cur[0] + step;
            if (n <= max) { cur[0] = n; s.putInt(key, n); show.run(); }
        });
        r.addView(minus);
        r.addView(val, Ui.lp(Ui.dp(this, 74), Ui.WRAP));
        r.addView(plus);
        show.run();
        return r;
    }

    private View toggle(String label, final String key, boolean def) {
        LinearLayout r = (LinearLayout) row(label);
        final Switch sw = new Switch(this);
        sw.setChecked(Store.get(this).getBool(key, def));
        sw.setOnCheckedChangeListener((b, on) -> Store.get(this).putBool(key, on));
        r.addView(sw);
        r.setOnClickListener(v -> sw.toggle());
        return r;
    }

    private View link(String label, View.OnClickListener l) {
        LinearLayout r = (LinearLayout) row(label);
        r.addView(Ui.tv(this, "›", 20, Ui.col(this, R.color.sub)));
        r.setBackground(Ui.ripple(null, Ui.rr(Color.BLACK, Ui.dp(this, 8))));
        r.setOnClickListener(l);
        return r;
    }

    private View channelRow() {
        LinearLayout r = (LinearLayout) row("更新通道");
        final Store s = Store.get(this);
        final TextView a = Ui.chip(this, "正式版");
        final TextView t = Ui.chip(this, "测试版");
        final Runnable show = () -> {
            int ch = s.getInt(Store.K_CHANNEL, Update.defaultChannel(this));
            Ui.setChip(this, a, ch == 0);
            Ui.setChip(this, t, ch == 1);
        };
        a.setOnClickListener(v -> { s.putInt(Store.K_CHANNEL, 0); show.run(); });
        t.setOnClickListener(v -> { s.putInt(Store.K_CHANNEL, 1); show.run(); });
        r.addView(a, Ui.lpm(this, Ui.WRAP, Ui.WRAP, 0, 0, 6, 0));
        r.addView(t);
        show.run();
        return r;
    }
}
