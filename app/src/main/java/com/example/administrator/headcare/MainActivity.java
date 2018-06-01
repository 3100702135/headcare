package com.example.administrator.headcare;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.headcare.util.BluetoothManager;
import com.example.administrator.headcare.util.BluetoothReceiver;
import com.example.administrator.headcare.util.TimerTextView;
import com.example.administrator.headcare.util.VerticalSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private  String powerValue;//电量值
    private  String timeValue;//设置的照射时间
    private  String lightValue;//设置的照射亮度
    private  String partF ="partF";//前区域
    private  String partT  ="partT";//上区域
    private  String partB ="partB";//后区域
    private  String items[];
    private  String clickAddress;

    ArrayList<String> blueList = new ArrayList<String>();
    private BluetoothAdapter adapter = null;
    private BluetoothManager mBluetoothManager = new BluetoothManager(this);
    private BluetoothDevice device = null;
    private BluetoothSocket btSocket  = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static final UUID MY_UUID = UUID.fromString("d2ea0fdc-1982-40e1-98e8-9dcd45130b8e");  //这条是蓝牙串口通用的UUID，不要更改
    private static String address = "b8:76:3f:ed:d0:a4"; // <==要连接的蓝牙设备MAC地址
    private EditText message;
    private TextView description;
    private BluetoothReceiver mReceiver = new BluetoothReceiver();
    HashMap<String,String> blueMap = new HashMap<String,String>();
    String objName = "YOGA";
    private String msg = "我是第?次通话 ";
    private int i = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        description=(TextView)findViewById(R.id.description);

        VerticalSeekBar verticalSeekbarF= (VerticalSeekBar) findViewById(R.id.verticalSeekbarF);//拿到前额控件实例
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
                description.setText("前额亮度："+progress+"%");
            }
        });

        VerticalSeekBar verticalSeekbarT= (VerticalSeekBar) findViewById(R.id.verticalSeekbarT);//拿到前额控件实例
        verticalSeekbarT.setMax(100);//为控件设置大小
        verticalSeekbarT.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                description.setText("头顶亮度："+progress+"%");
            }
        });

        VerticalSeekBar verticalSeekbarB= (VerticalSeekBar) findViewById(R.id.verticalSeekbarB);//拿到前额控件实例
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
                description.setText("后枕亮度："+progress+"%");
            }
        });

        //初始化倒计时控件
        final TimerTextView timerTextView =findViewById(R.id.timer_text_view);
        SeekBar seekBarTime= (SeekBar) findViewById(R.id.seekBarTime);//拿到控件实例
        seekBarTime.setMax(50);//为控件设置大小
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
                timerTextView.stopRun();
                timerTextView.destroyDrawingCache();
                description.setText("设置时间："+progress+"分钟后，关闭灯光");
                long[] times = {progress,0};
                timerTextView.setTimes(times);
                timerTextView.beginRun();
                SendStr("time"+progress);
            }
        });
        //打开，连接蓝牙
        startActivityForResult();
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
//        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
//        if (id == R.id.blueItem) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void startActivityForResult() {
        //获得BluetoothAdapter对象，该API是android 2.0开始支持的
