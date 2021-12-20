package com.kunchala.geofencingapp.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kunchala.geofencingapp.adapters.UsersAdapter;
import com.kunchala.geofencingapp.databinding.ActivityMainBinding;
import com.kunchala.geofencingapp.model.User;
import com.kunchala.geofencingapp.util.LocationRequester;
import com.kunchala.geofencingapp.viewmodels.UserViewModel;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements
        LocationRequester.LocationRequestAction,
        SwipeRefreshLayout.OnRefreshListener {

    //location
    private LocationRequester locationRequester;
    private double currentLat, currentLng;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    LocationRequester locationRequester = new LocationRequester(MainActivity.this,
                            MainActivity.this);
                    locationRequester.getCurrentLocation();

                } else {
                    Toast.makeText(this,
                            "You need to grant location access permission in order post your location"
                            , Toast.LENGTH_SHORT).show();
                }
            });

    //view
    private ActivityMainBinding binding;

    //Firebase
    private DocumentReference currentUserRef;
    private FirebaseAuth auth;

    //current user
    private FirebaseUser user;
//    private boolean userExists;

    //users
    private CollectionReference userRef;
    private UserViewModel userViewModel;
    private UsersAdapter usersAdapter;
    private ArrayList<User> users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        userRef = FirebaseFirestore.getInstance().collection("Users");

        Intent intent;
        if ((intent = getIntent()) != null) {
            currentLat = intent.getDoubleExtra("lat", 0);
            currentLng = intent.getDoubleExtra("lng", 0);
        }


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        if (user == null) {
            signUserInAnon();
        } else {
            currentUserRef = userRef.document(user.getUid());
        }


        users = new ArrayList<>();
        usersAdapter = new UsersAdapter(users);
        binding.rvUsers.setAdapter(usersAdapter);

        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);

        userViewModel.getFurthestDistanceFrom(currentLat, currentLng)
                .observe(this, new Observer<Double>() {
                    @Override
                    public void onChanged(Double aDouble) {
                        if (aDouble != null) {
                            if (aDouble.floatValue() > binding.slRadius.getValueTo()) {
                                binding.slRadius.setValueTo(aDouble.floatValue());
                            }
//                            binding.tvMaxRadius.setText(String.valueOf(aDouble.floatValue()));
                        }
                    }
                });

        userViewModel.getErrorLiveData().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                Toast.makeText(MainActivity.this, "An error occurred while fetching users!" +
                        "Please check your internet connection and try again", Toast.LENGTH_LONG).show();
                Log.d("ttt", s);
            }
        });

        userViewModel.getIsFetchingLiveData().observe(this,
                aBoolean -> binding.srlUsers.setRefreshing(aBoolean));

//        GeoLocation currentLocation = new GeoLocation(currentLat,currentLng);

        userViewModel.getUsersLiveData()
                .observe(this, fetchedUsers -> {
                    if (fetchedUsers != null && !fetchedUsers.isEmpty()) {
                        users.addAll(fetchedUsers);
                        usersAdapter.notifyItemRangeInserted(0, users.size());
                    }
                });


        if (intent == null && currentLat == 0 && currentLng == 0) {
            requestLocationPermission();
        } else {
            if (userViewModel.getUsersLiveData().getValue() == null) {
                userViewModel.getUsersInRadius(currentLat, currentLng, binding.slRadius.getValue());
            }
        }

