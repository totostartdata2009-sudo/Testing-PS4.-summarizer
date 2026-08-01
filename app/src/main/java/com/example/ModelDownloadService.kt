package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

enum class DownloadState {
    IDLE, DOWNLOADING, VERIFYING, SUCCESS, FAILED
}

data class DownloadProgress(
    val state: DownloadState = DownloadState.IDLE,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBps: Long = 0L,
    val etaSeconds: Long = 0L
)

class ModelDownloadService : Service() {

    companion object {
        private const val TAG = "ModelDownloadService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "model_download_channel"
        
        const val ACTION_START_DOWNLOAD = "com.example.action.START_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"
        
        // Use a static map to share progress with UI
        private val _progressMap = mutableMapOf<String, MutableStateFlow<DownloadProgress>>()
        
        fun getProgressFlow(fileName: String): StateFlow<DownloadProgress> {
            return _progressMap.getOrPut(fileName) { MutableStateFlow(DownloadProgress()) }
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    // Configure OkHttp for large file downloads (increased timeouts, etc.)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
        
    private val activeDownloads = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_DOWNLOAD) {
            val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
            val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: return START_NOT_STICKY
            
            startForegroundService(fileName)
            
            if (!activeDownloads.contains(fileName)) {
                activeDownloads.add(fileName)
                serviceScope.launch {
                    downloadFile(url, fileName)
                }
            }
        }
        
        return START_STICKY
    }

    private fun startForegroundService(fileName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Downloading AI Model")
            .setContentText("Downloading $fileName in background...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
    
    private suspend fun downloadFile(url: String, fileName: String) {
        val progressFlow = _progressMap.getOrPut(fileName) { MutableStateFlow(DownloadProgress()) }
        val finalFile = File(filesDir, fileName)
        val partFile = File(filesDir, "$fileName.part")
        
        var downloadedBytes = 0L
        if (partFile.exists()) {
            downloadedBytes = partFile.length()
        }
        
        var attempt = 0
        val maxAttempts = 100 // High resilience for network drops
        var isSuccess = false
        
        while (attempt < maxAttempts && !isSuccess) {
            attempt++
            try {
                progressFlow.value = DownloadProgress(DownloadState.DOWNLOADING, progressFlow.value.progress)
                
                val requestBuilder = Request.Builder().url(url)
                if (downloadedBytes > 0) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                    Log.d(TAG, "Resuming download from $downloadedBytes bytes")
                }
                
                val request = requestBuilder.build()
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful && response.code != 206) {
                    Log.e(TAG, "HTTP error: ${response.code}")
                    if (response.code == 416) {
                        isSuccess = true
                        break
                    }
                    val backoff = (3000L * attempt).coerceAtMost(15000L)
                    delay(backoff)
                    continue
                }
                
                val body = response.body ?: throw Exception("Empty body")
                
                if (response.code == 200 && downloadedBytes > 0) {
                    Log.d(TAG, "Server ignored Range header, restarting from 0")
                    downloadedBytes = 0L
                    if (partFile.exists()) partFile.delete()
                }
                
                val totalLength = downloadedBytes + body.contentLength()
                val inputStream = body.byteStream()
                
                val outputStream = RandomAccessFile(partFile, "rw")
                outputStream.seek(downloadedBytes)
                
                val buffer = ByteArray(64 * 1024) // 64KB buffer for speed
                var lastUIUpdateTime = System.currentTimeMillis()
                var bytesSinceLastUpdate = 0L
                var currentSpeedBps = 0L

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    
                    outputStream.write(buffer, 0, read)
                    downloadedBytes += read
                    bytesSinceLastUpdate += read
                    
                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastUIUpdateTime
                    if (timeDiff > 500) {
                        currentSpeedBps = if (timeDiff > 0) (bytesSinceLastUpdate * 1000L) / timeDiff else 0L
                        bytesSinceLastUpdate = 0L
                        
                        val remainingBytes = if (totalLength > downloadedBytes) totalLength - downloadedBytes else 0L
                        val etaSec = if (currentSpeedBps > 0) remainingBytes / currentSpeedBps else 0L
                        val progress = if (totalLength > 0) downloadedBytes.toFloat() / totalLength.toFloat() else 0f
                        
                        progressFlow.value = DownloadProgress(
                            state = DownloadState.DOWNLOADING,
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalLength,
                            speedBps = currentSpeedBps,
                            etaSeconds = etaSec
                        )
                        lastUIUpdateTime = now
                    }
                }
                
                outputStream.close()
                inputStream.close()
                
                isSuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "Download error on attempt $attempt", e)
                val backoff = (3000L * attempt).coerceAtMost(15000L)
                delay(backoff)
            }
        }
        
        if (isSuccess) {
            progressFlow.value = DownloadProgress(DownloadState.VERIFYING, 1.0f)
            Log.d(TAG, "Download complete. Verifying file...")
            
            delay(1000)
            
            val minBytes = 10 * 1024 * 1024L // Minimum 10MB expected for model files
            if (partFile.exists() && partFile.length() >= minBytes) {
                if (finalFile.exists()) finalFile.delete()
                val renamed = partFile.renameTo(finalFile)
                if (renamed) {
                    Log.d(TAG, "File verified and saved successfully. Length: ${finalFile.length()}")
                    progressFlow.value = DownloadProgress(DownloadState.SUCCESS, 1.0f)
                } else {
                    Log.e(TAG, "Failed to rename temp file.")
                    progressFlow.value = DownloadProgress(DownloadState.FAILED, 1.0f)
                }
            } else {
                Log.e(TAG, "File integrity check failed. File length too small or missing.")
                if (partFile.exists()) partFile.delete()
                progressFlow.value = DownloadProgress(DownloadState.FAILED, 0f)
            }
        } else {
            Log.e(TAG, "Download failed after max attempts.")
            progressFlow.value = DownloadProgress(DownloadState.FAILED, progressFlow.value.progress)
        }
        
        activeDownloads.remove(fileName)
        if (activeDownloads.isEmpty()) {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for downloading AI models in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
