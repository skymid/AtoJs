package com.jyj.atojs;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.jyj.atojs.R.id.webView;

public class MainActivity extends Activity {
    private WebView myWebView;

    ProgressBar bar;

    ProgressDialog progressBar = null;

    private String message,retCode,first;
    String url="http://112.116.119.47:81/android/hwy/web-msg-sender/";
    String str_reg_update = url+"reg_update.php";  //1更新tv登录，2退出状态 3重装时 mac 检查
    String str_index="";

    int p=1;
    public static final String action = "com.jyj.atojs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bar = (ProgressBar)findViewById(R.id.myProgressBar);
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // Toast.makeText(getApplicationContext(),  getMac(),Toast.LENGTH_SHORT).show();

        SharedPreferences pref = MainActivity.this.getSharedPreferences("data",MODE_PRIVATE);
        String str_mac = pref.getString("mac_8","");//和存储在本地的值对比，看是否是第一次使用
        //重装app时，还没有考虑
        if(!str_mac.equals(getMac())){
            Log.v("sss", "mac_8");
            //把mac发给 first_check_mac.php 进行mac区配
            RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
            StringRequest stringRequest = new StringRequest(Request.Method.POST, str_reg_update, listener, errorListener) {
                @Override
                protected Map<String, String> getParams() throws AuthFailureError {
                    Map<String, String> map = new HashMap<String, String>();
                    map.put("bz", "check");
                    map.put("mac", getMac());
                    return map;
                }
            };
            requestQueue.add(stringRequest);

            Toast.makeText(getApplicationContext(), "第一次，请扫描注册",Toast.LENGTH_SHORT).show();

            Intent intent =new Intent(MainActivity.this,QrActivity.class);
            Bundle bundle=new Bundle();
            bundle.putString("mac", getMac());
            intent.putExtras(bundle);
            startActivity(intent);
            str_index = url+"workerman.php?first=yes";  //显示二维码时，webview显示为空
        }else{
            str_index = url+"workerman.php?first=no";
        }

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.v("sss", "WebView");
        myWebView = (WebView) findViewById(webView);
        myWebView.getSettings().setSupportZoom(true);
        myWebView.getSettings().setSupportMultipleWindows(true);
        myWebView.getSettings().setJavaScriptEnabled(true); //js
        myWebView.getSettings().setDomStorageEnabled(true);
        myWebView.setDrawingCacheEnabled(true);
        myWebView.addJavascriptInterface(new JSHook(), "hello");
        myWebView.setWebViewClient(new MyWebViewClient ());
        myWebView.setWebChromeClient(new MyChromeWebClient());  //进度条
      //  myWebView.setVerticalScrollBarEnabled(false); //垂直不显示
       // myWebView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);//滚动条在WebView内侧显示
      //  myWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);


