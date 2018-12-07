package com.example.android.bluetoothlegatt;

/**
 * Created by faiza on 17/09/2018.
 */

public interface RssiFilter {
    double applyFilter(double rssi);
}