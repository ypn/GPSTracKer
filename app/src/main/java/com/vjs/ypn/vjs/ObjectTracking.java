package com.vjs.ypn.vjs;

/**
 * Created by ypn on 2/8/2018.
 */

public class ObjectTracking {
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMa_so() {
        return ma_so;
    }

    public void setMa_so(String ma_so) {
        this.ma_so = ma_so;
    }

    private Integer id;
    private String ma_so;

    @Override
    public String toString() {
        return ma_so;
    }


}
