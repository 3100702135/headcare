package com.example.administrator.headcare.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BluetoothUtil {
    private final static String TAG = "BluetoothUtil";
    private static ConcurrentHashMap<String, Boolean> mConnectMap =
            new ConcurrentHashMap<String, Boolean>();
    // 获取蓝牙的开关状态
    public static boolean getBlueToothStatus(Context context) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        boolean enabled = false;
        switch (bluetoothAdapter.getState()) {
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
                enabled = true;
                break;
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
            default:
                enabled = false;
                break;
        }
        return enabled;
    }

    // 打开或关闭蓝牙
    public static void setBlueToothStatus(Context context, boolean enabled) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (enabled == true) {
            bluetoothAdapter.enable();
        } else {
            bluetoothAdapter.disable();
        }
    }

    public static String readInputStream(InputStream inStream) {
        String result = "";
        try {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = inStream.read(buffer)) != -1) {
                outStream.write(buffer, 0, len);
            }
            byte[] data = outStream.toByteArray();
            outStream.close();
            inStream.close();
            result = new String(data, "utf8");
        } catch (Exception e) {
            e.printStackTrace();
            result = e.getMessage();
        }
        return result;
    }

    public static void writeOutputStream(BluetoothSocket socket, String message) {
        Log.d(TAG, "begin writeOutputStream message=" + message);
        try {
            OutputStream outStream = socket.getOutputStream();
            outStream.write(message.getBytes());
            //outStream.flush();
            //outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "end writeOutputStream");
    }

    @SuppressLint("NewApi")
    public static boolean isConnected(BluetoothSocket socket) {
            try {
                Field fld = BluetoothSocket.class.getDeclaredField("mClosed");
                fld.setAccessible(true);
                return fld.getBoolean(socket);
            } catch (Exception e) {
                Log.d(TAG, "error happened", e);
            }
        return false;
    }

    public static synchronized void notifyBeginConnection(String deviceAddress) {
        if(null == deviceAddress) {
            return;
        }
        mConnectMap.put(deviceAddress, true);
    }

    public static synchronized void clearConnectionFlags() {
        mConnectMap.clear();
    }

    public static synchronized boolean isConnectionBegined(String deviceAddress) {
        if(null == deviceAddress) {
            return false;
        }
        Boolean resultFlag = mConnectMap.get(deviceAddress);
        if(null == resultFlag) {
            return false;
        }
        return resultFlag;
    }

    public static boolean boundDeviceIfNeed(BluetoothDevice device) {
        // 未绑定
        try {
            // 调用配对绑定接口，到广播中返回绑定结果
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Method creMethod = BluetoothDevice.class
                        .getMethod("createBond");
                creMethod.invoke(device);
                Log.d(TAG, "createConnection()| createBound succ");
                return true;
            }
        } catch (Exception ex) {
            Log.d(TAG, "createConnection()| createBound failed", ex);
        }
        return false;
    }

    public static void makeBluetoothVisible(Context context, int visibleSecond) {
        if(null == context) {
            return;
        }
        if(visibleSecond < 0) {
            visibleSecond = 300;
        }
        Intent in=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        in.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, visibleSecond);
        context.startActivity(in);
    }

}
