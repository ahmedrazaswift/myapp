package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.example.ui.components.bounceOnClick
import com.example.ui.components.getStandardScreenTransitionSpec
import com.example.ui.components.AnimatedDialogContainer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ServiceQueueItem
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageServiceQueueView(
    viewModel: MotorcycleViewModel,
    selectedMode: String = "ADMIN",
    onModeChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val serviceQueue by viewModel.serviceQueue.collectAsState()

    // Start listening to real-time Firebase / Local queue updates when screen opens
    LaunchedEffect(Unit) {
        viewModel.listenToServiceQueue()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SlateDark)
            .padding(if (selectedMode == "TV_DISPLAY") 12.dp else 16.dp)
    ) {
        // Mode Selector Tabs - ONLY SHOWN IN ADMIN MODE
        if (selectedMode == "ADMIN") {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        Triple("ADMIN", "🛠️ Admin View", Icons.Default.Edit),
                        Triple("TV_DISPLAY", "📺 TV / Rider Screen", Icons.Default.Info)
                    )

                    modes.forEach { (modeKey, label, icon) ->
                        val isSelected = selectedMode == modeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MotorOrange.copy(alpha = 0.2f) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MotorOrange else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onModeChange(modeKey) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MotorOrange else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = { getStandardScreenTransitionSpec() },
            label = "QueueModeTransition"
        ) { mode ->
            when (mode) {
                "ADMIN" -> QueueAdminView(viewModel = viewModel, queueList = serviceQueue, context = context)
                else -> QueueTvDisplayView(
                    queueList = serviceQueue,
                    context = context,
                    onExitTvMode = { onModeChange("ADMIN") }
                )
            }
        }
    }
}

