package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    TextView deviceNameTextView;
    CheckBox enableCheckBox, visibleCheckBox;
    ListView listView;
    ArrayList<String> bluetoothList = new ArrayList<>();
    Boolean searching = false; //Using this to deactivate search_bluetooth button (on menu) while it is still searching
    ArrayAdapter<String> arrayAdapter;
    BluetoothAdapter bluetoothAdapter;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                Log.i("Output", action);
                if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    if (state == BluetoothAdapter.STATE_ON) {
                        enableCheckBox.setChecked(true);
                        Toast.makeText(getApplicationContext(), "Bluetooth is ON", Toast.LENGTH_SHORT).show();
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        enableCheckBox.setChecked(false);
                        clearListView();
                        Toast.makeText(getApplicationContext(), "Bluetooth is OFF", Toast.LENGTH_SHORT).show();
                    }
                } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                    searching = true;
                    Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                    searching = false;
                    Toast.makeText(getApplicationContext(), "Discovery finished", Toast.LENGTH_SHORT).show();
                } else if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    /**ACTION_FOUND Required: android.permission.ACCESS_COARSE_LOCATION*/
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String name = bluetoothDevice.getName();
                    if (name == null) {
                        name = bluetoothDevice.getAddress();
                    }
                    bluetoothList.add(name +
                            '\n' +
                            "RSSI: " + Integer.toString(intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)) + "dBm");

                    arrayAdapter.notifyDataSetChanged();

                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == 1) {
            clearListView();
            bluetoothAdapter.startDiscovery();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_bluetooth:
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "Bluetooth is not enabled.", Toast.LENGTH_SHORT).show();
                } else {
                    /**ACTION_FOUND Required: android.permission.ACCESS_COARSE_LOCATION*/
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    } else {
                        if (!searching) {
                            clearListView();
                            bluetoothAdapter.startDiscovery();
                        } else {
                            Toast.makeText(this, "Still searching...", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void clearListView() {
        bluetoothList.clear();
        bluetoothList.add("AVAILABLE DEVICES");
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        deviceNameTextView = findViewById(R.id.deviceNameTextView);
        enableCheckBox = findViewById(R.id.enableCheckBox);
        visibleCheckBox = findViewById(R.id.visibleCheckBox);
        listView = findViewById(R.id.listView);
        bluetoothList.add("AVAILABLE DEVICES");
        arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, bluetoothList);
        listView.setAdapter(arrayAdapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
        deviceNameTextView.setText("My device name: " + bluetoothAdapter.getName());

        if (bluetoothAdapter.isEnabled()) {
            enableCheckBox.setChecked(true);
        }
        enableCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    bluetoothAdapter.enable();
                    /**Other solution:*/
                    /*Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetooth,0);*/
                } else {
                    bluetoothAdapter.disable();
                    visibleCheckBox.setChecked(false);
                }
            }
        });
        visibleCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent setVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(setVisible, 1);
                } else {
                    bluetoothAdapter.cancelDiscovery();
                }
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothAdapter.disable();
        unregisterReceiver(broadcastReceiver);
    }
}
