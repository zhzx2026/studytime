import com.aidemo.studytime.Recs;

/** 每日记录：累计、落盘往返、坏行容错、裁剪。 */
public class RecsTest {
    public static void main(String[] a) {
        // 1. 累加
        Recs r = new Recs();
        r.addPomo("20260924", 25);
        r.addPomo("20260924", 25);
        r.addPomo("20260924", 30);
        r.addTask("20260924");
        r.addPomo("20260925", 15);
        T.eqi(3, r.pomosOn("20260924"), "当天番茄数");
        T.eqi(80, r.minutesOn("20260924"), "当天分钟数");
        T.eqi(1, r.tasksOn("20260924"), "当天任务数");
        T.eqi(1, r.pomosOn("20260925"), "另一天独立");
        T.eqi(0, r.pomosOn("20260101"), "没记录返回 0");

        // 2. totals
        int[] tot = r.totals();
        T.eqi(4, tot[0], "累计番茄");
        T.eqi(95, tot[1], "累计分钟");
        T.eqi(1, tot[2], "累计任务");

        // 3. 落盘往返
        String blob = r.save();
        Recs r2 = new Recs();
        r2.load(blob);
        T.eqi(4, r2.pomosOn("20260924") + r2.pomosOn("20260925"), "往返番茄数");
        T.eqi(80, r2.minutesOn("20260924"), "往返分钟");
        T.eqi(1, r2.tasksOn("20260924"), "往返任务");
        T.eqi(2, r2.days.size(), "往返天数");

        // 4. 坏行容错
        Recs r3 = new Recs();
        r3.load("20260924|1|25|0\n" +
                "坏行\n" +
                "2026092|x|1|0\n" +
                "20260925|a|b|c\n" +
                "20260926|-1|10|0\n" +
                "20260999|1|1|0\n" +  // 日期本身不校验（格式对就行），坏字段才丢
                "");
        T.eqi(2, r3.days.size(), "坏行只留好行"); // 20260924 与 20260999
        T.ok(!r3.days.containsKey("20260926"), "负数拒收");
        T.eqi(1, r3.pomosOn("20260924"), "好行照收");

        // 5. 裁剪：只留 from 之后
        Recs r4 = new Recs();
        r4.addPomo("20250101", 25);
        r4.addPomo("20260901", 25);
        r4.addPomo("20260925", 25);
        r4.trim("20260901");
        T.eqi(2, r4.days.size(), "裁掉旧日期");
        T.ok(!r4.days.containsKey("20250101"), "2025 没了");
        T.ok(r4.days.containsKey("20260901"), "边界日保留");

        // 6. 空数据
        Recs e = new Recs();
        e.load(null);
        T.eqi(0, e.days.size(), "null = 空");
        T.eq("", e.save(), "空的落盘是空串");
        e.load("");
        T.eqi(0, e.days.size(), "空串 = 空");

        T.done("RecsTest");
    }
}
