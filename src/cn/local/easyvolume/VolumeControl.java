package cn.local.easyvolume;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.util.Log;
import android.widget.Toast;

final class VolumeControl {
    static void adjust(Context c, int direction) {
        adjust(c,direction,true);
    }
    static void adjust(Context c,int direction,boolean showUi) {
        try {
            AudioManager a = (AudioManager)c.getSystemService(Context.AUDIO_SERVICE);
            int flags=showUi?AudioManager.FLAG_SHOW_UI:0;
            int mode = a.getMode();
            // Active cellular / VoIP calls take priority even when another app is in front.
            if (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION) {
                a.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, direction, flags);
                Log.i("EasyVolume", "route=call direction="+direction);
                return;
            }
            // Android's default suggested stream does not consistently choose a ringing alarm.
            // Query playback usages only: no app inventory, screen reading, microphone or accessibility.
            boolean alarm = false;
            try {
                for (AudioPlaybackConfiguration p : a.getActivePlaybackConfigurations()) {
                    if (p.getAudioAttributes().getUsage() == AudioAttributes.USAGE_ALARM) { alarm = true; break; }
                }
            } catch (RuntimeException e) { Log.w("EasyVolume", "Playback usage unavailable; use native routing"); }
            if (alarm && mode != AudioManager.MODE_RINGTONE) {
                a.adjustStreamVolume(AudioManager.STREAM_ALARM, direction, flags);
                Log.i("EasyVolume", "route=alarm direction="+direction);
            } else {
                // Native context routing: active music/video, incoming ringtone, or system default.
                a.adjustVolume(direction, flags);
                Log.i("EasyVolume", "route=native direction="+direction);
            }
        } catch (SecurityException e) {
            Toast.makeText(c, "请先在系统设置中退出勿扰或静音模式", Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(c, "系统暂时无法调节音量，请稍后再试", Toast.LENGTH_SHORT).show();
        }
    }
}
