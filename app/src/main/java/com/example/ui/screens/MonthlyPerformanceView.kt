package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StaffMember
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

fun cleanMechanicName(input: String): String {
    var s = input.trim()
    if (s.contains("+")) {
        s = try { java.net.URLDecoder.decode(s, "UTF-8") } catch (e: Exception) { s.replace("+", " ") }
    }
    return s.replace("+", " ").replace(Regex("\\s+"), " ").trim()
}

fun isSameMechanic(rawA: String, rawB: String): Boolean {
    if (rawA.isBlank() || rawB.isBlank()) return false
    val cleanA = cleanMechanicName(rawA)
    val cleanB = cleanMechanicName(rawB)
    if (cleanA.equals(cleanB, ignoreCase = true)) return true
    
    val alphaA = cleanA.replace(Regex("^\\d+[-:]*"), "").trim()
    val alphaB = cleanB.replace(Regex("^\\d+[-:]*"), "").trim()
    if (alphaA.isNotBlank() && alphaB.isNotBlank()) {
        if (alphaA.equals(alphaB, ignoreCase = true)) return true
        if (alphaA.contains(alphaB, ignoreCase = true) || alphaB.contains(alphaA, ignoreCase = true)) return true
    }
    return false
}

// Robust date month parser & checker
fun dateMatchesMonth(dateStr: String, targetMonth: String): Boolean {
    val cleanedDate = dateStr.lowercase().trim()
    val cleanedMonth = targetMonth.lowercase().trim()
    
    val monthMap = mapOf(
        "jan" to "01", "feb" to "02", "mar" to "03", "apr" to "04",
        "may" to "05", "jun" to "06", "jul" to "07", "aug" to "08",
        "sep" to "09", "oct" to "10", "nov" to "11", "dec" to "12",
        "january" to "01", "february" to "02", "march" to "03", "april" to "04",
        "june" to "06", "july" to "07", "august" to "08", "september" to "09",
        "october" to "10", "november" to "11", "december" to "12"
    )
    
    var targetM = ""
    var targetY = ""
    for (k in monthMap.keys) {
        if (cleanedMonth.contains(k)) {
            targetM = monthMap[k] ?: ""
            break
        }
    }
    if (targetM.isEmpty()) {
        val match = Regex("""(0[1-9]|1[0-2])""").find(cleanedMonth)
        if (match != null) targetM = match.value
    }
    
    val yearMatch = Regex("""\b(20\d{2})\b""").find(cleanedMonth)
    if (yearMatch != null) targetY = yearMatch.value
    
    if (targetY.isNotEmpty() && !cleanedDate.contains(targetY)) return false
    
    if (targetM.isNotEmpty()) {
        val pattern1 = "/$targetM/"
        val pattern2 = "-$targetM-"
        val pattern3 = ".$targetM."
        if (cleanedDate.contains(pattern1) || cleanedDate.contains(pattern2) || cleanedDate.contains(pattern3) || cleanedDate.endsWith("/$targetM") || cleanedDate.endsWith("-$targetM")) {
            return true
        }
        for (k in monthMap.keys) {
            if (monthMap[k] == targetM && cleanedDate.contains(k)) {
                return true
            }
        }
    }
    
    return cleanedDate.contains(cleanedMonth) || cleanedMonth.contains(cleanedDate)
}

