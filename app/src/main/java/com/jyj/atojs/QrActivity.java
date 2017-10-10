package com.jyj.atojs;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.uuzuche.lib_zxing.activity.CodeUtils;

public class QrActivity extends Activity {
    String str_mac;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        Bundle bundle = this.getIntent().getExtras();
        str_mac = bundle.getString("mac");

        Bitmap image = CodeUtils.createImage(str_mac, 500, 500, null);
        ImageView iv = (ImageView) findViewById(R.id.imageView2);
        iv.setImageBitmap(image);

        //----------------接收广播---------------
        IntentFilter filter = new IntentFilter(MainActivity.action);
        registerReceiver(broadcastReceiver, filter);
    }

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if(intent.getExtras().getString("data").equals("send_close")){
                SharedPreferences pref = QrActivity.this.getSharedPreferences("data",MODE_PRIVATE);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("mac_8",str_mac);
                editor.commit();

                finish();
            }
        }
    };

}
