package com.vinaya.vinayable;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.vinaya.vinayable.fragments.DeviceFragment;
import com.vinaya.vinayable.fragments.LoadingFragment;
import com.vinaya.vinayable.fragments.ScanFragment;
import com.vinaya.vinayable.managers.BluetoothLeService;
import com.vinaya.vinayable.views.adapters.ScanAdapter;

public class MainActivity extends AppCompatActivity implements ScanAdapter.OnConnectionStart {
    private static final int REQUEST_ENABLE_BT = 0;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean coarsePermissionGranted = checkCoarsePermission();

        boolean bluetoothAdapterActivated = false;
        if (coarsePermissionGranted)
            bluetoothAdapterActivated = checkBluetoothAdapter();
        if (bluetoothAdapterActivated) {
            if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED && BluetoothLeService.mBluetoothGatt != null) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String mac = prefs.getString(BluetoothLeService.SHARED_LATEST_MAC, "");
                startDeviceFragment(mac);
            } else
                startScanFragment();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish();
                return;
            }
            //Bluetooth enabled.
            startScanFragment();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * It check if the bluetooth adapter is activated, if not, it will request it
     *
     * @return true if the bluetooth adapter was already activated
     */
    private boolean checkBluetoothAdapter() {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if ((mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
        return true;
    }

    /**
     * It check if the access coarse location was granted, if not, it will request it
     *
     * @return true if the access coarse location was already granted
     */
    private boolean checkCoarsePermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    boolean bluetoothAdapterActivated = checkBluetoothAdapter();
                    if (bluetoothAdapterActivated)
                        startScanFragment();
                } else {
                    // permission denied, boo!
                    finish();
                }
            }
        }
    }

    /**
     * Start ScanFragment
     */
    private void startScanFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        ScanFragment scanFragment = new ScanFragment();
        fragmentTransaction.add(R.id.activity_main_container, scanFragment, getString(R.string.fragment_scan));
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Start LoadingFragment
     */
    private void startLoadingFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        LoadingFragment loadingFragment = new LoadingFragment();
        fragmentTransaction.add(R.id.activity_main_container, loadingFragment, getString(R.string.fragment_loading));
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Start DeviceFragment
     *
     * @param mac device address of the current device connected
     */
    private void startDeviceFragment(String mac) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        DeviceFragment deviceFragment = new DeviceFragment();
        Bundle bundle = new Bundle();
        bundle.putString(DeviceFragment.ARGS_MAC, mac);
        deviceFragment.setArguments(bundle);
        fragmentTransaction.add(R.id.activity_main_container, deviceFragment, getString(R.string.fragment_device));
        fragmentTransaction.commitAllowingStateLoss();
    }


    @Override
    public void onConnectionStartListener() {
        startLoadingFragment();
    }


    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     * ACTION_DATA_AVAILABLE: received data from the device.
     * ACTION_WRITE: send new data to the device
     * ACTION_RSSI UPDATE: received new RSSI value.
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        public boolean mConnected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                startDeviceFragment(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                startScanFragment();
            } else if (BluetoothLeService.
                    ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                DeviceFragment deviceFragment = (DeviceFragment) fragmentManager.findFragmentByTag(getString(R.string.fragment_device));
                if (deviceFragment != null)
                    deviceFragment.onUpdateTextReceivedListener(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_WRITE.equals(action)) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                DeviceFragment deviceFragment = (DeviceFragment) fragmentManager.findFragmentByTag(getString(R.string.fragment_device));
                if (deviceFragment != null)
                    deviceFragment.onUpdateTextSentListener(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            } else if (BluetoothLeService.ACTION_RSSI_UPDATE.equals(action)) {
                FragmentManager fragmentManager = getSupportFragmentManager();
                DeviceFragment deviceFragment = (DeviceFragment) fragmentManager.findFragmentByTag(getString(R.string.fragment_device));
                if (deviceFragment != null)
                    deviceFragment.onUpdateRSSIReceivedListener(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    /**
     * Register Broadcast receiver
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        filter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        filter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        filter.addAction(BluetoothLeService.ACTION_RSSI_UPDATE);
        filter.addAction(BluetoothLeService.ACTION_WRITE);
        registerReceiver(mGattUpdateReceiver, filter);
    }


}
