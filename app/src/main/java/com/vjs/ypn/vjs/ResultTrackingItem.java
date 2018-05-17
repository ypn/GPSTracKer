package com.vjs.ypn.vjs;

/**
 * Created by ypn on 2/12/2018.
 */

public class ResultTrackingItem {
    private String name;
    private String time_tracking;
    private String max_time;
    private String diff_time;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime_tracking() {
        return time_tracking;
    }

    public void setTime_tracking(String time_tracking) {
        this.time_tracking = time_tracking;
    }

    public String getMax_time() {
        return max_time;
    }

    public void setMax_time(String max_time) {
        this.max_time = max_time;
    }

    public String getDiff_time() {
        return diff_time;
    }

    public void setDiff_time(String diff_time) {
        this.diff_time = diff_time;
    }
}
