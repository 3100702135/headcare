package com.example.administrator.headcare;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;

import com.example.administrator.headcare.toolBars.MyTempView;
import com.example.administrator.headcare.toolBars.Themometer;
import com.example.administrator.headcare.util.BluetoothStrEnum;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chipsen.bleservice.BluetoothLeService;
import com.example.administrator.headcare.toolBars.LD_WaveView;
import com.example.administrator.headcare.util.BluetoothManager;
import com.example.administrator.headcare.util.BluetoothReceiver;
import com.example.administrator.headcare.util.ClsUtils;
import com.example.administrator.headcare.util.IBluetoothManager;
import com.example.administrator.headcare.util.IBluetoothManager.IBluetoothEventHandler;
import com.example.administrator.headcare.util.TimerTextView;
import com.example.administrator.headcare.util.VerticalSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private String powerValue;//电量值
    private String temp;//温度值
    private String timeMin;//剩余照射时间分钟
    private String timeSec;//剩余照射时间秒
    private String FLight;//设置的前照射亮度
    private String MLight;//设置的中射亮度
    private String BLight;//设置的后射亮度
    private String items[];
    private String clickAddress;

    ArrayList<String> blueList = new ArrayList<String>();
    private BluetoothAdapter adapter = null;
    private BluetoothManager mBluetoothManager = new BluetoothManager(this);
    private BluetoothDevice device = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  //这条是蓝牙串口通用的UUID，不要更改
    private EditText message;
    public TextView description;//显示提示控件
    public LD_WaveView waveViewCircle;//电量显示控件
    public TextView textViewTemp;//温度显示控件
    public TextView textViewTime;//剩余时间显示
    public SeekBar seekBarTime ;//照射时间设定
    public  MenuItem blueFresh;//蓝牙刷新按钮
    private MyTempView mTempView;//动态温度计
    private Themometer mTView;//动态温度计

    public  VerticalSeekBar verticalSeekbarF;//前额亮度控件
    public  VerticalSeekBar verticalSeekbarM;//头顶亮度控件
    public  VerticalSeekBar verticalSeekbarB;//后额亮度控件
    private boolean run=false; //是否启动了
    private Context context;

    private BluetoothReceiver mReceiver = new BluetoothReceiver();
    HashMap<String, String> blueMap = new HashMap<String, String>();
    private int i = 0;
    private final String PERMISSION_REQUEST_COARSE_LOCATION = "\n";
    public String receiveString = "";
    Handler handler = new Handler();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this.context;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
            }
        }
        mTView = (Themometer) findViewById(R.id.themometer); //温度动态展示
        textViewTemp = findViewById(R.id.textViewTemp);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        description = (TextView) findViewById(R.id.description);
        waveViewCircle = (LD_WaveView) findViewById(R.id.waveViewCircle);//电量显示控件
        textViewTime = findViewById(R.id.textViewTime);
        verticalSeekbarF = (VerticalSeekBar) findViewById(R.id.verticalSeekbarF);//拿到前额控件实例
        verticalSeekbarF.setMax(100);//为控件设置大小
        verticalSeekbarF.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                description.setText("亮度生效");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                description.setText("开始设置");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==100)
                {
                    verticalSeekbarF.setProgress(99);
                    progress=99;
                }
                description.setText("前额亮度：" + progress + "%");
                SendStr(BluetoothStrEnum.partF + progress);
                Log.d("TAG", "设置前额亮度：" + progress);
            }
        });

        verticalSeekbarM = (VerticalSeekBar) findViewById(R.id.verticalSeekbarT);//拿到前额控件实例
        verticalSeekbarM.setMax(100);//为控件设置大小
        verticalSeekbarM.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                description.setText("亮度生效");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                description.setText("开始设置");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==100)
                {
                    verticalSeekbarM.setProgress(99);
                    progress=99;
                }
                description.setText("头顶亮度：" + progress + "%");
                SendStr(BluetoothStrEnum.partM + progress);
                Log.d("TAG", "设置头顶亮度：" + progress);
            }
        });

        verticalSeekbarB = (VerticalSeekBar) findViewById(R.id.verticalSeekbarB);//拿到前额控件实例
        verticalSeekbarB.setMax(100);//为控件设置大小
        verticalSeekbarB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                description.setText("亮度生效");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                description.setText("开始设置");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==100)
                {
                    verticalSeekbarB.setProgress(99);
                    progress=99;
                }
                description.setText("后枕亮度：" + progress + "%");
                SendStr(BluetoothStrEnum.partB + progress);
                Log.d("TAG", "设置后枕亮度：" + progress);
            }
        });

        //初始化倒计时控件
