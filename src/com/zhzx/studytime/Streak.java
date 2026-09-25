package com.zhzx.studytime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** 连续天数算术（坚持打卡、专注天数共用）。纯 Java，StreakTest 覆盖。 */
public final class Streak {
    private Streak() {}

    /**
     * 当前连续：今天打了从今天往回数；今天还没打则从昨天往回数（今天还有机会，不算断）。
     */
    public static int current(Set<Integer> days, int today) {
        int d = days.contains(today) ? today : Day.add(today, -1);
        int n = 0;
        while (days.contains(d)) { n++; d = Day.add(d, -1); }
        return n;
    }

    /** 历史最长连续。 */
    public static int best(Collection<Integer> days) {
        if (days.isEmpty()) return 0;
        List<Integer> list = new ArrayList<>(days);
        Collections.sort(list);
        int best = 1, run = 1;
        for (int i = 1; i < list.size(); i++) {
            int prev = list.get(i - 1), cur = list.get(i);
            if (cur == prev) continue;
            if (Day.add(prev, 1) == cur) run++; else run = 1;
            if (run > best) best = run;
        }
        return best;
    }

    /** 某天之前（含）的 n 天里打了几天。 */
    public static int countIn(Set<Integer> days, int endDay, int n) {
        int c = 0, d = endDay;
        for (int i = 0; i < n; i++) { if (days.contains(d)) c++; d = Day.add(d, -1); }
        return c;
    }
}
