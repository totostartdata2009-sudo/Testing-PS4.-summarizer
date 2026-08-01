package com.example

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_bg_prefs", Context.MODE_PRIVATE) }
    val bgType = remember { prefs.getString("bg_type", "OCEAN") ?: "OCEAN" }
    val customBgUri = remember { prefs.getString("custom_bg_uri", "") ?: "" }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White, modifier = Modifier.padding(end = 48.dp))
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
            ) {
                Text(
                    text = "Privacy Policy for Voice Summary.ai",
                    color = Color(0xFFD0BCFF),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last updated: July 2026",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                PolicySection(
                    title = "1. Introduction",
                    content = "Welcome to Voice Summary.ai. We respect your privacy and are committed to protecting your personal data. This privacy policy will inform you as to how we look after your personal data when you use our application and tell you about your privacy rights and how the law protects you."
                )

                PolicySection(
                    title = "2. Data We Collect",
                    content = "We collect and process the following data to provide and improve our service:\n" +
                            "• Voice Audio Data: Used exclusively to generate smart summaries using AI.\n" +
                            "• User Profile Data: Such as Name, Email, and Profile Picture (via Google Sign-in if applicable) for account management.\n" +
                            "• App Usage Data: To understand how you interact with the app."
                )

                PolicySection(
                    title = "3. Device Permissions",
                    content = "Our app requires certain permissions to function correctly:\n" +
                            "• Microphone (RECORD_AUDIO): Required to record voice notes for summarization.\n" +
                            "• Storage (READ_EXTERNAL_STORAGE / READ_MEDIA_AUDIO): Required to read audio files from your device for processing.\n" +
                            "• Notifications (POST_NOTIFICATIONS): Used to remind you of important tasks or when a summary is ready.\n" +
                            "• Internet: Required to communicate with our AI models and perform backups."
                )

                PolicySection(
                    title = "4. Data Backup (Local & Cloud)",
                    content = "We offer two ways to safely backup your data:\n" +
                            "• Local Storage Backup: You can export your data (summaries, history, etc.) directly to your device's local storage. This file remains entirely on your device and is not uploaded to our servers.\n" +
                            "• Google Drive Backup: You can choose to back up your data securely to your Google Drive. By default, this uses the Gmail account you registered with and linked in your profile. You also have the option to authenticate with a different Gmail account for backup purposes. This data remains under your control within your personal Google account."
                )

                PolicySection(
                    title = "5. Use of Third-Party Services",
                    content = "We utilize third-party services that may collect information used to identify you:\n" +
                            "• Google AI / Gemini: We use Google's advanced AI models to process and summarize your audio data securely.\n" +
                            "• Google Drive API: We use this service strictly to facilitate the backup of your data to your personal Google Drive, if you choose that option."
                )

                PolicySection(
                    title = "6. Data Security & Retention",
                    content = "We implement appropriate security measures to prevent your personal data from being accidentally lost, used, or accessed in an unauthorized way. Your voice notes are processed for summarization and are not permanently stored on our servers without your explicit consent. Summaries and history are kept locally on your device unless backed up via Local Storage or Google Drive."
                )

                PolicySection(
                    title = "7. Your Rights",
                    content = "You have the right to access, update, or delete your personal data. You can delete your history from the app or use the provided backup functionality to export your data to Local Storage or Google Drive."
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PolicySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
            lineHeight = 22.sp
        )
    }
}

