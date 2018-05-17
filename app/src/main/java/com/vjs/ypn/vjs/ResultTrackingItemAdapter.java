package com.vjs.ypn.vjs;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ypn on 2/12/2018.
 */

public class ResultTrackingItemAdapter extends RecyclerView.Adapter<ResultTrackingItemAdapter.AViewHolder> {

    List<ResultTrackingItem> resultTrackingItems  = new ArrayList<>();

    public ResultTrackingItemAdapter(List<ResultTrackingItem> resultTrackingItems){
        this.resultTrackingItems = resultTrackingItems;
    }

    @Override
    public AViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater  inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.result_tracking_item,parent,false);
        return new AViewHolder(view);
    }

    @Override
    public void onBindViewHolder(AViewHolder holder, int position) {
        holder.tv_name_checkpoint.setText(resultTrackingItems.get(position).getName());
        holder.tv_tracking_time.setText(resultTrackingItems.get(position).getTime_tracking());
        holder.tv_estimate_time.setText(resultTrackingItems.get(position).getMax_time());
        holder.tv_diff_time.setText(resultTrackingItems.get(position).getDiff_time());

    }

    @Override
    public int getItemCount() {
        return resultTrackingItems.size();
    }

    public class AViewHolder extends RecyclerView.ViewHolder{
        TextView tv_name_checkpoint,tv_tracking_time,tv_estimate_time,tv_diff_time;
        public AViewHolder(View itemView) {
            super(itemView);
            tv_name_checkpoint = itemView.findViewById(R.id.tv_name_checkpoint);
            tv_tracking_time = itemView.findViewById(R.id.tv_tracking_time);
            tv_estimate_time = itemView.findViewById(R.id.tv_estimate_time);
            tv_diff_time = itemView.findViewById(R.id.tv_diff_time);
        }
    }
}
