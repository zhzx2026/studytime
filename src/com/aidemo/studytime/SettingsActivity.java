package com.aidemo.studytime;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/**
 * 设置：番茄节奏 / 每日目标 / 提示 / 数据 / 关于。
 * 改完立刻落盘，返回首页即生效（MainActivity.onResume 重新拉配置）。
 */
public class SettingsActivity extends Activity {
    Cfg cfg;
    LinearLayout body;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Ctl.ensure();
        cfg = Ctl.cfg; // 直接改进程里那一份，保存后 Ctl.cfgChanged() 让 pomo 跟上

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.col(this, R.color.bg));
        setContentView(root);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 14), Ui.dp(this, 6));
        TextView back = Ui.text(this, "‹", 26, Ui.col(this, R.color.accent));
        back.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 14), 0);
        back.setOnClickListener(v -> finish());
        TextView title = Ui.text(this, "设置", 19, Ui.col(this, R.color.text));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        top.addView(back);
        top.addView(title);
        root.addView(top, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(this, 20), Ui.dp(this, 4), Ui.dp(this, 20), Ui.dp(this, 24));

        section("番茄节奏");
        pickRow("专注时长", cfg.workMin + " 分钟", Cfg.WORK_CHOICES, cfg.workMin, v -> {
            cfg.workMin = v; save();
        });
        pickRow("短休息", cfg.shortMin + " 分钟", Cfg.SHORT_CHOICES, cfg.shortMin, v -> {
            cfg.shortMin = v; save();
        });
        pickRow("长休息", cfg.longMin + " 分钟", Cfg.LONG_CHOICES, cfg.longMin, v -> {
            cfg.longMin = v; save();
        });
        pickRow("每几轮放一次长休", cfg.longEvery + " 轮", Cfg.EVERY_CHOICES, cfg.longEvery, v -> {
            cfg.longEvery = v; save();
        });

        section("目标与提示");
        pickRow("每日目标", cfg.goalPomos + " 个番茄", Cfg.GOAL_CHOICES, cfg.goalPomos, v -> {
            cfg.goalPomos = v; save();
        });
        switchRow("专注结束自动开始休息", cfg.auto, on -> { cfg.auto = on; save(); });
        switchRow("休息结束自动开始下一轮", cfg.autoFocus, on -> { cfg.autoFocus = on; save(); });
        switchRow("计时中屏幕常亮", cfg.keepOn, on -> { cfg.keepOn = on; save(); });
        switchRow("完成提示音", cfg.sound, on -> { cfg.sound = on; save(); });
        switchRow("完成振动", cfg.vibrate, on -> { cfg.vibrate = on; save(); });

        section("数据");
        actionRow("清空全部数据", "番茄/待办/打卡全部归零", v ->
                Ui.confirm(this, "清空后番茄记录、待办、打卡连续都会消失，且无法恢复。确定？", "清空", () -> {
                    Prefs.clearAll();
                    Ui.toast(this, "已清空");
                    finish();
                }));

        section("关于");
        infoRow("版本", versionName() + "（" + BuildInfo.STAMP + "）");
        infoRow("包名", getPackageName());
        TextView about = Ui.text(this,
                "无 Gradle 工程：aapt2 + javac/ecj + d8 + apksigner 直出 APK；\n" +
                "全部数据只存在本机，不联网、不要权限（振动除外）。",
                12, Ui.col(this, R.color.text2));
        about.setPadding(0, Ui.dp(this, 12), 0, 0);
        body.addView(about);

        ScrollView wrap = new ScrollView(this);
        wrap.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(wrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
    }

    void save() {
        Prefs.saveCfg(cfg);
        Ctl.cfgChanged(); // 让 pomo/页面立刻用上新配置
    }

    String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    void section(String s) {
        TextView t = Ui.text(this, s, 13, Ui.col(this, R.color.accent));
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, Ui.dp(this, 20), 0, Ui.dp(this, 8));
        body.addView(t);
    }

    LinearLayout baseRow() {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        r.setBackground(Ui.round(this, Ui.col(this, R.color.card), 12));
        r.setPadding(Ui.dp(this, 14), Ui.dp(this, 13), Ui.dp(this, 14), Ui.dp(this, 13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 6);
        body.addView(r, lp);
        return r;
    }

    /** 单选行：标题 + 当前值 ›（checked 跟着最新值走，改过一次再点不会预选错） */
    void pickRow(String title, String value, int[] choices, int current, final ValCb cb) {
        LinearLayout r = baseRow();
        TextView tv = Ui.text(this, title, 15, Ui.col(this, R.color.text));
        r.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView val = Ui.text(this, value, 14, Ui.col(this, R.color.text2));
        r.addView(val);
        r.addView(Ui.text(this, " ›", 16, Ui.col(this, R.color.text2)));
        final int[] cur = {current};
        r.setOnClickListener(v -> {
            String[] items = new String[choices.length];
            int checked = 0;
            for (int i = 0; i < choices.length; i++) {
                items[i] = choices[i] + (title.contains("轮") ? " 轮" :
                        title.contains("目标") ? " 个番茄" : " 分钟");
                if (choices[i] == cur[0]) checked = i;
            }
            Ui.pick(this, title, items, checked, idx -> {
                cur[0] = choices[idx];
                cb.onVal(choices[idx]);
                val.setText(pickValueText(title, choices[idx]));
            });
        });
    }

    String pickValueText(String title, int v) {
        if (title.contains("轮")) return v + " 轮";
        if (title.contains("目标")) return v + " 个番茄";
        return v + " 分钟";
    }

    void switchRow(String title, boolean on, final SwitchCb cb) {
        LinearLayout r = baseRow();
        TextView tv = Ui.text(this, title, 15, Ui.col(this, R.color.text));
        r.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = new Switch(this);
        sw.setChecked(on);
        sw.setOnCheckedChangeListener((btn, checked) -> cb.onSwitch(checked));
        r.addView(sw);
    }

    void actionRow(String title, String sub, View.OnClickListener click) {
        LinearLayout r = baseRow();
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        TextView tv = Ui.text(this, title, 15, Ui.col(this, R.color.accent));
        TextView sv = Ui.text(this, sub, 12, Ui.col(this, R.color.text2));
        mid.addView(tv);
        mid.addView(sv);
        r.addView(mid, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.setOnClickListener(click);
    }

    void infoRow(String title, String value) {
        LinearLayout r = baseRow();
        TextView tv = Ui.text(this, title, 15, Ui.col(this, R.color.text));
        r.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView val = Ui.text(this, value, 13, Ui.col(this, R.color.text2));
        val.setGravity(Gravity.END);
        r.addView(val);
    }

    interface ValCb { void onVal(int v); }
    interface SwitchCb { void onSwitch(boolean on); }
}