fun formatMonthToYyyyMm(monthStr: String): String {
    try {
        val inputFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US)
        val date = inputFormat.parse(monthStr)
        if (date != null) {
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            return outputFormat.format(date)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyPerformanceView(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit,
    showSettings: Boolean = false,
    onSettingsDismissed: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // DB states
    val staffList by viewModel.staffMembers.collectAsState()
    val partsRecords by viewModel.serviceWithPartsRecords.collectAsState()
    val noPartsRecords by viewModel.serviceWithoutPartsRecords.collectAsState()

    val isFetchingBackend by viewModel.isFetchingPerformance.collectAsState()
    val fetchedBackendCounts by viewModel.fetchedPerformanceCounts.collectAsState()
    val fetchedPerformanceCountsMap by viewModel.fetchedPerformanceCountsMap.collectAsState()
    val appsScriptPerformanceUrl by viewModel.performanceAppsScriptUrl.collectAsState()

    var useBackendData by remember { mutableStateOf(appsScriptPerformanceUrl.isNotBlank()) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showSettings) {
        if (showSettings) {
            showSettingsDialog = true
        }
    }
    var tempUrlText by remember(appsScriptPerformanceUrl) { mutableStateOf(appsScriptPerformanceUrl) }

    // Filter to only display staff members who are Mechanics
    val mechanicsOnly = remember(staffList) {
        staffList.filter { it.designation.equals("Mechanic", ignoreCase = true) }
    }

    var performanceStyleMode by remember { mutableStateOf("SINGLE") } // "SINGLE", "TEAM_WISE", "MATRIX"

    // Generate list of past 12 months for month selection (excluding future)
    val monthsList = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.US)
        for (i in 0 until 12) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.MONTH, -1)
        }
        list
    }

    var selectedMonth by remember { mutableStateOf(monthsList.first()) }
    var selectedMechanicName by remember { mutableStateOf("") }
    
    var showMonthDropdown by remember { mutableStateOf(false) }
    var showMechanicDropdown by remember { mutableStateOf(false) }
    var showTeamDropdown by remember { mutableStateOf(false) }

    // Toggle Team selected for "TEAM_WISE" table view
    var selectedTeamForPerformance by remember { mutableStateOf("Shift A") }

    // Seed selected mechanic default when staff loads
    LaunchedEffect(mechanicsOnly) {
        if (selectedMechanicName.isEmpty() && mechanicsOnly.isNotEmpty()) {
            selectedMechanicName = mechanicsOnly.first().name
        }
    }

    // --- METRICS CALCULATION ENGINE (SINGLE MECHANIC) ---
    val metrics = remember(selectedMonth, selectedMechanicName, partsRecords, noPartsRecords) {
        if (selectedMechanicName.isBlank()) {
            return@remember MapMetrics(0, 0, 0, 0, 0, 0)
        }

        // 1. ENGINE OIL
        val oilMatching = partsRecords.filter { r ->
            isSameMechanic(r.mechanic, selectedMechanicName) &&
            dateMatchesMonth(r.vchDate, selectedMonth) &&
            r.itemName.lowercase().contains("oil") &&
            r.quantity == 1.0
        }
        val engineOilCount = oilMatching.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

        // 2. ROUTINE MAINTENANCE
        val routineMatching = partsRecords.filter { r ->
            isSameMechanic(r.mechanic, selectedMechanicName) &&
            dateMatchesMonth(r.vchDate, selectedMonth) &&
            r.serviceType.lowercase().contains("routine")
        }
        val routineCount = routineMatching.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

        // 3. ENGINE
        val engineMatching = partsRecords.filter { r ->
            isSameMechanic(r.mechanic, selectedMechanicName) &&
            dateMatchesMonth(r.vchDate, selectedMonth) &&
            r.serviceType.lowercase().contains("engine")
        }
        val engineCount = engineMatching.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

        // 4. ACCIDENTS
        val accidentMatching = partsRecords.filter { r ->
            isSameMechanic(r.mechanic, selectedMechanicName) &&
            dateMatchesMonth(r.vchDate, selectedMonth) &&
            r.serviceType.lowercase().contains("accident")
        }
        val accidentCount = accidentMatching.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

        // 5. SERVICE WITHOUT PARTS
        val noPartsMatching = noPartsRecords.filter { r ->
            isSameMechanic(r.mechanic, selectedMechanicName) &&
            dateMatchesMonth(r.date, selectedMonth)
        }
        val serviceWithoutPartsCount = noPartsMatching.map { "${it.bikeNo}_${it.date}" }.distinct().size

        val total = engineOilCount + routineCount + engineCount + accidentCount + serviceWithoutPartsCount

        MapMetrics(
            engineOil = engineOilCount,
            routine = routineCount,
            engine = engineCount,
            accidents = accidentCount,
            withoutParts = serviceWithoutPartsCount,
            totalRepairs = total
        )
    }

    val currentMetrics = if (useBackendData) {
        fetchedBackendCounts ?: MapMetrics(0, 0, 0, 0, 0, 0)
    } else {
        metrics
    }

    LaunchedEffect(useBackendData, selectedMonth, selectedMechanicName, selectedTeamForPerformance, performanceStyleMode, mechanicsOnly) {
        if (useBackendData) {
            val monthYyyyMm = formatMonthToYyyyMm(selectedMonth)
            val mechanicList = when (performanceStyleMode) {
                "SINGLE" -> if (selectedMechanicName.isNotBlank()) listOf(selectedMechanicName) else emptyList()
                "TEAM_WISE" -> {
                    mechanicsOnly.filter { it.shift.equals(selectedTeamForPerformance, ignoreCase = true) }.map { it.name }
                }
                else -> mechanicsOnly.map { it.name } // "MATRIX"
            }
            if (mechanicList.isNotEmpty() && monthYyyyMm.isNotEmpty()) {
                viewModel.fetchMechanicPerformance(mechanicList = mechanicList, monthOrStartDate = monthYyyyMm)
            } else {
                viewModel.clearFetchedPerformanceCounts()
            }
        }
    }

    // --- ALL MECHANICS METRICS GENERATOR ---
    val allMechanicsMetrics = remember(useBackendData, fetchedPerformanceCountsMap, selectedMonth, mechanicsOnly, partsRecords, noPartsRecords) {
        mechanicsOnly.map { member ->
            val name = member.name

            val backendMetric = if (useBackendData) {
                fetchedPerformanceCountsMap[name] ?: fetchedPerformanceCountsMap.entries.firstOrNull { isSameMechanic(it.key, name) }?.value
            } else null

            val oilCount: Int
            val routineCount: Int
            val engineCount: Int
            val accidentCount: Int
            val withoutPartsCount: Int

            if (backendMetric != null) {
                oilCount = backendMetric.engineOil
                routineCount = backendMetric.routine
                engineCount = backendMetric.engine
                accidentCount = backendMetric.accidents
                withoutPartsCount = backendMetric.withoutParts
            } else {
                // Engine Oil
                oilCount = partsRecords.filter { r ->
                    isSameMechanic(r.mechanic, name) &&
                    dateMatchesMonth(r.vchDate, selectedMonth) &&
                    r.itemName.lowercase().contains("oil") &&
                    r.quantity == 1.0
                }.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

                // Routine
                routineCount = partsRecords.filter { r ->
                    isSameMechanic(r.mechanic, name) &&
                    dateMatchesMonth(r.vchDate, selectedMonth) &&
                    r.serviceType.lowercase().contains("routine")
                }.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

                // Engine
                engineCount = partsRecords.filter { r ->
                    isSameMechanic(r.mechanic, name) &&
                    dateMatchesMonth(r.vchDate, selectedMonth) &&
                    r.serviceType.lowercase().contains("engine")
                }.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

                // Accidents
                accidentCount = partsRecords.filter { r ->
                    isSameMechanic(r.mechanic, name) &&
                    dateMatchesMonth(r.vchDate, selectedMonth) &&
                    r.serviceType.lowercase().contains("accident")
                }.map { "${it.vchNo}_${it.bikeNo}_${it.vchDate}" }.distinct().size

                // Service Without Parts
                withoutPartsCount = noPartsRecords.filter { r ->
                    isSameMechanic(r.mechanic, name) &&
                    dateMatchesMonth(r.date, selectedMonth)
                }.map { "${it.bikeNo}_${it.date}" }.distinct().size
            }

            val total = oilCount + routineCount + engineCount + accidentCount + withoutPartsCount

            RowMetrics(
                name = name,
                shift = member.shift,
                designation = member.designation,
                engineOil = oilCount,
                routine = routineCount,
                engine = engineCount,
                accidents = accidentCount,
                withoutParts = withoutPartsCount,
                total = total
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SlateDark)) {
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = {
                    showSettingsDialog = false
                    onSettingsDismissed()
                },
                title = {
                    Text("Backend API Settings", fontWeight = FontWeight.Bold, color = TextPrimary)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Configure the Apps Script Web App URL to retrieve real-time mechanic performance counts.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = tempUrlText,
                            onValueChange = { tempUrlText = it },
                            label = { Text("Apps Script Web App URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotorOrange,
                                focusedLabelColor = MotorOrange,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceDarkElevated,
                                unfocusedContainerColor = SurfaceDarkElevated
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "If empty, it falls back to the standard Google Sheet Web App URL.\n\nQuery parameters (mode=performance_count, mechanic, date) are formatted automatically.",
                            fontSize = 10.sp,
                            color = TextDisabled
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updatePerformanceAppsScriptUrl(tempUrlText)
                            showSettingsDialog = false
                            android.widget.Toast.makeText(context, "URL Updated successfully", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange)
                    ) {
                        Text("SAVE", color = SlateDark, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSettingsDialog = false }
                    ) {
                        Text("CANCEL", color = TextSecondary)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display Style Mode & Criteria Controls
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Top Row: Display Mode Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "SINGLE" to "1 Mechanic",
                                "TEAM_WISE" to "Team-wise",
                                "MATRIX" to "All Mechanics"
                            ).forEach { (mode, label) ->
                                val isSelected = performanceStyleMode == mode
                                Button(
                                    onClick = { performanceStyleMode = mode },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) MotorOrange else SurfaceDarkElevated
                                    ),
                                    border = BorderStroke(1.dp, if (isSelected) MotorOrange else BorderColor),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(36.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SlateDark else TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Row: Month / Mechanic / Team Criteria Selectors + Refresh Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Month Selector Dropdown
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    onClick = { showMonthDropdown = !showMonthDropdown },
                                    colors = CardDefaults.outlinedCardColors(containerColor = SurfaceDarkElevated),
                                    border = BorderStroke(1.dp, BorderColor),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Month", fontSize = 9.sp, color = TextSecondary)
                                            Text(selectedMonth, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = showMonthDropdown,
                                    onDismissRequest = { showMonthDropdown = false },
                                    modifier = Modifier.background(SurfaceDarkElevated)
                                ) {
                                    monthsList.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m, color = TextPrimary) },
                                            onClick = {
                                                selectedMonth = m
                                                showMonthDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (performanceStyleMode == "SINGLE") {
                                // Mechanic Selector Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedCard(
                                        onClick = { showMechanicDropdown = !showMechanicDropdown },
                                        colors = CardDefaults.outlinedCardColors(containerColor = SurfaceDarkElevated),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Mechanic", fontSize = 9.sp, color = TextSecondary)
                                                Text(
                                                    text = selectedMechanicName.ifEmpty { "None Selected" },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showMechanicDropdown,
                                        onDismissRequest = { showMechanicDropdown = false },
                                        modifier = Modifier.background(SurfaceDarkElevated)
                                    ) {
                                        if (mechanicsOnly.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("No mechanics created", color = TextDisabled) },
                                                onClick = { showMechanicDropdown = false }
                                            )
                                        } else {
                                            mechanicsOnly.forEach { staff ->
                                                DropdownMenuItem(
                                                    text = { 
                                                        Text("${staff.name} (${staff.shift})", color = TextPrimary) 
                                                    },
                                                    onClick = {
                                                        selectedMechanicName = staff.name
                                                        showMechanicDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else if (performanceStyleMode == "TEAM_WISE") {
                                // Team Selector Dropdown
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedCard(
                                        onClick = { showTeamDropdown = !showTeamDropdown },
                                        colors = CardDefaults.outlinedCardColors(containerColor = SurfaceDarkElevated),
                                        border = BorderStroke(1.dp, BorderColor),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Select Team", fontSize = 9.sp, color = TextSecondary)
                                                Text(
                                                    text = if (selectedTeamForPerformance == "Shift A") "Team A (Shift A)" else "Team B (Shift B)",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MotorOrange,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showTeamDropdown,
                                        onDismissRequest = { showTeamDropdown = false },
                                        modifier = Modifier.background(SurfaceDarkElevated)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Team A (Shift A)", color = TextPrimary, fontWeight = if (selectedTeamForPerformance == "Shift A") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedTeamForPerformance = "Shift A"
                                                showTeamDropdown = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Team B (Shift B)", color = TextPrimary, fontWeight = if (selectedTeamForPerformance == "Shift B") FontWeight.Bold else FontWeight.Normal) },
                                            onClick = {
                                                selectedTeamForPerformance = "Shift B"
                                                showTeamDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Refresh Live Data Button
                            OutlinedIconButton(
                                onClick = {
                                    val monthYyyyMm = formatMonthToYyyyMm(selectedMonth)
                                    val mechanicList = when (performanceStyleMode) {
                                        "SINGLE" -> if (selectedMechanicName.isNotBlank()) listOf(selectedMechanicName) else emptyList()
                                        "TEAM_WISE" -> {
                                            mechanicsOnly.filter { it.shift.equals(selectedTeamForPerformance, ignoreCase = true) }.map { it.name }
                                        }
                                        else -> mechanicsOnly.map { it.name }
                                    }
                                    if (mechanicList.isNotEmpty() && monthYyyyMm.isNotEmpty()) {
                                        viewModel.fetchMechanicPerformance(mechanicList = mechanicList, monthOrStartDate = monthYyyyMm)
                                    }
                                },
                                colors = IconButtonDefaults.outlinedIconButtonColors(containerColor = SurfaceDarkElevated),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                if (isFetchingBackend) {
                                    CircularProgressIndicator(
                                        color = MotorOrange,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh live data",
                                        tint = MotorOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Style-dependent Main Area
            when (performanceStyleMode) {
                "SINGLE" -> {
                    if (selectedMechanicName.isBlank()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Please add/select a mechanic to start checking performance.", textAlign = TextAlign.Center, fontSize = 13.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    } else {
                        // Stats Summary Card
                        item {
                            val mechanicShift = mechanicsOnly.find { it.name == selectedMechanicName }?.shift ?: "Shift A"
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                border = BorderStroke(1.dp, BorderColor),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = selectedMechanicName.uppercase(),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "$mechanicShift • Selected Month: $selectedMonth",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MotorOrange
                                            )
                                        }
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SmartExportButton(
                                                onExportPdf = {
                                                    exportSingleMechanicPdf(context, selectedMonth, selectedMechanicName, mechanicShift, currentMetrics)
                                                },
                                                onExportExcel = {
                                                    exportSingleMechanicExcel(context, selectedMonth, selectedMechanicName, mechanicShift, currentMetrics)
                                                }
                                            )

                                            // Big Round badge for total repairs
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(MotorOrange.copy(alpha = 0.15f))
                                                    .border(2.dp, MotorOrange, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = currentMetrics.totalRepairs.toString(),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MotorOrange
                                                    )
                                                    Text(
                                                        text = "TOTAL",
                                                        fontSize = 7.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = BorderColor)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Category wise score cards
                                    Text(
                                        text = "DUPLICATE-CONTROLLED BREAKDOWNS",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Category Row Items
                                    CategoryBreakdownRow(
                                        title = "ENGINE OIL -",
                                        subtitle = "Service with parts • Item is Engine Oil (1L)",
                                        value = currentMetrics.engineOil,
                                        color = ColorOilChange
                                    )
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                                    CategoryBreakdownRow(
                                        title = "ROUTINE",
                                        subtitle = "Service with parts • Type contains 'routine'",
                                        value = currentMetrics.routine,
                                        color = MotorOrange
                                    )
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                                    CategoryBreakdownRow(
                                        title = "ENGINE",
                                        subtitle = "Service with parts • Type contains 'engine'",
                                        value = currentMetrics.engine,
                                        color = Color.Red
                                    )
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                                    CategoryBreakdownRow(
                                        title = "ACCIDENTS",
                                        subtitle = "Service with parts • Type contains 'accident'",
                                        value = currentMetrics.accidents,
                                        color = Color.Magenta
                                    )
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                                    CategoryBreakdownRow(
                                        title = "SERVICE WIHOUT PARTS",
                                        subtitle = "Service without parts table • Unique Bike repairs",
                                        value = currentMetrics.withoutParts,
                                        color = ColorServiceWithoutParts
                                    )
                                }
                            }
                        }
                        item {
                            ServiceBreakdownBarChart(
                                metrics = currentMetrics,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }
                    }
                }

                "TEAM_WISE" -> {
                    item {
                        val teamMembers = if (selectedTeamForPerformance.equals("Shift B", ignoreCase = true)) {
                            allMechanicsMetrics.filter { it.shift.equals("Shift B", ignoreCase = true) }
                        } else {
                            allMechanicsMetrics.filter { !it.shift.equals("Shift B", ignoreCase = true) }
                        }

                        MechanicsPerformanceTable(
                            title = "${selectedTeamForPerformance.uppercase()} PERFORMANCE MATRIX",
                            selectedMonth = selectedMonth,
                            metricsList = teamMembers,
                            onExportPdf = {
                                exportTeamPerformancePdf(context, selectedMonth, selectedTeamForPerformance, teamMembers)
                            },
                            onExportExcel = {
                                exportTeamPerformanceExcel(context, selectedMonth, selectedTeamForPerformance, teamMembers)
                            }
                        )
                    }
                }

                "MATRIX" -> {
                    item {
                        MechanicsPerformanceTable(
                            title = "ALL MECHANICS PERFORMANCE MATRIX",
                            selectedMonth = selectedMonth,
                            metricsList = allMechanicsMetrics,
                            onExportPdf = {
                                exportPerformancePdf(context, selectedMonth, allMechanicsMetrics)
                            },
                            onExportExcel = {
                                exportPerformanceExcel(context, selectedMonth, allMechanicsMetrics)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SmartExportButton(
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = SurfaceDarkElevated,
                contentColor = TextPrimary
            ),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Export",
                tint = MotorOrange,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "EXPORT",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(14.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SurfaceDarkElevated)
        ) {
            DropdownMenuItem(
                text = {
                    Text("📄 Export PDF", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                },
                onClick = {
                    expanded = false
                    onExportPdf()
                }
            )
            DropdownMenuItem(
                text = {
                    Text("📊 Export Excel (CSV)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                },
                onClick = {
                    expanded = false
                    onExportExcel()
                }
            )
        }
    }
}

@Composable
fun CategoryBreakdownRow(
    title: String,
    subtitle: String,
    value: Int,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(subtitle, fontSize = 9.sp, color = TextSecondary)
        }
        Text(
            text = "$value Repairs",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
    }
}

@Composable
fun MechanicsPerformanceTable(
    title: String,
    selectedMonth: String,
    metricsList: List<RowMetrics>,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    val totalOil = metricsList.sumOf { it.engineOil }
    val totalRoutine = metricsList.sumOf { it.routine }
    val totalEngine = metricsList.sumOf { it.engine }
    val totalAccidents = metricsList.sumOf { it.accidents }
    val totalWithoutParts = metricsList.sumOf { it.withoutParts }
    val grandTotal = metricsList.sumOf { it.total }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with Title and Export Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MotorOrange,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )

                SmartExportButton(
                    onExportPdf = onExportPdf,
                    onExportExcel = onExportExcel
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable Table
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                Column {
                    // Column Headings
                    Row(
                        modifier = Modifier
                            .background(SurfaceDarkElevated)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Mechanic Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(180.dp).padding(horizontal = 6.dp))
                        Text("Designation", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(100.dp).padding(horizontal = 6.dp))
                        Text("ENGINE OIL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(90.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("ROUTINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(85.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(80.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("ACCIDENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(90.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("WITHOUT PARTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(130.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("Total", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MotorOrange, modifier = Modifier.width(80.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = BorderColor)

                    // Totals Row (Highlighted Amber Yellow matching reference image)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7))
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Monthly Performance Total", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(180.dp).padding(horizontal = 6.dp))
                        Text(formatMonthToShortCode(selectedMonth), fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(100.dp).padding(horizontal = 6.dp))
                        Text("$totalOil", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(90.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("$totalRoutine", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(85.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("$totalEngine", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(80.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("$totalAccidents", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(90.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("$totalWithoutParts", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(130.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                        Text("$grandTotal", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.width(80.dp).padding(horizontal = 6.dp), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(color = Color(0xFFF59E0B), thickness = 1.5.dp)

                    // Mechanic Rows
                    if (metricsList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp).width(835.dp), contentAlignment = Alignment.Center) {
                            Text("No mechanics found for selected filter.", fontSize = 12.sp, color = TextDisabled)
                        }
                    } else {
                        metricsList.forEachIndexed { idx, row ->
                            Row(
                                modifier = Modifier
                                    .background(if (idx % 2 == 0) SlateDark else SurfaceDark)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = row.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.width(180.dp).padding(horizontal = 6.dp)
                                )
                                Text(
                                    text = row.designation.ifBlank { "Servicing" },
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.width(100.dp).padding(horizontal = 6.dp)
                                )

                                PerformanceCell(count = row.engineOil, modifier = Modifier.width(90.dp).padding(horizontal = 6.dp))
                                PerformanceCell(count = row.routine, modifier = Modifier.width(85.dp).padding(horizontal = 6.dp))
                                PerformanceCell(count = row.engine, modifier = Modifier.width(80.dp).padding(horizontal = 6.dp))
                                PerformanceCell(count = row.accidents, modifier = Modifier.width(90.dp).padding(horizontal = 6.dp))
                                PerformanceCell(count = row.withoutParts, modifier = Modifier.width(130.dp).padding(horizontal = 6.dp))

                                Box(
                                    modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        "${row.total}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MotorOrange,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MotorOrange.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceCell(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterEnd
    ) {
        val bgColor = if (count > 0) Color(0xFF065F46).copy(alpha = 0.25f) else Color(0xFF991B1B).copy(alpha = 0.15f)
        val textColor = if (count > 0) Color(0xFF34D399) else Color(0xFFFCA5A5)
        
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(bgColor)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun formatMonthToShortCode(monthStr: String): String {
    try {
        val sdfIn = SimpleDateFormat("MMMM yyyy", Locale.US)
        val date = sdfIn.parse(monthStr)
        if (date != null) {
            val sdfOut = SimpleDateFormat("MMM-yyyy", Locale.US)
            return sdfOut.format(date)
        }
    } catch (e: Exception) {
        // Fallback
    }
    return monthStr
}

// Data models
data class MapMetrics(
    val engineOil: Int,
    val routine: Int,
    val engine: Int,
    val accidents: Int,
    val withoutParts: Int,
    val totalRepairs: Int
)

data class RowMetrics(
    val name: String,
    val shift: String,
    val designation: String = "Servicing",
    val engineOil: Int,
    val routine: Int,
    val engine: Int,
    val accidents: Int,
    val withoutParts: Int,
    val total: Int
)

// PDF Export for All Mechanics
fun exportPerformancePdf(
    context: android.content.Context,
    selectedMonth: String,
    metricsList: List<RowMetrics>
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = android.graphics.Paint()
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    
    paint.color = android.graphics.Color.rgb(255, 102, 0) // MotorOrange
    canvas.drawRect(30f, 30f, 565f, 75f, paint)
    
    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = 16f
    textPaint.isFakeBoldText = true
    canvas.drawText("ADVANCE AUTO - MONTHLY PERFORMANCE REPORT", 45f, 58f, textPaint)
    
    textPaint.color = android.graphics.Color.BLACK
    textPaint.textSize = 11f
    textPaint.isFakeBoldText = false
    canvas.drawText("Report Month: $selectedMonth", 45f, 100f, textPaint)
    canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 45f, 116f, textPaint)
    
    var y = 150f
    
    // Draw Table Header
    paint.color = android.graphics.Color.rgb(240, 240, 240)
    canvas.drawRect(30f, y, 565f, y + 25f, paint)
    
    textPaint.isFakeBoldText = true
    canvas.drawText("Technician", 35f, y + 17f, textPaint)
    canvas.drawText("Engine Oil", 170f, y + 17f, textPaint)
    canvas.drawText("Routine", 240f, y + 17f, textPaint)
    canvas.drawText("Engine Rep.", 310f, y + 17f, textPaint)
    canvas.drawText("Accidents", 390f, y + 17f, textPaint)
    canvas.drawText("No Parts", 460f, y + 17f, textPaint)
    canvas.drawText("Total", 520f, y + 17f, textPaint)
    
    y += 25f
    textPaint.isFakeBoldText = false
    
    metricsList.forEach { row ->
        if (y > 780f) return@forEach
        
        val name = row.name
        val oil = row.engineOil
        val routine = row.routine
        val engine = row.engine
        val accident = row.accidents
        val withoutParts = row.withoutParts
        val total = row.total
        
        paint.color = android.graphics.Color.rgb(252, 252, 252)
        canvas.drawRect(30f, y, 565f, y + 25f, paint)
        
        canvas.drawText(name, 35f, y + 17f, textPaint)
        canvas.drawText(oil.toString(), 170f, y + 17f, textPaint)
        canvas.drawText(routine.toString(), 240f, y + 17f, textPaint)
        canvas.drawText(engine.toString(), 310f, y + 17f, textPaint)
        canvas.drawText(accident.toString(), 390f, y + 17f, textPaint)
        canvas.drawText(withoutParts.toString(), 460f, y + 17f, textPaint)
        
        textPaint.isFakeBoldText = true
        canvas.drawText(total.toString(), 520f, y + 17f, textPaint)
        textPaint.isFakeBoldText = false
        
        y += 25f
        paint.color = android.graphics.Color.rgb(220, 220, 220)
        canvas.drawLine(30f, y, 565f, y, paint)
    }
    
    pdfDocument.finishPage(page)
    
    val fileName = "Advance_Auto_Performance_${selectedMonth.replace(" ", "_")}.pdf"
    saveFileCompat(context, fileName, "application/pdf") { out ->
        pdfDocument.writeTo(out)
    }
    Toast.makeText(context, "Performance report PDF saved to Downloads successfully!", Toast.LENGTH_LONG).show()
    pdfDocument.close()
}

// Excel Export for All Mechanics
fun exportPerformanceExcel(
    context: android.content.Context,
    selectedMonth: String,
    metricsList: List<RowMetrics>
) {
    val csvBuilder = java.lang.StringBuilder()
    csvBuilder.append("Technician Name,Shift,Engine Oil (1L),Routine Maintenance,Engine Repair,Accident Service,Service Without Parts,Total Repairs\n")
    
    metricsList.forEach { row ->
        val name = row.name
        val oil = row.engineOil
        val routine = row.routine
        val engine = row.engine
        val accident = row.accidents
        val withoutParts = row.withoutParts
        val total = row.total
        val escapedName = if (name.contains(",")) "\"$name\"" else name
        csvBuilder.append("$escapedName,${row.shift},$oil,$routine,$engine,$accident,$withoutParts,$total\n")
    }

    val fileName = "Advance_Auto_Performance_${selectedMonth.replace(" ", "_")}.csv"
    saveFileCompat(context, fileName, "text/csv") { out ->
        out.write(csvBuilder.toString().toByteArray())
    }
    Toast.makeText(context, "Performance report CSV saved to Downloads successfully!", Toast.LENGTH_LONG).show()
}

// PDF Export for Team Performance
fun exportTeamPerformancePdf(
    context: android.content.Context,
    selectedMonth: String,
    selectedTeam: String,
    metricsList: List<RowMetrics>
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = android.graphics.Paint()
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    
    paint.color = android.graphics.Color.rgb(255, 102, 0) // MotorOrange
    canvas.drawRect(30f, 30f, 565f, 75f, paint)
    
    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = 16f
    textPaint.isFakeBoldText = true
    canvas.drawText("ADVANCE AUTO - ${selectedTeam.uppercase()} PERFORMANCE", 45f, 58f, textPaint)
    
    textPaint.color = android.graphics.Color.BLACK
    textPaint.textSize = 11f
    textPaint.isFakeBoldText = false
    canvas.drawText("Report Month: $selectedMonth", 45f, 100f, textPaint)
    canvas.drawText("Team Shift: $selectedTeam", 45f, 116f, textPaint)
    canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 45f, 132f, textPaint)
    
    var y = 160f
    
    paint.color = android.graphics.Color.rgb(240, 240, 240)
    canvas.drawRect(30f, y, 565f, y + 25f, paint)
    
    textPaint.isFakeBoldText = true
    canvas.drawText("Technician", 35f, y + 17f, textPaint)
    canvas.drawText("Engine Oil", 170f, y + 17f, textPaint)
    canvas.drawText("Routine", 240f, y + 17f, textPaint)
    canvas.drawText("Engine Rep.", 310f, y + 17f, textPaint)
    canvas.drawText("Accidents", 390f, y + 17f, textPaint)
    canvas.drawText("No Parts", 460f, y + 17f, textPaint)
    canvas.drawText("Total", 520f, y + 17f, textPaint)
    
    y += 25f
    textPaint.isFakeBoldText = false
    
    metricsList.forEach { row ->
        if (y > 780f) return@forEach
        
        val name = row.name
        val oil = row.engineOil
        val routine = row.routine
        val engine = row.engine
        val accident = row.accidents
        val withoutParts = row.withoutParts
        val total = row.total
        
        paint.color = android.graphics.Color.rgb(252, 252, 252)
        canvas.drawRect(30f, y, 565f, y + 25f, paint)
        
        canvas.drawText(name, 35f, y + 17f, textPaint)
        canvas.drawText(oil.toString(), 170f, y + 17f, textPaint)
        canvas.drawText(routine.toString(), 240f, y + 17f, textPaint)
        canvas.drawText(engine.toString(), 310f, y + 17f, textPaint)
        canvas.drawText(accident.toString(), 390f, y + 17f, textPaint)
        canvas.drawText(withoutParts.toString(), 460f, y + 17f, textPaint)
        
        textPaint.isFakeBoldText = true
        canvas.drawText(total.toString(), 520f, y + 17f, textPaint)
        textPaint.isFakeBoldText = false
        
        y += 25f
        paint.color = android.graphics.Color.rgb(220, 220, 220)
        canvas.drawLine(30f, y, 565f, y, paint)
    }
    
    pdfDocument.finishPage(page)
    
    val fileName = "Advance_Auto_Performance_${selectedTeam.replace(" ", "_")}_${selectedMonth.replace(" ", "_")}.pdf"
    saveFileCompat(context, fileName, "application/pdf") { out ->
        pdfDocument.writeTo(out)
    }
    Toast.makeText(context, "$selectedTeam report PDF saved successfully!", Toast.LENGTH_LONG).show()
    pdfDocument.close()
}

// Excel Export for Team Performance
fun exportTeamPerformanceExcel(
    context: android.content.Context,
    selectedMonth: String,
    selectedTeam: String,
    metricsList: List<RowMetrics>
) {
    val csvBuilder = java.lang.StringBuilder()
    csvBuilder.append("Advance Auto Team Performance Report\n")
    csvBuilder.append("Team Shift:,$selectedTeam\n")
    csvBuilder.append("Month:,$selectedMonth\n\n")
    csvBuilder.append("Technician Name,Engine Oil (1L),Routine Maintenance,Engine Repair,Accident Service,Service Without Parts,Total Repairs\n")
    
    metricsList.forEach { row ->
        val name = row.name
        val oil = row.engineOil
        val routine = row.routine
        val engine = row.engine
        val accident = row.accidents
        val withoutParts = row.withoutParts
        val total = row.total
        val escapedName = if (name.contains(",")) "\"$name\"" else name
        csvBuilder.append("$escapedName,$oil,$routine,$engine,$accident,$withoutParts,$total\n")
    }
    
    val fileName = "Advance_Auto_Performance_${selectedTeam.replace(" ", "_")}_${selectedMonth.replace(" ", "_")}.csv"
    saveFileCompat(context, fileName, "text/csv") { out ->
        out.write(csvBuilder.toString().toByteArray())
    }
    Toast.makeText(context, "$selectedTeam CSV report saved successfully!", Toast.LENGTH_LONG).show()
}

// PDF Export for Single Mechanic
fun exportSingleMechanicPdf(
    context: android.content.Context,
    selectedMonth: String,
    name: String,
    shift: String,
    metrics: MapMetrics
) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas
    
    val paint = android.graphics.Paint()
    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        isAntiAlias = true
    }
    
    paint.color = android.graphics.Color.rgb(255, 102, 0) // MotorOrange
    canvas.drawRect(30f, 30f, 565f, 75f, paint)
    
    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = 16f
    textPaint.isFakeBoldText = true
    canvas.drawText("ADVANCE AUTO - INDIVIDUAL PERFORMANCE REPORT", 45f, 58f, textPaint)
    
    textPaint.color = android.graphics.Color.BLACK
    textPaint.textSize = 11f
    textPaint.isFakeBoldText = false
    canvas.drawText("Technician Name: $name", 45f, 100f, textPaint)
    canvas.drawText("Assigned Shift: $shift", 45f, 116f, textPaint)
    canvas.drawText("Report Month: $selectedMonth", 45f, 132f, textPaint)
    canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 45f, 148f, textPaint)
    
    var y = 190f
    
    paint.color = android.graphics.Color.rgb(240, 240, 240)
    canvas.drawRect(30f, y, 565f, y + 25f, paint)
    
    textPaint.isFakeBoldText = true
    canvas.drawText("Metric / Service Category", 45f, y + 17f, textPaint)
    canvas.drawText("Count", 450f, y + 17f, textPaint)
    
    y += 25f
    textPaint.isFakeBoldText = false
    
    val rows = listOf(
        "Engine Oil (1L)" to metrics.engineOil,
        "Routine Maintenance" to metrics.routine,
        "Engine Repair" to metrics.engine,
        "Accident Service" to metrics.accidents,
        "Service Without Parts" to metrics.withoutParts,
        "TOTAL COMPLETED REPAIRS" to metrics.totalRepairs
    )
    
    rows.forEach { (category, count) ->
        paint.color = android.graphics.Color.rgb(252, 252, 252)
        canvas.drawRect(30f, y, 565f, y + 25f, paint)
        
        if (category.startsWith("TOTAL")) {
            textPaint.isFakeBoldText = true
        }
        canvas.drawText(category, 45f, y + 17f, textPaint)
        canvas.drawText(count.toString(), 450f, y + 17f, textPaint)
        textPaint.isFakeBoldText = false
        
        y += 25f
        paint.color = android.graphics.Color.rgb(220, 220, 220)
        canvas.drawLine(30f, y, 565f, y, paint)
    }
    
    pdfDocument.finishPage(page)
    
    val fileName = "Advance_Auto_Performance_${name.replace(" ", "_")}_${selectedMonth.replace(" ", "_")}.pdf"
    saveFileCompat(context, fileName, "application/pdf") { out ->
        pdfDocument.writeTo(out)
    }
    Toast.makeText(context, "Individual PDF report saved successfully!", Toast.LENGTH_LONG).show()
    pdfDocument.close()
}

// Excel Export for Single Mechanic
fun exportSingleMechanicExcel(
    context: android.content.Context,
    selectedMonth: String,
    name: String,
    shift: String,
    metrics: MapMetrics
) {
    val csvBuilder = java.lang.StringBuilder()
    csvBuilder.append("Advance Auto Individual Performance Report\n")
    csvBuilder.append("Technician:,$name\n")
    csvBuilder.append("Shift:,$shift\n")
    csvBuilder.append("Month:,$selectedMonth\n\n")
    csvBuilder.append("Category,Count\n")
    csvBuilder.append("Engine Oil (1L),${metrics.engineOil}\n")
    csvBuilder.append("Routine Maintenance,${metrics.routine}\n")
    csvBuilder.append("Engine Repair,${metrics.engine}\n")
    csvBuilder.append("Accident Service,${metrics.accidents}\n")
    csvBuilder.append("Service Without Parts,${metrics.withoutParts}\n")
    csvBuilder.append("TOTAL COMPLETED REPAIRS,${metrics.totalRepairs}\n")
    
    val fileName = "Advance_Auto_Performance_${name.replace(" ", "_")}_${selectedMonth.replace(" ", "_")}.csv"
    saveFileCompat(context, fileName, "text/csv") { out ->
        out.write(csvBuilder.toString().toByteArray())
    }
    Toast.makeText(context, "Individual CSV report saved successfully!", Toast.LENGTH_LONG).show()
}

private fun saveFileCompat(
    context: android.content.Context,
    fileName: String,
    mimeType: String,
    writeBlock: (java.io.OutputStream) -> Unit
) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    writeBlock(out)
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    } else {
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { out ->
                writeBlock(out)
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ServiceBreakdownBarChart(
    metrics: MapMetrics,
    modifier: Modifier = Modifier
) {
    val oil = metrics.engineOil
    val routine = metrics.routine
    val engine = metrics.engine
    val accidents = metrics.accidents
    val withoutParts = metrics.withoutParts

    val routineColor = MotorOrange
    val oilColor = ColorOilChange
    val engineColor = Color.Red
    val accidentsColor = Color.Magenta
    val withoutPartsColor = ColorServiceWithoutParts

    val items = remember(oil, routine, engine, accidents, withoutParts, routineColor, oilColor, engineColor, accidentsColor, withoutPartsColor) {
        listOf(
            ChartBarItem("Oil Change", oil, oilColor),
            ChartBarItem("Routine", routine, routineColor),
            ChartBarItem("Engine", engine, engineColor),
            ChartBarItem("Accidents", accidents, accidentsColor),
            ChartBarItem("Labor Only", withoutParts, withoutPartsColor)
        )
    }

    val maxValue = remember(items) {
        items.maxOf { it.value }.coerceAtLeast(1)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("service_breakdown_chart_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "SERVICE TYPES VISUAL BREAKDOWN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MotorOrange,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (metrics.totalRepairs == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextDisabled,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No performance data available to display.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                var selectedIndex by remember { mutableStateOf(-1) }

                val animValues = remember(oil, routine, engine, accidents, withoutParts) {
                    List(5) { Animatable(0f) }
                }

                LaunchedEffect(oil, routine, engine, accidents, withoutParts) {
                    animValues.forEachIndexed { index, animatable ->
                        launch {
                            animatable.snapTo(0f)
                            delay(index * 75L)
                            animatable.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Background Grid Lines
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        repeat(4) { idx ->
                            val gridValue = (maxValue * (4 - idx) / 4)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = gridValue.toString(),
                                    fontSize = 9.sp,
                                    color = TextDisabled,
                                    modifier = Modifier.width(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                HorizontalDivider(
                                    color = BorderColor.copy(alpha = 0.15f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "0",
                                fontSize = 9.sp,
                                color = TextDisabled,
                                modifier = Modifier.width(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            HorizontalDivider(
                                color = BorderColor.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // The Bars Row
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 28.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        items.forEachIndexed { index, item ->
                            val heightProgress = remember(item.value, maxValue) {
                                if (maxValue > 0) item.value.toFloat() / maxValue else 0f
                            }
                            val animatedHeightProgress by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = heightProgress,
                                animationSpec = androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                )
                            )

                            val isSelected = selectedIndex == index
                            val animValue = animValues.getOrElse(index) { Animatable(1f) }.value

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chart_column_${item.name.lowercase().replace(" ", "_")}")
                                    .graphicsLayer(
                                        scaleX = 0.8f + (0.2f * animValue),
                                        scaleY = animValue,
                                        alpha = animValue,
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                    )
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectedIndex = if (isSelected) -1 else index
                                    }
                            ) {
                                Text(
                                    text = item.value.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MotorOrange else TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .fillMaxHeight(0.85f * animatedHeightProgress)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (selectedIndex == -1 || isSelected) {
                                                item.color
                                            } else {
                                                item.color.copy(alpha = 0.3f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) TextPrimary else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = item.shortName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MotorOrange else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = selectedIndex != -1,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    if (selectedIndex in items.indices) {
                        val selItem = items[selectedIndex]
                        val pct = if (metrics.totalRepairs > 0) {
                            (selItem.value.toFloat() / metrics.totalRepairs * 100).toInt()
                        } else 0
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDarkElevated)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
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
                                            .background(selItem.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selItem.name.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "${selItem.value} Repairs (${pct}%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = selItem.color
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendItem(name = "Oil Change", color = ColorOilChange)
                        LegendItem(name = "Routine", color = MotorOrange)
                        LegendItem(name = "Engine", color = Color.Red)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                    ) {
                        LegendItem(name = "Accidents", color = Color.Magenta)
                        LegendItem(name = "Labor Only", color = ColorServiceWithoutParts)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
    }
}

data class ChartBarItem(
    val name: String,
    val value: Int,
    val color: Color
) {
    val shortName: String
        get() = when (name) {
            "Oil Change" -> "OIL"
            "Routine" -> "ROUT"
            "Engine" -> "ENG"
            "Accidents" -> "ACC"
            "Labor Only" -> "LAB"
            else -> name
        }
}

