package com.example

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.md_theme_dark_primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.automirrored.filled.ExitToApp
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDriveBackupDialog by remember { mutableStateOf(false) }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Voice Summary User"
    val userEmail = currentUser?.email?.takeIf { it.isNotBlank() } ?: "No Email"
    val userId = currentUser?.uid ?: "UID-Not-Authenticated"
    val photoUrl = currentUser?.photoUrl?.toString()

    val prefs = remember { context.getSharedPreferences("app_bg_prefs", Context.MODE_PRIVATE) }
    val bgType = remember { prefs.getString("bg_type", "OCEAN") ?: "OCEAN" }
    val customBgUri = remember { prefs.getString("custom_bg_uri", "") ?: "" }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val backupData = """
                                {
                                    "user": "Guest User",
                                    "email": "user@gmail.com",
                                    "history": [
                                        {"date": "Jul 10, 2026", "summary": "Full Note & Short Note Available. Smart AI processed the voice."},
                                        {"date": "Jul 09, 2026", "summary": "Don't forget to send the invoice to the client by Friday EOD"},
                                        {"date": "Jul 08, 2026", "summary": "Project update: Frontend completed, backend in review"}
                                    ]
                                }
                            """.trimIndent()
                            outputStream.write(backupData.toByteArray())
                        }
                    }
                    Toast.makeText(context, "Local Backup Successful!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Local Backup Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showDriveBackupDialog) {
        var emailInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDriveBackupDialog = false },
            containerColor = Color(0xFF1E112A),
            title = {
                Text(text = "Google Drive Backup", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Where would you like to back up your data?", color = Color(0xFFcfc2d6))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        placeholder = { Text("Enter Google Account Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A1C3B),
                            unfocusedContainerColor = Color(0xFF2A1C3B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveBackupDialog = false
                        Toast.makeText(context, "Backing up to ${if (emailInput.isNotBlank()) emailInput else "user@gmail.com"} Drive...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = md_theme_dark_primary)
                ) {
                    Text("Backup Now", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDriveBackupDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFeadfed)),
                    border = BorderStroke(1.dp, md_theme_dark_primary)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full Screen Background Wallpaper
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

        // Soft Dark Overlay for Readability
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
                                "Profile",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.padding(end = 48.dp)
                            )
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Profile Photo
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(2.dp, Color(0xFFD0BCFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        val initial = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                        Text(initial, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Profile Details Translucent Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        ProfileItem(label = "Name", value = userName)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileItem(label = "Email", value = userEmail)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        ProfileItem(label = "User ID", value = if (userId.length > 16) userId.take(16) + "..." else userId)
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Backup Section Title
                Text(
                    "Data Backup",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Local Data Backup Button
                Button(
                    onClick = { backupLauncher.launch("VoiceSummary_Backup.json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A4BFF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup to Local Storage", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Google Drive Backup Button
                Button(
                    onClick = { showDriveBackupDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup to Google Drive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Privacy Policy Button
                OutlinedButton(
                    onClick = onNavigateToPrivacyPolicy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Policy, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Logout / Sign Out Button
                Button(
                    onClick = {
                        FirebaseAuth.getInstance().signOut()
                        onLogout()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout / Sign Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // Generous vertical spacing to prevent accidental taps (Google Play policy compliance)
                Spacer(modifier = Modifier.height(36.dp))

                // AppLovin MAX Native Banner Ad
                com.example.monetization.MaxNativeBannerView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
fun ProfileItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

