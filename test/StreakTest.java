import com.zhzx.studytime.Day;
import com.zhzx.studytime.Streak;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class StreakTest {
    static Set<Integer> set(Integer... k) { return new HashSet<>(Arrays.asList(k)); }

    public static void main(String[] a) {
        int today = 20260301;
        T.eq(Streak.current(set(), today), 0, "空 → 0");
        T.eq(Streak.current(set(20260301, 20260228, 20260227), today), 3, "跨 2 月底连续 3 天");
        T.eq(Streak.current(set(20260228, 20260227), today), 2, "今天没打不算断");
        T.eq(Streak.current(set(20260227), today), 0, "前天断了 → 0");
        T.eq(Streak.current(set(20240229, 20240301), 20240301), 2, "闰年 2/29");
        T.eq(Streak.best(set()), 0, "best 空 = 0");
        T.eq(Streak.best(set(20251230, 20251231, 20260101, 20260105)), 3, "跨年最长 3");
        T.eq(Streak.best(set(20260110)), 1, "单天 = 1");
        T.eq(Streak.countIn(set(20260301, 20260225, 20260220), today, 7), 2, "近 7 天打了 2 天");

        // Day
        T.eq(Day.add(20261231, 1), 20270101, "跨年 +1");
        T.eq(Day.add(20260301, -1), 20260228, "3/1 - 1 = 2/28");
        int[] g = Day.monthGrid(2026, 9); // 2026-09-01 是周二
        T.eq(g.length, 42, "月历 42 格");
        T.eq(g[0], 20260831, "首格 = 周一 8/31");
        T.eq(g[1], 20260901, "第二格 = 9/1");
        T.eq(Day.week(20260925), "周五", "2026-09-25 周五");
        T.eq(Day.rel(20260926, 20260925), "明天", "rel 明天");
        T.eq(Day.rel(20270102, 20260925), "2027/1/2", "rel 跨年带年份");
        T.eq(Day.clock(25 * 60_000L), "25:00", "clock 25:00");
        T.eq(Day.clock(1), "00:01", "不足 1 秒向上取整");
        T.eq(Day.clock(3_600_000L + 61_000L), "1:01:01", "超过 1 小时");
        T.ok(!Day.valid(20260230), "2/30 非法");
        T.ok(Day.valid(20260228), "2/28 合法");

        T.done("StreakTest");
    }
}
