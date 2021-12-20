package com.kunchala.geofencingapp.viewmodels;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.kunchala.geofencingapp.model.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UserViewModel extends ViewModel {

    //    public static final long USER_LIMIT = 10;
    private final MutableLiveData<List<User>> usersLiveData;
    private final MutableLiveData<String> errorLiveData;
    private final MutableLiveData<Boolean> isFetchingLiveData;


    private CollectionReference userRef;

    public LiveData<Boolean> getIsFetchingLiveData() {
        return isFetchingLiveData;
    }

    public LiveData<String> getErrorLiveData() {
        return errorLiveData;
    }

    private String currentUid;


    public LiveData<List<User>> getUsersLiveData() {
        return usersLiveData;
    }

    private List<Task<QuerySnapshot>> tasks;
    private boolean cancelPreviousTasks;

    public UserViewModel() {
        usersLiveData = new MutableLiveData<>();
        isFetchingLiveData = new MutableLiveData<>();
        errorLiveData = new MutableLiveData<>();
        FirebaseUser user;
        if ((user = FirebaseAuth.getInstance().getCurrentUser()) != null) {
            currentUid = user.getUid();
        }
    }

    public void getUsersInRadius(double usersLat, double usersLng, float radius) {

        Log.d("ttt", "usersLat: " + usersLat + " - usersLng" + usersLng);
        isFetchingLiveData.setValue(true);

        GeoLocation center = new GeoLocation(usersLat, usersLng);

        List<GeoQueryBounds> geoQueryBounds = GeoFireUtils.getGeoHashQueryBounds(center, radius * 1000);
        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        for (GeoQueryBounds b : geoQueryBounds) {
            Query query = getUserRef().orderBy("geohash").startAt(b.startHash).endAt(b.endHash);
            tasks.add(query.get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
            @Override
            public void onComplete(@NonNull Task<List<Task<?>>> task) {

                List<User> users = new ArrayList<>();

                for (Task<QuerySnapshot> userTask : tasks) {

                    if (userTask.isSuccessful() && userTask.getResult() != null && !userTask.getResult().isEmpty()) {

                        List<User> users2 = userTask.getResult().toObjects(User.class);

                        for (User user : users2) {

                            if (user.getID().equals(currentUid)) {
                                continue;
                            }

                            double distance = GeoFireUtils.getDistanceBetween(center,
                                    new GeoLocation(user.getLat(), user.getLng()));

                            if (distance <= radius * 1000) {
                                user.setDistanceAwayInMeters(distance);
                                users.add(user);
                            }
                        }
                        users2 = null;
                    }
                }

                tasks.clear();

                Collections.sort(users, new Comparator<User>() {
                    @Override
                    public int compare(User o1, User o2) {
                        return (int) (GeoFireUtils.getDistanceBetween(center,
                                new GeoLocation(o1.getLat(),
                                        o1.getLng())) -
                                GeoFireUtils.getDistanceBetween(center,
                                        new GeoLocation(o2.getLat(),
                                                o2.getLng())));
                    }
                });

                usersLiveData.setValue(users);
                Log.d("ttt", "users: " + users.size());

                isFetchingLiveData.setValue(false);
//            }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                isFetchingLiveData.setValue(false);
                errorLiveData.setValue(e.getMessage());
                isFetchingLiveData.setValue(false);
                Log.d("ttt", "failed to get geo task: " + e.getMessage());
            }
        });
    }


    public LiveData<Double> getFurthestDistanceFrom(double lat, double lng) {

        //live data for async task
        MutableLiveData<Double> liveData = new MutableLiveData<>();

        GeoLocation center = new GeoLocation(lat, lng);

        //we query based on maximum supported distance in the library
        List<GeoQueryBounds> geoQueryBounds = GeoFireUtils.getGeoHashQueryBounds(center, 8587000);

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        //furthest document could be he first or last in query so we fetch them both
        for (GeoQueryBounds b : geoQueryBounds) {
            Query query = getUserRef()
                    .orderBy("geohash").startAt(b.startHash).endAt(b.endHash)
                    .limit(1);
            tasks.add(query.get());
        }

        for (GeoQueryBounds b : geoQueryBounds) {
            Query query = getUserRef()
                    .orderBy("geohash").startAt(b.startHash).endAt(b.endHash)
                    .limitToLast(1);
            tasks.add(query.get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
            @Override
            public void onComplete(@NonNull Task<List<Task<?>>> task) {

                Log.d("ttt", " Tasks.whenAllComplete(tasks)");
                double maxDistance = 0;


                for (Task<QuerySnapshot> querySnapshotTask : tasks) {
                    for (DocumentSnapshot document : querySnapshotTask.getResult().getDocuments()) {

                        double distance = GeoFireUtils.getDistanceBetween(center,
                                new GeoLocation(document.getDouble("lat"),
                                        document.getDouble("lng")));

                        if (distance > maxDistance) {
                            maxDistance = distance;
                        }

                        Log.d("distance", String.valueOf(distance));
                    }
                }

                liveData.setValue((new BigDecimal(maxDistance)
                        .setScale(4, BigDecimal.ROUND_UP).intValue() / 1000) + 1d);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                liveData.setValue(null);
            }
        });

        return liveData;
    }

    public void refreshItems(double lat, double lng, float radius) {
//        lastDocSnap = null;
        isFetchingLiveData.setValue(true);
        usersLiveData.setValue(null);

        cancelPreviousTasks = tasks != null && !tasks.isEmpty();

        getUsersInRadius(lat, lng, radius);
    }

    public CollectionReference getUserRef() {
        if (userRef == null) {
            userRef = FirebaseFirestore.getInstance().collection("Users");
        }

        return userRef;
    }

//    public LiveData<List<User>> fetchUsersInRadius(long radiusInMeters,double lat,double lng){
//
//        //using live data for Firebase async task
//        MutableLiveData<List<User>> userMutableLiveData = new MutableLiveData<>();
//
//        GeoLocation center = new GeoLocation(lat,lng);
//
//        tasks = generateTasksForRadius(center,radiusInMeters);
//
//        //wait for the completion of all tasks
//        Tasks.whenAllComplete()
//                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
//            @Override
//            public void onComplete(@NonNull Task<List<Task<?>>> task) {
//
//                //creating a list to hold the fetched users
//                List<User> users = new ArrayList<>();
//
//                for (Task<QuerySnapshot> userTask : tasks) {
//
//                    if (userTask.isSuccessful() && userTask.getResult() != null &&
//                            !userTask.getResult().isEmpty()) {
//                        List<User> filteredUsers = filterUsersForRadius(
//                                userTask.getResult().toObjects(User.class),
//                                radiusInMeters, center);
//
//                        users.addAll(filteredUsers);
//                    }
//                }
//
//                tasks.clear();
//
//                //setting users as the mutable data value
//                userMutableLiveData.setValue(users);
//            }
//        });
//
//        return userMutableLiveData;
//    }
//
//    private List<Task<QuerySnapshot>> generateTasksForRadius(GeoLocation center,long radiusInMeters){
//
//        //create a list of tasks and our query tasks to it
//        if(tasks == null){
//            tasks = new ArrayList<>();
//        }
//
//        //looping on list of generated bounds that we will query based on
//        for (GeoQueryBounds b : GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)) {
//            //query that filters the users based on your specified radius
//            Query query = getUserRef().orderBy("geohash").startAt(b.startHash).endAt(b.endHash);
//            tasks.add(query.get());
//        }
//        return tasks;
//    }
//
//    private List<User> filterUsersForRadius(List<User> users,long radiusInMeters,GeoLocation center){
//
//        List<User> filteredUsers = new ArrayList<>();
//
//        for (User user : users) {
//
//            double distance = GeoFireUtils.getDistanceBetween(center,
//                    new GeoLocation(user.getLat(), user.getLng()));
//
//            if (distance <= radiusInMeters) {
//                user.setDistanceAwayInMeters(distance);
//                filteredUsers.add(user);
//            }
//        }
//
//        return filteredUsers;
//    }
}
