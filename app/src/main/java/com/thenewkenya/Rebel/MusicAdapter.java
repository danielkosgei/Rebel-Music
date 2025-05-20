package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MyViewHolder> {

    private List<MusicList> list;
    private List<MusicList> songList = new ArrayList<>();
    private List<MusicList> filteredList = new ArrayList<>();
    private final Context context;
    private int playingPosition = 0;
    private final SongChangeListener songChangeListener;

    public void setSongList(List<MusicList> songList) {
        this.songList = songList;
        notifyDataSetChanged();
    }

    public void setFilteredList(List<MusicList> filteredList) {
        if (filteredList == null) {
            return;
        }
        this.list = new ArrayList<>(filteredList);
        notifyDataSetChanged();
    }

    public MusicAdapter(List<MusicList> list, Context context) {
        this.list = list;
        this.context = context;
        this.songChangeListener = ((SongChangeListener) context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.music_adapter_layout, null), songChangeListener);
    }

    @NonNull
    @Override
    public void onBindViewHolder(@NonNull MusicAdapter.MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (holder == null || position < 0 || position >= list.size()) {
            return;
        }

        MusicList musicList = list.get(position);
        if (musicList == null) {
            return;
        }

        if (musicList.isPlaying()) {
            playingPosition = position;
        }

        try {
            String duration = musicList.getDuration();
            long durationMs = duration != null ? Long.parseLong(duration) : 0;
            String formattedDuration = String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(durationMs),
                TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs)));

            holder.title.setText(musicList.getTitle());
            holder.artist.setText(musicList.getArtist());
            holder.musicDuration.setText(formattedDuration);
            
            // Handle album art loading with fallback
            Uri albumArtUri = musicList.getAlbumArt();
            if (albumArtUri != null) {
                holder.albumArt.setImageURI(albumArtUri);
                // Set fallback if the image fails to load
                if (holder.albumArt.getDrawable() == null) {
                    holder.albumArt.setImageResource(R.drawable.default_album_art);
                }
            } else {
                holder.albumArt.setImageResource(R.drawable.default_album_art);
            }

            holder.rootLayout.setOnClickListener(v -> {
                if (playingPosition != -1 && playingPosition < list.size()) {
                    list.get(playingPosition).setPlaying(false);
                }
                musicList.setPlaying(true);
                playingPosition = position;

                if (songChangeListener != null) {
                    songChangeListener.onChanged(position);
                }

                notifyDataSetChanged();
            });
        } catch (NumberFormatException e) {
            e.printStackTrace();
            holder.musicDuration.setText("00:00");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateList(List<MusicList> newList) {
        if (newList == null) {
            return;
        }
        this.list = new ArrayList<>(newList); // Create a new copy to avoid concurrent modification
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {

        private final RelativeLayout rootLayout;
        private final TextView title;
        private final TextView artist;
        private final ImageView albumArt;
        private final TextView musicDuration;

        public MyViewHolder(@NonNull View itemView, SongChangeListener songChangeListener) {
            super(itemView);

            rootLayout = itemView.findViewById(R.id.rootLayout);
            title = itemView.findViewById(R.id.musicTitle);
            artist = itemView.findViewById(R.id.musicArtist);
            musicDuration = itemView.findViewById(R.id.musicDuration);
            albumArt = itemView.findViewById(R.id.albumArt);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    songChangeListener.onChanged(getAdapterPosition());
                }
            });
        }
    }
}