private fun playNewBikeAddedSound(context: Context) {
    try {
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
        if (ringtone != null) {
            ringtone.play()
        } else {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    } catch (e: Exception) {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (_: Exception) {}
    }
}

private data class DailyHistorySummary(
    val dateDisplay: String,
    val dateKey: String,
    val count: Int,
    val avgDurationMins: Long,
    val items: List<ServiceQueueItem>,
    val isToday: Boolean
)

// ---------------------------------------------------------------------------
// 1. ADMIN / GARAGE VIEW (EDITABLE VIEW WITH ACTIVE QUEUE & ARCHIVE)
// ---------------------------------------------------------------------------
@Composable
private fun QueueAdminView(
    viewModel: MotorcycleViewModel,
    queueList: List<ServiceQueueItem>,
    context: Context
) {
    var adminTab by remember { mutableStateOf("ACTIVE_QUEUE") } // "ACTIVE_QUEUE", "HISTORY_30_DAYS"
    var bikeNumberInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var archiveSearchQuery by remember { mutableStateOf("") }
    var showClearArchiveDialog by remember { mutableStateOf(false) }
    var expandedDateKeys by remember { mutableStateOf(setOf<String>()) }

    // Trigger subtle notification sound effect whenever a new bike is added to the queue
    var knownBikeIds by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(queueList) {
        val currentIds = queueList.map { it.id }.toSet()
        if (knownBikeIds != null) {
            val hasNewBike = currentIds.any { id -> id !in knownBikeIds!! }
            if (hasNewBike) {
                playNewBikeAddedSound(context)
            }
        }
        knownBikeIds = currentIds
    }

    val archiveList by viewModel.serviceArchive.collectAsState()

    val queuedBikes = remember(queueList) { queueList.filter { it.status == "QUEUED" } }
    val readyBikes = remember(queueList) { queueList.filter { it.status == "READY" } }

    val qatarZone = remember { TimeZone.getTimeZone("Asia/Qatar") }
    val timeFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).apply { timeZone = qatarZone } }

    val todayStartMillis = remember {
        val cal = Calendar.getInstance(qatarZone)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val todayCompletedList = remember(archiveList) {
        archiveList.filter { (it.completionTimeMillis ?: it.entryTimeMillis) >= todayStartMillis }
    }

    val durationsMins = remember(archiveList) {
        archiveList.mapNotNull { item ->
            val end = item.completionTimeMillis ?: item.readyTimeMillis
            if (end != null && end >= item.entryTimeMillis) {
                (end - item.entryTimeMillis) / (1000 * 60)
            } else null
        }
    }

    val avgDurationMins = remember(durationsMins) {
        if (durationsMins.isNotEmpty()) durationsMins.average().toLong() else 0L
    }
    val minDurationMins = remember(durationsMins) {
        if (durationsMins.isNotEmpty()) durationsMins.minOrNull() ?: 0L else 0L
    }
    val maxDurationMins = remember(durationsMins) {
        if (durationsMins.isNotEmpty()) durationsMins.maxOrNull() ?: 0L else 0L
    }

    fun formatDuration(mins: Long): String {
        return if (mins < 60) {
            "$mins mins"
        } else {
            val hrs = mins / 60
            val rem = mins % 60
            if (rem == 0L) "${hrs}h" else "${hrs}h ${rem}m"
        }
    }

    val past30Days = remember(archiveList) {
        val cal = Calendar.getInstance(qatarZone)
        val daysList = mutableListOf<DailyHistorySummary>()
        val dateFmtMain = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).apply { timeZone = qatarZone }
        val dateFmtKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = qatarZone }

        for (i in 0 until 30) {
            val dateCal = cal.clone() as Calendar
            dateCal.add(Calendar.DAY_OF_YEAR, -i)

            val startCal = dateCal.clone() as Calendar
            startCal.set(Calendar.HOUR_OF_DAY, 0)
            startCal.set(Calendar.MINUTE, 0)
            startCal.set(Calendar.SECOND, 0)
            startCal.set(Calendar.MILLISECOND, 0)
            val startMillis = startCal.timeInMillis

            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_YEAR, 1)
            val endMillis = endCal.timeInMillis

            val dayItems = archiveList.filter { item ->
                val t = item.completionTimeMillis ?: item.readyTimeMillis ?: item.entryTimeMillis
                t in startMillis until endMillis
            }

            val dayDurations = dayItems.mapNotNull { item ->
                val end = item.completionTimeMillis ?: item.readyTimeMillis
                if (end != null && end >= item.entryTimeMillis) {
                    (end - item.entryTimeMillis) / (1000 * 60)
                } else null
            }

            val dayAvgMins = if (dayDurations.isNotEmpty()) dayDurations.average().toLong() else 0L

            val titleStr = when (i) {
                0 -> "Today (${dateFmtMain.format(dateCal.time)})"
                1 -> "Yesterday (${dateFmtMain.format(dateCal.time)})"
                else -> dateFmtMain.format(dateCal.time)
            }

            daysList.add(
                DailyHistorySummary(
                    dateDisplay = titleStr,
                    dateKey = dateFmtKey.format(dateCal.time),
                    count = dayItems.size,
                    avgDurationMins = dayAvgMins,
                    items = dayItems,
                    isToday = (i == 0)
                )
            )
        }
        daysList
    }

    val filteredArchive = remember(archiveList, archiveSearchQuery) {
        if (archiveSearchQuery.isBlank()) archiveList
        else archiveList.filter {
            it.bikeNumber.contains(archiveSearchQuery, ignoreCase = true) ||
            timeFormat.format(Date(it.entryTimeMillis)).contains(archiveSearchQuery, ignoreCase = true)
        }
    }

    val onAddBike = {
        val trimmed = bikeNumberInput.trim()
        if (trimmed.isNotBlank()) {
            viewModel.addBikeToServiceQueue(trimmed) { success, msg ->
                statusMessage = msg
                if (success) {
                    bikeNumberInput = ""
                }
            }
        } else {
            Toast.makeText(context, "Please enter a bike number", Toast.LENGTH_SHORT).show()
        }
    }

    if (showClearArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showClearArchiveDialog = false },
            title = { Text("Clear Service Archive?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete all ${archiveList.size} archived bike service records. This action cannot be undone.", color = TextSecondary, fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showClearArchiveDialog = false
                        viewModel.clearServiceArchive { _, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Clear All", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearArchiveDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // TOP SUMMARY CARDS SECTION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ GARAGE PERFORMANCE SUMMARY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card 1: Total Bikes Serviced Today
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "SERVICED TODAY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${todayCompletedList.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "bikes completed",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Card 2: Current Queue Size
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "CURRENT QUEUE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotorOrange
                                    )
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        tint = MotorOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${queueList.size}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${queuedBikes.size} waiting • ${readyBikes.size} ready",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Card 3: Average Turnaround Time
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "AVG TURNAROUND",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2196F3)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (durationsMins.isNotEmpty()) formatDuration(avgDurationMins) else "--",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (durationsMins.isNotEmpty()) "Fastest: ${formatDuration(minDurationMins)}" else "no duration data",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-Navigation Tabs: Active Queue vs 30-Day History & Archive
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isQueueTab = adminTab == "ACTIVE_QUEUE"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isQueueTab) MotorOrange.copy(alpha = 0.2f) else SurfaceDark
                    ),
                    border = BorderStroke(
                        width = if (isQueueTab) 1.5.dp else 1.dp,
                        color = if (isQueueTab) MotorOrange else BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { adminTab = "ACTIVE_QUEUE" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = if (isQueueTab) MotorOrange else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Active Queue (${queueList.size})",
                            fontWeight = if (isQueueTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isQueueTab) TextPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                val isHistoryTab = adminTab != "ACTIVE_QUEUE"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHistoryTab) Color(0xFF9C27B0).copy(alpha = 0.2f) else SurfaceDark
                    ),
                    border = BorderStroke(
                        width = if (isHistoryTab) 1.5.dp else 1.dp,
                        color = if (isHistoryTab) Color(0xFF9C27B0) else BorderColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { adminTab = "HISTORY_30_DAYS" }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = if (isHistoryTab) Color(0xFFCE93D8) else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "30-Day History & Archive (${archiveList.size})",
                            fontWeight = if (isHistoryTab) FontWeight.Bold else FontWeight.Medium,
                            color = if (isHistoryTab) TextPrimary else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (adminTab == "ACTIVE_QUEUE") {
            // Entry Box: Single field Bike Number
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "➕ ADD BIKE TO SERVICE QUEUE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotorOrange,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enter bike number only. Date and arrival time will be auto-captured instantly.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bikeNumberInput,
                                onValueChange = { bikeNumberInput = it.filter { ch -> ch.isLetterOrDigit() || ch == '-' || ch == ' ' } },
                                label = { Text("Bike Number (e.g. 31235)") },
                                placeholder = { Text("31235") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { onAddBike() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MotorOrange,
                                    unfocusedBorderColor = BorderColor,
                                    focusedLabelColor = MotorOrange,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = SurfaceDarkElevated,
                                    unfocusedContainerColor = SurfaceDarkElevated
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = { onAddBike() },
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add to Queue", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (statusMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = statusMessage,
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Summary Stats Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("TOTAL QUEUED", fontSize = 9.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                            Text("${queueList.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFFFC107).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("IN SERVICE", fontSize = 9.sp, color = Color(0xFFFFC107), fontWeight = FontWeight.Bold)
                            Text("${queuedBikes.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC107))
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("READY PICKUP", fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                            Text("${readyBikes.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("SERVICED TODAY", fontSize = 9.sp, color = Color(0xFF2196F3), fontWeight = FontWeight.Bold)
                            Text("${todayCompletedList.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                        }
                    }
                }
            }

            // Header for Active Queue Table
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE SERVICE QUEUE (${queueList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )

                    if (readyBikes.isNotEmpty()) {
                        Button(
                            onClick = {
                                viewModel.archiveAllReadyBikes { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Archive All (${readyBikes.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Compact Ready Bikes Banner (if any)
            if (readyBikes.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2E17)),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                Text("READY FOR PICKUP (${readyBikes.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(readyBikes, key = { it.id }) { readyItem ->
                                    Surface(
                                        color = Color(0xFF1B4223),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column {
                                                Text("Bike #${readyItem.bikeNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("Ready", fontSize = 9.sp, color = Color(0xFF81C784))
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.completeAndArchiveBike(readyItem.id) { _, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                                shape = RoundedCornerShape(4.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 1.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Archive", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            IconButton(
                                                onClick = {
                                                    com.example.util.QueueNotificationAudioHelper.playReadyChimeAndAnnouncement(context, readyItem.bikeNumber)
                                                    Toast.makeText(context, "Announcing Bike #${readyItem.bikeNumber}...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Notifications, contentDescription = "Announce", tint = Color(0xFF81C784), modifier = Modifier.size(12.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.removeBikeFromQueue(readyItem.id) { _, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (queueList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Active queue is currently empty",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Enter a bike number above to add it to the live service queue.",
                                    color = TextDisabled,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(queueList, key = { _, item -> item.id }) { index, item ->
                    AdminQueueRowCard(
                        queuePosition = index + 1,
                        item = item,
                        onMarkReady = {
                            viewModel.markBikeReadyInQueue(item.id) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                com.example.util.QueueNotificationAudioHelper.playReadyChimeAndAnnouncement(context, item.bikeNumber)
                            }
                        },
                        onCompleteAndArchive = {
                            viewModel.completeAndArchiveBike(item.id) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onRemove = {
                            viewModel.removeBikeFromQueue(item.id) { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            // UNIFIED 30-DAY SERVICE HISTORY & ARCHIVE VIEW
            item {
                val total30DayServiced = remember(past30Days) { past30Days.sumOf { it.count } }
                val activeDaysCount = remember(past30Days) { past30Days.count { it.count > 0 } }
                val avgDailyVolume = remember(past30Days) {
                    if (past30Days.isNotEmpty()) past30Days.map { it.count }.average() else 0.0
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📅 30-DAY SERVICE HISTORY & ARCHIVE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCE93D8),
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF9C27B0).copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("30-Day Log", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                border = BorderStroke(1.dp, Color(0xFF9C27B0).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("30-DAY TOTAL", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$total30DayServiced", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCE93D8))
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("TODAY", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${todayCompletedList.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("AVG TURNAROUND", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        if (durationsMins.isNotEmpty()) formatDuration(avgDurationMins) else "--",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotorOrange
                                    )
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("ACTIVE DAYS", fontSize = 8.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("$activeDaysCount / 30", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2196F3))
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Clear Archive Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = archiveSearchQuery,
                        onValueChange = { archiveSearchQuery = it },
                        placeholder = { Text("Search by Bike # or Date...", fontSize = 12.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (archiveSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { archiveSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF9C27B0),
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDarkElevated,
                            unfocusedContainerColor = SurfaceDarkElevated
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    if (archiveList.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearArchiveDialog = true }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Archive", fontSize = 11.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (archiveList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TextDisabled,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No completed bike records in 30-day history",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Bikes marked completed in the active queue will automatically appear here grouped by date for 30 days.",
                                    color = TextDisabled,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else if (archiveSearchQuery.isNotBlank()) {
                // Showing search results as flat cards
                if (filteredArchive.isEmpty()) {
                    item {
                        Text("No records match '$archiveSearchQuery'", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    itemsIndexed(filteredArchive, key = { _, item -> item.id }) { _, item ->
                        AdminArchiveRowCard(
                            item = item,
                            timeFormat = timeFormat,
                            formatDuration = ::formatDuration,
                            onRestore = {
                                viewModel.restoreBikeFromArchive(item.id) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDelete = {
                                viewModel.deleteArchivedBike(item.id) { _, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            } else {
                // Showing 30-day date accordion
                itemsIndexed(past30Days, key = { _, day -> day.dateKey }) { _, day ->
                    val isExpanded = expandedDateKeys.contains(day.dateKey)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(
                            1.dp,
                            if (day.isToday) MotorOrange.copy(alpha = 0.6f)
                            else if (day.count > 0) Color(0xFF9C27B0).copy(alpha = 0.3f)
                            else BorderColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedDateKeys = if (isExpanded) {
                                            expandedDateKeys - day.dateKey
                                        } else {
                                            expandedDateKeys + day.dateKey
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = day.dateDisplay,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (day.isToday) MotorOrange else TextPrimary
                                        )
                                        if (day.isToday) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MotorOrange)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("TODAY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (day.count > 0) "⏱️ Avg Turnaround: ${formatDuration(day.avgDurationMins)}" else "No bikes serviced on this date",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (day.count > 0) Color(0xFF4CAF50).copy(alpha = 0.2f)
                                                else SurfaceDarkElevated
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${day.count} Bikes",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (day.count > 0) Color(0xFF4CAF50) else TextDisabled
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))

                                if (day.items.isEmpty()) {
                                    Text(
                                        text = "No completed bike records for this day.",
                                        fontSize = 11.sp,
                                        color = TextDisabled,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        day.items.forEach { item ->
                                            AdminArchiveRowCard(
                                                item = item,
                                                timeFormat = timeFormat,
                                                formatDuration = ::formatDuration,
                                                onRestore = {
                                                    viewModel.restoreBikeFromArchive(item.id) { _, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onDelete = {
                                                    viewModel.deleteArchivedBike(item.id) { _, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
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

@Composable
private fun AdminQueueRowCard(
    queuePosition: Int,
    item: ServiceQueueItem,
    onMarkReady: () -> Unit,
    onCompleteAndArchive: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReady = item.status == "READY"
    val qatarZone = remember { TimeZone.getTimeZone("Asia/Qatar") }
    val timeFormat = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).apply { timeZone = qatarZone } }
    val timeStr = remember(item.entryTimeMillis) { timeFormat.format(Date(item.entryTimeMillis)) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isReady) Color(0xFF4CAF50).copy(alpha = 0.6f) else BorderColor
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Queue Position Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isReady) Color(0xFF4CAF50).copy(alpha = 0.2f) else MotorOrange.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$queuePosition",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReady) Color(0xFF4CAF50) else MotorOrange
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Bike Info & Timestamp
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Bike #${item.bikeNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isReady) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFFC107).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isReady) "READY" else "IN QUEUE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) Color(0xFF4CAF50) else Color(0xFFFFC107)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = TextDisabled,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isReady) {
                    Button(
                        onClick = onMarkReady,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Mark Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Button(
                        onClick = onCompleteAndArchive,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Archive", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = TextDisabled, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AdminArchiveRowCard(
    item: ServiceQueueItem,
    timeFormat: SimpleDateFormat,
    formatDuration: (Long) -> String,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val entryStr = remember(item.entryTimeMillis) { timeFormat.format(Date(item.entryTimeMillis)) }
    val compTime = item.completionTimeMillis ?: item.readyTimeMillis ?: System.currentTimeMillis()
    val compStr = remember(compTime) { timeFormat.format(Date(compTime)) }

    val durationMins = remember(item.entryTimeMillis, compTime) {
        if (compTime >= item.entryTimeMillis) (compTime - item.entryTimeMillis) / (1000 * 60) else 0L
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Bike #${item.bikeNumber}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2196F3).copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "COMPLETED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3)
                        )
                    }
                }

                // Turnaround Duration Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MotorOrange.copy(alpha = 0.15f))
                        .border(1.dp, MotorOrange.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "⏱️ ${formatDuration(durationMins)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Entered: ", fontSize = 11.sp, color = TextSecondary)
                        Text(entryStr, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Completed: ", fontSize = 11.sp, color = TextSecondary)
                        Text(compStr, fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = onRestore,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore to Queue", fontSize = 10.sp, color = TextSecondary)
                    }

                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = TextDisabled, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 2. RIDER / TV DISPLAY VIEW (READ-ONLY HIGH-CONTRAST TV VIEW)
// ---------------------------------------------------------------------------
@Composable
private fun QueueTvDisplayView(
    queueList: List<ServiceQueueItem>,
    context: Context,
    onExitTvMode: () -> Unit = {}
) {
    val readyList = remember(queueList) { queueList.filter { it.status == "READY" } }
    val waitingList = remember(queueList) { queueList.filter { it.status == "QUEUED" } }

    val qatarZone = remember { TimeZone.getTimeZone("Asia/Qatar") }

    // Initialize TTS and Audio helper
    DisposableEffect(context) {
        com.example.util.QueueNotificationAudioHelper.initTts(context)
        onDispose { }
    }

    // Audio chime effect and voice announcement whenever ready list gets new bikes
    var prevReadyIds by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(readyList) {
        val currentIds = readyList.map { it.id }.toSet()
        if (prevReadyIds != null) {
            val newlyReady = readyList.filter { it.id !in (prevReadyIds ?: emptySet()) }
            newlyReady.forEach { newReadyItem ->
                com.example.util.QueueNotificationAudioHelper.playReadyChimeAndAnnouncement(context, newReadyItem.bikeNumber)
            }
        }
        prevReadyIds = currentIds
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()).apply { timeZone = qatarZone } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val isWideTvScreen = screenWidth >= 700.dp
        val itemsPerRow = if (screenWidth >= 1000.dp) 5 else if (screenWidth >= 600.dp) 4 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Section A: READY FOR PICKUP (PROMINENT GREEN HIGHLIGHT & FULL TV GRID)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (readyList.isNotEmpty()) Color(0xFF0F2E17) else SurfaceDark
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (readyList.isNotEmpty()) Color(0xFF4CAF50).copy(alpha = borderAlpha) else BorderColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "READY FOR PICKUP",
                                    fontSize = if (isWideTvScreen) 20.sp else 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF4CAF50),
                                    letterSpacing = 1.5.sp
                                )
                            }

                            if (readyList.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF4CAF50))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${readyList.size} BIKES READY",
                                        fontSize = if (isWideTvScreen) 13.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (readyList.isEmpty()) {
                            Text(
                                text = "No bikes ready for pickup at this moment.",
                                fontSize = 14.sp,
                                color = TextDisabled,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            // Display ALL Ready Bikes in multi-row responsive grid
                            val chunks = readyList.chunked(itemsPerRow)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                chunks.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { readyItem ->
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4223)),
                                                border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = "#${readyItem.bikeNumber}",
                                                        fontSize = if (isWideTvScreen) 28.sp else 22.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        letterSpacing = 1.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Ready @ ${timeFormat.format(Date(readyItem.readyTimeMillis ?: readyItem.entryTimeMillis))}",
                                                        fontSize = if (isWideTvScreen) 11.sp else 9.sp,
                                                        color = Color(0xFF81C784),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(
                                                        onClick = {
                                                            com.example.util.QueueNotificationAudioHelper.playReadyChimeAndAnnouncement(context, readyItem.bikeNumber)
                                                            Toast.makeText(context, "Announcing Bike #${readyItem.bikeNumber}...", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Announce", tint = Color.White, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Announce", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                        // Fill remaining slots in row for consistent card sizing
                                        val remaining = itemsPerRow - rowItems.size
                                        if (remaining > 0) {
                                            repeat(remaining) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section B: WAITING QUEUE LIST (FIFO ORDER)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WAITING IN QUEUE (FIRST-IN, FIRST-OUT ORDER)",
                        fontSize = if (isWideTvScreen) 14.sp else 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    if (waitingList.isNotEmpty()) {
                        Text(
                            text = "${waitingList.size} IN QUEUE",
                            fontSize = if (isWideTvScreen) 13.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MotorOrange
                        )
                    }
                }
            }

            if (waitingList.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("All bikes in service have been completed!", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                item {
                    // Table Header Column Labels
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Queue No.", modifier = Modifier.weight(1.2f), fontSize = if (isWideTvScreen) 13.sp else 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Bike Number", modifier = Modifier.weight(2.0f), fontSize = if (isWideTvScreen) 13.sp else 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Date & Entry Time", modifier = Modifier.weight(2.5f), fontSize = if (isWideTvScreen) 13.sp else 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Status", modifier = Modifier.weight(1.5f), fontSize = if (isWideTvScreen) 13.sp else 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                    }
                }

                itemsIndexed(waitingList, key = { _, item -> item.id }) { index, item ->
                    val fullDateFormat = remember { SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).apply { timeZone = qatarZone } }
                    val entryTimeStr = remember(item.entryTimeMillis) { fullDateFormat.format(Date(item.entryTimeMillis)) }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (index % 2 == 0) SurfaceDark else SurfaceDarkElevated
                        ),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = if (isWideTvScreen) 16.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Queue No.
                            Text(
                                text = "Position #${index + 1}",
                                modifier = Modifier.weight(1.2f),
                                fontSize = if (isWideTvScreen) 15.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MotorOrange
                            )

                            // Bike Number
                            Text(
                                text = item.bikeNumber,
                                modifier = Modifier.weight(2.0f),
                                fontSize = if (isWideTvScreen) 18.sp else 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )

                            // Date & Entry Time
                            Text(
                                text = entryTimeStr,
                                modifier = Modifier.weight(2.5f),
                                fontSize = if (isWideTvScreen) 14.sp else 12.sp,
                                color = TextSecondary
                            )

                            // Status
                            Box(
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFFC107).copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "In Progress",
                                        fontSize = if (isWideTvScreen) 12.sp else 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFC107)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onExitTvMode) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch to Admin View", fontSize = 13.sp, color = TextDisabled)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 3. RIDER MOBILE TRACKER VIEW
// ---------------------------------------------------------------------------
@Composable
private fun QueueRiderTrackerView(
    queueList: List<ServiceQueueItem>,
    context: Context
) {
    var riderBikeInput by remember { mutableStateOf("") }
    var trackedBikeNumber by remember { mutableStateOf("") }

    val trackedItem = remember(queueList, trackedBikeNumber) {
        if (trackedBikeNumber.isBlank()) null
        else queueList.find { it.bikeNumber.equals(trackedBikeNumber.trim(), ignoreCase = true) }
    }

    val waitingList = remember(queueList) { queueList.filter { it.status == "QUEUED" } }
    val position = remember(waitingList, trackedItem) {
        if (trackedItem == null || trackedItem.status != "QUEUED") -1
        else waitingList.indexOfFirst { it.id == trackedItem.id } + 1
    }

    var hasChimed by remember { mutableStateOf(false) }

    LaunchedEffect(trackedItem?.status) {
        if (trackedItem?.status == "READY" && !hasChimed) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 800)
                hasChimed = true
            } catch (e: Exception) {
                // Audio
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📱 TRACK YOUR BIKE SERVICE STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your bike number to see live queue position and receive instant pickup alert.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = riderBikeInput,
                            onValueChange = { riderBikeInput = it },
                            label = { Text("Your Bike Number") },
                            placeholder = { Text("e.g. 31235") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MotorOrange,
                                unfocusedBorderColor = BorderColor,
                                focusedLabelColor = MotorOrange,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = SurfaceDarkElevated,
                                unfocusedContainerColor = SurfaceDarkElevated
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                trackedBikeNumber = riderBikeInput.trim()
                                hasChimed = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(54.dp)
                        ) {
                            Text("Track Status", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (trackedBikeNumber.isNotEmpty()) {
            item {
                if (trackedItem == null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bike #$trackedBikeNumber is not in the active queue.", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text("Please verify with the garage admin if your bike has been registered.", color = TextDisabled, fontSize = 11.sp)
                        }
                    }
                } else if (trackedItem.status == "READY") {
                    // Ready Alert Banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4223)),
                        border = BorderStroke(2.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(56.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("YOUR BIKE IS READY FOR PICKUP!", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Bike #${trackedItem.bikeNumber}", fontWeight = FontWeight.Bold, color = Color(0xFF81C784), fontSize = 22.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Please proceed to Counter 1 / Mechanic Bay for key handover.", color = TextPrimary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    // Waiting in queue details
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        border = BorderStroke(1.dp, Color(0xFFFFC107)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("STATUS: IN SERVICE QUEUE", fontWeight = FontWeight.Bold, color = Color(0xFFFFC107), fontSize = 12.sp, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MotorOrange.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#$position", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MotorOrange)
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Your Bike #${trackedItem.bikeNumber} is at Position #$position", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)

                            val aheadCount = (position - 1).coerceAtLeast(0)
                            Text(
                                text = if (aheadCount == 0) "Your bike is currently being worked on!" else "$aheadCount bike(s) ahead of you in queue.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
