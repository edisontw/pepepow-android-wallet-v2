package net.pepepow.wallet.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.pepepow.wallet.viewmodel.ConsolidationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolidateScreen(
    viewModel: ConsolidationViewModel,
    onNavigateBack: () -> Unit
) {
    val address by viewModel.address.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val utxoCountState by viewModel.utxoCount.collectAsState()
    val mnemonicStr by viewModel.mnemonic.collectAsState()
    
    val isConsolidating by viewModel.isConsolidating.collectAsState()
    val apiMessage by viewModel.apiMessage.collectAsState()
    val lastTxid by viewModel.lastTxid.collectAsState()

    // Auto Mode States
    val isAutoMode by viewModel.isAutoMode.collectAsState()
    val autoCompletedRounds by viewModel.autoCompletedRounds.collectAsState()
    val autoState by viewModel.autoState.collectAsState()
    val autoStatusText by viewModel.autoStatusText.collectAsState()
    val canResume by viewModel.canResume.collectAsState()

    // Preferences & Estimates
    var selectedPreset by remember { mutableStateOf(40) }
    var maxRoundsStr by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val isKeysAvailable = !mnemonicStr.isNullOrBlank()
    val isApiAvailable = utxoCountState != null && utxoCountState != -1
    val isUtxoCountSufficient = utxoCountState != null && utxoCountState!! >= 2
    val isEligible = isKeysAvailable && isApiAvailable && isUtxoCountSufficient

    // Calculate actual estimates based on selectedPreset & current UTXOs
    val selectedInputCount = if (isEligible) {
        minOf(selectedPreset, utxoCountState ?: selectedPreset)
    } else {
        0
    }
    val estSize = 10 + selectedInputCount * 148 + 34
    val feeSat = ((estSize + 999) / 1000) * 100_000L
    val estFeeDouble = feeSat / 100_000_000.0

    // Trigger check on startup
    LaunchedEffect(Unit) {
        viewModel.checkResumeProgress()
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm UTXO Consolidation", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to proceed with UTXO consolidation?")
                    Text("• This creates a real transaction on-chain.", fontSize = 13.sp)
                    Text("• Estimated network fee: ${String.format("%.6f", estFeeDouble)} PEPEW", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("• Selected inputs: up to $selectedInputCount UTXOs.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.startManualConsolidation(selectedPreset)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consolidate UTXOs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PepepowBackground)
            )
        },
        containerColor = PepepowBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Summary Card
            Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Address", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        val displayAddress = if (address.length > 20) {
                            address.take(10) + "..." + address.takeLast(8)
                        } else {
                            address.ifBlank { "No address available" }
                        }
                        Text(
                            text = displayAddress,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confirmed Balance", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            "${String.format("%.4f", balance)} PEPEW",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Eligible UTXOs", fontSize = 13.sp, color = Color.Gray)
                        val utxoText = when (utxoCountState) {
                            null -> "Loading..."
                            -1 -> "Unavailable"
                            else -> utxoCountState.toString()
                        }
                        Text(
                            text = utxoText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Selected Limit", fontSize = 13.sp, color = Color.Gray)
                        Text(
                            "$selectedPreset UTXOs",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Warnings Banner
            Card(
                colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Consolidation creates an on-chain transaction and uses a network fee.\nKeys stay on this device. PEPEW Light only broadcasts the signed transaction.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            }

            // Resume Prompt
            if (canResume && !isAutoMode && !isConsolidating) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.History, null, tint = Color(0xFFE65100))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recent Auto Consolidation Interrupted", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
                        }
                        Text("A recent automatic consolidation session was found. You can resume it now.", fontSize = 12.sp, color = Color(0xFFE65100))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.resumeAutoConsolidation() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resume")
                            }
                            OutlinedButton(
                                onClick = { viewModel.cancelAutoConsolidation() },
                                border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", color = Color(0xFFE65100))
                            }
                        }
                    }
                }
            }

            // B. Preset Buttons (only show when not running)
            if (!isAutoMode && !isConsolidating) {
                Text("Select Presets Limit for Consolidation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PepepowPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(40, 80, 120, 150)
                    presets.forEach { preset ->
                        val selected = selectedPreset == preset
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) PepepowPrimary else PepepowSurface
                            ),
                            border = BorderStroke(1.dp, if (selected) PepepowPrimary else Color.LightGray.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedPreset = preset }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    preset.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // Auto mode configuration input (only show when not running)
            if (!isAutoMode && !isConsolidating) {
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Auto Multi-Round Options", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PepepowPrimary)
                        
                        OutlinedTextField(
                            value = maxRoundsStr,
                            onValueChange = { maxRoundsStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Max Rounds Limit") },
                            placeholder = { Text("1 (Default)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        Text(
                            text = "Use 1–2 rounds for testing. Leaving it blank defaults to 1 round.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Active Progress Banner
            val showProgressBanner = isConsolidating || isAutoMode || autoState == "paused"
            if (showProgressBanner) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    border = BorderStroke(1.dp, PepepowPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val bannerTitle = when {
                            isConsolidating -> "Manual Consolidation Running"
                            autoState == "paused" -> "Auto Consolidation Paused"
                            else -> "Auto Consolidation Running"
                        }

                        Text(
                            text = bannerTitle,
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontSize = 15.sp
                        )

                        val showSpinner = isConsolidating || (isAutoMode && autoState != "paused")
                        if (showSpinner) {
                            CircularProgressIndicator(color = PepepowPrimary)
                        }

                        if (isAutoMode || autoState == "paused") {
                            Text("Rounds Completed: $autoCompletedRounds", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Phase: $autoState", fontWeight = FontWeight.Medium, color = Color.Gray, fontSize = 13.sp)
                        }

                        Text(
                            text = autoStatusText.ifBlank { apiMessage ?: "Processing..." },
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )

                        if (isAutoMode || autoState == "paused") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isAutoMode) {
                                    Button(
                                        onClick = { viewModel.pauseAutoConsolidation() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Pause")
                                    }
                                } else if (autoState == "paused") {
                                    Button(
                                        onClick = { viewModel.resumeAutoConsolidation() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Resume")
                                    }
                                }
                                Button(
                                    onClick = { viewModel.cancelAutoConsolidation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // Compact Completed / Disabled warnings
            if (!isConsolidating && !isAutoMode) {
                when {
                    utxoCountState != null && utxoCountState != -1 && utxoCountState!! < 2 -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            border = BorderStroke(1.dp, PepepowPrimary)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = PepepowPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "No consolidation needed. Your wallet has fewer than 2 eligible UTXOs.",
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                    !isKeysAvailable -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFEF5350))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Local signing is unavailable. Please back up or import your wallet recovery phrase first.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                    utxoCountState == -1 -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            border = BorderStroke(1.dp, Color(0xFFEF5350))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CloudOff, null, tint = Color(0xFFC62828))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "UTXO lookup unavailable. Please check your network connection or API status.",
                                    fontSize = 13.sp,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }

            // C & D. Estimates & Triggers Card (only visible if eligible UTXOs >= 2 and signing key/API available)
            if (isEligible && !isConsolidating && !isAutoMode) {
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Consolidation Estimation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PepepowPrimary)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Inputs Selected", fontSize = 13.sp, color = Color.Gray)
                            Text("$selectedInputCount / $utxoCountState UTXOs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Transaction Size", fontSize = 13.sp, color = Color.Gray)
                            Text("$estSize Bytes", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Est. Network Fee", fontSize = 13.sp, color = Color.Gray)
                            Text("${String.format("%.6f", estFeeDouble)} PEPEW", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Divider(color = Color.LightGray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { showConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("CONSOLIDATE MANUALLY", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedButton(
                            onClick = {
                                val limit = maxRoundsStr.toIntOrNull() ?: 1
                                viewModel.startAutoConsolidation(selectedPreset, limit)
                            },
                            border = BorderStroke(1.2.dp, PepepowPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PepepowPrimary),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("RUN AUTO CONSOLIDATION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // E. Status Card (always visible to show outcome/errors)
            if (!isConsolidating && !isAutoMode && (apiMessage != null || !lastTxid.isNullOrBlank())) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = PepepowPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Status Update", fontWeight = FontWeight.Bold, color = PepepowPrimary, fontSize = 14.sp)
                        }
                        if (apiMessage != null) {
                            Text(apiMessage ?: "", fontSize = 13.sp, color = Color.DarkGray)
                        }
                        if (!lastTxid.isNullOrBlank()) {
                            Text("Last TXID: $lastTxid", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
