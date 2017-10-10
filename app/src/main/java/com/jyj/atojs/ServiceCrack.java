package com.jyj.atojs;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ServiceCrack extends Service{

    @Override
    public IBinder onBind(Intent arg0) {//这是Service必须要实现的方法，目前这里面什么都没有做

        return null;
    }

    @Override
    public void onCreate(){//在onCreate()方法中打印了一个log便于测试
        super.onCreate();
        Log.d("xxx","Service 已经启动成功");
    }

}