package com.vjs.ypn.vjs;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ypn on 2/9/2018.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>{

    private List<CheckPoints> data = new ArrayList<>();

    public RecyclerViewAdapter(List<CheckPoints> data) {
        this.data = data;
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.recycler_view_item, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        holder.tv_stt.setText(""+ position);
        holder.tv_name_checkpoint.setText(data.get(position).getName());
        holder.tv_time_max.setText(data.get(position).getMax_time());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }


    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        TextView tv_stt,tv_name_checkpoint,tv_time_max;
        public RecyclerViewHolder(View itemView) {
            super(itemView);
            tv_stt = itemView.findViewById(R.id.tv_stt);
            tv_name_checkpoint = itemView.findViewById(R.id.tv_name_checkpoint);
            tv_time_max = itemView.findViewById(R.id.tv_time_max);
        }
    }
}