        //handler.postDelayed(runnable, 5000);//每两秒执行一次runnable.
        // handler.removeCallbacks(runnable);  //3. 停止计时器
    }

    ///=================== Volley =====共用   php返回的数据（注册 重装） 在这里处理==============
    Response.Listener<String> listener = new Response.Listener<String>() {
        @Override
        public void onResponse(String s) {
            Log.v("sss_php", s);
            try {
                JSONObject jsonObject = new JSONObject(s);
                retCode = jsonObject.getString("success");
                first = jsonObject.getString("first");
                message=jsonObject.getString("message");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (retCode.equals("true")) {
                Toast.makeText(MainActivity.this, "OK!"+"_"+message, Toast.LENGTH_SHORT).show();
            }
            if (first.equals("false")) {
                //重装时检查 已注册过 //发送广播 关闭QrActivity
                Log.v("sss", "first==false");
                Intent intent = new Intent(action);
                intent.putExtra("data", "send_close");   //---发送广播 QrActivity [BroadcastReceiver broadcastReceiver = new BroadcastReceiver() ]----
                sendBroadcast(intent);
                str_index = url+"workerman.php?first=no";   //设置为不是第一次
            }else{
                //Toast.makeText(MainActivity.this, "用户名或密码错误!"+"_"+message, Toast.LENGTH_SHORT).show();
            }
        }
    };
    Response.ErrorListener errorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            Log.e("123", volleyError.getMessage(), volleyError);
        }
    };

    //==============android 和 js 交互===============
    public class JSHook{
        @JavascriptInterface
        public void javaMethod(String p){
            Log.d("sss" , "JSHook.JavaMethod() called! + "+p);
        }
        //android to js 把获取的手机 mac 发送到 网页上
        @JavascriptInterface
        public void showAndroid(){
            final String info = "来自手机内的内容！！！";
            MainActivity.this.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    myWebView.loadUrl("javascript:show('"+getMac()+"')");
                }
            });
        }
        //js to android（javascript:hello.getResult(a.percent);）
        @JavascriptInterface
        public void getResult(String str){
            Toast.makeText(getApplicationContext(), str,Toast.LENGTH_SHORT).show();

            //手机二维码扫描登记后，要发送 “regedit_ok” 确认并关闭QrActiviey
            if(str.equals("regedit_ok")){

                Toast.makeText(getApplicationContext(), "登记成功",Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(action);
                intent.putExtra("data", "send_close");   //---发送广播 [关闭二维码窗口] QrActivity [BroadcastReceiver broadcastReceiver = new BroadcastReceiver() ]----
                sendBroadcast(intent);

                Log.v("sss", "regedit_ok");
                str_index = url+"workerman.php?first=no";   //设置为不是第一次
            }
            if(str.equals("cut")){

                screenshot("/sdcard/temp.jpg");
                uploadFile("/sdcard/temp.jpg");
            }
            if(str.equals("live")){
                myWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        // myWebView.loadUrl("http://112.116.119.47:88/live/");
                    }
                });
            }
            if(str.equals("cut_1")){
                handler.postDelayed(runnable, 5000);//每两秒执行一次runnable.
            }
            if(str.equals("cut_2")){
                handler.removeCallbacks(runnable);
            }
        }
        public String getInfo(){
            return "获取手机内的信息！！";
        }
    }

    //定时器 截图
    //import android.os.Handler;
    Handler handler=new Handler();
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.v("sss_php", "22222");
            String s ="/sdcard/"+getStringDate()+".jpg";
            screenshot("/sdcard/temp.jpg");
            uploadFile("/sdcard/temp.jpg");

            handler.postDelayed(this, 5000);
        }
    };

    //---------只能截打开时的屏  如果改变过屏方向 也可以截出不同的来---------------
    final private String up_url = url+"up_pic.php?mac="+getMac();
    private void screenshot(final String filePath){
        Log.v("sss", up_url);
//        View dView = getWindow().getDecorView();
//        dView.setDrawingCacheEnabled(true);
//        dView.buildDrawingCache();
//        Bitmap bitmap = Bitmap.createBitmap(dView.getDrawingCache());

        myWebView.setDrawingCacheEnabled(true);
        Bitmap bitmap = myWebView.getDrawingCache();




        if (bitmap != null) {
            try {
                // 获取内置SD卡路径
                String sdCardPath = Environment.getExternalStorageDirectory().getPath();
                // 图片文件路径
                File file = new File(filePath);
                FileOutputStream os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                os.close();
                Log.d("sss", "存储完成");
                Toast.makeText(getApplicationContext(), "截屏！",Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }

        myWebView.setDrawingCacheEnabled(false);
        //dView.setDrawingCacheEnabled(false);
    }

    /**
     * 上传图片到服务端
     */
    private void uploadFile(final String filePath) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                String end = "\r\n";
                String twoHyphens = "--";
                String boundary = "******";
                try {
                    URL url = new URL(up_url);
                    HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                    // 设置每次传输的流大小
                    httpURLConnection.setChunkedStreamingMode(128 * 1024); //128K
                    // 允许输入输出流
                    httpURLConnection.setDoInput(true);
                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setUseCaches(false);
                    // 使用 POST 方法
                    httpURLConnection.setRequestMethod("POST");
                    httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
                    httpURLConnection.setRequestProperty("Charset", "UTF-8");
                    httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    DataOutputStream dos = new DataOutputStream(httpURLConnection.getOutputStream());
                    dos.writeBytes(twoHyphens + boundary + end);
                    // 设置 name 为 file
                    dos.writeBytes("Content-Disposition: form-data; aaaa=bbbbb;name=\"file\"; filename=\""
                            + filePath.substring(filePath.lastIndexOf("/") + 1)
                            + "\""
                            + end);
                    dos.writeBytes(end);
                    FileInputStream fis = new FileInputStream(filePath);
                    byte[] buffer = new byte[8192]; // 8k
                    int count = 0;
                    // 读取文件
                    while ((count = fis.read(buffer)) != -1) {
                        dos.write(buffer, 0, count);
                    }
                    fis.close();
                    dos.writeBytes(end);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + end);
                    dos.flush();


                     Log.v("sss", filePath.substring(filePath.lastIndexOf("/")+1));

                    // ResponseCode 可以用来判断错误类型
                    // int status = httpURLConnection.getResponseCode();
                    InputStream is = httpURLConnection.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is, "utf-8");
                    BufferedReader br = new BufferedReader(isr);
                    // 获取返回内容
                    String info = br.readLine();
                    Log.v("sss",info);

                    Toast.makeText(getApplicationContext(), "上传完成！",Toast.LENGTH_SHORT).show();
                    dos.close();
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    ///////////-----------------获取mac------------------------------------
    private String getMac()
    {
        String macSerial_eth0 = null;
        String str2 = "";
        try
        {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str2;)
            {
                str2 = input.readLine();
                if (str2 != null)
                {
                    macSerial_eth0 = str2.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }

        String macSerial_WLAN0 = null;
        String str = "";
        try
        {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str;)
            {
                str = input.readLine();
                if (str != null)
                {
                    macSerial_WLAN0 = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            // 赋予默认值
            ex.printStackTrace();
        }

        if(macSerial_eth0!=null){
            return macSerial_eth0;
        }else{
            return macSerial_WLAN0;
        }
    }

    ////////////=============WebView 内部操作=====================
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
        @Override
        public void onPageStarted(WebView view, String url,Bitmap favicon) {//网页页面开始加载的时候
            if (progressBar == null) {
                progressBar=new ProgressDialog(MainActivity.this);
                progressBar.setMessage("数据加载中，请稍后...");
                progressBar.show();
                myWebView.setEnabled(false);// 当加载网页的时候将网页进行隐藏
            }
            super.onPageStarted(view, url,favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {//网页加载结束的时候
            //super.onPageFinished(view, url);
            if (progressBar != null && progressBar.isShowing()) {
                progressBar.dismiss();
                progressBar = null;
                myWebView.setEnabled(true);
            }
        }
        @Override
        public void onReceivedError(WebView view, int errorCode,String description, String failingUrl) {
            Toast.makeText(MainActivity.this, "网页加载出错！", Toast.LENGTH_LONG);
        }
        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();  //webView默认是不处理https请求的，页面显示空白，需要进行如下设置
        }
    }
    ////////////=============WebView =====================
    public class MyChromeWebClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                bar.setVisibility(View.INVISIBLE);
            } else {
                if (View.INVISIBLE == bar.getVisibility()) {
                    bar.setVisibility(View.VISIBLE);
                }
                bar.setProgress(newProgress);
            }
            super.onProgressChanged(view, newProgress);
        }
    }

    @Override
    public boolean onKeyDown(int keyCoder,KeyEvent event)
    {
        //页面内回退
        if((keyCoder==KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()){
            myWebView.goBack();
            return true;
        }

        if (keyCoder == KeyEvent.KEYCODE_VOLUME_UP) {
            //myWebView.loadUrl("http://www.baidu.com");

            Toast.makeText(this, "声音+", Toast.LENGTH_SHORT).show();//----------
            return false;
        } else if (keyCoder == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Toast.makeText(this, "声音-", Toast.LENGTH_SHORT).show();//--------
            return false;
        } else if (keyCoder == KeyEvent.KEYCODE_VOLUME_MUTE) {
            Toast.makeText(this, "静音", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_DPAD_UP) {
            Toast.makeText(this, "向上", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_DPAD_DOWN) {
            Toast.makeText(this, "向下", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_DPAD_LEFT) {
            Toast.makeText(this, "向左", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_DPAD_RIGHT) {
            Toast.makeText(this, "向右", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_DPAD_CENTER) {
            Toast.makeText(this, "KEYCODE_DPAD_CENTER", Toast.LENGTH_SHORT).show();
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_BACK) {
            Toast.makeText(this, "返回", Toast.LENGTH_SHORT).show();//-----------
            return false;
        }else if (keyCoder == KeyEvent.KEYCODE_MENU) {
            Toast.makeText(this, "菜单", Toast.LENGTH_SHORT).show();//-----------
            return false;
        } else if (keyCoder == KeyEvent.KEYCODE_HOME) {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
            return false;
        }
        //else if (keyCoder == KeyEvent.KEYCODE_DPAD_CENTER) {
         //   Toast.makeText(this, "KEYCODE_DPAD_CENTER", Toast.LENGTH_SHORT).show();
          //  return false;
       // }

        return super.onKeyDown(keyCoder,event);
    }

    //==================生命周期=======================

    @Override       //用户可以看到部分activity但不能与它交互   【只有打开时显示一次】
    protected void onStart() {
        super.onStart();
        Log.v("sss", "start onStart~~~");
        //把mac发给 reg_update.php 进行   1、新装tv  2、开机时
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, str_reg_update, listener, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("user", "admin");
                map.put("bz", "login");
                map.put("mac", getMac());
                return map;
            }
        };
        requestQueue.add(stringRequest);
        //调到了onResume() 那里
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.v("sss", "start onRestart~~~");
        //好像没有用到
    }

    @Override   //是当该activity与用户能进行交互时被执行，用户可以获得activity的焦点，能够与用户交互 【可能被调用多次 如被盖住，又显示时】
    protected void onResume() {
        super.onResume();
        Log.v("sss", "start onResume~~~");
        myWebView.loadUrl(str_index);
        Log.v("sss_web_index", str_index);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v("sss", "start onPause~~~");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("sss", "start onStop~~~");

        //把mac发给 reg_update.php 进行   退出时
        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, str_reg_update, listener, errorListener) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> map = new HashMap<String, String>();
                map.put("user", "admin");
                map.put("bz", "logout");
                map.put("mac", getMac());
                return map;
            }
        };
        requestQueue.add(stringRequest);

        myWebView.loadUrl("");
        str_index=url+"workerman.php?first=no";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v("sss", "start onDestroy~~~");

        if (myWebView != null) {
            myWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            myWebView.clearHistory();

            ((ViewGroup) myWebView.getParent()).removeView(myWebView);
            myWebView.destroy();
            myWebView = null;
        }
    }

    public static String getStringDate() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }
}