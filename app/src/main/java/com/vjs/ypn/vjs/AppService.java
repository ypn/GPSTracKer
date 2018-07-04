package com.vjs.ypn.vjs;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ypn on 6/12/2018.
 */


public class AppService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String ERROR_PARSE_JSON = "ERROR_PARSE_JSON";
    private static final String PREFER_KEY_TIME_START_IN_CHECKPOINT = "PREFER_KEY_TIME_START_IN_CHECKPOINT";


    private Socket mSocket;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location location;

    private SharedPreferences sharedPreferences;

    private Integer currenIndexCheckPoint = -1; //Offset của checkpoint hiện tại.
    private boolean inCheckPoint = false; //Trạng thái hiện tại của đối tượng có nằm trong checkpoint hay không.
    private int modeId,objectId; //ID chế độ giám sát, ID trạm giám sát.
    private int sessionId; //ID phiên giám sát.
    private ArrayList<CheckPoint> listCheckPoints; //Danh sách trạm giám sát đối tượng sẽ đi qua.
    private String objectName; //Tên đối tương giám sát.
    private String strCheckPoints; //
    private Long startTime; //Thời điểm đối tượng giám sát vào checkpoint.
    private Integer timeInterval,timeStop=0;
    private JSONArray arrayPosition;

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
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        JSONObject jo = new JSONObject();
        try {
            jo.put("sessionId",sessionId);
            jo.put("mode_id",modeId);
            jo.put("object_id",objectId);
            mSocket.emit("stop_traking",jo);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.off();
        mSocket.disconnect();
        mSocket.close();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences(Constants.SESSION_TRACKING,MODE_PRIVATE);
        listCheckPoints = new ArrayList<>();
        arrayPosition = new JSONArray();

        startTime = sharedPreferences.getLong(PREFER_KEY_TIME_START_IN_CHECKPOINT,new Date().getTime());
        timeInterval = sharedPreferences.getInt(Constants.PREFERKEY_TIME_INTERVAL,5);
        strCheckPoints = sharedPreferences.getString("CHECKPOINTS",null);

        objectName = sharedPreferences.getString("OBJECT_NAME",null);
        objectId = sharedPreferences.getInt("ID_OBJECT_TRACKING",0);
        modeId = Integer.parseInt(sharedPreferences.getString("ID_MODE_TRACKING", Integer.toString(-1)));
        sessionId = sharedPreferences.getInt(Constants.PREFERKEY_SESSION_TRACKING_ID,-1);
        String arrPos = sharedPreferences.getString(Constants.PREFERKEY_LIST_STOP_POSITION,null);
        if(arrPos!=null){
            try {
                arrayPosition = new JSONArray(arrPos);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else{
            arrayPosition = new JSONArray();
        }

        confirmCreateNewSession();

        mSocket.connect();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(timeInterval * 1000)// 10 seconds, in milliseconds
                .setFastestInterval(timeInterval * 1000); // 1 second, in milliseconds

        mGoogleApiClient.connect();

        loadListCheckPoints();

        Intent notificationIntent = new Intent(this, InTrackingActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("RealtimeGPSTracker")
                .setContentText("Request location...")
                .setContentIntent(pendingIntent).build();

        startForeground(1990, notification);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("custom-event-name"));

    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if(!mSocket.connected()){
                mSocket.on("connected_success",connectSuccess);
                mSocket.connect();
            }else{
                stopSelf();
            }
        }
    };

    //Kết nối tới hệ thống thành công. Bắt đầu phiên kiểm tra
    private Emitter.Listener connectSuccess = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            stopSelf();
        }
    };

    private void loadListCheckPoints(){
        try{
            JSONArray jsonArrayCheckPoints  = new JSONArray(strCheckPoints);

            for(int i = 0; i< jsonArrayCheckPoints.length() ; i++ ){
                JSONObject checkpoint = jsonArrayCheckPoints.getJSONObject(i);
                JSONArray path = new JSONArray(checkpoint.getString("polygon"));
                Integer checkpointId = checkpoint.getInt("id");
                List<LatLng> polygon = new ArrayList<>();
                for(int j=0;j<path.length();j++){
                    JSONObject coordinate = path.getJSONObject(j);
                    polygon.add(new LatLng(coordinate.getDouble("lat"), coordinate.getDouble("lng")));
                }

                CheckPoint checkPoint = new CheckPoint(checkpointId,i,polygon);

                listCheckPoints.add(checkPoint);
            }

        }catch(JSONException ex){
            Log.e(ERROR_PARSE_JSON,ex.toString());
        }
    }

    @Override
    public void onLocationChanged(final Location location) {

        if(location.getAccuracy() > 20){
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
        JSONObject position = new JSONObject();
        try {
            position.put("lat", location.getLatitude());
            position.put("lng" ,location.getLongitude());
            position.put("time_at",timeStamp);
            position.put("speed",location.getSpeed());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(location.getSpeed() == 0){
            timeStop +=1;
            if(timeStop * timeInterval >= 1800 && (timeStop * timeInterval) % 600 == 0){
                RequestQueue queue = Volley.newRequestQueue(this);
                StringRequest stringRequest = new StringRequest(Request.Method.POST,Constants.SERVER_URL + "api/v1/mobile/reporttimeout",new Response.Listener<String>(){
                    @Override
                    public void onResponse(String response) {
                        Log.e("TAGGGGG",response);
                    }
                },new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("TAGGGGG",error.toString());
                    }
                }){
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String,String> params  = new HashMap<>();
                        params.put("object_name",""+objectName);
                        params.put("mode_id",""+modeId);
                        params.put("lat",""+location.getLatitude());
                        params.put("lng",""+location.getLongitude());
                        return  params;
                    }
                };

                queue.add(stringRequest);

            }

            arrayPosition.put(position);
            sharedPreferences.edit().putString(Constants.PREFERKEY_LIST_STOP_POSITION,arrayPosition.toString()).apply();
            if(mSocket.connected()){
                mSocket.disconnect();
            }
            return;
        }

        if(!mSocket.connected()){
            mSocket.connect();
        }else{
            timeStop = 0;
            mSocket.emit("update_location",position,arrayPosition,sessionId);
            arrayPosition = new JSONArray();
            //Khởi tạo vị trí checkpoint hiện tại nếu null.

            if(currenIndexCheckPoint!=-1 && PolyUtil.containsLocation(location.getLatitude(),location.getLongitude(),listCheckPoints.get(currenIndexCheckPoint).getPolygon(),false)){
                Log.e("Background Service","Xe van o trong checkpoint "+ listCheckPoints.get(currenIndexCheckPoint).getId());
            }else{

                if(inCheckPoint){

                    Date time_end = new Date();

                    long time_diff = time_end.getTime() - startTime ;


                    listCheckPoints.get(currenIndexCheckPoint).setTotal_time((int) (listCheckPoints.get(currenIndexCheckPoint).getTotal_time() + time_diff/1000));

                    JSONObject jso = new JSONObject();
                    try {
                        jso.put("sessionId",sessionId);
                        jso.put("checkpointId",listCheckPoints.get(currenIndexCheckPoint).getId());
                        jso.put("checkpointIndex",listCheckPoints.get(currenIndexCheckPoint).getIndex());
                        jso.put("mode_id",modeId);
                        jso.put("object_id",objectId);
                        jso.put("total_time",listCheckPoints.get(currenIndexCheckPoint).getTotal_time());
                        mSocket.emit("session_step_out_checkpoint",jso);
                        inCheckPoint = false;
                        currenIndexCheckPoint = -1;
                        sharedPreferences.edit().remove("CURRENT_CHECKPOINT");
                        sharedPreferences.edit().remove("time_start_checkpoint");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                for(int i = 0; i< listCheckPoints.size() ; i++){

                    List<LatLng> lln =  listCheckPoints.get(i).getPolygon();

                    if(PolyUtil.containsLocation(location.getLatitude(),location.getLongitude(),lln,false)){
                        currenIndexCheckPoint = i;
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putInt("CURRENT_CHECKPOINT",currenIndexCheckPoint);
                        inCheckPoint = true;
                        editor.putBoolean("IN_CHECKPOINT",inCheckPoint);
                        startTime = new Date().getTime();
                        editor.putLong("time_start_checkpoint",startTime);
                        editor.commit();

                        JSONObject jsonObject = new JSONObject();
                        try {
                            jsonObject.put("sessionId",sessionId);
                            jsonObject.put("mode_id",modeId);
                            jsonObject.put("object_id",objectId);
                            jsonObject.put("checkpointId", listCheckPoints.get(i).getId());
                            jsonObject.put("checkpointIndex", listCheckPoints.get(i).getIndex());
                        } catch (JSONException e){
                            Log.e("Errr",e.toString());
                            e.printStackTrace();
                        }
                        mSocket.emit("step_into_checkpoint",jsonObject);
                        break;
                    }
                }

            }
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("BACKGROUND_SERVICE","connect google api");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            Log.e("Background_service","Chưa khai báo permission trong manifes");
            this.stopSelf();
            return;
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        location =  LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location == null) {
            Log.e("Background_service","Không có location");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            if(sessionId == -1){
                try{
                    JSONObject position = new JSONObject();
                    position.put("lat", location.getLatitude());
                    position.put("lng" ,location.getLongitude());
                    position.put("checkpoint",strCheckPoints);

                    position.put("object_name",objectName);
                    position.put("mode_id",modeId);
                    position.put("object_id",objectId);

                    mSocket.emit("new_car_tracking_detected", position);

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }

    }


    private void confirmCreateNewSession(){
        mSocket.on("create_tracking_session",onCreateTrakingSession);
    }

    //Kết nối tới hệ thống thành công. Bắt đầu phiên kiểm tra
    private Emitter.Listener onCreateTrakingSession = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject response = (JSONObject)args[0];
            try {
                if(response.getInt("status") == 201){
                    sessionId = response.getInt("id");
                    sharedPreferences.edit().putInt(Constants.PREFERKEY_SESSION_TRACKING_ID,sessionId ).apply();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
