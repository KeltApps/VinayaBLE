package com.vinaya.vinayable.views.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vinaya.vinayable.Models.MyBluetoothDevice;
import com.vinaya.vinayable.R;

import java.util.List;


public class BLEAdapter extends RecyclerView.Adapter<BLEAdapter.ViewHolderBluetooth> {
    private List<MyBluetoothDevice> listDevice;
    private Context context;

    public BLEAdapter(Context context, List<MyBluetoothDevice> list) {
        this.context = context;
        listDevice = list;
    }

    @Override
    public ViewHolderBluetooth onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolderBluetooth(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ble, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolderBluetooth holder, int position) {
        holder.bindProfile(context, listDevice.get(position));
    }

    @Override
    public int getItemCount() {
        return listDevice.size();
    }

    public static class ViewHolderBluetooth extends RecyclerView.ViewHolder {
        View container;
        TextView textViewAddress;
        TextView textViewRSSI;

        public ViewHolderBluetooth(View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.item_ble_container);
            textViewAddress = (TextView) itemView.findViewById(R.id.item_ble_address);
            textViewRSSI = (TextView) itemView.findViewById(R.id.item_ble_rssi);
        }

        public void bindProfile(Context context, MyBluetoothDevice myBluetoothDevice) {
            if(myBluetoothDevice.isOfferService())
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorAbled));
                else
                container.setBackgroundColor(ContextCompat.getColor(context, R.color.colorDisabled));
            textViewAddress.setText(myBluetoothDevice.getAddress());
            textViewRSSI.setText(context.getString(R.string.bleFragment_dbm, Integer.toString(myBluetoothDevice.getRssi())));
        }
    }
}
