package com.example.ai

data class UnifiedModelPackage(
    val packageName: String = "Offline AI Model Package",
    val whisperName: String = "Whisper Base Multilingual (Q4)",
    val whisperUrl: String = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin",
    val whisperSizeMb: Long = 142L,
    val qwenName: String = "Qwen2.5-1.5B Instruct (Q4_K_M)",
    val qwenUrl: String = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
    val qwenSizeMb: Long = 986L
)

object ModelPackageRegistry {
    val UNIFIED_PACKAGE = UnifiedModelPackage()

    fun getPackage(): UnifiedModelPackage = UNIFIED_PACKAGE
}

