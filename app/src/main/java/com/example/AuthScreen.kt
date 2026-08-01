package com.example

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()

    // Pulse animation for header badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Entrance Animation State
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090412))
    ) {
        // Deep Luxury Background with Radial Glows
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5B10A4).copy(alpha = 0.35f),
                            Color(0xFF23074A).copy(alpha = 0.5f),
                            Color(0xFF090412)
                        ),
                        radius = 1800f
                    )
                )
        )

        // Accent Light Beam Glow at Top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF9A4BFF).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = Color.Transparent
        ) { innerPadding ->
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                    initialOffsetY = { 60 },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // AI App Badge & Header
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF9A4BFF),
                                        Color(0xFFD0BCFF),
                                        Color(0xFF6750A4)
                                    )
                                )
                            )
                            .shadow(16.dp, CircleShape, ambientColor = Color(0xFF9A4BFF), spotColor = Color(0xFFD0BCFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF130924)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFD0BCFF),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Voice Summary.ai",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (uiState.authMode) {
                            AuthMode.LOGIN -> "Sign in to access your AI summaries & cloud sync"
                            AuthMode.SIGN_UP -> "Create your account to unlock full AI potential"
                            AuthMode.FORGOT_PASSWORD -> "Enter your email to reset your password"
                        },
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Glassmorphic Form Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF140B22).copy(alpha = 0.82f))
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFD0BCFF).copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tab Selector (LOGIN vs SIGN UP) when not in Forgot Password mode
                            if (uiState.authMode != AuthMode.FORGOT_PASSWORD) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.Black.copy(alpha = 0.35f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                        .padding(4.dp)
                                ) {
                                    val isLogin = uiState.authMode == AuthMode.LOGIN

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isLogin) Color(0xFF9A4BFF) else Color.Transparent
                                            )
                                            .clickable { viewModel.setAuthMode(AuthMode.LOGIN) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Sign In",
                                            color = if (isLogin) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (!isLogin) Color(0xFF9A4BFF) else Color.Transparent
                                            )
                                            .clickable { viewModel.setAuthMode(AuthMode.SIGN_UP) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Create Account",
                                            color = if (!isLogin) Color.White else Color.White.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            } else {
                                // Header for Forgot Password Back button
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.setAuthMode(AuthMode.LOGIN) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Back to Sign In",
                                        color = Color(0xFFD0BCFF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Error Banner
                            AnimatedVisibility(
                                visible = uiState.errorMessage != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF3B0B14))
                                        .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ErrorOutline,
                                            contentDescription = null,
                                            tint = Color(0xFFFF5252),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = uiState.errorMessage ?: "",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Success Banner
                            AnimatedVisibility(
                                visible = uiState.successMessage != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F3B1A))
                                        .border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF4ADE80),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = uiState.successMessage ?: "",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            // Name Field (Sign Up mode only)
                            if (uiState.authMode == AuthMode.SIGN_UP) {
                                OutlinedTextField(
                                    value = uiState.name,
                                    onValueChange = { viewModel.onNameChange(it) },
                                    label = { Text("Full Name", color = Color.White.copy(alpha = 0.7f)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFFD0BCFF)
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFD0BCFF),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                            }

                            // Email Field
                            OutlinedTextField(
                                value = uiState.email,
                                onValueChange = { viewModel.onEmailChange(it) },
                                label = { Text("Email Address", color = Color.White.copy(alpha = 0.7f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = if (uiState.authMode == AuthMode.FORGOT_PASSWORD) ImeAction.Done else ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                    onDone = {
                                        focusManager.clearFocus()
                                        if (uiState.authMode == AuthMode.FORGOT_PASSWORD) {
                                            viewModel.sendPasswordReset()
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD0BCFF),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Password Fields (Login & Sign Up)
                            if (uiState.authMode != AuthMode.FORGOT_PASSWORD) {
                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedTextField(
                                    value = uiState.password,
                                    onValueChange = { viewModel.onPasswordChange(it) },
                                    label = { Text("Password", color = Color.White.copy(alpha = 0.7f)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFD0BCFF)
                                        )
                                    },
                                    trailingIcon = {
                                        IconButton(onClick = { viewModel.togglePasswordVisibility() }) {
                                            Icon(
                                                imageVector = if (uiState.isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle Password Visibility",
                                                tint = Color.White.copy(alpha = 0.6f)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = if (uiState.authMode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                                        onDone = {
                                            focusManager.clearFocus()
                                            if (uiState.authMode == AuthMode.LOGIN) {
                                                viewModel.loginWithEmailPassword(onAuthSuccess)
                                            }
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                                        unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFD0BCFF),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Confirm Password Field (Sign Up mode only)
                                if (uiState.authMode == AuthMode.SIGN_UP) {
                                    Spacer(modifier = Modifier.height(14.dp))

                                    OutlinedTextField(
                                        value = uiState.confirmPassword,
                                        onValueChange = { viewModel.onConfirmPasswordChange(it) },
                                        label = { Text("Confirm Password", color = Color.White.copy(alpha = 0.7f)) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color(0xFFD0BCFF)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { viewModel.toggleConfirmPasswordVisibility() }) {
                                                Icon(
                                                    imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = "Toggle Confirm Password Visibility",
                                                    tint = Color.White.copy(alpha = 0.6f)
                                                )
                                            }
                                        },
                                        singleLine = true,
                                        visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Password,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                                viewModel.signUpWithEmailPassword(onAuthSuccess)
                                            }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Black.copy(alpha = 0.35f),
                                            unfocusedContainerColor = Color.Black.copy(alpha = 0.25f),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFFD0BCFF),
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Forgot Password text link (Login mode only)
                                if (uiState.authMode == AuthMode.LOGIN) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Forgot Password?",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .align(Alignment.End)
                                            .clickable { viewModel.setAuthMode(AuthMode.FORGOT_PASSWORD) }
                                            .padding(vertical = 4.dp, horizontal = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Action Button (Login / Sign Up / Send Reset Link)
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    when (uiState.authMode) {
                                        AuthMode.LOGIN -> viewModel.loginWithEmailPassword(onAuthSuccess)
                                        AuthMode.SIGN_UP -> viewModel.signUpWithEmailPassword(onAuthSuccess)
                                        AuthMode.FORGOT_PASSWORD -> viewModel.sendPasswordReset()
                                    }
                                },
                                enabled = !uiState.isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A4BFF)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                if (uiState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color.White,
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Text(
                                        text = when (uiState.authMode) {
                                            AuthMode.LOGIN -> "Sign In"
                                            AuthMode.SIGN_UP -> "Create Account"
                                            AuthMode.FORGOT_PASSWORD -> "Send Reset Link"
                                        },
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Secondary Google Sign-In Option
                            if (uiState.authMode != AuthMode.FORGOT_PASSWORD) {
                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = "  OR CONTINUE WITH  ",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = Color.White.copy(alpha = 0.2f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // Google Sign-In Button
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            GoogleAuthHelper.launchGoogleSignIn(
                                                context = context,
                                                onSuccess = { idToken ->
                                                    viewModel.signInWithGoogleIdToken(idToken, onAuthSuccess)
                                                },
                                                onError = { errMsg ->
                                                    viewModel.loginWithEmailPassword { } // refresh/set error
                                                }
                                            )
                                        }
                                    },
                                    enabled = !uiState.isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        // Stylized Google "G" Icon
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "G",
                                                color = Color(0xFF4285F4),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Continue with Google",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Voice Summary.ai • Protected by Firebase Security",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
