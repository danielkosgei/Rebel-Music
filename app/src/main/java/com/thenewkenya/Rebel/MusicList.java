package com.thenewkenya.Rebel;

import android.net.Uri;

public class MusicList {

    private String title, artist, duration;
    private boolean isPlaying;

    private Uri musicFile;
    private Uri albumArt;
    private int playCount;
    private long lastPlayedTimestamp;


    public MusicList(String title, String artist, String duration, boolean isPlaying, Uri musicFile, Uri albumArt) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.isPlaying = isPlaying;
        this.musicFile = musicFile;
        this.albumArt = albumArt;
        this.playCount = 0;
        this.lastPlayedTimestamp = 0;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        if (playing) {
            playCount++;
            lastPlayedTimestamp = System.currentTimeMillis();
        }
    }

    public Uri getMusicFile() {
        return musicFile;
    }

    public void setMusicFile(Uri musicFile) {
        this.musicFile = musicFile;
    }

    public Uri getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Uri albumArt) {
        this.albumArt = albumArt;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public long getLastPlayedTimestamp() {
        return lastPlayedTimestamp;
    }

    public void setLastPlayedTimestamp(long lastPlayedTimestamp) {
        this.lastPlayedTimestamp = lastPlayedTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicList musicList = (MusicList) o;
        return musicFile != null ? musicFile.equals(musicList.musicFile) : musicList.musicFile == null;
    }

    @Override
    public int hashCode() {
        return musicFile != null ? musicFile.hashCode() : 0;
    }
}
