package com.aidemo.studytime;

import java.util.Map;
import java.util.TreeMap;

/**
 * 每日学习记录（纯 Java，可主机测试）。
 * key = yyyyMMdd（本地日界），value = 当天 {番茄个数, 专注分钟, 完成任务数}。
 * 落盘一行一天：date|pomos|minutes|tasks，按日期升序；坏行跳过。
 */
public final class Recs {
    public static final class Day {
        public int pomos;
        public int minutes;
        public int tasks;

        public Day() {}
        public Day(int pomos, int minutes, int tasks) {
            this.pomos = pomos; this.minutes = minutes; this.tasks = tasks;
        }
    }

    /** 有序 map：日历/热力图遍历要按日期来 */
    public final TreeMap<String, Day> days = new TreeMap<>();

    public static String key(int year, int month0, int day) {
        return String.format(java.util.Locale.ROOT, "%04d%02d%02d", year, month0 + 1, day);
    }

    public Day get(String dateKey) { return days.get(dateKey); }

    public Day at(String dateKey) {
        Day d = days.get(dateKey);
        if (d == null) { d = new Day(); days.put(dateKey, d); }
        return d;
    }

    /** 完成一个番茄：个数 +1，分钟按当时配置的专注时长记 */
    public void addPomo(String dateKey, int minutes) {
        Day d = at(dateKey);
        d.pomos++;
        d.minutes += Math.max(0, minutes);
    }

    /** 完成一个任务：计数 +1（无番茄也记，日历详情里看得到） */
    public void addTask(String dateKey) {
        at(dateKey).tasks++;
    }

    public int pomosOn(String dateKey) {
        Day d = days.get(dateKey);
        return d == null ? 0 : d.pomos;
    }

    public int minutesOn(String dateKey) {
        Day d = days.get(dateKey);
        return d == null ? 0 : d.minutes;
    }

    public int tasksOn(String dateKey) {
        Day d = days.get(dateKey);
        return d == null ? 0 : d.tasks;
    }

    /** 累计番茄 / 分钟 / 完成任务 */
    public int[] totals() {
        int p = 0, m = 0, t = 0;
        for (Day d : days.values()) { p += d.pomos; m += d.minutes; t += d.tasks; }
        return new int[]{p, m, t};
    }

    /** 超过一天的数据自动裁掉（热力图最多看一年，别无限膨胀） */
    public void trim(String keepFromKey) {
        String[] ks = days.keySet().toArray(new String[0]);
        for (String k : ks) if (k.compareTo(keepFromKey) < 0) days.remove(k);
    }

    public String save() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Day> e : days.entrySet()) {
            Day d = e.getValue();
            if (d.pomos == 0 && d.minutes == 0 && d.tasks == 0) continue;
            sb.append(e.getKey()).append('|').append(d.pomos).append('|')
              .append(d.minutes).append('|').append(d.tasks).append('\n');
        }
        return sb.toString();
    }

    public void load(String s) {
        days.clear();
        if (s == null || s.isEmpty()) return;
        String[] lines = s.split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) continue;
            String[] f = line.split("\\|", -1);
            if (f.length != 4 || f[0].length() != 8) continue;
            try {
                Day d = new Day(Integer.parseInt(f[1]), Integer.parseInt(f[2]), Integer.parseInt(f[3]));
                if (d.pomos < 0 || d.minutes < 0 || d.tasks < 0) continue;
                days.put(f[0], d);
            } catch (NumberFormatException ignore) {
            }
        }
    }
}
