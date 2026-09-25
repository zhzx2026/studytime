import com.zhzx.studytime.Pomo;

import java.util.List;

public class PomoTest {
    static final long M = 60_000L;

    public static void main(String[] a) {
        // 1. 基本开始 / 暂停 / 继续：暂停时间不计入
        Pomo p = new Pomo();
        p.reset();
        T.eq(p.remaining(0), 25 * M, "空闲时剩余 = 满时长");
        p.start(1000);
        T.ok(p.running && p.started, "开始后运行中");
        T.eq(p.remaining(1000 + 5 * M), 20 * M, "5 分钟后剩 20 分");
        p.pause(1000 + 5 * M);
        T.eq(p.remaining(1000 + 60 * M), 20 * M, "暂停期间不走");
        p.start(1000 + 60 * M);
        T.eq(p.endAt, 1000 + 80 * M, "继续后 endAt 顺延");
        T.ok(!p.due(1000 + 79 * M), "未到点");
        T.ok(p.due(1000 + 80 * M), "到点");

        // 2. 结算：专注 → 短休（不自动续）
        List<Pomo.Done> d = p.settle(1000 + 80 * M, false, false);
        T.eq(d.size(), 1, "结算 1 个阶段");
        T.eq(d.get(0).phase, Pomo.FOCUS, "结算的是专注");
        T.eq(d.get(0).minutes, 25, "专注分钟 = 设定时长（暂停不计）");
        T.eq(d.get(0).start, 1000L, "记录起点 = 第一次开始");
        T.eq(p.phase, Pomo.SHORT, "进入短休");
        T.ok(!p.started, "下一阶段待开始");
        T.eq(p.done, 1, "本轮完成 1 个");

        // 3. 第 4 个番茄后长休，长休结束重置轮次
        Pomo q = new Pomo();
        q.reset();
        long t = 0;
        for (int i = 0; i < 4; i++) {
            q.setPhase(Pomo.FOCUS);
            q.start(t); t += 25 * M; q.settle(t, false, false);
            if (i < 3) { T.eq(q.phase, Pomo.SHORT, "第" + (i + 1) + "个后短休"); q.skip(); }
        }
        T.eq(q.phase, Pomo.LONG, "第 4 个后长休");
        q.start(t); t += 15 * M; q.settle(t, false, false);
        T.eq(q.phase, Pomo.FOCUS, "长休后回到专注");
        T.eq(q.done, 0, "长休后轮次清零");

        // 4. 离开期间自动续：专注→休息→专注… 全部补结算，时间线不漂移
        Pomo r = new Pomo();
        r.reset();
        r.start(0);
        List<Pomo.Done> chain = r.settle(25 * M + 5 * M + 25 * M + 1, true, true);
        T.eq(chain.size(), 3, "补结算 专注+短休+专注");
        T.eq(chain.get(2).start, 30 * M, "第二个专注从短休 endAt 起算");
        T.ok(r.running && r.phase == Pomo.SHORT, "仍在自动续的短休中");
        T.eq(r.endAt, 60 * M, "短休 endAt 精确");

        // 5. autoBreak 开、autoFocus 关：休息结束停下
        Pomo s = new Pomo();
        s.reset();
        s.start(0);
        List<Pomo.Done> c2 = s.settle(100 * M, true, false);
        T.eq(c2.size(), 2, "专注 + 自动短休都结算");
        T.ok(!s.running && s.phase == Pomo.FOCUS, "停在待开始的专注");

        // 6. 跳过专注不计数
        Pomo k = new Pomo();
        k.reset();
        k.start(0);
        k.skip();
        T.eq(k.done, 0, "跳过的专注不计数");
        T.eq(k.phase, Pomo.SHORT, "跳过专注 → 短休");

        // 7. 编码往返 + 脏数据兜底
        Pomo e = new Pomo();
        e.reset();
        e.start(12345);
        e.taskId = 42;
        e.done = 2;
        Pomo e2 = Pomo.decode(e.encode());
        T.eq(e2.encode(), e.encode(), "编码往返一致");
        T.eq(e2.taskId, 42L, "关联任务保留");
        Pomo bad = Pomo.decode("p1,9,x");
        T.ok(!bad.started && bad.phase == Pomo.FOCUS, "脏数据 → 全新状态");
        T.ok(!Pomo.decode(null).started, "null → 全新状态");

        // 8. 运行中不能切阶段；进度 0..1
        Pomo f = new Pomo();
        f.reset();
        f.start(0);
        T.ok(!f.setPhase(Pomo.LONG), "运行中切阶段被拒");
        T.ok(Math.abs(f.progress(12 * M + 30_000) - 0.5f) < 1e-4, "过半进度 0.5");
        T.ok(f.progress(99 * M) == 1f, "进度封顶 1");

        // 9. 时长下限 1 分钟（配置被写成 0 也不会除零 / 瞬间到点）
        Pomo g = new Pomo();
        g.focusMin = 0;
        T.eq(g.full(Pomo.FOCUS), M, "0 分钟配置按 1 分钟算");

        T.done("PomoTest");
    }
}
