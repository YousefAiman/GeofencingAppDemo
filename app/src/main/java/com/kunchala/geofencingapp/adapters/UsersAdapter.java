package com.kunchala.geofencingapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kunchala.geofencingapp.R;
import com.kunchala.geofencingapp.model.User;

import java.util.ArrayList;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserVH> {

    private final ArrayList<User> users;

    public UsersAdapter(ArrayList<User> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public UserVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserVH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserVH holder, int position) {
        holder.bind(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserVH extends RecyclerView.ViewHolder {

        private final TextView tvUsername, tvLatLng;

        public UserVH(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvLatLng = itemView.findViewById(R.id.tvLatLng);
        }

        public void bind(User user) {

            tvUsername.setText(user.getUsername());


            tvLatLng.setText(String.format("%.2f", user.getDistanceAwayInMeters() / 1000) + " KM");
//            tvLatLng.setText("[" + user.getLat() +"," + user.getLng() + "]");
        }

    }

}
