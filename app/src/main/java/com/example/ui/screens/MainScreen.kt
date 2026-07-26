package com.example.ui.screens

import android.util.Log
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.components.AnimatedDialogContainer
import com.example.ui.components.bounceOnClick
import com.example.ui.components.getStandardScreenTransitionSpec
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Appointment
import com.example.data.Bike
import com.example.data.ServiceRecord
import com.example.data.RiderPhotoUpload
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = SurfaceDarkElevated,
    unfocusedContainerColor = SurfaceDarkElevated,
    focusedBorderColor = MotorOrange,
    unfocusedBorderColor = BorderColor,
    disabledContainerColor = SurfaceDarkElevated,
    errorContainerColor = SurfaceDarkElevated
)

private fun isSameDay(timeMs1: Long, timeMs2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timeMs1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timeMs2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextPrimary
) {
    val rotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    IconButton(
        onClick = onToggle,
        modifier = modifier
            .scale(scale)
            .testTag("theme_toggle_button")
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(
                    rotationZ = rotation,
                    compositingStrategy = CompositingStrategy.Offscreen
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDarkTheme) {
                // Crescent Moon
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * 0.38f
                    drawCircle(color = tint, radius = radius, center = center)
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius * 1.0f,
                        center = Offset(center.x - radius * 0.5f, center.y - radius * 0.3f),
                        blendMode = BlendMode.DstOut
                    )
                }
            } else {
                // Sun
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension * 0.22f
                    drawCircle(color = tint, radius = radius, center = center)
                    
                    val rayLength = size.minDimension * 0.16f
                    val rayStroke = 1.8.dp.toPx()
                    for (i in 0..7) {
                        val angle = i * Math.PI / 4
                        val startX = center.x + (radius + 2.dp.toPx()) * Math.cos(angle).toFloat()
                        val startY = center.y + (radius + 2.dp.toPx()) * Math.sin(angle).toFloat()
                        val endX = center.x + (radius + 2.dp.toPx() + rayLength) * Math.cos(angle).toFloat()
                        val endY = center.y + (radius + 2.dp.toPx() + rayLength) * Math.sin(angle).toFloat()
                        drawLine(
                            color = tint,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = rayStroke,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PortalSelectionScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onMenuClick: () -> Unit,
    onRoleSelected: (String) -> Unit
) {
    var selected by remember { mutableStateOf("RIDER") }
    Box(modifier = Modifier.fillMaxSize().background(SlateDark)) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu, 
                        contentDescription = "Menu", 
                        tint = TextPrimary
                    )
                }
                
                GarageLogo(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .height(48.dp)
                )
            }

            ThemeToggleButton(
                isDarkTheme = isDarkTheme,
                onToggle = onThemeToggle,
                tint = MotorOrange
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
        // Logo & Branding
        MotorcycleLogoIcon(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "ADVANCE AUTO",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = MotorOrange
            )
        )
        Text(
            text = "MOTOR TRADING",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Reliable motorcycle servicing, oil life tracking, and seamless slot bookings.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "CHOOSE YOUR PORTAL",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Bike Rider Option Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected == "RIDER") SurfaceDarkElevated else SurfaceDark
            ),
            border = BorderStroke(
                width = if (selected == "RIDER") 2.dp else 1.dp,
                color = if (selected == "RIDER") MotorOrange else BorderColor
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selected = "RIDER" }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (selected == "RIDER") MotorOrange.copy(alpha = 0.15f) else SurfaceDarkElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selected == "RIDER") MotorOrange else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bike Rider Portal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected == "RIDER") MotorOrange else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Check service records, monitor diagnostics, and book maintenance slots.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Management Option Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selected == "MANAGEMENT") SurfaceDarkElevated else SurfaceDark
            ),
            border = BorderStroke(
                width = if (selected == "MANAGEMENT") 2.dp else 1.dp,
                color = if (selected == "MANAGEMENT") MotorOrange else BorderColor
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selected = "MANAGEMENT" }
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (selected == "MANAGEMENT") MotorOrange.copy(alpha = 0.15f) else SurfaceDarkElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = if (selected == "MANAGEMENT") MotorOrange else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Management Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected == "MANAGEMENT") MotorOrange else TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Review logs, schedule release windows, set booking conditions, and access the Appointment desk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = { onRoleSelected(selected) },
            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("proceed_to_login_button")
        ) {
            Text("PROCEED TO LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    role: String,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    autoLoginChecked: Boolean,
    onAutoLoginCheckedChange: (Boolean) -> Unit,
    error: String,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Back Button & Theme Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Back to Selection", color = TextPrimary, fontWeight = FontWeight.Bold)
            }

            ThemeToggleButton(
                isDarkTheme = isDarkTheme,
                onToggle = onThemeToggle,
                tint = MotorOrange
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Heading
        Text(
            text = if (role == "RIDER") "Rider Portal Access" else "Management Access",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MotorOrange
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (role == "RIDER") "Sign in with your license plate or registered phone number." else "Provide authorized administrator credentials.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Username / License Plate Field
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text(if (role == "RIDER") "License Plate or Phone Number" else "Administrator Username") },
            placeholder = { Text(if (role == "RIDER") "e.g., MH-12-AB-1234" else "e.g., admin") },
            colors = customTextFieldColors(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("login_username_field")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Password field (only for Management, or optional for Rider)
        if (role == "MANAGEMENT") {
            var isPasswordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Access Password") },
                placeholder = { Text("Enter admin password") },
                colors = customTextFieldColors(),
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(
                            text = if (isPasswordVisible) "HIDE" else "SHOW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotorOrange
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_field")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Pre-filled values available for testing (admin / admin123)",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        } else {
            // Optional phone number / backup field to make rider login look rich
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Contact Number (Optional)") },
                placeholder = { Text("e.g., +1 555-0192") },
                colors = customTextFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Enter MH-12-AB-1234 or DL-3S-CD-5678 to view pre-seeded records",
                fontSize = 11.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save credentials & Auto-login option
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onAutoLoginCheckedChange(!autoLoginChecked) }
                .padding(vertical = 4.dp, horizontal = 2.dp)
        ) {
            Checkbox(
                checked = autoLoginChecked,
                onCheckedChange = onAutoLoginCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MotorOrange,
                    uncheckedColor = TextSecondary,
                    checkmarkColor = SlateDark
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "Save credentials & Auto-login",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Log in automatically whenever the app opens",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = Color(0xFFEF4444),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("login_submit_button")
        ) {
            Text("SECURE ACCESS LOGIN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MotorcycleViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showLogoDialog by remember { mutableStateOf(false) }

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    val outputStream = java.io.ByteArrayOutputStream()
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                    val base64Str = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                    viewModel.updateCustomLogoBase64(base64Str)
                    Toast.makeText(context, "Logo updated successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to decode image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var currentScreen by remember { mutableStateOf("PORTAL_SELECTION") } // "PORTAL_SELECTION", "LOGIN", "MAIN"
    var selectedRole by remember { mutableStateOf("RIDER") } // "RIDER" or "MANAGEMENT"
    
    // Active portal sub-menus
    var activeManagementMenu by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "SERVICE_HISTORY", "APPOINTMENTS", etc.
    var serviceQueueMode by remember { mutableStateOf("ADMIN") } // "ADMIN" or "TV_DISPLAY"
    var activeRiderMenu by remember { mutableStateOf("DASHBOARD") } // "DASHBOARD", "OIL", "APPOINTMENT_SECTION", "VEHICLE_CHECK"
    var showFirebaseSettingsInMain by remember { mutableStateOf(false) }
    var showPerformanceSettingsInMain by remember { mutableStateOf(false) }
    
    val sharedPrefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    var autoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("AUTO_LOGIN_ENABLED", true)) }

    // Login States
    var usernameInput by remember { mutableStateOf(sharedPrefs.getString("AUTO_LOGIN_USERNAME", "") ?: "") }
    var passwordInput by remember { mutableStateOf(sharedPrefs.getString("AUTO_LOGIN_PASSWORD", "") ?: "") }
    var loginError by remember { mutableStateOf("") }
    var loggedInManagerUsername by remember { mutableStateOf("") }

    fun saveCredentialsIfEnabled(role: String, user: String, pass: String) {
        if (autoLoginEnabled) {
            sharedPrefs.edit()
                .putBoolean("AUTO_LOGIN_ENABLED", true)
                .putString("AUTO_LOGIN_ROLE", role)
                .putString("AUTO_LOGIN_USERNAME", user)
                .putString("AUTO_LOGIN_PASSWORD", pass)
                .apply()
        } else {
            sharedPrefs.edit()
                .putBoolean("AUTO_LOGIN_ENABLED", false)
                .remove("AUTO_LOGIN_ROLE")
                .remove("AUTO_LOGIN_USERNAME")
                .remove("AUTO_LOGIN_PASSWORD")
                .apply()
        }
    }

    // Auto-login on launch if credentials were saved
    LaunchedEffect(Unit) {
        val isAuto = sharedPrefs.getBoolean("AUTO_LOGIN_ENABLED", false)
        val savedRole = sharedPrefs.getString("AUTO_LOGIN_ROLE", null)
        val savedUser = sharedPrefs.getString("AUTO_LOGIN_USERNAME", null)
        val savedPass = sharedPrefs.getString("AUTO_LOGIN_PASSWORD", null) ?: ""

        if (isAuto && !savedRole.isNullOrBlank() && !savedUser.isNullOrBlank()) {
            if (savedRole == "MANAGEMENT") {
                val u = savedUser.trim().lowercase()
                if (u == "admin" && (savedPass == "admin123" || savedPass.isBlank())) {
                    selectedRole = "MANAGEMENT"
                    usernameInput = savedUser
                    passwordInput = "admin123"
                    loggedInManagerUsername = u
                    viewModel.registerManagementUser(u)
                    currentScreen = "MAIN"
                } else if (viewModel.getRegisteredEmails().contains(u)) {
                    if (!viewModel.isPasswordSetupRequired(u)) {
                        val realPass = viewModel.getMgmtPassword(u)
                        if (savedPass == realPass) {
                            selectedRole = "MANAGEMENT"
                            usernameInput = savedUser
                            passwordInput = savedPass
                            loggedInManagerUsername = u
                            viewModel.registerManagementUser(u)
                            currentScreen = "MAIN"
                        }
                    }
                }
            } else if (savedRole == "RIDER") {
                selectedRole = "RIDER"
                usernameInput = savedUser
                passwordInput = savedPass
                viewModel.selectBike(savedUser)
                currentScreen = "MAIN"
            }
        }
    }

    var showFirstTimeSetupDialog by remember { mutableStateOf(false) }
    var setupPasswordInput by remember { mutableStateOf("") }
    var setupConfirmPasswordInput by remember { mutableStateOf("") }
    var setupErrorMsg by remember { mutableStateOf("") }

    val bikes by viewModel.bikes.collectAsState()
    val serviceRecords by viewModel.serviceRecords.collectAsState()
    val appointments by viewModel.appointments.collectAsState()

    LaunchedEffect(appointments) {
        if (appointments.isNotEmpty()) {
            com.example.util.AppointmentNotificationHelper.checkAndTriggerUpcomingReminders(context, appointments)
        }
    }

    var showAddBikeDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }

    // Live slot notification from management
    val liveNotificationMessage by viewModel.liveNotificationMessage.collectAsState()

    // Side navigation drawer and theme options
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val shiftATime by viewModel.shiftATiming.collectAsState()
    val shiftBTime by viewModel.shiftBTiming.collectAsState()
    val fridayNote by viewModel.fridayPrayerNote.collectAsState()

    val garageName by viewModel.garageName.collectAsState()
    val garageBuilding by viewModel.garageBuilding.collectAsState()
    val garageStreet by viewModel.garageStreet.collectAsState()
    val garageZone by viewModel.garageZone.collectAsState()
    val garageArea by viewModel.garageArea.collectAsState()

    var showEditGarageDialog by remember { mutableStateOf(false) }
    var editedGarageName by remember { mutableStateOf("") }
    var editedGarageBuilding by remember { mutableStateOf("") }
    var editedGarageStreet by remember { mutableStateOf("") }
    var editedGarageZone by remember { mutableStateOf("") }
    var editedGarageArea by remember { mutableStateOf("") }

    var showEditFridayNoteDialog by remember { mutableStateOf(false) }
    var editedFridayNote by remember { mutableStateOf("") }

    var lastTriggeredDayOfYear by remember { mutableStateOf(-1) }
    val releaseHour by viewModel.releaseHour.collectAsState()
    val releaseMinute by viewModel.releaseMinute.collectAsState()
    val releaseAmPm by viewModel.releaseAmPm.collectAsState()

    // Automatic slots publisher background task simulation
    LaunchedEffect(releaseHour, releaseMinute, releaseAmPm) {
        while (true) {
            val now = Calendar.getInstance()
            var targetHour = releaseHour
            if (releaseAmPm == "PM" && targetHour < 12) targetHour += 12
            if (releaseAmPm == "AM" && targetHour == 12) targetHour = 0
            
            if (now.get(Calendar.HOUR_OF_DAY) == targetHour && 
                now.get(Calendar.MINUTE) == releaseMinute && 
                now.get(Calendar.DAY_OF_YEAR) != lastTriggeredDayOfYear
            ) {
                lastTriggeredDayOfYear = now.get(Calendar.DAY_OF_YEAR)
                viewModel.triggerLiveSlotsNotification()
            }
            kotlinx.coroutines.delay(10000L) // Check every 10 seconds
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceDark,
                drawerContentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(300.dp)
                        .background(SurfaceDark)
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        MotorcycleLogoIcon(modifier = Modifier.size(48.dp), viewModel = viewModel)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ADVANCE AUTO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MotorOrange,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Garage App Options",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "APP SETTINGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Theme Toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkElevated)
                            .clickable { viewModel.toggleTheme() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.Settings else Icons.Default.Info,
                                contentDescription = null,
                                tint = MotorOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isDarkTheme) "Dark Theme" else "Light Theme",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MotorOrange,
                                checkedTrackColor = MotorOrange.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Shortcut buttons
                    Text(
                        text = "NAVIGATION SHORTCUTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceDarkElevated)
                            .clickable {
                                sharedPrefs.edit().putBoolean("AUTO_LOGIN_ENABLED", false).apply()
                                autoLoginEnabled = false
                                currentScreen = "PORTAL_SELECTION"
                                usernameInput = ""
                                passwordInput = ""
                                loginError = ""
                                scope.launch { drawerState.close() }
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Exit to Role Selection", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "GARAGE INFORMATION (QATAR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val locationContext = LocalContext.current
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(garageName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                if (selectedRole == "MANAGEMENT") {
                                    IconButton(
                                        onClick = {
                                            editedGarageName = garageName
                                            editedGarageBuilding = garageBuilding
                                            editedGarageStreet = garageStreet
                                            editedGarageZone = garageZone
                                            editedGarageArea = garageArea
                                            showEditGarageDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Garage Info",
                                            tint = MotorOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(garageBuilding, fontSize = 11.sp, color = TextSecondary)
                            Text(garageStreet, fontSize = 11.sp, color = TextSecondary)
                            Text(garageZone, fontSize = 11.sp, color = TextSecondary)
                            Text(garageArea, fontSize = 10.sp, color = TextDisabled)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val rawQuery = if (garageBuilding.contains("5G5F+363") || garageStreet.contains("Al Wukair")) {
                                            "5G5F+363, Al Wukair"
                                        } else {
                                            "$garageBuilding $garageStreet $garageZone $garageArea"
                                        }
                                        val query = java.net.URLEncoder.encode(rawQuery, "UTF-8")
                                        val mapsUrl = "https://www.google.com/maps/search/?api=1&query=$query"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl))
                                        locationContext.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(locationContext, "Google Maps could not be opened.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = SlateDark, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open Google Maps", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                            }
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("GARAGE TIMINGS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                             Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("All Days:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                val openingTime = getGarageOpeningTime(shiftATime)
                                val closingTime = getGarageClosingTime(shiftBTime)
                                Text("$openingTime - $closingTime", fontSize = 11.sp, color = TextSecondary)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Note:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = fridayNote,
                                        fontSize = 11.sp,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (selectedRole == "MANAGEMENT") {
                                    IconButton(
                                        onClick = {
                                            editedFridayNote = fridayNote
                                            showEditFridayNoteDialog = true
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Friday Note",
                                            tint = MotorOrange,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showEditFridayNoteDialog) {
                        Dialog(onDismissRequest = { showEditFridayNoteDialog = false }) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Edit Friday Timing Note",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    OutlinedTextField(
                                        value = editedFridayNote,
                                        onValueChange = { editedFridayNote = it },
                                        label = { Text("Friday Prayer Note") },
                                        placeholder = { Text("During the prayer time garage will be closed") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showEditFridayNoteDialog = false }) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.updateFridayPrayerNote(editedFridayNote.trim())
                                                showEditFridayNoteDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                                        ) {
                                            Text("Save", color = SlateDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showEditGarageDialog) {
                        Dialog(onDismissRequest = { showEditGarageDialog = false }) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Edit Garage Location Details",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    OutlinedTextField(
                                        value = editedGarageName,
                                        onValueChange = { editedGarageName = it },
                                        label = { Text("Garage Name") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedGarageBuilding,
                                        onValueChange = { editedGarageBuilding = it },
                                        label = { Text("Building Info") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedGarageStreet,
                                        onValueChange = { editedGarageStreet = it },
                                        label = { Text("Street Info") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedGarageZone,
                                        onValueChange = { editedGarageZone = it },
                                        label = { Text("Zone Info") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = editedGarageArea,
                                        onValueChange = { editedGarageArea = it },
                                        label = { Text("Area / Country") },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MotorOrange,
                                            unfocusedBorderColor = BorderColor,
                                            focusedLabelColor = MotorOrange,
                                            unfocusedLabelColor = TextSecondary,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { showEditGarageDialog = false }) {
                                            Text("Cancel", color = TextSecondary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.updateGarageDetails(
                                                    editedGarageName.trim(),
                                                    editedGarageBuilding.trim(),
                                                    editedGarageStreet.trim(),
                                                    editedGarageZone.trim(),
                                                    editedGarageArea.trim()
                                                )
                                                showEditGarageDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                                        ) {
                                            Text("Save", color = SlateDark, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Build v2.1.0 • Offline Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = SlateDark,
        topBar = {
            if (currentScreen == "MAIN") {
                val isInSubSection = (selectedRole == "MANAGEMENT" && activeManagementMenu != "DASHBOARD") || 
                                     (selectedRole == "RIDER" && activeRiderMenu != "DASHBOARD")
                
                val subSectionTitle = when (selectedRole) {
                    "MANAGEMENT" -> when (activeManagementMenu) {
                        "SERVICE_HISTORY" -> "Service History Portal"
                        "APPOINTMENTS" -> "Appointment desk"
                        "DATA_INPUT" -> "Data Input Center"
                        "MONTHLY_PERFORMANCE" -> "Performance Insights"
                        "STAFF_MANAGEMENT" -> "Staff Management"
                        "ACCESS_CONTROL" -> "Access Control Settings"
                        "GARAGE_TRAFFIC" -> "Garage Traffic Status"
                        "FIREBASE_PORTAL" -> "Firebase Live Portal"
                        "SERVICE_QUEUE" -> if (serviceQueueMode == "TV_DISPLAY") "LIVE GARAGE SERVICE DISPLAY" else "Service Queue Manager"
                        "SERVICE_ALERTS" -> "Service Overdue Alerts"
                        "QR_SCANNER" -> "QR Frame Scanner"
                        else -> ""
                    }
                    else -> when (activeRiderMenu) {
                        "OIL" -> "Recent Oil History"
                        "APPOINTMENT_SECTION" -> "Appointment Section"
                        "VEHICLE_CHECK" -> "Vehicle Check (Uploads)"
                        else -> ""
                    }
                }

                Column(
                    modifier = Modifier
                        .background(SlateDark)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Main Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isInSubSection) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (selectedRole == "MANAGEMENT") {
                                            activeManagementMenu = "DASHBOARD"
                                            serviceQueueMode = "ADMIN"
                                        } else {
                                            activeRiderMenu = "DASHBOARD"
                                        }
                                    },
                                    modifier = Modifier.padding(end = 4.dp).testTag("top_back_home_button")
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = "Home Page", tint = MotorOrange)
                                }
                                Text(
                                    text = subSectionTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.padding(end = 4.dp).testTag("top_menu_drawer_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu Drawer", tint = TextPrimary)
                                }
                                MotorcycleLogoIcon(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .padding(end = 8.dp),
                                    viewModel = viewModel
                                )
                                Column {
                                    Text(
                                        text = if (selectedRole == "MANAGEMENT") "ADVANCE AUTO" else "RIDER PORTAL",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 1.5.sp,
                                            color = MotorOrange
                                        )
                                    )
                                    Text(
                                        text = if (selectedRole == "MANAGEMENT") "Motor Trading" else "Advance Auto Garage",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            color = TextPrimary
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeToggleButton(
                                isDarkTheme = isDarkTheme,
                                onToggle = { viewModel.toggleTheme() },
                                tint = MotorOrange
                            )

                            if (selectedRole == "MANAGEMENT") {
                                if (!isInSubSection) {
                                    IconButton(
                                        onClick = { showLogoDialog = true },
                                        modifier = Modifier.testTag("top_bar_edit_logo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Logo",
                                            tint = MotorOrange
                                        )
                                    }
                                } else if (activeManagementMenu == "FIREBASE_PORTAL") {
                                    IconButton(
                                        onClick = { showFirebaseSettingsInMain = true },
                                        modifier = Modifier.testTag("top_bar_firebase_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Settings",
                                            tint = MotorOrange
                                        )
                                    }
                                } else if (activeManagementMenu == "MONTHLY_PERFORMANCE") {
                                    IconButton(
                                        onClick = { showPerformanceSettingsInMain = true },
                                        modifier = Modifier.testTag("top_bar_performance_settings_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = "Backend Settings",
                                            tint = MotorOrange
                                        )
                                    }
                                }
                            }

                            if (selectedRole == "RIDER" && !isInSubSection) {
                                val currentBikes by viewModel.currentBikes.collectAsState()
                                val statusText = getGarageStatus(currentBikes)
                                val statusColor = getGarageStatusColor(currentBikes)
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceDarkElevated)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = BorderColor, thickness = 1.dp)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen == "MAIN") paddingValues else PaddingValues(0.dp))
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    getStandardScreenTransitionSpec()
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    "PORTAL_SELECTION" -> {
                        PortalSelectionScreen(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { viewModel.toggleTheme() },
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onRoleSelected = { role ->
                                if (role == "FIREBASE_PORTAL") {
                                    currentScreen = "FIREBASE_PORTAL"
                                } else {
                                    selectedRole = role
                                    currentScreen = "LOGIN"
                                }
                            }
                        )
                    }
                    "FIREBASE_PORTAL" -> {
                        FirebaseLivePortalScreen(
                            viewModel = viewModel,
                            onBack = { currentScreen = "PORTAL_SELECTION" }
                        )
                    }
                    "LOGIN" -> {
                        LoginScreen(
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { viewModel.toggleTheme() },
                            role = selectedRole,
                            username = usernameInput,
                            onUsernameChange = { usernameInput = it },
                            password = passwordInput,
                            onPasswordChange = { passwordInput = it },
                            autoLoginChecked = autoLoginEnabled,
                            onAutoLoginCheckedChange = { autoLoginEnabled = it },
                            error = loginError,
                            onBackClick = { currentScreen = "PORTAL_SELECTION" },
                            onLoginClick = {
                                if (selectedRole == "MANAGEMENT") {
                                    val u = usernameInput.trim().lowercase()
                                    if (u.isBlank()) {
                                        loginError = "Please enter your username or registered email!"
                                    } else {
                                        if (u == "admin") {
                                            if (passwordInput == "admin123") {
                                                loggedInManagerUsername = u
                                                viewModel.registerManagementUser(u)
                                                saveCredentialsIfEnabled(selectedRole, usernameInput, passwordInput)
                                                currentScreen = "MAIN"
                                            } else {
                                                loginError = "Incorrect password for admin user!"
                                            }
                                        } else {
                                            // Validate registered email/username list
                                            val registered = viewModel.getRegisteredEmails()
                                            if (!registered.contains(u)) {
                                                loginError = "This user/email is not authorized for management access. Please contact an admin."
                                            } else {
                                                // Check if first-time password setup is required
                                                if (viewModel.isPasswordSetupRequired(u)) {
                                                    setupPasswordInput = ""
                                                    setupConfirmPasswordInput = ""
                                                    setupErrorMsg = ""
                                                    showFirstTimeSetupDialog = true
                                                } else {
                                                    val savedPass = viewModel.getMgmtPassword(u)
                                                    if (passwordInput == savedPass) {
                                                        loggedInManagerUsername = u
                                                        viewModel.registerManagementUser(u)
                                                        saveCredentialsIfEnabled(selectedRole, usernameInput, passwordInput)
                                                        currentScreen = "MAIN"
                                                    } else {
                                                        loginError = "Incorrect password! Please try again."
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // For Rider
                                    if (usernameInput.isBlank()) {
                                        loginError = "Please enter your Phone or License Plate!"
                                    } else {
                                        viewModel.selectBike(usernameInput)
                                        saveCredentialsIfEnabled(selectedRole, usernameInput, passwordInput)
                                        currentScreen = "MAIN"
                                    }
                                }
                            }
                        )

                        // First-Time Setup Dialog
                        if (showFirstTimeSetupDialog) {
                            AlertDialog(
                                onDismissRequest = { showFirstTimeSetupDialog = false },
                                title = {
                                    Text(
                                        "First-Time Login Setup",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MotorOrange
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            "Your account has been authorized for management access. Since this is your first login, please set up a secure password for future use.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        OutlinedTextField(
                                            value = setupPasswordInput,
                                            onValueChange = { setupPasswordInput = it },
                                            label = { Text("New Password") },
                                            colors = customTextFieldColors(),
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = setupConfirmPasswordInput,
                                            onValueChange = { setupConfirmPasswordInput = it },
                                            label = { Text("Confirm Password") },
                                            colors = customTextFieldColors(),
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (setupErrorMsg.isNotBlank()) {
                                            Text(
                                                text = setupErrorMsg,
                                                color = Color(0xFFEF4444),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val pass = setupPasswordInput.trim()
                                            val conf = setupConfirmPasswordInput.trim()
                                            if (pass.isEmpty()) {
                                                setupErrorMsg = "Password cannot be empty!"
                                            } else if (pass.length < 4) {
                                                setupErrorMsg = "Password must be at least 4 characters long!"
                                            } else if (pass != conf) {
                                                setupErrorMsg = "Passwords do not match!"
                                            } else {
                                                val u = usernameInput.trim().lowercase()
                                                viewModel.setMgmtPassword(u, pass)
                                                loggedInManagerUsername = u
                                                viewModel.registerManagementUser(u)
                                                saveCredentialsIfEnabled(selectedRole, usernameInput, pass)
                                                showFirstTimeSetupDialog = false
                                                currentScreen = "MAIN"
                                                Toast.makeText(context, "Password setup successful!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                                    ) {
                                        Text("SAVE & LOGIN", fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showFirstTimeSetupDialog = false }) {
                                        Text("CANCEL", color = TextSecondary)
                                    }
                                },
                                containerColor = SurfaceDark,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                    "MAIN" -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (selectedRole == "RIDER") {
                                RiderPortalView(
                                    viewModel = viewModel,
                                    activeMenu = activeRiderMenu,
                                    onActiveMenuChange = { activeRiderMenu = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                ManagementPortalView(
                                    viewModel = viewModel,
                                    activeMenu = activeManagementMenu,
                                    serviceQueueMode = serviceQueueMode,
                                    onServiceQueueModeChange = { serviceQueueMode = it },
                                    managerUsername = loggedInManagerUsername,
                                    onActiveMenuChange = { activeManagementMenu = it },
                                    onAddBikeClick = { showAddBikeDialog = true },
                                    onAddServiceClick = { showAddServiceDialog = true },
                                    onEditLogoClick = { showLogoDialog = true },
                                    showFirebaseSettings = showFirebaseSettingsInMain,
                                    onDismissFirebaseSettings = { showFirebaseSettingsInMain = false },
                                    showPerformanceSettings = showPerformanceSettingsInMain,
                                    onDismissPerformanceSettings = { showPerformanceSettingsInMain = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Notification Banner overlay for Riders when slots go live!
                            if (selectedRole == "RIDER" && liveNotificationMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    border = BorderStroke(2.dp, MotorOrange),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .animateContentSize()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Alert",
                                            tint = MotorOrange,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "NOTIFICATION FROM GARAGE",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MotorOrange
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = liveNotificationMessage!!,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        IconButton(onClick = { viewModel.clearLiveNotification() }) {
                                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    // Dialogs
    if (showAddBikeDialog) {
        AddBikeDialog(
            onDismiss = { showAddBikeDialog = false },
            onSave = { bike ->
                viewModel.addBike(bike)
                showAddBikeDialog = false
            }
        )
    }

    if (showAddServiceDialog) {
        AddServiceRecordDialog(
            bikes = bikes,
            onDismiss = { showAddServiceDialog = false },
            onSave = { record ->
                viewModel.addServiceRecord(record)
                showAddServiceDialog = false
            }
        )
    }

    if (showLogoDialog) {
        Dialog(onDismissRequest = { showLogoDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Customize Logo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Current Logo Preview
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, MotorOrange, RoundedCornerShape(16.dp))
                            .background(SlateDark),
                        contentAlignment = Alignment.Center
                    ) {
                        MotorcycleLogoIcon(
                            modifier = Modifier.size(100.dp),
                            viewModel = viewModel
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            logoPickerLauncher.launch("image/*")
                            showLogoDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                        modifier = Modifier.fillMaxWidth().testTag("upload_logo_gallery_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload New Photo", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            viewModel.updateCustomLogoBase64(null)
                            Toast.makeText(context, "Logo reset to default!", Toast.LENGTH_SHORT).show()
                            showLogoDialog = false
                        },
                        border = BorderStroke(1.dp, BorderColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("reset_logo_default_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset to Default")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { showLogoDialog = false }
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        }
    }
}

// Custom App Logo Image
@Composable
fun MotorcycleLogoIcon(
    modifier: Modifier = Modifier,
    viewModel: MotorcycleViewModel? = null
) {
    val context = LocalContext.current
    val customLogoBase64State = if (viewModel != null) {
        viewModel.customLogoBase64.collectAsState()
    } else {
        remember {
            val sharedPrefs = context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
            mutableStateOf(sharedPrefs.getString("custom_logo_base64", null))
        }
    }
    val customLogoBase64 = customLogoBase64State.value

    val bitmap = remember(customLogoBase64) {
        if (!customLogoBase64.isNullOrEmpty()) {
            try {
                val decodedBytes = Base64.decode(customLogoBase64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                bmp?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Advance Auto Logo",
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = R.drawable.img_app_logo),
            contentDescription = "Advance Auto Logo",
            modifier = modifier
        )
    }
}

// --------------------- RIDERS PORTAL VIEW ---------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderPortalView(
    viewModel: MotorcycleViewModel,
    activeMenu: String,
    onActiveMenuChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBike by viewModel.selectedBike.collectAsState()
    val selectedBikePlate by viewModel.selectedBikePlate.collectAsState()
    val selectedRiderMapping by viewModel.selectedRiderMapping.collectAsState()
    val serviceHistory by viewModel.serviceRecordsForSelectedBike.collectAsState()
    val appointments by viewModel.appointmentsForSelectedBike.collectAsState()
    val photoUploads by viewModel.photoUploadsForSelectedBike.collectAsState()
    val isVerifyingPhoto by viewModel.isVerifyingPhoto.collectAsState()
    val photoVerificationError by viewModel.photoVerificationError.collectAsState()

    val releaseHour by viewModel.releaseHour.collectAsState()
    val releaseMinute by viewModel.releaseMinute.collectAsState()
    val releaseAmPm by viewModel.releaseAmPm.collectAsState()
    val releaseDaysOffset by viewModel.releaseDaysOffset.collectAsState()
    val appointmentsPerDay by viewModel.appointmentsPerDay.collectAsState()
    val rebookingIntervalDays by viewModel.rebookingIntervalDays.collectAsState()
    val liveNotificationMessage by viewModel.liveNotificationMessage.collectAsState()
    val serviceIntervalKm by viewModel.serviceIntervalKm.collectAsState()
    val serviceIntervalDays by viewModel.serviceIntervalDays.collectAsState()
    val sentServiceAlerts by viewModel.sentServiceAlerts.collectAsState()

    var showBookAppointmentDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCameraSimDialog by remember { mutableStateOf(false) }
    var activePerspectiveIndex by remember { mutableStateOf<Int?>(null) }
    var activePerspectiveName by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        val idx = activePerspectiveIndex
        if (bitmap != null && idx != null) {
            try {
                // Resize to 1080px wide maintaining aspect ratio
                val resizedBitmap = if (bitmap.width > 1080) {
                    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                    val targetHeight = (1080 * aspectRatio).toInt()
                    android.graphics.Bitmap.createScaledBitmap(bitmap, 1080, targetHeight, true)
                } else {
                    bitmap
                }
                val file = java.io.File(context.cacheDir, "rider_verification_${idx}_${System.currentTimeMillis()}.jpg")
                java.io.FileOutputStream(file).use { out ->
                    resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                }
                viewModel.uploadRiderPhoto(file.absolutePath)
                Toast.makeText(context, "Photo captured successfully (Compressed & Resized)!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save captured photo: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else if (idx != null) {
            // Fallback to high-quality simulation if camera is unavailable or canceled in this non-hardware container environment
            showCameraSimDialog = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scope.launch {
                kotlinx.coroutines.delay(300L) // Safe post-resume delay
                try {
                    cameraLauncher.launch()
                } catch (e: Exception) {
                    Log.e("MainScreen", "Error launching camera after permission grant", e)
                    Toast.makeText(context, "Permission granted! Please tap again to open the camera.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show()
        }
    }

    // Photos state
    val currentPlate = selectedBikePlate ?: ""
    val (startDay, endDay) = viewModel.getRiderUploadCycle(currentPlate)
    val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val isPastEndDay = todayDay > endDay
    val uploadedCount = photoUploads.size
    val isRestrained = isPastEndDay && (uploadedCount < 4)

    var restrictionErrorMessage by remember { mutableStateOf<String?>(null) }

    val navigateToSection: (String) -> Unit = { section ->
        if (section == "OIL" || section == "APPOINTMENT_SECTION") {
            if (isRestrained) {
                restrictionErrorMessage = "Verification photos of bike are still pending, please upload now to use these services"
            } else {
                onActiveMenuChange(section)
            }
        } else {
            onActiveMenuChange(section)
        }
    }

    if (isVerifyingPhoto) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = MotorOrange, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Verifying Photo Quality...",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing brightness, blurriness, and motorcycle model via Gemini AI.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (photoVerificationError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPhotoVerificationError() },
            icon = { Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp)) },
            title = { Text("Photo Rejected", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(photoVerificationError!!, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearPhotoVerificationError() },
                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                ) {
                    Text("OK", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            containerColor = SurfaceDarkElevated
        )
    }

    if (restrictionErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { restrictionErrorMessage = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Access Restricted", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(restrictionErrorMessage!!, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        restrictionErrorMessage = null
                        onActiveMenuChange("VEHICLE_CHECK")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                ) {
                    Text("Upload Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { restrictionErrorMessage = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = SurfaceDarkElevated
        )
    }

    if (showCameraSimDialog && activePerspectiveIndex != null) {
        Dialog(onDismissRequest = { showCameraSimDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF333333)),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAMERA VIEWFINDER",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { showCameraSimDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Position motorcycle for: ${activePerspectiveName}",
                        fontSize = 12.sp,
                        color = MotorOrange,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Viewfinder Screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF111111))
                            .border(1.dp, Color.DarkGray),
                        contentAlignment = Alignment.Center
                    ) {
                        // Camera Grid Lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Vertical lines
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(w / 3f, 0f), Offset(w / 3f, h), 1f)
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(2f * w / 3f, 0f), Offset(2f * w / 3f, h), 1f)
                            // Horizontal lines
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, h / 3f), Offset(w, h / 3f), 1f)
                            drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), 1f)
                        }
                        
                        // Icon of a bike inside viewfinder
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = null,
                                tint = MotorOrange.copy(alpha = 0.7f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "LENS PREVIEW: LIVE FOCUS",
                                fontSize = 10.sp,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // REC Badge
                        Row(
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                            .clickable {
                                // Take simulated picture and compress to 1080px wide 70% JPEG
                                val idx = activePerspectiveIndex!!
                                val width = 1080
                                val height = 810 // 4:3 ratio
                                val simBitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(simBitmap)
                                val paint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.DKGRAY
                                }
                                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
                                
                                val accentPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.parseColor("#F97316") // MotorOrange
                                    style = android.graphics.Paint.Style.FILL
                                    isAntiAlias = true
                                }
                                canvas.drawCircle(300f, 500f, 120f, accentPaint)
                                canvas.drawCircle(780f, 500f, 120f, accentPaint)

                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 36f
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    isAntiAlias = true
                                }
                                canvas.drawText("BIKE VERIFICATION: ${activePerspectiveName.uppercase()}", 540f, 220f, textPaint)
                                canvas.drawText("LOCAL COMPRESSION: 70% | WIDTH: 1080PX", 540f, 280f, textPaint)

                                val file = java.io.File(context.cacheDir, "simulated_verification_${idx}_${System.currentTimeMillis()}.jpg")
                                try {
                                    java.io.FileOutputStream(file).use { out ->
                                        simBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, out)
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                viewModel.uploadRiderPhoto(file.absolutePath)
                                Toast.makeText(context, "📸 Photo Captured & Compressed locally!", Toast.LENGTH_SHORT).show()
                                showCameraSimDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "PRESS RED SHUTTER TO CAPTURE",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    when (activeMenu) {
        "DASHBOARD" -> {
            val latestOilChange = serviceHistory.filter { 
                it.type == "OIL_CHANGE" || it.notes.uppercase().contains("OIL") 
            }.maxByOrNull { it.date }

            val daysSinceLastOilChange = if (latestOilChange != null) {
                val diffMs = System.currentTimeMillis() - latestOilChange.date
                (diffMs / (1000 * 60 * 60 * 24)).toInt()
            } else {
                null
            }

            val isOilChangeNotificationActive = daysSinceLastOilChange != null && daysSinceLastOilChange > 10
            val isPhotoNotificationActive = todayDay >= startDay && uploadedCount < 4
            val isAppointmentNotificationActive = liveNotificationMessage != null

            // Service Overdue calculations for Rider
            val riderLastServiceRecord = serviceHistory.maxByOrNull { it.date }
            val riderLastServiceOdometer = riderLastServiceRecord?.odometer ?: 0
            val riderCurrentBikeOdometer = selectedBike?.currentMileage ?: 0
            val riderMileageSinceLastService = riderCurrentBikeOdometer - riderLastServiceOdometer
            val riderDaysSinceLastService = if (riderLastServiceRecord != null) {
                ((System.currentTimeMillis() - riderLastServiceRecord.date) / (1000 * 60 * 60 * 24)).toInt()
            } else {
                -1
            }
            val isRiderOverdueByMileage = riderMileageSinceLastService >= serviceIntervalKm
            val isRiderOverdueByTime = riderLastServiceRecord != null && riderDaysSinceLastService >= serviceIntervalDays
            val isRiderNeverServiced = serviceHistory.isEmpty()
            val hasManagerServiceAlert = selectedBikePlate != null && sentServiceAlerts.containsKey(selectedBikePlate!!.trim().uppercase())
            val managerServiceAlertMsg = if (selectedBikePlate != null) sentServiceAlerts[selectedBikePlate!!.trim().uppercase()] else null
            val isServiceOverdueNotificationActive = false

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header card with Rider Info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "RIDER ACCOUNT PROFILE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MotorOrange
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val name = selectedRiderMapping?.riderName ?: selectedBike?.ownerName ?: "Guest Rider"
                            val riderId = selectedRiderMapping?.riderId ?: "N/A (No Mapping)"
                            val bikePlate = selectedBikePlate ?: "No Bike Selected"
                            val bikeModel = selectedBike?.model ?: "Mapped Motorcycle"

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Full Name", fontSize = 11.sp, color = TextSecondary)
                                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Rider ID", fontSize = 11.sp, color = TextSecondary)
                                    Text(riderId, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Current Bike Plate", fontSize = 11.sp, color = TextSecondary)
                                    Text(bikePlate, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MotorOrange)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Bike Model", fontSize = 11.sp, color = TextSecondary)
                                    Text(bikeModel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                // Unified Active Notifications Hub
                item {
                    val upcomingScheduledAppts = remember(appointments) {
                        val now = System.currentTimeMillis()
                        val fortyEightHoursMs = 48 * 3600 * 1000L
                        appointments.filter { appt ->
                            (appt.status == "CONFIRMED" || appt.status == "PENDING") &&
                                    appt.preferredDate >= (now - 12 * 3600 * 1000L) &&
                                    appt.preferredDate <= (now + fortyEightHoursMs)
                        }
                    }
                    val isUpcomingApptNotificationActive = upcomingScheduledAppts.isNotEmpty()

                    val activeNotificationsCount = (if (isAppointmentNotificationActive) 1 else 0) +
                            (if (isPhotoNotificationActive) 1 else 0) +
                            (if (isOilChangeNotificationActive) 1 else 0) +
                            (if (isServiceOverdueNotificationActive) 1 else 0) +
                            (if (isUpcomingApptNotificationActive) 1 else 0)

                    if (activeNotificationsCount > 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = MotorOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ACTIVE NOTIFICATIONS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MotorOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$activeNotificationsCount NEW",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotorOrange
                                    )
                                }
                            }

                            // 0. Upcoming Service Appointment Reminder Notification
                            if (isUpcomingApptNotificationActive) {
                                val firstAppt = upcomingScheduledAppts.first()
                                val apptDateStr = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(Date(firstAppt.preferredDate))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B3822)),
                                    border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Upcoming Appointment Reminder",
                                                tint = Color(0xFF4CAF50),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "⏰ Upcoming Service Appointment (${upcomingScheduledAppts.size})",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF81C784)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Reminder: Service for bike ${firstAppt.bikePlate} is scheduled on $apptDateStr (${firstAppt.serviceType.replace("_", " ")}).",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    val success = com.example.util.AppointmentNotificationHelper
                                                        .sendAppointmentReminderNotification(context, firstAppt)
                                                    Toast.makeText(
                                                        context,
                                                        if (success) "🔔 Reminder notification posted!" else "Reminder notification triggered",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF81C784), modifier = Modifier.size(14.dp))
                                                    Text("Trigger System Reminder", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(
                                                onClick = { navigateToSection("APPOINTMENT_SECTION") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("View Appointments", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // 1. New Appointments Released Notification
                            if (isAppointmentNotificationActive && liveNotificationMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    border = BorderStroke(1.dp, MotorOrange),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Notifications,
                                                contentDescription = "Slots Released Alert",
                                                tint = MotorOrange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "New Appointments Released",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MotorOrange
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = liveNotificationMessage!!,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { navigateToSection("APPOINTMENT_SECTION") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Book Slots Now", color = MotorOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            TextButton(
                                                onClick = { viewModel.clearLiveNotification() },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Dismiss", color = TextSecondary, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Photo Upload Cycle Pending Notification
                            if (isPhotoNotificationActive) {
                                val borderCol = if (isRestrained) MaterialTheme.colorScheme.error else Color(0xFF4A90E2)
                                val bgCol = if (isRestrained) MaterialTheme.colorScheme.errorContainer else Color(0xFF1E3A5F).copy(alpha = 0.4f)
                                val titleCol = if (isRestrained) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF90CAF9)
                                
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = bgCol),
                                    border = BorderStroke(1.dp, borderCol),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = if (isRestrained) Icons.Default.Warning else Icons.Default.Info,
                                                contentDescription = "Photo Upload Alert",
                                                tint = if (isRestrained) MaterialTheme.colorScheme.error else Color(0xFF90CAF9),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isRestrained) "Monthly Photos Overdue!" else "Monthly Upload Cycle Active",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = titleCol
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isRestrained) {
                                                        "Your upload window (Date $startDay to $endDay) has passed. Upload 4 photos immediately to regain portal services."
                                                    } else {
                                                        "Your scheduled photo upload cycle is active (Date $startDay to $endDay). Upload all 4 verification photos now."
                                                    },
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { navigateToSection("VEHICLE_CHECK") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Upload Photos ($uploadedCount/4)", color = if (isRestrained) MaterialTheme.colorScheme.error else Color(0xFF90CAF9), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Oil Change Warning Notification
                            if (isOilChangeNotificationActive && latestOilChange != null) {
                                val oilDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(latestOilChange.date))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ColorOilChange.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, ColorOilChange),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Oil Change Alert",
                                                tint = ColorOilChange,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Engine Oil Warning",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = ColorOilChange
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "It has been $daysSinceLastOilChange days since your last oil change (on $oilDateStr). Verify your current kilometers immediately to avoid late oil replacement.",
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { navigateToSection("OIL") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("View Oil History", color = ColorOilChange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Motorcycle Service Overdue Notification
                            if (isServiceOverdueNotificationActive) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("service_overdue_rider_notification")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Service Overdue Alert",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Motorcycle Service Overdue!",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFEF4444)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val detailText = when {
                                                    isRiderNeverServiced -> {
                                                        "Your motorcycle (${selectedBikePlate ?: ""}) has no recorded service history. Please book an initial inspection service."
                                                    }
                                                    isRiderOverdueByMileage && isRiderOverdueByTime -> {
                                                        "Overdue by mileage AND time! Run: $riderMileageSinceLastService km (Interval: $serviceIntervalKm km) and $riderDaysSinceLastService days elapsed (Interval: $serviceIntervalDays days)."
                                                    }
                                                    isRiderOverdueByMileage -> {
                                                        "Overdue by mileage! You have run $riderMileageSinceLastService km since last service (Interval limit: $serviceIntervalKm km)."
                                                    }
                                                    isRiderOverdueByTime -> {
                                                        "Overdue by time! It has been $riderDaysSinceLastService days since last service (Interval limit: $serviceIntervalDays days)."
                                                    }
                                                    else -> {
                                                        "Your motorcycle requires regular maintenance checks."
                                                    }
                                                }
                                                Text(
                                                    text = detailText,
                                                    fontSize = 11.sp,
                                                    color = TextPrimary
                                                )

                                                if (hasManagerServiceAlert && managerServiceAlertMsg != null) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                                            .padding(8.dp)
                                                    ) {
                                                        Text(
                                                            text = "Workshop Supervisor Notice:\n$managerServiceAlertMsg",
                                                            fontSize = 10.sp,
                                                            color = TextPrimary,
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { navigateToSection("APPOINTMENT_SECTION") },
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Book Service Now", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (hasManagerServiceAlert) {
                                                Spacer(modifier = Modifier.width(8.dp))
                                                TextButton(
                                                    onClick = { selectedBikePlate?.let { viewModel.clearServiceAlert(it) } },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Acknowledge", color = TextSecondary, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Grid of options
                item {
                    Text(
                        text = "PORTAL SERVICES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // 1. Oil History Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigateToSection("OIL") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorOilChange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = ColorOilChange)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Recent Oil History", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("See last 5 oil changes of your current bike", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 2. Appointment Booking Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigateToSection("APPOINTMENT_SECTION") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MotorOrange.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = MotorOrange)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Appointment Section", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Book timeslots and inspect next release countdown", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 3. Vehicle Check Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navigateToSection("VEHICLE_CHECK") }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorServiceWithoutParts.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = ColorServiceWithoutParts)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Vehicle Check", fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("Upload monthly safety check photos ($uploadedCount of 4 uploaded)", fontSize = 11.sp, color = TextSecondary)
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        "OIL" -> {
            val oilChanges = serviceHistory.filter { 
                it.type == "OIL_CHANGE" || it.notes.uppercase().contains("OIL") 
            }.take(5)

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "LAST 5 OIL CHANGES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange
                    )
                }

                if (oilChanges.isEmpty()) {
                    item {
                        EmptyStateCard(
                            message = "No oil changes recorded for this bike yet. Records will appear here when garage team updates them.",
                            icon = Icons.Default.Info
                        )
                    }
                } else {
                    items(oilChanges) { record ->
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(record.date))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Date: $dateStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ColorOilChange.copy(alpha = 0.2f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("OIL CHANGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorOilChange)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Mileage: ${record.odometer} KM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notes: ${record.notes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cost: AED ${record.cost}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }
                }
            }
        }

        "APPOINTMENT_SECTION" -> {
            var countdownText by remember { mutableStateOf("") }
            LaunchedEffect(releaseHour, releaseMinute, releaseAmPm) {
                while (true) {
                    val target = getNextReleaseTimeMillis(releaseHour, releaseMinute, releaseAmPm)
                    val diff = target - System.currentTimeMillis()
                    if (diff <= 0) {
                        countdownText = "00:00:00"
                    } else {
                        val hrs = diff / (1000 * 60 * 60)
                        val mins = (diff % (1000 * 60 * 60)) / (1000 * 60)
                        val secs = (diff % (1000 * 60)) / 1000
                        countdownText = String.format("%02d:%02d:%02d", hrs, mins, secs)
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            val activeBooking = appointments.find { it.status == "CONFIRMED" || it.status == "PENDING" }
            val completedAppts = appointments.filter { it.status == "COMPLETED" || it.status == "ATTENDED" }
            val lastCompleted = completedAppts.maxByOrNull { it.preferredDate }

            val daysPassed = if (lastCompleted != null) {
                ((System.currentTimeMillis() - lastCompleted.preferredDate) / (1000 * 60 * 60 * 24)).toInt()
            } else {
                999
            }
            val isBlockedByInterval = daysPassed < rebookingIntervalDays
            val daysRemaining = rebookingIntervalDays - daysPassed

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Countdown card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NEXT SLOTS RELEASING IN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlateDark)
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = countdownText,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MotorOrange,
                                    letterSpacing = 2.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Daily Release: $releaseHour:${String.format("%02d", releaseMinute)} $releaseAmPm (offset: $releaseDaysOffset days, max $appointmentsPerDay per day)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Booking restrictions or action
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "BOOK SERVICE APPOINTMENT",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            if (activeBooking != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f))
                                        .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "You currently have a confirmed appointment scheduled. You are not allowed to book a new appointment until your scheduled appointment is completed and attended.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            } else if (isBlockedByInterval) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorOilChange.copy(alpha = 0.15f))
                                        .border(1.dp, ColorOilChange, RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "Rebooking limit restriction: You must wait at least $rebookingIntervalDays days between appointments. You have $daysRemaining days remaining before you can book another appointment.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            } else {
                                Text(
                                    text = "No booking limits active! You are eligible to book a new service session now.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showBookAppointmentDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("book_appointment_button")
                                ) {
                                    Text("Select Date & Timeslot", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Show active/future appointments for rider
                if (appointments.isNotEmpty()) {
                    item {
                        Text(
                            text = "Your Appointment Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(appointments) { appointment ->
                        RiderAppointmentItem(appointment)
                    }
                }
            }
        }

        "VEHICLE_CHECK" -> {
            val months = listOf("Front side of the bike", "Back side of the bike", "Right side of the bike", "Left side of the bike")
            val photosCount = photoUploads.size

            LazyColumn(
                modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Monthly Inspection Verification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange
                    )
                }

                // Upload progress summary
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Inspection Photo Progress", fontWeight = FontWeight.Bold, color = TextPrimary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (photosCount >= 4) Color(0xFF2E7D32) else MotorOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (photosCount >= 4) "COMPLETED" else "INCOMPLETE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (photosCount >= 4) Color(0xFFE8F5E9) else MotorOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { (photosCount / 4f).coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = if (photosCount >= 4) Color(0xFF81C784) else MotorOrange,
                                trackColor = BorderColor
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$photosCount of 4 photos uploaded for calendar month ${viewModel.currentCalendarMonth}. Photos must be uploaded during your personal cycle: Date $startDay to $endDay.",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Action to simulate upload
                if (photosCount < 4) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Capture Perspective using Camera:", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                months.forEachIndexed { idx, perspective ->
                                    val isUploaded = idx < photosCount
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isUploaded) Color(0xFF1B3B2B) else SurfaceDark)
                                            .clickable(!isUploaded) {
                                                activePerspectiveIndex = idx
                                                activePerspectiveName = perspective
                                                try {
                                                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                                        context,
                                                        android.Manifest.permission.CAMERA
                                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                                    if (hasPermission) {
                                                        try {
                                                            cameraLauncher.launch()
                                                        } catch (e: Exception) {
                                                            Log.e("MainScreen", "Error launching camera", e)
                                                            Toast.makeText(context, "Failed to start camera app. Opening simulator...", Toast.LENGTH_SHORT).show()
                                                            showCameraSimDialog = true
                                                        }
                                                    } else {
                                                        try {
                                                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                                        } catch (e: Exception) {
                                                            Log.e("MainScreen", "Error launching camera permission", e)
                                                            Toast.makeText(context, "Permission request failed. Opening simulator...", Toast.LENGTH_SHORT).show()
                                                            showCameraSimDialog = true
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e("MainScreen", "Error checking permission", e)
                                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Photo ${idx + 1}: $perspective",
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isUploaded) Color(0xFF81C784) else TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Icon(
                                            imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.Add,
                                            contentDescription = null,
                                            tint = if (isUploaded) Color(0xFF81C784) else MotorOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        val aiLoading by viewModel.aiAssessmentLoading.collectAsState()
                        val aiResult by viewModel.aiAssessmentResult.collectAsState()
                        val aiError by viewModel.aiAssessmentError.collectAsState()
                        val currentPlate = selectedBikePlate ?: ""
                        var localAssessment by remember(currentPlate) {
                            mutableStateOf(viewModel.getAiAssessment(currentPlate))
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, if (localAssessment != null || aiResult != null) Color(0xFF2E7D32) else MotorOrange.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "🤖 AI VEHICLE CLEANLINESS & DAMAGE ANALYSIS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MotorOrange
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (aiLoading) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = MotorOrange, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Analyzing uploaded photos...",
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Running computer vision models to score cleanliness and log scratches/dents...",
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else if (localAssessment == null && aiResult == null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "All 5 photos successfully uploaded! Click below to run the AI system inspection.",
                                            fontSize = 12.sp,
                                            color = TextPrimary,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                viewModel.runAiAssessmentForBike(currentPlate) {
                                                    localAssessment = viewModel.getAiAssessment(currentPlate)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("ai_submit_button")
                                        ) {
                                            Text("Submit for AI Inspection", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    val stars = localAssessment?.stars ?: aiResult?.stars ?: 0f
                                    val cleanlinessFeedback = localAssessment?.cleanlinessFeedback ?: aiResult?.cleanlinessFeedback ?: ""
                                    val damageFeedback = localAssessment?.damageFeedback ?: aiResult?.damageFeedback ?: ""
                                    
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Cleanliness Score:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                repeat(5) { index ->
                                                    val starIdx = index + 1
                                                    val isFilled = starIdx <= stars.toInt()
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        tint = if (isFilled) Color(0xFFEAB308) else Color.DarkGray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${stars}/5",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEAB308),
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Cleanliness Assessment (Included in Score):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
                                        Text(cleanlinessFeedback, fontSize = 12.sp, color = TextPrimary)
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Text("Structural Damage Check (NOT Included in Score):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFEF4444))
                                        Text(damageFeedback, fontSize = 12.sp, color = TextPrimary)

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF1B3B2B))
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🎉 Month's Verification Complete",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color(0xFF81C784)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Uploaded Photos Grid
                if (photoUploads.isNotEmpty()) {
                    item {
                        Text("Uploaded Verification Photos", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    items(photoUploads) { upload ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, BorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(SlateDark),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏍️", fontSize = 24.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Verification Photo ID: #${upload.id}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                        if (upload.rating != null) {
                                            Text(
                                                text = "⭐ Condition Rating: ${String.format("%.1f", upload.rating)}/10.0",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MotorOrange,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                        if (!upload.assessmentSummary.isNullOrEmpty()) {
                                            Text(
                                                text = "Assessment: ${upload.assessmentSummary}",
                                                fontSize = 11.sp,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "Uploaded: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(upload.uploadTimestamp)}",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteRiderPhoto(upload) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Photo", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val allAppointments by viewModel.appointments.collectAsState()
    if (showBookAppointmentDialog) {
        BookAppointmentDialog(
            viewModel = viewModel,
            defaultPlate = selectedBikePlate ?: "",
            allAppointments = allAppointments,
            rebookingIntervalDays = rebookingIntervalDays,
            appointmentsPerDay = appointmentsPerDay,
            onDismiss = { showBookAppointmentDialog = false },
            onSave = { appointment ->
                viewModel.addAppointment(appointment)
                showBookAppointmentDialog = false
            }
        )
    }
}

private fun getNextReleaseTimeMillis(releaseHour: Int, releaseMinute: Int, releaseAmPm: String): Long {
    val cal = Calendar.getInstance()
    val hour24 = if (releaseAmPm.uppercase() == "PM") {
        if (releaseHour < 12) releaseHour + 12 else 12
    } else {
        if (releaseHour == 12) 0 else releaseHour
    }
    cal.set(Calendar.HOUR_OF_DAY, hour24)
    cal.set(Calendar.MINUTE, releaseMinute)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val now = System.currentTimeMillis()
    if (cal.timeInMillis <= now) {
        cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return cal.timeInMillis
}

@Composable
fun BikeDetailsHeroCard(bike: Bike, onClear: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MotorOrange)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bike.licensePlate,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bike.model,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Year ${bike.year}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Owner Name", fontSize = 11.sp, color = TextSecondary)
                    Text(bike.ownerName, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Contact Phone", fontSize = 11.sp, color = TextSecondary)
                    Text(bike.ownerPhone, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun OilChangeDiagnosticsCard(
    oilChanges: List<ServiceRecord>,
    currentBike: Bike
) {
    val lastOilChange = oilChanges.maxByOrNull { it.date }
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ColorOilChange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Oil Diagnostic",
                        tint = ColorOilChange,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Oil Health Diagnostics",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Real-time engine lubricant status",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (lastOilChange == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDarkElevated)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "⚠️ No recorded oil changes found for this bike. We highly recommend scheduling an initial oil change to ensure peak engine lubrication.",
                        fontSize = 12.sp,
                        color = MotorAmber,
                        lineHeight = 18.sp
                    )
                }
            } else {
                // Calculate mileage/days since last change
                val daysPassed = ((System.currentTimeMillis() - lastOilChange.date) / (1000 * 60 * 60 * 24)).toInt()
                // Recommended change interval is typically 3000 km/miles or 180 days
                val healthPercentage = (100 - (daysPassed * 0.55f)).coerceIn(0f, 100f).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Last Change Odometer",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "${lastOilChange.odometer} km",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Completed on ${formatter.format(Date(lastOilChange.date))} ($daysPassed days ago)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Oil details: ${lastOilChange.partsDetails ?: "Standard Oil"}",
                            fontSize = 11.sp,
                            color = MotorOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Oil Health Circle Gauge
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(72.dp)
                        ) {
                            val currentBorderColor = BorderColor
                            val strokeColor = when {
                                healthPercentage > 70 -> ColorServiceWithoutParts // Green
                                healthPercentage > 35 -> MotorAmber // Yellow
                                else -> Color(0xFFEF4444) // Red
                            }

                            Canvas(modifier = Modifier.size(72.dp)) {
                                drawArc(
                                    color = currentBorderColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx())
                                )
                                drawArc(
                                    color = strokeColor,
                                    startAngle = -90f,
                                    sweepAngle = (healthPercentage / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Text(
                                text = "$healthPercentage%",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (healthPercentage > 70) "Good Health" else if (healthPercentage > 35) "Service Soon" else "Change Oil!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (healthPercentage > 70) ColorServiceWithoutParts else if (healthPercentage > 35) MotorAmber else Color(0xFFEF4444)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RiderAppointmentItem(appointment: Appointment) {
    val context = LocalContext.current
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val badgeColor = when (appointment.status) {
        "CONFIRMED" -> ColorServiceWithoutParts
        "COMPLETED" -> ColorServiceWithParts
        "CANCELLED" -> Color(0xFFEF4444)
        else -> MotorAmber
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appointment.serviceType.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            appointment.status,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }

                    if (appointment.status == "PENDING" || appointment.status == "CONFIRMED") {
                        IconButton(
                            onClick = {
                                val success = com.example.util.AppointmentNotificationHelper
                                    .sendAppointmentReminderNotification(context, appointment)
                                if (success) {
                                    Toast.makeText(context, "🔔 Service reminder notification sent!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Reminder notification created", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Send Reminder",
                                tint = MotorOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Appointment No: ${if (appointment.appointmentNumber.isEmpty()) "N/A" else appointment.appointmentNumber}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotorOrange
                )

                if (appointment.status == "PENDING" || appointment.status == "CONFIRMED") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MotorOrange.copy(alpha = 0.15f))
                            .clickable {
                                val success = com.example.util.AppointmentNotificationHelper
                                    .sendAppointmentReminderNotification(context, appointment)
                                Toast.makeText(
                                    context,
                                    if (success) "⏰ Appointment reminder triggered!" else "Reminder scheduled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(12.dp))
                            Text("Remind Me", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MotorOrange)
                        }
                    }
                }
            }

            val notesText = appointment.notes
            val slotPrefix = if (notesText.startsWith("[Slot ")) {
                val endIdx = notesText.indexOf("]")
                if (endIdx != -1) {
                    notesText.substring(0, endIdx + 1)
                } else null
            } else if (notesText.startsWith("[Manual: ")) {
                val endIdx = notesText.indexOf("]")
                if (endIdx != -1) {
                    notesText.substring(0, endIdx + 1)
                } else null
            } else null

            val displayNotes = if (slotPrefix != null) {
                notesText.substring(slotPrefix.length).trim().removePrefix("|").trim()
            } else {
                notesText
            }

            val timeLabel = if (slotPrefix != null) {
                if (slotPrefix.startsWith("[Manual: ")) {
                    "Time: " + slotPrefix.substringAfter("[Manual: ").removeSuffix("]")
                } else {
                    val match = Regex("\\(([^)]+)\\)").find(slotPrefix)
                    if (match != null) {
                        "Time: " + match.groupValues[1]
                    } else {
                        "Time: Slot " + slotPrefix.filter { it.isDigit() }
                    }
                }
            } else null

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scheduled Date: ${formatter.format(Date(appointment.preferredDate))}" + (if (timeLabel != null) " • $timeLabel" else ""),
                fontSize = 12.sp,
                color = TextPrimary
            )
            if (displayNotes.isNotBlank()) {
                Text(
                    text = "Request notes: \"$displayNotes\"",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServiceHistoryTabularView(
    serviceHistory: List<ServiceRecord>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    val serviceWithParts = remember(serviceHistory) {
        serviceHistory.filter { it.type == "SERVICE_WITH_PARTS" }
    }
    val oilHistory = remember(serviceHistory) {
        serviceHistory.filter { it.type == "OIL_CHANGE" }
    }
    val serviceWithoutParts = remember(serviceHistory) {
        serviceHistory.filter { it.type == "SERVICE_WITHOUT_PARTS" }
    }

    val tabTitles = listOf("Service with parts", "Oil history", "Service without parts")
    val tabColors = listOf(ColorServiceWithParts, ColorOilChange, ColorServiceWithoutParts)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        // Tab Row with Custom Colors & Indicator
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent,
            contentColor = TextPrimary,
            divider = { HorizontalDivider(color = BorderColor.copy(alpha = 0.3f)) },
            indicator = { tabPositions ->
                if (tabPositions.isNotEmpty()) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = tabColors[pagerState.currentPage]
                    )
                }
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (pagerState.currentPage == index) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) { page ->
            when (page) {
                0 -> RecordTable(records = serviceWithParts, isParts = true)
                1 -> RecordTable(records = oilHistory, isParts = false)
                2 -> RecordTable(records = serviceWithoutParts, isParts = false)
            }
        }
    }
}

@Composable
fun RecordTable(records: List<ServiceRecord>, isParts: Boolean) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📋", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("No records found in this category", fontSize = 12.sp, color = TextSecondary)
            }
        }
    } else {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurfaceDark)
        ) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDarkElevated)
                    .border(BorderStroke(0.5.dp, BorderColor))
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Date", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                Text("Odo (km)", modifier = Modifier.weight(1.2f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                Text("Cost", modifier = Modifier.weight(1.0f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                if (isParts) {
                    Text("Parts Used", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                }
                Text("Notes", modifier = Modifier.weight(2.0f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            }

            // Scrollable list of rows
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(records) { record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(0.5.dp, BorderColor.copy(alpha = 0.3f)))
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateStr = dateFormat.format(Date(record.date))
                        Text(dateStr, modifier = Modifier.weight(1.2f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(String.format("%,d", record.odometer), modifier = Modifier.weight(1.2f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
                        Text(String.format("$%,.2f", record.cost), modifier = Modifier.weight(1.0f), fontSize = 11.sp, color = TextPrimary, textAlign = TextAlign.Center)
                        if (isParts) {
                            Text(record.partsDetails ?: "None", modifier = Modifier.weight(1.8f), fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Text(record.notes, modifier = Modifier.weight(2.0f), fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceHistoryTimelineItem(record: ServiceRecord) {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val typeLabel = when (record.type) {
        "SERVICE_WITH_PARTS" -> "Service With Parts"
        "OIL_CHANGE" -> "Oil Change History"
        "SERVICE_WITHOUT_PARTS" -> "Service Without Parts"
        else -> record.type
    }
    val themeColor = when (record.type) {
        "SERVICE_WITH_PARTS" -> ColorServiceWithParts
        "OIL_CHANGE" -> ColorOilChange
        "SERVICE_WITHOUT_PARTS" -> ColorServiceWithoutParts
        else -> Color.White
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Vertical timeline bar decoration
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp, top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .border(BorderStroke(3.dp, themeColor), CircleShape)
                    .background(SlateDark)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(90.dp)
                    .background(BorderColor)
            )
        }

        // Service Content details
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = typeLabel,
                        fontWeight = FontWeight.Black,
                        color = themeColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = formatter.format(Date(record.date)),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Odometer: ${record.odometer} km",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$${String.format(Locale.getDefault(), "%.2f", record.cost)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (!record.partsDetails.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDarkElevated)
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                "Parts Installed:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColor
                            )
                            Text(
                                record.partsDetails,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }

                if (record.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notes: ${record.notes}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextDisabled,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}


// --------------------- GARAGE / STAFF MANAGEMENT VIEW ---------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementPortalView(
    viewModel: MotorcycleViewModel,
    activeMenu: String,
    serviceQueueMode: String = "ADMIN",
    onServiceQueueModeChange: (String) -> Unit = {},
    managerUsername: String = "",
    onActiveMenuChange: (String) -> Unit,
    onAddBikeClick: () -> Unit,
    onAddServiceClick: () -> Unit,
    onEditLogoClick: () -> Unit,
    showFirebaseSettings: Boolean = false,
    onDismissFirebaseSettings: () -> Unit = {},
    showPerformanceSettings: Boolean = false,
    onDismissPerformanceSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bikes by viewModel.bikes.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val bikeRiderMappings by viewModel.bikeRiderMappings.collectAsState()

    // Service History Search states
    var searchPlate by remember { mutableStateOf("") }
    var searchedBike by remember { mutableStateOf<Bike?>(null) }
    var searchedRecords by remember { mutableStateOf<List<ServiceRecord>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    var serviceHistoryTab by remember { mutableStateOf("PARTS") } // "PARTS", "OIL", "NOPARTS"

    // Appointments states
    var appointmentTab by remember { mutableStateOf("CONFIRMED") } // "CONFIRMED", "SETTINGS"

    // Settings from ViewModel
    val rebookingIntervalDays by viewModel.rebookingIntervalDays.collectAsState()
    val appointmentsPerDay by viewModel.appointmentsPerDay.collectAsState()
    val releaseTime by viewModel.releaseTime.collectAsState()
    val releaseHour by viewModel.releaseHour.collectAsState()
    val releaseMinute by viewModel.releaseMinute.collectAsState()
    val releaseAmPm by viewModel.releaseAmPm.collectAsState()
    val releaseDaysOffset by viewModel.releaseDaysOffset.collectAsState()

    val allServiceRecords by viewModel.serviceRecords.collectAsState()
    val serviceIntervalKm by viewModel.serviceIntervalKm.collectAsState()
    val serviceIntervalDays by viewModel.serviceIntervalDays.collectAsState()
    val sentServiceAlerts by viewModel.sentServiceAlerts.collectAsState()

    var showBroadcastSuccess by remember { mutableStateOf(false) }

    // Date filtering and manual booking states
    var selectedFilterDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showDateDropdown by remember { mutableStateOf(false) }
    var showAddManualAppointmentDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(SlateDark)) {
        // Redundant inner sub-header removed. Centrally managed in TopAppBar.


        AnimatedContent(
            targetState = activeMenu,
            transitionSpec = { getStandardScreenTransitionSpec() },
            label = "ManagementMenuTransition"
        ) { menu ->
            when (menu) {
            "DASHBOARD" -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "PORTAL MODULES GRID",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Row 1: Search Service History & Appointments Desk
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardGridCard(
                                title = "Service History Portal",
                                description = "Look up plate numbers and inspect repair categories.",
                                icon = Icons.Default.Search,
                                iconColor = MotorOrange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "SERVICE_HISTORY"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("SERVICE_HISTORY")
                            }

                            DashboardGridCard(
                                title = "Appointment desk",
                                description = "View verified bookings and threshold rules.",
                                icon = Icons.Default.DateRange,
                                iconColor = ColorOilChange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "APPOINTMENTS"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("APPOINTMENTS")
                            }
                        }
                    }

                    // Row 2: Data Input Center & Performance Insights
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardGridCard(
                                title = "Data Input Center",
                                description = "Spreadsheet pasted editor for bulk record entry.",
                                icon = Icons.Default.List,
                                iconColor = MotorOrange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "DATA_INPUT"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("DATA_INPUT")
                            }

                            DashboardGridCard(
                                title = "Performance Insights",
                                description = "Track technician duplicate-controlled metrics.",
                                icon = Icons.Default.Star,
                                iconColor = ColorServiceWithoutParts,
                                enabled = viewModel.getMgmtPermission(managerUsername, "MONTHLY_PERFORMANCE"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("MONTHLY_PERFORMANCE")
                            }
                        }
                    }

                    // Row 3: Staff Management & Access Control
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardGridCard(
                                title = "Staff Management",
                                description = "Manage designation dropdowns, shifts, day-offs, and custom timings.",
                                icon = Icons.Default.Person,
                                iconColor = MotorOrange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "STAFF_MANAGEMENT"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("STAFF_MANAGEMENT")
                            }

                            DashboardGridCard(
                                title = "Access Control",
                                description = "Grant/revoke rider portal feature access (Default: History only).",
                                icon = Icons.Default.Lock,
                                iconColor = Color.Yellow,
                                enabled = viewModel.getMgmtPermission(managerUsername, "ACCESS_CONTROL"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("ACCESS_CONTROL")
                            }
                        }
                    }

                    // Row 4: Garage Traffic Status & Firebase Live Portal
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardGridCard(
                                title = "Garage Traffic Status",
                                description = "Monitor automatic live garage occupancy and view 7-day traffic reports based on 100m geofence detections.",
                                icon = Icons.Default.Info,
                                iconColor = Color(0xFF4CAF50),
                                enabled = viewModel.getMgmtPermission(managerUsername, "GARAGE_TRAFFIC"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("GARAGE_TRAFFIC")
                            }

                            DashboardGridCard(
                                title = "Firebase Live Portal",
                                description = "Verify connections, manage URL configurations, and search live bike credentials.",
                                icon = Icons.Default.Refresh,
                                iconColor = MotorOrange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "FIREBASE_PORTAL"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("FIREBASE_PORTAL")
                            }
                        }
                    }

                    // Row 5: Service Queue Manager
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardGridCard(
                                title = "Service Queue Manager",
                                description = "Real-time FIFO bike queue with single-field entry, TV display screen, and rider tracking.",
                                icon = Icons.Default.List,
                                iconColor = MotorOrange,
                                enabled = viewModel.getMgmtPermission(managerUsername, "SERVICE_QUEUE"),
                                modifier = Modifier.weight(1f)
                            ) {
                                onActiveMenuChange("SERVICE_QUEUE")
                            }

                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            "SERVICE_ALERTS" -> {
                var showUpdateMileageDialog by remember { mutableStateOf(false) }
                var selectedBikeToUpdateMileage by remember { mutableStateOf<Bike?>(null) }
                var newMileageInput by remember { mutableStateOf("") }

                var showSendAlertDialog by remember { mutableStateOf(false) }
                var selectedBikeToSendAlert by remember { mutableStateOf<Bike?>(null) }
                var customAlertMessageInput by remember { mutableStateOf("") }

                var tempKmStr by remember(serviceIntervalKm) { mutableStateOf(serviceIntervalKm.toString()) }
                var tempDaysStr by remember(serviceIntervalDays) { mutableStateOf(serviceIntervalDays.toString()) }

                var filterTab by remember { mutableStateOf("OVERDUE") } // "ALL", "OVERDUE"

                // Dialogs
                if (showUpdateMileageDialog) {
                    Dialog(onDismissRequest = { showUpdateMileageDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Update Odometer Reading", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Plate: ${selectedBikeToUpdateMileage?.licensePlate}\nModel: ${selectedBikeToUpdateMileage?.model}", fontSize = 11.sp, color = TextSecondary)
                                OutlinedTextField(
                                    value = newMileageInput,
                                    onValueChange = { newMileageInput = it },
                                    label = { Text("Current Odometer (km)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = customTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth().testTag("update_mileage_input")
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showUpdateMileageDialog = false }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val bike = selectedBikeToUpdateMileage
                                            val mileage = newMileageInput.toIntOrNull()
                                            if (bike != null && mileage != null) {
                                                viewModel.updateBikeMileage(bike.licensePlate, mileage)
                                                showUpdateMileageDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                                    ) {
                                        Text("Update")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showSendAlertDialog) {
                    Dialog(onDismissRequest = { showSendAlertDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.padding(16.dp).fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Broadcast Rider Alert Warning", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("This warning alert notice will immediately pop up in the rider's active notifications hub on login.", fontSize = 11.sp, color = TextSecondary)
                                OutlinedTextField(
                                    value = customAlertMessageInput,
                                    onValueChange = { customAlertMessageInput = it },
                                    label = { Text("Enter Custom Notice Alert Message") },
                                    colors = customTextFieldColors(),
                                    modifier = Modifier.fillMaxWidth().testTag("custom_alert_input")
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { showSendAlertDialog = false }) {
                                        Text("Cancel", color = TextSecondary)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val bike = selectedBikeToSendAlert
                                            if (bike != null && customAlertMessageInput.isNotBlank()) {
                                                viewModel.sendServiceAlert(bike.licensePlate, customAlertMessageInput)
                                                showSendAlertDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                                    ) {
                                        Text("Broadcast Alert")
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rules Customizer Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Settings, contentDescription = "Rules", tint = MotorOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Maintenance Schedule Rules & Thresholds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = tempKmStr,
                                    onValueChange = { tempKmStr = it },
                                    label = { Text("Mileage limit (KM)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = customTextFieldColors(),
                                    modifier = Modifier.weight(1f).testTag("service_interval_km_input")
                                )
                                OutlinedTextField(
                                    value = tempDaysStr,
                                    onValueChange = { tempDaysStr = it },
                                    label = { Text("Time limit (Days)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = customTextFieldColors(),
                                    modifier = Modifier.weight(1f).testTag("service_interval_days_input")
                                )
                            }
                            Button(
                                onClick = {
                                    val km = tempKmStr.toIntOrNull() ?: 3000
                                    val days = tempDaysStr.toIntOrNull() ?: 90
                                    viewModel.updateServiceIntervals(km, days)
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_service_thresholds_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                            ) {
                                Text("Apply and Recalculate Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Overdue calculation logic for all bikes
                    val bikesOverdueData = bikes.map { bike ->
                        val bikePlate = bike.licensePlate.trim().uppercase()
                        val bikeRecords = allServiceRecords.filter { it.bikePlate.trim().uppercase() == bikePlate }
                        val lastRecord = bikeRecords.maxByOrNull { it.date }
                        val lastServiceOdometer = lastRecord?.odometer ?: 0
                        val mileageRun = bike.currentMileage - lastServiceOdometer
                        val daysElapsed = if (lastRecord != null) {
                            ((System.currentTimeMillis() - lastRecord.date) / (1000 * 60 * 60 * 24)).toInt()
                        } else {
                            -1
                        }
                        val neverServiced = bikeRecords.isEmpty()
                        val isOverdueByMileage = mileageRun >= serviceIntervalKm
                        val isOverdueByTime = lastRecord != null && daysElapsed >= serviceIntervalDays
                        val hasAlert = sentServiceAlerts.containsKey(bikePlate)

                        val isOverdue = neverServiced || isOverdueByMileage || isOverdueByTime || hasAlert

                        val statusLabel = when {
                            neverServiced -> "❌ Never Serviced"
                            isOverdueByMileage && isOverdueByTime -> "🚨 Overdue: Mileage & Time"
                            isOverdueByMileage -> "⚠️ Overdue: Mileage"
                            isOverdueByTime -> "⏳ Overdue: Time limit"
                            hasAlert -> "✉️ Alert Broadcasted"
                            else -> "✅ Service Healthy"
                        }

                        object {
                            val bike = bike
                            val neverServiced = neverServiced
                            val mileageRun = mileageRun
                            val daysElapsed = daysElapsed
                            val lastRecord = lastRecord
                            val isOverdue = isOverdue
                            val statusLabel = statusLabel
                            val hasAlert = hasAlert
                            val isOverdueByMileage = isOverdueByMileage
                            val isOverdueByTime = isOverdueByTime
                        }
                    }

                    val overdueCount = bikesOverdueData.count { it.isOverdue }

                    // Selector tabs
                    TabRow(
                        selectedTabIndex = if (filterTab == "OVERDUE") 0 else 1,
                        containerColor = SlateDark,
                        contentColor = MotorOrange,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = filterTab == "OVERDUE",
                            onClick = { filterTab = "OVERDUE" },
                            modifier = Modifier.testTag("filter_overdue_tab")
                        ) {
                            Text(
                                text = "OVERDUE ONLY ($overdueCount)",
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filterTab == "OVERDUE") MotorOrange else TextSecondary
                            )
                        }
                        Tab(
                            selected = filterTab == "ALL",
                            onClick = { filterTab = "ALL" },
                            modifier = Modifier.testTag("filter_all_tab")
                        ) {
                            Text(
                                text = "ALL MOTORBIKES (${bikes.size})",
                                modifier = Modifier.padding(vertical = 12.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filterTab == "ALL") MotorOrange else TextSecondary
                            )
                        }
                    }

                    // List of bikes
                    val filteredList = if (filterTab == "OVERDUE") {
                        bikesOverdueData.filter { it.isOverdue }
                    } else {
                        bikesOverdueData
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No bikes found in this category.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredList) { data ->
                                val bike = data.bike
                                val isOverdue = data.isOverdue
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.08f) else SurfaceDark
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.5f) else BorderColor
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("bike_overdue_card_${bike.licensePlate}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Header Row: Plate, Model, Badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = if (isOverdue) Color(0xFFEF4444) else MotorOrange, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = bike.licensePlate,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = if (isOverdue) Color(0xFFEF4444) else TextPrimary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(bike.model, fontSize = 11.sp, color = TextSecondary)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        color = if (isOverdue) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF4CAF50).copy(alpha = 0.2f),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = data.statusLabel.uppercase(),
                                                    color = if (isOverdue) Color(0xFFEF4444) else Color(0xFF4CAF50),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Owner & Phone info
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Rider: ${bike.ownerName} (${bike.ownerPhone})",
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                        }

                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))

                                        // Stats block
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "Current Odo: ",
                                                        fontSize = 11.sp,
                                                        color = TextSecondary
                                                    )
                                                    Text(
                                                        text = "${String.format("%,d", bike.currentMileage)} km",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            selectedBikeToUpdateMileage = bike
                                                            newMileageInput = bike.currentMileage.toString()
                                                            showUpdateMileageDialog = true
                                                        },
                                                        modifier = Modifier.size(24.dp).testTag("edit_mileage_button_${bike.licensePlate}")
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Edit Odo", tint = MotorOrange, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (data.neverServiced) {
                                                        "Last Service: None"
                                                    } else {
                                                        "Last Service: ${String.format("%,d", data.lastRecord?.odometer ?: 0)} km (${data.daysElapsed} days ago)"
                                                    },
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Mileage Since Service:",
                                                    fontSize = 10.sp,
                                                    color = TextSecondary
                                                )
                                                Text(
                                                    text = "${String.format("%,d", data.mileageRun)} km",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (data.isOverdueByMileage) Color(0xFFEF4444) else TextPrimary
                                                )
                                            }
                                        }

                                        // Actions block: trigger warning alert
                                        if (isOverdue) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (data.hasAlert) {
                                                    Text(
                                                        text = "Alert Notice Sent",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFEF4444),
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    TextButton(
                                                        onClick = { viewModel.clearServiceAlert(bike.licensePlate) },
                                                        modifier = Modifier.height(28.dp).testTag("clear_alert_button_${bike.licensePlate}"),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Clear Alert", color = TextSecondary, fontSize = 10.sp)
                                                    }
                                                } else {
                                                    Button(
                                                        onClick = {
                                                            selectedBikeToSendAlert = bike
                                                            val kmOver = if (data.neverServiced) bike.currentMileage else data.mileageRun
                                                            customAlertMessageInput = "⚠️ SERVICE WARNING NOTICE: Your motorbike (${bike.licensePlate}) is currently overdue for its scheduled maintenance! Odometer shows ${String.format("%,d", bike.currentMileage)} km (run of ${String.format("%,d", kmOver)} km since last service)."
                                                            showSendAlertDialog = true
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                                        modifier = Modifier.height(28.dp).testTag("trigger_alert_button_${bike.licensePlate}"),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Broadcast Alert Warning", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "QR_SCANNER" -> {
                QrScannerView(
                    viewModel = viewModel,
                    onNavigateToHistory = { onActiveMenuChange("SERVICE_HISTORY") },
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "SERVICE_HISTORY" -> {
                com.example.ui.ServiceHistoryScreen(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "APPOINTMENTS" -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (showAddManualAppointmentDialog) {
                        BookAppointmentDialog(
                            viewModel = viewModel,
                            defaultPlate = "",
                            allAppointments = appointments,
                            rebookingIntervalDays = rebookingIntervalDays,
                            appointmentsPerDay = appointmentsPerDay,
                            bookingType = "MANUAL",
                            onDismiss = { showAddManualAppointmentDialog = false },
                            onSave = { appt ->
                                viewModel.addAppointment(appt)
                                showAddManualAppointmentDialog = false
                            }
                        )
                    }

                    TabRow(
                        selectedTabIndex = when (appointmentTab) {
                            "CONFIRMED" -> 0
                            "SETTINGS" -> 1
                            else -> 0
                        },
                        containerColor = SurfaceDark,
                        contentColor = MotorOrange,
                        divider = { HorizontalDivider(color = BorderColor) }
                    ) {
                        Tab(
                            selected = appointmentTab == "CONFIRMED",
                            onClick = { appointmentTab = "CONFIRMED" },
                            text = { Text("Confirmed Appointments", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) }
                        )
                        Tab(
                            selected = appointmentTab == "SETTINGS",
                            onClick = { appointmentTab = "SETTINGS" },
                            text = { Text("Manage Appointments", fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                    }

                    when (appointmentTab) {
                        "CONFIRMED" -> {
                            val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
                            val activeApps = appointments.filter { it.status == "CONFIRMED" || it.status == "PENDING" }
                            val filteredActiveApps = activeApps.filter { isSameDay(it.preferredDate, selectedFilterDate) }

                            Column(modifier = Modifier.fillMaxSize()) {
                                // Date Filter Selection Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box {
                                        Button(
                                            onClick = { showDateDropdown = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                                            border = BorderStroke(1.dp, BorderColor),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MotorOrange,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isSameDay(selectedFilterDate, System.currentTimeMillis())) "Date: Today" else "Date: ${sdf.format(Date(selectedFilterDate))}",
                                                color = TextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showDateDropdown,
                                            onDismissRequest = { showDateDropdown = false },
                                            modifier = Modifier.background(SurfaceDark).border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Today", color = TextPrimary, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedFilterDate = System.currentTimeMillis()
                                                    showDateDropdown = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Tomorrow", color = TextPrimary, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedFilterDate = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
                                                    showDateDropdown = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Yesterday", color = TextPrimary, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    selectedFilterDate = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
                                                    showDateDropdown = false
                                                }
                                            )
                                            HorizontalDivider(color = BorderColor)
                                            DropdownMenuItem(
                                                text = { Text("Pick Any Date...", color = MotorOrange, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    showDatePickerDialog = true
                                                    showDateDropdown = false
                                                }
                                            )
                                        }
                                    }


                                }

                                if (showDatePickerDialog) {
                                    val context = LocalContext.current
                                    val calendar = Calendar.getInstance().apply { timeInMillis = selectedFilterDate }
                                    DisposableEffect(Unit) {
                                        val dpd = DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val newCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                selectedFilterDate = newCal.timeInMillis
                                                showDatePickerDialog = false
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        )
                                        dpd.setOnDismissListener { showDatePickerDialog = false }
                                        dpd.show()
                                        onDispose {
                                            dpd.dismiss()
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (filteredActiveApps.isEmpty()) {
                                        item {
                                            EmptyStateCard(
                                                message = "No active or pending appointments for this date.",
                                                icon = Icons.Default.DateRange
                                            )
                                        }
                                    } else {
                                        items(filteredActiveApps) { app ->
                                            StaffAppointmentItem(
                                                appointment = app,
                                                bikeRiderMappings = bikeRiderMappings,
                                                onConfirm = { viewModel.updateAppointmentStatus(app.id, "CONFIRMED") },
                                                onCancel = { viewModel.updateAppointmentStatus(app.id, "CANCELLED") },
                                                onComplete = { viewModel.updateAppointmentStatus(app.id, "COMPLETED") }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "SETTINGS" -> {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                // Manual Booking Action at the top
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "MANUAL APPOINTMENT BOOKING",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MotorOrange,
                                                letterSpacing = 0.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Instantly book a custom manual appointment for any rider. Manual bookings are separated from standard slots and bypass standard capacity cooldown logic.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Button(
                                                onClick = { showAddManualAppointmentDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Manual Booking", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // 1st Condition: Re-booking Interval
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "1. RIDER RE-BOOKING COOLDOWN",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotorOrange,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Minimum days required between successive rider appointments.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceDarkElevated)
                                                        .clickable { if (rebookingIntervalDays > 1) viewModel.updateRebookingIntervalDays(rebookingIntervalDays - 1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$rebookingIntervalDays",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = TextPrimary
                                                    )
                                                    Text(text = "Days", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceDarkElevated)
                                                        .clickable { if (rebookingIntervalDays < 90) viewModel.updateRebookingIntervalDays(rebookingIntervalDays + 1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                            }

                                            Text(
                                                text = "Wait Cooldown",
                                                fontWeight = FontWeight.Bold,
                                                color = MotorOrange,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                // 2nd Condition: Max Appointments per Day
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "2. DAILY BOOKING CAPACITY",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotorOrange,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Maximum number of appointments allowed in a single calendar date.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceDarkElevated)
                                                        .clickable { if (appointmentsPerDay > 1) viewModel.updateAppointmentsPerDay(appointmentsPerDay - 1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "$appointmentsPerDay",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = TextPrimary
                                                    )
                                                    Text(text = "Appts", fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(SurfaceDarkElevated)
                                                        .clickable { if (appointmentsPerDay < 100) viewModel.updateAppointmentsPerDay(appointmentsPerDay + 1) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                                }
                                            }

                                            Text(
                                                text = "Daily Limit",
                                                fontWeight = FontWeight.Bold,
                                                color = MotorOrange,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }

                                // 3rd Condition: Release Time & Notifications
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "3. RELEASE TIME & LIVE BROADCAST",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotorOrange,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Configure custom release times. Slots are automatically published and notifications sent to riders daily.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(SurfaceDark)
                                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp)
                                        ) {
                                            // Time editor (Hour, Minute, AM/PM)
                                            Text(
                                                text = "Daily Automatic Release Time",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                // Hour Selector
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("HOUR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                val h = if (releaseHour > 1) releaseHour - 1 else 12
                                                                viewModel.updateReleaseSettings(h, releaseMinute, releaseAmPm)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Text("-", color = MotorOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        }
                                                        Text(
                                                            text = String.format("%02d", releaseHour),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = TextPrimary,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                val h = if (releaseHour < 12) releaseHour + 1 else 1
                                                                viewModel.updateReleaseSettings(h, releaseMinute, releaseAmPm)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Text("+", color = MotorOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        }
                                                    }
                                                }

                                                // Minute Selector
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("MINUTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                val m = if (releaseMinute > 0) releaseMinute - 1 else 59
                                                                viewModel.updateReleaseSettings(releaseHour, m, releaseAmPm)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Text("-", color = MotorOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        }
                                                        Text(
                                                            text = String.format("%02d", releaseMinute),
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = TextPrimary,
                                                            modifier = Modifier.padding(horizontal = 8.dp)
                                                        )
                                                        IconButton(
                                                            onClick = {
                                                                val m = if (releaseMinute < 59) releaseMinute + 1 else 0
                                                                viewModel.updateReleaseSettings(releaseHour, m, releaseAmPm)
                                                            },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Text("+", color = MotorOrange, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                                        }
                                                    }
                                                }

                                                // AM / PM Selector
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                                                    Text("PERIOD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(SurfaceDarkElevated)
                                                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (releaseAmPm == "AM") MotorOrange else Color.Transparent)
                                                                .clickable { viewModel.updateReleaseSettings(releaseHour, releaseMinute, "AM") }
                                                                .padding(vertical = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("AM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (releaseAmPm == "AM") Color.White else TextSecondary)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(if (releaseAmPm == "PM") MotorOrange else Color.Transparent)
                                                                .clickable { viewModel.updateReleaseSettings(releaseHour, releaseMinute, "PM") }
                                                                .padding(vertical = 4.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text("PM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (releaseAmPm == "PM") Color.White else TextSecondary)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = BorderColor)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Configurable Timeslots
                                            Text(
                                                text = "Configure Release Timeslots",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Set the specific time for each of the $appointmentsPerDay appointments to be released.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))

                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                for (i in 1..appointmentsPerDay) {
                                                    val index = i - 1
                                                    val slotTime = viewModel.getSlotTime(index)
                                                    var localSlotText by remember(index, slotTime) { mutableStateOf(slotTime) }

                                                    OutlinedTextField(
                                                        value = localSlotText,
                                                        onValueChange = { 
                                                            localSlotText = it
                                                            viewModel.setSlotTime(index, it)
                                                        },
                                                        label = { Text("Appointment $i Scheduled Time") },
                                                        colors = customTextFieldColors(),
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(16.dp))
                                            HorizontalDivider(color = BorderColor)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Release window / days offset (7th day offset)
                                            Text(
                                                text = "Release Interval Window",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Configure how many days in advance slots are published.",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { if (releaseDaysOffset > 1) viewModel.updateReleaseDaysOffset(releaseDaysOffset - 1) },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(SurfaceDarkElevated)
                                                    ) {
                                                        Text("-", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                    Text(
                                                        text = "$releaseDaysOffset Days",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = TextPrimary,
                                                        modifier = Modifier.padding(horizontal = 14.dp)
                                                    )
                                                    IconButton(
                                                        onClick = { if (releaseDaysOffset < 30) viewModel.updateReleaseDaysOffset(releaseDaysOffset + 1) },
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape)
                                                            .background(SurfaceDarkElevated)
                                                    ) {
                                                        Text("+", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                    }
                                                }

                                                // Compute target release day name dynamically
                                                val cal = Calendar.getInstance()
                                                cal.add(Calendar.DAY_OF_YEAR, releaseDaysOffset)
                                                val targetDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)

                                                Text(
                                                    text = "Releases for $targetDayName",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MotorOrange
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Slider for custom days selection
                                            Slider(
                                                value = releaseDaysOffset.toFloat(),
                                                onValueChange = { viewModel.updateReleaseDaysOffset(it.toInt()) },
                                                valueRange = 1f..30f,
                                                steps = 28, // 30 - 1 - 1 = 28 intermediate steps
                                                colors = SliderDefaults.colors(
                                                    activeTrackColor = MotorOrange,
                                                    inactiveTrackColor = BorderColor,
                                                    thumbColor = MotorOrange,
                                                    activeTickColor = Color.Transparent,
                                                    inactiveTickColor = Color.Transparent
                                                 ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Quick choice preset chips
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(1, 3, 5, 7, 14, 30).forEach { preset ->
                                                    val isSelected = releaseDaysOffset == preset
                                                     Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MotorOrange else SurfaceDarkElevated)
                                                            .border(BorderStroke(1.dp, if (isSelected) MotorOrange else BorderColor), RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.updateReleaseDaysOffset(preset) }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                     ) {
                                                        Text(
                                                            text = "${preset}D",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) SlateDark else TextPrimary
                                                        )
                                                     }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(20.dp))
                                            HorizontalDivider(color = BorderColor)
                                            Spacer(modifier = Modifier.height(16.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.triggerLiveSlotsNotification()
                                                    showBroadcastSuccess = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp)
                                                    .testTag("release_broadcast_button")
                                            ) {
                                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Publish Slots & Notify Riders Now", fontWeight = FontWeight.Bold)
                                            }

                                            if (showBroadcastSuccess) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF065F46)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Live notification broadcasted successfully!",
                                                            color = Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "DATA_INPUT" -> {
                DataInputCenterView(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "MONTHLY_PERFORMANCE" -> {
                MonthlyPerformanceView(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") },
                    showSettings = showPerformanceSettings,
                    onSettingsDismissed = onDismissPerformanceSettings
                )
            }

            "STAFF_MANAGEMENT" -> {
                StaffManagementView(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "ACCESS_CONTROL" -> {
                AccessControlView(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "GARAGE_TRAFFIC" -> {
                GarageTrafficStatusView(
                    viewModel = viewModel,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "FIREBASE_PORTAL" -> {
                FirebaseLivePortalScreen(
                    viewModel = viewModel,
                    showSettingsDialog = showFirebaseSettings,
                    onDismissSettingsDialog = onDismissFirebaseSettings,
                    isFromManagement = true,
                    onBack = { onActiveMenuChange("DASHBOARD") }
                )
            }

            "SERVICE_QUEUE" -> {
                GarageServiceQueueView(
                    viewModel = viewModel,
                    selectedMode = serviceQueueMode,
                    onModeChange = onServiceQueueModeChange
                )
            }
        }
    }
}
}

@Composable
fun ServiceHistoryStaffItem(record: ServiceRecord, onDelete: () -> Unit) {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val typeColor = when (record.type) {
        "SERVICE_WITH_PARTS" -> ColorServiceWithParts
        "OIL_CHANGE" -> ColorOilChange
        "SERVICE_WITHOUT_PARTS" -> ColorServiceWithoutParts
        else -> Color.White
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = record.bikePlate,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = typeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = record.type.replace("_", " "),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Odo: ${record.odometer} km",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
                Text(
                    "Date: ${formatter.format(Date(record.date))}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Cost:",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Text(
                    "$${String.format(Locale.getDefault(), "%.2f", record.cost)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            if (!record.partsDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceDarkElevated)
                        .padding(8.dp)
                ) {
                    Text(
                        "Parts: ${record.partsDetails}",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Notes: ${record.notes}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun BikeStaffItem(bike: Bike) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDarkElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .border(BorderStroke(1.dp, MotorOrange.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = bike.licensePlate,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = MotorOrange
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        bike.model,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Text(
                        "Year: ${bike.year}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    bike.ownerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Text(
                    bike.ownerPhone,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun StaffAppointmentItem(
    appointment: Appointment,
    bikeRiderMappings: List<com.example.data.BikeRiderMapping>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val badgeColor = when (appointment.status) {
        "CONFIRMED" -> ColorServiceWithoutParts
        "COMPLETED" -> ColorServiceWithParts
        "CANCELLED" -> Color(0xFFEF4444)
        else -> MotorAmber
    }

    val notesText = appointment.notes
    val slotPrefix = if (notesText.startsWith("[Slot ")) {
        val endIdx = notesText.indexOf("]")
        if (endIdx != -1) notesText.substring(0, endIdx + 1) else null
    } else if (notesText.startsWith("[Manual: ")) {
        val endIdx = notesText.indexOf("]")
        if (endIdx != -1) notesText.substring(0, endIdx + 1) else null
    } else null

    val displayNotes = if (slotPrefix != null) {
        notesText.substring(slotPrefix.length).trim().removePrefix("|").trim()
    } else {
        notesText
    }

    val timeLabel = if (slotPrefix != null) {
        if (slotPrefix.startsWith("[Manual: ")) {
            slotPrefix.substringAfter("[Manual: ").removeSuffix("]")
        } else {
            val match = Regex("\\(([^)]+)\\)").find(slotPrefix)
            if (match != null) {
                match.groupValues[1]
            } else {
                "Slot " + slotPrefix.filter { it.isDigit() }
            }
        }
    } else "Scheduled Time"

    val displayTime = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(appointment.preferredDate)) + " @ " + timeLabel

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ALWAYS VISIBLE Row: Appointment Number and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Appointment Number Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MotorOrange.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "No: ${if (appointment.appointmentNumber.isEmpty()) "N/A" else appointment.appointmentNumber}",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = MotorOrange
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    // Time of Appointment
                    Column {
                        Text("Appointment Time", fontSize = 9.sp, color = TextSecondary)
                        Text(
                            text = displayTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }
                }

                // Badges & Expand Icon Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val typeBadgeColor = if (appointment.bookingType == "MANUAL") ColorServiceWithoutParts else ColorServiceWithParts
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(typeBadgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = appointment.bookingType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = typeBadgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            appointment.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // EXPANDED Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    HorizontalDivider(color = BorderColor)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bike plate & Service type Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bike License Plate", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = appointment.bikePlate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MotorOrange
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Service Category", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = appointment.serviceType.replace("_", " "),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rider Details & Rider ID
                    val mapping = bikeRiderMappings.find { it.bikePlate.trim().uppercase() == appointment.bikePlate.trim().uppercase() }
                    val riderId = mapping?.riderId ?: "N/A (No Mapping)"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Rider Name", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = appointment.riderName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rider ID", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = riderId,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MotorOrange
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Phone Number", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = appointment.riderPhone,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Calendar Date", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                text = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()).format(Date(appointment.preferredDate)),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    if (displayNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Rider Notes / Symptoms:", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = displayNotes,
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                    }

                    // Action buttons
                    if (appointment.status == "PENDING" || appointment.status == "CONFIRMED") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val success = com.example.util.AppointmentNotificationHelper
                                        .sendAppointmentReminderNotification(context, appointment)
                                    Toast.makeText(
                                        context,
                                        if (success) "🔔 Reminder notification sent to rider!" else "Reminder notification triggered",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                border = BorderStroke(1.dp, MotorOrange),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MotorOrange),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Send Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = onCancel,
                                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .padding(end = 8.dp)
                            ) {
                                Text("Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (appointment.status == "PENDING") {
                                Button(
                                    onClick = onConfirm,
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorServiceWithoutParts),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Confirm Booking", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (appointment.status == "CONFIRMED") {
                                Button(
                                    onClick = onComplete,
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorServiceWithParts),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Mark Completed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// --------------------- DIALOG FORMS ---------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBikeDialog(
    onDismiss: () -> Unit,
    onSave: (Bike) -> Unit
) {
    var plate by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("2024") }

    var validationError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        AnimatedDialogContainer {
            Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Motorbike Registry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                HorizontalDivider(color = BorderColor)

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase() },
                    label = { Text("License Plate") },
                    colors = customTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_bike_plate_input")
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model (e.g., Suzuki GSX-R750)") },
                    colors = customTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Owner Name") },
                    colors = customTextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Owner Phone Number") },
                    colors = customTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = yearStr,
                    onValueChange = { yearStr = it },
                    label = { Text("Production Year") },
                    colors = customTextFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError.isNotBlank()) {
                    Text(
                        text = validationError,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (plate.isBlank() || model.isBlank() || ownerName.isBlank() || phone.isBlank()) {
                                validationError = "All fields must be filled out!"
                            } else {
                                val yearInt = yearStr.toIntOrNull() ?: 2024
                                onSave(Bike(plate.trim(), model.trim(), ownerName.trim(), phone.trim(), yearInt))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_bike_save_button")
                    ) {
                        Text("Save Bike", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddServiceRecordDialog(
    bikes: List<Bike>,
    onDismiss: () -> Unit,
    onSave: (ServiceRecord) -> Unit
) {
    var selectedPlate by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("SERVICE_WITH_PARTS") } // "SERVICE_WITH_PARTS", "OIL_CHANGE", "SERVICE_WITHOUT_PARTS"
    var partsDetails by remember { mutableStateOf("") }
    var costStr by remember { mutableStateOf("") }
    var odometerStr by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Log Service Record",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item { HorizontalDivider(color = BorderColor) }

                // Bike Plate Input (Text field or pick from dropdown/list)
                item {
                    OutlinedTextField(
                        value = selectedPlate,
                        onValueChange = { selectedPlate = it.uppercase() },
                        label = { Text("Bike License Plate") },
                        colors = customTextFieldColors(),
                        singleLine = true,
                        placeholder = { Text("e.g. MH-12-AB-1234") },
                        modifier = Modifier.fillMaxWidth().testTag("add_service_plate_input")
                    )
                }

                // Service type chooser
                item {
                    Text("Service Type", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDarkElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "SERVICE_WITH_PARTS" to "Parts",
                            "OIL_CHANGE" to "Oil Change",
                            "SERVICE_WITHOUT_PARTS" to "No Parts"
                        ).forEach { (type, label) ->
                            val selected = serviceType == type
                            val color = when (type) {
                                "SERVICE_WITH_PARTS" -> ColorServiceWithParts
                                "OIL_CHANGE" -> ColorOilChange
                                "SERVICE_WITHOUT_PARTS" -> ColorServiceWithoutParts
                                else -> MotorOrange
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) color else Color.Transparent)
                                    .clickable { serviceType = type }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Parts field if service with parts or oil change
                if (serviceType == "SERVICE_WITH_PARTS" || serviceType == "OIL_CHANGE") {
                    item {
                        OutlinedTextField(
                            value = partsDetails,
                            onValueChange = { partsDetails = it },
                            label = { Text("Parts Used / Lubricants") },
                            colors = customTextFieldColors(),
                            placeholder = { Text("e.g. Brake pads, Spark plug, Mobil1 Oil") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = odometerStr,
                        onValueChange = { odometerStr = it },
                        label = { Text("Odometer Reading (km)") },
                        colors = customTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Service Cost ($)") },
                        colors = customTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Technician Notes") },
                        colors = customTextFieldColors(),
                        placeholder = { Text("e.g., Checked tire pressure and chain slack.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (validationError.isNotBlank()) {
                    item {
                        Text(
                            text = validationError,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (selectedPlate.isBlank() || odometerStr.isBlank() || costStr.isBlank()) {
                                    validationError = "Plate, Odometer, and Cost are required!"
                                } else {
                                    val costVal = costStr.toDoubleOrNull() ?: 0.0
                                    val odoVal = odometerStr.toIntOrNull() ?: 0
                                    onSave(
                                        ServiceRecord(
                                            bikePlate = selectedPlate.trim(),
                                            type = serviceType,
                                            partsDetails = if (serviceType == "SERVICE_WITHOUT_PARTS") null else partsDetails.trim(),
                                            cost = costVal,
                                            odometer = odoVal,
                                            notes = notes.trim()
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("add_service_save_button")
                        ) {
                            Text("Save Record", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentDialog(
    viewModel: MotorcycleViewModel,
    defaultPlate: String,
    allAppointments: List<Appointment>,
    rebookingIntervalDays: Int,
    appointmentsPerDay: Int,
    bookingType: String = "STANDARD",
    onDismiss: () -> Unit,
    onSave: (Appointment) -> Unit
) {
    val selectedRiderMapping by viewModel.selectedRiderMapping.collectAsState()

    var plate by remember { mutableStateOf(defaultPlate) }
    var riderName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("OIL_CHANGE") }
    var notes by remember { mutableStateOf("") }
    var manualTimeInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedRiderMapping, defaultPlate) {
        if (bookingType == "STANDARD") {
            plate = selectedRiderMapping?.bikePlate ?: defaultPlate
            riderName = selectedRiderMapping?.riderName ?: ""
        }
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var selectedTimeMs by remember { mutableStateOf(System.currentTimeMillis() + (24 * 60 * 60 * 1000L)) } // 1 day in future default
    val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    var validationError by remember { mutableStateOf("") }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            selectedTimeMs = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Slot selection states
    var selectedSlotIndex by remember { mutableStateOf(-1) } // 1-based index
    val bookedSlotsOnSelectedDate = remember(selectedTimeMs, allAppointments) {
        allAppointments.filter { 
            it.status != "CANCELLED" && isSameDay(it.preferredDate, selectedTimeMs)
        }.map { appt ->
            val notesUpper = appt.notes.uppercase()
            var foundIdx = -1
            for (i in 1..100) {
                if (notesUpper.startsWith("[SLOT $i]") || notesUpper.contains("SLOT $i")) {
                    foundIdx = i
                    break
                }
            }
            foundIdx
        }.filter { it != -1 }.toSet()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = if (bookingType == "MANUAL") "Manual Appointment Entry" else "Book Appointment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                item { HorizontalDivider(color = BorderColor) }

                if (bookingType == "STANDARD" && selectedRiderMapping != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Rider ID (Profile)", fontSize = 10.sp, color = TextSecondary)
                                    Text(
                                        text = selectedRiderMapping?.riderId ?: "N/A",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MotorOrange
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ColorServiceWithParts.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "AUTO-LINKED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorServiceWithParts
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = plate,
                        onValueChange = { if (bookingType == "MANUAL") plate = it.uppercase() },
                        label = { Text("Bike License Plate") },
                        colors = customTextFieldColors(),
                        singleLine = true,
                        readOnly = bookingType == "STANDARD",
                        modifier = Modifier.fillMaxWidth().testTag("appointment_plate_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = riderName,
                        onValueChange = { if (bookingType == "MANUAL") riderName = it },
                        label = { Text("Rider Name") },
                        colors = customTextFieldColors(),
                        singleLine = true,
                        readOnly = bookingType == "STANDARD",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Rider Phone Number") },
                        colors = customTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Requested Service Type", fontSize = 11.sp, color = TextSecondary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDarkElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "SERVICE_WITH_PARTS" to "Full Tuneup",
                            "OIL_CHANGE" to "Oil Change",
                            "SERVICE_WITHOUT_PARTS" to "Checkup"
                        ).forEach { (type, label) ->
                            val selected = serviceType == type
                            val color = when (type) {
                                "SERVICE_WITH_PARTS" -> ColorServiceWithParts
                                "OIL_CHANGE" -> ColorOilChange
                                "SERVICE_WITHOUT_PARTS" -> ColorServiceWithoutParts
                                else -> MotorOrange
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) color else Color.Transparent)
                                    .clickable { serviceType = type }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Preferred Booking Date", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDarkElevated)
                                .clickable { datePickerDialog.show() }
                                .padding(12.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pick Date", tint = MotorOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = dateFormatter.format(Date(selectedTimeMs)),
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Time slots selector / manual time writer section
                item {
                    if (bookingType == "MANUAL") {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Write Scheduled Time", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = manualTimeInput,
                                onValueChange = { manualTimeInput = it },
                                label = { Text("Appointment Time / Schedule") },
                                colors = customTextFieldColors(),
                                placeholder = { Text("e.g. 10:30 AM, Anytime, Morning slot") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Select Available Timeslot", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var visibleSlotsCount = 0
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in 1..appointmentsPerDay) {
                                    val index = i - 1
                                    val slotTime = viewModel.getSlotTime(index)
                                    val isBooked = bookedSlotsOnSelectedDate.contains(i)
                                    
                                    if (!isBooked) {
                                        visibleSlotsCount++
                                        val isSelected = selectedSlotIndex == i
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) MotorOrange.copy(alpha = 0.2f) else SurfaceDarkElevated)
                                                .border(1.dp, if (isSelected) MotorOrange else BorderColor, RoundedCornerShape(8.dp))
                                                .clickable { selectedSlotIndex = i }
                                                .padding(8.dp)
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = { selectedSlotIndex = i },
                                                colors = RadioButtonDefaults.colors(selectedColor = MotorOrange)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Appointment $i", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text("Time: $slotTime", fontSize = 11.sp, color = TextSecondary)
                                            }
                                        }
                                    }
                                }
                                
                                if (visibleSlotsCount == 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "No available slots on this date. Please select another date or wait for next release countdown.",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Describe Symptoms / Request details") },
                        colors = customTextFieldColors(),
                        placeholder = { Text("e.g. Engine ticking sound, Oil warning light came on.") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (validationError.isNotBlank()) {
                    item {
                        Text(
                            text = validationError,
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (plate.isBlank() || riderName.isBlank() || phone.isBlank()) {
                                    validationError = "Plate, Name, and Phone number are required!"
                                } else if (selectedSlotIndex == -1 && bookingType != "MANUAL") {
                                    validationError = "Please select an available timeslot!"
                                } else {
                                    if (bookingType != "MANUAL") {
                                        // 1. Re-booking Cooldown Condition Validation
                                        val existingPlateBookings = allAppointments.filter { 
                                            it.bikePlate.trim().uppercase() == plate.trim().uppercase() && 
                                            it.status != "CANCELLED" 
                                        }
                                        if (existingPlateBookings.isNotEmpty()) {
                                            val lastAppt = existingPlateBookings.maxByOrNull { it.preferredDate }
                                            if (lastAppt != null) {
                                                val diffMs = Math.abs(selectedTimeMs - lastAppt.preferredDate)
                                                val diffDays = diffMs / (24 * 60 * 60 * 1000L)
                                                if (diffDays < rebookingIntervalDays) {
                                                    validationError = "Cooldown restriction! You must wait at least $rebookingIntervalDays days between appointments."
                                                    return@Button
                                                }
                                            }
                                        }

                                        // 2. Daily Booking Capacity Condition Validation
                                        val targetDayBookings = allAppointments.count { 
                                            it.status != "CANCELLED" && isSameDay(it.preferredDate, selectedTimeMs) 
                                        }
                                        if (targetDayBookings >= appointmentsPerDay) {
                                            validationError = "Daily limit reached for this date!"
                                            return@Button
                                        }
                                    }

                                    val formattedNotes = if (bookingType == "MANUAL") {
                                        val timeStr = manualTimeInput.trim().ifBlank { "Anytime" }
                                        "[Manual: $timeStr] | ${notes.trim()}"
                                    } else if (selectedSlotIndex != -1) {
                                        val slotTimeText = viewModel.getSlotTime(selectedSlotIndex - 1)
                                        "[Slot $selectedSlotIndex] ($slotTimeText) | ${notes.trim()}"
                                    } else {
                                        notes.trim()
                                    }

                                    onSave(
                                        Appointment(
                                            bikePlate = plate.trim().uppercase(),
                                            riderName = riderName.trim(),
                                            riderPhone = phone.trim(),
                                            serviceType = serviceType,
                                            preferredDate = selectedTimeMs,
                                            notes = formattedNotes,
                                            status = if (bookingType == "MANUAL") "CONFIRMED" else "PENDING",
                                            bookingType = bookingType
                                        )
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("submit_appointment_dialog_button")
                        ) {
                            Text(if (bookingType == "MANUAL") "Confirm Booking" else "Submit Request", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGridCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        border = BorderStroke(1.dp, if (enabled) BorderColor else BorderColor.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .bounceOnClick(enabled = enabled) {
                if (enabled) {
                    onClick()
                } else {
                    Toast.makeText(context, "🔒 Locked: Access not granted for $title. Please contact an Administrator.", Toast.LENGTH_LONG).show()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun getGarageOpeningTime(timingStr: String): String {
    val parts = timingStr.split(Regex("[-to]"))
    if (parts.isNotEmpty()) {
        return parts[0].trim()
    }
    return "08:00 AM"
}

fun getGarageClosingTime(timingStr: String): String {
    val parts = timingStr.split(Regex("[-to]"))
    if (parts.size > 1) {
        return parts[parts.size - 1].trim()
    }
    return "04:00 AM"
}

@Composable
fun LocalFileImage(filePath: String, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap = remember(filePath) {
        try {
            if (filePath.startsWith("android.resource://") || filePath.isEmpty()) {
                null
            } else {
                android.graphics.BitmapFactory.decodeFile(filePath)
            }
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(SlateDark),
            contentAlignment = Alignment.Center
        ) {
            Text("🏍️", fontSize = 18.sp)
        }
    }
}
@Composable
fun GarageLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember {
        try {
            context.assets.open("logo.jpeg").use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(), 
            contentDescription = "Garage Logo",
            modifier = modifier
        )
    }
}

@Composable
fun AnimatedSplashScreen(onAnimationFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = tween(
            durationMillis = 1200,
            easing = FastOutSlowInEasing
        ),
        label = "Scale"
    )
    
    val rotationAnim by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 180f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "3DRotation"
    )

    val opacityAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "Opacity"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        kotlinx.coroutines.delay(2500)
        onAnimationFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            GarageLogo(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer {
                        scaleX = scaleAnim
                        scaleY = scaleAnim
                        rotationY = rotationAnim
                        cameraDistance = 12f
                        this.alpha = opacityAnim
                    }
            )
        }
    }
}

