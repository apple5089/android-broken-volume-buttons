package cn.local.easyvolume;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.*;
import android.widget.*;

public class VolumeService extends Service {
    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private LinearLayout panel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int repeatDirection;
    private final Runnable repeat = new Runnable() {
        public void run() { VolumeControl.adjust(VolumeService.this, repeatDirection,false); handler.postDelayed(this, 320); }
    };
    public static void start(Context c) { c.startForegroundService(new Intent(c, VolumeService.class)); }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onCreate() {
        super.onCreate(); wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel("buttons", "悬浮音量键", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("保持音量按钮可用；点通知打开设置"); channel.setShowBadge(false); nm.createNotificationChannel(channel);
        int icon = getResources().getIdentifier("ic_volume", "drawable", getPackageName());
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder n = new Notification.Builder(this, "buttons").setSmallIcon(icon)
            .setContentTitle("悬浮音量键已开启").setContentText("＋ 加大，− 减小；自动识别声音场景")
            .setOngoing(true).setContentIntent(open).setShowWhen(false).setCategory(Notification.CATEGORY_SERVICE);
        for (int direction : new int[]{-1, 1}) {
            PendingIntent action = PendingIntent.getService(this, direction+2,
                new Intent(this, VolumeService.class).setAction(direction>0?"UP":"DOWN"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            n.addAction(new Notification.Action.Builder(icon, direction>0?"＋ 加大":"− 减小", action).build());
        }
        startForeground(7,n.build());
    }
    @Override public int onStartCommand(Intent i,int flags,int id) {
        if (!Prefs.enabled(this) || !Settings.canDrawOverlays(this)) {stopSelf();return START_NOT_STICKY;}
        if (i!=null && "UP".equals(i.getAction())) VolumeControl.adjust(this,1);
        else if(i!=null && "DOWN".equals(i.getAction())) VolumeControl.adjust(this,-1);
        if(panel==null || (i!=null && "REBUILD".equals(i.getAction()))) buildPanel();
        return START_STICKY;
    }
    private int dp(float n) {return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView key(String symbol,int direction) {
        TextView key=new TextView(this);key.setText(symbol);key.setTextColor(Color.WHITE);
        key.setTextSize(TypedValue.COMPLEX_UNIT_DIP,32);key.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        key.setGravity(Gravity.CENTER);key.setIncludeFontPadding(false);key.setShadowLayer(dp(1),0,dp(1),0xCC000000);
        key.setContentDescription(direction>0?"音量加大，长按连续加大":"音量减小，长按连续减小");
        GradientDrawable bg=new GradientDrawable();bg.setColor(Color.argb(Math.round(Prefs.opacity(this)*2.55f),15,48,44));
        bg.setCornerRadius(dp(22));bg.setStroke(dp(1),0xCCFFFFFF);key.setBackground(bg);
        key.setClickable(true);key.setFocusable(true);
        final boolean[] touching={false};
        key.setOnClickListener(v->VolumeControl.adjust(this,direction,!touching[0]));
        key.setOnTouchListener((v,e)->{
            switch(e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touching[0]=true;key.setAlpha(0.65f);key.performClick();repeatDirection=direction;handler.removeCallbacks(repeat);handler.postDelayed(repeat,650);return true;
                case MotionEvent.ACTION_MOVE:
                    if(e.getX()<0||e.getY()<0||e.getX()>v.getWidth()||e.getY()>v.getHeight()){handler.removeCallbacks(repeat);key.setAlpha(1);}return true;
                case MotionEvent.ACTION_UP:
                    handler.removeCallbacks(repeat);touching[0]=false;key.setAlpha(1);VolumeControl.adjust(this,0,true);return true;
                case MotionEvent.ACTION_CANCEL:
                    handler.removeCallbacks(repeat);touching[0]=false;key.setAlpha(1);return true;
            }
            return true;
        });
        return key;
    }
    private void buildPanel() {
        handler.removeCallbacks(repeat);if(panel!=null)wm.removeView(panel);
        panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);
        panel.addView(key("+",1),new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams bottom=new LinearLayout.LayoutParams(dp(44),dp(44));bottom.topMargin=dp(12);
        panel.addView(key("−",-1),bottom);
        params=new WindowManager.LayoutParams(dp(44),dp(100),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.LEFT;params.setTitle("EasyVolumeOverlay");position();
        try{wm.addView(panel,params);}catch(RuntimeException e){panel=null;stopSelf();}
    }
    private void position() {
        android.util.DisplayMetrics metrics=new android.util.DisplayMetrics();wm.getDefaultDisplay().getMetrics(metrics);
        params.x=Math.max(0,metrics.widthPixels-dp(50));
        // Below the OEM volume slider; fixed location on each orientation.
        params.y=Math.max(0,Math.min(Math.round(metrics.heightPixels*0.66f),metrics.heightPixels-dp(112)));
    }
    @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(panel!=null){position();wm.updateViewLayout(panel,params);}}
    @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);if(panel!=null){wm.removeView(panel);panel=null;}super.onDestroy();}
}
