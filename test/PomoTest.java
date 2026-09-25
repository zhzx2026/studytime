import com.aidemo.studytime.Cfg;
import com.aidemo.studytime.Pomo;

/** 番茄钟状态机：开始/暂停/续跑、自动链、补账上限、跳过休息、快照恢复。 */
public class PomoTest {
    static final long MIN = 60000L;

    static Pomo mk() {
        Cfg c = new Cfg(); // 25/5/15 每4轮
        return new Pomo(c);
    }

    public static void main(String[] a) {
        long now = 1_700_000_000_000L; // 固定起点

        // 1. 开始：空闲 → 专注 running
        Pomo p = mk();
        p.start(now);
        T.eqi(Pomo.WORK, p.state, "start 后进入 WORK");
        T.ok(p.running, "start 后 running");
        T.eqi(now + 25 * MIN, p.endAt, "endAt = now + 25min");

        // 2. 没到点不结算
        T.eqi(0, p.tick(now + 25 * MIN - 1).length, "差 1ms 不结算");
        T.eqi(Pomo.WORK, p.state, "未到点仍是 WORK");

        // 3. 正点结算：WORK → 短休（自动续）
        int[] evs = p.tick(now + 25 * MIN);
        T.eqi(1, evs.length, "只发生一个事件");
        T.eqi(Pomo.EV_WORK, evs[0], "事件是完成专注");
        T.eqi(Pomo.SHORT, p.state, "进入短休");
        T.eqi(1, p.cycle, "本轮 1 个");
        T.eqi(now + 25 * MIN + 5 * MIN, p.endAt, "短休按原时间轴链下去");

        // 4. 暂停 / 继续：剩余时间守恒
        long t2 = now + 25 * MIN + 2 * MIN;
        p.pause(t2);
        T.ok(!p.running, "pause 后停表");
        T.eqi(3 * MIN, p.remainingAt(t2), "暂停剩 3 分钟");
        p.resume(t2 + 60_000);
        T.ok(p.running, "resume 后走表");
        T.eqi(t2 + 60_000 + 3 * MIN, p.endAt, "resume 从剩余接上");

        // 5. reset 回空闲、轮次归零
        p.reset();
        T.eqi(Pomo.IDLE, p.state, "reset 回 IDLE");
        T.eqi(0, p.cycle, "reset 轮次归零");

        // 6. 一轮走满 → 长休，cycle 归零
        Pomo q = mk();
        long t = 0;
        q.start(t);
        for (int i = 1; i <= 4; i++) {
            int[] e = q.tick(t += 25 * MIN);
            T.eqi(Pomo.EV_WORK, e[0], "第 " + i + " 个番茄完成");
            if (i < 4) {
                T.eqi(Pomo.SHORT, q.state, "前3轮进短休");
                // 跳过短休直接进下一轮
                t += 5 * MIN;
                int[] e2 = q.tick(t);
                T.eqi(Pomo.EV_SHORT, e2[0], "短休到点");
                T.eqi(Pomo.WORK, q.state, "短休后回专注");
            } else {
                T.eqi(Pomo.LONG, q.state, "第4轮进长休");
                T.eqi(0, q.cycle, "长休时轮次归零");
            }
        }
        // 长休结束 → 专注、轮次从头
        t += 15 * MIN;
        int[] e3 = q.tick(t);
        T.eqi(Pomo.EV_LONG, e3[0], "长休到点");
        T.eqi(Pomo.WORK, q.state, "长休后回专注");

        // 7. auto=false：到点停下等我开始
        Pomo r = mk();
        r.cfg.auto = false;
        r.start(now);
        int[] e4 = r.tick(now + 25 * MIN);
        T.eqi(Pomo.EV_WORK, e4[0], "auto off 完成专注");
        T.ok(!r.running, "auto off 停表");
        T.eqi(Pomo.SHORT, r.state, "停在短休");
        T.eqi(5 * MIN, r.remaining, "短休剩余拉满");
        r.resume(now + 25 * MIN);
        T.ok(r.running, "手动继续短休");

        // 8. 意外退出补账
        // 8a. 离开 30 分钟（25 专注 + 5 短休恰好走完）→ 回来时下一段专注已经开跑
        Pomo s = mk();
        s.start(now);
        int[] e5 = s.tick(now + 30 * MIN);
        T.eqi(2, e5.length, "补账承认：专注 + 短休");
        T.eqi(Pomo.EV_WORK, e5[0], "先结专注");
        T.eqi(Pomo.EV_SHORT, e5[1], "再结短休");
        T.eqi(Pomo.WORK, s.state, "落到下一段专注");
        T.ok(s.running, "下一段按原时间轴在跑");
        T.eqi(now + 55 * MIN, s.endAt, "下一段从短休结束点起算");
        T.eqi(1, s.cycle, "补账只承认一个番茄");

        // 8b. 离开 3 小时 → 承认一个番茄 + 休息，下一段停表拉满（不凭空多记）
        Pomo s4 = mk();
        s4.start(now);
        int[] e7 = s4.tick(now + 180 * MIN);
        T.eqi(2, e7.length, "长离开只结两个事件");
        T.eqi(Pomo.EV_WORK, e7[0], "第一个是番茄");
        T.eqi(Pomo.EV_SHORT, e7[1], "第二个是短休");
        T.eqi(Pomo.WORK, s4.state, "停在下一段专注");
        T.ok(!s4.running, "长离开后停表等人");
        T.eqi(25 * MIN, s4.remaining, "剩余拉满整段");
        T.eqi(1, s4.cycle, "仍然只有一个番茄");

        // 9. 只离开 10 分钟（没到点）→ 什么都不发生
        Pomo s2 = mk();
        s2.start(now);
        T.eqi(0, s2.tick(now + 10 * MIN).length, "没到点零事件");
        T.ok(s2.running, "还在跑");

        // 10. 离开 25 分钟整 → 恰好一个事件
        Pomo s3 = mk();
        s3.start(now);
        int[] e6 = s3.tick(now + 25 * MIN);
        T.eqi(1, e6.length, "恰好到点只结一个");
        T.eqi(Pomo.SHORT, s3.state, "随后是短休");

        // 11. 跳过休息（running）
        Pomo u = mk();
        u.start(now);
        long wEnd = now + 25 * MIN;
        u.tick(wEnd); // → 短休 running
        int sk = u.skip(wEnd + MIN);
        T.eqi(Pomo.EV_SHORT, sk, "skip 返回短休事件");
        T.eqi(Pomo.WORK, u.state, "skip 后进专注");
        T.eqi(wEnd + 5 * MIN + 25 * MIN, u.endAt, "skip 仍按原时间轴");

        // 12. 跳过休息（暂停中）
        Pomo u2 = mk();
        u2.cfg.auto = false;
        u2.start(now);
        u2.tick(now + 25 * MIN); // → 短休 paused
        int sk2 = u2.skip(now + 26 * MIN);
        T.eqi(Pomo.EV_SHORT, sk2, "paused skip 事件");
        T.ok(!u2.running, "auto off skip 后仍停着等我");
        T.eqi(Pomo.WORK, u2.state, "落点是专注");

        // 13. 快照往返
        Pomo v = mk();
        v.start(now);
        v.cycle = 2;
        String snap = v.save();
        Pomo v2 = mk();
        T.ok(v2.load(snap), "快照可载入");
        T.eqi(Pomo.WORK, v2.state, "状态恢复");
        T.eqi(now + 25 * MIN, v2.endAt, "endAt 恢复");
        T.eqi(2, v2.cycle, "轮次恢复");

        // 14. 脏快照拒收
        Pomo v3 = mk();
        T.ok(!v3.load("1|1|abc|0|0"), "坏数字拒收");
        T.ok(!v3.load("9|0|0|0|0"), "坏状态拒收");
        T.ok(!v3.load("junk"), "坏格式拒收");
        T.eqi(Pomo.IDLE, v3.state, "拒收后保持空闲");
        T.ok(v3.load("0|0|0|0|0"), "空闲快照照收");

        // 15. 暂停态快照：remaining 落盘
        Pomo v4 = mk();
        v4.start(now);
        v4.pause(now + 10 * MIN);
        Pomo v5 = mk();
        T.ok(v5.load(v4.save()), "暂停快照载入");
        T.ok(!v5.running, "仍是暂停");
        T.eqi(15 * MIN, v5.remaining, "剩余 15 分钟恢复");

        T.done("PomoTest");
    }
}
