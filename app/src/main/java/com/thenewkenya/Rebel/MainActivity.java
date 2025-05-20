package com.thenewkenya.Rebel;

import android.annotation.SuppressLint;
import android.Manifest;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.content.Context;

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
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.ContentView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.material.slider.Slider;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
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

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import android.widget.ImageButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.viewpager2.widget.ViewPager2;

public class MainActivity extends AppCompatActivity implements SongChangeListener {
    // List of all songs
    private List<MusicList> musicLists;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private AudioManager audioManager;
    private AudioAttributes audioAttributes;
    private AudioFocusRequest audioFocusRequest;

    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;

    private CardView bottomCardView;
    private ImageView albumArtImageView;
    private GestureDetectorCompat gestureDetector;
    private final Object mediaPlayerLock = new Object();

    private View playerBottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private ImageView expandedAlbumArt;
    private TextView expandedTitle, expandedArtist, currentTime, totalTime;
    private ImageButton previousButton, nextButton, repeatButton;
    private FloatingActionButton playPauseButtonExpanded;
    private Slider progressSlider;
    private BottomNavigationView bottomNavigation;
    private LinearLayout controlsContainer;

    private View miniPlayerLayout;
    private View expandedLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DynamicColors.applyToActivitiesIfAvailable(getApplication());
        com.thenewkenya.Rebel.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize views first
        initializeViews();
        
