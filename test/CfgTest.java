import com.aidemo.studytime.Cfg;

/** 配置：默认值、往返、容错、边界夹取。 */
public class CfgTest {
    public static void main(String[] a) {
        // 1. 默认
        Cfg d = new Cfg();
        T.eqi(25, d.workMin, "默认专注 25");
        T.eqi(5, d.shortMin, "默认短休 5");
        T.eqi(15, d.longMin, "默认长休 15");
        T.eqi(4, d.longEvery, "每 4 轮长休");
        T.eqi(4, d.goalPomos, "每日目标 4");
        T.ok(d.sound && d.vibrate && d.auto, "默认提示全开");
        T.ok(!d.autoFocus && !d.keepOn, "默认：下一轮不自动开跑、屏幕不常亮");

        // 2. 往返
        d.workMin = 45;
        d.shortMin = 3;
        d.longMin = 30;
        d.longEvery = 2;
        d.goalPomos = 8;
        d.sound = false;
        d.vibrate = false;
        d.auto = false;
        d.autoFocus = true;
        d.keepOn = true;
        Cfg back = Cfg.load(d.save());
        T.eqi(45, back.workMin, "往返 workMin");
        T.eqi(3, back.shortMin, "往返 shortMin");
        T.eqi(30, back.longMin, "往返 longMin");
        T.eqi(2, back.longEvery, "往返 longEvery");
        T.eqi(8, back.goalPomos, "往返 goal");
        T.ok(!back.sound, "往返 sound=false");
        T.ok(!back.vibrate, "往返 vibrate=false");
        T.ok(!back.auto, "往返 auto=false");
        T.ok(back.autoFocus, "往返 af=true");
        T.ok(back.keepOn, "往返 ko=true");

        // 2b. 老配置（没有 af/ko 两键）→ 都按关处理
        Cfg old = Cfg.load("w=45,s=3,l=30,n=2,g=8,snd=0,vib=0,auto=0");
        T.ok(!old.autoFocus, "老串 af 缺省关");
        T.ok(!old.keepOn, "老串 ko 缺省关");

        // 3. null / 空串 / 垃圾 → 全默认
        Cfg n1 = Cfg.load(null);
        T.eqi(25, n1.workMin, "null = 默认");
        Cfg n2 = Cfg.load("");
        T.eqi(25, n2.workMin, "空串 = 默认");
        Cfg n3 = Cfg.load("junk,wat,=,,x=y");
        T.eqi(25, n3.workMin, "垃圾不炸");
        Cfg n4 = Cfg.load("w=abc,s=xyz");
        T.eqi(25, n4.workMin, "坏数字用默认");

        // 4. 边界夹取（超大/超小/负数）
        T.eqi(180, Cfg.load("w=9999").workMin, "workMin 上限 180");
        T.eqi(1, Cfg.load("w=0").workMin, "workMin 下限 1");
        T.eqi(1, Cfg.load("w=-5").workMin, "负数按下限");
        T.eqi(24, Cfg.load("g=100").goalPomos, "goal 上限 24");
        T.eqi(1, Cfg.load("n=0").longEvery, "longEvery 下限 1");

        // 5. 未知键忽略、能和已知键共存
        Cfg n5 = Cfg.load("w=30,future=1,s=10");
        T.eqi(30, n5.workMin, "已知键照收");
        T.eqi(10, n5.shortMin, "后面的已知键也收");

        // 6. 开关位：0 关，其余开
        T.ok(!Cfg.load("snd=0").sound, "snd=0 关");
        T.ok(Cfg.load("snd=1").sound, "snd=1 开");
        T.ok(Cfg.load("snd=2").sound, "怪值当开（容错）");
        T.ok(!Cfg.load("auto=0,vib=0").auto, "auto=0 关");

        // 7. 毫秒换算
        Cfg c = new Cfg();
        c.workMin = 25;
        T.eqi(25 * 60 * 1000L, c.workMs(), "workMs");
        T.eqi(5 * 60 * 1000L, c.shortMs(), "shortMs");

        T.done("CfgTest");
    }
}
