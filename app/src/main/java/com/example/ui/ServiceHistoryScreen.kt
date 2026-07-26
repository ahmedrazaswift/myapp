package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.clip
import com.example.ui.components.bounceOnClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.example.data.MotorcycleEntity
import com.example.ui.theme.*

@Composable
fun ServiceHistoryScreen(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val records by viewModel.serviceHistoryRecords.collectAsState()
    val isGoogleSheet = true
    val googleSheetError by viewModel.googleSheetError.collectAsState()

    val googleSheetOilRecords by viewModel.googleSheetOilRecords.collectAsState()
    val localOilRecords by viewModel.oilHistoryRecords.collectAsState(initial = emptyList())
    val oilRecords = if (isGoogleSheet) googleSheetOilRecords else localOilRecords

    val googleSheetNoPartsRecords by viewModel.googleSheetNoPartsRecords.collectAsState()
    val localNoPartsRecords by viewModel.serviceWithoutPartsRecords.collectAsState(initial = emptyList())
    val noPartsRecords = if (isGoogleSheet) googleSheetNoPartsRecords else localNoPartsRecords

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Digital Filter Panel State
    var showAdvancedFilters by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Service W/Parts") }
    val tabs = listOf("Service W/Parts", "Oil history", "Service without parts", "Photos")
    var tableZoomScale by remember { mutableStateOf(1.0f) }
    val selectedYear by viewModel.selectedYear.collectAsState()
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    // Collect photo uploads for the bike plate
    val photoUploads by remember(searchQuery) {
        if (searchQuery.trim().isNotEmpty()) {
            viewModel.getPhotoUploadsForBike(searchQuery.trim().uppercase())
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    var zoomedPhotoPath by remember { mutableStateOf<String?>(null) }
    var zoomedPhotoTitle by remember { mutableStateOf("") }
    var selectedMechanic by remember { mutableStateOf("All") }
    var selectedDateRange by remember { mutableStateOf("All") }
    var selectedKmRange by remember { mutableStateOf("All") }
    var itemKeywordFilter by remember { mutableStateOf("") }

    // Extract unique mechanics from loaded records
    val mechanics = remember(records) {
        listOf("All") + records.map { it.mechanic_name.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    }

    // High-performance live filtered records
    val filteredRecords = remember(records, selectedMechanic, selectedDateRange, selectedKmRange, itemKeywordFilter) {
        records.filter { record ->
            // 1. Mechanic filter
            val matchesMechanic = selectedMechanic == "All" || record.mechanic_name.trim().equals(selectedMechanic.trim(), ignoreCase = true)
            
            // 2. Keyword filter
            val matchesKeyword = itemKeywordFilter.isEmpty() || 
                    record.item_name.contains(itemKeywordFilter, ignoreCase = true) ||
                    record.voucher_number.contains(itemKeywordFilter, ignoreCase = true) ||
                    record.mechanic_name.contains(itemKeywordFilter, ignoreCase = true)

            // 3. KM Range filter
            val kmString = record.quantity.replace(Regex("[^0-9.]"), "")
            val kmVal = kmString.toDoubleOrNull() ?: 0.0
            val matchesKm = when (selectedKmRange) {
                "Low (<10k)" -> kmVal < 10000.0
                "Mid (10k-50k)" -> kmVal in 10000.0..50000.0
                "High (>50k)" -> kmVal > 50000.0
                else -> true
            }

            // 4. Date Range filter (safely match current month/year or presets)
            val matchesDate = when (selectedDateRange) {
                "This Month" -> {
                    record.voucher_date.contains("/07/") || record.voucher_date.contains("-07-") || record.voucher_date.contains("/7/")
                }
                "This Year" -> {
                    record.voucher_date.contains("2026") || record.voucher_date.endsWith("26") || record.voucher_date.endsWith("2026")
                }
                else -> true
            }

            matchesMechanic && matchesKeyword && matchesKm && matchesDate
        }
    }

    // Native CSV Save Dialog using Activity Results
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val csvContent = generateCsvContent(filteredRecords)
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportOilLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    val csvContent = generateOilCsvContent(oilRecords)
                    outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                    Toast.makeText(context, "Oil CSV exported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(16.dp)
    ) {
        // Combined Search Box with internal Search action and adjacent Year Toggle Buttons ([2025] | [2026])
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Enter bike number...", color = TextDisabled, fontSize = 13.sp, maxLines = 1) },
                trailingIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.updateSearchQuery("") },
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
                                    val bikeQuery = searchQuery.trim()
                                    if (bikeQuery.isNotEmpty()) {
                                        keyboardController?.hide()
                                        viewModel.refreshData(query = bikeQuery) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Successfully retrieved service history!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to retrieve. Please verify credentials/network.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(context, "Please enter a bike number", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRefreshing) {
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        val bikeQuery = searchQuery.trim()
                        if (bikeQuery.isNotEmpty()) {
                            keyboardController?.hide()
                            viewModel.refreshData(query = bikeQuery) { success ->
                                if (success) {
                                    Toast.makeText(context, "Successfully retrieved service history!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to retrieve. Please verify credentials/network.", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please enter a bike number", Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MotorOrange,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = SurfaceDarkElevated,
                    unfocusedContainerColor = SurfaceDarkElevated,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                                    val bikeQuery = searchQuery.trim()
                                    if (bikeQuery.isNotEmpty()) {
                                        viewModel.refreshData(query = bikeQuery) { success -> }
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

        Spacer(modifier = Modifier.height(12.dp))

        // Options row below search box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) MotorOrange.copy(alpha = 0.15f) else SurfaceDarkElevated,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) MotorOrange else BorderColor,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { activeTab = tab }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = tab,
                        color = if (isSelected) MotorOrange else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (googleSheetError != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = googleSheetError!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Database History Table Layout
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isRefreshing) {
                ServiceHistorySkeleton()
            } else if (searchQuery.trim().isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Enter a bike number above to search.",
                            color = TextDisabled,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Search by bike number to view service history, oil records, and verification photos.",
                            color = TextDisabled.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (activeTab != "Photos") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💡 Pinch table to zoom in/out • Swipe horizontally to scroll",
                                color = TextDisabled,
                                fontSize = 11.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Text(
                                text = "Scale: ${(tableZoomScale * 100).toInt()}%",
                                color = MotorOrange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (activeTab) {
                    "Service W/Parts" -> {
                        if (records.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No records found for \"$searchQuery\"",
                                        color = TextDisabled,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Checked collections: service_history, motorcycle_records, etc.",
                                        color = TextDisabled.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "💡 Tip: Search '*' or 'all' to retrieve and list all records in the database to verify loaded data structure.",
                                        color = MotorOrange,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                    // Modern Control Panel Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "🏍️ Bike: $searchQuery",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Showing ${filteredRecords.size} of ${records.size} entries",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tune/Filter Button
                        IconButton(
                            onClick = { showAdvancedFilters = !showAdvancedFilters },
                            modifier = Modifier
                                .size(44.dp)
                                .background(if (showAdvancedFilters) MotorOrange.copy(alpha = 0.15f) else SurfaceDarkElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, if (showAdvancedFilters) MotorOrange else BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Advanced Filters",
                                tint = if (showAdvancedFilters) MotorOrange else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Export CSV Button
                        IconButton(
                            onClick = {
                                if (filteredRecords.isNotEmpty()) {
                                    exportLauncher.launch("Service_History_${searchQuery.trim().replace(" ", "_")}.csv")
                                } else {
                                    Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .background(SurfaceDarkElevated, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export CSV",
                                tint = MotorOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Advanced Digital Filter Panel (collapsible)
                    AnimatedVisibility(visible = showAdvancedFilters) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Title & Reset Control
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(MotorOrange, RoundedCornerShape(50))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "DIGITAL FILTER PANEL",
                                            color = MotorOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    if (selectedMechanic != "All" || selectedDateRange != "All" || selectedKmRange != "All" || itemKeywordFilter.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                selectedMechanic = "All"
                                                selectedDateRange = "All"
                                                selectedKmRange = "All"
                                                itemKeywordFilter = ""
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Clear Filters", color = MotorOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 1. Keyword search inside filtered results
                                OutlinedTextField(
                                    value = itemKeywordFilter,
                                    onValueChange = { itemKeywordFilter = it },
                                    placeholder = { Text("Filter by item, mechanic, or voucher number...", color = TextDisabled, fontSize = 11.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    trailingIcon = {
                                        if (itemKeywordFilter.isNotEmpty()) {
                                            IconButton(onClick = { itemKeywordFilter = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextDisabled, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MotorOrange,
                                        unfocusedBorderColor = BorderColor,
                                        focusedContainerColor = SurfaceDark,
                                        unfocusedContainerColor = SurfaceDark,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                // 2. Mechanic filter (Chips)
                                Column {
                                    Text("Filter by Mechanic:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        mechanics.forEach { mech ->
                                            val isSelected = selectedMechanic == mech
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (isSelected) MotorOrange.copy(alpha = 0.15f) else SurfaceDark,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) MotorOrange else BorderColor,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { selectedMechanic = mech }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = mech,
                                                    color = if (isSelected) MotorOrange else TextSecondary,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Two columns for Date & KM ranges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Date Ranges Column
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Date Filter:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("All", "This Month", "This Year").forEach { dateOpt ->
                                                val isSelected = selectedDateRange == dateOpt
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isSelected) MotorOrange.copy(alpha = 0.15f) else SurfaceDark,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) MotorOrange else BorderColor,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { selectedDateRange = dateOpt }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = dateOpt,
                                                        color = if (isSelected) MotorOrange else TextSecondary,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // KM Ranges Column
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Kilometer Filter:", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf("All", "Low (<10k)", "Mid (10k-50k)", "High (>50k)").forEach { kmOpt ->
                                                val isSelected = selectedKmRange == kmOpt
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isSelected) MotorOrange.copy(alpha = 0.15f) else SurfaceDark,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSelected) MotorOrange else BorderColor,
                                                            RoundedCornerShape(8.dp)
                                                        )
                                                        .clickable { selectedKmRange = kmOpt }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = kmOpt,
                                                        color = if (isSelected) MotorOrange else TextSecondary,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Table Content with Zoom and Horizontal Scroll
                    val tableWidth = (800 * tableZoomScale).dp
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    val newScale = (tableZoomScale * zoom).coerceIn(0.4f, 2.0f)
                                    tableZoomScale = newScale
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                            Column(modifier = Modifier.width(tableWidth).fillMaxHeight()) {
                                // Table Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceDark)
                                        .padding(horizontal = (12 * tableZoomScale).dp, vertical = (10 * tableZoomScale).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Date", modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                    Text(text = "Vch No.", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                    Text(text = "Item Name", modifier = Modifier.weight(4.0f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                    Text(text = "Mechanic", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                }
                                HorizontalDivider(color = BorderColor)

                                if (filteredRecords.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No records match the active filters.",
                                            color = TextSecondary,
                                            fontSize = (13 * tableZoomScale).sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        itemsIndexed(filteredRecords) { index, record ->
                                            val interactionSource = remember { MutableInteractionSource() }
                                            val isHovered by interactionSource.collectIsHoveredAsState()
                                            val targetBgColor = if (isHovered) {
                                                MotorOrange.copy(alpha = 0.12f)
                                            } else {
                                                if (index % 2 == 0) SurfaceDark else SurfaceDarkElevated
                                            }
                                            val rowBgColor by animateColorAsState(targetValue = targetBgColor)

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .hoverable(interactionSource)
                                                    .background(rowBgColor)
                                                    .padding(horizontal = (12 * tableZoomScale).dp, vertical = (12 * tableZoomScale).dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. Date
                                                Text(
                                                    text = record.voucher_date,
                                                    modifier = Modifier.weight(2.0f),
                                                    color = TextPrimary,
                                                    fontSize = (12 * tableZoomScale).sp,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                // 2. Voucher Number Badge
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1.8f)
                                                        .padding(end = (4 * tableZoomScale).dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(BorderColor.copy(alpha = 0.15f), RoundedCornerShape((4 * tableZoomScale).dp))
                                                            .border(0.5.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape((4 * tableZoomScale).dp))
                                                            .padding(horizontal = (6 * tableZoomScale).dp, vertical = (2 * tableZoomScale).dp)
                                                    ) {
                                                        Text(
                                                            text = record.voucher_number,
                                                            color = TextPrimary,
                                                            fontSize = (11 * tableZoomScale).sp,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                // 3. Item Name (Clean & prominent)
                                                Text(
                                                    text = record.item_name,
                                                    modifier = Modifier.weight(4.0f),
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (12 * tableZoomScale).sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // 4. Mechanic (Soft Gray tag)
                                                Box(
                                                    modifier = Modifier.weight(2.2f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(BorderColor.copy(alpha = 0.2f), RoundedCornerShape((6 * tableZoomScale).dp))
                                                            .padding(horizontal = (8 * tableZoomScale).dp, vertical = (3 * tableZoomScale).dp)
                                                    ) {
                                                        Text(
                                                            text = record.mechanic_name,
                                                            color = TextSecondary,
                                                            fontSize = (11 * tableZoomScale).sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                            if (index < filteredRecords.size - 1) {
                                                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
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
                    "Oil history" -> {
                        if (oilRecords.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No oil history found for \"$searchQuery\"",
                                        color = TextDisabled,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ensure that the Oil History Web App is correctly configured in the Data Input Center.",
                                        color = TextDisabled.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Modern Control Panel Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "🏍️ Bike: $searchQuery",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Showing ${oilRecords.size} records from source",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Export CSV Button
                                    IconButton(
                                        onClick = {
                                            if (oilRecords.isNotEmpty()) {
                                                exportOilLauncher.launch("Oil_History_${searchQuery.trim().replace(" ", "_")}.csv")
                                            } else {
                                                Toast.makeText(context, "No records to export", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(44.dp)
                                            .background(SurfaceDarkElevated, RoundedCornerShape(12.dp))
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export Oil History CSV",
                                            tint = MotorOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                // Table Content with Zoom and Horizontal Scroll
                                val tableWidth = (800 * tableZoomScale).dp
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, _, zoom, _ ->
                                                val newScale = (tableZoomScale * zoom).coerceIn(0.4f, 2.0f)
                                                tableZoomScale = newScale
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                                        Column(modifier = Modifier.width(tableWidth).fillMaxHeight()) {
                                            // Table Header Row
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SurfaceDark)
                                                    .padding(horizontal = (12 * tableZoomScale).dp, vertical = (10 * tableZoomScale).dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "Date", modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Current KM", modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Next Oil Change", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "KM Driven", modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Remarks", modifier = Modifier.weight(3.5f), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                            }
                                            HorizontalDivider(color = BorderColor)

                                            // Table Rows Content
                                            LazyColumn(
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                itemsIndexed(oilRecords) { index, record ->
                                                    val interactionSource = remember { MutableInteractionSource() }
                                                    val isHovered by interactionSource.collectIsHoveredAsState()

                                                    val kmDrivenText = if (index > 0) {
                                                        val currKm = extractKmValue(record.nextService)
                                                        val prevKm = extractKmValue(oilRecords[index - 1].nextService)
                                                        if (currKm != null && prevKm != null) {
                                                            val diff = (currKm - prevKm).toLong()
                                                            "$diff KM"
                                                        } else ""
                                                    } else ""

                                                    val kmDrivenVal = if (index > 0) {
                                                        val currKm = extractKmValue(record.nextService)
                                                        val prevKm = extractKmValue(oilRecords[index - 1].nextService)
                                                        if (currKm != null && prevKm != null) {
                                                            (currKm - prevKm).toLong()
                                                        } else null
                                                    } else null

                                                    val defaultRowBg = if (index % 2 == 0) SurfaceDark else SurfaceDarkElevated
                                                    val kmHighlightBg = when {
                                                        kmDrivenVal != null && kmDrivenVal > 4000 -> Color(0x3DF87171) // Soft Red
                                                        kmDrivenVal != null && kmDrivenVal > 3300 -> Color(0x3BF59E0B) // More Yellow
                                                        kmDrivenVal != null && kmDrivenVal > 3000 -> Color(0x22F59E0B) // Light Yellow
                                                        else -> null
                                                    }

                                                    val targetBgColor = if (isHovered) {
                                                        MotorOrange.copy(alpha = 0.15f)
                                                    } else {
                                                        kmHighlightBg ?: defaultRowBg
                                                    }
                                                    val rowBgColor by animateColorAsState(targetValue = targetBgColor)

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .hoverable(interactionSource)
                                                            .background(rowBgColor)
                                                            .padding(horizontal = (12 * tableZoomScale).dp, vertical = (12 * tableZoomScale).dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // 1. Date
                                                        Text(
                                                            text = record.month,
                                                            modifier = Modifier.weight(2.0f),
                                                            color = TextPrimary,
                                                            fontSize = (12 * tableZoomScale).sp,
                                                            fontWeight = FontWeight.Medium
                                                        )

                                                        // 2. Current Kilometer
                                                        Text(
                                                            text = "${record.kilometer} KM",
                                                            modifier = Modifier.weight(2.0f),
                                                            color = TextPrimary,
                                                            fontSize = (12 * tableZoomScale).sp,
                                                            fontWeight = FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        // 3. Next Oil Change
                                                        Text(
                                                            text = record.nextService,
                                                            modifier = Modifier.weight(2.2f),
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = (12 * tableZoomScale).sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        // 4. KM Driven
                                                        Text(
                                                            text = kmDrivenText,
                                                            modifier = Modifier.weight(2.0f),
                                                            color = TextPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = (12 * tableZoomScale).sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        // 5. Remarks
                                                        Text(
                                                            text = record.remarks,
                                                            modifier = Modifier.weight(3.5f),
                                                            color = TextSecondary,
                                                            fontSize = (11 * tableZoomScale).sp,
                                                            maxLines = 3,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    if (index < oilRecords.size - 1) {
                                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Service without parts" -> {
                        if (noPartsRecords.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "No service without parts found for \"$searchQuery\"",
                                        color = TextDisabled,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ensure that the Service Without Parts Web App URL is correctly configured in the Data Input Center.",
                                        color = TextDisabled.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Modern Control Panel Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "🏍️ Bike: $searchQuery",
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Showing ${noPartsRecords.size} service without parts records",
                                                    color = TextSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                // Double-scrollable or horizontally scrollable table block with Zoom and Horizontal Scroll
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, _, zoom, _ ->
                                                val newScale = (tableZoomScale * zoom).coerceIn(0.4f, 2.0f)
                                                tableZoomScale = newScale
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                    border = BorderStroke(1.dp, BorderColor)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
                                        Column {
                                            // Table Header Row
                                            Row(
                                                modifier = Modifier
                                                    .background(SurfaceDark)
                                                    .padding(horizontal = (12 * tableZoomScale).dp, vertical = (12 * tableZoomScale).dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "Date", modifier = Modifier.width((100 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Talabat ID", modifier = Modifier.width((110 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "KM Run", modifier = Modifier.width((90 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Job Card", modifier = Modifier.width((100 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Job Done", modifier = Modifier.width((180 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Service Type", modifier = Modifier.width((120 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                                Text(text = "Mechanic", modifier = Modifier.width((120 * tableZoomScale).dp), fontWeight = FontWeight.Bold, color = MotorOrange, fontSize = (11 * tableZoomScale).sp)
                                            }
                                            HorizontalDivider(color = BorderColor)

                                            // Table Rows
                                            LazyColumn(
                                                modifier = Modifier.fillMaxHeight().wrapContentWidth()
                                            ) {
                                                itemsIndexed(noPartsRecords) { index, record ->
                                                    val interactionSource = remember { MutableInteractionSource() }
                                                    val isHovered by interactionSource.collectIsHoveredAsState()
                                                    val targetBgColor = if (isHovered) {
                                                        MotorOrange.copy(alpha = 0.12f)
                                                    } else {
                                                        if (index % 2 == 0) SurfaceDark else SurfaceDarkElevated
                                                    }
                                                    val rowBgColor by animateColorAsState(targetValue = targetBgColor)

                                                    Row(
                                                        modifier = Modifier
                                                            .hoverable(interactionSource)
                                                            .background(rowBgColor)
                                                            .padding(horizontal = (12 * tableZoomScale).dp, vertical = (12 * tableZoomScale).dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = record.date, modifier = Modifier.width((100 * tableZoomScale).dp), color = TextPrimary, fontSize = (12 * tableZoomScale).sp, fontWeight = FontWeight.Medium)
                                                        Text(text = record.talabatId, modifier = Modifier.width((110 * tableZoomScale).dp), color = TextSecondary, fontSize = (12 * tableZoomScale).sp)
                                                        
                                                        // KM Run custom box
                                                        Box(modifier = Modifier.width((90 * tableZoomScale).dp).padding(end = (8 * tableZoomScale).dp)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(MotorOrange.copy(alpha = 0.1f), RoundedCornerShape((6 * tableZoomScale).dp))
                                                                    .border(1.dp, MotorOrange.copy(alpha = 0.25f), RoundedCornerShape((6 * tableZoomScale).dp))
                                                                    .padding(horizontal = (8 * tableZoomScale).dp, vertical = (3 * tableZoomScale).dp)
                                                            ) {
                                                                Text(
                                                                    text = "${record.kmRun} KM",
                                                                    color = MotorOrange,
                                                                    fontSize = (11 * tableZoomScale).sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                        
                                                        Text(text = record.jobCard, modifier = Modifier.width((100 * tableZoomScale).dp), color = TextPrimary, fontSize = (12 * tableZoomScale).sp)
                                                        Text(text = record.jobDone, modifier = Modifier.width((180 * tableZoomScale).dp), color = TextSecondary, fontSize = (12 * tableZoomScale).sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                                        Text(text = record.serviceType, modifier = Modifier.width((120 * tableZoomScale).dp), color = TextPrimary, fontSize = (12 * tableZoomScale).sp, fontWeight = FontWeight.SemiBold)
                                                        Text(text = record.mechanic, modifier = Modifier.width((120 * tableZoomScale).dp), color = TextSecondary, fontSize = (12 * tableZoomScale).sp)
                                                    }
                                                    if (index < noPartsRecords.size - 1) {
                                                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Photos" -> {
                        if (photoUploads.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No rider portal upload photos found for \"$searchQuery\"",
                                    color = TextDisabled,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(photoUploads.size) { index ->
                                    val photo = photoUploads[index]
                                    val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(photo.uploadTimestamp))
                                    val perspectiveText = "ID #${photo.id} (${photo.uploadMonth}) - $dateStr"
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clickable {
                                                zoomedPhotoPath = photo.photoUri
                                                zoomedPhotoTitle = perspectiveText
                                            },
                                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .background(SlateDark),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                val file = java.io.File(photo.photoUri)
                                                if (file.exists()) {
                                                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                                    if (bitmap != null) {
                                                        androidx.compose.foundation.Image(
                                                            bitmap = bitmap.asImageBitmap(),
                                                            contentDescription = perspectiveText,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(Icons.Default.Close, contentDescription = "Error loading image", tint = TextDisabled)
                                                    }
                                                } else {
                                                    Icon(Icons.Default.Close, contentDescription = "Image file not found", tint = TextDisabled)
                                                }
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SurfaceDarkElevated)
                                                    .padding(8.dp)
                                            ) {
                                                Text(
                                                    text = perspectiveText,
                                                    color = TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (photo.rating != null) {
                                                    Text(
                                                        text = "⭐ Rating: ${String.format("%.1f", photo.rating)}/10.0",
                                                        color = MotorOrange,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                                if (!photo.assessmentSummary.isNullOrEmpty()) {
                                                    Text(
                                                        text = photo.assessmentSummary,
                                                        color = TextSecondary,
                                                        fontSize = 9.sp,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(top = 1.dp)
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
    }

            // Zoom Dialog overlay
            if (zoomedPhotoPath != null) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { zoomedPhotoPath = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = zoomedPhotoTitle,
                                    color = MotorOrange,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { zoomedPhotoPath = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .background(SlateDark),
                                contentAlignment = Alignment.Center
                            ) {
                                val file = java.io.File(zoomedPhotoPath!!)
                                if (file.exists()) {
                                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    if (bitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = zoomedPhotoTitle,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    } else {
                                        Text("Error loading image", color = TextDisabled)
                                    }
                                } else {
                                    Text("Image file not found", color = TextDisabled)
                                }
                            }
                            val matchedPhoto = photoUploads.find { it.photoUri == zoomedPhotoPath }
                            if (matchedPhoto != null) {
                                if (matchedPhoto.rating != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "⭐ Condition Rating: ${String.format("%.1f", matchedPhoto.rating)}/10.0",
                                        color = MotorOrange,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (!matchedPhoto.assessmentSummary.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = matchedPhoto.assessmentSummary,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Central Loading Spinner Overlay
            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SlateDark.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MotorOrange,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

// Utility CSV helper methods at the bottom of the file
private fun generateCsvContent(records: List<MotorcycleEntity>): String {
    val sb = java.lang.StringBuilder()
    sb.append("Date,Voucher Number,Item Name,Mechanic Name\n")
    for (record in records) {
        val date = escapeCsv(record.voucher_date)
        val vchNo = escapeCsv(record.voucher_number)
        val itemName = escapeCsv(record.item_name)
        val mech = escapeCsv(record.mechanic_name)
        sb.append("$date,$vchNo,$itemName,$mech\n")
    }
    return sb.toString()
}

private fun escapeCsv(value: String): String {
    var clean = value.replace("\"", "\"\"")
    if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
        clean = "\"$clean\""
    }
    return clean
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

private fun generateOilCsvContent(records: List<com.example.data.OilHistoryRecord>): String {
    val sb = java.lang.StringBuilder()
    sb.append("Date,Current Kilometer,Next Oil Change,KM Driven,Remarks\n")
    for ((index, record) in records.withIndex()) {
        val date = escapeCsv(record.month)
        val km = escapeCsv(record.kilometer.toString())
        val next = escapeCsv(record.nextService)
        val kmDriven = if (index > 0) {
            val currKm = extractKmValue(record.nextService)
            val prevKm = extractKmValue(records[index - 1].nextService)
            if (currKm != null && prevKm != null) {
                "${(currKm - prevKm).toLong()} KM"
            } else ""
        } else ""
        val remarks = escapeCsv(record.remarks)
        sb.append("$date,$km,$next,${escapeCsv(kmDriven)},$remarks\n")
    }
    return sb.toString()
}

@Composable
fun ServiceHistorySkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Skeleton Control Panel (matching the "Modern Control Panel Row")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(SurfaceDarkElevated.copy(alpha = alpha), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SurfaceDarkElevated.copy(alpha = alpha), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SurfaceDarkElevated.copy(alpha = alpha), RoundedCornerShape(12.dp))
                    .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )
        }

        // 2. Skeleton Table Header (matching "Table Header Row")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Date", modifier = Modifier.weight(2.0f), fontWeight = FontWeight.Bold, color = MotorOrange.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(text = "Vch No.", modifier = Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = MotorOrange.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(text = "Item Name", modifier = Modifier.weight(4.0f), fontWeight = FontWeight.Bold, color = MotorOrange.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(text = "Mechanic", modifier = Modifier.weight(2.2f), fontWeight = FontWeight.Bold, color = MotorOrange.copy(alpha = 0.5f), fontSize = 11.sp)
            }
        }

        // 3. Skeleton Table Rows Content
        Card(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                repeat(6) { index ->
                    val rowBgColor = if (index % 2 == 0) SurfaceDark else SurfaceDarkElevated
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBgColor)
                            .padding(horizontal = 12.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Date Skeleton
                        Box(
                            modifier = Modifier
                                .weight(2.0f)
                                .height(12.dp)
                                .fillMaxWidth(0.7f)
                                .background(BorderColor.copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 2. Voucher Number Badge Skeleton
                        Box(
                            modifier = Modifier
                                .weight(1.8f)
                                .padding(end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(18.dp)
                                    .background(BorderColor.copy(alpha = alpha), RoundedCornerShape(4.dp))
                            )
                        }

                        // 3. Item Name Skeleton
                        Box(
                            modifier = Modifier
                                .weight(4.0f)
                                .height(12.dp)
                                .fillMaxWidth(0.85f)
                                .background(BorderColor.copy(alpha = alpha), RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 4. Mechanic tag Skeleton
                        Box(
                            modifier = Modifier.weight(2.2f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(20.dp)
                                    .background(BorderColor.copy(alpha = alpha * 0.5f), RoundedCornerShape(6.dp))
                            )
                        }
                    }
                    if (index < 5) {
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}


