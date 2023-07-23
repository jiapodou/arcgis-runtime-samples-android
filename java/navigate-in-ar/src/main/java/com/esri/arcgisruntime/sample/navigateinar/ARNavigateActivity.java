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

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.location.AndroidLocationDataSource;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.MobileScenePackage;
import com.esri.arcgisruntime.mapping.NavigationConstraint;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties;
import com.esri.arcgisruntime.navigation.RouteTracker;
import com.esri.arcgisruntime.symbology.MultilayerPolylineSymbol;
import com.esri.arcgisruntime.symbology.Renderer;
import com.esri.arcgisruntime.symbology.SimpleFillSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleRenderer;
import com.esri.arcgisruntime.symbology.SolidStrokeSymbolLayer;
import com.esri.arcgisruntime.symbology.StrokeSymbolLayer;
import com.esri.arcgisruntime.symbology.SymbolLayer;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.toolkit.ar.ArLocationDataSource;
import com.esri.arcgisruntime.toolkit.ar.ArcGISArView;
import com.esri.arcgisruntime.toolkit.control.JoystickSeekBar;
import com.google.ar.core.Plane;

public class ARNavigateActivity extends AppCompatActivity {

  private static final String TAG = ARNavigateActivity.class.getSimpleName();

  private ArcGISArView mArView;

  private MobileScenePackage mMobileScenePackage;

  private TextView mHelpLabel;
  private View mCalibrationView;

  // public static RouteResult sRouteResult;
  public static Geometry sParcel;
  public static FeatureLayer exeFLayer;
  private ArcGISScene mScene;

  private boolean mIsCalibrating = false;
  private RouteTracker mRouteTracker;
  private TextToSpeech mTextToSpeech;

