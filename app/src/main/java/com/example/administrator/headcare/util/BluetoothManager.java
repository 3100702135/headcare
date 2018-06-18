package com.example.administrator.headcare.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.example.administrator.headcare.MainActivity;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Thread.sleep;

public  class BluetoothManager implements IBluetoothManager {
    //*********************************************************//
    /**
     * 蓝牙模块被外面可见时间
     */
    private static final int BLUETOOTH_VISIBLE_TIME = 300;
    public static final String BLUETOOTH_SOCKET_NAME = "Headcare";
    public static final String pin = "1234";
    //系统蓝牙设备帮助类
    private BluetoothAdapter mBluetoothAdapter;
    //监听蓝牙设备的广播
    private BluetoothReceiver mBluetoothReceiver;
    //已发现的设备列表
    private List<BluetoothDevice> mFoundDeviceList = new ArrayList<BluetoothDevice>();
    //已绑定的设备列表
    private List<BluetoothDevice> mBoundedDeviceList = new ArrayList<BluetoothDevice>();
    /**
     * 判断设备是否匹配目标的接口
     */
    public String receiveString = "";
    //作为服务端的socket
    private BluetoothSocket mClientBluetoothSocket;
    //作为客户端的读取socket
    private HashMap<String, BluetoothSocket> mClientCommunicateBluetoothSocketMap = new HashMap<String, BluetoothSocket>();
    //当前正在进行绑定操作的设备列表
    private CopyOnWriteArrayList<String> mBoundingDeviceFlagList = new CopyOnWriteArrayList<String>();
    public MainActivity mainActivity;
    private static final String TAG = "BluetoothManager";
    /**
     * 客户端的UUID
     */
    private static final UUID UUID_CLIENT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //*********************单例相关代码****************************//
    private static BluetoothManager mInstance;
    private Context mContext;

    public BluetoothManager(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
    }

    public static BluetoothManager getInstance(Context context) {
        if (null == mInstance) {
            synchronized (BluetoothManager.class) {
                if (null == mInstance) {
                    mInstance = new BluetoothManager(context);
                }
            }
        }
        return mInstance;
    }


    @Override
    public void setBluetoothEventHandler(IBluetoothEventHandler eventHandler) {
        Log.d(TAG, "setBluetoothEventHandler()");
        mBluetoothEventHandler = eventHandler;
    }

