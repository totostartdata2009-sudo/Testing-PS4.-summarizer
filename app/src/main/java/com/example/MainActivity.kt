package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme

import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val modelManager = remember { ModelManager(context) }
    
    val whisperState by modelManager.whisperState.collectAsState()
    val qwenState by modelManager.qwenState.collectAsState()
    
    val isModelDownloaded = whisperState == ModelState.READY && qwenState == ModelState.READY
    
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(onNavigateNext = {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    val destination = if (isModelDownloaded) "main" else "setup"
                    navController.navigate(destination) { popUpTo("splash") { inclusive = true } }
                } else {
                    navController.navigate("auth") { popUpTo("splash") { inclusive = true } }
                }
            })
        }
        composable("auth") {
            AuthScreen(onAuthSuccess = {
                val destination = if (isModelDownloaded) "main" else "setup"
                navController.navigate(destination) {
                    popUpTo("auth") { inclusive = true }
                }
            })
        }
        composable("setup") {
            SetupScreen(onDownloadComplete = {
                navController.navigate("main") { popUpTo("setup") { inclusive = true } }
            })
        }
        composable("main") {
            MainScreen(
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToPurchase = { navController.navigate("purchase") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPrivacyPolicy = { navController.navigate("privacy_policy") },
                onLogout = {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("purchase") {
            PurchaseScreen(onBack = { navController.popBackStack() })
        }
        composable("privacy_policy") {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
    }
}
