package com.example.vs_project.utils;

import android.content.Context;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

public class SmsAlertManager {

    private static final String TAG = "SmsAlertManager";
    
    
    private static final String ADMIN_PHONE = "+1234567890";

    public static void sendIntruderAlert(Context context, String orgId, String reason) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            String message = "VISION_SECURE ALERT: Unauthorized access attempt detected at Org " + orgId + ". Reason: " + reason;
            
            smsManager.sendTextMessage(ADMIN_PHONE, null, message, null, null);
            Log.d(TAG, "Intruder alert SMS sent successfully.");
            Toast.makeText(context, "Admin Notified via SMS", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS alert: " + e.getMessage());
        }
    }
}
