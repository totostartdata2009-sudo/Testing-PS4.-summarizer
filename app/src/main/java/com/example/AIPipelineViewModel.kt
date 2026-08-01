package com.example

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.data.AppDatabase
import com.example.data.HistoryItem
import com.example.data.ReminderItem

class AIPipelineViewModel : ViewModel() {
    companion object {
        private const val TAG = "AIPipeline"
        private val GREETING_REGEX = Regex("(?i)^(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy|sup)[!.?]*$")
    }
    
    private fun logMemory(tag: String) {
        val runtime = Runtime.getRuntime()
        val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB = runtime.maxMemory() / 1048576L
        Log.d(TAG, "[$tag] Memory Usage: Used=${usedMemInMB}MB, Max=${maxHeapSizeInMB}MB")
    }
    
    private val _summary = MutableStateFlow<String?>(null)
    val summary: StateFlow<String?> = _summary

    private val _reply = MutableStateFlow<String?>(null)
    val reply: StateFlow<String?> = _reply

    fun getHistory(context: Context): StateFlow<List<HistoryItem>> {
        val db = AppDatabase.getDatabase(context)
        return db.historyDao().getAllHistory()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun getReminders(context: Context): StateFlow<List<ReminderItem>> {
        val db = AppDatabase.getDatabase(context)
        return db.reminderDao().getAllReminders()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    }

    fun deleteHistory(context: Context, item: HistoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(context).historyDao().deleteHistory(item)
        }
    }

