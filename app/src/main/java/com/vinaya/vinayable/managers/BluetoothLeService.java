package com.vinaya.vinayable.managers;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.vinaya.vinayable.Models.MyBluetoothDevice;
import com.vinaya.vinayable.R;

import java.util.UUID;

// A service that interacts with the BLE device via the Android BLE API.
public class BluetoothLeService extends Service implements MyBluetoothManager.OnMyBluetoothManager, MyBluetoothManagerAPI21.OnMyBluetoothManagerAPI21 {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    public final static String ARGS_BLUETOOTH_DEVICE = "args_bluetooth_device";
    public final static String SHARED_LATEST_MAC = "shared_latest_mac";
    public final static String SHARED_ID_COMMUNICATION = "shared_id_communication";

    private Context context;

    //RSSI value boundary
    private static final int alertRssi = -70;
    //The RSSI alert was displayed. This value will be restarted when RSSI value be "strong enough"
    private boolean alertRssiDisplayed = false;
    //Value in milliseconds to request the RSSI value. This value should be higher in production environment to save battery
    private static final int readRemoteRssiPeriod = 10000;

    public static BluetoothGatt mBluetoothGatt;
    public static BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;

    //Connection states
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    //Current connection state
    public static int mConnectionState = STATE_DISCONNECTED;

    //Action to send through Broadcast
    public final static String ACTION_GATT_CONNECTED = "com.vinaya.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.vinaya.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.vinaya.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.vinaya.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_WRITE = "com.vinaya.bluetooth.le.ACTION_WRITE";
    public final static String ACTION_RSSI_UPDATE = "com.vinaya.bluetooth.le.ACTION_RSSI_UPDATE";
    public final static String EXTRA_DATA = "com.vinaya.bluetooth.le.EXTRA_DATA";

    //Service UUID
    public final static UUID SERVICE_UUID = UUID.fromString("abc00001-1234-5678-1234-abcd0123abcd");
    //Tx UUID characteristic
    public final static UUID TX_UUID = UUID.fromString("abc00002-1234-5678-1234-abcd0123abcd");

    //Rx UUID characteristic
    public final static UUID RX_UUID = UUID.fromString("abc00003-1234-5678-1234-abcd0123abcd");

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(final BluetoothGatt gatt, int status,
                                                    int newState) {
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        intentAction = ACTION_GATT_CONNECTED;
                        mConnectionState = STATE_CONNECTED;
                        //Notify through broadcast
                        broadcastUpdate(intentAction, gatt.getDevice().getAddress());
                        Log.i(TAG, "Connected to GATT server.");

                        //Discover available services
                        mBluetoothGatt.discoverServices();

                        //Request RSSI value periodically.
                        final Handler handler = new Handler(Looper.getMainLooper());
                        Runnable runnable = new Runnable() {
                            @Override
                            public void run() {
                                // do your stuff - don't create a new runnable here!
                                if (mBluetoothGatt != null) {
                                    mBluetoothGatt.readRemoteRssi();
                                }
                                handler.postDelayed(this, readRemoteRssiPeriod);
                            }
                        };
                        // start it with:
                        handler.post(runnable);

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        intentAction = ACTION_GATT_DISCONNECTED;
                        mConnectionState = STATE_DISCONNECTED;
                        Log.i(TAG, "Disconnected from GATT server.");
                        broadcastUpdate(intentAction);
                        //Notify disconnection to user
                        createToast(getString(R.string.toast_disconnected, gatt.getDevice().getAddress()));
                        //Stop service
                        stopSelf();
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    boolean txCharacteristicReceived = false;
                    boolean rxCharacteristicReceived = false;
                    boolean readySend = false;
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        for (BluetoothGattService bluetoothGattService : gatt.getServices()) {
                            for (BluetoothGattCharacteristic bluetoothGattCharacteristic : bluetoothGattService.getCharacteristics()) {
                                if (bluetoothGattCharacteristic.getUuid().equals(TX_UUID)) {
                                    txCharacteristic = bluetoothGattCharacteristic;
                                    txCharacteristicReceived = true;
                                } else if (bluetoothGattCharacteristic.getUuid().equals(RX_UUID)) {
                                    rxCharacteristic = bluetoothGattCharacteristic;
                                    //Subscribe to RX Characteristic
                                    rxCharacteristicReceived = mBluetoothGatt.setCharacteristicNotification(rxCharacteristic, true);
                                    //Read actual value
                                    mBluetoothGatt.readCharacteristic(rxCharacteristic);
                                }
                                //Tx and Rx characteristic was received. Send ready.
                                if (txCharacteristicReceived && rxCharacteristicReceived && !readySend)
                                    readySend = writeStringTXCharacteristic("Ready");
                            }
                        }
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }


                @Override
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    String characteristicValue = readCharacteristic(characteristic, status);
                    writeCharacteristicAnswer(characteristicValue);
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    String characteristicValue = readCharacteristic(characteristic);
                    writeCharacteristicAnswer(characteristicValue);
                }

                @Override
                public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        broadcastUpdate(ACTION_RSSI_UPDATE, Integer.toString(rssi));
                        if (rssi < alertRssi && !alertRssiDisplayed) {
                            createToast(context.getString(R.string.toast_rssiWeak));
                            alertRssiDisplayed = true;
                        } else if (rssi >= alertRssi)
                            alertRssiDisplayed = false;
                    }
                }

            };

    /**
     * @param characteristic
     * @param status
     * @return
     */
    private String readCharacteristic(BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS)
            return readCharacteristic(characteristic);
        return "";
    }

    /**
     * Read characteristic value received
     *
     * @param characteristic
     * @return string received
     */
    private String readCharacteristic(BluetoothGattCharacteristic characteristic) {
        String data = characteristic.getStringValue(0);
        broadcastUpdate(ACTION_DATA_AVAILABLE, data);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int idCommunication = prefs.getInt(SHARED_ID_COMMUNICATION + mBluetoothGatt.getDevice().getAddress(), 0);
        idCommunication++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SHARED_ID_COMMUNICATION + mBluetoothGatt.getDevice().getAddress(), idCommunication);
        editor.commit();
        return data;
    }

    /**
     * Create String answer and send it
     *
     * @param characteristicValue string to send
     * @return
     */
    private boolean writeCharacteristicAnswer(String characteristicValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int idCommunication = prefs.getInt(SHARED_ID_COMMUNICATION + mBluetoothGatt.getDevice().getAddress(), 0);
        characteristicValue = Integer.toHexString(idCommunication) + characteristicValue;

        idCommunication++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(SHARED_ID_COMMUNICATION + mBluetoothGatt.getDevice().getAddress(), idCommunication);
        editor.commit();

        return writeStringTXCharacteristic(characteristicValue);
    }


    /**
     * Add null terminator and send string
     *
     * @param string string to send
     * @return
     */
    private boolean writeStringTXCharacteristic(String string) {
        //check mBluetoothGatt is available
        if (mBluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }

        if (txCharacteristic == null) {
            Log.e(TAG, "char not found!");
            return false;
        }
        if (string != null)
            broadcastUpdate(ACTION_WRITE, string);
        string += "\0";
        txCharacteristic.setValue(string.getBytes());
        return mBluetoothGatt.writeCharacteristic(txCharacteristic);
    }

    /**
     * Notify through broadcast
     *
     * @param action current action
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
     * Notify through broadcast with extra data
     *
     * @param action current aciont
     * @param data   extra data
     */
    private void broadcastUpdate(String action, String data) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, data);
        sendBroadcast(intent);
    }

    /**
     * Create and display a Toast
     *
     * @param data string to display
     */
    private void createToast(final String data) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, data,
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = this;
        if (intent != null) {
            BluetoothDevice bluetoothDevice = intent.getParcelableExtra(ARGS_BLUETOOTH_DEVICE);
            mBluetoothGatt = bluetoothDevice.connectGatt(this, true, mGattCallback);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(SHARED_LATEST_MAC, bluetoothDevice.getAddress());
            editor.commit();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                MyBluetoothManagerAPI21 myBluetoothManagerAPI21 = new MyBluetoothManagerAPI21(this, this, BluetoothLeService.SERVICE_UUID.toString());
                myBluetoothManagerAPI21.scanLEDevice(true);
            } else {
                MyBluetoothManager myBluetoothManager = new MyBluetoothManager(this, this, BluetoothLeService.SERVICE_UUID.toString());
                myBluetoothManager.scanLeDevice(true);
            }
        }
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }


    @Override
    public void onNewDeviceListener(MyBluetoothDevice myBluetoothDevice) {
        restartDeviceConnection(myBluetoothDevice);
    }


    @Override
    public void onNewDeviceListenerAPI21(MyBluetoothDevice myBluetoothDevice) {
        restartDeviceConnection(myBluetoothDevice);
    }

    /**
     * Restart connection with the device
     *
     * @param myBluetoothDevice Bluetooth device
     */
    private void restartDeviceConnection(MyBluetoothDevice myBluetoothDevice) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String mac = prefs.getString(SHARED_LATEST_MAC, "");
        if (myBluetoothDevice.getAddress().equals(mac)) {
            mBluetoothGatt = myBluetoothDevice.getDevice().connectGatt(this, true, mGattCallback);
        }
    }
}

