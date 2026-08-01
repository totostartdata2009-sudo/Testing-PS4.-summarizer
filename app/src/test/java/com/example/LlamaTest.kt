package com.example

import android.content.Context
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@RunWith(RobolectricTestRunner::class)
class LlamaTest {
    @Test
    fun testLlama() = runBlocking {
        val url = "https://huggingface.co/Qwen/Qwen1.5-1.8B-Chat-GGUF/resolve/main/qwen1_5-1_8b-chat-q8_0.gguf"
        // Just download a tiny model to test instead of 1.9GB. Wait, 1.9GB is too big for a quick test.
        // Let's just download a tiny model like TinyLlama (100MB) or just rely on Qwen if we must.
    }
}
