package com.aidemo.studytime;

/**
 * 番茄与目标设置（纯 Java，可主机测试）。
 * 序列化成一串 k=v 逗号对，读写都容错：不认识的键跳过、缺键用默认值。
 */
public final class Cfg {
    public int workMin = 25;      // 专注时长（分钟）
    public int shortMin = 5;      // 短休息
    public int longMin = 15;      // 长休息
    public int longEvery = 4;     // 每做完几个番茄放一次长休
    public int goalPomos = 4;     // 每日目标（番茄个数）
    public boolean sound = true;  // 完成提示音
    public boolean vibrate = true;// 振动
    public boolean auto = true;   // 到点自动进入下一段

    public Cfg() {}

    public long workMs() { return workMin * 60000L; }
    public long shortMs() { return shortMin * 60000L; }
    public long longMs() { return longMin * 60000L; }

    /** 固定候选档（设置页逐个展示） */
    public static final int[] WORK_CHOICES = {15, 20, 25, 30, 45, 50};
    public static final int[] SHORT_CHOICES = {3, 5, 10};
    public static final int[] LONG_CHOICES = {10, 15, 20, 30};
    public static final int[] EVERY_CHOICES = {2, 3, 4, 6};
    public static final int[] GOAL_CHOICES = {1, 2, 3, 4, 6, 8};

    public String save() {
        return "w=" + workMin + ",s=" + shortMin + ",l=" + longMin + ",n=" + longEvery +
               ",g=" + goalPomos + ",snd=" + (sound ? 1 : 0) +
               ",vib=" + (vibrate ? 1 : 0) + ",auto=" + (auto ? 1 : 0);
    }

    public static Cfg load(String s) {
        Cfg c = new Cfg();
        if (s == null || s.isEmpty()) return c;
        String[] kv = s.split(",");
        for (String part : kv) {
            int i = part.indexOf('=');
            if (i <= 0) continue;
            String k = part.substring(0, i).trim(), v = part.substring(i + 1).trim();
            try {
                if (k.equals("w")) c.workMin = clamp(Integer.parseInt(v), 1, 180);
                else if (k.equals("s")) c.shortMin = clamp(Integer.parseInt(v), 1, 60);
                else if (k.equals("l")) c.longMin = clamp(Integer.parseInt(v), 1, 120);
                else if (k.equals("n")) c.longEvery = clamp(Integer.parseInt(v), 1, 12);
                else if (k.equals("g")) c.goalPomos = clamp(Integer.parseInt(v), 1, 24);
                else if (k.equals("snd")) c.sound = !v.equals("0");
                else if (k.equals("vib")) c.vibrate = !v.equals("0");
                else if (k.equals("auto")) c.auto = !v.equals("0");
            } catch (NumberFormatException ignore) {
            }
        }
        return c;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
