package cn.local.easyvolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(i.getAction()) && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(i.getAction())) return;
        if (Prefs.enabled(c) && Prefs.get(c).getBoolean("boot", true) && Settings.canDrawOverlays(c)) {
            try { c.startForegroundService(new Intent(c, VolumeService.class)); }
            catch (RuntimeException e) { Log.w("EasyVolume", "Open app to restore volume buttons", e); }
        }
    }
}
