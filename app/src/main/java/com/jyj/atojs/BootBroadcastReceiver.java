package com.jyj.atojs;

/**
 * Created by Administrator on 2017-09-29.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {
    static final String ACTION = "android.intent.action.BOOT_COMPLETED";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("xxx", "intent.getAction() ===== " + intent.getAction());

        if (intent.getAction().equals(ACTION)) {

            //1.启动一个Activity
            Intent mainActivityIntent = new Intent(context, MainActivity.class);// 要启动的Activity
            Log.d("xxx","开机自启动一个Activity");
            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityIntent);

            //2.启动一个Service
            Intent service = new Intent(context,ServiceCrack.class);// 要启动的Service
            context.startService(service);
            Log.d("xxx","开机自启动一个Service");

            //3.启动一个app
            Intent app = context.getPackageManager().getLaunchIntentForPackage("com.jyj.atojs");//包名
            context.startActivity(app);

        }
    }
}