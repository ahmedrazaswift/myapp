package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.Bike
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun QrScannerView(
    viewModel: MotorcycleViewModel,
    onNavigateToHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bikes by viewModel.bikes.collectAsState()

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required to scan motorbike QR labels.", Toast.LENGTH_LONG).show()
        }
    }

    // Checking permission on composition
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Scanning state simulation
    var isSimulatingScan by remember { mutableStateOf(false) }
    var scanningProgress by remember { mutableStateOf(0f) }
    var simulatedBikePlate by remember { mutableStateOf("") }
    var manualInputPlate by remember { mutableStateOf("") }

    // Pulse animation for scan reticle
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    // Trigger simulation routine
    fun runScanSimulation(plate: String) {
        if (isSimulatingScan) return
        scope.launch {
            simulatedBikePlate = plate
            isSimulatingScan = true
            scanningProgress = 0f
            
            // Increment progress bar to look like real parsing
            for (i in 1..10) {
                delay(120)
                scanningProgress = i / 10f
            }
            
            // Update search query & load records
            val finalPlate = plate.trim().uppercase()
            viewModel.updateSearchQuery(finalPlate)
            viewModel.refreshData(query = finalPlate) { success ->
                isSimulatingScan = false
                if (success) {
                    Toast.makeText(context, "QR Label Found: $finalPlate! Instantly loaded records.", Toast.LENGTH_SHORT).show()
                    onNavigateToHistory()
                } else {
                    Toast.makeText(context, "No service records linked to label: $finalPlate. Navigating to history search...", Toast.LENGTH_LONG).show()
                    onNavigateToHistory()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        // App bar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("qr_scanner_back_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Management Portal",
                    tint = MotorOrange
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Motorbike QR Frame Scanner",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Instantly retrieve digital history by scanning chassis frame label",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }

        // Main scanner visual body
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                // Compile and show the real CameraX PreviewView
                CameraXPreview(
                    modifier = Modifier.fillMaxSize()
                )

                // High-fidelity graphic reticle overlay
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .border(BorderStroke(2.dp, Color(0xFF00BCD4)), RoundedCornerShape(12.dp))
                        .testTag("scan_reticle_target")
                ) {
                    // Corners styling
                    Box(modifier = Modifier.size(16.dp).align(Alignment.TopStart).border(BorderStroke(4.dp, Color(0xFF00BCD4)), RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.size(16.dp).align(Alignment.TopEnd).border(BorderStroke(4.dp, Color(0xFF00BCD4)), RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.size(16.dp).align(Alignment.BottomStart).border(BorderStroke(4.dp, Color(0xFF00BCD4)), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 0.dp)))
                    Box(modifier = Modifier.size(16.dp).align(Alignment.BottomEnd).border(BorderStroke(4.dp, Color(0xFF00BCD4)), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 12.dp)))

                    // Pulsing animated scanner laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(laserYOffset)
                            .align(Alignment.TopCenter)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFF5722), Color(0xFFFF5722).copy(alpha = 0.4f))
                                    )
                                )
                        )
                    }
                }

                // Instruction Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFF00BCD4).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF00BCD4), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Camera Active • Point at Chassis Label",
                            color = Color(0xFF00BCD4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Explanatory placeholder block when camera is not authorized
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Icon",
                        tint = TextDisabled,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Access Required",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "We require camera permission to detect and decode physical QR labels on motorcycle chassis frame labels.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                        modifier = Modifier.testTag("grant_camera_permission_button")
                    ) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            // Scanning Loading Spinner Overlay
            if (isSimulatingScan) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MotorOrange,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Decoding Chassis Frame Tag...",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matching code: $simulatedBikePlate",
                            color = MotorOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { scanningProgress },
                            color = MotorOrange,
                            trackColor = Color.DarkGray,
                            modifier = Modifier
                                .width(180.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }

        // Dynamic Interactive HUD Console Panel
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Simulator Mode Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sim Mode",
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Virtual QR Simulator Control Console",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00BCD4)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00BCD4).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "STABLE SIMULATION MODE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00BCD4)
                    )
                }
            }

            Text(
                text = "Tap on any registered motorcycle's QR tag to instantly simulate scanning its chassis plate in this virtual testing platform:",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
            )

            // Horizontal or Vertical List of Bikes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (bikes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No registered motorcycles found in system.", color = TextDisabled, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(bikes) { bike ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { runScanSimulation(bike.licensePlate) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("sim_scan_row_${bike.licensePlate}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null,
                                    tint = MotorOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = bike.licensePlate,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Model: ${bike.model} • Owner: ${bike.ownerName}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Button(
                                onClick = { runScanSimulation(bike.licensePlate) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(26.dp)
                                    .testTag("sim_scan_button_${bike.licensePlate}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Scan Tag", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            // Custom Frame Label manual entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualInputPlate,
                    onValueChange = { manualInputPlate = it },
                    placeholder = { Text("Or enter custom label code...", fontSize = 12.sp, color = TextDisabled) },
                    colors = customTextFieldColors(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("manual_qr_input"),
                    textStyle = TextStyle(fontSize = 13.sp, color = TextPrimary)
                )
                Button(
                    onClick = {
                        if (manualInputPlate.isNotBlank()) {
                            runScanSimulation(manualInputPlate)
                        } else {
                            Toast.makeText(context, "Please enter a valid frame code to scan.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("submit_manual_qr_button")
                ) {
                    Text("Read", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CameraXPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (exc: Exception) {
                    Log.e("CameraXPreview", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
            previewView
        },
        modifier = modifier
    )
}
