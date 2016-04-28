package com.vinaya.vinayable.managers;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;

import com.vinaya.vinayable.Models.MyBluetoothDevice;

import java.util.List;
import java.util.UUID;

/**
 * Bluetooth manager when the current device use lower than Lollipop
 */
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


    /**
     * Scan callback.
     */
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    List<UUID> listUuids = MyBluetoothManagerCommon.parseAdvertisementPacket(scanRecord);
                    boolean offerService = MyBluetoothManagerCommon.checkOfferSerivce(uuidService,listUuids);
                    MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(device,device.getAddress(), rssi, offerService);
                    onMyBluetoothCallback.onNewDeviceListener(myBluetoothDevice);
                }
            };

    /**
     * Start or stop the scan devices
     * @param enable start if is true, stop if is false
     */
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

    public static long getScanPeriod() {
        return SCAN_PERIOD;
    }

    /**
     * Interface to send the new devices found
     */
    public interface OnMyBluetoothManager {
        void onNewDeviceListener(MyBluetoothDevice myBluetoothDevice);
    }


}
