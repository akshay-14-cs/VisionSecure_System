package com.example.vs_project.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.vs_project.R;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class IntruderAlertService extends Service {

    private static final String CHANNEL_ID = "IntruderAlertChannel";
    private FirebaseFirestore db;
    private ListenerRegistration listener;
    private String orgId = "";

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ORG_ID")) {
            orgId = intent.getStringExtra("ORG_ID");
            startListeningForIntruders();
        }
        return START_STICKY;
    }

    private void startListeningForIntruders() {
        if (orgId.isEmpty()) return;
        
        if (listener != null) listener.remove();

        listener = db.collection("intruder_records")
                .whereEqualTo("org_id", orgId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentChange dc : value.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            
                            String title = dc.getDocument().getString("title");
                            String details = dc.getDocument().getString("details");
                            
                            
                            
                            sendNotification(title, details);
                        }
                    }
                });
    }

    private void sendNotification(String title, String details) {
        if (title == null) title = "Intruder Alert!";
        if (details == null) details = "A security breach or face mismatch was detected.";

        Intent intent = new Intent(this, AnalyticsDashboardActivity.class);
        intent.putExtra("ORG_ID", orgId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(details)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Intruder Alerts";
            String description = "Critical alerts for security breaches and face mismatches.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
