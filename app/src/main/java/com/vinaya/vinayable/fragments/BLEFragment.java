package com.vinaya.vinayable.fragments;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.vinaya.vinayable.Models.MyBluetoothDevice;
import com.vinaya.vinayable.R;
import com.vinaya.vinayable.managers.MyBluetoothManager;
import com.vinaya.vinayable.managers.MyBluetoothManagerAPI21;
import com.vinaya.vinayable.views.adapters.BLEAdapter;

import java.util.ArrayList;
import java.util.List;


public class BLEFragment extends Fragment implements MyBluetoothManager.OnMyBluetoothManager, MyBluetoothManagerAPI21.OnMyBluetoothManagerAPI21 {
    private static final String SERVICE_UUID = "abc00001-1234-5678-1234-abcd0123abcd";
    private List<MyBluetoothDevice> myBluetoothDevices;
    private BLEAdapter bleAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_ble, container, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MyBluetoothManagerAPI21 myBluetoothManagerAPI21 = new MyBluetoothManagerAPI21(getContext(), this, SERVICE_UUID);
            myBluetoothManagerAPI21.scanLEDevice(true);
        } else {
            MyBluetoothManager myBluetoothManager = new MyBluetoothManager(getContext(), this, SERVICE_UUID);
            myBluetoothManager.scanLeDevice(true);
        }

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_ble_recyclerView);
        myBluetoothDevices = new ArrayList<>();
        bleAdapter = new BLEAdapter(getContext(),myBluetoothDevices);
        recyclerView.setAdapter(bleAdapter);

        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayout.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }


    @Override
    public void onNewDeviceListener(MyBluetoothDevice myBluetoothDevice) {
        addDevice(myBluetoothDevice);
    }

    @Override
    public void onNewDeviceListenerAPI21(MyBluetoothDevice myBluetoothDevice) {
        addDevice(myBluetoothDevice);
    }

    private void addDevice(MyBluetoothDevice myBluetoothDevice) {
        myBluetoothDevices.add(myBluetoothDevice);
        if (bleAdapter != null)
            bleAdapter.notifyDataSetChanged();
    }
}
