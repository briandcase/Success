package com.example.success;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class PlayVideoActivity extends AppCompatActivity {
    // declare variables
    MediaController mediaControls;
    VideoView videoView;
    Intent practiceIntent;
    Intent mainActivity;
    String videoTextId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);
        videoView = findViewById(R.id.videoView);

        // setup media controller
        setUpMediaController();

        // grab intent object from MainActivity
        mainActivity = getIntent();

        // start video when activity is transitioned
        playVideo(mainActivity);
    }

    public void setUpMediaController(){
        mediaControls = new MediaController(PlayVideoActivity.this);
        mediaControls.setAnchorView(videoView);
        videoView.setMediaController(mediaControls);
    }

    public void playVideo(Intent i) {
        videoTextId = i.getStringExtra("message_key");
        videoView.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/raw/" + videoTextId ));
        videoView.start();
    }

    /** Called when the user touches the replay button **/
    public void replayVideo(View view){
        // do something in response to button click
        videoView.start();
    }

    /** Called when the user touches the practice button **/
    public void goToPracticeActivity(View view) {
        practiceIntent = new Intent(this, PracticeActivity.class);
        practiceIntent.putExtra("gesture_to_practice", videoTextId);
        startActivity(practiceIntent);
    }
}