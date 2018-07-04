package com.vjs.ypn.vjs;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


public class InTrackingActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,LocationListener {

    private Button btn_stop_tracking;
    private Context mContext;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private TextView tv_quangduong,tv_time_tracked;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_tracking);
        mContext = this;
        tv_quangduong = findViewById(R.id.tv_quangduong);

        btn_stop_tracking = findViewById(R.id.btn_stop_tracking);
        tv_time_tracked = findViewById(R.id.tv_time_tracked);



        if(!isServiceRunning(AppService.class)){

            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(10 * 1000)// 10 seconds, in milliseconds
                    .setFastestInterval(10 * 1000); // 1 second, in milliseconds
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {

                return;
            }

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mGoogleApiClient.connect();

            new CountDownTimer(6*1000, 1000) {
                public void onTick(long millisUntilFinished) {
                    btn_stop_tracking.setText(""+ millisUntilFinished / 1000);
                }

                public void onFinish() {
                    //Start service
                    SharedPreferences.Editor editor = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE).edit();
                    Intent intent = new Intent(mContext,AppService.class);
                    editor.putString("CHECKPOINTS",getIntent().getStringExtra("CHECKPOINTS"));
                    editor.putString("OBJECT_NAME",getIntent().getStringExtra("BIENSO"));
                    editor.putInt("ID_OBJECT_TRACKING",getIntent().getIntExtra("ID_OBJECT_TRACKING",0));
                    editor.putString("ID_MODE_TRACKING",getIntent().getStringExtra("ID_MODE_TRACKING"));
                    editor.putLong("time_start",SystemClock.uptimeMillis());
                    editor.commit();
                    startService(intent);

                    startTime = SystemClock.uptimeMillis();
                    customHandler.postDelayed(updateTimerThread, 0);

                    btn_stop_tracking.setText("Dừng theo dõi!");
                    btn_stop_tracking.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(InTrackingActivity.this);
                            alertBuilder.setTitle("Kết thúc theo dõi");
                            alertBuilder.setMessage("Bạn có chắc chắn muốn kết thúc quá trình theo dõi?");
                            alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
                            alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //Dừng theo dõi
                                    stopTracking();
                                    //stopService(new Intent(mContext,AppService.class));
                                    startActivity(new Intent(InTrackingActivity.this,ShowResultActivity.class).putExtra("d",tv_quangduong.getText()));

                                    stopTimer = true;
                                    customHandler.removeCallbacks(updateTimerThread);
                                }
                            });

                            alertBuilder.setNegativeButton("No",null);

                            alertBuilder.show();

                        }
                    });
                }
            }.start();
        }else{

            SharedPreferences sharedPref = getSharedPreferences(Constants.SESSION_TRACKING,Context.MODE_PRIVATE);
            startTime = sharedPref.getLong("time_start",SystemClock.uptimeMillis());
            customHandler.postDelayed(updateTimerThread, 0);


            btn_stop_tracking.setText("Dừng theo dõi!");
            btn_stop_tracking.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(InTrackingActivity.this);
                    alertBuilder.setTitle("Kết thúc theo dõi");
                    alertBuilder.setMessage("Bạn có chắc chắn muốn kết thúc quá trình theo dõi?");
                    alertBuilder.setIcon(android.R.drawable.ic_dialog_alert);
                    alertBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            //Dừng theo dõi
                            stopTracking();
                            //stopService(new Intent(mContext,AppService.class));
                            Intent intent = new Intent(InTrackingActivity.this,ShowResultActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra("EXIT", true);
                            startActivity(new Intent(intent));
                            finish();
                        }
                    });

                    alertBuilder.setNegativeButton("No",null);

                    alertBuilder.show();

                }
            });
        }

    }

    private long startTime = 0L;

    private Handler customHandler = new Handler();

    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;
    boolean stopTimer = false;

    private Runnable updateTimerThread = new Runnable() {

        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            updatedTime = timeSwapBuff + timeInMilliseconds;

            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            String localtime = "" + mins + ":" + String.format("%02d", secs);
            tv_time_tracked.setText(localtime);
            if (!stopTimer)
                customHandler.postDelayed(this, 0);
        }

    };


    private void stopTracking() {
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    private boolean isServiceRunning(Class<?> serviceClass){
        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.e("Background_service","Chưa khai báo permission trong manifes");
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }
}
