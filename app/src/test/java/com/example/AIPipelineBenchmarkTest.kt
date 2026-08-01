package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.DeviceCapabilityAnalyzer
import com.example.ai.ModelPackageRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AIPipelineBenchmarkTest {

    @Test
    fun testUnifiedModelPackageConfiguredCorrectly() {
        val unifiedPackage = ModelPackageRegistry.getPackage()
        assertEquals("Offline AI Model Package", unifiedPackage.packageName)
        assertEquals("Whisper Base Multilingual (Q4)", unifiedPackage.whisperName)
        assertEquals("Qwen2.5-1.5B Instruct (Q4_K_M)", unifiedPackage.qwenName)
        assertEquals(142L, unifiedPackage.whisperSizeMb)
        assertEquals(986L, unifiedPackage.qwenSizeMb)
    }

    @Test
    fun testDeviceCapabilityAnalyzerHardwareScan() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val analyzer = DeviceCapabilityAnalyzer(context)
        val scanResult = analyzer.performHardwareScan()
        assertNotNull(scanResult)
        
        assertTrue(scanResult.totalRamGb >= 0)
        assertTrue(scanResult.cores > 0)
        assertTrue(scanResult.abi.isNotBlank())
        assertTrue(scanResult.androidVersion.isNotBlank())
        
        val details = analyzer.getHardwareDetails()
        assertTrue(details.contains("RAM"))
        assertTrue(details.contains("CPU Cores"))
    }

    @Test
    fun testGreetingFilterPattern() {
        val greetingRegex = Regex("(?i)^(hi|hello|hey|greetings|good morning|good afternoon|good evening|howdy|sup)[!.?]*$")
        assertTrue(greetingRegex.matches("Hi"))
        assertTrue(greetingRegex.matches("hello!"))
        assertTrue(greetingRegex.matches("GOOD MORNING."))
        assertFalse(greetingRegex.matches("Hi, please summarize this meeting tomorrow at 3pm."))
    }

    @Test
    fun testReminderParsingLogic() {
        val sampleOutput = """
            Here is the summary of the audio note:
            - Discussed Q3 roadmap and target deliverables.
            - Decided on launching beta test by mid-month.
            
            ### REMINDERS ###
            Team Sync | 2026-08-15 14:00
            Submit Status Report | 2026-08-16 09:30
        """.trimIndent()

        val splitIndex = sampleOutput.indexOf("### REMINDERS ###")
        assertTrue(splitIndex != -1)
        
        val summaryPart = sampleOutput.substring(0, splitIndex).trim()
        val remindersPart = sampleOutput.substring(splitIndex + "### REMINDERS ###".length).trim()
        
        assertTrue(summaryPart.contains("Discussed Q3 roadmap"))
        assertFalse(summaryPart.contains("REMINDERS"))
        
        val lines = remindersPart.lines()
        assertEquals(2, lines.size)
        val parts = lines[0].split("|")
        assertEquals("Team Sync", parts[0].trim())
        assertEquals("2026-08-15 14:00", parts[1].trim())
    }
}