  private float mCurrentVerticalOffset;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_ar);

    // ensure at route has been set by the previous activity
    if (sParcel == null) {
      String error = "Parcel not set before launching activity!";
      Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
      Log.e(TAG, error);
    }

    requestPermissions();
  }

  private void navigateInAr() {
    // get a reference to the ar view
    mArView = findViewById(R.id.arView);
    mArView.registerLifecycle(getLifecycle());
    // disable touch interactions with the scene view
    mArView.getSceneView().setOnTouchListener((view, motionEvent) -> true);
    // create a scene and add it to the scene view
    mScene = new ArcGISScene(BasemapStyle.ARCGIS_IMAGERY);
    mArView.getSceneView().setScene(mScene);
    // create and add an elevation surface to the scene
    ArcGISTiledElevationSource elevationSource = new ArcGISTiledElevationSource(getString(R.string.elevation_url));
    Surface elevationSurface = new Surface();
    elevationSurface.getElevationSources().add(elevationSource);
    mArView.getSceneView().getScene().setBaseSurface(elevationSurface);
    // allow the user to navigate underneath the surface
    elevationSurface.setNavigationConstraint(NavigationConstraint.STAY_ABOVE);
    // hide the basemap. The image feed provides map context while navigating in AR
    elevationSurface.setOpacity(0f);
    // disable plane visualization. It is not useful for this AR scenario.
    mArView.getArSceneView().getPlaneRenderer().setEnabled(false);
    mArView.getArSceneView().getPlaneRenderer().setVisible(false);
    // add an ar location data source to update location
    mArView.setLocationDataSource(new ArLocationDataSource(this));

    // create and add a graphics overlay for showing the route line
    GraphicsOverlay routeOverlay = new GraphicsOverlay();
    mArView.getSceneView().getGraphicsOverlays().add(routeOverlay);
    Polygon polygon = (Polygon)sParcel;
    Polyline lines = polygon.toPolyline();
    Graphic parcelGraphic = new Graphic(lines);

    String type = parcelGraphic.getGeometry().getGeometryType().name();
    Log.d("MainAR","type is" +type);

    routeOverlay.getGraphics().add(parcelGraphic);

    // display the graphic 3 meters above the ground
    routeOverlay.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
    routeOverlay.getSceneProperties().setAltitudeOffset(1);

    // create a renderer for the route geometry
    SolidStrokeSymbolLayer strokeSymbolLayer = new SolidStrokeSymbolLayer(0.5, Color.GREEN, new LinkedList<>(),
        StrokeSymbolLayer.LineStyle3D.TUBE);
    strokeSymbolLayer.setCapStyle(StrokeSymbolLayer.CapStyle.ROUND);
    ArrayList<SymbolLayer> layers = new ArrayList<>();
    layers.add(strokeSymbolLayer);
    MultilayerPolylineSymbol polylineSymbol = new MultilayerPolylineSymbol(layers);
    SimpleRenderer polylineRenderer = new SimpleRenderer(polylineSymbol);
    routeOverlay.setRenderer(polylineRenderer);

    // create and add a graphics overlay for showing the route line

    final SimpleLineSymbol lineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.BLACK, 1.0f);
    final SimpleFillSymbol fillSymbol = new SimpleFillSymbol(SimpleFillSymbol.Style.SOLID, Color.WHITE, lineSymbol);
    SimpleRenderer renderer = new SimpleRenderer(fillSymbol);
    Renderer.SceneProperties renderProperties = renderer.getSceneProperties();
    renderProperties.setExtrusionMode(Renderer.SceneProperties.ExtrusionMode.BASE_HEIGHT);
    renderProperties.setExtrusionExpression("4");

    if (exeFLayer != null) {

      FeatureLayer copy = exeFLayer.copy();
      // add the feature layer to the scene
      mScene.getOperationalLayers().add(copy);

      copy.getSceneProperties().setSurfacePlacement(LayerSceneProperties.SurfacePlacement.RELATIVE);
      copy.getSceneProperties().setAltitudeOffset(0);

      copy.setRenderer(renderer);
    }


    // create and start a location data source for use with the route tracker
    AndroidLocationDataSource trackingLocationDataSource = new AndroidLocationDataSource(this);
    trackingLocationDataSource.addLocationChangedListener(locationChangedEvent -> {
      if (mRouteTracker != null) {
        // pass new location to the route tracker
        mRouteTracker.trackLocationAsync(locationChangedEvent.getLocation());
      }
    });
    trackingLocationDataSource.startAsync();


    // get references to the ui views defined in the layout
    mHelpLabel = findViewById(R.id.helpLabelTextView);
    mArView = findViewById(R.id.arView);
    mCalibrationView = findViewById(R.id.calibrationView);

    // show/hide calibration view
    Button calibrationButton = findViewById(R.id.calibrateButton);
    calibrationButton.setOnClickListener(v -> {
      // toggle calibration
      mIsCalibrating = !mIsCalibrating;
      if (mIsCalibrating) {
        mScene.getBaseSurface().setOpacity(0.5f);
        mCalibrationView.setVisibility(View.VISIBLE);
      } else {
        mScene.getBaseSurface().setOpacity(0f);
        mCalibrationView.setVisibility(View.GONE);
      }
    });

    // start navigation
    Button navigateButton = findViewById(R.id.navigateStartButton);
    // start turn-by-turn when the user is ready
    navigateButton.setOnClickListener(v -> Log.d("MainAR", "ScreenShot taken"));

    // wire up joystick seek bars to allow manual calibration of height and heading
    JoystickSeekBar headingJoystick = findViewById(R.id.headingJoystick);
    // listen for calibration value changes for heading
    headingJoystick.addDeltaProgressUpdatedListener(delta -> {
      // get the origin camera
      Camera camera = mArView.getOriginCamera();
      // add the heading delta to the existing camera heading
      double heading = camera.getHeading() + delta;
      // get a camera with a new heading
      Camera newCam = camera.rotateTo(heading, camera.getPitch(), camera.getRoll());
      // apply the new origin camera
      mArView.setOriginCamera(newCam);
    });
    JoystickSeekBar altitudeJoystick = findViewById(R.id.altitudeJoystick);
    // listen for calibration value changes for altitude
    altitudeJoystick.addDeltaProgressUpdatedListener(delta -> {
      mCurrentVerticalOffset += delta;
      // get the origin camera
      Camera camera = mArView.getOriginCamera();
      // elevate camera by the delta
      Camera newCam = camera.elevate(delta);
      // apply the new origin camera
      mArView.setOriginCamera(newCam);
    });
    // this step is handled on the back end anyways, but we're applying a vertical offset to every update as per the
    // calibration step above
    mArView.getLocationDataSource().addLocationChangedListener(locationChangedEvent -> {
      Point updatedLocation = locationChangedEvent.getLocation().getPosition();
      mArView.setOriginCamera(new Camera(
          new Point(updatedLocation.getX(), updatedLocation.getY(), updatedLocation.getZ() + mCurrentVerticalOffset),
          mArView.getOriginCamera().getHeading(), mArView.getOriginCamera().getPitch(),
          mArView.getOriginCamera().getRoll()));
    });

    // remind the user to calibrate the heading and altitude before starting navigation
    Toast.makeText(this, "Calibrate your heading and altitude before navigating!", Toast.LENGTH_LONG).show();
  }

  /**
   * Load the mobile scene package and get the first (and only) scene inside it. Set it to the ArView's SceneView and
   * set the base surface to opaque and remove any navigation constraint, thus allowing the user to look at a scene
   * from below. Then call updateTranslationFactorAndOriginCamera with the plane detected by ArCore.
   *
   */
  private void loadSceneFromPackage() {
    // create a mobile scene package from a path a local .mspk
    mMobileScenePackage = new MobileScenePackage(
            getExternalFilesDir(null) + "/mobilescene.mspk");
    // load the mobile scene package
    mMobileScenePackage.loadAsync();
    mMobileScenePackage.addDoneLoadingListener(() -> {
      // if it loaded successfully and the mobile scene package contains a scene
      if (mMobileScenePackage.getLoadStatus() == LoadStatus.LOADED && !mMobileScenePackage.getScenes()
              .isEmpty()) {
        // get a reference to the first scene in the mobile scene package, which is of a section of philadelphia
        ArcGISScene philadelphiaScene = mMobileScenePackage.getScenes().get(0);
        // add the scene to the AR view's scene view
        mArView.getSceneView().setScene(philadelphiaScene);
      } else {
        String error = "Failed to load mobile scene package: " + mMobileScenePackage.getLoadError()
                .getMessage();
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        Log.e(TAG, error);
      }
    });
  }

  /**
   * Request read external storage for API level 23+.
   */
  private void requestPermissions() {
    // define permission to request
    String[] reqPermission = { Manifest.permission.CAMERA };
    int requestCode = 2;
    if (ContextCompat.checkSelfPermission(this, reqPermission[0]) == PackageManager.PERMISSION_GRANTED) {
      navigateInAr();
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
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      navigateInAr();
    } else {
      // report to user that permission was denied
      Toast.makeText(this, getString(R.string.navigate_ar_permission_denied), Toast.LENGTH_SHORT).show();
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  protected void onPause() {
    if (mArView != null) {
      mArView.stopTracking();
    }
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mArView != null) {
      mArView.startTracking(ArcGISArView.ARLocationTrackingMode.CONTINUOUS);
    }
  }
}
