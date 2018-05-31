package com.example.administrator.headcare.util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public  class BluetoothManager implements IBluetoothManager {
    private static final String TAG = "BluetoothManager";

//  /**通用的UUID*/
//  private static final UUID UUID_COMMON = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**服务端的UUID*/
    private static final UUID UUID_SERVER = UUID.fromString("d2ea0fdc-1982-40e1-98e8-9dcd45130b8e");

    /**客户端的UUID*/
    private static final UUID UUID_CLIENT = UUID_SERVER;

    //*********************单例相关代码****************************//
    private static BluetoothManager mInstance;
    private Context mContext;

    private BluetoothManager(Context context) {
        mContext = context;
    }

    public static BluetoothManager getInstance(Context context) {
        if(null == mInstance) {
            synchronized (BluetoothManager.class) {
                if(null == mInstance) {
                    mInstance = new BluetoothManager(context);
                }
            }
        }
        return mInstance;
    }

    //*********************************************************//
    /**蓝牙模块被外面可见时间*/
    private static final int BLUETOOTH_VISIBLE_TIME = 200;
    private static final String BLUETOOTH_SOCKET_NAME = "BLUETOOTH_CONNECTION";
    //系统蓝牙设备帮助类
    private BluetoothAdapter mBluetoothAdapter;
    //监听蓝牙设备的广播
    private BluetoothReceiver mBluetoothReceiver;
    //已发现的设备列表
    private List<BluetoothDevice> mFoundDeviceList = new ArrayList<BluetoothDevice>();
    //已绑定的设备列表
    private List<BluetoothDevice> mBoundedDeviceList = new ArrayList<BluetoothDevice>();
    /**判断设备是否匹配目标的接口*/
    private IBluetoothEventHandler mBluetoothEventHandler;
    //服务器端Socket，由此获取服务端BluetoothSocket
    private BluetoothServerSocket mBluetoothServerSocket;
    //作为服务端的socket
    private BluetoothSocket mServerCommuicateBluetoothSocket;
    //作为客户端的socket, 可能有多个客户端
    private HashMap<String, BluetoothSocket> mClientCommunicateBluetoothSocketMap = new HashMap<String, BluetoothSocket>();

    //当前正在进行绑定操作的设备列表
    private CopyOnWriteArrayList<String> mBoundingDeviceFlagList = new CopyOnWriteArrayList<String>();

    @Override
    public void setBluetoothEventHandler(IBluetoothEventHandler eventHandler) {
        Log.d(TAG, "setBluetoothEventHandler()");
        mBluetoothEventHandler = eventHandler;
    }

    @Override
    public int enableBluetooth() {
        Log.d(TAG, "enableBluetooth()");
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(null == mBluetoothAdapter) {
            //不支持蓝牙设备
            return BluetoothErrorCode.RESULT_ERROR_NOT_SUPPORT;
        }
        //各种广播状态监听
        IntentFilter filter = new IntentFilter();
        //蓝牙状态改变广播
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        //蓝牙绑定状态改变广播
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        //搜索蓝牙设备开始的广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        //搜索蓝牙设备结束的广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        //搜索到某个蓝牙设备的广播
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        mBluetoothReceiver = new BluetoothReceiver();
        mContext.registerReceiver(mBluetoothReceiver, filter);
        //如果蓝牙没有启动，则启动之
        if(!mBluetoothAdapter.isEnabled()) {
            //在蓝牙设备状态改变的广播中继续未完成的搜索设备功能
            mBluetoothAdapter.enable();
        } else {
            //搜索可以看到的设备
            searchAvailableDevice();
            //连接已绑定集合中合适的设备
            //modified 连接设备改为由用户去触发
//          connectBoundedDevice();
        }
        //使蓝牙设备可见，方便配对
        BluetoothUtil.makeBluetoothVisible(mContext, BLUETOOTH_VISIBLE_TIME);

        return BluetoothErrorCode.RESULT_SUCCESS;
    }

    @Override
    public int searchAvailableDevice() {
        //当蓝牙设备没有启动成功时返回false
        boolean result = mBluetoothAdapter.startDiscovery();
        Log.d(TAG, "searchAvailableDevice()| triggered? " + result);
        //建立当前设备对外的监听
        createServerSocket();
        return result ? BluetoothErrorCode.RESULT_SUCCESS : BluetoothErrorCode.RESULT_ERROR;
    }

    @Override
    public void connectBoundedDevice() {
        List<BluetoothDevice> boundedList = getBoundedDevices();
        if(null == boundedList || boundedList.size() <= 0) {
            return;
        }
        if(null == mBluetoothEventHandler) {
            return;
        }
        for(BluetoothDevice device : boundedList) {
            if(null == device) {
                continue;
            }
            if(mBluetoothEventHandler.isDeviceMatch(device)) {
                createConnection(device);
            }
        }
    }

    private void createServerSocket() {
        int resultCode = IBluetoothEventHandler.RESULT_FAIL;
        try {
            mBluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                    BLUETOOTH_SOCKET_NAME, UUID_SERVER);
            Log.d(TAG, "createServerSocket()| wait for a client request");
            resultCode = IBluetoothEventHandler.RESULT_SUCCESS;
        } catch (Exception ex) {
            Log.d(TAG, "createServerSocket() | error happened", ex);
        }

        if(null != mBluetoothEventHandler) {
            mBluetoothEventHandler.onServerSocketConnectResult(resultCode);
        }
        //创建Server连接后进入监听数据进入的模式，有数据则对外回调数据
        readDataFromServerConnection();
    }



    @Override
    public List<BluetoothDevice> getFoundedDevices() {
        Log.d(TAG, "getFoundedDevices()");
        return mFoundDeviceList;
    }

    public List<BluetoothDevice> getBoundedDevices() {
        Log.d(TAG, "getBoundedDevices()");
        Set<BluetoothDevice> boundedDeviceSet = mBluetoothAdapter.getBondedDevices();
        if(null != boundedDeviceSet) {
            mBoundedDeviceList.clear();
            mBoundedDeviceList.addAll(boundedDeviceSet);
        }
        return mBoundedDeviceList;
    }

    @Override
    public void createConnection(BluetoothDevice device) {
        Log.d(TAG, "createConnection() device= " + device);
        if(null == device) {
            return;
        }

        if (null != mBluetoothAdapter) {
            mBluetoothAdapter.cancelDiscovery();
        }
        String address = device.getAddress();
        if(null == address) {
            return;
        }

        //如果已经开始了创建连接的动作，则不再进行后续操作，否则会出现socket连接被覆盖，socket关闭等各种异常
        if(BluetoothUtil.isConnectionBegined(address)) {
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
                if(!isInBoundingList(address)) {
                    mBoundingDeviceFlagList.add(address);
                }
                break;
            default:
                boolean hasBound = BluetoothUtil.boundDeviceIfNeed(device);
                if(hasBound) {
                    // 触发了绑定，所以加入绑定集合
                    mBoundingDeviceFlagList.add(address);
                } else {
                    //do nothing
                }
                break;
        }
    }

    private void createClientSocket(BluetoothDevice device) {
        int resultCode = IBluetoothEventHandler.RESULT_FAIL;
        BluetoothSocket clientBluetoothSocket = null;
        try {
            clientBluetoothSocket = device.createRfcommSocketToServiceRecord(UUID_CLIENT);
            mClientCommunicateBluetoothSocketMap.put(device.getAddress(), clientBluetoothSocket);
            Log.d(TAG, "createClientSocket()| create a client socket success");

            resultCode = IBluetoothEventHandler.RESULT_SUCCESS;
        } catch (Exception ex) {
            Log.d(TAG, "createClientSocket()| error happened", ex);
        }

        if (null != mBluetoothEventHandler) {
            mBluetoothEventHandler.onClientSocketConnectResult(device, resultCode);
        }

        //对外连接建立后需要开始监听从client链接写入的数据，并返回给外层
        readDataFromClientConnection(device.getAddress());
    }

    //标识是否触发过对服务Socket的读监听，已经开启读监听则不再开启
    private boolean mIsServerSocketReadTrigged = false;
    private void readDataFromServerConnection() {
        if(mIsServerSocketReadTrigged) {
            Log.d(TAG, "readDataFromServerConnection() has triggered, do nothing");
            return;
        }
        mIsServerSocketReadTrigged = true;

        Log.d(TAG, "readDataFromServerConnection()");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                InputStream is = null;
                try {
                    if(null == mServerCommuicateBluetoothSocket) {
                        mServerCommuicateBluetoothSocket = mBluetoothServerSocket.accept();
                        Log.d(TAG, "readDataFromServerConnection()| connect success");
                    }
                    is = mServerCommuicateBluetoothSocket.getInputStream();
                    byte[] tempData = new byte[256];
                    while(true) {
                        //is的 read是阻塞的，来了数据才往下走
                        int bytesRead = is.read(tempData);

                        if(bytesRead == -1) {
                            continue;
                        }

                        if(null != mBluetoothEventHandler) {
                            mBluetoothEventHandler.onReadServerSocketData(
                                    mServerCommuicateBluetoothSocket.getRemoteDevice(), tempData, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "readDataFromServerConnection()| read data failed", e);
                } finally {
                    //不能加is.close()，否则下次读失败
                }
            }
        });
        thread.start();
    }

    @Override
    public void writeDataToServerConnection(final byte[] data) {
        Log.d(TAG, "writeDataToServerConnection()");
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                OutputStream os = null;
                try {
                    if(null == mServerCommuicateBluetoothSocket) {
                        mServerCommuicateBluetoothSocket = mBluetoothServerSocket.accept();
                        Log.d(TAG, "writeDataToServerConnection()| connect success");
                    }
                    //TODO 增加多线程控制
                    os = mServerCommuicateBluetoothSocket.getOutputStream();
                    os.write(data);
                    os.flush();
                    Log.d(TAG, "writeDataToServerConnection()| write success");
                } catch (Exception e) {
                    Log.d(TAG, "writeDataToServerConnection()| write data failed", e);
                } finally {
                    //不能加os.close()，否则对方读失败
                }
            }
        });
        thread.start();
    }

    //此方法仅能执行一次，执行多次会在connect处报崩溃
    private void readDataFromClientConnection(final String deviceAddress) {
        Log.d(TAG, "readDataFromClientConnection() deviceAddress= " + deviceAddress);

        if(null == mClientCommunicateBluetoothSocketMap) {
            Log.d(TAG, "readDataFromClientConnection()| map is null");
            return;
        }
        final BluetoothSocket clientSocket = mClientCommunicateBluetoothSocketMap.get(deviceAddress);
        if(null == clientSocket) {
            Log.d(TAG, "readDataFromClientConnection()| socket is null");
            return;
        }

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                InputStream is = null;
                try {
                    // 如果调用两次connect,会抛出异常
                    clientSocket.connect();
                    Log.d(TAG, "readDataFromClientConnection()| connect success");
                    is = clientSocket.getInputStream();
                    byte[] tempData = new byte[256];
                    while (true) {
                        // is的 read是阻塞的，来了数据就往下走
                        int bytesRead = is.read(tempData);
                        if (bytesRead == -1) {
                            continue;
                        }
                        if (null != mBluetoothEventHandler) {
                            mBluetoothEventHandler.onReadClientSocketData(
                                    clientSocket.getRemoteDevice(), tempData, bytesRead);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "readDataFromClientConnection()|read data failed", e);
                } finally {
                    //不能调用is.close()，否则下次再读就失败了
                }
            }
        });
        thread.start();
    }

    @Override
    public void writeDataToClientConnection(final String deviceAddress, final byte[] data) {
        Log.d(TAG, "writeDataToClientConnection() deviceAddress= " + deviceAddress);
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                OutputStream os = null;
                try {
                    if(null == mClientCommunicateBluetoothSocketMap) {
                        return;
                    }
                    BluetoothSocket clientSocket = mClientCommunicateBluetoothSocketMap.get(deviceAddress);
                    if(null == clientSocket) {
                        return;
                    }
                    if (!BluetoothUtil.isConnectionBegined(deviceAddress)) {
                        //标识已经开始了connect动作
                        //TODO 这里有个缺陷，如果这时候还没有触发建立连接操作，则以后都建立不了了
                        BluetoothUtil.notifyBeginConnection(deviceAddress);
                        //如果调用两次connect,会抛出异常
                        clientSocket.connect();
                        Log.d(TAG, "readDataFromClientConnection()| connect success");
                    }

                    //可能第一个线程卡在Connect方法内，第二个线程运行到此处了
                    while(!BluetoothUtil.isConnected(clientSocket)) {
                        Thread.sleep(300);
                    }

                    //TODO 增加同步控制，否则两个线程会都运行到此处使用里面的outputStream
                    os = clientSocket.getOutputStream();
                    os.write(data);
                    os.flush();
                    Log.d(TAG, "writeDataToClientConnection ()| success");
                } catch (Exception e) {
                    Log.d(TAG, "writeDataToClientConnection ()| write data failed", e);
                } finally {
//                  if(null != os) {
                    //这里要注意, 不能close, 否则对方收不到消息
                    //报错:java.io.IOException: bt socket closed, read return: -1
//                      try {
//                          //os.close();
//                          os = null;
//                      } catch (Exception e) {
//                          Log.d(TAG, "writeDataToClientConnection ()| close outputstream failed", e);
//                      }
//                  }
                }
            }
        });
        thread.start();
    }

    @Override
    public void endConnection() {
        Log.d(TAG, "endConnection()");

        if(null != mBluetoothAdapter) {
            mBluetoothAdapter.cancelDiscovery();
        }
        try {
            if(null != mServerCommuicateBluetoothSocket) {
                mServerCommuicateBluetoothSocket.close();
                mServerCommuicateBluetoothSocket = null;
            }
            Iterator<Map.Entry<String, BluetoothSocket>> iterator = mClientCommunicateBluetoothSocketMap.entrySet().iterator();
            while(iterator.hasNext()) {
                Map.Entry<String, BluetoothSocket> entry = iterator.next();
                String deviceAddress = entry.getKey();
                Log.d(TAG, "endConnection()| free socket for :" + deviceAddress);
                BluetoothSocket clientSocket = entry.getValue();
                if(null != clientSocket) {
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

        if(null != mBluetoothAdapter) {
            mBluetoothAdapter.disable();
        }
    }

    private boolean isInBoundingList(String deviceAddress) {
        if(null == deviceAddress) {
            return false;
        }
        Log.d(TAG, "deviceAddress = " + deviceAddress + " list=" + mBoundingDeviceFlagList);
        for(String address : mBoundingDeviceFlagList) {
            Log.d(TAG, "address = " + address);
            if(deviceAddress.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null == intent) {
                return;
            }
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                Log.d(TAG, "onReceive()| state change state= " + state);
                if(BluetoothAdapter.STATE_ON == state) {
                    if(null != mBluetoothEventHandler) {
                        mBluetoothEventHandler.onBluetoothOn();
                    }
                } else if(BluetoothAdapter.STATE_OFF == state) {
                    if(null != mBluetoothEventHandler) {
                        mBluetoothEventHandler.onBluetoothOff();
                    }
                }
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(null == device) {
                    Log.d(TAG, "onReceive()| device is null");
                    return;
                }
                Log.d(TAG, "onReceive()| device= " + device.getName() + "(" + device.getAddress() + ")");
                mFoundDeviceList.add(device);
                if(null != mBluetoothEventHandler) {
                    mBluetoothEventHandler.onDeviceFound(device);
                }

                //自动连接因为对方没有应用没有起来而经常失败，这里不能用自动连接的方式，改为用户触发的方式比较好
//                if(null != mBluetoothEventHandler && mBluetoothEventHandler.isDeviceMatch(device)) {
//                  createConnection(device);
//                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.d(TAG, "onReceive()| discovery started");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                Log.d(TAG, "onReceive()| discovery finished");
            } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                // 绑定状态改变的广播
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d(TAG, "onReceive()| boundState change " + device + " list : " + mBoundingDeviceFlagList);
                if( null == device || null == mBoundingDeviceFlagList) {
                    //异常不需要处理
                    Log.d(TAG, "onReceive()| boundState change param is null");
                    return;
                }
                String address = device.getAddress();
                if(!isInBoundingList(address)) {
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
        if(null != mContext) {
            mContext.unregisterReceiver(mBluetoothReceiver);
            mBluetoothReceiver = null;
            mContext = null;
        }
        mBluetoothAdapter = null;
        mFoundDeviceList = null;
        mBoundedDeviceList = null;
        mBluetoothEventHandler = null;
        mServerCommuicateBluetoothSocket = null;
        mClientCommunicateBluetoothSocketMap = null;
        mBluetoothServerSocket = null;
    }
}
