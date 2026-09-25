package com.zhzx.studytime;

import android.view.View;

/** 底部导航的一个页签。视图懒构建、缓存复用。 */
abstract class Page {
    final MainActivity act;
    private View root;

    Page(MainActivity a) { act = a; }

    final View view() {
        if (root == null) root = build();
        return root;
    }

    abstract String title();
    protected abstract View build();

    /** 切到前台 / 从别处回来：重读数据刷新 */
    void onShow() {}
    void onHide() {}
    /** 闹钟接收器在前台时结算了番茄 */
    void onAlarm() {}
}
