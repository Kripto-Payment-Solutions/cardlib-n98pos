package com.kriptops.wizarpos.demoapp;

import android.Manifest;
//import android.app.ActionBar;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
//import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;

import com.kriptops.wizarpos.demoapp.view.Fruit;
import com.kriptops.wizarpos.demoapp.view.FruitAdapter;
import com.newpos.mposlib.sdk.CardInfoEntity;
import com.newpos.mposlib.sdk.DeviceInfoEntity;
import com.newpos.mposlib.sdk.INpSwipeListener;
import com.newpos.mposlib.sdk.NpPosManager;

import java.util.ArrayList;


public class BluetoothActivity extends BaseActivity implements AdapterView.OnItemClickListener, INpSwipeListener {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 100;
    private static String testName;
    public ArrayList<Fruit> mDeviceList;
    FruitAdapter arrayAdapter;
    private ListView lvDeviceList;

    private NpPosManager posManager;
    public static String conDevice;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.hide();
        }
        mDeviceList = new ArrayList<>();
        init();
        posManager = NpPosManager.sharedInstance(getApplicationContext(), this);
        if (testName == null) {
            Log.e("GAT", "textName is null");
        } else {
            Log.e("GAT", "textName is " + testName);
        }
        arrayAdapter = new FruitAdapter(BluetoothActivity.this, R.layout.fruit_item, mDeviceList);
        lvDeviceList.setAdapter(arrayAdapter);
    }
     /*

       校验蓝牙权限
        Verificar permisos de bluetooth
      */

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int checkAccessFinePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (checkAccessFinePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                return;
            }

        }
        //做下面该做的事
    }

    /**
     * 对返回的值进行处理，相当于StartActivityForResult
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // 权限拒绝，提示用户开启权限
                denyPermission();
            }
        }
    }

    private void denyPermission() {
        Toast.makeText(BluetoothActivity.this, getApplicationContext().getText(R.string.blue_no_priority), Toast.LENGTH_SHORT).show();
    }

    /***
     * 其他活动跳转到本活动调用
     * @param context
     * @param str
     */
    public static void actionStart(Context context, String str) {
        Intent intent = new Intent(context, BluetoothActivity.class);
        testName = str;
        context.startActivity(intent);
    }

    /***
     * 初始化控件
     */
    private void init() {
        lvDeviceList = (ListView) findViewById(R.id.lv_device_list);
        lvDeviceList.setOnItemClickListener(BluetoothActivity.this);
    }

    /***
     * 点击蓝牙view跳转到这里
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        final String deviceInfo = mDeviceList.get(position).getMdevice();
        Builder builder = new Builder(BluetoothActivity.this);
        builder.setTitle(getApplicationContext().getText(R.string.tips));
        builder.setTitle(getApplicationContext().getText(R.string.connect_bluetooth)).setMessage(deviceInfo)
                .setPositiveButton(getApplicationContext().getText(R.string.connect), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String address = deviceInfo;
                                Log.d("Bluetooth", address);
                                BluetoothDevice bluetoothDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceInfo);
                                conDevice = bluetoothDevice.getAddress();
                                posManager.connectBluetoothDevice(bluetoothDevice.getAddress());
                            }
                        }).start();
                    }
                }).setNegativeButton(getApplicationContext().getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }

    /**
     * 自动调用蓝牙扫描
     */
    @Override
    protected void onResume() {
        super.onResume();
        arrayAdapter.clear();
        arrayAdapter.notifyDataSetChanged();
        new Thread(new Runnable() {
            @Override
            public void run() {
                posManager.scanBlueDevice(10000);
            }
        }).start();
    }

    /**
     * 销毁或退出本活动调用
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onScannerResult(final BluetoothDevice bluetoothDevice) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                String deviceName = bluetoothDevice.getName();
                String deviceAddress = bluetoothDevice.getAddress();
                Fruit fruit = new Fruit();
                fruit.setName(deviceName);
                fruit.setMdevice(deviceAddress);
                if (bluetoothDevice == null) {
                    Log.d("Bluetooth", "mDeviceList is null");
                    return;
                }
                mDeviceList.add(fruit);
                arrayAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDeviceConnected() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothActivity.this, getApplicationContext().getText(R.string.device_connect_success), Toast.LENGTH_SHORT).show();
                BluetoothActivity.this.finish();
            }
        });

    }

    @Override
    public void onDeviceDisConnected() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BluetoothActivity.this, getApplicationContext().getText(R.string.device_disconnect), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onGetDeviceInfo(DeviceInfoEntity deviceInfoEntity) {

    }

    @Override
    public void onGetTransportSessionKey(String s) {

    }

    @Override
    public void onUpdateMasterKeySuccess() {

    }

    @Override
    public void onUpdateWorkingKeySuccess() {

    }

    @Override
    public void onAddAidSuccess() {

    }

    @Override
    public void onAddRidSuccess() {

    }

    @Override
    public void onClearAids() {

    }

    @Override
    public void onClearRids() {

    }

    @Override
    public void onGetCardNumber(String s) {

    }

    @Override
    public void onGetDeviceBattery(boolean b) {

    }

    @Override
    public void onDetachedIC() {

    }

    @Override
    public void onGetReadCardInfo(CardInfoEntity cardInfoEntity) {

    }

    @Override
    public void onGetReadInputInfo(String s) {

    }

    @Override
    public void onGetICCardWriteback(boolean b) {

    }

    @Override
    public void onCancelReadCard() {

    }

    @Override
    public void onGetCalcMacResult(String s) {

    }

    @Override
    public void onUpdateFirmwareProcess(float v) {

    }

    @Override
    public void onUpdateFirmwareSuccess() {

    }

    @Override
    public void onGenerateQRCodeSuccess() {

    }

    @Override
    public void onSetTransactionInfoSuccess() {

    }

    @Override
    public void onGetTransactionInfoSuccess(String s) {

    }

    @Override
    public void onDisplayTextOnScreenSuccess() {

    }

    @Override
    public void onReceiveErrorCode(int i, String s) {

    }
}
