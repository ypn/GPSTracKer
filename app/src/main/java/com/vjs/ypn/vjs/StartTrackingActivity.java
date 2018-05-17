package com.vjs.ypn.vjs;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import android.app.ProgressDialog;

import android.net.Uri;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class StartTrackingActivity extends AppCompatActivity implements
        TrackingFragment.OnFragmentInteractionListener,
        MapFragment.OnFragmentInteractionListener {

    private RequestQueue queue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_tracking);
        loadListCheckPoints();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        queue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
        queue.stop();
    }

    private  void loadListCheckPoints(){
        final  ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("đang tải...");
        dialog.show();
        queue = Volley.newRequestQueue(this);
        String url_request = Constants.SERVER_URL + "api/v1/mobile/checkpoints/list-checkpoint-of-mode";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url_request, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Bundle bundle = new Bundle();
                bundle.putString(Constants.LIST_CHECKPOINTS,response.toString());
                bundle.putString("OBJECT_TRACKING",getIntent().getStringExtra("OBJECT_TRACKING"));
                bundle.putInt("ID_OBJECT_TRACKING",getIntent().getIntExtra("ID_OBJECT_TRACKING",0));
                bundle.putString("ID_MODE_TRACKING",getIntent().getStringExtra(Constants.ID_TRACKING_CASE));

                FragmentManager fragMan = getFragmentManager();
                FragmentTransaction fragTransaction = fragMan.beginTransaction();

                TrackingFragment trackingFragment  = TrackingFragment.newInstance("1","2");
                trackingFragment.setArguments(bundle);

                fragTransaction.add(R.id.content,trackingFragment);

                fragTransaction.commit();

                dialog.dismiss();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dialog.dismiss();
            }
        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String,String> params = new HashMap<>();
                params.put("mode_id",getIntent().getStringExtra(Constants.ID_TRACKING_CASE));
                return params;
            }
        };

        queue.add(stringRequest);
    }

}
