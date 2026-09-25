package com.aidemo.studytime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 番茄到点：App 在后台、锁屏甚至被系统杀掉也会被系统叫醒。
 * 结算本身是幂等的（Ctl.recordEvents + Pomo.tick），和页面心跳撞上也只记一笔。
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        int[] evs = Ctl.settleFromAlarm(c, System.currentTimeMillis());
        if (evs.length == 0) return;
        MainActivity a = MainActivity.live;
        if (a != null) a.refreshFromAlarm(evs);
    }
}
