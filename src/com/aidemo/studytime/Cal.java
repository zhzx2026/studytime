package com.aidemo.studytime;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 日历与热力图数学（纯 Java，可主机测试）。
 * 一周从周一起算（国内习惯）；日期 key 与 Recs 一致：yyyyMMdd。
 */
public final class Cal {

    /** 今天（本地时区）的 yyyyMMdd */
    public static String todayKey() {
        return todayKey(System.currentTimeMillis());
    }

    public static String todayKey(long epochMs) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochMs);
        return Recs.key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /** key → Calendar（非法输入返回 null） */
    public static Calendar parse(String key) {
        if (key == null || key.length() != 8) return null;
        try {
            int y = Integer.parseInt(key.substring(0, 4));
            int m = Integer.parseInt(key.substring(4, 6));
            int d = Integer.parseInt(key.substring(6, 8));
            if (m < 1 || m > 12 || d < 1 || d > 31) return null;
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(y, m - 1, d);
            if (c.get(Calendar.MONTH) != m - 1 || c.get(Calendar.DAY_OF_MONTH) != d) return null;
            return c;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean valid(String key) { return parse(key) != null; }

    /** key 往前/往后推 n 天（原地改，返回同一对象；key 非法则原样返回） */
    public static String addDays(String key, int n) {
        Calendar c = parse(key);
        if (c == null) return key;
        c.add(Calendar.DAY_OF_MONTH, n);
        return Recs.key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /** key 所在周的周一 */
    public static String weekStart(String key) {
        Calendar c = parse(key);
        if (c == null) return key;
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=周日
        int back = dow == Calendar.MONDAY ? 0 : dow == Calendar.SUNDAY ? 6 : dow - 2;
        c.add(Calendar.DAY_OF_MONTH, -back);
        return Recs.key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    /** 该月天数 */
    public static int daysInMonth(int year, int month0) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, 1);
        return c.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /** 该月 1 号是周几（0=周一 … 6=周日） */
    public static int firstWeekday(int year, int month0) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month0, 1);
        int dow = c.get(Calendar.DAY_OF_WEEK); // 1=周日
        return dow == Calendar.SUNDAY ? 6 : dow - 2;
    }

    /** 月视图网格：返回 weeks*7 的日期数组，非本月的位置是 ""（长度恒为 42） */
    public static String[] monthGrid(int year, int month0) {
        int lead = firstWeekday(year, month0);
        int dim = daysInMonth(year, month0);
        String[] g = new String[42];
        for (int i = 0; i < 42; i++) {
            int dayNum = i - lead + 1;
            if (dayNum >= 1 && dayNum <= dim) {
                g[i] = Recs.key(year, month0, dayNum);
            } else {
                // 相邻月的格子：本月 1 号 + (dayNum-1) 天，一条式子通吃上月末/下月初
                Calendar c = Calendar.getInstance();
                c.clear();
                c.set(year, month0, 1);
                c.add(Calendar.DAY_OF_MONTH, dayNum - 1);
                g[i] = Recs.key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
            }
        }
        return g;
    }

    /**
     * 热力等级 0~4：0 没学；1 学了没达标；2 达标；3 达标 ×1.5；4 达标 ×2.5。
     * （与 GitHub 热力图同思路：颜色深浅只看「离当天目标多远」）
     */
    public static int level(int pomos, int goal) {
        if (pomos <= 0) return 0;
        if (pomos < goal) return 1;
        if (pomos >= Math.ceil(goal * 2.5)) return 4;
        if (pomos >= Math.ceil(goal * 1.5)) return 3;
        return 2;
    }

    /** 等级 0~4 对应的颜色资源 id 由调用方映射；这里只算等级 */
    public static int levelOf(Recs recs, String dateKey, int goal) {
        if (!valid(dateKey)) return 0;
        return level(recs.pomosOn(dateKey), goal);
    }

    /** 月份标题：2026年9月 */
    public static String monthTitle(int year, int month0) {
        return String.format(Locale.CHINA, "%d年%d月", year, month0 + 1);
    }

    /** 「9月25日 周四」样式；dateKey 非法返回原串 */
    public static String dayTitle(String dateKey) {
        Calendar c = parse(dateKey);
        if (c == null) return dateKey;
        String[] wk = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int dow = c.get(Calendar.DAY_OF_WEEK);
        String w = dow == Calendar.SUNDAY ? wk[6] : wk[dow - Calendar.MONDAY];
        return String.format(Locale.CHINA, "%d月%d日 %s", c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH), w);
    }

    /** 当前时区名（设置页/诊断用，纯函数便于测试） */
    public static String tzName() {
        return TimeZone.getDefault().getID();
    }
}
