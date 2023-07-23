package com.esri.arcgisruntime.sample.navigateinar;

import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.LayoutInflater;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

// The adapter class which extends RecyclerView Adapter
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.MyView> {

    // List with String type
    private List<String> list;

    private List<Drawable> listImage;

    public void setArticles(List<Drawable> listImage) {
        if (this.listImage == null) this.listImage = new ArrayList<>();
        this.listImage.clear();
        this.listImage.addAll(listImage);
        notifyDataSetChanged();
    }


    // View Holder class which extends RecyclerView.ViewHolder
    public class MyView extends RecyclerView.ViewHolder {

        // Text View
        TextView textView;
        ImageView imageView;

        // parameterised constructor for View Holder class
        // which takes the view as a parameter
        public MyView(View view) {
            super(view);


            // initialise TextView with id
            imageView = (ImageView) view.findViewById(R.id.image_view);
        }
    }

    interface ItemCallback {
        void onOpenDetails();

        void onOpenIntent(int pos);
    }

    private ItemCallback itemCallback;

    public void setItemCallback(ItemCallback itemCallback) {
        this.itemCallback = itemCallback;
    }

    // Constructor for adapter class
    // which takes a list of String type
    public ImageAdapter(List<String> horizontalList, List<Drawable> drawables) {
        this.list = horizontalList;
        this.listImage = drawables;
    }

    // Override onCreateViewHolder which deals with the inflation of the card layout as an item for the RecyclerView.
    @Override
    public MyView onCreateViewHolder(ViewGroup parent,
                                     int viewType) {

        // Inflate item.xml using LayoutInflator
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_image, parent, false);

        // return itemView
        return new MyView(itemView);
    }

    // Override onBindViewHolder which deals with the setting of different data
    // and methods related to clicks on
    // particular items of the RecyclerView.
    @Override
    public void onBindViewHolder(final MyView holder, final int position) {

        if (listImage != null && listImage.size() != 0) {
            holder.imageView.setImageDrawable(listImage.get(position));
            Log.d("MainAdapr", String.valueOf(listImage.size()));
        }

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("MainAdapr", "clicked");

                if (listImage == null || listImage.isEmpty()) {
                    itemCallback.onOpenDetails();
                } else {
                    itemCallback.onOpenIntent(holder.getAbsoluteAdapterPosition());
                    Log.d("MainAdapr", "open" + holder.getAbsoluteAdapterPosition());
                }
            }
        });

    }

    // Override getItemCount which Returns
    // the length of the RecyclerView.
    @Override
    public int getItemCount() {
        if (listImage == null) return 1;
        return listImage.size() == 0 ? 1 : listImage.size();
    }
}