        // Then check permissions and load music
        checkReadStoragePermissions();
    }

    private void initializeViews() {
        musicLists = new ArrayList<>();

        // Initialize views
        miniPlayerLayout = findViewById(R.id.miniPlayerLayout);
        expandedLayout = findViewById(R.id.expandedLayout);
        playerBottomSheet = findViewById(R.id.playerBottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(playerBottomSheet);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Initialize expanded player views
        expandedAlbumArt = findViewById(R.id.expandedAlbumArt);
        expandedTitle = findViewById(R.id.expandedTitle);
        expandedArtist = findViewById(R.id.expandedArtist);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        repeatButton = findViewById(R.id.repeatButton);
        playPauseButtonExpanded = findViewById(R.id.playPauseButtonExpanded);
        progressSlider = findViewById(R.id.progressSlider);
        controlsContainer = findViewById(R.id.controlsContainer);

        // Set up ViewPager2 and fragments BEFORE loading music
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        DiscoverFragment discoverFragment = new DiscoverFragment();
        adapter.addFragment(discoverFragment);
        viewPager.setAdapter(adapter);

        // Set up expanded player controls
        setupPlayerControls();
        setupBottomSheet();
        setupAudioSystem();
    }

    private void checkReadStoragePermissions() {
        Log.d("MusicPlayer", "Checking storage permissions...");
        boolean hasPermission = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                Log.d("MusicPlayer", "Requesting READ_MEDIA_AUDIO permission for Android 13+");
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_AUDIO}, 101);
            }
        } else {
            hasPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!hasPermission) {
                Log.d("MusicPlayer", "Requesting READ_EXTERNAL_STORAGE permission for Android 12 and below");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }

        if (hasPermission) {
            Log.d("MusicPlayer", "Permission already granted, loading music files");
            // Add a small delay to ensure fragment is properly initialized
            new Handler().postDelayed(() -> {
                getMusicFiles();
            }, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check permissions and reload music files if needed
        boolean hasPermission = false;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission = checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
        } else {
            hasPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        
        if (hasPermission && (musicLists == null || musicLists.isEmpty())) {
            Log.d("MusicPlayer", "Reloading music files in onResume");
            new Handler().postDelayed(() -> {
                getMusicFiles();
            }, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MusicPlayer", "Permission granted, loading music files");
                getMusicFiles();
            } else {
                Log.e("MusicPlayer", "Permission denied!");
                Toast.makeText(this, "Storage permission is required to load music files", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setupPlayerControls() {
        // Set up expanded player controls
        playPauseButtonExpanded.setOnClickListener(v -> {
            if (isPlaying) {
                pausePlayback();
            } else {
                startPlayback();
            }
        });

        previousButton.setOnClickListener(v -> switchToPreviousSong());
        nextButton.setOnClickListener(v -> switchToNextSong());

        // Set up progress slider
        progressSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo((int) value);
                    currentTime.setText(formatDuration((long) value));
                }
            }
        });

        // Set up repeat button
        repeatButton.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (!mediaPlayer.isLooping()) {
                    mediaPlayer.setLooping(true);
                    repeatButton.setImageResource(R.drawable.repeat_one);
                    repeatButton.setAlpha(1.0f);
                } else {
                    mediaPlayer.setLooping(false);
                    repeatButton.setImageResource(R.drawable.repeat);
                    repeatButton.setAlpha(0.5f);
                }
            }
        });

        // Set up back button to collapse sheet
        ImageButton backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }
            });
        }
    }

    private void setupBottomSheet() {
        // Set up bottom sheet behavior
        bottomSheetBehavior.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.bottom_sheet_peek_height));
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    expandedLayout.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    expandedLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                miniPlayerLayout.setAlpha(1 - slideOffset);
                expandedLayout.setAlpha(slideOffset);
                expandedLayout.setVisibility(View.VISIBLE);
                
                float scale = 1 + (slideOffset * 0.5f);
                expandedAlbumArt.setScaleX(scale);
                expandedAlbumArt.setScaleY(scale);
                
                controlsContainer.setAlpha(slideOffset);
            }
        });

        miniPlayerLayout.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
    }

    private void setupAudioSystem() {
        gestureDetector = new GestureDetectorCompat(this, new MyGestureListener(this));
        mediaPlayer = new MediaPlayer();

        // Initialize audio manager
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        
        // Set up audio attributes
        audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();

        // Create audio focus request (for API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setOnAudioFocusChangeListener(focusChange -> {
                        switch (focusChange) {
                            case AudioManager.AUDIOFOCUS_GAIN:
                                if (mediaPlayer != null) {
                                    if (!isPlaying) {
                                        mediaPlayer.start();
                                        isPlaying = true;
                                    }
                                    mediaPlayer.setVolume(1.0f, 1.0f);
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS:
                                if (mediaPlayer != null && isPlaying) {
                                    pausePlayback();
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                                if (mediaPlayer != null && isPlaying) {
                                    mediaPlayer.pause();
                                    isPlaying = false;
                                }
                                break;
                            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                if (mediaPlayer != null && isPlaying) {
                                    mediaPlayer.setVolume(0.2f, 0.2f);
                                }
                                break;
                        }
                    })
                    .build();
        }
    }

    @SuppressLint("Range")
    private void getMusicFiles() {
        Log.d("MusicPlayer", "Starting getMusicFiles()");
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Log the URI we're querying
        Log.d("MusicPlayer", "Querying URI: " + uri.toString());

        String[] projection = new String[]{
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.DATA
        };

        // Modify selection to include more audio formats and remove IS_MUSIC restriction
        String selection = "(" +
                MediaStore.Audio.Media.DATA + " LIKE '%.mp3' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.m4a' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.wav' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.aac' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.wma' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.ogg' OR " +
                MediaStore.Audio.Media.DATA + " LIKE '%.flac')";

        Log.d("MusicPlayer", "Selection query: " + selection);

        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, projection, selection, null, sortOrder);
            
            if(cursor == null) {
                Log.e("MusicPlayer", "Cursor is null - query failed");
                Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show();
                return;
            } 
            
            Log.d("MusicPlayer", "Cursor obtained. Count: " + cursor.getCount());
            
            if (!cursor.moveToFirst()) {
                Log.e("MusicPlayer", "No music files found in the query");
                Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
                return;
            }

            musicLists.clear(); // Clear existing list before adding new items
            int count = 0;
            
            do {
                String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                
                Log.d("MusicPlayer", String.format("Found music file %d: Title='%s', Artist='%s', Path='%s'", 
                    ++count, title, artist, path));
                
                int albumIdInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
                int albumInd = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
                
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String album = cursor.getString(albumInd);
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);

                long albumId = cursor.getLong(albumIdInd);
                Uri albumArt = ContentUris.withAppendedId(Uri.parse(getResources().getString(R.string.album_art_dir)), albumId);
                
                String getDuration = "0";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(
                    title != null ? title : "Unknown Title",
                    artist != null ? artist : "Unknown Artist",
                    getDuration,
                    false,
                    musicFileUri,
                    albumArt
                );
                musicLists.add(musicList);
                
            } while(cursor.moveToNext());
            
            Log.d("MusicPlayer", "Total music files processed: " + musicLists.size());
            
            // Update the fragment with the music lists
            DiscoverFragment discoverFragment = (DiscoverFragment) getSupportFragmentManager()
                .findFragmentByTag("f" + 0);
            if (discoverFragment != null) {
                discoverFragment.updateMusicLists(musicLists);
                Log.d("MusicPlayer", "Updated DiscoverFragment with music list");
            } else {
                Log.e("MusicPlayer", "DiscoverFragment not found!");
            }
            
        } catch (Exception e) {
            Log.e("MusicPlayer", "Error loading music files", e);
            e.printStackTrace();
            Toast.makeText(this, "Error loading music files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    void updateBottomCardView(MusicList musicList) {
        if (musicList == null) {
            return;
        }

        // Show the bottom sheet instead of the old card view
        if (playerBottomSheet != null) {
            playerBottomSheet.setVisibility(View.VISIBLE);
        }

        ImageView album_art = findViewById(R.id.album_art);
        TextView textViewTitle = findViewById(R.id.textViewTitle);
        TextView textViewArtist = findViewById(R.id.textViewArtist);
        ImageView btn_play_pause = findViewById(R.id.btn_play_pause);

        if (textViewTitle != null) {
            textViewTitle.setText(musicList.getTitle());
        }
        if (textViewArtist != null) {
            textViewArtist.setText(musicList.getArtist());
        }
        
        // Safely set album art
        if (album_art != null) {
            try {
                Uri albumArtUri = musicList.getAlbumArt();
                if (albumArtUri != null) {
                    album_art.setImageURI(albumArtUri);
                    // Set a fallback if the image fails to load
                    if (album_art.getDrawable() == null) {
                        album_art.setImageResource(R.drawable.music_player_default_art);
                    }
                } else {
                    album_art.setImageResource(R.drawable.music_player_default_art);
                }
            } catch (Exception e) {
                Log.e("MusicPlayer", "Error loading album art", e);
                album_art.setImageResource(R.drawable.music_player_default_art);
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
        if (mediaPlayer != null) {
            try {
                int currentPosition = mediaPlayer.getCurrentPosition();
                progressSlider.setValue(currentPosition);
                currentTime.setText(formatDuration(currentPosition));

                // Update duration if it wasn't set before
                if (totalTime != null && mediaPlayer.getDuration() > 0) {
                    totalTime.setText(formatDuration(mediaPlayer.getDuration()));
                }
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
            if (bottomCardView != null) {
                bottomCardView.setCardBackgroundColor(getResources().getColor(com.google.android.material.R.color.material_dynamic_neutral_variant20));
            }
        }
    }

    private void startPlayback() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null) {
                // Request audio focus before starting playback
                int result;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result = audioManager.requestAudioFocus(audioFocusRequest);
                } else {
                    result = audioManager.requestAudioFocus(
                            focusChange -> {
                                // Handle focus change...
                            },
                            AudioManager.STREAM_MUSIC,
                            AudioManager.AUDIOFOCUS_GAIN
                    );
                }

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    if (timer != null) {
                        timer.purge();
                        timer.cancel();
                    }
                    timer = new Timer();
                    startUpdatingProgressBar();

                    ImageView btn_play_pause = findViewById(R.id.btn_play_pause);
                    try {
                        mediaPlayer.start();
                        isPlaying = true;
                        if (btn_play_pause != null) {
                            btn_play_pause.setImageResource(R.drawable.pause_icon);
                        }
                        if (playPauseButtonExpanded != null) {
                            playPauseButtonExpanded.setImageResource(R.drawable.pause_icon);
                        }
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error starting playback", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void pausePlayback() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null && isPlaying) {
                stopUpdatingProgressBar();
                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                    timer = null;
                }

                ImageView btn_play_pause = findViewById(R.id.btn_play_pause);
                try {
                    mediaPlayer.pause();
                    isPlaying = false;
                    if (btn_play_pause != null) {
                        btn_play_pause.setImageResource(R.drawable.play_icon);
                    }
                    if (playPauseButtonExpanded != null) {
                        playPauseButtonExpanded.setImageResource(R.drawable.play_icon);
                    }
                    
                    // Abandon audio focus
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioManager.abandonAudioFocusRequest(audioFocusRequest);
                    } else {
                        audioManager.abandonAudioFocus(null);
                    }
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error pausing playback", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void updateExpandedPlayer(MusicList musicList) {
        if (musicList == null) return;

        expandedTitle.setText(musicList.getTitle());
        expandedArtist.setText(musicList.getArtist());
        
        // Safely set expanded album art
        try {
            Uri albumArtUri = musicList.getAlbumArt();
            if (albumArtUri != null) {
                expandedAlbumArt.setImageURI(albumArtUri);
                if (expandedAlbumArt.getDrawable() == null) {
                    expandedAlbumArt.setImageResource(R.drawable.music_player_default_art);
                }
            } else {
                expandedAlbumArt.setImageResource(R.drawable.music_player_default_art);
            }
        } catch (Exception e) {
            Log.e("MusicPlayer", "Error loading expanded album art", e);
            expandedAlbumArt.setImageResource(R.drawable.music_player_default_art);
        }

        // Only update duration-related UI if MediaPlayer is prepared
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            progressSlider.setValueFrom(0);
            progressSlider.setValueTo(mediaPlayer.getDuration());
            progressSlider.setValue(mediaPlayer.getCurrentPosition());
            totalTime.setText(formatDuration(mediaPlayer.getDuration()));
            currentTime.setText(formatDuration(mediaPlayer.getCurrentPosition()));
        }

        playPauseButtonExpanded.setImageResource(isPlaying ? R.drawable.pause_icon : R.drawable.play_icon);
    }

    private String formatDuration(long duration) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    @Override
    public void onChanged(int position) {
        synchronized (mediaPlayerLock) {
            if (position < 0 || position >= musicLists.size()) {
                return;
            }

            ImageView btn_play_pause = findViewById(R.id.btn_play_pause);
            MusicList musicList = musicLists.get(position);
            updateBottomCardView(musicList);

            currentSongListPosition = position;

            if (mediaPlayer != null) {
                try {
                    mediaPlayer.reset();
                    mediaPlayer.setAudioAttributes(
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build());
                    mediaPlayer.setDataSource(MainActivity.this, musicList.getMusicFile());
                    
                    // Prepare synchronously to avoid state issues
                    mediaPlayer.prepare();
                    
                    // Now that the media is prepared, we can safely get duration
                    progressSlider.setValueFrom(0);
                    progressSlider.setValueTo(mediaPlayer.getDuration());
                    progressSlider.setValue(0);
                    
                    if (totalTime != null) {
                        totalTime.setText(formatDuration(mediaPlayer.getDuration()));
                    }
                    
                    isPlaying = true;
                    startPlayback();
                    updateExpandedPlayer(musicList);

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

                            // Update the fragment with the music lists
                            DiscoverFragment discoverFragment = (DiscoverFragment) getSupportFragmentManager()
                                .findFragmentByTag("f" + 0);
                            if (discoverFragment != null) {
                                discoverFragment.updateMusicLists(musicLists);
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

    void switchToNextSong() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null) {
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

                // Update the fragment with the music lists
                DiscoverFragment discoverFragment = (DiscoverFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + 0);
                if (discoverFragment != null) {
                    discoverFragment.updateMusicLists(musicLists);
                }

                onChanged(nextSongListPosition);
            }
        }
    }

    void switchToPreviousSong() {
        synchronized (mediaPlayerLock) {
            if (mediaPlayer != null) {
                mediaPlayer.reset();

                if (timer != null) {
                    timer.purge();
                    timer.cancel();
                }

                isPlaying = false;

                int previousSongListPosition = currentSongListPosition - 1;
                if (previousSongListPosition < 0) {
                    previousSongListPosition = musicLists.size() - 1;
                }

                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(previousSongListPosition).setPlaying(true);

                if (musicAdapter != null) {
                    musicAdapter.updateList(musicLists);
                }

                // Update the fragment with the music lists
                DiscoverFragment discoverFragment = (DiscoverFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + 0);
                if (discoverFragment != null) {
                    discoverFragment.updateMusicLists(musicLists);
                }

                onChanged(previousSongListPosition);
            }
        }
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
            
            // Abandon audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            } else {
                audioManager.abandonAudioFocus(null);
            }
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }
        stopUpdatingProgressBar();
        handler.removeCallbacksAndMessages(null);
    }
}