package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Helper functions for status logic
fun getGarageStatus(currentBikes: Int, notBusyMax: Int = 8, moderateMax: Int = 18): String {
    return when {
        currentBikes <= notBusyMax -> "Not Busy 🟢"
        currentBikes <= moderateMax -> "Moderate 🟡"
        else -> "Busy 🔴"
    }
}

fun getGarageStatusColor(currentBikes: Int, notBusyMax: Int = 8, moderateMax: Int = 18): Color {
    return when {
        currentBikes <= notBusyMax -> Color(0xFF4CAF50)    // Green
        currentBikes <= moderateMax -> Color(0xFFFFC107)   // Amber/Yellow
        else -> Color(0xFFF44336)                  // Red
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GarageTrafficStatusView(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentBikes by viewModel.currentBikes.collectAsState()
    val notBusyMax by viewModel.notBusyMax.collectAsState()
    val moderateMax by viewModel.moderateMax.collectAsState()
    
    // Past 7 days calculation
    val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }
    
    val past7Dates = remember {
        (0..6).map { daysAgo ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
            cal.time
        }
    }
    
    var selectedDateIndex by remember { mutableStateOf(0) }
    val selectedDate = past7Dates[selectedDateIndex]
    val selectedDateStr = sdfDate.format(selectedDate)
    
    // Read the traffic count for the selected date from viewmodel
    val selectedDateTraffic = viewModel.getTrafficForDate(selectedDateStr)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Live Status Meter Card
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
                            text = "LIVE GARAGE OCCUPANCY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Large circle visualizer representing occupancy
                        val occupancyColor = getGarageStatusColor(currentBikes, notBusyMax, moderateMax)
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(55.dp))
                                .background(occupancyColor.copy(alpha = 0.12f))
                                .border(2.dp, occupancyColor, RoundedCornerShape(55.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = currentBikes.toString(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "BIKES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Status indicator label
                        Text(
                            text = getGarageStatus(currentBikes, notBusyMax, moderateMax),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = occupancyColor
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Automatically counted when riders enter or exit the 100-meter radius around the garage location (5G5F+363, Al Wukair).",
                            fontSize = 11.sp,
                            color = TextDisabled,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // Section 2: Custom Threshold Settings
            item {
                var localNotBusyMax by remember(notBusyMax) { mutableStateOf(notBusyMax.toString()) }
                var localModerateMax by remember(moderateMax) { mutableStateOf(moderateMax.toString()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚙️ STATUS THRESHOLD SETTINGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Threshold Limits
                        Text(
                            text = "Configure Status Threshold Limits",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Customize how many bikes determine each occupancy label",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = localNotBusyMax,
                                onValueChange = { localNotBusyMax = it },
                                label = { Text("Not Busy Max", fontSize = 10.sp, color = TextSecondary) },
                                singleLine = true,
                                colors = customTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = localModerateMax,
                                onValueChange = { localModerateMax = it },
                                label = { Text("Moderate Max", fontSize = 10.sp, color = TextSecondary) },
                                singleLine = true,
                                colors = customTextFieldColors(),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val notBusyInt = localNotBusyMax.toIntOrNull()
                                val moderateInt = localModerateMax.toIntOrNull()
                                if (notBusyInt != null && moderateInt != null) {
                                    if (notBusyInt >= 0 && moderateInt > notBusyInt) {
                                        viewModel.updateTrafficThresholds(notBusyInt, moderateInt)
                                        Toast.makeText(context, "Threshold limits updated!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Moderate Max must be greater than Not Busy Max", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter valid numbers", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Thresholds", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live threshold summary block
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Current Active Threshold Rules:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text("🟢 Not Busy: 0 to $notBusyMax", fontSize = 11.sp, color = Color(0xFF4CAF50))
                                    Text("🟡 Moderate: ${notBusyMax + 1} to $moderateMax", fontSize = 11.sp, color = Color(0xFFFFC107))
                                    Text("🔴 Very Busy: > $moderateMax", fontSize = 11.sp, color = Color(0xFFF44336))
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: 7-Day Historical Analytics Block
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "7-DAY TRAFFIC HISTORY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Visual bar chart for the 7 days
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            past7Dates.reversed().forEachIndexed { index, date ->
                                val dateStr = sdfDate.format(date)
                                val trafficCount = viewModel.getTrafficForDate(dateStr)
                                
                                // Normalize height (max 25 bikes represent 100% height)
                                val barWeight = (trafficCount.toFloat() / 25f).coerceIn(0.1f, 1.0f)
                                val barColor = getGarageStatusColor(trafficCount, notBusyMax, moderateMax)
                                val originalIndex = 6 - index // since list is reversed for chronological order
                                val isSelected = originalIndex == selectedDateIndex
                                
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedDateIndex = originalIndex }
                                ) {
                                    Text(
                                        text = trafficCount.toString(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MotorOrange else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight(barWeight * 0.75f) // scale to leave room for text
                                            .width(18.dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            .background(if (isSelected) barColor else barColor.copy(alpha = 0.5f))
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.dp,
                                                color = if (isSelected) Color.White else Color.Transparent,
                                                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = SimpleDateFormat("dd", Locale.getDefault()).format(date),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MotorOrange else TextDisabled
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Dropdown selection / slider details
                        Text(
                            text = "SELECT HISTORIC DATE:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Render date selectors as clean selectable chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            var expandedDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expandedDropdown = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MotorOrange),
                                    border = BorderStroke(1.dp, BorderColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${displayFormat.format(selectedDate)} (Selected) ▾",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .background(SurfaceDarkElevated)
                                        .border(1.dp, BorderColor)
                                ) {
                                    past7Dates.forEachIndexed { idx, date ->
                                        val dStr = sdfDate.format(date)
                                        val count = viewModel.getTrafficForDate(dStr)
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(displayFormat.format(date), color = TextPrimary)
                                                    Text(getGarageStatus(count, notBusyMax, moderateMax), color = getGarageStatusColor(count, notBusyMax, moderateMax), fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            onClick = {
                                                selectedDateIndex = idx
                                                expandedDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Detailed display of the selected date's logs
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = displayFormat.format(selectedDate).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MotorOrange,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Occupancy Status", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            text = getGarageStatus(selectedDateTraffic, notBusyMax, moderateMax),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = getGarageStatusColor(selectedDateTraffic, notBusyMax, moderateMax)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Max Rider Count", fontSize = 10.sp, color = TextSecondary)
                                        Text(
                                            text = "$selectedDateTraffic Riders",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextPrimary
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
