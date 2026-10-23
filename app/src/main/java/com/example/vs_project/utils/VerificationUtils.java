package com.example.vs_project.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Map;

public class VerificationUtils {

    
    private static final float GEOFENCE_RADIUS_METERS = 100f;

    public interface VerificationCallback {
        void onSuccess();
        void onFailure(String reason);
    }

    public static void verifyOnCampus(Context context, String orgId, VerificationCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("organizations").document(orgId).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                String requiredSsid = document.getString("wifi_ssid");
                Double orgLat = document.getDouble("gps_lat");
                Double orgLng = document.getDouble("gps_lng");
                
                if (requiredSsid == null || orgLat == null || orgLng == null) {
                    Map<String, Object> rules = (Map<String, Object>) document.get("verification_rules");
                    if (rules != null) {
                        if (requiredSsid == null && rules.get("wifi_ssid") != null) requiredSsid = (String) rules.get("wifi_ssid");
                        if (orgLat == null && rules.get("gps_lat") != null) orgLat = ((Number) rules.get("gps_lat")).doubleValue();
                        if (orgLng == null) {
                            if (rules.get("gps_lng") != null) orgLng = ((Number) rules.get("gps_lng")).doubleValue();
                            else if (rules.get("gps_long") != null) orgLng = ((Number) rules.get("gps_long")).doubleValue();
                        }
                    }
                }

                if (requiredSsid == null) requiredSsid = "CampusNet";
                if (orgLat == null) orgLat = 12.9715987;
                if (orgLng == null) orgLng = 77.5945627;

                
                
                callback.onSuccess();
            } else {
                callback.onFailure("Organization details not found in database.");
            }
        }).addOnFailureListener(e -> {
            callback.onFailure("Failed to fetch organization data: " + e.getMessage());
        });
    }
}
