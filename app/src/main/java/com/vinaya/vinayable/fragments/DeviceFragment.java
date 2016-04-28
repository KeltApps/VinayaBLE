package com.vinaya.vinayable.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.vinaya.vinayable.R;
import com.vinaya.vinayable.managers.BluetoothLeService;
import com.vinaya.vinayable.utils.FileOperations;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to interact with the connected device
 */
public class DeviceFragment extends Fragment {
    public final static String ARGS_MAC = "args_mac";
    private TextView textViewRSSI;
    private TextView textViewAddress;
    private TextView rxZero;
    private TextView rxOne;
    private TextView rxTwo;
    private TextView txZero;
    private TextView txOne;
    private TextView txTwo;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_device, container, false);

        textViewAddress = (TextView) rootView.findViewById(R.id.fragment_device_address);
        textViewRSSI = (TextView) rootView.findViewById(R.id.fragment_device_rssi);
        Bundle bundle = getArguments();
        textViewAddress.setText(bundle.getString(ARGS_MAC));
        final FloatingActionButton floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.fragment_device_floating_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        //Initialize TextView for rx
        View rxView = rootView.findViewById(R.id.fragment_device_rx);
        TextView rxTitle = (TextView) rxView.findViewById(R.id.fragment_device_latest_communication_title);
        rxZero = (TextView) rxView.findViewById(R.id.fragment_device_latest_communication_0);
        rxOne = (TextView) rxView.findViewById(R.id.fragment_device_latest_communication_1);
        rxTwo = (TextView) rxView.findViewById(R.id.fragment_device_latest_communication_2);

        //Initialize TextView for tx
        View txView = rootView.findViewById(R.id.fragment_device_tx);
        TextView txTitle = (TextView) txView.findViewById(R.id.fragment_device_latest_communication_title);
        txZero = (TextView) txView.findViewById(R.id.fragment_device_latest_communication_0);
        txOne = (TextView) txView.findViewById(R.id.fragment_device_latest_communication_1);
        txTwo = (TextView) txView.findViewById(R.id.fragment_device_latest_communication_2);

        //Set TextView titles for rx/tx
        rxTitle.setText(getString(R.string.deviceFragment_rxTitle));
        txTitle.setText(getString(R.string.deviceFragment_txTitle));

        final EditText editText = (EditText) rootView.findViewById(R.id.fragment_device_editText);
        Button sendButton = (Button) rootView.findViewById(R.id.fragment_device_sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String data = editText.getText().toString();
                if(data.equals(""))
                    return;
                //Send text
                writeStringTXCharacteristic(data);
                //Update tx records
                FileOperations.saveLatestCommunication(getContext(), data, textViewAddress.getText().toString(), true);
                //Update TextViews
                readAndUpdateTextView(textViewAddress.getText().toString(), true, txZero, txOne, txTwo);
                editText.setText("");
            }
        });

        //Set old values into TextViews
        readAndUpdateTextView(textViewAddress.getText().toString(), true, txZero, txOne, txTwo);
        readAndUpdateTextView(textViewAddress.getText().toString(), false, rxZero, rxOne, rxTwo);

        return rootView;
    }

    /**
     * Close the current connection
     */
    public void disconnect() {
        if (BluetoothLeService.mBluetoothGatt == null) {
            return;
        }

        BluetoothLeService.mBluetoothGatt.disconnect();
        BluetoothLeService.mBluetoothGatt.close();
        BluetoothLeService.mBluetoothGatt = null;
        getContext().stopService(new Intent(getContext(), BluetoothLeService.class));

        String intentAction = BluetoothLeService.ACTION_GATT_DISCONNECTED;
        Intent intent = new Intent(intentAction);
        getContext().sendBroadcast(intent);
    }

    /**
     * Send text
     *
     * @param string Text to send
     * @return
     */
    private boolean writeStringTXCharacteristic(String string) {
        //check mBluetoothGatt is available
        if (BluetoothLeService.mBluetoothGatt == null) {
            return false;
        }

        if (BluetoothLeService.txCharacteristic == null) {
            return false;
        }
        BluetoothLeService.txCharacteristic.setValue(string.getBytes());

        //Update id communication
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        int idCommunication = prefs.getInt(BluetoothLeService.SHARED_ID_COMMUNICATION + BluetoothLeService.mBluetoothGatt.getDevice().getAddress(), 0);
        idCommunication++;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(BluetoothLeService.SHARED_ID_COMMUNICATION + BluetoothLeService.mBluetoothGatt.getDevice().getAddress(), idCommunication);
        editor.commit();

        //send text
        return BluetoothLeService.mBluetoothGatt.writeCharacteristic(BluetoothLeService.txCharacteristic);
    }

    /**
     * Update the textViews with the values in stringList
     *
     * @param stringList new values
     * @param textViews  TextViews to update
     */
    private void updateTextView(List<String> stringList, List<TextView> textViews) {
        int sizeStringList = stringList.size();
        int sizeTextViews = textViews.size();
        int size = sizeStringList <= sizeTextViews ? sizeStringList : sizeTextViews;
        for (int i = 0; i < size; i++) {
            textViews.get(i).setText(stringList.get(i));
        }
    }

    /**
     * Read the values saved in the records and update TextViews
     *
     * @param mac  address device
     * @param tx   tx if is true, rx if is false
     * @param zero first textView
     * @param one  second textView
     * @param two  third textView
     */
    private void readAndUpdateTextView(String mac, boolean tx, TextView zero, TextView one, TextView two) {
        List<String> stringList = FileOperations.readLatestCommunication(getContext(), mac, tx);
        List<TextView> textViews = new ArrayList<>();
        textViews.add(zero);
        textViews.add(one);
        textViews.add(two);
        updateTextView(stringList, textViews);
    }

    /**
     * Update RSSI value
     *
     * @param data new RSSI value
     */
    public void onUpdateRSSIReceivedListener(String data) {
        textViewRSSI.setText(getContext().getString(R.string.scanFragment_dbm, data));
    }

    /**
     * Update rx TextViews
     *
     * @param data new String received
     */
    public void onUpdateTextReceivedListener(String data) {
        FileOperations.saveLatestCommunication(getContext(), data, textViewAddress.getText().toString(), false);
        readAndUpdateTextView(textViewAddress.getText().toString(), false, rxZero, rxOne, rxTwo);
    }

    /**
     * Update tx TextViews
     *
     * @param data new String send
     */
    public void onUpdateTextSentListener(String data) {
        FileOperations.saveLatestCommunication(getContext(), data, textViewAddress.getText().toString(), true);
        readAndUpdateTextView(textViewAddress.getText().toString(), true, txZero, txOne, txTwo);
    }
}
