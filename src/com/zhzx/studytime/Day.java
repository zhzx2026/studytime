package com.zhzx.studytime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 日期工具：全 App 统一用 int 日期键 yyyyMMdd（如 20260925）存「哪一天」，
 * 排序 = 数值排序、可直接当 Map/Set 键，也不受时区序列化困扰。纯 Java，主机侧可测（DayTest）。
 */
public final class Day {
    private Day() {}

    private static final String[] WEEK = {"一", "二", "三", "四", "五", "六", "日"};

    public static int key(LocalDate d) {
        return d.getYear() * 10000 + d.getMonthValue() * 100 + d.getDayOfMonth();
    }

    public static LocalDate of(int k) {
        return LocalDate.of(k / 10000, (k / 100) % 100, k % 100);
    }

    public static int today() {
        return key(LocalDate.now());
    }

    public static int keyAt(long ms) {
        return key(Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate());
    }

    public static int add(int k, int days) {
        return key(of(k).plusDays(days));
    }

    public static boolean valid(int k) {
        try { of(k); return k > 19000101; } catch (RuntimeException e) { return false; }
    }

    /** 月历 6×7 = 42 格，周一开头；返回每格的日期键（含上月尾/下月头）。 */
    public static int[] monthGrid(int year, int month) {
        LocalDate first = LocalDate.of(year, month, 1);
        int dow = first.getDayOfWeek().getValue(); // 1 = 周一
        LocalDate start = first.minusDays(dow - 1);
        int[] out = new int[42];
        for (int i = 0; i < 42; i++) out[i] = key(start.plusDays(i));
        return out;
    }

    public static int year(int k) { return k / 10000; }
    public static int month(int k) { return (k / 100) % 100; }
    public static int dom(int k) { return k % 100; }

    /** 「周四」 */
    public static String week(int k) {
        return "周" + WEEK[of(k).getDayOfWeek().getValue() - 1];
    }

    /** 「9月25日 周四」 */
    public static String label(int k) {
        return month(k) + "月" + dom(k) + "日 " + week(k);
    }

    /** 相对今天的人话：今天 / 明天 / 昨天 / 9月25日 */
    public static String rel(int k, int today) {
        if (k == today) return "今天";
        if (k == add(today, 1)) return "明天";
        if (k == add(today, -1)) return "昨天";
        if (year(k) != year(today)) return year(k) + "/" + month(k) + "/" + dom(k);
        return month(k) + "月" + dom(k) + "日";
    }

    /** 时:分 */
    public static String hm(long ms) {
        java.time.LocalTime t = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime();
        return String.format(java.util.Locale.US, "%02d:%02d", t.getHour(), t.getMinute());
    }

    /** 倒计时文本 mm:ss（超过 1 小时显示 h:mm:ss） */
    public static String clock(long ms) {
        long s = (ms + 999) / 1000;
        if (s < 0) s = 0;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h > 0) return String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec);
        return String.format(java.util.Locale.US, "%02d:%02d", m, sec);
    }
}
