package toptierlabs.sampleapp;

import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class DBoardActivity extends FragmentActivity implements OnMapReadyCallback,
        OnConnectionFailedListener, ConnectionCallbacks {

    private static final String MTAG = "DBoardActivity";
    public static final int INIT_ZOOM = 13;

    private GoogleMap mMap;
    private GoogleApiClient gapiClient;
    private Location mLoc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dboard);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Click events in compat image views
        AppCompatImageView profile = (AppCompatImageView) findViewById(R.id.profileIcon);
        profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { goProfile(view); }
        });

        // connect google api client
        // handle connection events in this activity
        if (gapiClient == null) {
            gapiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API) // the location api
                .build();
        }
    }

    @Override
    protected void onStart() {
        gapiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        gapiClient.disconnect();
        super.onStop();
    }

    /**
     * Triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // enable the my location' button
        //mMap.setMyLocationEnabled(true);

        if (mLoc != null)
            setLocInMap();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // TODO: Google Client API couldn't connect
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        try {
            mLoc = LocationServices.FusedLocationApi.getLastLocation(gapiClient);
        }
        catch (SecurityException ex) {
            // TODO: there's no permission to get the user location
        }

        if (mMap != null && mLoc != null)
            setLocInMap();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    /**
     * once map is ready and location received
     */
    private void setLocInMap() {
        LatLng where = new LatLng(mLoc.getLatitude(), mLoc.getLongitude());

        // create the marker for current position
        mMap.addMarker(new MarkerOptions()
            .position(where)
            .title(getString(R.string.dboard_mloc))
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
        );

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(where, INIT_ZOOM));
    }

    /**
     * Go tho the user profile screen
     */
    public void goProfile(View view) {
        Intent intent = new Intent(this, ProfileAct.class);
        startActivity(intent);
    }

    /**
     * Click on new target
     * Show the Topics fragment
     */
    public void clickNewTarget(View view) {
        TopicsFragment frg = new TopicsFragment();

        View nTargetView = findViewById(R.id.nTargetContainer);
        nTargetView.setVisibility(View.VISIBLE);

        // force the height
        int h = Resources.getSystem().getDisplayMetrics().heightPixels / 2;
        Log.v(MTAG, "height: " + h);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, h);
        params.setMargins(0, h, 0, 0); // also margin of the view
        nTargetView.setLayoutParams(params);

        getSupportFragmentManager()
            .beginTransaction()
            .setCustomAnimations(R.anim.topics_in, R.anim.topics_out)
            .add(R.id.nTargetContainer, frg)
            .commit();
    }

}