//        timerTextView = findViewById(R.id.timer_text_view);
        seekBarTime = (SeekBar) findViewById(R.id.seekBarTime);//拿到控件实例
        seekBarTime.setMax(35);//为控件设置大小
        seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                description.setText("设置生效");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                description.setText("开始设置");
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==Integer.parseInt(timeMin))
                {
                    return;
                }
                if(progress<10)
                {
                    verticalSeekbarB.setProgress(10);
                    progress=10;
                }
                description.setText("设置时间：" + progress + "分钟后，关闭灯光");
                SendStr(BluetoothStrEnum.timeMin + progress);
                Log.d("TAG", "设置时间：" + progress + "分钟");
            }
        });

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        blueFresh=  menu.findItem(R.id.blueFresh);
        //打开，连接蓝牙
        startActivityForResult();
        mBluetoothManager.setBluetoothEventHandler(mBluetoothManager.mBluetoothEventHandler);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.blueFresh && blueFresh.getTitle().equals("重新连接")) {
            startActivityForResult();
            mBluetoothManager = new BluetoothManager(this);
            mBluetoothManager.setBluetoothEventHandler(mBluetoothManager.mBluetoothEventHandler);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_gallery) //脱友经验
        {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, useBar.class);
            MainActivity.this.startActivity(intent);
        } else if (id == R.id.nav_slideshow)//使用教程
        {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, useCourse.class);
            MainActivity.this.startActivity(intent);

        } else if (id == R.id.nav_manage)//使用设置
        {

        } else if (id == R.id.nav_share)//分享
        {

        } else if (id == R.id.nav_send)//发送
        {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startActivityForResult() {
        //获得BluetoothAdapter对象，该API是android 2.0开始支持的
        try {
            mBluetoothManager.enableBluetooth();
            mBluetoothManager.mainActivity = this;
            List<BluetoothDevice> boundedList = mBluetoothManager.getBoundedDevices();
            if (null != boundedList) {
                for (BluetoothDevice device : boundedList) {
                    if (device.getName().contains(mBluetoothManager.BLUETOOTH_SOCKET_NAME)) {
                        mBluetoothManager.createConnection(device);
                        return;
                    }
                }
            }
            Log.d("TAG", "create connection for bounded devices");
            List<BluetoothDevice> foundedList = mBluetoothManager.getFoundedDevices();
            if (null != foundedList) {
                for (BluetoothDevice device : foundedList) {
                    if (device.getName().contains(mBluetoothManager.BLUETOOTH_SOCKET_NAME)) {
                        mBluetoothManager.createConnection(device);
                    }
                }
            }
            Log.v("TAG", "create connection for found devices");
        } catch (Exception e) {
            Log.v("TAG", "create connection for found devices Exception");
            this.show("连接蓝牙异常，请重试！");
        }

    }

    // Function to send Bluetooth message
    public void SendStr(String str) {
        BluetoothDevice targetDevice = null;
        //获取蓝牙服务
        targetDevice = GetBluetoothDevice(mBluetoothManager);
        if (null == targetDevice) {
            Toast.makeText(getApplicationContext(), "未连接蓝牙设备", Toast.LENGTH_SHORT).show();
            return;
        }
        //发送蓝牙指令
        mBluetoothManager.writeDataToServerConnection(targetDevice.getAddress(), str.getBytes());

    }

    //获取连接设备
    public BluetoothDevice GetBluetoothDevice(BluetoothManager mBluetoothManager) {
        if (mBluetoothManager == null) {
            return null;
        }
        BluetoothDevice targetDevice = null;
        List<BluetoothDevice> boundedList = mBluetoothManager.getBoundedDevices();
        Log.v("TAG", "绑定的设备：");
        if (null != boundedList) {
            for (BluetoothDevice device : boundedList) {
                if (device.getName().contains(mBluetoothManager.BLUETOOTH_SOCKET_NAME)) {
                    targetDevice = device;
                    Log.v("TAG", targetDevice.getName());
                    break;
                }
            }
        }
        List<BluetoothDevice> foundedList = mBluetoothManager.getFoundedDevices();
        Log.v("TAG", "发现的设备：");
        if (null != foundedList) {
            for (BluetoothDevice device : foundedList) {
                if (device.getName().contains(mBluetoothManager.BLUETOOTH_SOCKET_NAME)) {
                    targetDevice = device;
                    Log.v("TAG", targetDevice.getName());
                    break;
                }
            }
        }
        return targetDevice;
    }

    public void show(String message) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("提示")//设置对话框的标题
                .setMessage(message)//设置对话框的内容
                //设置对话框的按钮
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "点击了取消按钮", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "点击了确定的按钮", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }


