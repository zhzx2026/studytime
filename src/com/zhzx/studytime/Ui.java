package com.zhzx.studytime;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 纯代码搭界面用的小工具（无 XML 布局、无第三方库）。 */
final class Ui {
    private Ui() {}

    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static int col(Context c, int id) {
        return c.getColor(id);
    }

    static boolean night(Context c) {
        int m = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return m == Configuration.UI_MODE_NIGHT_YES;
    }

    static TextView tv(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(true);
        return t;
    }

    static TextView bold(TextView t) {
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    static GradientDrawable rr(int fill, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(radiusPx);
        return g;
    }

    static GradientDrawable rrs(int fill, float radiusPx, int stroke, int strokePx) {
        GradientDrawable g = rr(fill, radiusPx);
        g.setStroke(strokePx, stroke);
        return g;
    }

    static GradientDrawable oval(int fill) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(fill);
        return g;
    }

    static Drawable ripple(Drawable content, Drawable mask) {
        return new RippleDrawable(ColorStateList.valueOf(0x22888888), content, mask);
    }

    static LinearLayout v(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout h(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = v(c);
        l.setBackground(rr(col(c, R.color.card), dp(c, 18)));
        int p = dp(c, 16);
        l.setPadding(p, p, p, p);
        l.setElevation(dp(c, 1));
        return l;
    }

    static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    /** 带外边距（dp） */
    static LinearLayout.LayoutParams lpm(Context c, int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        return p;
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, WRAP, w);
    }

    /** 圆角按钮 */
    static TextView pill(Context c, String s, int bg, int fg) {
        TextView t = bold(tv(c, s, 15, fg));
        t.setGravity(Gravity.CENTER);
        int r = dp(c, 24);
        t.setBackground(ripple(rr(bg, r), rr(Color.BLACK, r)));
        t.setPadding(dp(c, 18), dp(c, 11), dp(c, 18), dp(c, 11));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    /** 可选中小标签 */
    static TextView chip(Context c, String s) {
        TextView t = tv(c, s, 14, col(c, R.color.text));
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 14), dp(c, 7), dp(c, 14), dp(c, 7));
        t.setClickable(true);
        setChip(c, t, false);
        return t;
    }

    static void setChip(Context c, TextView t, boolean sel) {
        int r = dp(c, 16);
        if (sel) {
            t.setBackground(rr(col(c, R.color.brand), r));
            t.setTextColor(Color.WHITE);
            t.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            t.setBackground(ripple(rr(col(c, R.color.field), r), rr(Color.BLACK, r)));
            t.setTextColor(col(c, R.color.text));
            t.setTypeface(Typeface.DEFAULT);
        }
    }

    static EditText field(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        e.setTextColor(col(c, R.color.text));
        e.setHintTextColor(col(c, R.color.sub));
        e.setBackground(rr(col(c, R.color.field), dp(c, 12)));
        e.setPadding(dp(c, 14), dp(c, 11), dp(c, 14), dp(c, 11));
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setSingleLine(true);
        return e;
    }

    static View space(Context c, int dpH) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpH)));
        return v;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(col(c, R.color.line));
        v.setLayoutParams(new LinearLayout.LayoutParams(MATCH, Math.max(1, dp(c, 0.7f))));
        return v;
    }

    static TextView section(Context c, String s) {
        TextView t = bold(tv(c, s, 13, col(c, R.color.sub)));
        t.setPadding(dp(c, 4), dp(c, 14), 0, dp(c, 6));
        return t;
    }

    static void toast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    static AlertDialog.Builder dialog(Context c) {
        return new AlertDialog.Builder(c, night(c)
                ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert);
    }

    static int alpha(int color, float a) {
        return (color & 0x00FFFFFF) | (Math.round(Color.alpha(color) * a) << 24);
    }

    static int blend(int a, int b, float t) {
        float u = 1 - t;
        return Color.argb(
                Math.round(Color.alpha(a) * u + Color.alpha(b) * t),
                Math.round(Color.red(a) * u + Color.red(b) * t),
                Math.round(Color.green(a) * u + Color.green(b) * t),
                Math.round(Color.blue(a) * u + Color.blue(b) * t));
    }

    /** 专注分钟 → 热力等级 0..4 */
    static int heatLevel(int minutes) {
        if (minutes <= 0) return 0;
        if (minutes <= 25) return 1;
        if (minutes <= 60) return 2;
        if (minutes <= 120) return 3;
        return 4;
    }

    static int heatColor(Context c, int level) {
        if (level <= 0) return Color.TRANSPARENT;
        float[] t = {0f, 0.22f, 0.42f, 0.66f, 0.92f};
        return blend(col(c, R.color.card), col(c, R.color.brand), t[Math.min(4, level)]);
    }

    /** 「1小时20分」/「45分钟」 */
    static String dur(int minutes) {
        if (minutes < 60) return minutes + "分钟";
        int h = minutes / 60, m = minutes % 60;
        return m == 0 ? h + "小时" : h + "小时" + m + "分";
    }
}
