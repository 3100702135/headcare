package com.example.administrator.headcare;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
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

    private BluetoothAdapter adapter = null;
    private BluetoothManager bluetoothManager;
    private BluetoothDevice device = null;
    private BluetoothSocket btSocket  = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");  //这条是蓝牙串口通用的UUID，不要更改
    private static String address = "b8:76:3f:ed:d0:a4"; // <==要连接的蓝牙设备MAC地址
    private EditText message;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        final TextView description=(TextView)findViewById(R.id.description);

        SeekBar seekBarLigft= (SeekBar) findViewById(R.id.seekBarLigft);//拿到控件实例
        seekBarLigft.setMax(100);//为控件设置大小
        seekBarLigft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                description.setText("当前亮度："+progress);
            }
        });

        SeekBar seekBarTime= (SeekBar) findViewById(R.id.seekBarTime);//拿到控件实例
        seekBarTime.setMax(100);//为控件设置大小
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
                description.setText("设置时间："+progress+"分钟后，关闭灯光");
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
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
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
        adapter = BluetoothAdapter.getDefaultAdapter();
        //adapter不等于null，说明本机有蓝牙设备
        if(adapter != null){
            System.out.println("本机有蓝牙设备！");
            //如果蓝牙设备未开启
            if(adapter !=null){
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                //请求开启蓝牙设备
                startActivity(intent);
                ensureDiscoverable();
            }
            //获得已配对的远程蓝牙设备的集合
            Set<BluetoothDevice> devices = adapter.getBondedDevices();
            if(devices.size()>0){
                for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();){
                    BluetoothDevice device = (BluetoothDevice)it.next();
                    //打印出远程蓝牙设备的物理地址
                    show(device.getName());
                    if(device.getName().equals("EDIFIER W800BT"))
                    {
                        show(device.getName());
                        show(device.getAddress());
//                        BluetoothGatt mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                    }

                }
            }else{
                show("还没有已配对的远程蓝牙设备！");
            }
        }else{
            System.out.println("本机没有蓝牙设备！");
        }
    }

    // Function to send Bluetooth message
    public void SendStr(String str) {
        byte[] bf = new byte[33];
        bf = str.getBytes();
        if((!str.equals("")) && (btSocket!=null)) {
            try {
                outStream = btSocket.getOutputStream();
                outStream.write(bf);
                outStream.write('\0');    // Send an ending sign
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (outStream != null) {
                    outStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //打开灯板
    public  void  openPart(View v)
    {
//        SendStr("helloWord");
        final TextView description=(TextView)findViewById(R.id.description);
        int viewId = v.getId();
        if (viewId == R.id.imageButton1) {
            description.setText("点亮1");
        } else if (viewId == R.id.imageButton2) {
            description.setText("点亮2");
        }else if (viewId == R.id.imageButton3) {
            description.setText("点亮3");
        }


    }

    //使本机蓝牙在300秒内可被搜索
    private void ensureDiscoverable() {
        if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

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


    private void  showChoose()
    {
//        final String items[] = {"我是Item一", "我是Item二", "我是Item三", "我是Item四"};
        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setIcon(R.mipmap.icon)//设置标题的图片
                .setTitle("单选列表对话框")//设置对话框的标题
                .setSingleChoiceItems(items, 1, new DialogInterface.OnClickListener() {
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
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
    }
}
