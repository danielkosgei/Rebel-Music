package com.thenewkenya.Rebel;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiscoverFragment extends Fragment {
    private RecyclerView trendingRecyclerView;
    private RecyclerView recentlyPlayedRecyclerView;
    private RecyclerView searchResultsRecyclerView;
    private TabLayout tabLayout;
    private SearchBar searchBar;
    private SearchView searchView;
    private MusicAdapter trendingAdapter;
    private MusicAdapter recentlyPlayedAdapter;
    private MusicAdapter searchAdapter;
    
    private List<MusicList> allMusicList = new ArrayList<>();
    private List<MusicList> trendingList = new ArrayList<>();
    private List<MusicList> recentlyPlayedList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_discover, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        trendingRecyclerView = view.findViewById(R.id.trendingRecyclerView);
        recentlyPlayedRecyclerView = view.findViewById(R.id.recentlyPlayedRecyclerView);
        searchResultsRecyclerView = view.findViewById(R.id.searchResultsRecyclerView);
        tabLayout = view.findViewById(R.id.tabLayout);
        searchBar = view.findViewById(R.id.searchBar);
        searchView = view.findViewById(R.id.searchView);

        // Set up trending RecyclerView
        trendingRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        trendingAdapter = new MusicAdapter(new ArrayList<>(), requireActivity(), true);
        trendingRecyclerView.setAdapter(trendingAdapter);

        // Set up recently played RecyclerView
        recentlyPlayedRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentlyPlayedAdapter = new MusicAdapter(new ArrayList<>(), requireActivity(), false);
        recentlyPlayedRecyclerView.setAdapter(recentlyPlayedAdapter);

        // Set up search results RecyclerView
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        searchAdapter = new MusicAdapter(new ArrayList<>(), requireActivity(), false);
        searchResultsRecyclerView.setAdapter(searchAdapter);

        // Set up tabs
        setupTabs();

        // Set up search
        setupSearch();
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Recently"));
        tabLayout.addTab(tabLayout.newTab().setText("Popular"));
        tabLayout.addTab(tabLayout.newTab().setText("Similar"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: // Recently
                        recentlyPlayedAdapter.updateList(recentlyPlayedList);
                        break;
                    case 1: // Popular
                        List<MusicList> popularList = new ArrayList<>(allMusicList);
                        // Sort by play count (you'll need to add this field to MusicList)
                        Collections.sort(popularList, (a, b) -> b.getPlayCount() - a.getPlayCount());
                        recentlyPlayedAdapter.updateList(popularList);
                        break;
                    case 2: // Similar
                        // Get currently playing song and find similar songs
                        MusicList currentSong = getCurrentlyPlayingSong();
                        if (currentSong != null) {
                            List<MusicList> similarSongs = findSimilarSongs(currentSong);
                            recentlyPlayedAdapter.updateList(similarSongs);
                        }
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        searchView.setupWithSearchBar(searchBar);
        searchView.addTransitionListener((searchView, previousState, newState) -> {
            if (newState == SearchView.TransitionState.SHOWN) {
                searchView.setText("");
            }
        });

        searchView.getEditText().addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterList(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterList(String query) {
        if (query == null || query.isEmpty()) {
            searchAdapter.updateList(new ArrayList<>());
            return;
        }

        List<MusicList> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (MusicList music : allMusicList) {
            if (music.getTitle().toLowerCase().contains(lowerQuery) ||
                music.getArtist().toLowerCase().contains(lowerQuery)) {
                filteredList.add(music);
            }
        }

        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), "No songs found", Toast.LENGTH_SHORT).show();
        }
        
        searchAdapter.updateList(filteredList);
    }

    private MusicList getCurrentlyPlayingSong() {
        for (MusicList music : allMusicList) {
            if (music.isPlaying()) {
                return music;
            }
        }
        return null;
    }

    private List<MusicList> findSimilarSongs(MusicList currentSong) {
        List<MusicList> similarSongs = new ArrayList<>();
        
        // Find songs by the same artist
        for (MusicList music : allMusicList) {
            if (music.getArtist().equals(currentSong.getArtist()) && !music.equals(currentSong)) {
                similarSongs.add(music);
            }
        }

        // TODO: Implement more sophisticated similarity matching
        // - Genre matching
        // - BPM matching
        // - Release year proximity
        // - User listening patterns

        return similarSongs;
    }

    public void updateMusicLists(List<MusicList> musicLists) {
        if (musicLists == null) return;

        this.allMusicList = new ArrayList<>(musicLists);

        // Update trending list (most played songs)
        trendingList = new ArrayList<>(musicLists);
        Collections.sort(trendingList, (a, b) -> b.getPlayCount() - a.getPlayCount());
        trendingList = trendingList.subList(0, Math.min(trendingList.size(), 10));
        trendingAdapter.updateList(trendingList);

        // Update recently played (based on last played timestamp)
        recentlyPlayedList = new ArrayList<>(musicLists);
        Collections.sort(recentlyPlayedList, (a, b) -> 
            Long.compare(b.getLastPlayedTimestamp(), a.getLastPlayedTimestamp()));
        recentlyPlayedAdapter.updateList(recentlyPlayedList);
    }
} 