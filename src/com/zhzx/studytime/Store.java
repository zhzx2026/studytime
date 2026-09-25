package com.zhzx.studytime;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 全部数据：SharedPreferences + JSON（待办 / 习惯 / 专注记录 / 设置 / 番茄钟状态）。
 * 进程内单例；每次修改立刻落盘（别指望 onPause）。
 */
final class Store {
    static final class Task {
        long id;
        String title = "";
        String note = "";
        int due;          // 截止日 yyyyMMdd，0 = 无
        int pri;          // 0 普通 / 1 重要 / 2 紧急
        int est = 1;      // 预估番茄数
        int pomos;        // 已完成番茄数
        boolean done;
        int doneDay;      // 完成日
        long created;
    }

    static final class Habit {
        long id;
        String name = "";
        String emoji = "✅";
        int target = 21;  // 目标天数
        TreeSet<Integer> days = new TreeSet<>();
        long created;
    }

    static final class Session {
        long start, end;
        int min;
        long task = -1;
        String taskTitle = "";
        int day;
    }

    // ---- 设置键 ----
    static final String K_FOCUS = "focus", K_SHORT = "short", K_LONG = "long", K_EVERY = "every";
    static final String K_AUTO_BREAK = "auto_break", K_AUTO_FOCUS = "auto_focus";
    static final String K_VIBRATE = "vibrate", K_SOUND = "sound", K_KEEP_ON = "keep_on";
    static final String K_POMO = "pomo", K_GOAL = "goal", K_CHANNEL = "channel";

    private static Store inst;

    static synchronized Store get(Context c) {
        if (inst == null) inst = new Store(c.getApplicationContext());
        return inst;
    }

    final SharedPreferences sp;
    final List<Task> tasks = new ArrayList<>();
    final List<Habit> habits = new ArrayList<>();
    final List<Session> sessions = new ArrayList<>();

    private Store(Context c) {
        sp = c.getSharedPreferences("studytime", Context.MODE_PRIVATE);
        load();
    }

    private void load() {
        tasks.clear(); habits.clear(); sessions.clear();
        try { parseTasks(new JSONArray(sp.getString("tasks", "[]")), tasks); } catch (JSONException ignored) {}
        try { parseHabits(new JSONArray(sp.getString("habits", "[]")), habits); } catch (JSONException ignored) {}
        try { parseSessions(new JSONArray(sp.getString("sessions", "[]")), sessions); } catch (JSONException ignored) {}
    }

