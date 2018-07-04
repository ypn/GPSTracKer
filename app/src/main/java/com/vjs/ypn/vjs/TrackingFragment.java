package com.vjs.ypn.vjs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TrackingFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TrackingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackingFragment extends Fragment implements OnMapReadyCallback {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private MapView mapView;
    private GoogleMap gmap;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView tv_object_tracking;

    private OnFragmentInteractionListener mListener;
    private FloatingActionButton btn_start_tracking;

    private Button btnToggleMap;
    RecyclerView mRecyclerView;
    RecyclerViewAdapter mRcvAdapter;
    List<CheckPoints> data;
    private String objectTracking;

    ArrayList<PolygonOptions> listPolygon = new ArrayList<>();


    public TrackingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TrackingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TrackingFragment newInstance(String param1, String param2) {
        TrackingFragment fragment = new TrackingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_tracking, container, false);
        data = new ArrayList<>();
        btnToggleMap = v.findViewById(R.id.btnToggleMap);
        objectTracking = getArguments().getString("OBJECT_TRACKING");

        tv_object_tracking = v.findViewById(R.id.tv_object_tracking);
        tv_object_tracking.setText(objectTracking);

        mapView = v.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        btnToggleMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapFragment mapFragment = MapFragment.newInstance("a","b");

                getFragmentManager()
                        .beginTransaction()
                        .addSharedElement(mapView, ViewCompat.getTransitionName(mapView))
                        .addToBackStack(null)
                        .replace(R.id.content,mapFragment)
                        .commit();
            }
        });

        mRecyclerView = v.findViewById(R.id.rcv_list_checkpoints);


        try {
            JSONArray listCheckPoints = new JSONArray(getArguments().getString(Constants.LIST_CHECKPOINTS));

            for(int i =0 ; i< listCheckPoints.length(); i++){
                try {

                    JSONObject object = listCheckPoints.getJSONObject(i);

                    Log.e("OBJECT",object.toString());

                    String polygon = object.getString("polygon");

                    JSONArray jsonArray = new JSONArray(polygon);

                    PolygonOptions options = new PolygonOptions()
                            .strokeColor(Color.argb(80,0, 188, 212))
                            .strokeWidth(1)
                            .fillColor(Color.argb(35,0, 188, 212));

                    for(int j=0;j<jsonArray.length() ; j++){
                        options.add(new LatLng( jsonArray.getJSONObject(j).getDouble("lat"), jsonArray.getJSONObject(j).getDouble("lng")));
                    }

                    listPolygon.add(options);

                    CheckPoints checkPoints = new CheckPoints();
                    checkPoints.setId(object.getInt("id"));

                    int time = object.getInt("time");

                    int min = time /60;

                    int sec = time -min * 60;

                    checkPoints.setMax_time(min+" phút " + (sec!=0 ? sec + " giây" : ""));
                    checkPoints.setName(object.getString("name"));
                    data.add(checkPoints);
                    mRcvAdapter = new RecyclerViewAdapter(data);

                    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
                    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

                    mRecyclerView.setLayoutManager(layoutManager);
                    mRecyclerView.setAdapter(mRcvAdapter);

                } catch (JSONException e) {
                    Log.e("ERRRR",e.toString());
                    e.printStackTrace();
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

        btn_start_tracking = v.findViewById(R.id.btn_start_tracking);

        btn_start_tracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(),InTrackingActivity.class);
                intent.putExtra("CHECKPOINTS",getArguments().getString(Constants.LIST_CHECKPOINTS));
                intent.putExtra("BIENSO",objectTracking);
                intent.putExtra("ID_OBJECT_TRACKING",getArguments().getInt("ID_OBJECT_TRACKING"));
                intent.putExtra("ID_MODE_TRACKING",getArguments().getString("ID_MODE_TRACKING"));
                startActivity(intent);
            }
        });
        return  v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gmap = googleMap;
        gmap.setMinZoomPreference(12);
        LatLng ny = new LatLng(20.905397, 106.631005);

        for (PolygonOptions op: listPolygon
             ) {
            gmap.addPolygon(op);
        }
        gmap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        gmap.moveCamera(CameraUpdateFactory.newLatLng(ny));
        gmap.moveCamera(CameraUpdateFactory.zoomTo(16f));
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

        @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
