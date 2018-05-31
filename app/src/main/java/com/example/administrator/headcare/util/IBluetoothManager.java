package com.example.administrator.headcare.util;

import android.bluetooth.BluetoothDevice;

import java.util.List;

public interface IBluetoothManager {

    public void setBluetoothEventHandler(IBluetoothEventHandler eventHandler);
    /**
     * 开启蓝牙设备
     * @return resultCode 结果错误码
     */
    public int enableBluetooth();

    public int searchAvailableDevice();

    public void connectBoundedDevice();
    public List<BluetoothDevice> getFoundedDevices();

    public List<BluetoothDevice> getBoundedDevices();

    public void createConnection(BluetoothDevice device);

    public void writeDataToServerConnection(byte[] data);

    public void writeDataToClientConnection(String deviceAddress, byte[] data);

    public void endConnection();

    public void disableBluetooth();

    public interface IBluetoothEventHandler {
        int RESULT_SUCCESS = 0;
        int RESULT_FAIL = 1;

        public boolean isDeviceMatch(BluetoothDevice device);

        public void onServerSocketConnectResult(int resultCode);

       public void onDeviceFound(BluetoothDevice device);

        public void onClientSocketConnectResult(BluetoothDevice device, int resultCode);

        public void onReadServerSocketData(BluetoothDevice remoteDevice, byte[] data, int length);

        public void onReadClientSocketData(BluetoothDevice remoteDevice, byte[] data, int length);

        public void onBluetoothOn();

        public void onBluetoothOff();
    }
}