    // ---------------- JSON ----------------
    private static void parseTasks(JSONArray a, List<Task> out) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Task t = new Task();
            t.id = o.optLong("id");
            t.title = o.optString("title", "");
            t.note = o.optString("note", "");
            t.due = o.optInt("due");
            t.pri = o.optInt("pri");
            t.est = o.optInt("est", 1);
            t.pomos = o.optInt("pomos");
            t.done = o.optBoolean("done");
            t.doneDay = o.optInt("doneDay");
            t.created = o.optLong("created");
            out.add(t);
        }
    }

    private static JSONArray tasksJson(List<Task> list) throws JSONException {
        JSONArray a = new JSONArray();
        for (Task t : list) {
            JSONObject o = new JSONObject();
            o.put("id", t.id); o.put("title", t.title); o.put("note", t.note); o.put("due", t.due);
            o.put("pri", t.pri); o.put("est", t.est); o.put("pomos", t.pomos); o.put("done", t.done);
            o.put("doneDay", t.doneDay); o.put("created", t.created);
            a.put(o);
        }
        return a;
    }

    private static void parseHabits(JSONArray a, List<Habit> out) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Habit h = new Habit();
            h.id = o.optLong("id");
            h.name = o.optString("name", "");
            h.emoji = o.optString("emoji", "✅");
            h.target = o.optInt("target", 21);
            h.created = o.optLong("created");
            JSONArray d = o.optJSONArray("days");
            if (d != null) for (int j = 0; j < d.length(); j++) {
                int k = d.optInt(j);
                if (Day.valid(k)) h.days.add(k);
            }
            out.add(h);
        }
    }

    private static JSONArray habitsJson(List<Habit> list) throws JSONException {
        JSONArray a = new JSONArray();
        for (Habit h : list) {
            JSONObject o = new JSONObject();
            o.put("id", h.id); o.put("name", h.name); o.put("emoji", h.emoji);
            o.put("target", h.target); o.put("created", h.created);
            JSONArray d = new JSONArray();
            for (int k : h.days) d.put(k);
            o.put("days", d);
            a.put(o);
        }
        return a;
    }

    private static void parseSessions(JSONArray a, List<Session> out) {
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Session s = new Session();
            s.start = o.optLong("s"); s.end = o.optLong("e"); s.min = o.optInt("m");
            s.task = o.optLong("t", -1); s.taskTitle = o.optString("tt", ""); s.day = o.optInt("d");
            if (s.day == 0 && s.end > 0) s.day = Day.keyAt(s.end);
            out.add(s);
        }
    }

    private static JSONArray sessionsJson(List<Session> list) throws JSONException {
        JSONArray a = new JSONArray();
        for (Session s : list) {
            JSONObject o = new JSONObject();
            o.put("s", s.start); o.put("e", s.end); o.put("m", s.min);
            o.put("t", s.task); o.put("tt", s.taskTitle); o.put("d", s.day);
            a.put(o);
        }
        return a;
    }

    void saveTasks() {
        try { sp.edit().putString("tasks", tasksJson(tasks).toString()).apply(); } catch (JSONException ignored) {}
    }

    void saveHabits() {
        try { sp.edit().putString("habits", habitsJson(habits).toString()).apply(); } catch (JSONException ignored) {}
    }

    void saveSessions() {
        try { sp.edit().putString("sessions", sessionsJson(sessions).toString()).apply(); } catch (JSONException ignored) {}
    }

    long nextId() {
        long id = Math.max(sp.getLong("seq", 0) + 1, 1);
        sp.edit().putLong("seq", id).apply();
        return id;
    }

    // ---------------- 设置 ----------------
    int getInt(String k, int d) { return sp.getInt(k, d); }
    void putInt(String k, int v) { sp.edit().putInt(k, v).apply(); }
    boolean getBool(String k, boolean d) { return sp.getBoolean(k, d); }
    void putBool(String k, boolean v) { sp.edit().putBoolean(k, v).apply(); }
    String getStr(String k, String d) { return sp.getString(k, d); }
    void putStr(String k, String v) { sp.edit().putString(k, v).apply(); }

    // ---------------- 待办 ----------------
    Task task(long id) {
        for (Task t : tasks) if (t.id == id) return t;
        return null;
    }

    Task addTask(String title, int due) {
        Task t = new Task();
        t.id = nextId();
        t.title = title;
        t.due = due;
        t.created = System.currentTimeMillis();
        tasks.add(t);
        saveTasks();
        return t;
    }

    void setDone(Task t, boolean done) {
        t.done = done;
        t.doneDay = done ? Day.today() : 0;
        saveTasks();
    }

    void deleteTask(Task t) {
        tasks.remove(t);
        saveTasks();
    }

    List<Task> tasksDueOn(int day) {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) if (t.due == day) out.add(t);
        return out;
    }

    List<Task> tasksDoneOn(int day) {
        List<Task> out = new ArrayList<>();
        for (Task t : tasks) if (t.done && t.doneDay == day) out.add(t);
        return out;
    }

    // ---------------- 习惯 ----------------
    Habit habit(long id) {
        for (Habit h : habits) if (h.id == id) return h;
        return null;
    }

    void toggleHabit(Habit h, int day) {
        if (!h.days.remove(day)) h.days.add(day);
        saveHabits();
    }

    int habitsCheckedOn(int day) {
        int n = 0;
        for (Habit h : habits) if (h.days.contains(day)) n++;
        return n;
    }

    // ---------------- 专注记录 ----------------
    void addSession(long start, long end, int min, long taskId) {
        Session s = new Session();
        s.start = start; s.end = end; s.min = min; s.task = taskId; s.day = Day.keyAt(end);
        Task t = taskId >= 0 ? task(taskId) : null;
        if (t != null) {
            s.taskTitle = t.title;
            t.pomos++;
            saveTasks();
        }
        sessions.add(s);
        saveSessions();
    }

    List<Session> sessionsOn(int day) {
        List<Session> out = new ArrayList<>();
        for (Session s : sessions) if (s.day == day) out.add(s);
        return out;
    }

    Map<Integer, Integer> minutesByDay() {
        Map<Integer, Integer> m = new HashMap<>();
        for (Session s : sessions) {
            Integer v = m.get(s.day);
            m.put(s.day, (v == null ? 0 : v) + s.min);
        }
        return m;
    }

    Set<Integer> focusDays() {
        Set<Integer> d = new HashSet<>();
        for (Session s : sessions) d.add(s.day);
        return d;
    }

    int minutesOn(int day) {
        int m = 0;
        for (Session s : sessions) if (s.day == day) m += s.min;
        return m;
    }

    int pomosOn(int day) {
        int n = 0;
        for (Session s : sessions) if (s.day == day) n++;
        return n;
    }

    int totalMinutes() {
        int m = 0;
        for (Session s : sessions) m += s.min;
        return m;
    }

    // ---------------- 备份 / 恢复（剪贴板文本） ----------------
    String exportJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("app", "studytime");
            o.put("v", 1);
            o.put("tasks", tasksJson(tasks));
            o.put("habits", habitsJson(habits));
            o.put("sessions", sessionsJson(sessions));
            o.put("seq", sp.getLong("seq", 0));
            return o.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    /** 覆盖式恢复；格式不对返回 false，且本机数据不动。 */
    boolean importJson(String text) {
        try {
            JSONObject o = new JSONObject(text.trim());
            if (!"studytime".equals(o.optString("app"))) return false;
            List<Task> t = new ArrayList<>();
            List<Habit> h = new ArrayList<>();
            List<Session> s = new ArrayList<>();
            parseTasks(o.optJSONArray("tasks") == null ? new JSONArray() : o.optJSONArray("tasks"), t);
            parseHabits(o.optJSONArray("habits") == null ? new JSONArray() : o.optJSONArray("habits"), h);
            parseSessions(o.optJSONArray("sessions") == null ? new JSONArray() : o.optJSONArray("sessions"), s);
            long seq = o.optLong("seq", 0);
            for (Task x : t) seq = Math.max(seq, x.id);
            for (Habit x : h) seq = Math.max(seq, x.id);
            tasks.clear(); tasks.addAll(t);
            habits.clear(); habits.addAll(h);
            sessions.clear(); sessions.addAll(s);
            sp.edit().putLong("seq", seq).apply();
            saveTasks(); saveHabits(); saveSessions();
            return true;
        } catch (JSONException | RuntimeException e) {
            return false;
        }
    }
}
