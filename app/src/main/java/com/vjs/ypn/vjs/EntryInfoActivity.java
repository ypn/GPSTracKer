package com.vjs.ypn.vjs;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryInfoActivity extends AppCompatActivity {

    private List<ObjectTracking> patterm  ;
    RequestQueue queue;
    private ActionBar actionBar;
    private TextView tvTitleObjectTracing;
    private SearchableSpinner searchableSpinner;
    private ProgressBar pbLoad;
    private LinearLayout ln_search,ln_object_info;
    private Button btn_next;
    private int modeId;
    private JSONObject data_mode;
    private String objectTracking = new String();
    private int idObjectTracking;
    private JSONArray listData;
    private int time_interval;

    private TextView tv_don_vi,tv_so_dien_thoai,tv_cmt,tv_ma_so,tv_object_owner;
    String url ="http://113.160.215.214:8092/api/v1/mobile/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_info);

        queue = Volley.newRequestQueue(this);
        patterm = new ArrayList<>();

        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        ln_search = findViewById(R.id.ln_search);
        pbLoad = findViewById(R.id.pb_load);


        tvTitleObjectTracing = ln_search.findViewById(R.id.tv_title_object_tracking);


        try {
            data_mode = new JSONObject(getIntent().getStringExtra("DATA_MODE"));
            actionBar.setTitle(data_mode.getString("name"));
            modeId  = data_mode.getInt("id");
            time_interval = data_mode.getInt("time_frequency");
            SharedPreferences settings = getSharedPreferences(Constants.SESSION_TRACKING, MODE_PRIVATE);
            settings.edit().putInt("TIME_INTERVAL" ,5);
            settings.edit().commit();

            tvTitleObjectTracing.setText(data_mode.getString("display_property"));

        } catch (JSONException e) {
            e.printStackTrace();
        }


        loadListObjectTracking();

        btn_next = findViewById(R.id.btn_next);



        btn_next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(EntryInfoActivity.this,StartTrackingActivity.class);
                intent.putExtra(Constants.ID_TRACKING_CASE,"" + modeId);
                intent.putExtra("OBJECT_TRACKING",objectTracking);
                intent.putExtra("ID_OBJECT_TRACKING",idObjectTracking);
                startActivity(intent);
                queue.cancelAll(new RequestQueue.RequestFilter() {
                    @Override
                    public boolean apply(Request<?> request) {
                        return true;
                    }
                });
                queue.stop();
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        Fragment searchableSpinnerDialog = getFragmentManager().findFragmentByTag("TAG");
        if (searchableSpinnerDialog != null && searchableSpinnerDialog.isAdded()) {
            getFragmentManager().beginTransaction().remove(searchableSpinnerDialog).commit();
        }
    }

    @Override
    protected void onDestroy() {
        pbLoad.setVisibility(View.INVISIBLE);
        super.onDestroy();
    }


    //Tải danh sách đối tượng theo dõi theo chế độ theo dõi
    private void loadListObjectTracking(){
        pbLoad.setVisibility(View.VISIBLE);
        StringRequest stringRequest = new StringRequest(Request.Method.POST,url + "object-tracking/list",new Response.Listener<String>(){
            @Override
            public void onResponse(String response) {

                searchableSpinner = ln_search.findViewById(R.id.ss_object_tracking);

                try {
                    JSONObject obj = new JSONObject(response);
                    listData = obj.getJSONArray("list");
                    searchableSpinner.setTitle(data_mode.getString("display_property"));

                    for(int i =0; i< listData.length() ; i++){
                        ObjectTracking obt = new ObjectTracking();
                        obt.setId(listData.getJSONObject(i).getInt("id"));
                        obt.setMa_so(listData.getJSONObject(i).getString("object_name"));
                        patterm.add(obt);
                    }

                    ArrayAdapter<ObjectTracking> adapter = new ArrayAdapter<>(EntryInfoActivity.this,android.R.layout.simple_list_item_1,patterm);


                    searchableSpinner.setAdapter(adapter);

                    searchableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            showObjectInfo(i);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    pbLoad.setVisibility(View.INVISIBLE);
                    ln_search.setVisibility(View.VISIBLE);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        },new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("TAGGGGG",error.toString());
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params  = new HashMap<>();
                try {
                    params.put("table",data_mode.getString("table_reference"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return  params;
            }
        };

        queue.add(stringRequest);
    }

    private void showObjectInfo(int index){
        try {
            JSONObject obj = listData.getJSONObject(index);

            objectTracking = obj.getString("object_name");
            idObjectTracking = obj.getInt("id");

            if(ln_object_info == null){
                ln_object_info = findViewById(R.id.ln_object_info);
            }
            if(tv_ma_so==null){
                tv_ma_so = findViewById(R.id.tv_ma_so);
            }
            if(tv_don_vi == null){
                tv_don_vi = ln_object_info.findViewById(R.id.tv_don_vi);
            }

            if(tv_object_owner == null){
                tv_object_owner = ln_object_info.findViewById(R.id.tv_object_owner);
            }

            if(tv_so_dien_thoai == null) {
                tv_so_dien_thoai = ln_object_info.findViewById(R.id.tv_so_dien_thoai);
            }

            if(tv_cmt == null){
                tv_cmt = ln_object_info.findViewById(R.id.tv_cmt);
            }

            if(!data_mode.isNull("object_owner")){
                tv_object_owner.setText(data_mode.getString("object_owner"));
                tv_ma_so.setText(obj.getString("object_owner"));
            }
            else{
                tv_object_owner.setText("Họ và tên");
                tv_ma_so.setText(obj.getString("object_name"));
            }

            if(!obj.isNull("phone_number")){
                tv_so_dien_thoai.setText(obj.getString("phone_number"));
            }

            if(!obj.isNull("identification")){
                tv_cmt.setText(obj.getString("identification"));
            }


            tv_don_vi.setText(obj.getString("organization"));

            ln_object_info.setVisibility(View.VISIBLE);


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
