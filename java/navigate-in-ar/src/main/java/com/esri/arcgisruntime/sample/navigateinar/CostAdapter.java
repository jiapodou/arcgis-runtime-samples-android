package com.esri.arcgisruntime.sample.navigateinar;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;
import androidx.recyclerview.widget.RecyclerView;

import org.w3c.dom.Text;

import java.util.List;

// The adapter class which extends RecyclerView Adapter
public class CostAdapter extends RecyclerView.Adapter<CostAdapter.MyView> {

    // List with String type
    private List<List<String>> list;

    private List<String> listImage;

    // View Holder class which extends RecyclerView.ViewHolder
    public class MyView extends RecyclerView.ViewHolder {

        // Text View
        TextView title;
        TextView textViewSite;
        TextView textViewUtil;
        TextView textViewCons;
        TextView textViewDesign;
        TextView textViewSur;
        TextView textViewPer;
        TextView textViewHarcS;
        TextView textViewHarcE;
        TextView textViewSoftS;
        TextView textViewSoftE;
        TextView textViewTS;
        TextView textViewTE;


        // parameterised constructor for View Holder class
        // which takes the view as a parameter
        public MyView(View view) {
            super(view);

            // initialise TextView with id
            title = (TextView) view.findViewById(R.id.textview1);
            textViewSite = (TextView) view.findViewById(R.id.textviewSite);
            textViewUtil = (TextView) view.findViewById(R.id.textviewUtilities);
            textViewCons = (TextView) view.findViewById(R.id.textviewConstruction);
            textViewDesign = (TextView) view.findViewById(R.id.textviewDesign);
            textViewSur = (TextView) view.findViewById(R.id.textviewSurvey);
            textViewPer = (TextView) view.findViewById(R.id.textviewPermits);
            textViewHarcS = (TextView) view.findViewById(R.id.textviewHardStart);
            textViewHarcE = (TextView) view.findViewById(R.id.textviewHardEnd);
            textViewSoftS = (TextView) view.findViewById(R.id.textviewSoftStart);
            textViewSoftE = (TextView) view.findViewById(R.id.textviewSoftEnd);
            textViewTS = (TextView) view.findViewById(R.id.textviewTStart);
            textViewTE = (TextView) view.findViewById(R.id.textviewTEnd);

        }
    }


    // Constructor for adapter class
    // which takes a list of String type
    public CostAdapter(List<List<String>> horizontalList) {
        this.list = horizontalList;
    }

    // Override onCreateViewHolder which deals
    // with the inflation of the card layout
    // as an item for the RecyclerView.
    @Override
    public MyView onCreateViewHolder(ViewGroup parent,
                                     int viewType) {

        // Inflate item.xml using LayoutInflator
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_cost, parent, false);

        // return itemView
        return new MyView(itemView);
    }

    // Override onBindViewHolder which deals with the setting of different data
    // and methods related to clicks on
    // particular items of the RecyclerView.
    @Override
    public void onBindViewHolder(final MyView holder, final int position) {
        // Set the text of each item of
        // Recycler view with the list items
        holder.textViewSite.setText(list.get(position).get(0));
        holder.textViewUtil.setText(list.get(position).get(1));
        holder.textViewCons.setText(list.get(position).get(2));
        holder.textViewDesign.setText(list.get(position).get(3));
        holder.textViewSur.setText(list.get(position).get(4));
        holder.textViewPer.setText(list.get(position).get(5));
        holder.textViewHarcS.setText(list.get(position).get(6));
        holder.textViewHarcE.setText(list.get(position).get(7));
        holder.textViewSoftS.setText(list.get(position).get(8));
        holder.textViewSoftE.setText(list.get(position).get(9));
        holder.textViewTS.setText(list.get(position).get(10));
        holder.textViewTE.setText(list.get(position).get(11));
        holder.title.setText(list.get(position).get(12));
    }

    // Override getItemCount which Returns
    // the length of the RecyclerView.
    @Override
    public int getItemCount() {
        return list.size();
    }
}
