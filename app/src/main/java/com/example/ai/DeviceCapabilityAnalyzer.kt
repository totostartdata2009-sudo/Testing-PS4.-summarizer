package com.example.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build

data class HardwareScanResult(
    val totalRamGb: Double,
    val availableRamGb: Double,
    val cores: Int,
    val abi: String,
    val androidVersion: String,
    val isLowRamDevice: Boolean // RAM < 4GB
)

class DeviceCapabilityAnalyzer(private val context: Context) {

    fun performHardwareScan(): HardwareScanResult {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
        val availableRamGb = memoryInfo.availMem / (1024 * 1024 * 1024.0)
        val cores = Runtime.getRuntime().availableProcessors()
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val isLowRamDevice = totalRamGb < 3.8 // Devices reported as <4GB RAM

        return HardwareScanResult(
            totalRamGb = totalRamGb,
            availableRamGb = availableRamGb,
            cores = cores,
            abi = primaryAbi,
            androidVersion = androidVersion,
            isLowRamDevice = isLowRamDevice
        )
    }
    
    fun isRamBelow4Gb(): Boolean {
        return performHardwareScan().isLowRamDevice
    }

    fun getHardwareDetails(): String {
        val scan = performHardwareScan()
        return "${String.format("%.1f", scan.totalRamGb)}GB RAM | ${scan.cores} CPU Cores | ${scan.abi}"
    }
}

