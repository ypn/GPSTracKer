package com.vjs.ypn.vjs;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CaseTrackingActivity extends AppCompatActivity {

    String url ="http://113.160.215.214:8092/api/v1/mobile";
    RequestQueue queue;
    private ProgressBar pbLoad;

    JSONArray listMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences settings = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE);
        settings.edit().clear().commit();
        setContentView(R.layout.activity_case_tracking);

        queue = Volley.newRequestQueue(this);
        pbLoad = findViewById(R.id.pb_load);

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.e("REQUEST LOCATION","LOCATION");
        } else {
            Log.e("REQUEST LOCATION","LOCATION Failess");
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }

        RadioGroup radioGroup = findViewById(R.id.radiogroup_choose_case);
        createRadioGroup();
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                Intent intent = new Intent(CaseTrackingActivity.this,EntryInfoActivity.class);
                try {
                    intent.putExtra("DATA_MODE",listMode.getJSONObject(i).toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        pbLoad.setVisibility(View.INVISIBLE);
        super.onDestroy();

    }

    private void createRadioGroup(){
        pbLoad.setVisibility(View.VISIBLE);

        final RadioGroup group = findViewById(R.id.radiogroup_choose_case);
        final RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 5, 0, 5);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url + "/mode-tracking/list-enabled", null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    listMode = response.getJSONArray("list");

                    for(int i = 0; i < listMode.length() ; i++){

                    RadioButton radioButton = new RadioButton(getApplicationContext());
                    try {
                        String ten = listMode.getJSONObject(i).getString("name");
                        radioButton.setText(ten);
                        radioButton.setTextSize(20f);
                        radioButton.setLayoutParams(params);
                        radioButton.setId(i);

                        radioButton.setTextColor(Color.DKGRAY);
                        group.addView(radioButton);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    pbLoad.setVisibility(View.INVISIBLE);

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    pbLoad.setVisibility(View.INVISIBLE);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("Err","loiiii");
                pbLoad.setVisibility(View.INVISIBLE);
            }
        });

        queue.add(jsonObjectRequest);
    }
}
