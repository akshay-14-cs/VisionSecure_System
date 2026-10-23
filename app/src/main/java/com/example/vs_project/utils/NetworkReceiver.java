package com.example.vs_project.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class NetworkReceiver extends BroadcastReceiver {

    private TTSManager ttsManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (!isConnected) {
                if (ttsManager == null) {
                    ttsManager = new TTSManager(context);
                }
                Toast.makeText(context, "Network Disconnected! Offline Mode.", Toast.LENGTH_LONG).show();
                
                
                new android.os.Handler().postDelayed(() -> {
                    ttsManager.speak("Warning. Network disconnected. Verifications will be queued offline.");
                }, 1000);
            }
        }
    }
}
