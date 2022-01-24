package com.newpos.mposlib.api;


import android.bluetooth.BluetoothDevice;

public interface IBluetoothDevListener {

    /**
     * 连接蓝牙设备成功与否
     * @param isConnected true:连接设备成功 false:连接设备失败
     */
    void onConnectedDevice(boolean isConnected);

    /**
     * 蓝牙设备断开连接
     */
    void onDisconnectedDevice();

    /**
     * 搜索蓝牙设备完成，超时
     */
    void onSearchComplete();

    /**
     * 搜索到一个蓝牙设备
     * @param bluetoothDevInfo
     */
    void onSearchOneDevice(BluetoothDevice bluetoothDevInfo);
}
