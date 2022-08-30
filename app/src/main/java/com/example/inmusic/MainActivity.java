package com.example.inmusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SongChangeListener{

    private final List<MusicList> musicLists = new ArrayList<>();
    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView startTime,endTime;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImg;
    private Timer timer;
    private int currentSongPosition = 0;
    private MusicAdapter musicAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decodeVIew = getWindow().getDecorView();

        int option = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decodeVIew.setSystemUiVisibility(option);
        setContentView(R.layout.activity_main);



        final LinearLayout searchBtn = findViewById(R.id.searchBtn);
        final LinearLayout menuBtn = findViewById(R.id.menuBtn);
        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);
        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView previousBtn = findViewById(R.id.previousBtn);
        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        playerSeekBar = findViewById(R.id.playerSeekBar);

        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mediaPlayer = new MediaPlayer();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            getMusicFiles();
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},11);
            }
            else {
                getMusicFiles();
            }
        }

        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlaying){
                    isPlaying = false;
                    mediaPlayer.pause();
                    playPauseImg.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                }
                else {
                    isPlaying = true;
                    mediaPlayer.start();
                    playPauseImg.setImageResource(R.drawable.ic_baseline_pause_24);
                }
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nextSongListPosition = currentSongPosition+1;

                if (nextSongListPosition >= musicLists.size()){
                    nextSongListPosition = 0;
                }
                musicLists.get(currentSongPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(nextSongListPosition);
                onChanged(nextSongListPosition);
            }
        });

        previousBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevSongListPosition = currentSongPosition-1;

                if (prevSongListPosition >= musicLists.size()){
                    prevSongListPosition = 0;
                }
                musicLists.get(currentSongPosition).setPlaying(false);
                musicLists.get(prevSongListPosition).setPlaying(true);

                musicAdapter.updateList(musicLists);

                musicRecyclerView.scrollToPosition(prevSongListPosition);
                onChanged(prevSongListPosition);
            }
        });
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b){
                    if (isPlaying){
                        mediaPlayer.seekTo(i);
                    }
                    else{
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    private void getMusicFiles(){
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = contentResolver.query(uri, null,MediaStore.Audio.Media.DATA+" Like?", new String[]{"%.mp3%"},null);
        if (cursor == null){
            Toast.makeText(this, "Something Want Wrong!!!", Toast.LENGTH_SHORT).show();
        }
        else if (!cursor.moveToNext()){
            Toast.makeText(this, "No Music Found", Toast.LENGTH_SHORT).show();
        }else{
            while (cursor.moveToNext()){
                final String getMusicFileName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                final String getArtistName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                long cursorId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);
                String getDuration = "00:00";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                    getDuration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION));
                }

                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musicLists.add(musicList);
            }
            musicAdapter = new MusicAdapter(musicLists, this);
            musicRecyclerView.setAdapter(musicAdapter);
        }

        cursor.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
        }else {
            Toast.makeText(this, "Application Need Permission", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            View decodeVIew = getWindow().getDecorView();

            int option = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decodeVIew.setSystemUiVisibility(option);
        }
    }

    @Override
    public void onChanged(int position) {

        currentSongPosition = position;
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(MainActivity.this,musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Unable To Play Music", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                final int getTotalDuration = mediaPlayer.getDuration();

                String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration), TimeUnit.MILLISECONDS.toSeconds(getTotalDuration),
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration)));
                endTime.setText(generateDuration);
                isPlaying = true;

                mediaPlayer.start();

                playerSeekBar.setMax(getTotalDuration);

                playPauseImg.setImageResource(R.drawable.ic_baseline_pause_24);
            }
        });
        timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final int getCurrentDuration = mediaPlayer.getCurrentPosition();
                        String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                                TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration), TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration),
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration)));
                        playerSeekBar.setProgress(getCurrentDuration);

                        startTime.setText(generateDuration);
                    }
                });

            }
        }, 1000,1000);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.reset();

                timer.purge();
                timer.cancel();
                isPlaying = false;

                playPauseImg.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                playerSeekBar.setProgress(0);
            }
        });
    }
}