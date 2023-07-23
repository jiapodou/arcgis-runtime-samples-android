/*
 *  Copyright 2019 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.esri.arcgisruntime.sample.navigateinar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RESULT_LOAD_IMAGE = 1;

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView mHelpLabel;
    private Button mNavigateButton;

    private MapView mMapView;

    private GraphicsOverlay mRouteOverlay;
    private GraphicsOverlay mStopsOverlay;

    private Point mStartPoint;
    private Point mEndPoint;

    // objects that implement Loadable must be class fields to prevent being garbage collected before loading
    private RouteTask mRouteTask;

    private MobileMapPackage mMapPackage;
    private ArcGISMap mMap;
    private FeatureLayer mFeatureLayer;
    private ArcGISFeature mSelectedArcGISFeature;
    private Callout mCallout;
    private String mAttributeID;
    private android.graphics.Point mTapPoint;

    // Recycler View object
    RecyclerView recyclerView;

    // Array list for recycler view data source
    ArrayList<String> source;

    // Layout Manager
    RecyclerView.LayoutManager RecyclerViewLayoutManager;

    // adapter class object
    ImageAdapter adapter;

    // Linear Layout Manager
    LinearLayoutManager HorizontalLayout;

    BottomSheetBehavior<View> sheetBehavior;

    private List<Attachment> attachments;

    private ArrayList<String> attachmentList = new ArrayList<>();

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressDialog = new ProgressDialog(this);

        // initialisation with id's
        recyclerView
                = (RecyclerView)findViewById(R.id.image_recycler_view);
        RecyclerViewLayoutManager
                = new LinearLayoutManager(getApplicationContext());

        // Set LayoutManager on Recycler View
        recyclerView.setLayoutManager(
                RecyclerViewLayoutManager);

        // Adding items to RecyclerView.
        addItemsToRecyclerViewArrayList();

        // calling constructor of adapter
        // with source list as a parameter
        adapter = new ImageAdapter(source);

        // Set Horizontal Layout Manager
        // for Recycler view
        HorizontalLayout
                = new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(HorizontalLayout);

        // Set adapter on recycler view
        recyclerView.setAdapter(adapter);


        adapter.setItemCallback(new ImageAdapter.ItemCallback() {
            @Override
            public void onOpenDetails() {
                selectAttachment();
            }
        });

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet));
        sheetBehavior.setPeekHeight(260);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        // authentication with an API key or named user is required
        // to access basemaps and other location services
        ArcGISRuntimeEnvironment.setApiKey(BuildConfig.API_KEY);

        // get a reference to the map view
        mMapView = findViewById(R.id.mapView);

        // get references to the views defined in the layout
        mHelpLabel = findViewById(R.id.helpLabelTextView);
        mNavigateButton = findViewById(R.id.navigateButton);
        // request location permissions before starting
        requestPermissions();

        //[DocRef: Name=Open Mobile Map Package-android, Category=Work with maps, Topic=Create an offline map]
        // create the mobile map package
        mMapPackage = new MobileMapPackage(getExternalFilesDir(null) + "/Steve_Beta111.mmpk");
        // load the mobile map package asynchronously
        mMapPackage.loadAsync();

        // add done listener which will invoke when mobile map package has loaded
        mMapPackage.addDoneLoadingListener(() -> {
            // check load status and that the mobile map package has maps
            if (mMapPackage.getLoadStatus() == LoadStatus.LOADED && !mMapPackage.getMaps().isEmpty()) {
                // add the map from the mobile map package to the MapView
                mMap = mMapPackage.getMaps().get(0);
                mMapView.setMap(mMap);
                LayerList operationalLayers = mMap.getOperationalLayers();
                Log.d("MainActivity",operationalLayers.get(0).getName());
                mFeatureLayer = (FeatureLayer) operationalLayers.get(0);
                ARNavigateActivity.exeFLayer = (FeatureLayer) operationalLayers.get(1);
                mMap.setBasemap(new Basemap(BasemapStyle.ARCGIS_LIGHT_GRAY));

            } else {
                String error = "Error loading mobile map package: " + mMapPackage.getLoadError().getMessage();
                Log.e(TAG, error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBottomSheetDialog() {
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    // Function to add items in RecyclerView.
    public void addItemsToRecyclerViewArrayList()
    {
        // Adding items to ArrayList
        source = new ArrayList<>();
        source.add("");
    }

    private void selectAttachment() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture"), 1);
    }

    /**
     * Upload the selected image from the gallery as an attachment to the selected feature
     *
     * @param requestCode RESULT_LOAD_IMAGE request code to identify the requesting activity
     * @param resultCode  activity result code
     * @param data        Uri of the selected image
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            try {
                fetchAttachment();
                Log.d("Main", String.valueOf(attachments.size()));
            } catch (Exception e) {
                String error = "Error converting image to byte array: " + e.getMessage();
                Log.e(TAG, error);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * Start location display and define route task and graphic overlays.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void solveRouteBetweenTwoPoints() {

        // enable the map view's location display
        LocationDisplay locationDisplay = mMapView.getLocationDisplay();
        // listen for changes in the status of the location data source.
        locationDisplay.addDataSourceStatusChangedListener(dataSourceStatusChangedEvent -> {
            if (!dataSourceStatusChangedEvent.isStarted() || dataSourceStatusChangedEvent.getError() != null) {
                // report data source errors to the user
                String message = String.format(getString(R.string.data_source_status_error),
                        dataSourceStatusChangedEvent.getSource().getLocationDataSource().getError().getMessage());
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                mHelpLabel.setText(getString(R.string.location_failed_error_message));
            }
        });
        // enable autopan and start location display
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
        locationDisplay.startAsync();

        // create and load a route task from the world routing service. This will trigger logging in to your AGOL account
        mRouteTask = new RouteTask(this, getString(R.string.world_routing_service_url));
        mRouteTask.loadAsync();
        // enable the user to specify a route once the service is ready

        mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent motionEvent) {

                // get the point that was clicked and convert it to a point in map coordinates
                mTapPoint = new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY()));

                // clear any previous selection
                mFeatureLayer.clearSelection();
                mSelectedArcGISFeature = null;

                // identify the GeoElements in the given layer
                final ListenableFuture<IdentifyLayerResult> futureIdentifyLayer = mMapView
                        .identifyLayerAsync(mFeatureLayer, mTapPoint, 5, false, 1);

                // add done loading listener to fire when the selection returns
                futureIdentifyLayer.addDoneListener(() -> {
                    try {
                        // call get on the future to get the result
                        IdentifyLayerResult layerResult = futureIdentifyLayer.get();
                        List<GeoElement> resultGeoElements = layerResult.getElements();
                        if (!resultGeoElements.isEmpty()) {
                            if (resultGeoElements.get(0) instanceof ArcGISFeature) {
                                mSelectedArcGISFeature = (ArcGISFeature) resultGeoElements.get(0);
                                // highlight the selected feature
                                mFeatureLayer.selectFeature(mSelectedArcGISFeature);
                                Log.d("MainActivity", mSelectedArcGISFeature.toString());
                                mSelectedArcGISFeature.getGeometry();
                                mAttributeID = mSelectedArcGISFeature.getAttributes().get("objectid").toString();

                                showBottomSheetDialog();

                                mNavigateButton.setOnClickListener(v -> {
                                    if (mSelectedArcGISFeature != null) {
                                        // set the route result in ar navigate activity
                                        ARNavigateActivity.sParcel = mSelectedArcGISFeature.getGeometry();
                                        // pass route to activity and navigate
                                        Intent intent = new Intent(MainActivity.this, ARNavigateActivity.class);
                                        Bundle bundle = new Bundle();
                                        startActivity(intent, bundle);
                                    }

                                });
                                mNavigateButton.setVisibility(View.VISIBLE);
                                mHelpLabel.setText(R.string.nav_ready_message);

                            }
                        } else {
                            // none of the features on the map were selected
                            Log.d("MainActivity","Nothing Selected");
                            sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        }
                    } catch (Exception e1) {
                        Log.e(TAG, "Select feature failed: " + e1.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(motionEvent);
            }
        });

        // create a graphics overlay for showing the calculated route and add it to the map view
        mRouteOverlay = new GraphicsOverlay();
        SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.YELLOW, 1);
        SimpleRenderer routeRenderer = new SimpleRenderer(routeSymbol);
        mRouteOverlay.setRenderer(routeRenderer);
        mMapView.getGraphicsOverlays().add(mRouteOverlay);
        // create and configure an overlay for showing the route's stops and add it to the map view
        mStopsOverlay = new GraphicsOverlay();
        SimpleMarkerSymbol stopSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 5);
        SimpleRenderer stopRenderer = new SimpleRenderer(stopSymbol);
        mStopsOverlay.setRenderer(stopRenderer);
        mMapView.getGraphicsOverlays().add(mStopsOverlay);
    }

    private void fetchAttachment() {

        attachmentList = new ArrayList<>();

        final ListenableFuture<List<Attachment>> attachmentResults = mSelectedArcGISFeature.fetchAttachmentsAsync();
        attachmentResults.addDoneListener(() -> {
            try {
                attachments = attachmentResults.get();
                // if selected feature has attachments, display them in a list fashion
                if (!attachments.isEmpty()) {
                    for (Attachment attachment : attachments) {
                        attachmentList.add(attachment.getName());
                    }
                    runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
//                        adapter = new CustomList(this, attachmentList);
//                        listView.setAdapter(adapter);
//                        adapter.notifyDataSetChanged();
                    });
                }
            } catch (Exception e) {
                String error = "Error getting attachment: " + e.getMessage();
                Log.e(TAG, error);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchAttachmentAsync(final int position) {

        progressDialog.setTitle("Downloading");
        progressDialog.setMessage("Wait");
        progressDialog.show();

        // create a listenableFuture to fetch the attachment asynchronously
        final ListenableFuture<InputStream> fetchDataFuture = attachments.get(position).fetchDataAsync();
        fetchDataFuture.addDoneListener(() -> {
            try {
                String fileName = attachmentList.get(position);
                // create a drawable from InputStream
                Drawable d = Drawable.createFromStream(fetchDataFuture.get(), fileName);
                // create a bitmap from drawable
                Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                File fileDir = new File(getExternalFilesDir(null) + "/ArcGIS/Attachments");
                // create folder /ArcGIS/Attachments in external storage
                boolean isDirectoryCreated = fileDir.exists();
                if (!isDirectoryCreated) {
                    isDirectoryCreated = fileDir.mkdirs();
                }
                File file = null;
                if (isDirectoryCreated) {
                    file = new File(fileDir, fileName);
                    FileOutputStream fos = new FileOutputStream(file);
                    // compress the bitmap to PNG format
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.flush();
                    fos.close();
                }

                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                // open the file in gallery
                Intent i = new Intent();
                i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.setAction(Intent.ACTION_VIEW);
                Uri contentUri = FileProvider
                        .getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", file);
                i.setDataAndType(contentUri, "image/png");
                startActivity(i);

            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        });
    }

    /**
     * Request read external storage for API level 23+.
     */
    private void requestPermissions() {
        // define permission to request
        String[] reqPermission = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };
        int requestCode = 2;
        if (ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, reqPermission[1]) == PackageManager.PERMISSION_GRANTED) {
            solveRouteBetweenTwoPoints();
        } else {
            // request permission
            ActivityCompat.requestPermissions(this, reqPermission, requestCode);
        }
    }

    /**
     * Handle the permissions request response.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            solveRouteBetweenTwoPoints();
        } else {
            // report to user that permission was denied
            Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onPause() {
        mMapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.resume();
    }

    @Override
    protected void onDestroy() {
        mMapView.dispose();
        super.onDestroy();
    }
}