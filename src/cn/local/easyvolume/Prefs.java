package cn.local.easyvolume;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context c) { return c.getSharedPreferences("volume", Context.MODE_PRIVATE); }
    static boolean enabled(Context c) { return get(c).getBoolean("enabled", false); }
    static int opacity(Context c) { return get(c).getInt("compactOpacity", 48); }
}