//        adapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothManager.enableBluetooth();
        List<BluetoothDevice> boundedList = mBluetoothManager.getBoundedDevices();
        if(null != boundedList) {
            for(BluetoothDevice device : boundedList) {
                if(device.getName().contains(objName)) {
                    mBluetoothManager.createConnection(device);
                }
            }
        }
        Log.d("TAG", "create connection for bounded devices");
        List<BluetoothDevice> foundedList = mBluetoothManager.getFoundedDevices();
        if(null != foundedList) {
            for(BluetoothDevice device : foundedList) {
                if(device.getName().contains(objName)) {
                    mBluetoothManager.createConnection(device);
                }
            }
        }
        Log.d("TAG", "create connection for found devices");
    }

    // Function to send Bluetooth message
    public void SendStr(String str) {
        BluetoothDevice targetDevice = null;
        targetDevice = GetBluetoothDevice(mBluetoothManager);
        if(null == targetDevice) {
            Toast.makeText(getApplicationContext(), "未连接蓝牙设备", Toast.LENGTH_SHORT).show();
            return;
        }
        mBluetoothManager.writeDataToClientConnection(targetDevice.getAddress(),("来自客户端手机端消息：" + str).getBytes());

    }

    //刷新蓝牙
    public  BluetoothDevice  GetBluetoothDevice(BluetoothManager mBluetoothManager)
    {
        if(mBluetoothManager==null)
        {
            return null;
        }
        BluetoothDevice targetDevice = null;
        List<BluetoothDevice> boundedList = mBluetoothManager.getBoundedDevices();
        if(null != boundedList) {
            for(BluetoothDevice device : boundedList) {
                if(device.getName().contains(objName)) {
                    targetDevice = device;
                    break;
                }
            }
        }
        List<BluetoothDevice> foundedList = mBluetoothManager.getFoundedDevices();
        if(null != foundedList) {
            for(BluetoothDevice device : foundedList) {
                if(device.getName().contains(objName)) {
                    targetDevice = device;
                    break;
                }
            }
        }
        return targetDevice;
    }

    //打开灯板
    public  void  openPart(View v)
    {
//        SendStr("helloWord");

    }

//    //刷新蓝牙
//    public  void  reflashBlue(View v)
//    {
////        SendStr("helloWord");
//        startActivityForResult();
//
//    }

//    //使本机蓝牙在300秒内可被搜索
//    private void ensureDiscoverable() {
//        if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
//            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(discoverableIntent);
//        }
//        //当蓝牙设备没有启动成功时返回false
//        boolean result = adapter.startDiscovery();
//        //获得已配对的远程蓝牙设备的集合
//        blueList.clear();
//        blueMap.clear();
//        Set<BluetoothDevice> devices = adapter.getBondedDevices();
////        if(devices.size()>0){
////            for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();){
////                BluetoothDevice device = (BluetoothDevice)it.next();
////                //打印出远程蓝牙设备的物理地址
////                blueList.add(device.getName());
////                blueMap.put(device.getName(),device.getAddress());
////            }
////        }else{
////            show("还没有已配对的远程蓝牙设备！");
////        }
////        showChoose(blueList.toArray(new String[blueList.size()]));
//        }

    private void  show( String message)
    {
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


    private void  showChoose(final String items[])
    {
//        final String items[] = {"我是Item一", "我是Item二", "我是Item三", "我是Item四"};
        HashMap<String, String> map = new HashMap<String, String>();
        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("已搜索到的蓝牙设备")//设置对话框的标题
                .setSingleChoiceItems(items, items.length-1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, items[which], Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        description.setText(items[which+1]);
                        clickAddress =blueMap.get(items[which+1]);
                        dialog.dismiss();
                        adapter.cancelDiscovery();
                        new BlueThread().start();
                    }
                }).create();
        dialog.show();
    }

    public class BlueThread extends Thread {
        //继承Thread类，并改写其run方法
        public void run(){
            BluetoothDevice device = adapter.getRemoteDevice(clickAddress);
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Log.e("error", "ON RESUME: BT connection established, data transfer link open.");
            } catch (IOException e) {
                try {
                    try {
                        btSocket =(BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device,1);
                        btSocket.connect();
                    } catch (IllegalAccessException e1) {
                        e1.printStackTrace();
                    } catch (InvocationTargetException e1) {
                        e1.printStackTrace();
                    } catch (NoSuchMethodException e1) {
                        e1.printStackTrace();
                    }
                } catch (IOException e2) {
                    try {
                        btSocket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        Log.e("error", "关闭蓝牙异常", e2);
                    }
                    Log.e("error", "连接蓝牙异常", e2);
                }
            }
        }
    }


}
