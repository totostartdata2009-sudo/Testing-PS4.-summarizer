package com.example

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme

class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure transparent background for this activity
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val intent = intent
        val isText = intent.type?.startsWith("text/") == true
        val sharedText = if (isText) intent.getStringExtra(Intent.EXTRA_TEXT) ?: "" else ""
        
        setContent {
            MyApplicationTheme {
                var isMinimized by remember { mutableStateOf(false) }

                LaunchedEffect(isMinimized) {
                    val params = window.attributes
                    if (isMinimized) {
                        params.width = WindowManager.LayoutParams.WRAP_CONTENT
                        params.height = WindowManager.LayoutParams.WRAP_CONTENT
                        params.gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    } else {
                        params.width = WindowManager.LayoutParams.MATCH_PARENT
                        params.height = WindowManager.LayoutParams.MATCH_PARENT
                        params.gravity = Gravity.CENTER
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                    }
                    window.attributes = params
                }

                if (isMinimized) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFb76dff))
                            .clickable { isMinimized = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = "Open VoiceSummary",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isMinimized = true }
                    ) {
                        AnimatedVisibility(
                            visible = !isMinimized,
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        ) {
                            BottomSheetContent(
                                isText = isText,
                                sharedText = sharedText,
                                modifier = Modifier.clickable(enabled = false) {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomSheetContent(
    isText: Boolean, 
    sharedText: String, 
    modifier: Modifier = Modifier,
    viewModel: AIPipelineViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var activeTab by remember { mutableStateOf(if (isText) 1 else 0) }
    var currentText by remember { mutableStateOf(sharedText) }
    
    val summary by viewModel.summary.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.initializeModels(context)
        if (isText && sharedText.isNotBlank()) {
            viewModel.processText(sharedText, context)
        } else if (!isText) {
            // For audio, assuming intent data has URI, we'll process it.
            // For now, simulate trigger
            viewModel.processAudioFile("content://shared_audio", context)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = Color(0xFF0A0A0F).copy(alpha = 0.88f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            // Drag handle
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF4d4354).copy(alpha = 0.5f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reminder Pill (Optional depending on intent)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFb76dff).copy(alpha = 0.2f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFFddb7ff), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI analyzing content...", color = Color(0xFFddb7ff), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.weight(1f).clickable { activeTab = 0 },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Voice Summary", color = if (activeTab == 0) Color(0xFFeadfed) else Color(0xFFcfc2d6).copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (activeTab == 0) Color(0xFFddb7ff) else Color.Transparent))
                }
                Column(
                    modifier = Modifier.weight(1f).clickable { activeTab = 1 },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Text Summarize", color = if (activeTab == 1) Color(0xFFeadfed) else Color(0xFFcfc2d6).copy(alpha = 0.5f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(if (activeTab == 1) Color(0xFFddb7ff) else Color.Transparent))
                }
            }

            // Divider
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF4d4354).copy(alpha = 0.3f)))

            Spacer(modifier = Modifier.height(24.dp))

            if (activeTab == 1) {
                // Text Summarize Tab
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    placeholder = { Text("Paste long text here to summarize...", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E112A),
                        unfocusedContainerColor = Color(0xFF1E112A),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFb76dff),
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                val usageManager = remember { com.example.monetization.UsageManager.getInstance(context) }
                Button(
                    onClick = {
                        if (currentText.isNotBlank()) {
                            if (!usageManager.canGenerateSummary()) {
                                android.widget.Toast.makeText(context, "0 Summaries remaining! Please upgrade to Golden Pro.", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                usageManager.useSummary()
                                viewModel.processText(currentText, context)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFb76dff)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Summarize Text", color = Color.White, fontWeight = FontWeight.Bold)
                }
                
                if (summary != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("AI Result:", color = Color(0xFFddb7ff), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(summary ?: "", color = Color.White, fontSize = 14.sp)
                }
            } else {
                // Voice Tab Content
                Text(
                    text = summary ?: if (isText) "Upload a voice note to summarize." else "Initializing Voice Summary AI...",
                    color = Color(0xFFeadfed).copy(alpha = 0.95f),
                    fontSize = 18.sp,
                    lineHeight = 27.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Copy Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                    .clickable { /* Copy */ }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFeadfed))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Summary", color = Color(0xFFeadfed), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

