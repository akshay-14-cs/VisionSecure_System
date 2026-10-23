package com.example.vs_project.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.vs_project.R;

public class WelcomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        android.content.SharedPreferences prefs = getSharedPreferences("VisionSecureSession", MODE_PRIVATE);
        if (prefs.getBoolean("IS_LOGGED_IN", false)) {
            startActivity(new Intent(this, GetStartedActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_welcome);
        
        findViewById(R.id.btnGetStarted).setOnClickListener(v -> {
            startActivity(new Intent(WelcomeActivity.this, GetStartedActivity.class));
            finish();
        });

        android.widget.VideoView vvLogo = findViewById(R.id.vvLogo);
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        int videoResId;
        if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            videoResId = R.raw.welcome_video_dark;
        } else {
            videoResId = R.raw.welcome_video_light;
        }
        
        vvLogo.setVideoURI(android.net.Uri.parse("android.resource://" + getPackageName() + "/" + videoResId));
        vvLogo.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            
            mp.setVolume(0f, 0f);
        });
        vvLogo.start();
    }
}
