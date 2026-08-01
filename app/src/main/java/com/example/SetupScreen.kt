package com.example

import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.DeviceCapabilityAnalyzer
import com.example.ai.ModelPackageRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(onDownloadComplete: () -> Unit) {
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    
    val whisperState by modelManager.whisperState.collectAsState()
    val qwenState by modelManager.qwenState.collectAsState()
    
    val whisperProgress by modelManager.whisperProgress.collectAsState()
    val qwenProgress by modelManager.qwenProgress.collectAsState()

    val whisperDetails by modelManager.whisperProgressDetails.collectAsState()
    val qwenDetails by modelManager.qwenProgressDetails.collectAsState()

    var showConfirmation by remember { mutableStateOf(false) }
    var useMobileData by remember { mutableStateOf(false) }
    
    val analyzer = remember { DeviceCapabilityAnalyzer(context) }
    val isLowRam = remember { analyzer.isRamBelow4Gb() }
    val unifiedPackage = remember { ModelPackageRegistry.getPackage() }
    
    val stat = remember { StatFs(context.filesDir.path) }
    val availableStorageGb = remember { (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024.0) }
    
    val totalSizeMb = modelManager.whisperSizeMb + modelManager.qwenSizeMb
    val requiredStorageGb = (totalSizeMb * 1.5) / 1024.0
    val hasEnoughSpace = availableStorageGb >= requiredStorageGb

    LaunchedEffect(whisperState, qwenState) {
        if (whisperState == ModelState.READY && qwenState == ModelState.READY) {
            delay(1000)
            onDownloadComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0F)),
        contentAlignment = Alignment.Center
    ) {
        if (!showConfirmation && whisperState == ModelState.NOT_DOWNLOADED && qwenState == ModelState.NOT_DOWNLOADED) {
            // First time setup explanation screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Model Download Required",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The AI models must be downloaded to process speech and text offline.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                if (isLowRam) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF3B2D11)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "This device has less than 4GB RAM. AI processing may be slower than recommended.",
                                color = Color(0xFFFFE0B2),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(unifiedPackage.packageName, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        ModelInfoRow("Speech Model:", unifiedPackage.whisperName)
                        Spacer(modifier = Modifier.height(8.dp))
                        ModelInfoRow("Text Model:", unifiedPackage.qwenName)
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        ModelInfoRow("Download Size:", "≈ ${totalSizeMb.toInt()} MB")
                        Spacer(modifier = Modifier.height(8.dp))
                        ModelInfoRow("Required Storage:", "≈ ${String.format("%.1f", requiredStorageGb)} GB")
                        Spacer(modifier = Modifier.height(8.dp))
                        ModelInfoRow(
                            "Available Storage:", 
                            "≈ ${String.format("%.1f", availableStorageGb)} GB",
                            color = if (hasEnoughSpace) Color.White else Color(0xFFF94040)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!hasEnoughSpace) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF94040))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Not enough storage space. Please free up space to continue.", color = Color(0xFFF94040))
                    }
                } else {
                    Button(
                        onClick = { showConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFddb7ff)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (showConfirmation && whisperState == ModelState.NOT_DOWNLOADED && qwenState == ModelState.NOT_DOWNLOADED) {
            // Confirmation Screen
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Download Network",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Downloading these AI models will use approximately ${totalSizeMb.toInt()} MB of data.",
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                val scope = rememberCoroutineScope()
                Button(
                    onClick = {
                        showConfirmation = false
                        scope.launch { modelManager.downloadWhisper() }
                        scope.launch { modelManager.downloadQwen() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFddb7ff)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Download using Wi-Fi Only", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = {
                        useMobileData = true
                        showConfirmation = false
                        scope.launch { modelManager.downloadWhisper() }
                        scope.launch { modelManager.downloadQwen() }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Download using Mobile Data", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        } else {
            // Downloading Screen
            DownloadingView(
                whisperState, whisperProgress, whisperDetails,
                qwenState, qwenProgress, qwenDetails
            )
        }
    }
}

@Composable
fun DownloadingView(
    whisperState: ModelState, whisperProgress: Float, whisperDetails: DownloadProgress,
    qwenState: ModelState, qwenProgress: Float, qwenDetails: DownloadProgress
) {
    val activeDetails = if (whisperState == ModelState.DOWNLOADING) whisperDetails else qwenDetails

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        val currentTitle = when {
            whisperState == ModelState.DOWNLOADING -> "Downloading Speech Model"
            whisperState == ModelState.VERIFYING -> "Verifying Speech Model"
            qwenState == ModelState.DOWNLOADING -> "Downloading Text Model"
            qwenState == ModelState.VERIFYING -> "Verifying Text Model"
            whisperState == ModelState.READY && qwenState == ModelState.READY -> "Installed Successfully"
            whisperState == ModelState.DOWNLOAD_FAILED || qwenState == ModelState.DOWNLOAD_FAILED -> "Download Failed"
            else -> "Processing..."
        }
        
        val currentProgress = when {
            whisperState == ModelState.DOWNLOADING -> whisperProgress
            whisperState == ModelState.VERIFYING -> 1f
            qwenState == ModelState.DOWNLOADING -> qwenProgress
            qwenState == ModelState.VERIFYING -> 1f
            whisperState == ModelState.DOWNLOAD_FAILED || qwenState == ModelState.DOWNLOAD_FAILED -> 0f
            else -> 1f
        }

        Text(
            text = currentTitle,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { currentProgress },
                modifier = Modifier.size(160.dp),
                color = Color(0xFFddb7ff),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeWidth = 8.dp,
            )
            Text(
                text = "${(currentProgress * 100).toInt()}%",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Show Live Download Speed, ETA, and Downloaded / Total MB
        if (whisperState == ModelState.DOWNLOADING || qwenState == ModelState.DOWNLOADING) {
            val downloadedMb = activeDetails.downloadedBytes / (1024 * 1024.0)
            val totalMb = activeDetails.totalBytes / (1024 * 1024.0)
            val speedMbPerSec = activeDetails.speedBps / (1024 * 1024.0)
            val eta = activeDetails.etaSeconds

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ModelInfoRow("Downloaded:", "${String.format("%.1f", downloadedMb)} MB / ${String.format("%.1f", totalMb)} MB")
                    Spacer(modifier = Modifier.height(4.dp))
                    ModelInfoRow("Speed:", "${String.format("%.2f", speedMbPerSec)} MB/s")
                    Spacer(modifier = Modifier.height(4.dp))
                    ModelInfoRow("Estimated Remaining Time:", if (eta > 0) "${eta}s" else "Calculating...")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        DownloadStep("Speech Recognition Model", whisperState)
        Spacer(modifier = Modifier.height(16.dp))
        DownloadStep("AI Summarization Model", qwenState)
    }
}

@Composable
fun DownloadStep(name: String, state: ModelState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        val color = when (state) {
            ModelState.READY -> Color(0xFFddb7ff)
            ModelState.DOWNLOADING -> Color.White
            else -> Color.White.copy(alpha = 0.3f)
        }
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(name, color = color, fontSize = 16.sp)
    }
}

@Composable
fun FeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFFddb7ff),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun ModelInfoRow(name: String, size: String, color: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, color = Color.White.copy(alpha = 0.8f))
        Text(size, color = color)
    }
}
