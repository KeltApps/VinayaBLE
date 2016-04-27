package com.vinaya.vinayable.managers;


import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.vinaya.vinayable.Models.MyBluetoothDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class MyBluetoothManagerAPI21 {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private OnMyBluetoothManagerAPI21 onMyBluetoothCallback;
    private String uuidService;

    public MyBluetoothManagerAPI21(Context context, OnMyBluetoothManagerAPI21 onMyBluetoothCallback, String uuidService) {
        this.onMyBluetoothCallback = onMyBluetoothCallback;
        this.uuidService = uuidService;
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<>();
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            addDevice(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results)
                addDevice(result);
        }

        private void addDevice(ScanResult result) {
            BluetoothDevice bluetoothDevice = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            if (scanRecord == null)
                return;
            List<UUID> listUuids = MyBluetoothManagerCommon.parseAdvertisementPacket(scanRecord);
            boolean offerService = MyBluetoothManagerCommon.checkOfferSerivce(uuidService, listUuids);
            MyBluetoothDevice myBluetoothDevice = new MyBluetoothDevice(bluetoothDevice.getAddress(), result.getRssi(), offerService);
            onMyBluetoothCallback.onNewDeviceListenerAPI21(myBluetoothDevice);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };


    public void scanLEDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);
            mLEScanner.startScan(filters, settings, mScanCallback);
        } else {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    public interface OnMyBluetoothManagerAPI21 {
        void onNewDeviceListenerAPI21(MyBluetoothDevice myBluetoothDevice);
    }


}
