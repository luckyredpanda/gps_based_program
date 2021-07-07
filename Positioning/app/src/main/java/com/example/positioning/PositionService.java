package com.example.positioning;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PositionService extends Service implements LocationListener {
    private static String TAG = PositionService.class.getCanonicalName();
    static private final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private boolean serviceRunning = false;
    private boolean firstCall = true;
    private Location startLocation;

    private LocationManager locationManager;
    private PositionServiceImpl impl;

    // minimum time between location updates[ms]
    static private final long minTime = 1000;

    // minimum distance between location updates[m]
    static private final float minDistance = 1.0f;

    private long initTime;
    private double longitude;
    private double latitude;
    private double distance;
    private double speed;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding Service");
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        serviceRunning = true;
        firstCall = true;
        setLocationManager();
        return impl;
    }

    private void setLocationManager(){
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d(TAG, "GPS is disabled");
            Intent intent = new Intent("OPEN_GPS");
            sendBroadcast(intent);
        }else{
            Log.d(TAG, "GPS is enabled");
            //gps is enabled.
            if(Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED){
                Log.d(TAG, "Request permission");
                Intent intent = new Intent("REQUEST_GPS_PERMISSION");
                sendBroadcast(intent);
            }else{
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);
                Log.d(TAG, "Request permission not necessary");
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Unbinding Service");
        serviceRunning = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating Service");
        impl = new PositionServiceImpl();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Calling Service");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onLocationChanged(Location location) {
        if(serviceRunning){
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Intent intent = new Intent("LOCATION_CHANGED");
            sendBroadcast(intent);
            if(firstCall){
                startLocation = location;
                initTime = location.getTime();
                firstCall = false;
            }
            distance = startLocation.distanceTo(location);
            Log.d(TAG, "timeStep" + (location.getTime() - initTime)/1000);
            speed = distance/((location.getTime() - initTime)/1000);
        }
        //Log.d(TAG, "longitude" + longitude);
        //fLog.d(TAG, "latitude" + latitude);
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    private class PositionServiceImpl extends IPositionService.Stub{
        @Override
        public double getLongitude() throws RemoteException {
            return longitude;
        }

        @Override
        public double getLatitude() throws RemoteException {
            return latitude;
        }

        @Override
        public double getDistance() throws RemoteException {
            return distance;
        }

        @Override
        public double getAverageSpeed() throws RemoteException {
            return speed;
        }
    }
}
