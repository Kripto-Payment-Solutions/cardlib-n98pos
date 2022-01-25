/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.newpos.mposlib.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.newpos.mposlib.BuildConfig;
import com.newpos.mposlib.api.IBluetoothDevListener;
import com.newpos.mposlib.exception.SDKException;
import com.newpos.mposlib.impl.AckRequestMgr;
import com.newpos.mposlib.impl.Command;
import com.newpos.mposlib.model.Packet;
import com.newpos.mposlib.model.ResponseData;
import com.newpos.mposlib.util.LogUtil;
import com.newpos.mposlib.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressLint("NewApi")
public class BluetoothService {
    // Debugging
    private static final String TAG = "bts";

    private static final UUID UUID_OTHER_DEVICE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Member fields
    private final BluetoothAdapter mAdapter;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private ProcessMessageThread mProcessMessageThread;
    private volatile int mState;
    private boolean isAndroid = BluetoothState.DEVICE_ANDROID;
    private final static int DELAY = 400; //ms
    private final static int SLICE = 450;//256; //byte

    // Constructor. Prepares a new BluetoothChat session
    // context : The UI Activity Context
    // handler : A Handler to send messages back to the UI Activity
    private BluetoothService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = BluetoothState.STATE_NONE;
    }

    private static BluetoothService instance = null;

    public static BluetoothService I() {
        if(instance == null) {
            synchronized (BluetoothService.class) {
                if(instance == null) {
                    instance = new BluetoothService();
                }
            }
        }
        return instance;
    }
    
    // Set the current state of the chat connection
    // state : An integer defining the current connection state
    private synchronized void setBtState(int state) {
        LogUtil.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
        
        // Give the new state to the Handler so the UI Activity can update
        //mHandler.obtainMessage(BluetoothState.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    // Return the current connection state. 
    public synchronized int getBtState() {
        return mState;
    }


    // Start the ConnectThread to initiate a connection to a remote device
    // device : The BluetoothDevice to connect
    // secure : Socket Security type - Secure (true) , Insecure (false)
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == BluetoothState.STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel(false);
            mConnectedThread = null;
        }

        // Cancel any thread currently running a connection
        if (mProcessMessageThread != null) {
            mProcessMessageThread.cancel();
            mProcessMessageThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setBtState(BluetoothState.STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel(false);
            mConnectedThread = null;
        }

        // Cancel any thread currently running a connection
        if (mProcessMessageThread != null) {
            mProcessMessageThread.cancel();
            mProcessMessageThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        mProcessMessageThread = new ProcessMessageThread();
        mProcessMessageThread.start();
    }

    // Stop all threads
    public synchronized void stop(boolean needCallback) {

        if (recvBuffer != null) {
            recvBuffer.clear();
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel(needCallback);
            mConnectedThread.interrupt();
            mConnectedThread = null;
        }

        if (mProcessMessageThread != null) {
            mProcessMessageThread.cancel();
            mProcessMessageThread = null;
        }

        setBtState(BluetoothState.STATE_NONE);
    }

    // Write to the ConnectedThread in an unsynchronized manner
    // out : The bytes to write
    public void writeData(byte[] out) throws SDKException {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != BluetoothState.STATE_CONNECTED) {
                throw new SDKException(SDKException.ERR_CODE_COMMUNICATE_ERROR);
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    // Indicate that the connection attempt failed and notify the UI Activity
    private void connectionFailed() {
        setBtState(BluetoothState.STATE_NONE);
    }

    // Indicate that the connection was lost and notify the UI Activity
    private void connectionLost() {
        setBtState(BluetoothState.STATE_NONE);
    }

    // This thread runs while attempting to make an outgoing connection
    // with a device. It runs straight through
    // the connection either succeeds or fails
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private boolean isCancel;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            isCancel = false;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
            }  catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }
            mmSocket = tmp;
        }

        public void run() {
            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (Throwable e) {
                if (isCancel) {
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e0) {
                }

                if (isCancel || isConnected()) {
                    return;
                }

                try {
                    mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(UUID_OTHER_DEVICE);
                    mmSocket.connect();
                } catch (Throwable e1) {
                    if (isCancel || isConnected()) {
                        return;
                    }
                    // Close the socket
                    try {
                        if (mmSocket != null) {
                            mmSocket.close();
                        }
                    } catch (Throwable e2) {
                        if (LogUtil.DEBUG) {
                            e2.printStackTrace();
                        }
                    }

                    connectionFailed();
                    if (mIBluetoothDevListener != null) {
                        mIBluetoothDevListener.onConnectedDevice(false);
                    }
                    return;
                }
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                isCancel = true;
                if (mmSocket != null) {
                    mmSocket.getInputStream().close();
                }
            } catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }

            try {
                if (mmSocket != null) {
                    mmSocket.close();
                }
            }  catch (Throwable e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doAckResponse(Packet packet) {
        AckRequestMgr.RequestTask task = AckRequestMgr.I().getAndRemove(packet.packetID);
        if (task != null) {
            task.success(packet);
        }
    }

    class ProcessMessageThread extends Thread {

        /**
         * thread cancel flag
         */
        private volatile boolean isCancel = false;

        public ProcessMessageThread() {
            this.isCancel = false;
        }

        public void cancel() {
            isCancel = true;
        }

        @Override
        public void run() {
            while (!isCancel) {
                if (!recvBuffer.isEmpty()) {
                    byte[] data = recvBuffer.poll();
                    final Packet packet = Command.parsePacket(data);
                    if (packet != null) {
                        if (packet.isAck()) {
                            doAckResponse(packet);
                        }
                        processResponse(packet);
                     }
                }

                try {
                    if (recvBuffer.isEmpty()) {
                        Thread.sleep(60);
                    }
                } catch (InterruptedException e) {
                    if (LogUtil.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    // This thread runs during a connection with a remote device.
    // It handles all incoming and outgoing transmissions.
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private volatile boolean isCancel = false;
        private volatile boolean needCallback = true;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            needCallback = true;
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            if (getBtState() != BluetoothState.STATE_CONNECTED) {
                setBtState(BluetoothState.STATE_CONNECTED);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (mIBluetoothDevListener != null) {
                            mIBluetoothDevListener.onConnectedDevice(true);
                        }
                    }
                }).start();
            }

            // Keep listening to the InputStream while connected
            while (!isCancel) {
                try {
                    //int data = mmInStream.read();

                    byte[] respBytes = null;
                    byte[] headBytes = new byte[1];
                    byte[] LenBytes = new byte[2];
                    while (true) {
                        // read until return some data
                        headBytes = readFixedBytes(1);
                        if (Packet.PACKET_HEAD == headBytes[0]) {
                            LenBytes = readFixedBytes(2);
                            int length = Integer.valueOf(StringUtil.byte2HexStr(LenBytes)).intValue() + 2;
                            respBytes = readFixedBytes(length);
                            if (Packet.PACKET_TAIL == respBytes[respBytes.length - 2]) {
                                // get one data
                                break;
                            } else {
                                if (BuildConfig.DEBUG) {
                                    //LogUtil.e("recv error packet:" + StringUtil.byte2HexStr(respBytes));
                                }
                            }
                        } else {
                            //LogUtil.i("recv error packet:" + headBytes[0]);
                        }
                    }

                    if (respBytes != null) {
                        byte[] readData = new byte[(headBytes.length + LenBytes.length + respBytes.length)];
                        System.arraycopy(headBytes, 0, readData, 0, 1);
                        System.arraycopy(LenBytes, 0, readData, 1, 2);
                        System.arraycopy(respBytes, 0, readData, 3, respBytes.length);
                        if (LogUtil.DEBUG) {
                            LogUtil.i("<< " + StringUtil.byte2HexStr(readData));
                        }
                        recvBuffer.offer(readData);
                    }

                } catch (IOException e) {
                    connectionLost();

                    LogUtil.e( "Exception during read\n" + e);

                    if (recvBuffer != null) {
                        recvBuffer.clear();
                    }
                    AckRequestMgr.I().clear();
                    Command.I().clear();

                    if (needCallback) {
                        if (mIBluetoothDevListener != null) {
                            mIBluetoothDevListener.onDisconnectedDevice();
                        }
                    }
                    break;
                } catch (Throwable e) {
                    if (LogUtil.DEBUG) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private byte[] readFixedBytes(int len) throws IOException {
            byte[] result = new byte[len];
            byte[] data = new byte[len];
            int lens = 0x0;
            int position = 0x0;
            while (len > 0) {
                lens = mmInStream.read(data);
                len -= lens;
                System.arraycopy(data, 0x0, result, position, lens);
                position += lens;
                data = new byte[len];
            }
            return result;
        }

        // Write to the connected OutStream.
        // @param buffer  The bytes to write
        public void write(byte[] data) {
//            final int perSise = SLICE;
//            if (data.length > perSise) {
//                int totalLen = data.length;
//                int cnt = totalLen / perSise;
//                int left = totalLen % perSise;
//                for (int i = 0; i < cnt; i++) {
//                    byte[] toSend = new byte[perSise];
//                    System.arraycopy(data, i * perSise, toSend, 0, perSise);
//                    try {
//                        mmOutStream.write(toSend);
//                    } catch (Throwable e) {
//                        LogUtil.e("Exception during write\n" + e);
//                        setState(BluetoothState.STATE_NONE);
//                    }
//
//                    try {
//                        Thread.sleep(DELAY);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                if (left != 0) {
//                    byte[] toSend = new byte[left];
//                    System.arraycopy(data, cnt * perSise, toSend, 0, left);
//                    try {
//                        mmOutStream.write(toSend);
//                    } catch (Throwable e) {
//                        LogUtil.e("Exception during write\n" + e);
//                        setState(BluetoothState.STATE_NONE);
//                    }
//                }
//            } else {
                try {
                    mmOutStream.write(data);
                } catch (Throwable e) {
                    LogUtil.e("Exception during write\n" + e);
                    setBtState(BluetoothState.STATE_NONE);
                }
          //  }

        }

        public synchronized void cancel(boolean needCallback) {
            isCancel = true;
            this.needCallback = needCallback;
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    /**
     * receive buffer queue
     */
    private final Queue<byte[]> recvBuffer = new ConcurrentLinkedQueue<>();

    private List<BluetoothDevice> deviceList;
    private IBluetoothDevListener mIBluetoothDevListener;
    private Context mContext;
    private Timer timer = new Timer();
    private TimerTask searchTimerTask;
    private BluetoothReceiver bluetoothReceiver;

    private void registerBluetoothReceiver(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        if((bluetoothReceiver == null) && (mContext != null)) {
            bluetoothReceiver = new BluetoothReceiver();
            mContext.registerReceiver(bluetoothReceiver, filter);
        }
    }

    private void unregisterBluetoothReceiver() {
        try {
            if((bluetoothReceiver != null) && (mContext != null)) {
                mContext.unregisterReceiver(bluetoothReceiver);
                bluetoothReceiver = null;
                return;
            }
        } catch(Exception ex) {
            if (LogUtil.DEBUG) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * search bluetooth device
     * @param timeOut ms
     */
    public void searchBluetoothDev(IBluetoothDevListener iBluetoothDevListener, Context context, int timeOut) {
        LogUtil.d("searchBluetoothDev");
        if (timeOut <= 0 || timeOut > StringUtil.MIN_MS) {
            timeOut = StringUtil.MIN_MS;
        }

        mIBluetoothDevListener = iBluetoothDevListener;

        // start timer to process timeout event
        if (searchTimerTask != null) {
            searchTimerTask.cancel();
        }

        searchTimerTask = new TimerTask() {
            @Override
            public void run() {
                stopSearch();
            }
        };
        timer.schedule(searchTimerTask, timeOut);

        if (!mAdapter.isEnabled()) {
            mAdapter.enable();
        }

        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
            try {
                Thread.sleep(600);
            } catch (InterruptedException e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        } else {
            deviceList.clear();
        }

        registerBluetoothReceiver(context);
        int tryCount = 3;
        while (!mAdapter.startDiscovery() && tryCount > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                if (LogUtil.DEBUG) {
                    e.printStackTrace();
                }
            }
            tryCount--;
        }

        if (tryCount > 0) {
            registerBluetoothReceiver(context);
        }
    }

    /**
     * bluetooth broadcast receiver
     */
    class BluetoothReceiver extends BroadcastReceiver {

        public BluetoothReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.d("action:" + action);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice bDevice = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bDevice != null && bDevice.getName() != null) {
                    if (deviceList != null) {
                        boolean add = true;
                        for(BluetoothDevice dev : deviceList) {
                            if (dev.getAddress().equals(bDevice.getAddress())) {
                                add = false;
                                break;
                            }
                        }

                        if (add) {
                            deviceList.add(bDevice);
                            if (mIBluetoothDevListener != null) {
                                mIBluetoothDevListener.onSearchOneDevice(bDevice);
                            }
                        }
                    }
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                unregisterBluetoothReceiver();
            }
        }

    }

    public synchronized void stopSearch() {
        LogUtil.i("stopSearch");
        try {
            if (mAdapter.isDiscovering()) {
                mAdapter.cancelDiscovery();
            }
        } catch (Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }

        if (deviceList != null) {
            deviceList.clear();
        }

        unregisterBluetoothReceiver();

    }

    public void setCallback(IBluetoothDevListener listener) {
        mIBluetoothDevListener = listener;
    }
    /**
     * connect remoute bluetooth device with mac address, block until return
     * @param macAddress remote device address
     * @return true:connected false:connect fail
     */
    public synchronized void connectDevice(String macAddress) {
        LogUtil.d("connectDevice MAC:" + macAddress + " mState:" + mState);
        if (searchTimerTask != null) {
            searchTimerTask.cancel();
        }

        if (TextUtils.isEmpty(macAddress)) {
            if (mIBluetoothDevListener != null) {
                mIBluetoothDevListener.onConnectedDevice(false);
            }
            return;
        }

        if (isConnected()) {
            if (mIBluetoothDevListener != null) {
                mIBluetoothDevListener.onConnectedDevice(true);
            }
            return;
        } else if (mState == BluetoothState.STATE_CONNECTING) {
            LogUtil.d("STATE_CONNECTING");
            return;
        }

        try {
            BluetoothDevice bluetoothDevice = mAdapter.getRemoteDevice(macAddress);
            connect(bluetoothDevice);
        } catch(Throwable e) {
            if (LogUtil.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return (getBtState() == BluetoothState.STATE_CONNECTED);
    }

    private void processResponse(Packet packet) {
        int packetID = packet.getPacketID();
        if (packetID == 1) {
            packetID = 255;
        } else {
            packetID = packetID -1;
        }

        Command.RequestTask task = Command.I().getAndRemove(packet.getStrCommand());
        if (task != null) {
            ResponseData responseData = new ResponseData(packet);
            task.setResponse(responseData);
        } else {

        }
    }
}