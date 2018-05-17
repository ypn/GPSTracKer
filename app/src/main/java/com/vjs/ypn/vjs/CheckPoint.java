package com.vjs.ypn.vjs;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ypn on 1/7/2018.
 */

public class CheckPoint {
    private Integer id;
    private Integer index;

    private Integer total_time = 0;
    private List<LatLng> polygon = new ArrayList<>();



    public CheckPoint(){
        this.polygon = new ArrayList<>();
        this.id =0;

    }

    public CheckPoint(Integer id, Integer index, List<LatLng> polygon) {
        this.id = id;
        this.polygon = polygon;
        this.index = index;
    }

    public Integer getTotal_time() {
        return total_time;
    }

    public void setTotal_time(Integer total_time) {
        this.total_time = total_time;
    }

    public List<LatLng> getPolygon() {
        return polygon;
    }

    public Integer getId() {
        return id;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
