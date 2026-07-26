package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import com.example.ui.components.getStandardScreenTransitionSpec
import com.example.ui.components.bounceOnClick
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseLivePortalScreen(
    viewModel: MotorcycleViewModel,
    showSettingsDialog: Boolean = false,
    onDismissSettingsDialog: (() -> Unit)? = null,
    onShowSettingsDialog: (() -> Unit)? = null,
    isFromManagement: Boolean = false,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val firebaseDbUrl by viewModel.firebaseDbUrl.collectAsState()
    var inputDbUrl by remember(firebaseDbUrl) { mutableStateOf(firebaseDbUrl) }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val firebaseBikeData by viewModel.firebaseBikeData.collectAsState()
    val fetchedTabs by viewModel.fetchedTabs.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    var searchBikeNumber by remember { mutableStateOf("") }
    var lastSearchedNumber by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var localShowSettingsDialog by remember { mutableStateOf(false) }

    val activeShowSettings = if (isFromManagement) showSettingsDialog else localShowSettingsDialog
    val dismissSettings = {
        if (isFromManagement) {
            onDismissSettingsDialog?.invoke()
        } else {
            localShowSettingsDialog = false
        }
    }

    // Settings Dialog containing Connection details and DB structure guidelines
    if (activeShowSettings) {
        AlertDialog(
            onDismissRequest = { dismissSettings() },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MotorOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Firebase Settings",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Verify the database URL configured in your secrets file or provide a custom target URL manually below:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    OutlinedTextField(
                        value = inputDbUrl,
                        onValueChange = { inputDbUrl = it },
                        label = { Text("Firebase Realtime Database URL", color = TextSecondary, fontSize = 11.sp) },
                        placeholder = { Text("https://your-app-default-rtdb.firebaseio.com/", color = TextDisabled, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = customTextFieldColors()
                    )

                    Button(
                        onClick = {
                            val trimmed = inputDbUrl.trim()
                            if (trimmed.isNotEmpty() && !trimmed.startsWith("http")) {
                                Toast.makeText(context, "Please enter a valid HTTP/HTTPS database URL", Toast.LENGTH_SHORT).show()
                            } else {
                                keyboardController?.hide()
                                viewModel.updateFirebaseDbUrl(trimmed)
                                Toast.makeText(context, "Firebase Database URL updated successfully!", Toast.LENGTH_SHORT).show()
                                dismissSettings()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save & Apply URL", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))

                    Text(
                        text = "💡 Database Structure requirement:",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "Ensure your Firebase Realtime Database contains records at path /bikes/{bike_number} formatted like:\n\n" +
                                "{\n" +
                                "  \"engine_oil\": [\n" +
                                "    {\n" +
                                "      \"date_of_service\": \"2026-07-01\",\n" +
                                "      \"kilometer\": \"15200\",\n" +
                                "      \"next_service\": \"2026-08-01\",\n" +
                                "      \"remarks\": \"Oil level normal\"\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"bike_visits\": [\n" +
                                "    {\n" +
                                "      \"date\": \"2026-07-05\",\n" +
                                "      \"talabat_id\": \"T-9921\",\n" +
                                "      \"km_run\": \"15300\",\n" +
                                "      \"job_card\": \"JC-7082\",\n" +
                                "      \"job_done\": \"Brake replacement\",\n" +
                                "      \"service_type\": \"Minor Repair\",\n" +
                                "      \"mechanic\": \"John Doe\"\n" +
                                "    }\n" +
                                "  ],\n" +
                                "  \"exported_reports\": [\n" +
                                "    {\n" +
                                "      \"type\": \"With Parts\",\n" +
                                "      \"vch_no\": \"VCH-1092\",\n" +
                                "      \"vch_date\": \"2026-07-05\",\n" +
                                "      \"item_name\": \"Front Brake Shoe\",\n" +
                                "      \"mechanic\": \"John Doe\"\n" +
                                "    }\n" +
                                "  ]\n" +
                                "}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { dismissSettings() }) {
                    Text("Close", color = MotorOrange, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark,
            tonalElevation = 6.dp
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .then(if (isFromManagement) Modifier else Modifier.statusBarsPadding())
    ) {
        // App bar (Only visible when NOT inside the Management Portal)
        if (!isFromManagement) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Firebase Live Portal",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { localShowSettingsDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MotorOrange
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Combined Search Box with internal Search action and adjacent Year Toggle Buttons ([2025] | [2026])
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchBikeNumber,
                    onValueChange = { searchBikeNumber = it },
                    placeholder = { Text("Search Bike Number...", color = TextSecondary, fontSize = 13.sp) },
                    trailingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            if (searchBikeNumber.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchBikeNumber = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRefreshing) TextDisabled else MotorOrange)
                                    .bounceOnClick(enabled = !isRefreshing) {
                                        val target = searchBikeNumber.trim()
                                        if (target.isEmpty()) {
                                            Toast.makeText(context, "Please enter a bike number", Toast.LENGTH_SHORT).show()
                                        } else {
                                            lastSearchedNumber = target
                                            keyboardController?.hide()
                                            viewModel.fetchFirebaseBikeDataForTab(target, selectedTab) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Search results updated from Firebase!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Lookup failed for $target in year $selectedYear.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRefreshing && selectedTab !in fetchedTabs) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Search",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            val target = searchBikeNumber.trim()
                            if (target.isEmpty()) {
                                Toast.makeText(context, "Please enter a bike number", Toast.LENGTH_SHORT).show()
                            } else {
                                lastSearchedNumber = target
                                keyboardController?.hide()
                                viewModel.fetchFirebaseBikeDataForTab(target, selectedTab) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Search results updated from Firebase!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lookup failed for $target in year $selectedYear.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = MotorOrange,
                        unfocusedBorderColor = BorderColor.copy(alpha = 0.5f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = MotorOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Year Toggle Buttons ([2025] | [2026]) positioned on the right side
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("2025", "2026").forEach { yr ->
                        val isYearSelected = selectedYear == yr
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isYearSelected) MotorOrange else Color.Transparent)
                                .bounceOnClick(scaleDownRatio = 0.95f) {
                                    if (selectedYear != yr) {
                                        viewModel.updateSelectedYear(yr)
                                        viewModel.clearFirebaseBikeData()
                                        if (lastSearchedNumber.isNotEmpty()) {
                                            viewModel.fetchFirebaseBikeDataForTab(lastSearchedNumber, selectedTab, yr) { success ->
                                                if (!success) {
                                                    Toast.makeText(context, "No Firebase records found for $yr", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = yr,
                                color = if (isYearSelected) Color.White else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Always Visible Custom Navigation Tab Bar (Keep three service type options visible at all times)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceDarkElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val data = firebaseBikeData
                val tabs = listOf(
                    "ENGINE OIL" to (if (lastSearchedNumber.isNotEmpty() && 0 in fetchedTabs) data?.engineOilList?.size else null),
                    "Without Parts" to (if (lastSearchedNumber.isNotEmpty() && 1 in fetchedTabs) data?.bikeVisitsList?.size else null),
                    "With Parts" to (if (lastSearchedNumber.isNotEmpty() && 2 in fetchedTabs) data?.exportedReportsList?.size else null)
                )
                tabs.forEachIndexed { index, (title, count) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) MotorOrange else Color.Transparent)
                            .clickable {
                                selectedTab = index
                                // Fetch data ONLY when the tab is clicked, minimizing download bandwidth
                                if (lastSearchedNumber.isNotEmpty() && index !in fetchedTabs) {
                                    viewModel.fetchFirebaseBikeDataForTab(lastSearchedNumber, index) { success ->
                                        if (!success) {
                                            Toast.makeText(context, "Lookup failed for $title", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = when {
                                    lastSearchedNumber.isEmpty() -> "- records"
                                    count != null -> "$count records"
                                    else -> "Click to load"
                                },
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextSecondary,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Display results if we have searched or found something
            if (lastSearchedNumber.isNotEmpty()) {
                val data = firebaseBikeData
                val isTabLoading = isRefreshing && (selectedTab !in fetchedTabs || data == null || data.bikeNumber != lastSearchedNumber.trim().uppercase())

                if (isTabLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val infiniteTransition = rememberInfiniteTransition(label = "title_skeleton")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.6f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "title_alpha"
                            )
                            Text(
                                text = "🏍️ Fetching data for Bike: ${lastSearchedNumber.trim().uppercase()}...",
                                color = MotorOrange.copy(alpha = alpha),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val skeletonHeaders = when (selectedTab) {
                                0 -> listOf("Date Of Service", "Kilometer", "next service", "Remarks")
                                1 -> listOf("Date", "TALABAT ID", "KM RUN", "JOB CARD", "JOB DONE", "Service type", "MECHANIC")
                                else -> listOf("Type", "Vch No", "Vch Date", "Item Name", "Mechanic")
                            }
                            val skeletonWidths = when (selectedTab) {
                                0 -> listOf(130.dp, 100.dp, 120.dp, 180.dp)
                                1 -> listOf(110.dp, 100.dp, 85.dp, 95.dp, 140.dp, 110.dp, 110.dp)
                                else -> listOf(110.dp, 90.dp, 110.dp, 150.dp, 110.dp)
                            }
                            TabularSectionSkeleton(headers = skeletonHeaders, widths = skeletonWidths)
                        }
                    }
                } else if (data == null || data.bikeNumber != lastSearchedNumber.trim().uppercase()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MotorOrange,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No records found for '$lastSearchedNumber'",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Ensure record exists at '/bikes/$lastSearchedNumber.json'",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏍️ Real-time Data for Bike: ${data.bikeNumber}",
                                color = MotorOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                              )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Render Tabular Form based on active selection
                            when (selectedTab) {
                                0 -> {
                                    // 1st Type: ENGINE OIL
                                    // Columns: Date Of Service, Kilometer, next service, Remarks
                                    val headers = listOf("Date Of Service", "Kilometer", "next service", "KM Driven", "Remarks")
                                    val widths = listOf(130.dp, 100.dp, 120.dp, 110.dp, 180.dp)
                                    val rows = data.engineOilList.mapIndexed { index, record ->
                                        val kmDriven = if (index > 0) {
                                            val currKm = extractKmValue(record.nextService)
                                            val prevKm = extractKmValue(data.engineOilList[index - 1].nextService)
                                            if (currKm != null && prevKm != null) {
                                                val diff = (currKm - prevKm).toLong()
                                                "$diff KM"
                                            } else ""
                                        } else ""

                                        listOf(
                                            formatFirebaseDate(record.dateOfService),
                                            record.kilometer,
                                            formatFirebaseDate(record.nextService),
                                            kmDriven,
                                            record.remarks
                                        )
                                    }
                                    
                                    TabularSection(
                                        title = "ENGINE OIL",
                                        headers = headers,
                                        widths = widths,
                                        rows = rows
                                    )
                                }
                                1 -> {
                                    // 2nd Type: BIKES VISIT
                                    // Columns: Date, TALABAT ID, KM RUN, JOB CARD, JOB DONE, Service type, MECHANIC
                                    val headers = listOf("Date", "TALABAT ID", "KM RUN", "JOB CARD", "JOB DONE", "Service type", "MECHANIC")
                                    val widths = listOf(110.dp, 100.dp, 85.dp, 95.dp, 140.dp, 110.dp, 110.dp)
                                    val rows = data.bikeVisitsList.map { record ->
                                        listOf(
                                            formatFirebaseDate(record.date),
                                            record.talabatId,
                                            record.kmRun,
                                            record.jobCard,
                                            record.jobDone,
                                            record.serviceType,
                                            record.mechanic
                                        )
                                    }
                                    
                                    TabularSection(
                                        title = "Without Parts",
                                        headers = headers,
                                        widths = widths,
                                        rows = rows
                                    )
                                }
                                2 -> {
                                    // 3rd Type: EXPORTED REPORT
                                    // Columns: Type, Vch No, Vch Date, Item Name, Mechanic
                                    val headers = listOf("Type", "Vch No", "Vch Date", "Item Name", "Mechanic")
                                    val widths = listOf(110.dp, 90.dp, 110.dp, 150.dp, 110.dp)
                                    val rows = data.exportedReportsList.map { record ->
                                        listOf(
                                            record.type,
                                            record.vchNo,
                                            formatFirebaseDate(record.vchDate),
                                            record.itemName,
                                            record.mechanic
                                        )
                                    }
                                    
                                    TabularSection(
                                        title = "With Parts",
                                        headers = headers,
                                        widths = widths,
                                        rows = rows
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

@Composable
fun TabularSectionSkeleton(
    headers: List<String>,
    widths: List<Dp>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tabular_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.width(widths.map { it.value }.sum().dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkElevated)
                            .padding(vertical = 10.dp)
                    ) {
                        headers.forEachIndexed { index, header ->
                            Text(
                                text = header,
                                color = MotorOrange.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .width(widths[index])
                                    .padding(horizontal = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp)

                    val isLight = MaterialTheme.colorScheme.background == LightSlateDark
                    val evenRowColor = if (isLight) MotorOrange.copy(alpha = 0.06f) else MotorOrange.copy(alpha = 0.08f)
                    val oddRowColor = SurfaceDark

                    for (rowIndex in 0..3) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (rowIndex % 2 == 0) evenRowColor else oddRowColor)
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            widths.forEach { width ->
                                Box(
                                    modifier = Modifier
                                        .width(width)
                                        .padding(horizontal = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.8f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(BorderColor.copy(alpha = alpha))
                                    )
                                }
                            }
                        }
                        if (rowIndex < 3) {
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.15f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabularSection(
    title: String,
    headers: List<String>,
    widths: List<Dp>,
    rows: List<List<String>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            // Horizontal scroll wrapper to contain digital grid columns perfectly
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.width(widths.map { it.value }.sum().dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkElevated)
                            .padding(vertical = 10.dp)
                    ) {
                        headers.forEachIndexed { index, header ->
                            Text(
                                text = header,
                                color = MotorOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .width(widths[index])
                                    .padding(horizontal = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 1.dp)

                    if (rows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No records found in this category",
                                color = TextDisabled,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        val isLight = MaterialTheme.colorScheme.background == LightSlateDark
                        val evenRowColor = if (isLight) MotorOrange.copy(alpha = 0.06f) else MotorOrange.copy(alpha = 0.08f)
                        val oddRowColor = SurfaceDark

                        rows.forEachIndexed { rowIndex, rowData ->
                            val defaultRowColor = if (rowIndex % 2 == 0) evenRowColor else oddRowColor

                            val kmDrivenColIndex = headers.indexOf("KM Driven")
                            val kmDrivenVal = if (kmDrivenColIndex >= 0 && kmDrivenColIndex < rowData.size) {
                                val str = rowData[kmDrivenColIndex]
                                str.replace(Regex("[^0-9]"), "").toLongOrNull()
                            } else null

                            val rowBgColor = when {
                                kmDrivenVal != null && kmDrivenVal > 4000 -> Color(0x3DF87171) // Soft Red
                                kmDrivenVal != null && kmDrivenVal > 3300 -> Color(0x3BF59E0B) // More Yellow
                                kmDrivenVal != null && kmDrivenVal > 3000 -> Color(0x22F59E0B) // Light Yellow
                                else -> defaultRowColor
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBgColor)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowData.forEachIndexed { colIndex, cellValue ->
                                    Text(
                                        text = cellValue.ifEmpty { "-" },
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .width(widths[colIndex])
                                            .padding(horizontal = 8.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (rowIndex < rows.lastIndex) {
                                HorizontalDivider(color = BorderColor.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatFirebaseDate(rawDate: String): String {
    if (rawDate.isBlank() || rawDate == "N/A" || rawDate == "-") return rawDate
    try {
        val cleanDate = rawDate.trim()
        
        // 1. Check if ISO 8601 (contains T)
        if (cleanDate.contains("T")) {
            val datePart = cleanDate.split("T")[0] // "2025-01-01"
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                val monthStr = getMonthAbbreviation(month)
                val shortYear = if (year.length >= 4) year.substring(year.length - 2) else year
                val formattedDay = if (day.length == 1) "0$day" else day
                return "$formattedDay-$monthStr-$shortYear"
            }
        }
        
        // 2. Check if YYYY-MM-DD format (like "2025-01-01")
        val partsYMD = cleanDate.split("-")
        if (partsYMD.size == 3 && partsYMD[0].length == 4) {
            val year = partsYMD[0]
            val month = partsYMD[1]
            val day = partsYMD[2]
            val monthStr = getMonthAbbreviation(month)
            val shortYear = year.substring(2)
            val formattedDay = if (day.length == 1) "0$day" else day
            return "$formattedDay-$monthStr-$shortYear"
        }
        
        // 3. Check if DD-MM-YYYY format (like "01-01-2025")
        if (partsYMD.size == 3 && partsYMD[2].length == 4) {
            val day = partsYMD[0]
            val month = partsYMD[1]
            val year = partsYMD[2]
            val monthStr = getMonthAbbreviation(month)
            val shortYear = year.substring(2)
            val formattedDay = if (day.length == 1) "0$day" else day
            return "$formattedDay-$monthStr-$shortYear"
        }

        // 4. Check if separated by slashes YYYY/MM/DD or DD/MM/YYYY
        val partsSlash = cleanDate.split("/")
        if (partsSlash.size == 3) {
            if (partsSlash[0].length == 4) {
                val year = partsSlash[0]
                val month = partsSlash[1]
                val day = partsSlash[2]
                val monthStr = getMonthAbbreviation(month)
                val shortYear = year.substring(2)
                val formattedDay = if (day.length == 1) "0$day" else day
                return "$formattedDay-$monthStr-$shortYear"
            } else if (partsSlash[2].length == 4) {
                val day = partsSlash[0]
                val month = partsSlash[1]
                val year = partsSlash[2]
                val monthStr = getMonthAbbreviation(month)
                val shortYear = year.substring(2)
                val formattedDay = if (day.length == 1) "0$day" else day
                return "$formattedDay-$monthStr-$shortYear"
            }
        }
    } catch (e: Exception) {
        // fallback
    }
    return rawDate
}

private fun getMonthAbbreviation(month: String): String {
    return when (month.trim().lowercase().removePrefix("0")) {
        "1", "jan", "01" -> "Jan"
        "2", "feb", "02" -> "Feb"
        "3", "mar", "03" -> "Mar"
        "4", "apr", "04" -> "Apr"
        "5", "may", "05" -> "May"
        "6", "jun", "06" -> "Jun"
        "7", "jul", "07" -> "Jul"
        "8", "aug", "08" -> "Aug"
        "9", "sep", "09" -> "Sep"
        "10", "oct" -> "Oct"
        "11", "nov" -> "Nov"
        "12", "dec" -> "Dec"
        else -> "Jan"
    }
}

private fun extractKmValue(str: String): Double? {
    if (str.isBlank()) return null
    val trimmed = str.trim()
    if (trimmed.contains("-") && trimmed.split("-").size == 3 && trimmed.length >= 8) {
        return null
    }
    val cleaned = trimmed.replace(",", "").replace(Regex("[^0-9.]"), "")
    return cleaned.toDoubleOrNull()
}

