package com.example.upworksheetsproject.data

import com.example.upworksheetsproject.util.Constants.BASE_URL
import com.example.upworksheetsproject.util.Constants.KEY
import com.google.gson.GsonBuilder
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SheetsApi {

    companion object {
        val gson = GsonBuilder()
            .setLenient()
            .create()
        val instance by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(SheetsApi::class.java)
        }

        val readInstance by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SheetsApi::class.java)
        }
    }

    @Headers("Content-Type: application/json")
    @POST("macros/s/$KEY/exec")
    suspend fun exportData(
        @Query("name") name: String,
        @Query("phone") phone: String,
        @Query("company") company: String,
        @Query("activity") activity: String,
        @Query("rating") rating: String,
        @Query("comment") comment: String
    ): String

    @Headers("Content-Type: application/json")
    @GET("macros/s/$KEY/exec")
    suspend fun getCompany(): String
}