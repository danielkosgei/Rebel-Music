package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.Manifest;

import androidx.core.view.GestureDetectorCompat;
import androidx.palette.graphics.Palette;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.ContentView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.color.DynamicColors;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.thenewkenya.Rebel.databinding.ActivityMainBinding;

import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NonNls;

import java.io.InputStream;
import java.security.SecureRandom;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener {
    // searchview
    private SearchView searchView;
    // List of all songs
    private List<MusicList> musicLists;

    private RecyclerView musicRecyclerView;

    private ExtendedFloatingActionButton shuffleButton;
    private TextView songCountText;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    private ProgressBar progressBar;

    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;


    private CardView bottomCardView;
    private ImageView albumArtImageView;

    private GestureDetectorCompat gestureDetector;

    private final Object mediaPlayerLock = new Object();

    // start here
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        searchView = findViewById(R.id.searchView);
        searchView.clearFocus();

        musicLists = new ArrayList<>();

        bottomCardView = findViewById(R.id.bottomCardView);
        albumArtImageView = findViewById(R.id.album_art);
        gestureDetector = new GestureDetectorCompat(this, new MyGestureListener(this));

        mediaPlayer = new MediaPlayer();
        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        shuffleButton = findViewById(R.id.shuffleButton);



        // Android 13+ requires specific storage permissions
        // Android 12 and earlier use READ_EXTERNAL_STORAGE for all.
        if (Utils.isTiramisu()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                checkReadStoragePermissions();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                checkReadStoragePermissions();
            }
        }

        // to be worked on later
        /*
        if (Utils.isTiramisu()) {
            ContextCompat.startForegroundService(
                    MainActivity.this.getApplicationContext(),
                    new Intent(MainActivity.this.getApplicationContext(), MediaSessionService.class));
        }*/





        shuffleButton.setOnClickListener(v -> {
            // shuffle logic

            shuffleSongs();
            startPlayingShuffledSongs();
        });

        bottomCardView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
        });



        // For the search
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            // we return false here unless we wanted a result only after submitting.
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            // To display search results everytime a new letter is input
            @Override
            public boolean onQueryTextChange(String newText) {
                filterList(newText);
                return true;
            }
        });


    }


    private void shuffleSongs() {
        Collections.shuffle(musicLists);
    }

    private int currentSongIndex = 0;

    private void startPlayingShuffledSongs() {
        if (musicLists.isEmpty()) {
            return;
        }
        MediaPlayer mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            currentSongIndex++;
            if (currentSongIndex < musicLists.size()) {
                playSong();
            } else {
                // All songs played
                mediaPlayer.release();
                currentSongIndex = 0; // resets index
            }
        });
        playSong();

        updateShuffleFABColor(musicLists.get(currentSongIndex));
    }

    private void playSong() {
        onChanged(currentSongIndex);
    }

    private void updateShuffleFABColor(MusicList musicList) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
        };

        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                @SuppressLint("Range") long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));

                Uri albumArtUri = ContentUris.withAppendedId(Uri.parse(getResources().getString(R.string.album_art_dir)), albumId);


            }
            cursor.close();
        }
        performColorExtraction(musicList.getAlbumArt());
    }

    private void filterList(String text) {
        List<MusicList> filteredList = new ArrayList<>();

        for(MusicList musicList : musicLists) {
            // to search by title and also to search in lowercase letters
            if (musicList.getTitle().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(musicList);
            }
            // To search by artist and also to search in lowercase letters
            if (musicList.getArtist().toLowerCase().contains(text.toLowerCase())) {
                filteredList.add(musicList);
            }

        }

        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No track found", Toast.LENGTH_SHORT).show();
        } else {

            musicAdapter.setFilteredList(filteredList);

        }

    }

    @SuppressLint("Range")
    private void getMusicFiles() {
        bottomCardView.setVisibility(View.INVISIBLE);
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,  // error from android side, it works < 29
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND (" +
                MediaStore.Audio.Media.DATA + " LIKE '%.mp3' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.flac')";
        String sortOrder = MediaStore.Audio.Media.DEFAULT_SORT_ORDER;

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null, sortOrder);
            
            if(cursor == null) {
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                return;
            } 
            
            if (!cursor.moveToFirst()) {
                Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
                return;
            }

            musicLists.clear(); // Clear existing list before adding new items
            
            do {
                int albumIdInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                
                final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String album = cursor.getString(albumInd);
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);

                long albumId = cursor.getLong(albumIdInd);
                Uri albumArt = ContentUris.withAppendedId(Uri.parse(getResources().getString(R.string.album_art_dir)), albumId);
                
                String getDuration = "00:00";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(
                    getMusicFileName != null ? getMusicFileName : "Unknown Title",
                    getArtistName != null ? getArtistName : "Unknown Artist",
                    getDuration,
                    false,
                    musicFileUri,
                    albumArt
                );
                musicLists.add(musicList);
                
            } while(cursor.moveToNext());

            if (shuffleButton != null) {
                shuffleButton.setText(String.valueOf(musicLists.size()));
            }

            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);
            if (musicRecyclerView != null) {
                musicRecyclerView.setAdapter(musicAdapter);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading music files", Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    void updateBottomCardView(MusicList musicList) {
        if (musicList == null || bottomCardView == null) {
            return;
        }

        bottomCardView.setVisibility(View.VISIBLE);

        ImageView album_art = bottomCardView.findViewById(R.id.album_art);
        TextView textViewTitle = bottomCardView.findViewById(R.id.textViewTitle);
        TextView textViewArtist = bottomCardView.findViewById(R.id.textViewArtist);
        ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);

        if (textViewTitle != null) {
            textViewTitle.setText(musicList.getTitle());
        }
        if (textViewArtist != null) {
            textViewArtist.setText(musicList.getArtist());
        }
        if (album_art != null) {
            Uri albumArtUri = musicList.getAlbumArt();
            if (albumArtUri != null) {
                album_art.setImageURI(albumArtUri);
                // Set a fallback for when the image fails to load
                if (album_art.getDrawable() == null) {
                    album_art.setImageResource(R.drawable.default_album_art);
                }
            } else {
                album_art.setImageResource(R.drawable.default_album_art);
            }
        }

        performColorExtraction(musicList.getAlbumArt());

        if (btn_play_pause != null) {
            btn_play_pause.setOnClickListener(view -> {
                if (isPlaying) {
                    pausePlayback();
                } else {
                    startPlayback();
                }
            });
        }
    }

    private void updateProgressBar() {
        if (mediaPlayer != null && progressBar != null) {
            try {
                int currentPosition = mediaPlayer.getCurrentPosition();
                progressBar.setProgress(currentPosition);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    Handler handler = new Handler();
    Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                updateProgressBar();
                handler.postDelayed(this, 100); // Update every 100ms for smoother progress
            }
        }
    };

    private void startUpdatingProgressBar() {
        handler.removeCallbacks(updateProgressRunnable);
        handler.post(updateProgressRunnable);
    }

    private void stopUpdatingProgressBar() {
        handler.removeCallbacks(updateProgressRunnable);
    }

    private void performColorExtraction(Uri albumArtUri) {
        if (albumArtUri == null) {
            // Use default colors if no album art
            if (shuffleButton != null) {
                shuffleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral_variant30)));
            }
            if (bottomCardView != null) {
                bottomCardView.setCardBackgroundColor(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral_variant20));
            }
            return;
        }

        try {
            InputStream inputStream = getContentResolver().openInputStream(albumArtUri);
            if (inputStream != null) {
                Bitmap albumArtBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (albumArtBitmap != null) {
                    Palette.from(albumArtBitmap).generate(palette -> {
                        if (palette != null) {
                            int defaultColor = ContextCompat.getColor(this, com.google.android.material.R.color.material_dynamic_neutral_variant0);
                            int darkMutedColor = palette.getDarkMutedColor(defaultColor);
                            int mutedColor = palette.getMutedColor(defaultColor);

                            if (shuffleButton != null) {
                                shuffleButton.setBackgroundTintList(ColorStateList.valueOf(mutedColor));
                            }
                            if (bottomCardView != null) {
                                bottomCardView.setCardBackgroundColor(darkMutedColor);
                            }
                        }
                    });
                }
            }
        } catch (IOException | SecurityException e) {
            e.printStackTrace();
            // Use default colors on error
            if (shuffleButton != null) {
                shuffleButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral_variant30)));
            }
            if (bottomCardView != null) {
                bottomCardView.setCardBackgroundColor(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral_variant20));
            }
        }
    }

    private void startPlayback() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null) {
                startUpdatingProgressBar();
                ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
                try {
                    mediaPlayer.start();
                    isPlaying = true;
                    btn_play_pause.setImageResource(R.drawable.pause_icon);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void pausePlayback() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null && isPlaying) {
                stopUpdatingProgressBar();
                ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
                try {
                    mediaPlayer.pause();
                    isPlaying = false;
                    btn_play_pause.setImageResource(R.drawable.play_icon);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error pausing playback", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }





    @Override
    public void onChanged(int position) {
        synchronized (mediaPlayerLock) {
            if (position < 0 || position >= musicLists.size()) {
                return;
            }

            ImageView btn_play_pause = bottomCardView.findViewById(R.id.btn_play_pause);
            startUpdatingProgressBar();
            btn_play_pause.setImageResource(R.drawable.pause_icon);

            MusicList musicList = musicLists.get(position);
            updateBottomCardView(musicList);

            progressBar = findViewById(R.id.media_player_bar_progress_indicator);
            currentSongListPosition = position;

            if (mediaPlayer != null) {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build());
                    mediaPlayer.setDataSource(MainActivity.this, musicList.getMusicFile());
                    mediaPlayer.prepareAsync();

                    mediaPlayer.setOnPreparedListener(mp -> {
                        if (progressBar != null) {
                            progressBar.setMax(mediaPlayer.getDuration());
                        }
                        isPlaying = true;
                        startPlayback();
                    });

                    mediaPlayer.setOnCompletionListener(mp -> {
                        synchronized (mediaPlayerLock) {
                            mediaPlayer.reset();
                            if (timer != null) {
                                timer.purge();
                                timer.cancel();
                            }
                            isPlaying = false;

                            int nextSongListPosition = currentSongListPosition + 1;
                            if (nextSongListPosition >= musicLists.size()) {
                                nextSongListPosition = 0;
                            }

                            musicLists.get(currentSongListPosition).setPlaying(false);
                            musicLists.get(nextSongListPosition).setPlaying(true);

                            if (musicAdapter != null) {
                                musicAdapter.updateList(musicLists);
                            }

                            if (musicRecyclerView != null) {
                                musicRecyclerView.scrollToPosition(nextSongListPosition);
                            }

                            onChanged(nextSongListPosition);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Player in invalid state", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Pass touch events to the gesture detector
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    void switchToPreviousSong() {
        mediaPlayer.reset();

        timer.purge();
        timer.cancel();

        isPlaying = false;



        int nextSongListPosition = currentSongListPosition-1;

        if(nextSongListPosition >= musicLists.size()) {
            nextSongListPosition = 0;
        } else if (nextSongListPosition == -1) {
            nextSongListPosition = musicLists.size()-1;
        }

        musicLists.get(currentSongListPosition).setPlaying(false);
        musicLists.get(nextSongListPosition).setPlaying(true);

        musicAdapter.updateList(musicLists);

        musicRecyclerView.scrollToPosition(nextSongListPosition);

        onChanged(nextSongListPosition);
    }

    void switchToNextSong() {
        mediaPlayer.reset();

        timer.purge();
        timer.cancel();

        isPlaying = false;



        int nextSongListPosition = currentSongListPosition+1;

        if(nextSongListPosition >= musicLists.size()) {
            nextSongListPosition = 0;
        }

        musicLists.get(currentSongListPosition).setPlaying(false);
        musicLists.get(nextSongListPosition).setPlaying(true);

        musicAdapter.updateList(musicLists);

        musicRecyclerView.scrollToPosition(nextSongListPosition);

        onChanged(nextSongListPosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }
        stopUpdatingProgressBar();
        handler.removeCallbacksAndMessages(null);
    }






    // PERMISSIONS ARE ALL BELOW

    private void checkReadStoragePermissions() {

        if (Utils.isTiramisu()) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionRationale();
            } else {
                onPermissionGranted();
            }
        } else if (Utils.isMarshmallow()) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionRationale();
            } else {
                onPermissionGranted();
            }
        } else {
            onPermissionGranted();
        }


    }

    private void permissionRationale() {
        Intent intent = new Intent(this, PermissionRationale.class);
        startActivity(intent);
    }






    @Override
    public void onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            permissionRationale();
        } else {
            onPermissionGranted();
        }
    }

    // needs modification
    private void onPermissionGranted() {

        getMusicFiles();

    }




}