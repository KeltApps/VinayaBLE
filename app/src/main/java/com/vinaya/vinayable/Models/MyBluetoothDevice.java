package com.vinaya.vinayable.Models;


import android.bluetooth.BluetoothDevice;

public class MyBluetoothDevice {
    private BluetoothDevice device;
    private String address;
    private int rssi;
    private boolean offerService;

    public MyBluetoothDevice(BluetoothDevice device, String address, int rssi, boolean offerService) {
        this.device = device;
        this.address = address;
        this.rssi = rssi;
        this.offerService = offerService;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public boolean isOfferService() {
        return offerService;
    }

    public void setOfferService(boolean offerService) {
        this.offerService = offerService;
    }
}
