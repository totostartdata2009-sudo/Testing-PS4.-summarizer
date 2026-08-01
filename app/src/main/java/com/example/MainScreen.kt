package com.example

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.md_theme_dark_primary

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import android.content.Context

import android.app.Activity
import android.util.Log
import com.example.monetization.MaxAdManager
import com.example.monetization.MaxBannerAdView
import com.example.monetization.UsageManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToPurchase: () -> Unit,
    viewModel: AIPipelineViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var textToSummarize by remember { mutableStateOf("") }
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("app_bg_prefs", Context.MODE_PRIVATE) }
    
    val usageManager = remember { UsageManager.getInstance(context) }
    val remainingSummaries by usageManager.remainingSummariesFlow.collectAsState()
    val isPremium by usageManager.isPremiumFlow.collectAsState()

    var shouldTriggerInterstitialForNextSummary by remember { mutableStateOf(false) }
    var lastProcessedSummary by remember { mutableStateOf<String?>(null) }
    
    // Background type: "OCEAN", "CUSTOM", "DARK"
    var bgType by remember { mutableStateOf(prefs.getString("bg_type", "OCEAN") ?: "OCEAN") }
    var customBgUri by remember { mutableStateOf(prefs.getString("custom_bg_uri", "") ?: "") }
    var showBgMenu by remember { mutableStateOf(false) }

    val summary by viewModel.summary.collectAsState()

    // 3-second Interstitial Flow when summary is generated & viewed
    LaunchedEffect(summary) {
        if (summary != null && summary != lastProcessedSummary && !summary!!.contains("Please enter text") && !summary!!.contains("This is only a greeting")) {
            lastProcessedSummary = summary
            if (shouldTriggerInterstitialForNextSummary) {
                shouldTriggerInterstitialForNextSummary = false
                scope.launch {
                    Log.d("MainScreen", "Waiting 3 seconds before showing Interstitial Ad...")
                    delay(3000) // Wait exactly 3 seconds as required
                    if (activity != null) {
                        MaxAdManager.getInstance(context).showInterstitial(activity) {}
                    }
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.initializeModels(context)
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (!usageManager.canGenerateSummary()) {
                Toast.makeText(context, "0 Summaries remaining! Opening Premium Screen...", Toast.LENGTH_LONG).show()
                onNavigateToPurchase()
            } else {
                val wasRewarded = usageManager.useSummary()
                shouldTriggerInterstitialForNextSummary = usageManager.shouldShowInterstitial(wasRewarded)
                Toast.makeText(context, "Processing audio locally...", Toast.LENGTH_SHORT).show()
                viewModel.processAudioFile(uri.toString(), context)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            customBgUri = uriStr
            bgType = "CUSTOM"
            prefs.edit().putString("bg_type", "CUSTOM").putString("custom_bg_uri", uriStr).apply()
            Toast.makeText(context, "Custom background set successfully!", Toast.LENGTH_SHORT).show()
        }
    }

    val historyItems by viewModel.getHistory(context).collectAsState(initial = emptyList())
    val reminderItems by viewModel.getReminders(context).collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        // Render Full Screen Background Image or Wallpaper
        when {
            bgType == "CUSTOM" && customBgUri.isNotBlank() -> {
                AsyncImage(
                    model = customBgUri,
                    contentDescription = "Custom PNG Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            bgType == "OCEAN" -> {
                Image(
                    painter = painterResource(id = R.drawable.img_ocean_bg),
                    contentDescription = "Ocean & Mountains Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0F))
                )
            }
        }

        // Soft dark overlay to ensure readability without blocking the image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.15f))
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                "Voice Summary.ai",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showBgMenu = true }) {
                                Icon(Icons.Default.Wallpaper, contentDescription = "Change Background", tint = Color.White)
                            }
                            DropdownMenu(
                                expanded = showBgMenu,
                                onDismissRequest = { showBgMenu = false },
                                modifier = Modifier.background(Color(0xFF1E112A))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("🌊 Ocean & Mountains", color = Color.White) },
                                    onClick = {
                                        bgType = "OCEAN"
                                        prefs.edit().putString("bg_type", "OCEAN").apply()
                                        showBgMenu = false
                                        Toast.makeText(context, "Ocean background set", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🖼️ Pick Custom PNG Photo", color = Color.White) },
                                    onClick = {
                                        showBgMenu = false
                                        imagePicker.launch("image/*")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🌙 Dark Theme", color = Color.White) },
                                    onClick = {
                                        bgType = "DARK"
                                        prefs.edit().putString("bg_type", "DARK").apply()
                                        showBgMenu = false
                                        Toast.makeText(context, "Dark theme set", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        // Remaining Summaries Badge
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, if (isPremium) Color(0xFFFFD700) else Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onNavigateToPurchase() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (isPremium) "PRO ⭐" else "$remainingSummaries Left",
                                color = if (isPremium) Color(0xFFFFD700) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = onNavigateToPurchase,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.25f))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.one_time_purchase_icon),
                                contentDescription = "Upgrade to Pro",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    // Banner Ad placed on Voice Upload (0), History (1), and Reminders (2) Screens
                    if (selectedTab in 0..2) {
                        MaxBannerAdView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                        )
                    }

                    NavigationBar(
                        containerColor = Color.Black.copy(alpha = 0.35f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                            label = { Text("Voice Upload", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFFb76dff).copy(alpha = 0.4f),
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("History", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFFb76dff).copy(alpha = 0.4f),
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.Alarm, contentDescription = null) },
                            label = { Text("Reminders", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFFb76dff).copy(alpha = 0.4f),
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                        NavigationBarItem(
                            selected = false,
                            onClick = onNavigateToProfile,
                            icon = { Icon(Icons.Default.Person, contentDescription = null) },
                            label = { Text("Profile", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            if (selectedTab == 1) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (historyItems.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                Text("No history available", color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
                            }
                        }
                    } else {
                        items(historyItems.size) { index ->
                            val item = historyItems[index]
                            HistoryCard(
                                date = item.date,
                                time = item.time,
                                summary = item.summary,
                                hasVoice = item.hasVoice,
                                onDelete = { viewModel.deleteHistory(context, item) }
                            )
                        }
                    }
                }
            } else if (selectedTab == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.35f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Upload Voice Note",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Smart AI will summarize key points locally.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = { audioPicker.launch("audio/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A4BFF)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Audio File", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "— OR PASTE TEXT —",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = textToSummarize,
                            onValueChange = { textToSummarize = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            placeholder = { Text("Paste long text here to summarize...", color = Color.White.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                                unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFD0BCFF),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                if (textToSummarize.isNotBlank()) {
                                    if (!usageManager.canGenerateSummary()) {
                                        Toast.makeText(context, "0 Summaries remaining! Opening Premium Screen...", Toast.LENGTH_LONG).show()
                                        onNavigateToPurchase()
                                    } else {
                                        val wasRewarded = usageManager.useSummary()
                                        shouldTriggerInterstitialForNextSummary = usageManager.shouldShowInterstitial(wasRewarded)
                                        Toast.makeText(context, "Analyzing text locally...", Toast.LENGTH_SHORT).show()
                                        viewModel.processText(textToSummarize, context)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Summarize Text", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        
                        if (summary != null) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text("AI Summary Result", color = Color(0xFFD0BCFF), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(summary ?: "", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 2) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (reminderItems.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                                    Text("No reminders set", color = Color.LightGray, fontSize = 16.sp)
                                }
                            }
                        } else {
                            items(reminderItems.size) { index ->
                                val item = reminderItems[index]
                                ReminderCard(
                                    title = item.title,
                                    time = item.time,
                                    summaryContext = item.summaryContext,
                                    onDelete = { viewModel.deleteReminder(context, item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCard(date: String, time: String, summary: String, hasVoice: Boolean, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp)
            .clickable { /* Expand logic */ }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    color = Color(0xFFcfc2d6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasVoice) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play Audio",
                            tint = md_theme_dark_primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = time,
                        color = md_theme_dark_primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp).clickable { onDelete() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                color = Color(0xFFeadfed),
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReminderCard(title: String, time: String, summaryContext: String, onDelete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color(0xFFA855F7).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = Color(0xFFcfc2d6),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = time,
                        color = Color(0xFFcfc2d6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (summaryContext.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = summaryContext,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.Red.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp).clickable { onDelete() }.padding(top = 2.dp)
            )
        }
    }
}
