package com.ofekcohen.chordof.Adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseUser;
import com.ofekcohen.chordof.Activities.ChordViewerActivity;
import com.ofekcohen.chordof.Activities.UserActivity;
import com.ofekcohen.chordof.Classes.Song;
import com.ofekcohen.chordof.Classes.User;
import com.ofekcohen.chordof.Core.Constants;
import com.ofekcohen.chordof.R;

import java.util.ArrayList;

import static com.ofekcohen.chordof.Core.Constants.SONG_REF_EXTRA_TAG;
import static com.ofekcohen.chordof.Core.Constants.TAG;
import static com.ofekcohen.chordof.Core.Constants.USER_ACTIVITY_UID_EXTRA_TAG;

public class HallOfFameRecyclerViewAdapter extends RecyclerView.Adapter<HallOfFameRecyclerViewAdapter.ViewHolder> {

    private ArrayList<User> users;
    private Context context;

    public HallOfFameRecyclerViewAdapter(ArrayList<User> users, Context context) {
        this.users = users;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_song_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvUserName.setText(users.get(position).getName());
        holder.tvUploadsCount.setText(users.get(position).getUploadsCount() + "");
        Glide.with(context).load(users.get(position).getPhotoUrl()).into(holder.imgBackground);
    }

    @Override
    public int getItemCount() {
        if (users == null)
            return 0;
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUploadsCount;
        ImageView imgBackground;

        public ViewHolder(View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvSongName);
            tvUploadsCount = itemView.findViewById(R.id.tvSingerName);
            imgBackground = itemView.findViewById(R.id.imgBackground);

            // On user click
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, UserActivity.class);
                    intent.putExtra(USER_ACTIVITY_UID_EXTRA_TAG, users.get(getAdapterPosition()).getUid());
                    context.startActivity(intent);
                }
            });
        }
    }
}
