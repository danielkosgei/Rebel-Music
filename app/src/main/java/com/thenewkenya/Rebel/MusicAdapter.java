package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_TRENDING = 0;
    private static final int VIEW_TYPE_REGULAR = 1;

    private List<MusicList> list;
    private final Context context;
    private int playingPosition = 0;
    private final SongChangeListener songChangeListener;
    private final boolean isTrendingAdapter;

    public MusicAdapter(List<MusicList> list, Context context, boolean isTrendingAdapter) {
        this.list = list;
        this.context = context;
        this.songChangeListener = ((SongChangeListener) context);
        this.isTrendingAdapter = isTrendingAdapter;
    }

    @Override
    public int getItemViewType(int position) {
        return isTrendingAdapter ? VIEW_TYPE_TRENDING : VIEW_TYPE_REGULAR;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_TRENDING) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trending_music, parent, false);
            return new TrendingViewHolder(view, songChangeListener);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_adapter_layout, parent, false);
            return new RegularViewHolder(view, songChangeListener);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        if (position < 0 || position >= list.size()) return;

        MusicList musicList = list.get(position);
        if (musicList == null) return;

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

            if (holder instanceof TrendingViewHolder) {
                bindTrendingViewHolder((TrendingViewHolder) holder, musicList, position);
            } else if (holder instanceof RegularViewHolder) {
                bindRegularViewHolder((RegularViewHolder) holder, musicList, formattedDuration, position);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindTrendingViewHolder(TrendingViewHolder holder, MusicList musicList, int position) {
        holder.title.setText(musicList.getTitle());
        holder.artist.setText(musicList.getArtist());
        
        Uri albumArtUri = musicList.getAlbumArt();
        if (albumArtUri != null) {
            holder.albumArt.setImageURI(albumArtUri);
            if (holder.albumArt.getDrawable() == null) {
                holder.albumArt.setImageResource(R.drawable.default_album_art);
            }
        } else {
            holder.albumArt.setImageResource(R.drawable.default_album_art);
        }

        holder.playButton.setImageResource(musicList.isPlaying() ? R.drawable.pause_icon : R.drawable.play_icon);
        holder.playButton.setOnClickListener(v -> {
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
    }

    private void bindRegularViewHolder(RegularViewHolder holder, MusicList musicList, String formattedDuration, int position) {
        holder.title.setText(musicList.getTitle());
        holder.artist.setText(musicList.getArtist());
        holder.musicDuration.setText(formattedDuration);
        
        Uri albumArtUri = musicList.getAlbumArt();
        if (albumArtUri != null) {
            holder.albumArt.setImageURI(albumArtUri);
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
    }

    public void updateList(List<MusicList> newList) {
        if (newList == null) return;
        this.list = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return list != null ? list.size() : 0;
    }

    static class TrendingViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView rootLayout;
        private final TextView title;
        private final TextView artist;
        private final ImageView albumArt;
        private final FloatingActionButton playButton;

        public TrendingViewHolder(@NonNull View itemView, SongChangeListener songChangeListener) {
            super(itemView);
            rootLayout = itemView.findViewById(R.id.rootLayout);
            title = itemView.findViewById(R.id.musicTitle);
            artist = itemView.findViewById(R.id.musicArtist);
            albumArt = itemView.findViewById(R.id.albumArt);
            playButton = itemView.findViewById(R.id.playButton);
        }
    }

    static class RegularViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView rootLayout;
        private final TextView title;
        private final TextView artist;
        private final ImageView albumArt;
        private final TextView musicDuration;

        public RegularViewHolder(@NonNull View itemView, SongChangeListener songChangeListener) {
            super(itemView);
            rootLayout = itemView.findViewById(R.id.rootLayout);
            title = itemView.findViewById(R.id.musicTitle);
            artist = itemView.findViewById(R.id.musicArtist);
            musicDuration = itemView.findViewById(R.id.musicDuration);
            albumArt = itemView.findViewById(R.id.albumArt);
        }
    }
}
