package com.vjs.ypn.vjs;


import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ypn on 1/3/2018.
 */

public class UpdatePosition extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;
    private ArrayList<CheckPoint> aaa;
    private boolean inCheckpoint = false;
    private String object_name;
    private int object_id,mode_id;
    private Integer sessionID;
    private Integer currenIndexCheckPoint = -1;
    Date time_start;
    private PowerManager.WakeLock mWakelock;
    private String strCheckPoints;

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://113.160.215.214:3000");
        } catch (URISyntaxException e) {
            Log.e("khong co ket noi",e.getMessage().toString());
            this.stopSelf();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient.connect();
        aaa = new ArrayList<>();

        strCheckPoints = intent.getStringExtra("CHECKPOINTS");
        object_name = intent.getStringExtra("OBJECT_NAME");
        object_id = intent.getIntExtra("ID_OBJECT_TRACKING",0);
        mode_id = Integer.parseInt(intent.getStringExtra("ID_MODE_TRACKING"));

        try {
            JSONArray jsonArrayCheckpoints = new JSONArray(strCheckPoints);
            for(int i = 0; i< jsonArrayCheckpoints.length() ; i++ ){
                JSONObject checkpoint = jsonArrayCheckpoints.getJSONObject(i);
                JSONArray path = new JSONArray(checkpoint.getString("polygon"));
                Integer checkpointId = checkpoint.getInt("id");
                List<LatLng> polygon = new ArrayList<>();
                for(int j=0;j<path.length();j++){
                    JSONObject coordinate = path.getJSONObject(j);
                    polygon.add(new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng")));
                }

                CheckPoint checkPoint = new CheckPoint(checkpointId,i,polygon);

                aaa.add(checkPoint);

            }


        } catch (JSONException e) {
            Log.e("Loi",e.toString());
            e.printStackTrace();
        }

        return START_STICKY ;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Intent notificationIntent = new Intent(this, InTrackingActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RealtimeGPSTracker")
                .setContentText("Request location...")
                .setContentIntent(pendingIntent).build();

        startForeground(1992, notification);

        confirmCreateNewSession();
        mSocket.connect();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1 * 1000)// 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

    }

    @Override
    public void onDestroy() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }


        JSONObject jo = new JSONObject();
        try {
            jo.put("sessionId",sessionID);
            jo.put("mode_id",mode_id);
            jo.put("object_id",object_id);
            mSocket.emit("stop_traking",jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(mSocket.connected()){
            mSocket.disconnect();
            mSocket.off();
        }

        if (mWakelock != null) {
            mWakelock.release();
        }

        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location crrLoc) {

        if(!(crrLoc.getSpeed() >0 && sessionID!=null)){
            return;
        }

        Log.e("Background service", "Location change");
        JSONObject position = new JSONObject();
        try {
            position.put("lat", crrLoc.getLatitude());
            position.put("lng" ,crrLoc.getLongitude());
            position.put("sessionId",sessionID);
            mSocket.emit("update_location", position);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //Khởi tạo vị trí checkpoint hiện tại nếu null.

        if(currenIndexCheckPoint!=-1 && PolyUtil.containsLocation(crrLoc.getLatitude(),crrLoc.getLongitude(),aaa.get(currenIndexCheckPoint).getPolygon(),false)){
            Log.e("Background Service","Xe van o trong checkpoint "+ aaa.get(currenIndexCheckPoint).getId());

        }else{

            if(inCheckpoint){
                Log.e("Background service","Xe ra ngoai check point " + aaa.get(currenIndexCheckPoint).getId());

                Date time_end = new Date();

                long time_diff = time_end.getTime() - time_start.getTime();

                aaa.get(currenIndexCheckPoint).setTotal_time((int) (aaa.get(currenIndexCheckPoint).getTotal_time() + time_diff/1000));

                JSONObject jso = new JSONObject();
                try {
                    jso.put("sessionId",sessionID);
                    jso.put("checkpointId",aaa.get(currenIndexCheckPoint).getId());
                    jso.put("checkpointIndex",aaa.get(currenIndexCheckPoint).getIndex());
                    jso.put("mode_id",mode_id);
                    jso.put("object_id",object_id);
                    jso.put("total_time",aaa.get(currenIndexCheckPoint).getTotal_time());
                    mSocket.emit("session_step_out_checkpoint",jso);
                    inCheckpoint = false;
                    currenIndexCheckPoint = -1;
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            for(int i = 0; i< aaa.size() ; i++){

                List<LatLng> lln =  aaa.get(i).getPolygon();

                if(PolyUtil.containsLocation(crrLoc.getLatitude(),crrLoc.getLongitude(),lln,false)){
                    Log.e("Xe vao checkpoint","Xe vao checkpoint");
                    currenIndexCheckPoint = i;
                    inCheckpoint = true;

                    time_start = new Date();

                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("sessionId",sessionID);
                        jsonObject.put("mode_id",mode_id);
                        jsonObject.put("object_id",object_id);
                        jsonObject.put("checkpointId", aaa.get(i).getId());
                        jsonObject.put("checkpointIndex", aaa.get(i).getIndex());
                    } catch (JSONException e){
                        Log.e("Errr",e.toString());
                        e.printStackTrace();
                    }
                    mSocket.emit("step_into_checkpoint",jsonObject);
                    //aaa.remove(i); //Loại bỏ checkpoint ra khỏi checklist.
                    break;
                }
            }

        }

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.e("BACKGROUND SERVICE","connect locattion");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
             Log.e("Background service","Chưa khai báo permission trong manifes");
             this.stopSelf();
             return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        location =  LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.e("Background Service","Không có location");
        }
        else {
            try{

                JSONObject position = new JSONObject();
                position.put("lat", location.getLatitude());
                position.put("lng" ,location.getLongitude());
                position.put("checkpoint",strCheckPoints);
               // Log.e("Bienso",strBienso);
                position.put("object_name",object_name);
                position.put("mode_id",mode_id);
                position.put("object_id",object_id);

                mSocket.emit("new_car_tracking_detected", position);

                Log.e("BACKGROUND SERVICE","Start new tracking");

                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                mWakelock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
                mWakelock.acquire();
            }catch (JSONException e){
                e.printStackTrace();
            }
        }
    }

    //Kết nối tới hệ thống thành công. Bắt đầu phiên kiểm tra
    private Emitter.Listener onCreateTrakingSession = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject response = (JSONObject)args[0];
            try {
                Log.e("status",response.getString("status"));
                if(response.getInt("status") == 201){
                    sessionID = response.getInt("id");
                    Log.e("SESSION ID",sessionID.toString());

                    SharedPreferences.Editor editor = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE).edit();
                    editor.putInt(Constants.SESSION_TRACKING, sessionID);
                    editor.apply();
                }else{
                    Log.e("errorrr","khong thanh cong");
                }
            } catch (JSONException e) {
                Log.e("Errprrr",e.toString());
                e.printStackTrace();
            }
        }
    };

    private void confirmCreateNewSession(){
        mSocket.on("create_tracking_session",onCreateTrakingSession);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("BACKGROUND SERVICE","CONNECTED LOCATION FAILED");
    }
}