    @Override
    public int enableBluetooth() {
        Log.d(TAG, "enableBluetooth()");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == mBluetoothAdapter) {
            //不支持蓝牙设备
            return BluetoothErrorCode.RESULT_ERROR_NOT_SUPPORT;
        }
        //各种广播状态监听
        IntentFilter filter = new IntentFilter();
        //搜索到某个蓝牙设备的广播
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        //蓝牙状态改变广播
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //蓝牙绑定状态改变广播
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //搜索蓝牙设备开始的广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //搜索蓝牙设备结束的广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mBluetoothReceiver = new BluetoothReceiver();
        mContext.registerReceiver(mBluetoothReceiver, filter);
        //如果蓝牙没有启动，则启动之
        if (!mBluetoothAdapter.isEnabled()) {
            //在蓝牙设备状态改变的广播中继续未完成的搜索设备功能
            mBluetoothAdapter.enable();
        }
        //使蓝牙设备可见，方便配对
        BluetoothUtil.makeBluetoothVisible(mContext, BLUETOOTH_VISIBLE_TIME);
        //搜索可以看到的设备
        searchAvailableDevice();
        return BluetoothErrorCode.RESULT_SUCCESS;
    }

    @Override
    public int searchAvailableDevice() {
        //当蓝牙设备没有启动成功时返回false
        boolean result = mBluetoothAdapter.startDiscovery();
        Log.d(TAG, "searchAvailableDevice()| triggered? " + result);
        return result ? BluetoothErrorCode.RESULT_SUCCESS : BluetoothErrorCode.RESULT_ERROR;
    }

    @Override
    public void connectBoundedDevice() {
        List<BluetoothDevice> boundedList = getBoundedDevices();
        if (null == boundedList || boundedList.size() <= 0) {
            return;
        }
        if (null == mBluetoothEventHandler) {
            return;
        }
        for (BluetoothDevice device : boundedList) {
            if (null == device) {
                continue;
            }
            if (mBluetoothEventHandler.isDeviceMatch(device)) {
                createConnection(device);
            }
        }
    }


    @Override
    public List<BluetoothDevice> getFoundedDevices() {
        Log.d(TAG, "getFoundedDevices()");
        return mFoundDeviceList;
    }

    public List<BluetoothDevice> getBoundedDevices() {
        Log.d(TAG, "getBoundedDevices()");
        Set<BluetoothDevice> boundedDeviceSet = mBluetoothAdapter.getBondedDevices();
        if (null != boundedDeviceSet) {
            mBoundedDeviceList.clear();
            mBoundedDeviceList.addAll(boundedDeviceSet);
        }
        return mBoundedDeviceList;
    }

    @Override
    public void createConnection(BluetoothDevice device) {
        Log.d(TAG, "createConnection() device= " + device);
        if (null == device) {
            return;
        }
        if (null != mBluetoothAdapter) {
            mBluetoothAdapter.cancelDiscovery();
        }
        String address = device.getAddress();
        if (null == address) {
            return;
        }
        //如果已经开始了创建连接的动作，则不再进行后续操作，否则会出现socket连接被覆盖，socket关闭等各种异常
        if (BluetoothUtil.isConnectionBegined(address)) {
            return;
        }
        BluetoothUtil.notifyBeginConnection(address);
        switch (device.getBondState()) {
            case BluetoothDevice.BOND_BONDED:
                // 已经绑定了，则创建指向目标的客户端socket
                createClientSocket(device);
                break;
            case BluetoothDevice.BOND_BONDING:
                // 正在绑定，加了待处理列表
                if (!isInBoundingList(address)) {
                    mBoundingDeviceFlagList.add(address);
                }
                break;
            case BluetoothDevice.BOND_NONE:
                // 已经绑定了，则创建指向目标的客户端socket
                try {
                    ClsUtils.createBond(device.getClass(), device);
                } catch (Exception e) {

                }

                break;
            default:
                boolean hasBound = BluetoothUtil.boundDeviceIfNeed(device);
                if (hasBound) {
                    // 触发了绑定，所以加入绑定集合
                    mBoundingDeviceFlagList.add(address);
                } else {
                    // 已经绑定了，则创建指向目标的客户端socket
                    createClientSocket(device);
                }
                break;
        }
    }

    public void createClientSocket(BluetoothDevice device) {
        int resultCode = IBluetoothEventHandler.RESULT_FAIL;
        try {
            mBluetoothAdapter.cancelDiscovery();
            mClientBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_CLIENT);
            mClientCommunicateBluetoothSocketMap.put(device.getAddress(), mClientBluetoothSocket);
            Log.d(TAG, "createClientSocket()| create a client socket success");
            resultCode = IBluetoothEventHandler.RESULT_SUCCESS;
            Log.v(TAG, "createClientSocket()| create a client socket success");
        } catch (Exception ex) {
            Log.d(TAG, "createClientSocket()| error happened", ex);
        }
        if (null != mBluetoothEventHandler) {
            mainActivity.mBluetoothEventHandler.onClientSocketConnectResult(device, resultCode);
        }
        //对外连接建立后需要开始监听从client链接写入的数据，并返回给外层
        readDataFromServerConnection(device.getAddress());
    }

    //此方法仅能执行一次，执行多次会在connect处报崩溃
    public void readDataFromServerConnection(final String deviceAddress) {
        Log.d(TAG, "readDataFromClientConnection() deviceAddress= " + deviceAddress);
        try {
            mClientBluetoothSocket = mClientCommunicateBluetoothSocketMap.get(deviceAddress);
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
        if (null == mClientBluetoothSocket) {
            Log.d(TAG, "readDataFromClientConnection()| socket is null");
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;

                try {
                    if (!mClientBluetoothSocket.isConnected()) {
                        mClientBluetoothSocket.connect();
                    }
                    byte[] tempData = new byte[256];
                    is = mClientBluetoothSocket.getInputStream();
                    while(null != is) {
                        //is的 read是阻塞的，来了数据才往下走
                        int bytesRead = is.read(tempData);
                        if(bytesRead == -1) {
                            continue;
                        }
                        if (null != mBluetoothEventHandler) {
//                            mBluetoothEventHandler.onReadServerSocketData(mClientBluetoothSocket.getRemoteDevice(), tempData, bytesRead);
                            mainActivity.mBluetoothEventHandler.reflash(tempData, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "readDataFromServerConnection()| read data failed", e);
//                    mainActivity.description.setText("连接蓝牙异常，请重试！");
                } finally {
                    //不能加is.close()，否则下次读失败
                }
            }
        });
        thread.start();
    }

    @Override
    public void writeDataToServerConnection(final String deviceAddress, final byte[] data) {
        Log.d(TAG, "writeDataToClientConnection() deviceAddress= " + deviceAddress);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputStream os = null;
                try {
                    if (null == mClientBluetoothSocket) {
                        mClientBluetoothSocket = mClientCommunicateBluetoothSocketMap.get(deviceAddress);
                    }
                    if (null == mClientBluetoothSocket) {
                        return;
                    }
                    if (!BluetoothUtil.isConnectionBegined(deviceAddress)) {
                        //标识已经开始了connect动作
                        //TODO 这里有个缺陷，如果这时候还没有触发建立连接操作，则以后都建立不了了
                        BluetoothUtil.notifyBeginConnection(deviceAddress);
                        //如果调用两次connect,会抛出异常
                        mClientBluetoothSocket.connect();
                        Log.d(TAG, "readDataFromClientConnection()| connect success");
                    }
                    //可能第一个线程卡在Connect方法内，第二个线程运行到此处了
                    //TODO 增加同步控制，否则两个线程会都运行到此处使用里面的outputStream
                    if (!mClientBluetoothSocket.isConnected()) {
                        mClientBluetoothSocket.connect();
                    }
                    byte[] tempData = new byte[4];
                    tempData[0]=data[0];
                    tempData[1]=data[1];
                    tempData[2]=data[2];
                    tempData[3]=(byte)(data[0]+data[1]+data[2]);
//                    data[3]=(byte)(data[0]+data[1]+data[2]);
                    os = mClientBluetoothSocket.getOutputStream();
                    os.write(tempData);
                    os.flush();
                    Log.d(TAG, "writeDataToClientConnection ()| success");
                } catch (Exception e) {
                    Log.d(TAG, "writeDataToClientConnection ()| write data failed", e);
                } finally {
                    if (null != os) {

                    }
                }
            }
        });
        thread.start();
    }


    @Override
    public void endConnection() {
        Log.d(TAG, "endConnection()");

        if (null != mBluetoothAdapter) {
            mBluetoothAdapter.cancelDiscovery();
        }
        try {
            if (null != mClientBluetoothSocket) {
                mClientBluetoothSocket.close();
                mClientBluetoothSocket = null;
            }
            Iterator<Map.Entry<String, BluetoothSocket>> iterator = mClientCommunicateBluetoothSocketMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, BluetoothSocket> entry = iterator.next();
                String deviceAddress = entry.getKey();
                Log.d(TAG, "endConnection()| free socket for :" + deviceAddress);
                BluetoothSocket clientSocket = entry.getValue();
                if (null != clientSocket) {
                    clientSocket.close();
                    clientSocket = null;
                }
            }
            mClientCommunicateBluetoothSocketMap.clear();
        } catch (Exception ex) {
            Log.d(TAG, "endConnection() failed", ex);
        }
    }

    @Override
    public void disableBluetooth() {
        Log.d(TAG, "disableBluetooth()");

        if (null != mBluetoothAdapter) {
            mBluetoothAdapter.disable();
        }
    }

    private boolean isInBoundingList(String deviceAddress) {
        if (null == deviceAddress) {
            return false;
        }
        Log.d(TAG, "deviceAddress = " + deviceAddress + " list=" + mBoundingDeviceFlagList);
        for (String address : mBoundingDeviceFlagList) {
            Log.d(TAG, "address = " + address);
            if (deviceAddress.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (null == intent) {
                return;
            }
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                Log.d(TAG, "onReceive()| state change state= " + state);
                if (BluetoothAdapter.STATE_ON == state) {
                    if (null != mBluetoothEventHandler) {
                        mBluetoothEventHandler.onBluetoothOn();
                    }
                } else if (BluetoothAdapter.STATE_OFF == state) {
                    if (null != mBluetoothEventHandler) {
                        mBluetoothEventHandler.onBluetoothOff();
                    }
                } else if (BluetoothAdapter.STATE_TURNING_OFF == state) {
                    if (null != mBluetoothEventHandler) {
                        mBluetoothEventHandler.onBluetoothOff();
                    }
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (null == device) {
                    Log.d(TAG, "onReceive()| device is null");
                    return;
                }
                Log.d(TAG, "onReceive()| device= " + device.getName() + "(" + device.getAddress() + ")");
                mFoundDeviceList.add(device);
                //自动连接因为对方没有应用没有起来而经常失败，这里不能用自动连接的方式，改为用户触发的方式比较好
                if (null != mBluetoothEventHandler && mBluetoothEventHandler.isDeviceMatch(device) && BLUETOOTH_SOCKET_NAME.equals(device.getName())) {
                    createClientSocket(device);
                }
            } else if ("android.bluetooth.device.action.PAIRING_REQUEST".equals(action)) {
                Log.d(TAG, "onReceive()| discovery started");
                try {
                    if (BLUETOOTH_SOCKET_NAME.equals(device.getName())) {
                        ClsUtils.setPairingConfirmation(device.getClass(), device, true);
                        abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                        //3.调用setPin方法进行配对...
                        boolean ret = ClsUtils.setPin(device.getClass(), device, pin);
                    }
                    if (null != mBluetoothEventHandler && mBluetoothEventHandler.isDeviceMatch(device) && BLUETOOTH_SOCKET_NAME.equals(device.getName())) {
                        createClientSocket(device);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "PAIRING_REQUEST()| PAIRING_REQUEST started");
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "onReceive()| discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(TAG, "onReceive()| discovery finished");
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // 绑定状态改变的广播
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive()| boundState change " + device + " list : " + mBoundingDeviceFlagList);
                if (null == device || null == mBoundingDeviceFlagList) {
                    //异常不需要处理
                    Log.d(TAG, "onReceive()| boundState change param is null");
                    return;
                }
                String address = device.getAddress();
                if (!isInBoundingList(address)) {
                    //不是我们触发的绑定结果我们不需要处理
                    Log.d(TAG, "onReceive()| boundState change no need handle");
                    return;
                }
                int boundState = device.getBondState();
                Log.d(TAG, "onReceive()| boundState= " + boundState);
                switch (boundState) {
                    case BluetoothDevice.BOND_BONDED:
                        mBoundingDeviceFlagList.remove(address);
                        // 已经绑定
                        createClientSocket(device);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public void destroy() {
        Log.d(TAG, "destroy()");
        mInstance = null;
        endConnection();
        disableBluetooth();
        if (null != mContext) {
            mContext.unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
            mContext = null;
        }
        mBluetoothAdapter = null;
        mFoundDeviceList = null;
        mBoundedDeviceList = null;
        mBluetoothEventHandler = null;
        mClientBluetoothSocket = null;
    }


    public IBluetoothEventHandler mBluetoothEventHandler = new IBluetoothEventHandler() {
        @Override
        public void onDeviceFound(BluetoothDevice device) {
            Log.d(TAG, "onDeviceFound() device= " + device);
            if (null == device) {
                return;
            }
        }
        @Override
        public void onServerSocketConnectResult(int resultCode) {
            Log.d(TAG, "onDeviceFound() device= " );

        }

        @Override
        public void onClientSocketConnectResult(final BluetoothDevice device, int resultCode) {
            Log.d(TAG, "onDeviceFound() device= " );
            if (null == device || RESULT_SUCCESS != resultCode) {
                return;
            }
        }

        /*读取服务端数据*/
        @Override
        public void onReadServerSocketData(BluetoothDevice remoteDevice, byte[] data, int length) {
            String receiveStr = new String(data);
            try {
                receiveStr = new String(data, 0, length, "utf-8");
                if(receiveStr.length()<3)
                {
                    receiveString=receiveString+receiveStr;
                }
                if (receiveString.length()>=3)
                {
//                    traslateString(receiveString);//界面赋值
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "onReadServerSocketData: "+receiveString);
        }

        @Override
        public boolean isDeviceMatch(BluetoothDevice device) {
            if (null == device) {
                return false;
            }
            if (BLUETOOTH_SOCKET_NAME.equals(device.getName())) {
                return true;
            }
            return false;
        }


        @Override
        public void onBluetoothOn() {
//            mInstance.searchAvailableDevice();
            //modified 连接设备改为由用户去触发
            connectBoundedDevice();
        }

        @Override
        public void onBluetoothOff() {
            // TODO Auto-generated method stub
            Log.d(TAG, "onDeviceFound() device= " );

        }

        @Override
        public void reflash( byte[] data, int length) {

        }
    };

//    public String traslateString(String strData)
//    {
////        if (strData ==null || strData.length()<3)
////        {
////            return "";
////        }
////        String strFlag ="";
////        String strValue ="";
////        if(strData.length()>=3)
////        {
////            Message msg = new Message();
////            Bundle bundle = new Bundle();
////            bundle.putString("data",strData);
////            msg.setData(bundle);
//////            mainActivity.mHandler.sendMessageDelayed(msg,0);
//////            strFlag = strData.substring(0,1);//截取前五位标志位
//////            strValue = strData.substring(1,3);//截取中间两位数据位
//////            switch (strFlag)
//////            {
//////                case BluetoothStrEnum.temp:
//////                    mainActivity.description.setText("同步温度："+strValue);
//////                    mainActivity.textViewTemp.setText(strValue+"℃");
//////                    Toast.makeText(mContext, "同步温度"+strValue+"℃", Toast.LENGTH_SHORT).show();
//////                    break;
//////                case BluetoothStrEnum.power:
//////                    mainActivity.description.setText("同步电量："+strValue);
//////                    mainActivity.waveViewCircle.setmProgress(Integer.getInteger(strValue));
//////                    Toast.makeText(mContext, "同步电量："+strValue+"%", Toast.LENGTH_SHORT).show();
//////                    break;
//////                default:
//////                    break;
//////            }
////        }
////        receiveString="";
////        return strValue;
//    }



}