//    private void  showChoose(final String items[])
//    {
////        final String items[] = {"我是Item一", "我是Item二", "我是Item三", "我是Item四"};
//        HashMap<String, String> map = new HashMap<String, String>();
//        AlertDialog dialog = new AlertDialog.Builder(this)
////                .setIcon(R.mipmap.icon)//设置标题的图片
//                .setTitle("已搜索到的蓝牙设备")//设置对话框的标题
//                .setSingleChoiceItems(items, items.length-1, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
//                    }
//                })
//                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        description.setText(items[which+1]);
//                        clickAddress =blueMap.get(items[which+1]);
//                        dialog.dismiss();
//                        adapter.cancelDiscovery();
//                        new BlueThread().start();
//                    }
//                }).create();
//        dialog.show();
//    }


    public IBluetoothEventHandler mBluetoothEventHandler = new IBluetoothEventHandler() {

        @Override
        public boolean isDeviceMatch(BluetoothDevice device) {
            return false;
        }

        @Override
        public void onServerSocketConnectResult(int resultCode) {

        }

        @Override
        public void onDeviceFound(BluetoothDevice device) {

        }

        @Override
        public void onClientSocketConnectResult(BluetoothDevice device, int resultCode) {
            Log.d("TAG", "重新连接 " );
            if (null == device || RESULT_SUCCESS != resultCode) {
                blueFresh.setTitle("重新连接");
            }
            else
            {
                blueFresh.setTitle("已连接");
            }
        }

        @Override
        public void onReadServerSocketData(BluetoothDevice remoteDevice, byte[] data, int length) {

        }

        @Override
        public void onBluetoothOn() {
            description.setText("蓝牙已打开");
        }

        @Override
        public void onBluetoothOff() {
            description.setText("蓝牙已断开");
        }

        /*读取服务端数据*/
        @Override
        public void reflash( byte[] data, int length) {
            String receiveStr = new String(data);
            try {
                receiveStr = new String(data, 0, length, "utf-8");
                if (receiveStr.length()>=4)
                {
                    String strFlag ="";
                    String strValue ="";
                    strFlag = receiveStr.substring(0,1);//截取前五位标志位
                    strValue = receiveStr.substring(1,3);//截取中间两位数据位
                    switch (strFlag)
                    {
                        case BluetoothStrEnum.temp:
                            temp=strValue;
                            break;
                        case BluetoothStrEnum.power:
                            powerValue=strValue;
                            break;
                        case BluetoothStrEnum.timeMin:
                            timeMin=strValue;
                            break;
                        case BluetoothStrEnum.timeSec:
                            timeSec=strValue;
                            break;
                        case BluetoothStrEnum.partF:
                            FLight=strValue;
                            break;
                        case BluetoothStrEnum.partM:
                            MLight=strValue;
                            break;
                        case BluetoothStrEnum.partB:
                            BLight=strValue;
                            break;
                        default:
                            break;
                    }
                    new Thread(){
                        public void run(){
                            handler.post(runnableUi);
                        }
                    }.start();
                    receiveString="";
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    // 构建Runnable对象，在runnable中更新界面
    Runnable   runnableUi=new  Runnable(){
        @Override
        public void run() {
            try{
                description.setText("接收数据："+receiveString);
                //更新界面
                if (temp!=null && !temp.equals(""))
                {
                    description.setText("同步温度："+temp+"℃");
                    mTView.setTemperature(Integer.parseInt(temp));
                    textViewTemp.setText(temp+"℃");
                    Toast.makeText(MainActivity.this, "同步温度："+temp+"℃", Toast.LENGTH_SHORT).show();
                    temp="";
                }
                if (powerValue!=null && !powerValue.equals(""))
                {
                    description.setText("同步电量："+powerValue+"%");
                    waveViewCircle.setmProgress(Integer.parseInt(powerValue));
                    Toast.makeText(MainActivity.this, "同步电量："+powerValue+"%", Toast.LENGTH_SHORT).show();
                    powerValue="";
                }
                if (timeMin!=null && !timeMin.equals("")) {

                }
                if (timeSec!=null && !timeSec.equals("")) {
                    Toast.makeText(MainActivity.this, "同步设置时间："+timeMin+"分钟", Toast.LENGTH_SHORT).show();
                    description.setText("设置时间：" + timeMin + "分钟后，关闭灯光");
                    seekBarTime.setProgress(Integer.parseInt(timeMin));
                    String strTime=  "剩余时间："+timeMin+"分钟:"+timeSec+"秒";
                    textViewTime.setText(strTime);
                    timeSec=null;
                }
//                if (FLight!=null && !FLight.equals("")) {
//                    Toast.makeText(MainActivity.this, "同步前额亮度："+FLight+"%", Toast.LENGTH_SHORT).show();
//                    description.setText("同步前额亮度：" + FLight + "%");
//                    verticalSeekbarF.setProgress(Integer.parseInt(FLight));
//                    FLight="";
//                }
//                if (MLight!=null && !MLight.equals("")) {
//                    Toast.makeText(MainActivity.this, "同步顶部亮度："+MLight+"%", Toast.LENGTH_SHORT).show();
//                    description.setText("同步顶部亮度：" + MLight + "%");
//                    verticalSeekbarM.setProgress(Integer.parseInt(MLight));
//                    MLight="";
//                }
//                if (BLight!=null && !BLight.equals("")) {
//                    Toast.makeText(MainActivity.this, "同步后枕亮度："+BLight+"%", Toast.LENGTH_SHORT).show();
//                    description.setText("同步前额亮度：" + BLight + "%");
//                    verticalSeekbarB.setProgress(Integer.parseInt(BLight));
//                    BLight="";
//                }
            }
            catch (Exception e)
            {
                Log.d("TAG", "runnableUi()| runnableUi failed", e);
            }

        }

    };

}
