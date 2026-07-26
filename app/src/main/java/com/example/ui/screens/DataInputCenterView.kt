package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.OilHistoryRecord
import com.example.data.ServiceWithPartsRecord
import com.example.data.ServiceWithoutPartsRecord
import com.example.data.BikeRiderMapping
import com.example.ui.MotorcycleViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Robust HTTP utility to fetch Google Sheet CSV asynchronously
suspend fun fetchCsvFromUrl(urlString: String): String = withContext(Dispatchers.IO) {
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 45000
    connection.readTimeout = 45000
    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)")
    
    val responseCode = connection.responseCode
    if (responseCode == HttpURLConnection.HTTP_OK) {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val stringBuilder = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = reader.readLine()
        }
        reader.close()
        stringBuilder.toString()
    } else {
        throw Exception("Server returned HTTP $responseCode")
    }
}

// Universal TSV/CSV string cells parser
fun parseCsvOrTsv(text: String): List<List<String>> {
    val lines = text.split("\n")
    val result = mutableListOf<List<String>>()
    for (line in lines) {
        if (line.trim().isBlank()) continue
        
        val cells = if (line.contains("\t")) {
            line.split("\t")
        } else {
            // CSV split by comma with quotes handling
            val tokens = mutableListOf<String>()
            val currentToken = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '"') {
                    inQuotes = !inQuotes
                } else if (c == ',' && !inQuotes) {
                    tokens.add(currentToken.toString().trim())
                    currentToken.setLength(0)
                } else {
                    currentToken.append(c)
                }
                i++
            }
            tokens.add(currentToken.toString().trim())
            tokens
        }
        
        val cleanedCells = cells.map { 
            var s = it.trim()
            if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
                s = s.substring(1, s.length - 1).trim()
            }
            s
        }
        result.add(cleanedCells)
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataInputCenterView(
    viewModel: MotorcycleViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 3 tabs: "PARTS" (Service with parts), "OIL" (Oil history), "NOPARTS" (Service without parts)
    var selectedTab by remember { mutableStateOf("PARTS") }
    var mainDataType by remember { mutableStateOf("SERVICE_HISTORY") } // "SERVICE_HISTORY" or "BIKE_RIDER"
    
    // Loaded database states
    val dbPartsRecords by viewModel.serviceWithPartsRecords.collectAsState()
    val dbOilRecords by viewModel.oilHistoryRecords.collectAsState()
    val dbNoPartsRecords by viewModel.serviceWithoutPartsRecords.collectAsState()
    val dbBikeRiderMappings by viewModel.bikeRiderMappings.collectAsState()

    // Interactive UI Spreadsheet rows (string-based matrix for direct screen editing)
    var gridRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    
    // Bulk paste and Google sheets integration states
    var bulkPasteText by remember { mutableStateOf("") }
    var googleSheetUrl by remember { mutableStateOf("") }
    var isSyncingSheet by remember { mutableStateOf(false) }
    var showPasteSection by remember { mutableStateOf(false) }
    var showSheetSection by remember { mutableStateOf(false) }
    
    // Inline Edit Dialog states
    var editingRowIndex by remember { mutableStateOf<Int?>(null) }
    var editingRowCells by remember { mutableStateOf<List<String>>(emptyList()) }

    // Column Definitions based on tab/mainDataType
    val columnHeadings = remember(mainDataType, selectedTab) {
        if (mainDataType == "BIKE_RIDER") {
            listOf("Bike Plate", "Rider ID", "Rider Name")
        } else {
            when (selectedTab) {
                "PARTS" -> listOf(
                    "Service type", "SKU#", "Vch No", "Vch Date", "Part No", 
                    "Item Name", "Voucher Type", "Garage", "Bike No", "Division", 
                    "Mechanic", "Quantity"
                )
                "OIL" -> listOf(
                    "Month", "Bike Number", "Kilometer", "next service", 
                    "Company", "remarks", " ", "MECHANIC"
                )
                else -> listOf(
                    "Date", "TALABAT ID", "BIKE #", "KM RUN", "JOB CARD", 
                    "JOB DONE", "Service type", "MECHANIC", "COMPANY"
                )
            }
        }
    }

    // Effect to reload grid rows when mainDataType, tab, or DB content changes
    LaunchedEffect(mainDataType, selectedTab, dbPartsRecords, dbOilRecords, dbNoPartsRecords, dbBikeRiderMappings) {
        gridRows = if (mainDataType == "SERVICE_HISTORY") {
            when (selectedTab) {
                "PARTS" -> dbPartsRecords.map { r ->
                    listOf(
                        r.serviceType, r.sku, r.vchNo, r.vchDate, r.partNo, 
                        r.itemName, r.voucherType, r.garage, r.bikeNo, r.division, 
                        r.mechanic, r.quantity.toString()
                    )
                }
                "OIL" -> dbOilRecords.map { r ->
                    listOf(
                        r.month, r.bikeNumber, r.kilometer.toString(), r.nextService, 
                        r.company, r.remarks, r.blankColumn, r.mechanic
                    )
                }
                else -> dbNoPartsRecords.map { r ->
                    listOf(
                        r.date, r.talabatId, r.bikeNo, r.kmRun.toString(), r.jobCard, 
                        r.jobDone, r.serviceType, r.mechanic, r.company
                    )
                }
            }
        } else {
            dbBikeRiderMappings.map { r ->
                listOf(r.bikePlate, r.riderId, r.riderName)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SlateDark)) {


        // Main Choice Selector TabRow (Service History vs Bike & Rider Mapping)
        TabRow(
            selectedTabIndex = if (mainDataType == "SERVICE_HISTORY") 0 else 1,
            containerColor = SurfaceDarkElevated,
            contentColor = MotorOrange,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (mainDataType == "SERVICE_HISTORY") 0 else 1]),
                    color = MotorOrange
                )
            }
        ) {
            Tab(
                selected = mainDataType == "SERVICE_HISTORY",
                onClick = { mainDataType = "SERVICE_HISTORY" },
                text = { Text("SERVICE HISTORY", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = mainDataType == "BIKE_RIDER",
                onClick = { mainDataType = "BIKE_RIDER" },
                text = { Text("BIKE & RIDER DATA", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        // Sub-tabs (only show for SERVICE_HISTORY)
        if (mainDataType == "SERVICE_HISTORY") {
            TabRow(
                selectedTabIndex = when (selectedTab) {
                    "PARTS" -> 0
                    "OIL" -> 1
                    else -> 2
                },
                containerColor = SurfaceDark,
                contentColor = MotorOrange,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[when (selectedTab) {
                            "PARTS" -> 0
                            "OIL" -> 1
                            else -> 2
                        }]),
                        color = MotorOrange
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == "PARTS",
                    onClick = { selectedTab = "PARTS" },
                    text = { Text("Service with Parts", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == "OIL",
                    onClick = { selectedTab = "OIL" },
                    text = { Text("Oil History", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                Tab(
                    selected = selectedTab == "NOPARTS",
                    onClick = { selectedTab = "NOPARTS" },
                    text = { Text("Service without Parts", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }

        if (mainDataType == "SERVICE_HISTORY" && selectedTab == "PARTS") {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, MotorOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚙️ Google Sheets Integration Setup",
                            color = MotorOrange,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "To search matching service history fast directly from your Google Sheet, paste your Google Apps Script Web App URL below:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val googleSheetUrlVal2025 by viewModel.googleSheetAppUrl2025.collectAsState()
                        val googleSheetUrlVal2026 by viewModel.googleSheetAppUrl2026.collectAsState()
                        var inputSheetUrl2025 by remember(googleSheetUrlVal2025) { mutableStateOf(googleSheetUrlVal2025) }
                        var inputSheetUrl2026 by remember(googleSheetUrlVal2026) { mutableStateOf(googleSheetUrlVal2026) }

                        OutlinedTextField(
                            value = inputSheetUrl2025,
                            onValueChange = { inputSheetUrl2025 = it },
                            label = { Text("Google Apps Script Web App URL - 2025 (Service with Parts)", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        OutlinedTextField(
                            value = inputSheetUrl2026,
                            onValueChange = { inputSheetUrl2026 = it },
                            label = { Text("Google Apps Script Web App URL - 2026 (Service with Parts)", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec?type=parts&bike_no=", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    inputSheetUrl2025 = ""
                                    inputSheetUrl2026 = ""
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Links", color = TextSecondary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val url2025 = inputSheetUrl2025.trim()
                                    val url2026 = inputSheetUrl2026.trim()
                                    if (url2025.isNotEmpty() && !url2025.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2025 Parts URL", Toast.LENGTH_SHORT).show()
                                    } else if (url2026.isNotEmpty() && !url2026.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2026 Parts URL", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateGoogleSheetAppUrl2025(url2025)
                                        viewModel.updateGoogleSheetAppUrl2026(url2026)
                                        Toast.makeText(context, "Google Sheet Web App URLs saved successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Save & Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Google Apps Script Setup Instructions:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Open your Google Sheet.\n" +
                                   "2. Click 'Extensions' -> 'Apps Script'.\n" +
                                   "3. Paste the doGet() function script that returns spreadsheet records matching the 'bike_number' query parameter as JSON.\n" +
                                   "4. Click 'Deploy' -> 'New deployment'. Select 'Web app', configure access to 'Anyone', and deploy.\n" +
                                   "5. Copy and paste the resulting Web App URL here.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else if (mainDataType == "SERVICE_HISTORY" && selectedTab == "OIL") {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, MotorOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚙️ Google Sheets Oil History Integration Setup",
                            color = MotorOrange,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Oil History data is fetched exclusively using a Google Sheet link. Paste your Google Apps Script Web App URL for Oil History below:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val googleSheetOilUrlVal2025 by viewModel.googleSheetOilUrl2025.collectAsState()
                        val googleSheetOilUrlVal2026 by viewModel.googleSheetOilUrl2026.collectAsState()
                        var inputSheetOilUrl2025 by remember(googleSheetOilUrlVal2025) { mutableStateOf(googleSheetOilUrlVal2025) }
                        var inputSheetOilUrl2026 by remember(googleSheetOilUrlVal2026) { mutableStateOf(googleSheetOilUrlVal2026) }

                        OutlinedTextField(
                            value = inputSheetOilUrl2025,
                            onValueChange = { inputSheetOilUrl2025 = it },
                            label = { Text("Google Apps Script Oil History URL - 2025", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("Empty", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        OutlinedTextField(
                            value = inputSheetOilUrl2026,
                            onValueChange = { inputSheetOilUrl2026 = it },
                            label = { Text("Google Apps Script Oil History URL - 2026", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    inputSheetOilUrl2025 = ""
                                    inputSheetOilUrl2026 = ""
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Links", color = TextSecondary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val url2025 = inputSheetOilUrl2025.trim()
                                    val url2026 = inputSheetOilUrl2026.trim()
                                    if (url2025.isNotEmpty() && !url2025.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2025 Oil URL", Toast.LENGTH_SHORT).show()
                                    } else if (url2026.isNotEmpty() && !url2026.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2026 Oil URL", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateGoogleSheetOilUrl2025(url2025)
                                        viewModel.updateGoogleSheetOilUrl2026(url2026)
                                        Toast.makeText(context, "Google Sheet Oil URLs saved successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Save & Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Google Apps Script Setup Instructions:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Open your Google Sheet.\n" +
                                   "2. Click 'Extensions' -> 'Apps Script'.\n" +
                                   "3. Paste the doGet() function script that returns spreadsheet records matching the 'bike_number' query parameter as JSON.\n" +
                                   "4. Click 'Deploy' -> 'New deployment'. Select 'Web app', configure access to 'Anyone', and deploy.\n" +
                                   "5. Copy and paste the resulting Web App URL here.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else if (mainDataType == "SERVICE_HISTORY" && selectedTab == "NOPARTS") {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, MotorOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚙️ Google Sheets Service Without Parts Integration Setup",
                            color = MotorOrange,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Service Without Parts data is fetched exclusively using a Google Sheet link. Paste your Google Apps Script Web App URL for Service Without Parts below:",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val googleSheetNoPartsUrlVal2025 by viewModel.googleSheetNoPartsUrl2025.collectAsState()
                        val googleSheetNoPartsUrlVal2026 by viewModel.googleSheetNoPartsUrl2026.collectAsState()
                        var inputSheetNoPartsUrl2025 by remember(googleSheetNoPartsUrlVal2025) { mutableStateOf(googleSheetNoPartsUrlVal2025) }
                        var inputSheetNoPartsUrl2026 by remember(googleSheetNoPartsUrlVal2026) { mutableStateOf(googleSheetNoPartsUrlVal2026) }

                        OutlinedTextField(
                            value = inputSheetNoPartsUrl2025,
                            onValueChange = { inputSheetNoPartsUrl2025 = it },
                            label = { Text("Google Apps Script Service Without Parts URL - 2025", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("Empty", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        OutlinedTextField(
                            value = inputSheetNoPartsUrl2026,
                            onValueChange = { inputSheetNoPartsUrl2026 = it },
                            label = { Text("Google Apps Script Service Without Parts URL - 2026", color = TextSecondary, fontSize = 11.sp) },
                            placeholder = { Text("https://script.google.com/macros/s/.../exec", color = TextDisabled, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            singleLine = true,
                            colors = customTextFieldColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    inputSheetNoPartsUrl2025 = ""
                                    inputSheetNoPartsUrl2026 = ""
                                },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Clear Links", color = TextSecondary, fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val url2025 = inputSheetNoPartsUrl2025.trim()
                                    val url2026 = inputSheetNoPartsUrl2026.trim()
                                    if (url2025.isNotEmpty() && !url2025.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2025 URL", Toast.LENGTH_SHORT).show()
                                    } else if (url2026.isNotEmpty() && !url2026.startsWith("http")) {
                                        Toast.makeText(context, "Please enter a valid HTTP/HTTPS 2026 URL", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.updateGoogleSheetNoPartsUrl2025(url2025)
                                        viewModel.updateGoogleSheetNoPartsUrl2026(url2026)
                                        Toast.makeText(context, "Google Sheet Service Without Parts URLs saved successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text("Save & Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Google Apps Script Setup Instructions:",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "1. Open your Google Sheet.\n" +
                                   "2. Click 'Extensions' -> 'Apps Script'.\n" +
                                   "3. Paste the doGet() function script that returns spreadsheet records matching the 'bike_no' query parameter as JSON.\n" +
                                   "4. Click 'Deploy' -> 'New deployment'. Select 'Web app', configure access to 'Anyone', and deploy.\n" +
                                   "5. Copy and paste the resulting Web App URL here.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Integration options header card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "IMPORT AND INTEGRATION SERVICES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { 
                                    showPasteSection = !showPasteSection
                                    showSheetSection = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showPasteSection) MotorOrange else SurfaceDarkElevated
                                ),
                                border = BorderStroke(1.dp, if (showPasteSection) MotorOrange else BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp),
                                    tint = if (showPasteSection) SlateDark else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bulk Paste", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (showPasteSection) SlateDark else TextPrimary
                                )
                            }

                            Button(
                                onClick = { 
                                    showSheetSection = !showSheetSection
                                    showPasteSection = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showSheetSection) MotorOrange else SurfaceDarkElevated
                                ),
                                border = BorderStroke(1.dp, if (showSheetSection) MotorOrange else BorderColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp),
                                    tint = if (showSheetSection) SlateDark else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google Sheets", 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = if (showSheetSection) SlateDark else TextPrimary
                                )
                            }
                        }

                        // Bulk Paste Expandable Block
                        AnimatedVisibility(visible = showPasteSection) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    text = "Copy cell ranges from Google Sheets or Excel (Ctrl+C) and paste below:",
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = bulkPasteText,
                                    onValueChange = { bulkPasteText = it },
                                    placeholder = { Text("Paste cells here (tab or comma separated values)", color = TextDisabled) },
                                    modifier = Modifier.fillMaxWidth().height(120.dp),
                                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                    colors = customTextFieldColors()
                                )
                                
                                if (bulkPasteText.isNotBlank()) {
                                    val previewRows = try {
                                        parseCsvOrTsv(bulkPasteText).take(4)
                                    } catch (e: Exception) {
                                        emptyList()
                                    }
                                    
                                    if (previewRows.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "📊 LIVE EXCEL COLUMNS ALIGNMENT DETECTOR:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MotorOrange,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        
                                        // Scrollable horizontal excel preview
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                                .background(SurfaceDarkElevated)
                                                .horizontalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            Column {
                                                // Preview Headers Row
                                                Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                                    columnHeadings.forEachIndexed { colIndex, heading ->
                                                        val letter = ('A' + colIndex).toString()
                                                        Column(
                                                            modifier = Modifier
                                                                .width(100.dp)
                                                                .padding(horizontal = 4.dp),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Text(
                                                                text = "Col $letter",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MotorOrange
                                                            )
                                                            Text(
                                                                text = heading,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = TextPrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                                HorizontalDivider(color = BorderColor)
                                                
                                                // Preview Cells Row
                                                previewRows.forEachIndexed { rowIndex, cells ->
                                                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                                        columnHeadings.forEachIndexed { colIndex, _ ->
                                                            val cellValue = cells.getOrNull(colIndex) ?: ""
                                                            Text(
                                                                text = cellValue.ifEmpty { "—" },
                                                                fontSize = 10.sp,
                                                                color = if (cellValue.isEmpty()) TextDisabled else TextSecondary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier
                                                                    .width(100.dp)
                                                                    .padding(horizontal = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                if (previewRows.size >= 4) {
                                                    Text(
                                                        text = "... and more rows parsed correctly ...",
                                                        fontSize = 9.sp,
                                                        color = TextDisabled,
                                                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { bulkPasteText = "" },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text("Clear", color = TextSecondary)
                                    }
                                    Button(
                                        onClick = {
                                            if (bulkPasteText.isBlank()) {
                                                Toast.makeText(context, "Paste contents are empty", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val parsed = parseCsvOrTsv(bulkPasteText)
                                            if (parsed.isEmpty()) {
                                                Toast.makeText(context, "Could not parse any rows", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            
                                            // Validate and pad columns
                                            val requiredCount = columnHeadings.size
                                            val validRows = parsed.filter { it.isNotEmpty() && it.any { cell -> cell.isNotBlank() } }
                                            
                                            if (validRows.isEmpty()) {
                                                Toast.makeText(context, "No valid rows found in pasted data.", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }
                                            
                                            gridRows = gridRows + validRows.map { row ->
                                                if (row.size >= requiredCount) {
                                                    row.take(requiredCount)
                                                } else {
                                                    row + List(requiredCount - row.size) { "" }
                                                }
                                            }
                                            bulkPasteText = ""
                                            showPasteSection = false
                                            Toast.makeText(context, "Pasted ${validRows.size} rows successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PARSE & ADD TO GRID", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Google Sheets CSV Expandable Block
                        AnimatedVisibility(visible = showSheetSection) {
                            Column(modifier = Modifier.padding(top = 14.dp)) {
                                Text(
                                    text = "1. In Google Sheets, click File > Share > Publish to web\n2. Select Entire Document, format as Comma-separated values (.csv)\n3. Paste the generated link below to automatically import:",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                OutlinedTextField(
                                    value = googleSheetUrl,
                                    onValueChange = { googleSheetUrl = it },
                                    placeholder = { Text("https://docs.google.com/spreadsheets/d/.../pub?output=csv", color = TextDisabled) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = customTextFieldColors(),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        if (googleSheetUrl.isBlank() || !googleSheetUrl.startsWith("http")) {
                                            Toast.makeText(context, "Please enter a valid HTTP URL", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isSyncingSheet = true
                                        scope.launch {
                                            try {
                                                val content = fetchCsvFromUrl(googleSheetUrl)
                                                val parsed = parseCsvOrTsv(content)
                                                val requiredCount = columnHeadings.size
                                                
                                                // Filter out headers if they match column names exactly
                                                val dataRows = parsed.filter { row ->
                                                    !(row.isNotEmpty() && (row[0].contains("Service type", true) || row[0].contains("Month", true) || row[0].contains("Date", true)))
                                                }
                                                
                                                val validRows = dataRows.filter { it.isNotEmpty() && it.any { cell -> cell.isNotBlank() } }
                                                
                                                if (validRows.isEmpty()) {
                                                    throw Exception("No valid rows found in the sheet data.")
                                                }
                                                
                                                gridRows = validRows.map { row ->
                                                    if (row.size >= requiredCount) {
                                                        row.take(requiredCount)
                                                    } else {
                                                        row + List(requiredCount - row.size) { "" }
                                                    }
                                                }
                                                isSyncingSheet = false
                                                showSheetSection = false
                                                Toast.makeText(context, "Synced ${validRows.size} records successfully!", Toast.LENGTH_LONG).show()
                                            } catch (e: Exception) {
                                                isSyncingSheet = false
                                                Toast.makeText(context, "Sync Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSyncingSheet
                                ) {
                                    if (isSyncingSheet) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SlateDark, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ESTABLISHING CHANNEL...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("FETCH & SYNC SHEET NOW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Spreadsheet Actions and Row count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (gridRows.isNotEmpty()) MotorOrange else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spreadsheet Matrix: ${gridRows.size} rows",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = {
                                // Add default empty row
                                val newRow = List(columnHeadings.size) { "" }
                                gridRows = gridRows + listOf(newRow)
                                editingRowIndex = gridRows.size - 1
                                editingRowCells = newRow
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceDarkElevated)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Row", tint = MotorOrange, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                gridRows = emptyList()
                                Toast.makeText(context, "Grid cleared. Hit Save to empty database.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SurfaceDarkElevated)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Grid", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // SpreadSheet Table
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Double scrollable box: vertical and horizontal scrollable table!
                    Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        Column {
                            // Table Letters Row
                            Row(
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Excel ID",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDisabled,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                                )
                                columnHeadings.forEachIndexed { colIndex, _ ->
                                    val letter = ('A' + colIndex).toString()
                                    Text(
                                        text = letter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MotorOrange,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(110.dp).padding(horizontal = 6.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                            // Table Header Row
                            Row(
                                modifier = Modifier
                                    .background(SurfaceDarkElevated)
                                    .padding(vertical = 10.dp)
                            ) {
                                // Empty cell for action column
                                Text(
                                    text = "Action",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MotorOrange,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp)
                                )
                                columnHeadings.forEach { header ->
                                    Text(
                                        text = header,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        modifier = Modifier.width(110.dp).padding(horizontal = 6.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = BorderColor)

                            if (gridRows.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp).width((columnHeadings.size * 110).dp + 70.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.List, contentDescription = null, tint = TextDisabled, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No rows inside grid. Click + or Paste data.", fontSize = 12.sp, color = TextSecondary)
                                    }
                                }
                            } else {
                                gridRows.forEachIndexed { rowIndex, cells ->
                                    Row(
                                        modifier = Modifier
                                            .background(if (rowIndex % 2 == 0) SlateDark else SurfaceDark)
                                            .clickable {
                                                editingRowIndex = rowIndex
                                                editingRowCells = cells
                                            }
                                            .padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Delete and Edit buttons column
                                        Row(
                                            modifier = Modifier.width(70.dp).padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${rowIndex + 1}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextDisabled,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        gridRows = gridRows.filterIndexed { index, _ -> index != rowIndex }
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete Row", tint = Color.Red, modifier = Modifier.size(12.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        editingRowIndex = rowIndex
                                                        editingRowCells = cells
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Row", tint = MotorOrange, modifier = Modifier.size(12.dp))
                                                }
                                            }
                                        }

                                        cells.forEachIndexed { _, cellValue ->
                                            Text(
                                                text = cellValue.ifEmpty { "—" },
                                                fontSize = 12.sp,
                                                color = if (cellValue.isEmpty()) TextDisabled else TextPrimary,
                                                modifier = Modifier.width(110.dp).padding(horizontal = 6.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }

            // Save to database button
            item {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                if (mainDataType == "BIKE_RIDER") {
                                    viewModel.clearBikeRiderMappings()
                                    val records = gridRows.mapNotNull { row ->
                                        if (row.size < 3) return@mapNotNull null
                                        BikeRiderMapping(
                                            bikePlate = row[0].uppercase().trim(),
                                            riderId = row[1].trim(),
                                            riderName = row[2].trim()
                                        )
                                    }
                                    viewModel.addBikeRiderMappings(records)
                                } else {
                                    when (selectedTab) {
                                        "PARTS" -> {
                                            viewModel.clearServiceWithPartsRecords()
                                            val records = gridRows.mapNotNull { row ->
                                                if (row.size < 12) return@mapNotNull null
                                                ServiceWithPartsRecord(
                                                    serviceType = row[0],
                                                    sku = row[1],
                                                    vchNo = row[2],
                                                    vchDate = row[3],
                                                    partNo = row[4],
                                                    itemName = row[5],
                                                    voucherType = row[6],
                                                    garage = row[7],
                                                    bikeNo = row[8],
                                                    division = row[9],
                                                    mechanic = row[10],
                                                    quantity = row[11].toDoubleOrNull() ?: 0.0
                                                )
                                            }
                                            viewModel.addServiceWithPartsRecords(records)
                                        }
                                        "OIL" -> {
                                            viewModel.clearOilHistoryRecords()
                                            val records = gridRows.mapNotNull { row ->
                                                if (row.size < 8) return@mapNotNull null
                                                OilHistoryRecord(
                                                    month = row[0],
                                                    bikeNumber = row[1],
                                                    kilometer = row[2].toIntOrNull() ?: 0,
                                                    nextService = row[3],
                                                    company = row[4],
                                                    remarks = row[5],
                                                    blankColumn = row[6],
                                                    mechanic = row[7]
                                                )
                                            }
                                            viewModel.addOilHistoryRecords(records)
                                        }
                                        "NOPARTS" -> {
                                            viewModel.clearServiceWithoutPartsRecords()
                                            val records = gridRows.mapNotNull { row ->
                                                if (row.size < 9) return@mapNotNull null
                                                ServiceWithoutPartsRecord(
                                                    date = row[0],
                                                    talabatId = row[1],
                                                    bikeNo = row[2],
                                                    kmRun = row[3].toIntOrNull() ?: 0,
                                                    jobCard = row[4],
                                                    jobDone = row[5],
                                                    serviceType = row[6],
                                                    mechanic = row[7],
                                                    company = row[8]
                                                )
                                            }
                                            viewModel.addServiceWithoutPartsRecords(records)
                                        }
                                    }
                                }
                                Toast.makeText(context, "💾 Saved ${gridRows.size} records to local database!", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SAVE TO SECURE LOCAL DATABASE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
        }
    }

    // Modal Edit Dialog for Row Cells
    if (editingRowIndex != null) {
        Dialog(onDismissRequest = { editingRowIndex = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Edit Row: #${editingRowIndex!! + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MotorOrange
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Box(modifier = Modifier.weight(1f, fill = false).heightIn(max = 400.dp)) {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            columnHeadings.forEachIndexed { colIndex, heading ->
                                val cellVal = editingRowCells.getOrElse(colIndex) { "" }
                                OutlinedTextField(
                                    value = cellVal,
                                    onValueChange = { newValue ->
                                        val mutable = editingRowCells.toMutableList()
                                        while (mutable.size <= colIndex) mutable.add("")
                                        mutable[colIndex] = newValue
                                        editingRowCells = mutable
                                    },
                                    label = { Text(heading, color = TextSecondary) },
                                    colors = customTextFieldColors(),
                                    singleLine = true,
                                    keyboardOptions = if (
                                        (selectedTab == "PARTS" && colIndex == 11) ||
                                        (selectedTab == "OIL" && colIndex == 2) ||
                                        (selectedTab == "NOPARTS" && colIndex == 3)
                                    ) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { editingRowIndex = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                val mutableRows = gridRows.toMutableList()
                                val reqSize = columnHeadings.size
                                val normalizedCells = editingRowCells.toMutableList()
                                while (normalizedCells.size < reqSize) normalizedCells.add("")
                                mutableRows[editingRowIndex!!] = normalizedCells.take(reqSize)
                                gridRows = mutableRows
                                editingRowIndex = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MotorOrange),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("UPDATE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

