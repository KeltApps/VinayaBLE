package com.vinaya.vinayable.fragments;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.LinearLayout;

import com.vinaya.vinayable.Models.MyBluetoothDevice;
import com.vinaya.vinayable.R;
import com.vinaya.vinayable.managers.BluetoothLeService;
import com.vinaya.vinayable.managers.MyBluetoothManager;
import com.vinaya.vinayable.managers.MyBluetoothManagerAPI21;
import com.vinaya.vinayable.views.adapters.ScanAdapter;

import java.util.ArrayList;
import java.util.List;

import static android.animation.ValueAnimator.ofInt;

/**
 * Fragment to scan devices
 */
public class ScanFragment extends Fragment implements MyBluetoothManager.OnMyBluetoothManager, MyBluetoothManagerAPI21.OnMyBluetoothManagerAPI21 {
    private List<MyBluetoothDevice> myBluetoothDevices;
    private ScanAdapter scanAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_scan, container, false);

        final FloatingActionButton floatingActionButton = (FloatingActionButton) rootView.findViewById(R.id.fragment_scan_floating_button);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan(floatingActionButton);
            }
        });
        startScan(floatingActionButton);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.fragment_scan_recyclerView);
        myBluetoothDevices = new ArrayList<>();
        scanAdapter = new ScanAdapter(getContext(), myBluetoothDevices, (ScanAdapter.OnConnectionStart) getActivity());
        recyclerView.setAdapter(scanAdapter);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayout.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return rootView;
    }

    /**
     * Start the scan of devices
     *
     * @param view View to animate (Floating button)
     */
    private void startScan(View view) {
        ValueAnimator valueAnimator;
        //if the current device use Lollipop or higher, use new features
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MyBluetoothManagerAPI21 myBluetoothManagerAPI21 = new MyBluetoothManagerAPI21(getContext(), this, BluetoothLeService.SERVICE_UUID.toString());
            myBluetoothManagerAPI21.scanLEDevice(true);
            valueAnimator = animationRotation(view, MyBluetoothManagerAPI21.getScanPeriod());
        } else {
            MyBluetoothManager myBluetoothManager = new MyBluetoothManager(getContext(), this, BluetoothLeService.SERVICE_UUID.toString());
            myBluetoothManager.scanLeDevice(true);
            valueAnimator = animationRotation(view, MyBluetoothManager.getScanPeriod());
        }
        //Start view animation
        valueAnimator.start();
    }

    /**
     * Rotate the view
     *
     * @param view       View to animate
     * @param scanPeriod animation duration
     * @return ValueAnimator object
     */
    private ValueAnimator animationRotation(final View view, long scanPeriod) {
        int secondsScanPeriod = (int) (scanPeriod / 1000);
        ValueAnimator valueAnimatorRotation = ofInt(0, 360 * secondsScanPeriod);
        valueAnimatorRotation.setDuration(scanPeriod);
        valueAnimatorRotation.setInterpolator(new LinearInterpolator());
        valueAnimatorRotation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setRotation((int) animation.getAnimatedValue() * 1.0f);
                view.requestLayout();
            }
        });
        return valueAnimatorRotation;
    }


    @Override
    public void onNewDeviceListener(MyBluetoothDevice myBluetoothDevice) {
        addDevice(myBluetoothDevice);
    }

    @Override
    public void onNewDeviceListenerAPI21(MyBluetoothDevice myBluetoothDevice) {
        addDevice(myBluetoothDevice);
    }

    /**
     * Add new device to the RecyclerView
     *
     * @param myBluetoothDevice device to add
     */
    private void addDevice(MyBluetoothDevice myBluetoothDevice) {
        for (MyBluetoothDevice device : myBluetoothDevices) {
            if (device.getAddress().equals(myBluetoothDevice.getAddress()))
                return;
        }
        myBluetoothDevices.add(myBluetoothDevice);
        if (scanAdapter != null)
            scanAdapter.notifyDataSetChanged();
    }

}
