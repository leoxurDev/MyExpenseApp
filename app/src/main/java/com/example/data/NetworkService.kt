package com.example.data

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Structs for Apps Script interaction
    data class SyncPayload(
        val action: String, // "get" or "sync" or "test"
        val expenses: List<ExpenseDto>? = null
    )

    data class ExpenseDto(
        val id: Long,
        val amount: Double,
        val category: String,
        val date: Long,
        val description: String,
        val paymentMethod: String
    )

    data class SyncResponse(
        val success: Boolean,
        val message: String?,
        val expenses: List<ExpenseDto>?
    )

    private val payloadAdapter = moshi.adapter(SyncPayload::class.java)
    private val responseAdapter = moshi.adapter(SyncResponse::class.java)

    /**
     * Test connection to the custom Web App Script URL
     */
    suspend fun testConnection(webAppUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (webAppUrl.isBlank() || !webAppUrl.startsWith("http")) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL template"))
            }

            val payload = SyncPayload(action = "test")
            val jsonPayload = payloadAdapter.toJson(payload)

            val body = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val syncResponse = responseAdapter.fromJson(responseBody)
                if (syncResponse?.success == true) {
                    Result.success(syncResponse.message ?: "Connection successful")
                } else {
                    Result.failure(IOException(syncResponse?.message ?: "Received unsuccessful response from sheet"))
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkService", "Connection test failed", e)
            Result.failure(e)
        }
    }

    /**
     * Push offline entries to the remote Google Sheet
     */
    suspend fun exportExpenses(webAppUrl: String, expenses: List<Expense>): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (webAppUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Sheets Web App URL is not configured"))
            }

            val dtos = expenses.map {
                ExpenseDto(
                    id = it.id,
                    amount = it.amount,
                    category = it.category,
                    date = it.date,
                    description = it.description,
                    paymentMethod = it.paymentMethod
                )
            }

            val payload = SyncPayload(action = "sync", expenses = dtos)
            val jsonPayload = payloadAdapter.toJson(payload)

            val body = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val syncResponse = responseAdapter.fromJson(responseBody)
                if (syncResponse?.success == true) {
                    Result.success(syncResponse.message ?: "Sync uploaded successfully")
                } else {
                    Result.failure(IOException(syncResponse?.message ?: "Export synch action failed"))
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkService", "Export failed", e)
            Result.failure(e)
        }
    }

    /**
     * Pull remote records in from the Google Sheet
     */
    suspend fun importExpenses(webAppUrl: String): Result<List<Expense>> = withContext(Dispatchers.IO) {
        try {
            if (webAppUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Sheets Web App URL is not configured"))
            }

            val payload = SyncPayload(action = "get")
            val jsonPayload = payloadAdapter.toJson(payload)

            val body = jsonPayload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(webAppUrl)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server error: ${response.code}"))
                }

                val responseBody = response.body?.string() ?: ""
                val syncResponse = responseAdapter.fromJson(responseBody)
                if (syncResponse?.success == true && syncResponse.expenses != null) {
                    val rawExpenses = syncResponse.expenses.map {
                        Expense(
                            id = it.id,
                            amount = it.amount,
                            category = it.category,
                            date = it.date,
                            description = it.description,
                            paymentMethod = it.paymentMethod,
                            synced = true
                        )
                    }
                    Result.success(rawExpenses)
                } else {
                    Result.failure(IOException(syncResponse?.message ?: "Failed to import rows from Google Sheets"))
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkService", "Import failed", e)
            Result.failure(e)
        }
    }
}
