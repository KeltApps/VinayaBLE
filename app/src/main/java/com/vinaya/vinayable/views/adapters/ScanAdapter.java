package com.vinaya.vinayable.views.adapters;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vinaya.vinayable.Models.MyBluetoothDevice;
import com.vinaya.vinayable.R;
import com.vinaya.vinayable.managers.BluetoothLeService;

import java.util.List;

/**
 * Adapter of the RecyclerView
 */
public class ScanAdapter extends RecyclerView.Adapter<ScanAdapter.ViewHolderScan> {
    private List<MyBluetoothDevice> listDevice;
    private Context context;
    private OnConnectionStart onConnectionStart;

    public ScanAdapter(Context context, List<MyBluetoothDevice> list, OnConnectionStart onConnectionStart) {
        this.context = context;
        listDevice = list;
        this.onConnectionStart = onConnectionStart;
    }

    @Override
    public ViewHolderScan onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderScan(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_scan, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolderScan holder, int position) {
        holder.bindProfile(context, listDevice.get(position), onConnectionStart);
    }

    @Override
    public int getItemCount() {
        return listDevice.size();
    }

    /**
     * View holder for the adapter
     */
    public static class ViewHolderScan extends RecyclerView.ViewHolder {
        View container;
        TextView textViewAddress;
        TextView textViewRSSI;

        public ViewHolderScan(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.item_scan_container);
            textViewAddress = (TextView) itemView.findViewById(R.id.item_scan_address);
            textViewRSSI = (TextView) itemView.findViewById(R.id.item_scan_rssi);
        }

        public void bindProfile(final Context context, final MyBluetoothDevice myBluetoothDevice, final OnConnectionStart onConnectionStart) {
            if (myBluetoothDevice.isOfferService()) {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAbled));
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Initialize BluetoothLeService
                        Intent intent = new Intent(context, BluetoothLeService.class);
                        BluetoothDevice bluetoothDevice = myBluetoothDevice.getDevice();
                        intent.putExtra(BluetoothLeService.ARGS_BLUETOOTH_DEVICE, bluetoothDevice);
                        context.startService(intent);
                        //Notify activity
                        onConnectionStart.onConnectionStartListener();
                    }
                });
            } else {
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDisabled));
                container.setOnClickListener(null);
            }
            textViewAddress.setText(myBluetoothDevice.getAddress());
            textViewRSSI.setText(context.getString(R.string.scanFragment_dbm, Integer.toString(myBluetoothDevice.getRssi())));
        }

    }

    /**
     * Interface to notify activity that is going to start the connection with a device
     */
    public interface OnConnectionStart {
        void onConnectionStartListener();
    }
}
