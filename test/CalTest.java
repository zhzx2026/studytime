import com.aidemo.studytime.Cal;
import com.aidemo.studytime.Recs;

/** 日历数学：周一起算、闰年、跨月推移、月网格、热力分档。 */
public class CalTest {
    public static void main(String[] a) {
        // 1. 合法 / 非法 key
        T.ok(Cal.valid("20260925"), "正常日期");
        T.ok(!Cal.valid("20260230"), "2月30不存在");
        T.ok(!Cal.valid("20261301"), "13月非法");
        T.ok(!Cal.valid("20269101"), "91月非法");
        T.ok(!Cal.valid(null), "null 非法");
        T.ok(Cal.valid("20240229"), "闰年 2/29 合法");
        T.ok(!Cal.valid("20260229"), "平年 2/29 非法");

        // 2. 跨月/跨年推移
        T.eq("20260101", Cal.addDays("20251231", 1), "跨年 +1");
        T.eq("20251231", Cal.addDays("20260101", -1), "跨年 -1");
        T.eq("20260301", Cal.addDays("20260228", 1), "2月尾 +1");
        T.eq("20240301", Cal.addDays("20240229", 1), "闰年2月尾 +1");
        T.eq("20260926", Cal.addDays("20260925", 1), "月内 +1");
        T.eq("20260918", Cal.addDays("20260925", -7), "回一周");

        // 3. 周一开头
        T.eq("20260921", Cal.weekStart("20260925"), "周五所在周的周一是21");
        T.eq("20260921", Cal.weekStart("20260921"), "周一自己");
        T.eq("20260921", Cal.weekStart("20260927"), "周日归上一个周一");
        T.eq("20260914", Cal.weekStart("20260920"), "周日往回 6 天");
        // 2026-09-01 是周二
        T.eqi(1, Cal.firstWeekday(2026, 8), "2026-09-01 是周二 → 前导 1 格");

        // 4. 天数 / 闰年
        T.eqi(30, Cal.daysInMonth(2026, 8), "9月30天");
        T.eqi(31, Cal.daysInMonth(2026, 0), "1月31天");
        T.eqi(28, Cal.daysInMonth(2026, 1), "2026 二月 28");
        T.eqi(29, Cal.daysInMonth(2024, 1), "2024 二月 29");
        T.eqi(28, Cal.daysInMonth(2100, 1), "2100 不是闰年");
        T.eqi(29, Cal.daysInMonth(2000, 1), "2000 是闰年");

        // 5. 月网格：42 格、本月 1 号在正确位置、日子不重不漏
        String[] g = Cal.monthGrid(2026, 8);
        T.eqi(42, g.length, "恒 42 格");
        T.eq("20260901", g[1], "9/1 周二 → 下标 1");
        int count = 0;
        for (int i = 0; i < 42; i++) {
            T.ok(g[i] != null && Cal.valid(g[i]), "第 " + i + " 格是合法日期");
            if (g[i].startsWith("202609")) count++;
        }
        T.eqi(30, count, "本月天数不重不漏");
        // 2月平年网格
        String[] f = Cal.monthGrid(2026, 1);
        int fc = 0;
        for (String s : f) if (s != null && s.startsWith("202602")) fc++;
        T.eqi(28, fc, "2026-02 网格 28 天");

        // 6. 热力分档（目标 4）
        T.eqi(0, Cal.level(0, 4), "0 个 = 空白");
        T.eqi(1, Cal.level(1, 4), "1 个 = 学了没达标");
        T.eqi(1, Cal.level(3, 4), "3 个 = 还差一点");
        T.eqi(2, Cal.level(4, 4), "4 个 = 达标");
        T.eqi(2, Cal.level(5, 4), "5 个 = 仍 2 档");
        T.eqi(3, Cal.level(6, 4), "6 个 = 1.5 倍");
        T.eqi(4, Cal.level(10, 4), "10 个 = 2.5 倍");
        T.eqi(3, Cal.level(9, 4), "9 个 = 还没到 2.5 倍");
        // 目标 1
        T.eqi(2, Cal.level(1, 1), "目标1：1个即达标");
        T.eqi(4, Cal.level(3, 1), "目标1：3个 = 4 档");
        // levelOf 走记录
        Recs r = new Recs();
        r.addPomo("20260925", 25);
        r.addPomo("20260925", 25);
        T.eqi(1, Cal.levelOf(r, "20260925", 4), "2 个番茄 → 1 档");
        T.eqi(0, Cal.levelOf(r, "20260101", 4), "没记录 → 0 档");

        // 7. 今天 key 自洽
        String tk = Cal.todayKey();
        T.ok(Cal.valid(tk), "todayKey 合法");
        java.util.Calendar now = java.util.Calendar.getInstance();
        T.eqi(now.get(java.util.Calendar.YEAR), Integer.parseInt(tk.substring(0, 4)), "today 年");
        T.eqi(now.get(java.util.Calendar.MONTH) + 1, Integer.parseInt(tk.substring(4, 6)), "today 月");

        // 8. 标题文案
        T.eq("2026年9月", Cal.monthTitle(2026, 8), "月标题");
        T.eq("2026年12月", Cal.monthTitle(2026, 11), "12月标题");
        T.eq("9月25日 周五", Cal.dayTitle("20260925"), "日标题");
        T.eq("1月1日 周四", Cal.dayTitle("20260101"), "元旦标题");

        T.done("CalTest");
    }
}
