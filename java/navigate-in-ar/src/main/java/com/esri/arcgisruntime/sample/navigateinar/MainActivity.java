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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
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

import java.util.List;

public class MainActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomSheetBehavior<View> sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.standard_bottom_sheet));
        sheetBehavior.setPeekHeight(260);
        sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

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
        mMapPackage = new MobileMapPackage(getExternalFilesDir(null) + "/Steve_Beta.mmpk");
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
                mFeatureLayer = (FeatureLayer) operationalLayers.get(6);

            } else {
                String error = "Error loading mobile map package: " + mMapPackage.getLoadError().getMessage();
                Log.e(TAG, error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
        //[DocRef: END]


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

                                mNavigateButton.setOnClickListener(v -> {
                                    // set the route result in ar navigate activity
                                    ARNavigateActivity.sParcel = mSelectedArcGISFeature.getGeometry();
                                    // pass route to activity and navigate
                                    Intent intent = new Intent(MainActivity.this, ARNavigateActivity.class);
                                    Bundle bundle = new Bundle();
                                    startActivity(intent, bundle);
                                });
                                mNavigateButton.setVisibility(View.VISIBLE);
                                mHelpLabel.setText(R.string.nav_ready_message);

                            }
                        } else {
                            // none of the features on the map were selected
                            Log.d("MainActivity","Nothing Selected");
                        }
                    } catch (Exception e1) {
                        Log.e(TAG, "Select feature failed: " + e1.getMessage());
                    }
                });
                return super.onSingleTapConfirmed(motionEvent);
            }
        });


        /*
        mRouteTask.addDoneLoadingListener(() -> {
            if (mRouteTask.getLoadStatus() == LoadStatus.LOADED) {
                // notify the user to place start point
                mHelpLabel.setText(R.string.place_start_message);
                // listen for a single tap
                mMapView.setOnTouchListener(new DefaultMapViewOnTouchListener(this, mMapView) {
                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                        // if no start point has been defined
                        if (mStartPoint == null) {
                            // create a start point at the tapped point
                            mStartPoint = MainActivity.this.mMapView.screenToLocation(
                                    new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY())));
                            Graphic graphic = new Graphic(mStartPoint);
                            mStopsOverlay.getGraphics().add(graphic);
                            // notify user to place end point
                            mHelpLabel.setText(R.string.place_end_message);
                            // if no end point has been defined
                        } else if (mEndPoint == null) {
                            // crate an end point at the tapped point
                            mEndPoint = MainActivity.this.mMapView.screenToLocation(
                                    new android.graphics.Point(Math.round(motionEvent.getX()), Math.round(motionEvent.getY())));
                            Graphic graphic = new Graphic(mEndPoint);
                            mStopsOverlay.getGraphics().add(graphic);
                            // solve the route between the two points
                            // update UI
                            mHelpLabel.setText(R.string.solving_route_message);
                            final ListenableFuture<RouteParameters> listenableFuture = mRouteTask.createDefaultParametersAsync();
                            listenableFuture.addDoneListener(() -> {
                                try {
                                    RouteParameters routeParameters = listenableFuture.get();
                                    // parameters needed for navigation (happens in ARNavigate)
                                    routeParameters.setReturnStops(true);
                                    routeParameters.setReturnDirections(true);
                                    routeParameters.setReturnRoutes(true);
                                    // this sample is intended for navigating while walking only
                                    List<TravelMode> travelModes = mRouteTask.getRouteTaskInfo().getTravelModes();
                                    TravelMode walkingMode = travelModes.get(0);
                                    routeParameters.setTravelMode(walkingMode);
                                    // add stops
                                    Collection<Stop> routeStops = new ArrayList<>();
                                    routeStops.add(new Stop(mStartPoint));
                                    routeStops.add(new Stop(mEndPoint));
                                    routeParameters.setStops(routeStops);
                                    // set return directions as true to return turn-by-turn directions in the result of
                                    routeParameters.setReturnDirections(true);
                                    // solve the route
                                    ListenableFuture<RouteResult> routeResultFuture = mRouteTask.solveRouteAsync(routeParameters);
                                    routeResultFuture.addDoneListener(() -> {
                                        try {
                                            // get the route result
                                            RouteResult routeResult = routeResultFuture.get();
                                            // get the route from the route result
                                            Route route = routeResult.getRoutes().get(0);
                                            // create a mRouteSymbol graphic
                                            Graphic routeGraphic = new Graphic(route.getRouteGeometry());
                                            // add mRouteSymbol graphic to the map
                                            mRouteOverlay.getGraphics().add(routeGraphic);
                                            mNavigateButton.setOnClickListener(v -> {
                                                // set the route result in ar navigate activity
                                                ARNavigateActivity.sRouteResult = routeResult;
                                                // pass route to activity and navigate
                                                Intent intent = new Intent(MainActivity.this, ARNavigateActivity.class);
                                                Bundle bundle = new Bundle();
                                                startActivity(intent, bundle);
                                            });
                                            mNavigateButton.setVisibility(View.VISIBLE);
                                            mHelpLabel.setText(R.string.nav_ready_message);
                                        } catch (InterruptedException | ExecutionException e) {
                                            String error = "Error getting route result: " + e.getMessage();
                                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                                            Log.e(TAG, error);
                                        }
                                    });
                                } catch (InterruptedException | ExecutionException ex) {
                                    String error = "Error generating route parameters: " + ex.getMessage();
                                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                                    Log.e(TAG, error);
                                }
                            });
                        }
                        return true;
                    }
                });
            } else {
                String error = "Error connecting to route service: " + mRouteTask.getLoadError().getCause();
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                Log.e(TAG, error);
                mHelpLabel.setText(getString(R.string.route_failed_error_message));
            }
        });
        */

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