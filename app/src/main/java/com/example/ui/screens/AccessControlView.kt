package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessControlView(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val managementUsernames by viewModel.managementUsernames.collectAsState()
    var newMemberEmail by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsernames = remember(managementUsernames, searchQuery) {
        if (searchQuery.isBlank()) {
            managementUsernames
        } else {
            managementUsernames.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
    ) {
        // Top Action Header with Search and Registration
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
            border = BorderStroke(1.dp, BorderColor),
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MotorOrange,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ACCESS CONTROL MATRIX",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "${managementUsernames.size} Registered Authorized Users • Modular Feature Flags",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search & Add Bar in 2 Rows for Maximum Space
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search member...", color = TextDisabled, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = MotorOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    )

                    // Quick Add Member Field
                    OutlinedTextField(
                        value = newMemberEmail,
                        onValueChange = { newMemberEmail = it },
                        placeholder = { Text("Username / Email", color = TextDisabled, fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = MotorOrange,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp)
                    )

                    Button(
                        onClick = {
                            val input = newMemberEmail.trim().lowercase()
                            if (input.isNotEmpty()) {
                                if (input.length < 2) {
                                    Toast.makeText(context, "Must be at least 2 characters!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.registerEmailForMgmt(input)
                                    Toast.makeText(context, "Added manager: $input", Toast.LENGTH_SHORT).show()
                                    newMemberEmail = ""
                                }
                            } else {
                                Toast.makeText(context, "Please enter a username or email!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = SlateDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ADD", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = SlateDark)
                    }
                }
            }
        }

        // Users List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredUsernames, key = { it }) { username ->
                SmartUserAccessCard(
                    username = username,
                    viewModel = viewModel,
                    onDelete = {
                        viewModel.removeEmailForMgmt(username)
                        Toast.makeText(context, "Deleted user: $username", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun SmartUserAccessCard(
    username: String,
    viewModel: MotorcycleViewModel,
    onDelete: () -> Unit
) {
    val isMaster = username == "admin" || username == "ahmedraza.swift@gmail.com"
    var expanded by remember { mutableStateOf(false) }

    // Re-trigger states when permissions change
    var refreshTrigger by remember { mutableStateOf(0) }

    val isFullAccess = remember(username, refreshTrigger) {
        viewModel.isFullAccessGranted(username)
    }

    // Permission checks
    val permissionsMap = remember(username, refreshTrigger) {
        listOf(
            "SERVICE_HISTORY" to viewModel.getMgmtPermission(username, "SERVICE_HISTORY"),
            "APPOINTMENTS" to viewModel.getMgmtPermission(username, "APPOINTMENTS"),
            "SERVICE_QUEUE" to viewModel.getMgmtPermission(username, "SERVICE_QUEUE"),
            "GARAGE_TRAFFIC" to viewModel.getMgmtPermission(username, "GARAGE_TRAFFIC"),
            "MONTHLY_PERFORMANCE" to viewModel.getMgmtPermission(username, "MONTHLY_PERFORMANCE"),
            "DATA_INPUT" to viewModel.getMgmtPermission(username, "DATA_INPUT"),
            "STAFF_MANAGEMENT" to viewModel.getMgmtPermission(username, "STAFF_MANAGEMENT"),
            "FIREBASE_PORTAL" to viewModel.getMgmtPermission(username, "FIREBASE_PORTAL"),
            "ACCESS_CONTROL" to viewModel.getMgmtPermission(username, "ACCESS_CONTROL")
        )
    }

    val activeCount = permissionsMap.count { it.second }
    val totalCount = permissionsMap.size

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, if (isMaster) MotorOrange.copy(alpha = 0.6f) else BorderColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isMaster) MotorOrange.copy(alpha = 0.2f) else SurfaceDarkElevated),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMaster) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MotorOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = username.take(1).uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                color = MotorOrange,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = username.lowercase(),
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isMaster) "Super Administrator" else if (isFullAccess) "Full Access Manager" else "$activeCount of $totalCount Modules Allowed",
                            fontSize = 10.sp,
                            color = if (isMaster) MotorOrange else if (isFullAccess) Color(0xFF4CAF50) else TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isMaster -> MotorOrange.copy(alpha = 0.15f)
                                    isFullAccess -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    activeCount > 1 -> Color(0xFF2196F3).copy(alpha = 0.15f)
                                    else -> SurfaceDarkElevated
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when {
                                isMaster -> "MASTER"
                                isFullAccess -> "FULL ADMIN"
                                activeCount > 1 -> "CUSTOM ($activeCount/9)"
                                else -> "BASIC (1/9)"
                            },
                            color = when {
                                isMaster -> MotorOrange
                                isFullAccess -> Color(0xFF4CAF50)
                                activeCount > 1 -> Color(0xFF2196F3)
                                else -> TextDisabled
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }

                    if (!isMaster) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Manager",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Expandable Permission Control Center
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (isMaster) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDarkElevated),
                            border = BorderStroke(1.dp, MotorOrange.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MotorOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Primary Master account has permanent full control over all modules.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        // Role Preset Quick Actions Bar
                        Text(
                            text = "INSTANT ROLE PRESETS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "FULL" to "👑 Full Admin",
                                "SERVICE_DESK" to "🛠️ Service Desk",
                                "ANALYST" to "📊 Analyst",
                                "BASIC" to "🔒 Basic Only"
                            ).forEach { (presetKey, label) ->
                                OutlinedButton(
                                    onClick = {
                                        viewModel.applyRolePreset(username, presetKey)
                                        refreshTrigger++
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SurfaceDarkElevated,
                                        contentColor = TextPrimary
                                    ),
                                    border = BorderStroke(1.dp, BorderColor),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                ) {
                                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category 1: Operations & Queues
                        PermissionCategoryGroup(
                            title = "OPERATIONS & DESK",
                            accentColor = Color(0xFF2196F3)
                        ) {
                            PermissionModuleChip(
                                title = "Service History Search",
                                icon = Icons.Default.Search,
                                checked = true,
                                isAlwaysOn = true,
                                onToggle = {}
                            )
                            PermissionModuleChip(
                                title = "Appointment Desk",
                                icon = Icons.Default.DateRange,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "APPOINTMENTS"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "APPOINTMENTS", !viewModel.getMgmtPermission(username, "APPOINTMENTS"))
                                    refreshTrigger++
                                }
                            )
                            PermissionModuleChip(
                                title = "Service Queue Manager",
                                icon = Icons.Default.List,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "SERVICE_QUEUE"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "SERVICE_QUEUE", !viewModel.getMgmtPermission(username, "SERVICE_QUEUE"))
                                    refreshTrigger++
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category 2: Analytics & Data Input
                        PermissionCategoryGroup(
                            title = "ANALYTICS & DATA ENTRY",
                            accentColor = MotorOrange
                        ) {
                            PermissionModuleChip(
                                title = "Performance Insights",
                                icon = Icons.Default.Star,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "MONTHLY_PERFORMANCE"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "MONTHLY_PERFORMANCE", !viewModel.getMgmtPermission(username, "MONTHLY_PERFORMANCE"))
                                    refreshTrigger++
                                }
                            )
                            PermissionModuleChip(
                                title = "Data Input Center",
                                icon = Icons.Default.Edit,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "DATA_INPUT"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "DATA_INPUT", !viewModel.getMgmtPermission(username, "DATA_INPUT"))
                                    refreshTrigger++
                                }
                            )
                            PermissionModuleChip(
                                title = "Garage Traffic Status",
                                icon = Icons.Default.Info,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "GARAGE_TRAFFIC"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "GARAGE_TRAFFIC", !viewModel.getMgmtPermission(username, "GARAGE_TRAFFIC"))
                                    refreshTrigger++
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category 3: System & Security
                        PermissionCategoryGroup(
                            title = "SYSTEM & MANAGEMENT",
                            accentColor = Color(0xFF9C27B0)
                        ) {
                            PermissionModuleChip(
                                title = "Staff Management",
                                icon = Icons.Default.Person,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "STAFF_MANAGEMENT"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "STAFF_MANAGEMENT", !viewModel.getMgmtPermission(username, "STAFF_MANAGEMENT"))
                                    refreshTrigger++
                                }
                            )
                            PermissionModuleChip(
                                title = "Firebase Live Portal",
                                icon = Icons.Default.Refresh,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "FIREBASE_PORTAL"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "FIREBASE_PORTAL", !viewModel.getMgmtPermission(username, "FIREBASE_PORTAL"))
                                    refreshTrigger++
                                }
                            )
                            PermissionModuleChip(
                                title = "Access Control Settings",
                                icon = Icons.Default.Lock,
                                checked = if (isFullAccess) true else viewModel.getMgmtPermission(username, "ACCESS_CONTROL"),
                                isAlwaysOn = isFullAccess,
                                onToggle = {
                                    viewModel.setMgmtPermission(username, "ACCESS_CONTROL", !viewModel.getMgmtPermission(username, "ACCESS_CONTROL"))
                                    refreshTrigger++
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionCategoryGroup(
    title: String,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                letterSpacing = 0.5.sp
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            content()
        }
    }
}

@Composable
fun PermissionModuleChip(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    isAlwaysOn: Boolean = false,
    onToggle: () -> Unit
) {
    Card(
        onClick = { if (!isAlwaysOn) onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = if (checked) SurfaceDarkElevated else SurfaceDark
        ),
        border = BorderStroke(
            1.dp,
            if (checked) MotorOrange.copy(alpha = 0.6f) else BorderColor
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MotorOrange else TextDisabled,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal,
                    color = if (checked) TextPrimary else TextSecondary
                )
            }

            if (isAlwaysOn) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.DarkGray)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("ALWAYS ON", color = Color.LightGray, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier
                        .height(20.dp)
                        .padding(start = 6.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = MotorOrange,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = SurfaceDark
                    )
                )
            }
        }
    }
}
