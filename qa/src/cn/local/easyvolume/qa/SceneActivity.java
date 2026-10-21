package cn.local.easyvolume.qa;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.os.Bundle;
import android.widget.TextView;

public class SceneActivity extends Activity {
    private AudioTrack track; private AudioManager audio;
    @Override public void onCreate(Bundle state) {super.onCreate(state);audio=(AudioManager)getSystemService(AUDIO_SERVICE);scene(getIntent());}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);scene(i);}
    private void reset(){if(track!=null){track.stop();track.release();track=null;}audio.setMode(AudioManager.MODE_NORMAL);}
    private void scene(Intent i){
        reset();String scene=i.getStringExtra("scene");
        int usage="alarm".equals(scene)?AudioAttributes.USAGE_ALARM:"call".equals(scene)?AudioAttributes.USAGE_VOICE_COMMUNICATION:AudioAttributes.USAGE_MEDIA;
        if("call".equals(scene))audio.setMode(AudioManager.MODE_IN_COMMUNICATION);
        byte[] silence=new byte[48000];
        track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(new AudioFormat.Builder().setSampleRate(16000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(silence.length).build();
        track.write(silence,0,silence.length);track.setLoopPoints(0,silence.length/2,-1);track.play();
        TextView label=new TextView(this);label.setText("自动音量路由验证\n\n场景："+scene+"\n\n使用静音 PCM 测试信号，无实际拨号。\n验证完成后删除此临时应用。");label.setTextSize(24);label.setPadding(50,100,50,50);setContentView(label);
    }
    @Override protected void onDestroy(){reset();super.onDestroy();}
}
