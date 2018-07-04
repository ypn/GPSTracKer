package com.vjs.ypn.vjs;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ShowResultActivity extends AppCompatActivity implements OnMapReadyCallback {

    private RecyclerView recyclerView;
    private List<ResultTrackingItem> data = new ArrayList<>();
    private LinearLayout ln_reset,ln_other_mode;
    private GoogleMap map;
    private TextView tv_time_tracking,tv_d;

    ResultTrackingItemAdapter adapter;
    RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_result);

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("đang tải...");
        dialog.show();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);

        recyclerView = findViewById(R.id.rcv_tracking_result);

        tv_time_tracking = findViewById(R.id.tv_time_tracking);
        tv_d = findViewById(R.id.tv_d);

        SharedPreferences prefs = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE);
        Integer sessionId = prefs.getInt(Constants.PREFERKEY_SESSION_TRACKING_ID, -1);
        Toast.makeText(this,"sdfsdfsd:" + sessionId,Toast.LENGTH_SHORT).show();
        if (sessionId != -1) {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.SERVER_URL + "api/v1/report/get-tracking-detail/" + sessionId, null, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    SharedPreferences settings = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE);
                    settings.edit().clear().commit();
                    dialog.dismiss();
                    tv_d.setText(getIntent().getStringExtra("d")+ "met");
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        Date created_at = sdf.parse(response.getString("created_at"));
                        Date end_at = sdf.parse(response.getString("ended_at"));

                        long time_diff = (end_at.getTime() - created_at.getTime())/1000;

                        tv_time_tracking.setText(time_diff/60+"min"+":"+time_diff%60+"sec");

                        String path = response.getString("path");

                        JSONArray jsonArray = new JSONArray(path);

                        PolylineOptions options = new PolylineOptions().width(5).color(Color.RED);

                        for(int i=0;i < jsonArray.length() ; i++){
                            JSONObject jsonObject =  new JSONObject(jsonArray.getString(i));
                            options.add(new LatLng(jsonObject.getDouble("lat"),jsonObject.getDouble("lng")));
                        }

                        map.addPolyline(options);

                        JSONArray checkpoints = response.getJSONArray("status");

                        for(int i=0; i<checkpoints.length() ; i++){
                            JSONObject cp = checkpoints.getJSONObject(i);
                            ResultTrackingItem item = new ResultTrackingItem();
                            item.setName(cp.getString("name"));

                            Integer timeTracking = !cp.getString("total_time").equals("") ? Integer.parseInt(cp.getString("total_time")) :0;
                            int min = timeTracking/60;
                            int sec = timeTracking%60;

                            item.setTime_tracking(min+"min" + ":" + sec+"sec");

                            Integer max_time =  Integer.parseInt(cp.getString("max_time"));
                            int m = max_time/60;

                            item.setMax_time(m + " phút");

                            data.add(item);
                            adapter = new ResultTrackingItemAdapter(data);

                            LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
                            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

                            recyclerView.setLayoutManager(layoutManager);
                            recyclerView.setAdapter(adapter);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (ParseException ex){
                        ex.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    dialog.dismiss();
                    Log.e("Result request",error.toString());
                }
            });

            queue.add(jsonObjectRequest);
        }else{
            dialog.dismiss();
        }

        ln_reset = findViewById(R.id.ln_reset);
        ln_other_mode = findViewById(R.id.ln_other_mode);

        ln_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ShowResultActivity.this,CaseTrackingActivity.class));
            }
        });

        ln_other_mode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startActivity(new Intent(ShowResultActivity.this,EntryInfoActivity.class));
                startActivity(new Intent(ShowResultActivity.this,CaseTrackingActivity.class));
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences settings = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE);
        settings.edit().clear().commit();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng ny = new LatLng(20.905397, 106.631005);
        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        map.moveCamera(CameraUpdateFactory.newLatLng(ny));
        map.moveCamera(CameraUpdateFactory.zoomTo(17f));
    }
}
