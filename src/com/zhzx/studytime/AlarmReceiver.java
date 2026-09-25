package com.zhzx.studytime;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** 番茄钟到点（App 在后台 / 被杀也会走到这里）：结算 + 记账 + 提醒通知。 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Pomo p = PomoCtl.get(c);
        List<Pomo.Done> done = PomoCtl.settle(c, p);
        PomoCtl.save(c, p);
        if (!done.isEmpty()) {
            Notify.done(c, done.get(done.size() - 1), p);
            MainActivity.pingFromAlarm();
        }
    }
}
