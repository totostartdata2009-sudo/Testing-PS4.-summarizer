package com.example

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.monetization.MaxAdManager
import com.example.monetization.UsageManager
import com.example.ui.theme.md_theme_dark_primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember { context.getSharedPreferences("app_bg_prefs", Context.MODE_PRIVATE) }
    val bgType = remember { prefs.getString("bg_type", "OCEAN") ?: "OCEAN" }
    val customBgUri = remember { prefs.getString("custom_bg_uri", "") ?: "" }

    val usageManager = remember { UsageManager.getInstance(context) }
    val remainingSummaries by usageManager.remainingSummariesFlow.collectAsState()
    val isPremium by usageManager.isPremiumFlow.collectAsState()
    val rewardedAdsWatched by usageManager.rewardedAdsWatchedFlow.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            bgType == "CUSTOM" && customBgUri.isNotBlank() -> {
                AsyncImage(
                    model = customBgUri,
                    contentDescription = "Custom Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.img_premium_purchase_bg),
                    contentDescription = "Special Golden Subscription Background",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.2f))
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Premium & Usage Access", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(end = 48.dp))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Current Usage Status Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFD0BCFF).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your Current AI Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                if (isPremium) "UNLIMITED ACCESS" else "$remainingSummaries Summaries Remaining",
                                color = if (isPremium) Color(0xFFFFD700) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isPremium) Color(0xFFFFD700).copy(alpha = 0.2f) else Color(0xFF9A4BFF).copy(alpha = 0.3f),
                                    RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                if (isPremium) "GOLDEN PRO" else "FREE TIER",
                                color = if (isPremium) Color(0xFFFFD700) else Color(0xFFD0BCFF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Option 2: Watch Rewarded Video (+2 Summaries)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E112A).copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFb76dff).copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color(0xFFD0BCFF), modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Option 2: Watch Rewarded Ad", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Get 2 Extra AI Summaries per video watched", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        FeatureItem("Earn +2 AI Summaries immediately", tint = Color(0xFF4ADE80))
                        FeatureItem("NO forced interstitial ads during earned summaries", tint = Color(0xFF4ADE80))
                        FeatureItem("Watched: $rewardedAdsWatched / ${UsageManager.MAX_REWARDED_ADS} Rewarded Ads", tint = Color.LightGray)

                        Spacer(modifier = Modifier.height(16.dp))

                        val canWatch = usageManager.canWatchRewardedAd()
                        Button(
                            onClick = {
                                if (activity != null) {
                                    Toast.makeText(context, "Loading Rewarded Video...", Toast.LENGTH_SHORT).show()
                                    MaxAdManager.getInstance(context).showRewardedAd(
                                        activity = activity,
                                        onRewardEarned = {
                                            val granted = usageManager.grantRewardedAdBonus()
                                            if (granted) {
                                                Toast.makeText(context, "🎉 Rewarded! +2 summaries added to your balance.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        onAdClosed = {
                                            // Refresh screen
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Ad unavailable: $error", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            enabled = canWatch,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9A4BFF),
                                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (canWatch) "Watch Video (+2 Summaries)" else "Rewarded Ads Exhausted ($rewardedAdsWatched/5)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Option 1: Golden Pro Translucent Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E112A).copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.one_time_purchase_icon),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Option 1: Golden Pro", color = Color(0xFFFFD700), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("One Time Purchase — Lifetime Access", color = md_theme_dark_primary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        FeatureItem("Unlimited Voice & Text Summarizations", tint = Color(0xFFFFD700))
                        FeatureItem("100% Ad-Free Experience", tint = Color(0xFFFFD700))
                        FeatureItem("Premium Support & VIP Access", tint = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("₹29 INR", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("One-time payment", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                usageManager.setPremiumStatus(true)
                                Toast.makeText(context, "🌟 Congratulations! You are now a Golden Pro Member!", Toast.LENGTH_LONG).show()
                            },
                            enabled = !isPremium,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD700),
                                disabledContainerColor = Color(0xFFFFD700).copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF490080))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (isPremium) "Golden Pro Active" else "Get Golden Pro (₹29)",
                                color = Color(0xFF490080),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Premium Animation Video Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            android.widget.VideoView(ctx).apply {
                                setVideoURI(android.net.Uri.parse("android.resource://${ctx.packageName}/${R.raw.premium_animation}"))
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun FeatureItem(text: String, tint: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
    }
}


