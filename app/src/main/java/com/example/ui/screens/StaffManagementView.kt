package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.example.ui.components.AnimatedDialogContainer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.StaffMember
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import java.io.OutputStream
import kotlin.math.roundToInt
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementView(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val staffList by viewModel.staffMembers.collectAsState()
    val shiftATime by viewModel.shiftATiming.collectAsState()
    val shiftBTime by viewModel.shiftBTiming.collectAsState()

    var isManageTeamsMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncStaffMembersWithFirebase()
    }
    
    // Add Staff states
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var newStaffName by remember { mutableStateOf("") }
    var newStaffDesignation by remember { mutableStateOf("Mechanic") }
    var newStaffShift by remember { mutableStateOf("Shift A") }
    var showDesignationDropdown by remember { mutableStateOf(false) }

    // Edit Timings states
    var showEditTimingsDialog by remember { mutableStateOf(false) }
    var editShiftATime by remember { mutableStateOf("") }
    var editShiftBTime by remember { mutableStateOf("") }

    val designations = listOf("Mechanic", "Supervisor", "Storekeeper", "Admin", "Cleaner")
    val daysOfWeek = listOf("None", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    Column(modifier = Modifier.fillMaxSize().background(SlateDark)) {

        TabRow(
            selectedTabIndex = if (isManageTeamsMode) 1 else 0,
            containerColor = SurfaceDark,
            contentColor = MotorOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (isManageTeamsMode) 1 else 0]),
                    color = MotorOrange
                )
            }
        ) {
            Tab(
                selected = !isManageTeamsMode,
                onClick = { isManageTeamsMode = false },
                text = { Text("Staff List", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = isManageTeamsMode,
                onClick = { isManageTeamsMode = true },
                text = { Text("Manage Teams & Shifts", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Add & Export Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        newStaffName = ""
                        newStaffDesignation = "Mechanic"
                        newStaffShift = "Shift A"
                        showAddStaffDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1.3f).height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = SlateDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add New Staff", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SlateDark)
                }

                var showExportDropdown by remember { mutableStateOf(false) }

                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = { showExportDropdown = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkElevated),
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Export", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(12.dp))
                    }

                    DropdownMenu(
                        expanded = showExportDropdown,
                        onDismissRequest = { showExportDropdown = false },
                        modifier = Modifier.background(SurfaceDarkElevated)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.List, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download as Excel File", color = TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showExportDropdown = false
                                exportStaffExcel(context, staffList, shiftATime, shiftBTime)
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Menu, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Download as PDF File", color = TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = {
                                showExportDropdown = false
                                exportStaffPdf(context, staffList, shiftATime, shiftBTime)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = isManageTeamsMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { isDragMode ->
                if (isDragMode) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Timings Row - ONLY visible in Manage Teams
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("SHIFT WORK HOURS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Shift A (Morning): $shiftATime", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("Shift B (Evening): $shiftBTime", fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                }
                                IconButton(
                                    onClick = {
                                        editShiftATime = shiftATime
                                        editShiftBTime = shiftBTime
                                        showEditTimingsDialog = true
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MotorOrange.copy(alpha = 0.12f))
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Shift Timings", tint = MotorOrange, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Manage Teams Drag interface (Split Table)
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Column: Shift A
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MotorOrange.copy(alpha = 0.15f))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "SHIFT A (MORNING)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MotorOrange
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val shiftAMembers = staffList.filter { it.shift == "Shift A" }
                                if (shiftAMembers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Empty Team", fontSize = 11.sp, color = TextDisabled)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(shiftAMembers) { member ->
                                            DraggableStaffCard(
                                                member = member,
                                                onDragToRight = {
                                                    viewModel.addStaffMember(member.copy(shift = "Shift B"))
                                                },
                                                onDragToLeft = {},
                                                onDelete = { viewModel.deleteStaffMember(member) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Split line
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(BorderColor)
                            )

                            // Right Column: Shift B
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorServiceWithoutParts.copy(alpha = 0.15f))
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "SHIFT B (EVENING)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorServiceWithoutParts
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                val shiftBMembers = staffList.filter { it.shift == "Shift B" }
                                if (shiftBMembers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Empty Team", fontSize = 11.sp, color = TextDisabled)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(shiftBMembers) { member ->
                                            DraggableStaffCard(
                                                member = member,
                                                onDragToRight = {},
                                                onDragToLeft = {
                                                    viewModel.addStaffMember(member.copy(shift = "Shift A"))
                                                },
                                                onDelete = { viewModel.deleteStaffMember(member) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Regular Staff List
                    if (staffList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No registered staff found.", color = TextDisabled, fontSize = 13.sp)
                            }
                        }
                    } else {
                        val shiftAMembersSorted = staffList.filter { !it.shift.equals("Shift B", ignoreCase = true) }
                            .sortedWith(compareBy({ getDesignationRank(it.designation) }, { it.name }))

                        val shiftBMembersSorted = staffList.filter { it.shift.equals("Shift B", ignoreCase = true) }
                            .sortedWith(compareBy({ getDesignationRank(it.designation) }, { it.name }))

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Column: Team A (Shift A)
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MotorOrange.copy(alpha = 0.12f))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "TEAM A (${shiftAMembersSorted.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MotorOrange,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            shiftATime,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (shiftAMembersSorted.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No members", fontSize = 10.sp, color = TextDisabled)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(shiftAMembersSorted, key = { it.name }) { staff ->
                                            StaffTableRow(
                                                staff = staff,
                                                daysOfWeek = daysOfWeek,
                                                onOffChange = { newDay ->
                                                    viewModel.addStaffMember(staff.copy(weeklyOff = newDay))
                                                },
                                                onDelete = {
                                                    viewModel.deleteStaffMember(staff)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(BorderColor.copy(alpha = 0.5f))
                            )

                            // Right Column: Team B (Shift B)
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(ColorServiceWithoutParts.copy(alpha = 0.12f))
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "TEAM B (${shiftBMembersSorted.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ColorServiceWithoutParts,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            shiftBTime,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextSecondary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                if (shiftBMembersSorted.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .border(1.dp, BorderColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No members", fontSize = 10.sp, color = TextDisabled)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(shiftBMembersSorted, key = { it.name }) { staff ->
                                            StaffTableRow(
                                                staff = staff,
                                                daysOfWeek = daysOfWeek,
                                                onOffChange = { newDay ->
                                                    viewModel.addStaffMember(staff.copy(weeklyOff = newDay))
                                                },
                                                onDelete = {
                                                    viewModel.deleteStaffMember(staff)
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

    // Modal Dialog to Add Staff
    if (showAddStaffDialog) {
        Dialog(onDismissRequest = { showAddStaffDialog = false }) {
            AnimatedDialogContainer {
                Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Add New Staff",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MotorOrange
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newStaffName,
                        onValueChange = { newStaffName = it },
                        placeholder = { Text("Full Name (e.g. John Doe)", color = TextDisabled) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotorOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = MotorOrange,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = MotorOrange
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Designation drop down selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { showDesignationDropdown = !showDesignationDropdown },
                            colors = CardDefaults.outlinedCardColors(containerColor = SurfaceDarkElevated),
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
                                    Text("Designation", fontSize = 9.sp, color = TextSecondary)
                                    Text(newStaffDesignation, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextPrimary)
                            }
                        }

                        DropdownMenu(
                            expanded = showDesignationDropdown,
                            onDismissRequest = { showDesignationDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(SurfaceDarkElevated)
                        ) {
                            designations.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d, color = TextPrimary) },
                                    onClick = {
                                        newStaffDesignation = d
                                        showDesignationDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Team assignment row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { newStaffShift = "Shift A" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newStaffShift == "Shift A") MotorOrange else SurfaceDarkElevated
                            ),
                            border = BorderStroke(1.dp, if (newStaffShift == "Shift A") MotorOrange else BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Shift A",
                                color = if (newStaffShift == "Shift A") SlateDark else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { newStaffShift = "Shift B" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newStaffShift == "Shift B") MotorOrange else SurfaceDarkElevated
                            ),
                            border = BorderStroke(1.dp, if (newStaffShift == "Shift B") MotorOrange else BorderColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "Shift B",
                                color = if (newStaffShift == "Shift B") SlateDark else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showAddStaffDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                if (newStaffName.trim().isBlank()) {
                                    Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addStaffMember(
                                        StaffMember(
                                            name = newStaffName.trim(),
                                            shift = newStaffShift,
                                            designation = newStaffDesignation,
                                            weeklyOff = "None"
                                        )
                                    )
                                    viewModel.syncStaffMembersWithFirebase()
                                    showAddStaffDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Staff", color = SlateDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog to Edit Timings
    if (showEditTimingsDialog) {
        Dialog(onDismissRequest = { showEditTimingsDialog = false }) {
            AnimatedDialogContainer {
                Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Shift Timings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MotorOrange
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editShiftATime,
                        onValueChange = { editShiftATime = it },
                        label = { Text("Shift A (Morning)", color = MotorOrange) },
                        placeholder = { Text("08:00 am - 06:00 pm", color = TextDisabled) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotorOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = MotorOrange,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = MotorOrange
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editShiftBTime,
                        onValueChange = { editShiftBTime = it },
                        label = { Text("Shift B (Evening)", color = MotorOrange) },
                        placeholder = { Text("06:00 pm - 04:00 am", color = TextDisabled) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MotorOrange,
                            unfocusedBorderColor = BorderColor,
                            focusedLabelColor = MotorOrange,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = MotorOrange
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { showEditTimingsDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                viewModel.updateShiftATiming(editShiftATime.trim())
                                viewModel.updateShiftBTiming(editShiftBTime.trim())
                                showEditTimingsDialog = false
                                Toast.makeText(context, "Shift timings updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save", color = SlateDark, fontWeight = FontWeight.Bold)
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
fun DraggableStaffCard(
    member: StaffMember,
    onDragToRight: () -> Unit,
    onDragToLeft: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animOffsetX by animateFloatAsState(targetValue = offsetX)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
        border = BorderStroke(1.dp, BorderColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .offset { IntOffset(animOffsetX.roundToInt(), 0) }
            .pointerInput(member) {
                detectDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX > 80f) {
                            onDragToRight()
                        } else if (offsetX < -80f) {
                            onDragToLeft()
                        }
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                    }
                )
            }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag to swap",
                        tint = TextDisabled,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = member.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MotorOrange.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(member.designation, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MotorOrange)
                }
                if (member.weeklyOff != "None") {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ColorServiceWithoutParts.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Off: ${member.weeklyOff}", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = ColorServiceWithoutParts)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member.shift == "Shift A") {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MotorOrange.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("drag right to Shift B", fontSize = 8.sp, color = TextDisabled)
                } else {
                    Text("drag left to Shift A", fontSize = 8.sp, color = TextDisabled)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = ColorServiceWithoutParts.copy(alpha = 0.5f), modifier = Modifier.size(10.dp))
                }
            }
        }
    }
}

// PDF & Excel Export functions for Staff
fun exportStaffPdf(
    context: android.content.Context,
    staffMembers: List<StaffMember>,
    shiftATime: String,
    shiftBTime: String
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

    // Header Bar
    paint.color = android.graphics.Color.rgb(255, 102, 0) // MotorOrange Accent
    canvas.drawRect(30f, 30f, 565f, 75f, paint)

    textPaint.color = android.graphics.Color.WHITE
    textPaint.textSize = 16f
    textPaint.isFakeBoldText = true
    canvas.drawText("ADVANCE AUTO - STAFF DIRECTORY REPORT", 45f, 58f, textPaint)

    textPaint.color = android.graphics.Color.BLACK
    textPaint.textSize = 11f
    textPaint.isFakeBoldText = false
    canvas.drawText("Shift A Timing: $shiftATime", 45f, 100f, textPaint)
    canvas.drawText("Shift B Timing: $shiftBTime", 45f, 116f, textPaint)
    canvas.drawText("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", 45f, 132f, textPaint)

    var y = 160f

    // Table Header
    paint.color = android.graphics.Color.rgb(240, 240, 240)
    canvas.drawRect(30f, y, 565f, y + 25f, paint)

    textPaint.isFakeBoldText = true
    canvas.drawText("Staff Name", 35f, y + 17f, textPaint)
    canvas.drawText("Designation", 180f, y + 17f, textPaint)
    canvas.drawText("Assigned Shift", 320f, y + 17f, textPaint)
    canvas.drawText("Weekly Day Off", 450f, y + 17f, textPaint)

    y += 25f
    textPaint.isFakeBoldText = false

    staffMembers.forEach { member ->
        if (y > 780f) return@forEach

        paint.color = android.graphics.Color.rgb(252, 252, 252)
        canvas.drawRect(30f, y, 565f, y + 25f, paint)

        canvas.drawText(member.name, 35f, y + 17f, textPaint)
        canvas.drawText(member.designation, 180f, y + 17f, textPaint)
        canvas.drawText(member.shift, 320f, y + 17f, textPaint)
        canvas.drawText(member.weeklyOff, 450f, y + 17f, textPaint)

        y += 25f
        paint.color = android.graphics.Color.rgb(220, 220, 220)
        canvas.drawLine(30f, y, 565f, y, paint)
    }

    pdfDocument.finishPage(page)

    val fileName = "Advance_Auto_Staff_Directory.pdf"
    saveFileCompat(context, fileName, "application/pdf") { out ->
        pdfDocument.writeTo(out)
    }
    Toast.makeText(context, "Staff directory PDF saved to Downloads folder successfully!", Toast.LENGTH_LONG).show()
    pdfDocument.close()
}

fun exportStaffExcel(
    context: android.content.Context,
    staffMembers: List<StaffMember>,
    shiftATime: String,
    shiftBTime: String
) {
    val csvBuilder = java.lang.StringBuilder()
    csvBuilder.append("Advance Auto Staff Directory\n")
    csvBuilder.append("Shift A Timing:,$shiftATime\n")
    csvBuilder.append("Shift B Timing:,$shiftBTime\n\n")
    csvBuilder.append("Staff Name,Designation,Shift,Weekly Day Off\n")

    staffMembers.forEach { member ->
        val escapedName = if (member.name.contains(",")) "\"${member.name}\"" else member.name
        csvBuilder.append("$escapedName,${member.designation},${member.shift},${member.weeklyOff}\n")
    }

    val fileName = "Advance_Auto_Staff_Directory.csv"
    saveFileCompat(context, fileName, "text/csv") { out ->
        out.write(csvBuilder.toString().toByteArray())
    }
    Toast.makeText(context, "Staff directory CSV saved to Downloads successfully!", Toast.LENGTH_LONG).show()
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

fun getDesignationRank(designation: String): Int {
    return when (designation) {
        "Supervisor" -> 1
        "Admin" -> 2
        "Mechanic" -> 3
        "Cleaner" -> 4
        "Storekeeper" -> 5
        else -> 6
    }
}

@Composable
fun getDesignationHighlightColors(designation: String): Triple<Color, Color, Color> {
    val isLight = MaterialTheme.colorScheme.background == LightSlateDark
    return when (designation) {
        "Supervisor" -> {
            val baseColor = if (isLight) Color(0xFFD84315) else Color(0xFFFF8A65)
            Triple(baseColor.copy(alpha = 0.08f), baseColor, baseColor.copy(alpha = 0.25f))
        }
        "Admin" -> {
            val baseColor = MotorOrange
            Triple(baseColor.copy(alpha = 0.08f), baseColor, baseColor.copy(alpha = 0.25f))
        }
        "Cleaner" -> {
            val baseColor = if (isLight) Color(0xFF2E7D32) else Color(0xFF81C784)
            Triple(baseColor.copy(alpha = 0.08f), baseColor, baseColor.copy(alpha = 0.25f))
        }
        "Storekeeper" -> {
            val baseColor = ColorServiceWithoutParts
            Triple(baseColor.copy(alpha = 0.08f), baseColor, baseColor.copy(alpha = 0.25f))
        }
        else -> {
            Triple(SurfaceDark, TextPrimary, BorderColor)
        }
    }
}

@Composable
fun StaffTableRow(
    staff: StaffMember,
    daysOfWeek: List<String>,
    onOffChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    val highlightColors = getDesignationHighlightColors(staff.designation)
    val isHighlighted = staff.designation != "Mechanic"

    Card(
        colors = CardDefaults.cardColors(containerColor = highlightColors.first),
        border = BorderStroke(
            1.dp,
            if (isHighlighted) highlightColors.third else BorderColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            // First Row: Name and Designation Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = staff.name,
                    fontSize = 12.sp,
                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isHighlighted) highlightColors.second else TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
                
                // Compact Designation Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(highlightColors.second.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = staff.designation,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = highlightColors.second
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            
            // Second Row: Weekly Off dropdown and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var showOffDropdown by remember { mutableStateOf(false) }
                
                // Weekly Off selector
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, BorderColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .background(SurfaceDarkElevated)
                            .clickable { showOffDropdown = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (staff.weeklyOff == "None") "No Off" else "Off: ${staff.weeklyOff.take(3)}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showOffDropdown,
                        onDismissRequest = { showOffDropdown = false },
                        modifier = Modifier.background(SurfaceDarkElevated)
                    ) {
                        daysOfWeek.forEach { day ->
                            DropdownMenuItem(
                                text = { Text(day, color = TextPrimary, fontSize = 11.sp) },
                                onClick = {
                                    onOffChange(day)
                                    showOffDropdown = false
                                }
                            )
                        }
                    }
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

