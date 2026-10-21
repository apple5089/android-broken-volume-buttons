package cn.local.easyvolume;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    private LinearLayout page; private TextView status; private boolean pendingEnable;
    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(0xFF193C37);t.setPadding(0,dp(8),0,dp(8));page.addView(t);return t;}
    private void button(String label,boolean primary,View.OnClickListener action){
        Button b=new Button(this);b.setText(label);b.setTextSize(19);b.setMinHeight(dp(60));b.setTextColor(primary?Color.WHITE:0xFF155F50);
        GradientDrawable bg=new GradientDrawable();bg.setColor(primary?0xFF155F50:0xFFE1ECE6);bg.setCornerRadius(dp(14));b.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);page.addView(b,p);b.setOnClickListener(action);
    }
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);getWindow().setStatusBarColor(0xFFF3F6EF);getWindow().setNavigationBarColor(0xFFF3F6EF);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(0xFFF3F6EF);
        page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(22),dp(20),dp(22),dp(40));scroll.addView(page);setContentView(scroll);
        text("悬浮音量键",28).setTypeface(Typeface.DEFAULT,Typeface.BOLD);text("右侧固定 ＋ / −\n点按一档，长按连续调节",20);status=text("",18);
        button("开启 / 恢复按钮",true,v->enable());
        button("返回桌面",false,v->startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)));
        text("自动识别声音场景",22).setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        text("视频、音乐：媒体音量\n正在通话：通话音量\n闹钟正在响：闹钟音量\n其他情况：跟随系统音量规则",18);
        text("打开某个页面但没有发声时，跟随系统判断。可在原生音量条中展开更多音量。",16);
        TextView label=text("背景浓度："+Prefs.opacity(this)+"%",19);
        SeekBar opacity=new SeekBar(this);opacity.setMax(50);opacity.setProgress(Prefs.opacity(this)-25);page.addView(opacity,new LinearLayout.LayoutParams(-1,dp(56)));
        opacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar b,int p,boolean user){label.setText("背景浓度："+(p+25)+"%（越低越透明）");}
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){Prefs.get(MainActivity.this).edit().putInt("compactOpacity",b.getProgress()+25).apply();if(Prefs.enabled(MainActivity.this)&&Settings.canDrawOverlays(MainActivity.this))startForegroundService(new Intent(MainActivity.this,VolumeService.class).setAction("REBUILD"));}
        });
        Switch boot=new Switch(this);boot.setText("开机自动恢复");boot.setTextSize(20);boot.setMinHeight(dp(64));boot.setChecked(Prefs.get(this).getBoolean("boot",true));page.addView(boot);
        boot.setOnCheckedChangeListener((b,checked)->Prefs.get(this).edit().putBoolean("boot",checked).apply());
        button("允许后台常驻",false,v->{try{startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));}catch(ActivityNotFoundException e){startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}});
        button("应用系统设置",false,v->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))));
        button("关闭悬浮按钮",false,v->{Prefs.get(this).edit().putBoolean("enabled",false).apply();stopService(new Intent(this,VolumeService.class));updateStatus();});
        text("离线运行，无广告。vivo 需允许自启动与后台运行。锁屏、支付及部分权限页面可能隐藏悬浮窗。强行停止后，请重新开启。",15);
    }
    private void updateStatus(){if(status!=null)status.setText(Prefs.enabled(this)&&Settings.canDrawOverlays(this)?"● 已开启 · 自动调节":"○ 尚未开启");}
    private void enable(){
        if(!Settings.canDrawOverlays(this)){pendingEnable=true;startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}
        Prefs.get(this).edit().putBoolean("enabled",true).apply();VolumeService.start(this);updateStatus();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
    }
    @Override public void onResume(){super.onResume();if(pendingEnable&&Settings.canDrawOverlays(this)){pendingEnable=false;enable();}else if(Prefs.enabled(this)&&Settings.canDrawOverlays(this)){VolumeService.start(this);}updateStatus();}
}
