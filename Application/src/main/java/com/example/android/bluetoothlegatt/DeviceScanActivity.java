/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    KalmanFilter kalmanFilter ;


    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        mScanning = false;
        mHandler = new Handler();
        kalmanFilter = new KalmanFilter();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                // Permission already Granted
                //Do your work here
                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                // BluetoothAdapter through BluetoothManager.

                mLeDeviceListAdapter = new LeDeviceListAdapter();
                setListAdapter(mLeDeviceListAdapter);
                scanLeDevice(true);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

        }}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();

                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                invalidateOptionsMenu();
                mHandler.removeCallbacks(stopScan);

                mHandler.removeCallbacks(startScan);

                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
      /*  if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }*/

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        else if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED) {
                // Permission already Granted
                //Do your work here
                // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
                // BluetoothAdapter through BluetoothManager.

                mLeDeviceListAdapter = new LeDeviceListAdapter();
                setListAdapter(mLeDeviceListAdapter);
                scanLeDevice(true);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mHandler.removeCallbacks(startScan);
        mHandler.removeCallbacks(stopScan);

        invalidateOptionsMenu();
        //   mLeDeviceListAdapter.clear();
    }
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {

            startScan.run();
            Log.e("BLE_Scanner", "DiscoverBLE");}
        else {

            stopScan.run();        }
    }
    private Runnable startScan = new Runnable() {


        @Override
        public void run() {

            mScanning = true;
            mLeDeviceListAdapter.clear();

            mLeDeviceListAdapter.notifyDataSetChanged();


            mHandler.postDelayed(stopScan, 10000);

            // mBluetoothAdapter.startLeScan(new UUID[]{ParcelUuid.fromString("0000b81d-0000-1000-8000-00805f9b34fb").getUuid()
            //},mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            invalidateOptionsMenu();


        }
    };
    private Runnable stopScan = new Runnable() {
        @Override
        public void run() {
            mScanning = false;



            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mHandler.postDelayed(startScan, 1000);

            invalidateOptionsMenu();


        }
    };

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<Integer> mLeRssi;
        private ArrayList<Integer> mLeTx;


        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mLeRssi = new ArrayList<Integer>();
            mLeTx = new ArrayList<Integer>();


            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device,int rssi,int txPower) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                mLeRssi.add(rssi);
                mLeTx.add(txPower);

            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
            mLeRssi.clear();
            mLeTx.clear();
        }


        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.deviceRssi= (TextView) view.findViewById(R.id.device_rssi);
                viewHolder.deviceTx= (TextView) view.findViewById(R.id.device_tx);
                viewHolder.deviceDistance= (TextView) view.findViewById(R.id.device_distance);



                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());
            viewHolder.deviceRssi.setText(String.valueOf(mLeRssi.get(i)));
            viewHolder.deviceTx.setText(String.valueOf(mLeTx.get(i)));
            double accuracy = calculateDistance(mLeTx.get(i) ,mLeRssi.get(i));

            viewHolder.deviceDistance.setText(String.valueOf(accuracy)+" m ("+getDistance(accuracy)+")");



            return view;
        }
        private String getDistance(double accuracy) {
            if (accuracy == -1.0) {
                return "Unknown";
            } else if (accuracy < 5 ) {
                return "Immediate";
            } else if (accuracy < 10) {
                return "Near";
            } else {
                return "Far";
            }
        }
        public double calculateDistance(int txPower, double rssi) {
            if (rssi == 0) {

                return -1.0; // if we cannot determine accuracy, return -1.

            }
            double ratio = rssi*1.0/txPower;
            if (ratio < 1.0) {

                return Math.pow(ratio,10);

            }
            else {
                double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
                return accuracy;
            }
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //KalmanFilter
                            int rssiFiltred = (int)kalmanFilter.applyFilter(rssi);

                            if (scanRecord[7] == 0x02 && scanRecord[8] == 0x15) { // iBeacon indicator
                                System.out.println("iBeacon Packet:  "+ bytesToHexString(scanRecord));

                                byte txpw = scanRecord[29];
                                System.out.println( " TxPw " + (int)txpw );
                                mLeDeviceListAdapter.addDevice(device,rssiFiltred,txpw);
                                mLeDeviceListAdapter.notifyDataSetChanged();


                            }
                            else {

                                mLeDeviceListAdapter.addDevice(device,rssiFiltred,-59);
                                mLeDeviceListAdapter.notifyDataSetChanged();}
                        }
                    });
                }
            };

    public static String bytesToHexString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for(int i=0; i<bytes.length; i++) {
            buffer.append(String.format("%02x", bytes[i]));
        }
        return buffer.toString();
    }

    public double calculateDistance(int txPower, double rssi) {
        if (rssi == 0) {

            return -1.0; // if we cannot determine accuracy, return -1.

        }
        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {

            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }



    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRssi,deviceDistance,deviceTx;

    }
}