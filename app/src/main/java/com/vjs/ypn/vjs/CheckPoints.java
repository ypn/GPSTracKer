package com.vjs.ypn.vjs;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ypn on 2/9/2018.
 */

public class CheckPoints {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private Integer index;

    public Integer getTotal_time() {
        return total_time;
    }

    public void setTotal_time(Integer total_time) {
        this.total_time = total_time;
    }

    private Integer total_time;
    private List<LatLng> polygon = new ArrayList<>();

//    public CheckPoints(Integer id, Integer index, List<LatLng> polygon) {
//        this.id = id;
//        this.polygon = polygon;
//        this.index = index;
//    }

    public List<LatLng> getPolygon() {
        return polygon;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public void setPolygon(List<LatLng> polygon) {
        this.polygon = polygon;
    }

    public Integer getIndex() {
        return index;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMax_time() {
        return max_time;
    }

    public void setMax_time(String max_time) {
        this.max_time = max_time;
    }

    private int id;
    private String name;
    private String max_time;
}
