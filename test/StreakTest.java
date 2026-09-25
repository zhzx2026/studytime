import com.aidemo.studytime.Cal;
import com.aidemo.studytime.Recs;
import com.aidemo.studytime.Streak;

/** 连续打卡：达标日连数、今天没达标不断签、断档归零、历史最高。 */
public class StreakTest {

    static Recs day(String key, int pomos) {
        Recs r = new Recs();
        for (int i = 0; i < pomos; i++) r.addPomo(key, 25);
        return r;
    }

    /** 往 map 里造连续 n 天，end 往前推 */
    static Recs span(String end, int n, int pomos) {
        Recs r = new Recs();
        String k = end;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < pomos; j++) r.addPomo(k, 25);
            k = Cal.addDays(k, -1);
        }
        return r;
    }

    /** 造一天达标数据（4 个番茄） */
    static void addMet(Recs r, String key) {
        for (int i = 0; i < 4; i++) r.addPomo(key, 25);
    }

    public static void main(String[] a) {
        String today = "20260925"; // 周五

        // 1. 空数据
        T.eqi(0, Streak.current(new Recs().days, today, 4), "空 = 0");
        T.eqi(0, Streak.best(new Recs().days, 4), "空的最高 = 0");

        // 2. 今天达标 + 昨天往前连着 → 连续数
        Recs r = span(today, 3, 4); // 25,24,23 各 4 个
        T.eqi(3, Streak.current(r.days, today, 4), "连续 3 天");

        // 3. 今天还没达标 ≠ 断签：昨天达标就还活着
        Recs r2 = span(Cal.addDays(today, -1), 5, 4); // 24~20
        T.eqi(5, Streak.current(r2.days, today, 4), "今天没达标仍算延续");

        // 4. 今天和昨天都不达标 → 0
        Recs r3 = span(Cal.addDays(today, -2), 4, 4);
        T.eqi(0, Streak.current(r3.days, today, 4), "断两天 = 0");

        // 5. 中间缺一天 → 断档
        Recs r4 = new Recs();
        addMet(r4, today);
        addMet(r4, Cal.addDays(today, -1));
        addMet(r4, Cal.addDays(today, -3)); // 缺昨天的昨天
        T.eqi(2, Streak.current(r4.days, today, 4), "缺口截断当前连续");

        // 6. 目标边界：恰好达标算，差一个不算
        Recs r5 = day(today, 4);
        T.ok(Streak.met(r5.days, today, 4), "4/4 达标");
        Recs r6 = day(today, 3);
        T.ok(!Streak.met(r6.days, today, 4), "3/4 不达标");
        T.eqi(0, Streak.current(r6.days, today, 4), "今天差一个 = 还没开张");

        // 7. 历史最高：跨缺口找最长段
        Recs r7 = new Recs();
        String k = today;
        // 结构（从今天往前）：1天达标 · 1天空 · 4天达标 · 2天空 · 2天达标
        for (int i = 0; i < 1; i++) { addMet(r7, k); k = Cal.addDays(k, -1); }
        k = Cal.addDays(k, -1); // 空
        for (int i = 0; i < 4; i++) { addMet(r7, k); k = Cal.addDays(k, -1); }
        k = Cal.addDays(k, -2); // 空
        for (int i = 0; i < 2; i++) { addMet(r7, k); k = Cal.addDays(k, -1); }
        T.eqi(4, Streak.best(r7.days, 4), "历史最高 4");
        T.eqi(1, Streak.current(r7.days, today, 4), "当前 1");

        // 8. 单天数据：最高 1
        Recs r8 = new Recs();
        addMet(r8, today);
        T.eqi(1, Streak.best(r8.days, 4), "只有 1 天 = 最高 1");

        // 9. 目标 0 / 非法目标兜底
        T.eqi(0, Streak.current(r8.days, today, 0), "目标 0 返回 0（防死循环）");

        // 10. 激励文案分段
        T.eq("今天开始，就是第一天", Streak.motto(0), "motto 0");
        T.ok(Streak.motto(3).length() > 0 && Streak.motto(365).length() > 0, "motto 不为空");
        T.ok(Streak.motto(0) != Streak.motto(10), "不同档文案不同");

        T.done("StreakTest");
    }
}
