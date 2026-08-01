package com.example

import android.content.Context
import android.util.Log
import com.example.ai.ModelPackageRegistry
import com.example.ai.UnifiedModelPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class ModelState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    VERIFYING,
    READY,
    DOWNLOAD_FAILED
}

class ModelManager(private val context: Context) {
    companion object {
        private const val TAG = "ModelManager"
    }

    private val activePackage: UnifiedModelPackage = ModelPackageRegistry.getPackage()

    private val whisperUrl get() = activePackage.whisperUrl
    private val qwenUrl get() = activePackage.qwenUrl
    private val whisperFileName = "whisper_model.bin"
    private val qwenFileName = "qwen_model.bin"

    private val _whisperState = MutableStateFlow(ModelState.NOT_DOWNLOADED)
    val whisperState: StateFlow<ModelState> = _whisperState

    private val _qwenState = MutableStateFlow(ModelState.NOT_DOWNLOADED)
    val qwenState: StateFlow<ModelState> = _qwenState

    private val _whisperProgress = MutableStateFlow(0f)
    val whisperProgress: StateFlow<Float> = _whisperProgress

    private val _qwenProgress = MutableStateFlow(0f)
    val qwenProgress: StateFlow<Float> = _qwenProgress

    val whisperProgressDetails: StateFlow<DownloadProgress> = ModelDownloadService.getProgressFlow(whisperFileName)
    val qwenProgressDetails: StateFlow<DownloadProgress> = ModelDownloadService.getProgressFlow(qwenFileName)

    val whisperSizeMb get() = activePackage.whisperSizeMb.toFloat()
    val qwenSizeMb get() = activePackage.qwenSizeMb.toFloat()

    init {
        checkModels()
        
        // Attach to ongoing downloads using Coroutines
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            launch {
                ModelDownloadService.getProgressFlow(whisperFileName).collect { progress ->
                    updateStateFromProgress(progress, _whisperState, _whisperProgress, "whisper_ready")
                }
            }
            launch {
                ModelDownloadService.getProgressFlow(qwenFileName).collect { progress ->
                    updateStateFromProgress(progress, _qwenState, _qwenProgress, "qwen_ready")
                }
            }
        }
    }

    private fun updateStateFromProgress(
        progress: DownloadProgress,
        stateFlow: MutableStateFlow<ModelState>,
        progressFlow: MutableStateFlow<Float>,
        prefKey: String
    ) {
        progressFlow.value = progress.progress
        when (progress.state) {
            DownloadState.DOWNLOADING -> stateFlow.value = ModelState.DOWNLOADING
            DownloadState.VERIFYING -> stateFlow.value = ModelState.VERIFYING
            DownloadState.SUCCESS -> {
                stateFlow.value = ModelState.READY
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(prefKey, true)
                    .apply()
            }
            DownloadState.FAILED -> stateFlow.value = ModelState.DOWNLOAD_FAILED
            DownloadState.IDLE -> { /* Ignore if it's already READY */ }
        }
    }

    private fun checkModels() {
        val whisperFile = File(context.filesDir, whisperFileName)
        if (whisperFile.exists() && whisperFile.length() > 10 * 1024 * 1024L) {
            _whisperState.value = ModelState.READY
            _whisperProgress.value = 1f
        }

        val qwenFile = File(context.filesDir, qwenFileName)
        if (qwenFile.exists() && qwenFile.length() > 100 * 1024 * 1024L) {
            _qwenState.value = ModelState.READY
            _qwenProgress.value = 1f
        }
    }

    suspend fun downloadWhisper() {
        startDownloadService(whisperUrl, whisperFileName)
    }

    suspend fun downloadQwen() {
        startDownloadService(qwenUrl, qwenFileName)
    }

    private fun startDownloadService(url: String, fileName: String) {
        val intent = android.content.Intent(context, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_START_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_URL, url)
            putExtra(ModelDownloadService.EXTRA_FILE_NAME, fileName)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun getWhisperModelPath(): String {
        return File(context.filesDir, whisperFileName).absolutePath
    }

    fun getQwenModelPath(): String {
        return File(context.filesDir, qwenFileName).absolutePath
    }
}

