package com.aidemo.studytime;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** 完成提示：短促提示音 + 振动（设置里可分别关掉）。 */
public final class SoundFx {
    private SoundFx() {}

    public static void ding(Context c, boolean sound, boolean vibrate) {
        if (sound) {
            try {
                ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 180);
                new android.os.Handler(c.getMainLooper()).postDelayed(() -> {
                    try { tg.release(); } catch (Throwable ignore) {}
                }, 400);
            } catch (Throwable ignore) {
                // 有些机型没有通知流，静默降级
            }
        }
        if (vibrate) {
            try {
                Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
                if (v == null || !v.hasVibrator()) return;
                if (Build.VERSION.SDK_INT >= 26) {
                    v.vibrate(VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(160);
                }
            } catch (Throwable ignore) {
            }
        }
    }
}