    fun deleteReminder(context: Context, item: ReminderItem) {
        viewModelScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(context).reminderDao().deleteReminder(item)
        }
    }

    fun initializeModels(context: Context) {
        // Models are initialized sequentially on-demand to save RAM.
    }

    fun processAudioFile(uri: String, context: Context) {
        Log.d(TAG, "[processAudioFile] Audio selection received: $uri")
        logMemory("Audio Selection")
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _summary.value = "Running Whisper On-Device..."
                _reply.value = ""
            }
            
            val startTimeAudioPrep = System.currentTimeMillis()
            Log.d(TAG, "[PROFILE] Stage 1: Audio Preprocessing started for: $uri")
            logMemory("Audio Selection")
            
            withContext(Dispatchers.Main) {
                _summary.value = "Running Whisper On-Device..."
                _reply.value = ""
            }
            
            val transcription = runLocalWhisper(uri, context)
            val audioPrepTotal = System.currentTimeMillis() - startTimeAudioPrep
            Log.d(TAG, "[PROFILE] Audio & Whisper Pipeline finished in ${audioPrepTotal}ms")
            
            if (transcription.isBlank() || transcription.startsWith("Transcription failed") || transcription.startsWith("Model not found")) {
                withContext(Dispatchers.Main) {
                    _summary.value = transcription
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                _summary.value = "Transcription complete. Processing summary...\n\nTranscription:\n$transcription"
            }
            
            Log.d(TAG, "[PROFILE] Starting Qwen summarization pipeline for audio transcript")
            runLocalQwen(transcription, context, true, uri)
            Log.d(TAG, "[PROFILE] Entire Voice Note Pipeline completed successfully")
        }
    }

    fun processText(text: String, context: Context) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            _summary.value = "Please enter text to summarize."
            return
        }
        
        // Fast greeting check
        if (GREETING_REGEX.matches(trimmed)) {
            _summary.value = "This is only a greeting. There is nothing to summarize."
            return
        }

        Log.d(TAG, "[processText] Text processing received. Length: ${trimmed.length}")
        logMemory("Text Selection")
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _summary.value = "Loading Qwen Text Model..."
                _reply.value = ""
            }
            Log.d(TAG, "[processText] Starting Qwen summarization pipeline")
            runLocalQwen(trimmed, context, false, null)
            Log.d(TAG, "[processText] Qwen summarization pipeline finished")
        }
    }

    private suspend fun runLocalWhisper(uriString: String, context: Context): String {
        Log.d(TAG, "[runLocalWhisper] Starting local Whisper execution")
        return try {
            val modelManager = ModelManager(context)
            val whisperPath = modelManager.getWhisperModelPath()
            Log.d(TAG, "[runLocalWhisper] Whisper model path: $whisperPath")
            
            val whisperFile = File(whisperPath)
            if (whisperFile.exists() && whisperFile.length() > 1024 * 1024) {
                Log.d(TAG, "[runLocalWhisper] Model file found. Loading Whisper model...")
                logMemory("Before Whisper Load")
                val startTimeLoad = System.currentTimeMillis()
                val model = Whisper.loadModel(context, whisperPath)
                val loadTime = System.currentTimeMillis() - startTimeLoad
                Log.d(TAG, "[runLocalWhisper] Whisper model loaded successfully in ${loadTime}ms")
                logMemory("After Whisper Load")
                
                try {
                    val config = WhisperConfig(language = "en")
                    
                    val uri = android.net.Uri.parse(uriString)
                    val tempFile = File(context.cacheDir, "temp_audio.wav")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "[runLocalWhisper] Audio file copied to temp location: ${tempFile.absolutePath}")
                    
                    Log.d(TAG, "[runLocalWhisper] Starting Whisper transcribe...")
                    val startTimeTranscribe = System.currentTimeMillis()
                    val result = Whisper.transcribe(model, tempFile.absolutePath, config)
                    val transcribeTime = System.currentTimeMillis() - startTimeTranscribe
                    Log.d(TAG, "[runLocalWhisper] Transcription completed in ${transcribeTime}ms. Result length: ${result.text.length}")
                    
                    if (tempFile.exists()) tempFile.delete()
                    result.text.trim()
                } finally {
                    Log.d(TAG, "[runLocalWhisper] Releasing Whisper model...")
                    Whisper.releaseModel(model)
                    Log.d(TAG, "[runLocalWhisper] Whisper model released")
                    logMemory("After Whisper Unload")
                }
            } else {
                Log.e(TAG, "[runLocalWhisper] Whisper model not found or incomplete at path: $whisperPath")
                "Model not found. Please download Whisper."
            }
        } catch (e: Exception) {
            Log.e(TAG, "[runLocalWhisper] Exception during transcription", e)
            e.printStackTrace()
            "Transcription failed."
        }
    }

    private suspend fun runLocalQwen(text: String, context: Context, hasVoice: Boolean = false, audioUri: String? = null) {
        val trimmedText = text.trim()
        if (GREETING_REGEX.matches(trimmedText)) {
            withContext(Dispatchers.Main) {
                _summary.value = "This is only a greeting. There is nothing to summarize."
            }
            return
        }

        Log.d(TAG, "[runLocalQwen] Starting local Qwen execution for text length: ${trimmedText.length}")
        var resultText = ""
        try {
            val modelManager = ModelManager(context)
            val qwenPath = modelManager.getQwenModelPath()
            Log.d(TAG, "[runLocalQwen] Qwen model path: $qwenPath")
            
            val qwenFile = File(qwenPath)
            if (qwenFile.exists() && qwenFile.length() > 10 * 1024 * 1024) {
                Log.d(TAG, "[runLocalQwen] Model file found. Loading Qwen model...")
                logMemory("Before Qwen Load")
                val startTimeLoad = System.currentTimeMillis()
                
                val cpuCores = Runtime.getRuntime().availableProcessors()
                val threadCount = minOf(cpuCores, 6).coerceAtLeast(2)
                
                val model = Llama.loadModel(
                    modelPath = qwenPath,
                    config = LlamaConfig(contextSize = 2048, threads = threadCount)
                )
                val loadTime = System.currentTimeMillis() - startTimeLoad
                Log.d(TAG, "[runLocalQwen] Qwen model loaded in ${loadTime}ms with $threadCount threads")
                logMemory("After Qwen Load")
                
                try {
                    val prompt = """<|im_start|>system
You are an offline voice and text summarizer.
Your only tasks:
1. Provide a clear, bulleted summary of the main points in the provided text.
2. Extract reminders ONLY if explicit tasks with dates or times are mentioned in the source text.
Rules:
- Never hallucinate information.
- Never act as a conversational assistant.
- Do not create reminders if there are no explicit dates/times in the text.
- If reminders exist, append them at the end under "### REMINDERS ###" in the format: Task | YYYY-MM-DD HH:mm<|im_end|>
<|im_start|>user
$trimmedText<|im_end|>
<|im_start|>assistant
""".trimIndent()
    
                    Log.d(TAG, "[runLocalQwen] Prompt sent to model:\n$prompt")
                    val startTimeGen = System.currentTimeMillis()
                    val result = Llama.complete(
                        model,
                        prompt = prompt,
                        systemPrompt = "",
                        maxTokens = 384
                    )
                    val genTime = System.currentTimeMillis() - startTimeGen
                    Log.d(TAG, "[PROFILE] Stage 5: Summary Generation completed in ${genTime}ms. Raw token output length: ${result.text.length}")
                    
                    var rawOutput = result.text.trim()
                    
                    // Anti-echoing safeguard: If output echoes prompt or user input, strip it
                    if (rawOutput.startsWith(prompt)) {
                        rawOutput = rawOutput.substring(prompt.length).trim()
                    }
                    if (rawOutput.contains("<|im_start|>assistant")) {
                        rawOutput = rawOutput.substringAfter("<|im_start|>assistant").trim()
                    }
                    if (rawOutput.startsWith(trimmedText)) {
                        rawOutput = rawOutput.substring(trimmedText.length).trim()
                    }
                    
                    resultText = rawOutput
                } finally {
                    Log.d(TAG, "[PROFILE] Stage 6: Unloading Qwen model from memory")
                    Llama.releaseModel(model)
                    Log.d(TAG, "[PROFILE] Qwen model memory released")
                    logMemory("After Qwen Unload")
                }
                
                val startTimeParse = System.currentTimeMillis()
                Log.d(TAG, "[PROFILE] Stage 7: Output Parsing & Reminder Extraction")
                var summaryPart = resultText
                var remindersPart: String? = null
                
                val remindersMarker = "### REMINDERS ###"
                val splitIndex = resultText.indexOf(remindersMarker)
                if (splitIndex != -1) {
                    summaryPart = resultText.substring(0, splitIndex).trim()
                    remindersPart = resultText.substring(splitIndex + remindersMarker.length).trim()
                }

                if (summaryPart.contains("<|im_end|>")) {
                    summaryPart = summaryPart.replace("<|im_end|>", "").trim()
                }
                if (summaryPart.contains("<|endoftext|>")) {
                    summaryPart = summaryPart.replace("<|endoftext|>", "").trim()
                }
                if (summaryPart.isEmpty()) {
                    summaryPart = "Summary completed."
                }

                // Save to history
                val startTimeDb = System.currentTimeMillis()
                Log.d(TAG, "[PROFILE] Stage 8: Saving Summary to Room Database")
                val db = AppDatabase.getDatabase(context)
                val sdfDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                val sdfTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                val now = java.util.Date()
                
                db.historyDao().insertHistory(
                    HistoryItem(
                        date = sdfDate.format(now),
                        time = sdfTime.format(now),
                        summary = summaryPart,
                        hasVoice = hasVoice,
                        audioUri = audioUri
                    )
                )
                Log.d(TAG, "[PROFILE] History saved in ${System.currentTimeMillis() - startTimeDb}ms")
                
                // Parse and save reminders strictly if present
                remindersPart?.lines()?.forEach { line ->
                    val cleanLine = line.trim().removePrefix("-").removePrefix("*").trim()
                    val parts = cleanLine.split("|")
                    if (parts.size == 2) {
                        val desc = parts[0].trim().removePrefix("[").removeSuffix("]").trim()
                        val timeStr = parts[1].trim().removePrefix("[").removeSuffix("]").trim()
                        if (desc.isNotBlank() && timeStr.isNotBlank()) {
                            try {
                                val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                val date = format.parse(timeStr)
                                if (date != null && date.time > System.currentTimeMillis()) {
                                    val timestamp = date.time
                                    val displayFormat = java.text.SimpleDateFormat("MMM dd, hh:mm a", java.util.Locale.getDefault())
                                    val id = db.reminderDao().insertReminder(
                                        ReminderItem(
                                            title = desc,
                                            time = displayFormat.format(date),
                                            isDone = false,
                                            summaryContext = summaryPart,
                                            timestamp = timestamp
                                        )
                                    )
                                    ReminderScheduler.schedule(context, id.toInt(), desc, summaryPart, timestamp)
                                    Log.d(TAG, "[PROFILE] Extracted & scheduled reminder: '$desc' for $timeStr")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to parse reminder timestamp: $timeStr", e)
                            }
                        }
                    }
                }
                Log.d(TAG, "[PROFILE] Reminder extraction completed in ${System.currentTimeMillis() - startTimeParse}ms")
            } else {
                resultText = "Model file not found. Please download the Qwen AI model."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Qwen inference", e)
            e.printStackTrace()
            resultText = "Summary processing failed."
        } finally {
            withContext(Dispatchers.Main) {
                var displayResult = if (resultText.indexOf("### REMINDERS ###") != -1) {
                    resultText.substring(0, resultText.indexOf("### REMINDERS ###")).trim()
                } else {
                    resultText
                }
                
                displayResult = displayResult.replace("<|im_end|>", "").trim()
                if (displayResult.isEmpty()) {
                    displayResult = if (resultText.isNotBlank()) resultText else "No summary generated."
                }
                
                _summary.value = displayResult
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

