package com.aidemo.studytime;

import android.app.Application;

/** 全局上下文入口（页面栈之外拿 Context 用，落盘/提示/通知都靠它）。 */
public class App extends Application {
    private static App inst;

    @Override public void onCreate() {
        super.onCreate();
        inst = this;
        Notify.channels(this); // 通知渠道建一次，receiver 复用
    }

    public static App get() { return inst; }
}
