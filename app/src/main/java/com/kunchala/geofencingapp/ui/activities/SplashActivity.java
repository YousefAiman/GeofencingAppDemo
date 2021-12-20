package com.kunchala.geofencingapp.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.kunchala.geofencingapp.R;
import com.kunchala.geofencingapp.util.LocationRequester;

public class SplashActivity extends AppCompatActivity implements
        LocationRequester.LocationRequestAction {

    private LocationRequester locationRequester;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    LocationRequester locationRequester = new LocationRequester(this, this);
                    locationRequester.getCurrentLocation();

                } else {
                    Toast.makeText(this,
                            "You need to grant location access permission in order user the application"
                            , Toast.LENGTH_SHORT).show();
                }
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

//        throw new RuntimeException("Test Crash");

//        FirebaseAuth.getInstance().signOut();
        requestLocationPermission();

//        FirebaseFirestore.getInstance().collection("Users")
//                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
//            @Override
//            public void onSuccess(@NonNull QuerySnapshot queryDocumentSnapshots) {
//
//                for(DocumentSnapshot documentSnapshot:queryDocumentSnapshots){
//
//                    if(!documentSnapshot.contains("lat")){
//                        continue;
//                    }
//                    documentSnapshot.getReference().update("geohash",
//                            GeoFireUtils.getGeoHashForLocation(
//                                    new GeoLocation(
//                                            documentSnapshot.getDouble("lat"),
//                                            documentSnapshot.getDouble("lng")
//                                    )
//                            ));
//
//                }
//
//            }
//        });


    }


    @Override
    public void locationFetched(double lat, double lng) {

        Log.d("ttt", "lat: " + lat + " - lng: " + lng);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            auth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(@NonNull AuthResult authResult) {
                    Log.d("ttt", "splash  - lat: " + lat + " - lng: " + lng);
                    startActivity(new Intent(SplashActivity.this, UsernameActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra("lat", lat)
                            .putExtra("lng", lng));
                    finish();
                }
            });
        } else {
            startActivity(new Intent(SplashActivity.this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("lat", lat)
                    .putExtra("lng", lng));
            finish();
        }

    }

    @Override
    public void locationFetchFailed() {

    }

    @Override
    protected void onResume() {
        if (locationRequester != null) {
            locationRequester.resumeLocationUpdates();
        }
        super.onResume();
    }


    @Override
    protected void onPause() {
        if (locationRequester != null) {
            locationRequester.stopLocationUpdates();
        }
        super.onPause();
    }


    private void requestLocationPermission() {

        final String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, locationPermission)
                    == PackageManager.PERMISSION_GRANTED) {

                if (locationRequester == null) {
                    locationRequester = new LocationRequester(SplashActivity.this,
                            SplashActivity.this);
                }
                locationRequester.getCurrentLocation();
            } else {

                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        }

    }


}