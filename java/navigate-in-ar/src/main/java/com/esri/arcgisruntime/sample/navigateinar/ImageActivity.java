package com.esri.arcgisruntime.sample.navigateinar;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class ImageActivity extends AppCompatActivity {

    public static Drawable drawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        // ensure at route has been set by the previous activity
        if (drawable == null) {
            String error = "Parcel not set before launching activity!";
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            Log.e("MainImage", error);
        }

        ImageView imageview= (ImageView)findViewById(R.id.image_main);
        imageview.setImageDrawable(drawable);
    }
}
