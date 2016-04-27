package com.vinaya.vinayable.managers;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import com.vinaya.vinayable.Models.MyBluetoothDevice;

import java.util.List;
import java.util.UUID;

public class MyBluetoothManager {

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private OnMyBluetoothManager onMyBluetoothCallback;
    private String uuidService;

    public MyBluetoothManager(Context context, OnMyBluetoothManager onMyBluetoothCallback, String uuidService) {
        this.onMyBluetoothCallback = onMyBluetoothCallback;
        this.uuidService = uuidService;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
    }


    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    List<UUID> listUuids = MyBluetoothManagerCommon.parseAdvertisementPacket(scanRecord);
                    boolean offerService = MyBluetoothManagerCommon.checkOfferSerivce(uuidService,listUuids);
                    MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(device.getAddress(), rssi, offerService);
                    onMyBluetoothCallback.onNewDeviceListener(myBluetoothDevice);
                }
            };

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    public boolean ismScanning() {
        return mScanning;
    }

    public interface OnMyBluetoothManager {
        void onNewDeviceListener(MyBluetoothDevice myBluetoothDevice);
    }


}
