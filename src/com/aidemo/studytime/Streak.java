package com.aidemo.studytime;

import java.util.Map;

/**
 * 连续打卡（纯 Java，可主机测试）。
 *
 * 规则（习惯类 App 的通行口径）：
 *  · 某天「番茄数 ≥ 每日目标」= 达标日；
 *  · 今天还没达标 ≠ 断签 —— 只要昨天达标，连续照样延续（今天补上就续住）；
 *  · 从今天/昨天往回数，遇到第一个不达标日为止 = 当前连续；
 *  · 历史最高 = 把所有达标日压成 01 串后最长的连续 1 段。
 */
public final class Streak {

    /** 当前连续天数 */
    public static int current(Map<String, Recs.Day> days, String todayKey, int goal) {
        if (goal <= 0) return 0;
        String start = met(days, todayKey, goal) ? todayKey : Cal.addDays(todayKey, -1);
        if (!met(days, start, goal)) return 0;
        int n = 0;
        String k = start;
        while (met(days, k, goal)) {
            n++;
            k = Cal.addDays(k, -1);
        }
        return n;
    }

    /** 历史最高连续 */
    public static int best(Map<String, Recs.Day> days, int goal) {
        if (goal <= 0 || days.isEmpty()) return 0;
        // 取最小 key 往前推一天做锚点，从 min 往 max 逐日扫（缺日 = 不达标）
        String min = null, max = null;
        for (String k : days.keySet()) {
            if (!Cal.valid(k)) continue;
            if (min == null || k.compareTo(min) < 0) min = k;
            if (max == null || k.compareTo(max) > 0) max = k;
        }
        if (min == null) return 0;
        int best = 0, run = 0;
        String k = min;
        while (k.compareTo(max) <= 0) {
            if (met(days, k, goal)) {
                run++;
                if (run > best) best = run;
            } else {
                run = 0;
            }
            k = Cal.addDays(k, 1);
        }
        return best;
    }

    /** 某天是否达标 */
    public static boolean met(Map<String, Recs.Day> days, String key, int goal) {
        Recs.Day d = days.get(key);
        return d != null && d.pomos >= goal;
    }

    /** 连续天数对应的激励文案（UI 用；纯函数便于测试） */
    public static String motto(int streak) {
        if (streak <= 0) return "今天开始，就是第一天";
        if (streak < 3) return "开了个好头，别断";
        if (streak < 7) return "习惯正在成形";
        if (streak < 30) return "一周达成，稳住就是赢";
        if (streak < 100) return "狠人，已经一个月了";
        return "三位数的坚持，了不起";
    }
}
