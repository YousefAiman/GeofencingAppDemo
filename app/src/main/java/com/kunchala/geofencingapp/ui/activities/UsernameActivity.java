package com.kunchala.geofencingapp.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.kunchala.geofencingapp.databinding.ActivityUsernameBinding;

import java.util.HashMap;

public class UsernameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUsernameBinding binding = ActivityUsernameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent;
        if ((intent = getIntent()) != null) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);

            binding.btnContinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String name = binding.tiedName.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(UsernameActivity.this, "Please Enter your name!", Toast.LENGTH_SHORT).show();
                    } else {

                        binding.btnContinue.setEnabled(false);

                        FirebaseUser user;
                        if ((user = FirebaseAuth.getInstance().getCurrentUser()) != null) {

                            HashMap<String, Object> userMap = new HashMap<>();
                            userMap.put("ID", user.getUid());
                            userMap.put("username", name);


                            FirebaseFirestore.getInstance().collection("Users")
                                    .document(user.getUid())
                                    .set(userMap)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(@NonNull Void unused) {
                                            startActivity(new Intent(UsernameActivity.this, MainActivity.class)
                                                    .putExtra("lat", lat)
                                                    .putExtra("lng", lng)
                                                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                                            finish();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    binding.btnContinue.setEnabled(true);
                                    Toast.makeText(UsernameActivity.this,
                                            "An error occurred! Please try again", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                    }
                }
            });
        }


    }


}