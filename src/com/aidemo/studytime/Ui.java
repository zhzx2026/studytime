package com.aidemo.studytime;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 手搓 UI 小工具：dp/颜色/圆角底/文本/对话框，全部免 XML（和参考仓同一思路）。 */
public final class Ui {

    public interface Pick { void onPick(int index); }
    public interface Ok { void onOk(); }

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int col(Context c, int resId) {
        return c.getResources().getColor(resId, c.getTheme());
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    /** 圆角矩形 drawable */
    public static GradientDrawable round(Context c, int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radiusDp));
        return d;
    }

    /** 圆角矩形 + 描边 */
    public static GradientDrawable roundStroke(Context c, int color, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable d = round(c, color, radiusDp);
        d.setStroke(dp(c, strokeDp), strokeColor);
        return d;
    }

    public static TextView text(Context c, String s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView mono(Context c, String s, float sp, int color) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        return t;
    }

    /** 可点的文本按钮（纯 TextView + 圆角底，免控件样式表） */
    public static TextView btn(Context c, String label, float sp, int bg, int fg) {
        TextView t = text(c, label, sp, fg);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        t.setGravity(Gravity.CENTER);
        t.setBackground(round(c, bg, 14));
        t.setPadding(dp(c, 18), dp(c, 11), dp(c, 18), dp(c, 11));
        return t;
    }

    /** 卡片容器：白底圆角、内部纵排 */
    public static LinearLayout card(Context c, float padDp) {
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setBackground(round(c, col(c, R.color.card), 16));
        int p = dp(c, padDp);
        ll.setPadding(p, p, p, p);
        return ll;
    }

    public static LinearLayout row(Context c) {
        LinearLayout ll = new LinearLayout(c);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_VERTICAL);
        return ll;
    }

    public static View vline(Context c) {
        View v = new View(c);
        v.setBackgroundColor(col(c, R.color.div));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1));
        v.setLayoutParams(lp);
        return v;
    }

    /** 单选弹窗（设置项都走它） */
    public static void pick(Context c, String title, String[] items, int checked, Pick cb) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setSingleChoiceItems(items, checked, (d, which) -> {
                    cb.onPick(which);
                    d.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /** 确认弹窗 */
    public static void confirm(Context c, String msg, String okText, Ok ok) {
        new AlertDialog.Builder(c)
                .setMessage(msg)
                .setPositiveButton(okText, (d, which) -> ok.onOk())
                .setNegativeButton("再想想", null)
                .show();
    }

    /** 列表弹窗 */
    public static void list(Context c, String title, String[] items, Pick cb) {
        new AlertDialog.Builder(c)
                .setTitle(title)
                .setItems(items, (d, which) -> cb.onPick(which))
                .setNegativeButton("取消", null)
                .show();
    }
}
