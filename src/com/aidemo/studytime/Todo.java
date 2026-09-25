package com.aidemo.studytime;

import java.util.ArrayList;
import java.util.List;

/**
 * 待办清单（纯 Java，可主机测试）。
 * 每条：id · 标题 · 预计番茄 · 已做番茄 · 完成标志。
 * 落盘一行一条，字段用 \t 分隔，标题里的 \t \n \\ 全部转义，保证「一行一条」永不变形。
 */
public final class Todo {
    public static final class Task {
        public int id;
        public String title;
        public int est;      // 预计番茄（0 = 没填）
        public int donePomos;// 已在番茄钟里完成的个数
        public boolean done;
        /** 「完成任务数」是否已经记进当天的 Recs（防反复勾选刷计数） */
        public boolean counted;

        public Task(int id, String title, int est) {
            this.id = id;
            this.title = title;
            this.est = est;
        }
    }

    public final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    /** 加任务；标题去首尾空白、掐掉控制字符，空标题返回 null */
    public Task add(String rawTitle, int est) {
        if (rawTitle == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawTitle.length(); i++) {
            char c = rawTitle.charAt(i);
            if (c >= ' ' && c != 127) sb.append(c);
            else if (c == '\n' || c == '\t') sb.append(' ');
        }
        String t = sb.toString().trim();
        if (t.isEmpty()) return null;
        if (t.length() > 60) t = t.substring(0, 60);
        Task task = new Task(nextId++, t, Math.max(0, est));
        tasks.add(task);
        return task;
    }

    public Task byId(int id) {
        for (Task t : tasks) if (t.id == id) return t;
        return null;
    }

    /** 翻转完成状态；id 不存在返回 false */
    public boolean toggle(int id) {
        Task t = byId(id);
        if (t == null) return false;
        t.done = !t.done;
        return true;
    }

    public boolean remove(int id) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id == id) { tasks.remove(i); return true; }
        }
        return false;
    }

    public int clearDone() {
        int n = 0;
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).done) { tasks.remove(i); n++; }
        }
        return n;
    }

    /** 下一个未完成任务（列表顺序）；没有返回 null */
    public Task nextUndone() {
        for (Task t : tasks) if (!t.done) return t;
        return null;
    }

    public int countDone() {
        int n = 0;
        for (Task t : tasks) if (t.done) n++;
        return n;
    }

    public int countOpen() { return tasks.size() - countDone(); }

    // ── 落盘 ────────────────────────────────────────────────────────────────
    public String save() {
        StringBuilder sb = new StringBuilder();
        sb.append("n=").append(nextId).append('\n');
        for (Task t : tasks) {
            sb.append(t.id).append('\t').append(t.est).append('\t').append(t.donePomos)
              .append('\t').append(t.done ? 1 : 0).append('\t').append(t.counted ? 1 : 0)
              .append('\t').append(esc(t.title)).append('\n');
        }
        return sb.toString();
    }

    public void load(String s) {
        tasks.clear();
        nextId = 1;
        if (s == null || s.isEmpty()) return;
        String[] lines = s.split("\n", -1);
        for (String line : lines) {
            if (line.isEmpty()) continue;
            if (line.startsWith("n=")) {
                try { nextId = Math.max(1, Integer.parseInt(line.substring(2))); } catch (NumberFormatException ignore) {}
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f.length != 5 && f.length != 6) continue; // 脏行直接丢，别让一条坏数据毁整本
            try {
                Task t = new Task(Integer.parseInt(f[0]), unesc(f[f.length - 1]), Integer.parseInt(f[1]));
                t.donePomos = Integer.parseInt(f[2]);
                t.done = f[3].equals("1");
                t.counted = f.length == 6 ? f[4].equals("1") : t.done;
                tasks.add(t);
                if (t.id >= nextId) nextId = t.id + 1;
            } catch (NumberFormatException ignore) {
            }
        }
    }

    static String esc(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') sb.append("\\\\");
            else if (c == '\t') sb.append("\\t");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else sb.append(c);
        }
        return sb.toString();
    }

    static String unesc(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 't') sb.append('\t');
                else if (n == 'n') sb.append('\n');
                else if (n == 'r') sb.append('\r');
                else sb.append(n);
            } else sb.append(c);
        }
        return sb.toString();
    }
}
