package com.aidemo.studytime;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 落盘层：SharedPreferences 存 5 个字符串键。
 * 纯逻辑（Cfg/Todo/Recs/Pomo）自己管序列化，这里只负责「读出来、写回去」。
 */
public final class Prefs {
    private static final String K_CFG = "cfg";
    private static final String K_TODO = "todos";
    private static final String K_RECS = "recs";
    private static final String K_POMO = "pomo";
    private static final String K_BIND = "bind";

    private Prefs() {}

    private static SharedPreferences p() {
        return App.get().getSharedPreferences("studytime", Context.MODE_PRIVATE);
    }

    public static Cfg cfg() { return Cfg.load(p().getString(K_CFG, null)); }

    public static void saveCfg(Cfg c) {
        p().edit().putString(K_CFG, c.save()).apply();
    }

    public static Todo todo() {
        Todo t = new Todo();
        t.load(p().getString(K_TODO, null));
        return t;
    }

    public static void saveTodo(Todo t) {
        p().edit().putString(K_TODO, t.save()).apply();
    }

    public static Recs recs() {
        Recs r = new Recs();
        r.load(p().getString(K_RECS, null));
        return r;
    }

    public static void saveRecs(Recs r) {
        p().edit().putString(K_RECS, r.save()).apply();
    }

    /** 番茄钟现场（state|run|endAt|remaining|cycle），闪退/杀后台都靠它接着走 */
    public static void savePomo(Pomo p) {
        p().edit().putString(K_POMO, p.save()).apply();
    }

    public static void loadPomo(Pomo pomo) {
        pomo.load(p().getString(K_POMO, null));
    }

    /** 当前绑定的待办 id（0 = 没绑） */
    public static int bindId() { return p().getInt(K_BIND, 0); }

    public static void saveBind(int id) { p().edit().putInt(K_BIND, id).apply(); }

    /** 清空全部学习数据（设置页的「清空数据」用）；配置一并归默认 */
    public static void clearAll() {
        p().edit().clear().apply();
    }
}
