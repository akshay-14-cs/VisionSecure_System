package com.example.vs_project.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("biometric/verify-offline")
    Call<ResponseBody> verifyFaceOffline(
            @Part("orgId") RequestBody orgId,
            @Part("studentId") RequestBody studentId,
            @Part("date") RequestBody selectedDate,
            @Part("timing") RequestBody selectedTiming,
            @Part("classroom") RequestBody classroom,
            @Part("invigilatorId") RequestBody invigilatorId,
            @Part MultipartBody.Part faceImage
    );

    @Multipart
    @POST("biometric/verify-online")
    Call<ResponseBody> verifyFaceOnline(
            @Part("orgId") RequestBody orgId,
            @Part("sessionId") RequestBody sessionId,
            @Part MultipartBody.Part faceImage
    );

    @Multipart
    @POST("biometric/register")
    Call<ResponseBody> registerFace(
            @Part("orgId") RequestBody orgId,
            @Part("userId") RequestBody userId,
            @Part("inchargeId") RequestBody inchargeId,
            @Part MultipartBody.Part faceImage
    );

    @Multipart
    @POST("api/timetable/upload")
    Call<ResponseBody> uploadTimetable(
            @Part("org_id") RequestBody orgId,
            @Part MultipartBody.Part csvFile
    );

    @FormUrlEncoded
    @POST("api/user/setpassword")
    Call<ResponseBody> setUserPassword(
            @Field("user_id") String userId,
            @Field("new_password_hash") String newPasswordHash
    );
}