//        binding.slRadius.r
        binding.slRadius.setOnTouchListener(new View.OnTouchListener() {
            float previousValue = binding.slRadius.getValue();

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("ttt", "event.getAction(): " + event.getAction());
                if (event.getAction() == MotionEvent.ACTION_UP &&
                        (binding.slRadius.getValue()) != previousValue) {
                    if (userViewModel.getIsFetchingLiveData().getValue()) {
                        userViewModel.getIsFetchingLiveData().observe(MainActivity.this, new Observer<Boolean>() {
                            @Override
                            public void onChanged(Boolean aBoolean) {
                                if (!aBoolean) {
                                    userViewModel.getIsFetchingLiveData().removeObserver(this);
                                    onRefresh();
                                }
                            }
                        });
                    } else {
                        onRefresh();
                    }
                    previousValue = binding.slRadius.getValue();
                }
                return false;
            }
        });

        binding.srlUsers.setOnRefreshListener(this);

        binding.btnPostLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                changePostBtnState(false);

                requestLocationPermission();
            }
        });


    }

    private void changePostBtnState(boolean isEnabled) {
        binding.btnPostLocation.setClickable(isEnabled);
        binding.btnPostLocation.setEnabled(isEnabled);
    }


    private void requestLocationPermission() {

        final String locationPermission = Manifest.permission.ACCESS_FINE_LOCATION;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, locationPermission)
                    == PackageManager.PERMISSION_GRANTED) {

                if (locationRequester == null) {
                    locationRequester = new LocationRequester(MainActivity.this,
                            MainActivity.this);
                }
                locationRequester.getCurrentLocation();
            } else {

                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        } else {

            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

        }

    }

    private void signUserInAnon() {
        auth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(@NonNull AuthResult authResult) {
                user = auth.getCurrentUser();

                if (user != null) {
                    currentUserRef = userRef.document(user.getUid());
                }

            }
        });

    }

    @Override
    public void locationFetched(double lat, double lng) {

        currentLat = lat;
        currentLng = lng;

        if (user == null) {
            auth.signInAnonymously().addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(@NonNull AuthResult authResult) {
                    user = auth.getCurrentUser();
                    if (user != null) {
                        updateUsersFirebaseLoc(lat, lng);
                    }
                }
            });

        } else {
            updateUsersFirebaseLoc(lat, lng);
        }

    }

    @Override
    public void locationFetchFailed() {
        Toast.makeText(MainActivity.this,
                "Location Fetch Failed! Please check that Gps is on and the location permissions are granted",
                Toast.LENGTH_LONG).show();
    }

    private void updateUsersFirebaseLoc(double lat, double lng) {

        HashMap<String, Object> locationUpdateMap = new HashMap<>();
        final String hash = GeoFireUtils.getGeoHashForLocation(
                new GeoLocation(lat, lng));

        locationUpdateMap.put("geohash", hash);
        locationUpdateMap.put("lat", lat);
        locationUpdateMap.put("lng", lng);

        Log.d("ttt", currentUserRef.getId());

//        GeoFirestore firestore = GeoFirestore(userRef);
//        firestore.setLocation(currentUserRef.getId(),new GeoPoint())

        currentUserRef.update(locationUpdateMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        changePostBtnState(true);
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(@NonNull Void unused) {

                userViewModel.getFurthestDistanceFrom(currentLat, currentLng)
                        .observe(MainActivity.this, new Observer<Double>() {
                            @Override
                            public void onChanged(Double aDouble) {
                                if (aDouble != null) {
                                    if (aDouble > binding.slRadius.getValueFrom()) {

//                                                binding.slRadius.setValue(1);
                                        if (aDouble < binding.slRadius.getValueTo() &&
                                                binding.slRadius.getValue() == binding.slRadius.getValueTo()) {
                                            binding.slRadius.setValue(aDouble.floatValue());
                                        }

                                        binding.slRadius.setValueTo(aDouble.floatValue());

                                    }
                                    onRefresh();
                                }
                            }
                        });

                Toast.makeText(MainActivity.this, "Location Updated!",
                        Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Location Update Failed!" +
                        " Please check your internet connection and try again", Toast.LENGTH_LONG).show();
            }
        });


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

    @Override
    public void onRefresh() {

        if (!users.isEmpty()) {
            users.clear();
            usersAdapter.notifyDataSetChanged();
        }

        userViewModel.refreshItems(currentLat, currentLng, binding.slRadius.getValue());
    }


}