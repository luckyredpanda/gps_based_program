package com.example.positioning;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.icu.text.DecimalFormat;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity{
    final private static String TAG = ServiceConnection.class.getCanonicalName();
    final private static int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;

    private IPositionService positionServiceProxy = null;

    private IntentFilter intentFilter;
    private ConnectionReceiver connectionReceiver;

    private CheckBox autoUpdateCb;
    private Button startServiceBtn, stopServiceBtn, updateBtn;
    private TextView textViewLatitude, textViewLongitude, textViewDistance, textViewSpeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectionReceiver = new ConnectionReceiver();
        intentFilter = new IntentFilter("REQUEST_GPS_PERMISSION");
        intentFilter.addAction("OPEN_GPS");
        intentFilter.addAction("LOCATION_CHANGED");
        registerReceiver(connectionReceiver, intentFilter);

        autoUpdateCb = findViewById(R.id.cb_update_mode);
        startServiceBtn = findViewById(R.id.btn_start_service);
        stopServiceBtn = findViewById(R.id.btn_stop_service);
        updateBtn = findViewById(R.id.btn_update);
        textViewLatitude = findViewById(R.id.tv_latitude);
        textViewLongitude = findViewById(R.id.tv_longitude);
        textViewDistance = findViewById(R.id.tv_distance);
        textViewSpeed = findViewById(R.id.tv_speed);

        autoUpdateCb.setChecked(false);
        autoUpdateCb.setEnabled(false);
        updateBtn.setEnabled(false);
        stopServiceBtn.setEnabled(false);

        autoUpdateCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(positionServiceProxy == null){
                    Toast.makeText(getApplicationContext(), "PositionService is not running", Toast.LENGTH_SHORT).show();
                }else {
                    if(b){
                        updateBtn.setEnabled(false);
                    }else{
                        updateBtn.setEnabled(true);
                    }
                }
            }
        });

        startServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), PositionService.class);
                bindService(intent,connection, BIND_AUTO_CREATE);
            }
        });

        stopServiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(positionServiceProxy!= null){
                    unbindService(connection);
                }
                autoUpdateCb.setEnabled(false);
                autoUpdateCb.setChecked(false);
                startServiceBtn.setEnabled(true);
            }
        });

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(positionServiceProxy == null){
                    Toast.makeText(getApplicationContext(), "positionServiceProxy is null", Toast.LENGTH_SHORT).show();
                }else{
                    try {
                        DecimalFormat fmt = new DecimalFormat("0.0000000");
                        textViewLongitude.setText("Longitude:" + fmt.format(positionServiceProxy.getLongitude()));
                        textViewLatitude.setText("Latitude:" + fmt.format(positionServiceProxy.getLatitude()));
                        textViewDistance.setText("Distance:" + fmt.format(positionServiceProxy.getDistance()));
                        textViewSpeed.setText("Speed:" + fmt.format(positionServiceProxy.getAverageSpeed()) + "m/s");
                    }
                    catch (Exception e){

                    }
                }
            }
        });
    }

    private ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connect Service");
            positionServiceProxy = IPositionService.Stub.asInterface(service);
            autoUpdateCb.setEnabled(true);
            stopServiceBtn.setEnabled(true);
            updateBtn.setEnabled(true);
            startServiceBtn.setEnabled(false);
        }

        public void onServiceDisconnected(ComponentName className){
            Log.d(TAG, "Disconnect Service");
            positionServiceProxy = null;
            autoUpdateCb.setEnabled(false);
            stopServiceBtn.setEnabled(false);
            updateBtn.setEnabled(false);
        }
    };

    public class ConnectionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Receive Intent");
            if (intent.getAction().equals("REQUEST_GPS_PERMISSION")) {
                Log.d(TAG, "CALL FUNCTION REQUEST_LOCATION_PERMISSION");
                requestLocationPermission();
            }else if(intent.getAction().equals("OPEN_GPS")){
                enableLocationSettings();
            }else if(intent.getAction().equals("LOCATION_CHANGED")){
//                Log.d(TAG, "LOCATION_CHANGED");
                if(autoUpdateCb.isChecked()){
                    try{
                        DecimalFormat fmt = new DecimalFormat("0.0000000");
                        textViewLongitude.setText("Longitude:"  + fmt.format(positionServiceProxy.getLongitude()));
                        textViewLatitude.setText("Latitude:"  + fmt.format(positionServiceProxy.getLatitude()));
                        textViewDistance.setText("Distance" + fmt.format(positionServiceProxy.getDistance()));
                        textViewSpeed.setText("Speed:" + fmt.format(positionServiceProxy.getAverageSpeed()) + "m/s");
                    }catch (Exception e){

                    }
                }
            }
        }
    }

    private void requestLocationPermission(){
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    private void enableLocationSettings(){
        new AlertDialog.Builder(this)
                .setTitle("Enable GPS")
                .setMessage("GPS currently disabled. Do you want to enable GPS")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(settingsIntent);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}