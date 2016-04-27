package com.vinaya.vinayable.Models;


public class MyBluetoothDevice {
    private String address;
    private int rssi;
    private boolean offerService;

    public MyBluetoothDevice(String address, int rssi, boolean offerService) {
        this.address = address;
        this.rssi = rssi;
        this.offerService = offerService;
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
