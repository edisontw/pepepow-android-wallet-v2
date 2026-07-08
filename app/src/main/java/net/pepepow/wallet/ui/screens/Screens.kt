package net.pepepow.wallet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import net.pepepow.wallet.data.ApiState
import net.pepepow.wallet.data.Transaction
import net.pepepow.wallet.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

// Custom Pepepow Theme Green colors
val PepepowPrimary = Color(0xFF2E7D32) // Froggy Green
val PepepowSecondary = Color(0xFFC8E6C9)
val PepepowBackground = Color(0xFFF1F8E9)
val PepepowSurface = Color(0xFFFFFFFF)

private fun formatPepewAmount(value: Double, scale: Int = 4): String {
    return try {
        java.math.BigDecimal(value.toString())
            .setScale(scale, java.math.RoundingMode.HALF_UP)
            .toPlainString()
    } catch (e: Exception) {
        String.format(java.util.Locale.US, "%.${scale}f", value)
    }
}

private fun formatUsdtValue(usdtValue: Double): String {
    if (usdtValue >= 0.01) {
        return "%,.2f USDT".format(usdtValue)
    }
    if (usdtValue <= 0.0) {
        return "0.00 USDT"
    }
    var temp = usdtValue
    var leadingZeros = 0
    while (temp < 0.1 && leadingZeros < 10) {
        temp *= 10.0
        leadingZeros++
    }
    val decimals = (leadingZeros + 3).coerceIn(2, 8)
    val formatted = String.format(java.util.Locale.US, "%.${decimals}f", usdtValue)
    var trimmed = formatted
    while (trimmed.endsWith("0") && trimmed.substringAfter(".").length > 2) {
        trimmed = trimmed.dropLast(1)
    }
    return "~ $trimmed USDT"
}

private fun openAddressInExplorer(context: Context, address: String) {
    if (address.isBlank()) return
    val intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://explorer.pepepow.net/address/$address")
    )
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onNavigateToCreate: () -> Unit) {
    Scaffold(
        containerColor = PepepowBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizer,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Large Froggy Icon Mock
                Surface(
                    color = PepepowPrimary,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🐸", fontSize = 48.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "PEPEW WALLET",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "The premier non-custodial wallet for PEPEW. Speed. Security. Froggy Power.",
                    fontSize = 15.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security",
                                tint = PepepowPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Non-Custodial & Secure",
                                fontWeight = FontWeight.SemiBold,
                                color = PepepowPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your keys never leave your device. We do not store your recovery phrase, and we cannot access your funds.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("CREATE NEW WALLET", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWalletScreen(
    viewModel: WalletViewModel,
    onNavigateToBackupWarning: () -> Unit
) {
    val mnemonic by viewModel.mnemonic.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generate Seed Phrase", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PepepowBackground)
            )
        },
        containerColor = PepepowBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Write Down Your Mnemonic",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your mnemonic is a series of 12 words that back up your entire wallet. Write it down on paper and keep it in a secure place.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Mnemonic Word Box Grid
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        mnemonic?.let { mStr ->
                            val words = mStr.split(" ")
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 3,
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                words.forEachIndexed { idx, word ->
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp, vertical = 4.dp)
                                            .background(
                                                color = PepepowBackground,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                PepepowSecondary,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${idx + 1}. $word",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Medium,
                                            color = PepepowPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "You can view this mnemonic phrase again later in Settings.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                mnemonic?.let { mStr ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Mnemonic", mStr)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Mnemonic copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            border = BorderStroke(1.dp, PepepowPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PepepowPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Text("Copy mnemonic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Copy Warning Box
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Never take a screenshot or share this seed phrase. Anyone with these 12 words can access all your PEPEW.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onNavigateToBackupWarning,
                colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("I HAVE WRITTEN IT DOWN", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BackupWarningScreen(
    viewModel: WalletViewModel,
    onNavigateToDashboard: () -> Unit
) {
    var check1 by remember { mutableStateOf(false) }
    var check2 by remember { mutableStateOf(false) }
    var check3 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup Confirmation", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PepepowBackground)
            )
        },
        containerColor = PepepowBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Are you absolutely sure?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Confirming these safety practices is required to protect your non-custodial PEPEW wallet assets.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Checkbox 1
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PepepowSurface, RoundedCornerShape(12.dp))
                            .clickable { check1 = !check1 }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = check1,
                            onCheckedChange = { check1 = it },
                            colors = CheckboxDefaults.colors(checkedColor = PepepowPrimary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "I understand that my keys stay local on this device. No external servers store them.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }

                    // Checkbox 2
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PepepowSurface, RoundedCornerShape(12.dp))
                            .clickable { check2 = !check2 }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = check2,
                            onCheckedChange = { check2 = it },
                            colors = CheckboxDefaults.colors(checkedColor = PepepowPrimary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "I confirm that I have written down my mnemonic phrase safely on a physical medium.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }

                    // Checkbox 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PepepowSurface, RoundedCornerShape(12.dp))
                            .clickable { check3 = !check3 }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = check3,
                            onCheckedChange = { check3 = it },
                            colors = CheckboxDefaults.colors(checkedColor = PepepowPrimary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "I understand that PEPEW Support cannot recover my funds if I lose this phrase.",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Button(
                onClick = onNavigateToDashboard,
                enabled = check1 && check2 && check3,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PepepowPrimary,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("ENTER PEPEW WALLET", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    walletViewModel: WalletViewModel,
    historyViewModel: HistoryViewModel,
    apiStatusViewModel: ApiStatusViewModel,
    onNavigateToSend: () -> Unit,
    onNavigateToReceive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToApiStatus: () -> Unit
) {
    val balance by walletViewModel.balance.collectAsState()
    val address by walletViewModel.address.collectAsState()
    val apiState by apiStatusViewModel.apiState.collectAsState()
    val transactions by historyViewModel.transactions.collectAsState()
    val usdPrice by walletViewModel.usdPrice.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        walletViewModel.refreshWalletData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = net.pepepow.wallet.R.drawable.pepew_logo),
                            contentDescription = "PEPEW Logo",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PEPEW", fontWeight = FontWeight.Bold, color = PepepowPrimary)
                    }
                },
                actions = {
                    val isApiLoading by walletViewModel.isApiLoading.collectAsState()
                    IconButton(
                        onClick = { walletViewModel.refreshWalletData() },
                        enabled = !isApiLoading,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isApiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PepepowPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToApiStatus,
                        modifier = Modifier.size(48.dp)
                    ) {
                        val color = when (apiState) {
                            ApiState.READY, ApiState.CONNECTED -> Color(0xFF4CAF50)
                            ApiState.FAILED -> Color(0xFFF44336)
                        }
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "API Status",
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
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
        ) {
            val apiMessage by walletViewModel.apiMessage.collectAsState()
            if (apiState == ApiState.FAILED) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Unable to refresh balance",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp
                            )
                            Text(
                                text = apiMessage,
                                color = Color(0xFFC62828),
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Balance Card
            Card(
                colors = CardDefaults.cardColors(containerColor = PepepowPrimary),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Total Balance",
                        color = PepepowSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${formatPepewAmount(balance, 4)} PEPEW",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val usdtText = if (usdPrice != null) {
                        val usdtValue = balance * usdPrice!!
                        formatUsdtValue(usdtValue)
                    } else {
                        "~ price unavailable"
                    }
                    Text(
                        text = usdtText,
                        color = PepepowSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .clickable {
                                    if (address.isNotEmpty()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("PEPEW Address", address)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Address",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (address.length > 20) address.take(10) + "..." + address.takeLast(8) else address,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        OutlinedButton(
                            onClick = { openAddressInExplorer(context, address) },
                            enabled = address.isNotBlank(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Explorer", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Quick Actions Button Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    imageVector = Icons.Default.ArrowOutward,
                    label = "Send",
                    onClick = onNavigateToSend
                )
                ActionButton(
                    imageVector = Icons.Default.QrCode,
                    label = "Receive",
                    onClick = onNavigateToReceive
                )
                ActionButton(
                    imageVector = Icons.Default.History,
                    label = "History",
                    onClick = onNavigateToHistory
                )
            }

            // Recent Transactions Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "View All",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = PepepowPrimary,
                    modifier = Modifier.clickable { onNavigateToHistory() }
                )
            }

            // Transaction list
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No transactions yet",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions.take(4)) { tx ->
                        TxItem(tx = tx)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val isApiMode by walletViewModel.isApiMode.collectAsState()
            val modeText = if (isApiMode) "Read-only API mode" else "Prototype mode: mock wallet data only"
            Text(
                text = modeText,
                color = Color.Gray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ActionButton(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 72.dp).clickable { onClick() }
    ) {
        Surface(
            color = PepepowSurface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.size(60.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = label,
                    tint = PepepowPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun TxItem(tx: Transaction) {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(tx.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = PepepowSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val surfaceColor = if (tx.isSend || tx.isSelfTransfer) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                val iconTint = if (tx.isSend || tx.isSelfTransfer) Color(0xFFC62828) else Color(0xFF2E7D32)
                val iconVector = if (tx.isSend || tx.isSelfTransfer) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                Surface(
                    color = surfaceColor,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = when {
                            tx.isSelfTransfer -> "Self Send"
                            tx.isSend -> "Sent PEPEW"
                            else -> "Received PEPEW"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    val subtitle = when {
                        tx.isSelfTransfer -> if (tx.isPending) "Pending • Fee only" else "Sent to self / fee only"
                        tx.isPending -> "Pending..."
                        else -> dateStr
                    }
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (tx.isPending && !tx.isSelfTransfer) PepepowPrimary else Color.Gray,
                        fontWeight = if (tx.isPending && !tx.isSelfTransfer) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountText = if (tx.isSelfTransfer) {
                    "- ${formatPepewAmount(tx.amount, 4)} PEPEW"
                } else {
                    "${if (tx.isSend) "-" else "+"} ${formatPepewAmount(tx.amount, 4)} PEPEW"
                }
                Text(
                    text = amountText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (tx.isSend || tx.isSelfTransfer) Color.Black else PepepowPrimary,
                    fontFamily = FontFamily.Monospace
                )
                val displayAddress = if (tx.address.length > 18) {
                    tx.address.take(12) + "..." + tx.address.takeLast(6)
                } else {
                    tx.address
                }
                Text(
                    text = displayAddress,
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

fun generateQrCode(text: String, size: Int): Bitmap? {
    if (text.isEmpty()) return null
    return try {
        val bitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    walletViewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val address by walletViewModel.address.collectAsState()
    val context = LocalContext.current
    val qrBitmap = remember(address) {
        if (address.isNotEmpty()) {
            generateQrCode(address, 512)
        } else {
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receive PEPEW", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your PEPEW Address",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Real QR Code Box
                Surface(
                    color = PepepowSurface,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(180.dp)
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "QR Code Placeholder",
                                    tint = Color.Black,
                                    modifier = Modifier.size(160.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Address Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = address,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (address.isNotEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("PEPEW Address", address)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Address copied", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PepepowBackground),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = PepepowPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COPY ADDRESS", color = PepepowPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Disclaimer Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Disclaimer",
                        tint = PepepowPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Only send PEPEW to this address. Sending any other cryptocurrency will result in permanent loss.",
                        fontSize = 11.sp,
                        color = PepepowPrimary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    walletViewModel: WalletViewModel,
    sendViewModel: SendViewModel,
    onNavigateBack: () -> Unit
) {
    var recipientAddress by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    val balance by walletViewModel.balance.collectAsState()
    val addressError by sendViewModel.addressError.collectAsState()
    val amountError by sendViewModel.amountError.collectAsState()
    val sendSuccess by sendViewModel.sendSuccess.collectAsState()

    val isSending by sendViewModel.isSending.collectAsState()
    val sendProgress by sendViewModel.sendProgress.collectAsState()

    val addressValidation = if (recipientAddress.isBlank()) null else net.pepepow.wallet.domain.address.AddressValidator.validateAddress(recipientAddress)
    val amountDouble = amountStr.toDoubleOrNull()
    
    val isAddressOk = addressValidation == net.pepepow.wallet.domain.address.AddressValidator.AddressValidationResult.ValidP2PKH
    val isAmountOk = amountDouble != null && amountDouble > 0 && (amountDouble + 0.001) <= balance
    val sendButtonEnabled = isAddressOk && isAmountOk && !isSending

    val disabledReason = when {
        recipientAddress.isBlank() -> "Enter recipient address"
        addressValidation == net.pepepow.wallet.domain.address.AddressValidator.AddressValidationResult.InvalidAddress -> "Invalid PEPEPOW address"
        addressValidation == net.pepepow.wallet.domain.address.AddressValidator.AddressValidationResult.UnsupportedAddressType -> "Unsupported address type (P2SH not supported)"
        amountStr.isBlank() -> "Enter amount"
        amountDouble == null || amountDouble <= 0 -> "Enter valid amount (> 0)"
        (amountDouble + 0.001) > balance -> "Insufficient balance (need amount + 0.001 fee)"
        else -> null
    }

    LaunchedEffect(sendSuccess) {
        if (sendSuccess == true) {
            sendViewModel.resetSendState()
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        sendViewModel.resetSendState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send PEPEW", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isSending) {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Available Balance label
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Available Balance:", color = Color.Gray, fontSize = 13.sp)
                        Text(
                            "${formatPepewAmount(balance, 4)} PEPEW",
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Recipient Address Input
                OutlinedTextField(
                    value = recipientAddress,
                    onValueChange = { recipientAddress = it },
                    label = { Text("Recipient PEPEW Address") },
                    placeholder = { Text("P...") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending,
                    isError = addressError != null,
                    supportingText = {
                        if (addressError != null) {
                            Text(addressError!!, color = Color.Red)
                        } else {
                            Text("Must be a valid P2PKH address starting with 'P'", color = Color.Gray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PepepowPrimary,
                        focusedLabelColor = PepepowPrimary
                    )
                )

                // Amount Input
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (PEPEW)") },
                    placeholder = { Text("0.0") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSending,
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError!!, color = Color.Red)
                        }
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = { amountStr = (balance - 0.001).coerceAtLeast(0.0).toString() },
                            enabled = !isSending
                        ) {
                            Text("MAX", color = PepepowPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PepepowPrimary,
                        focusedLabelColor = PepepowPrimary
                    )
                )

                // Transaction Fee Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Network Fee", fontSize = 13.sp, color = Color.Gray)
                        Text("0.001 PEPEW", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Diagnostics Panel
                var showDiagnostics by remember { mutableStateOf(false) }
                var diagnostics by remember { mutableStateOf<net.pepepow.wallet.data.WalletDiagnostics?>(null) }

                LaunchedEffect(showDiagnostics, sendSuccess) {
                    if (showDiagnostics) {
                        while (true) {
                            diagnostics = walletViewModel.checkDiagnostics()
                            kotlinx.coroutines.delay(5000)
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDiagnostics = !showDiagnostics },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Diagnostics Panel", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PepepowPrimary)
                            Text(if (showDiagnostics) "▲" else "▼", color = PepepowPrimary, fontWeight = FontWeight.Bold)
                        }

                        if (showDiagnostics) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            val d = diagnostics
                            if (d == null) {
                                Text("Loading diagnostics...", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("API Connected", fontSize = 12.sp, color = Color.Gray)
                                        Text(if (d.apiConnected) "YES" else "NO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (d.apiConnected) PepepowPrimary else Color.Red)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("UTXO Endpoint", fontSize = 12.sp, color = Color.Gray)
                                        Text(d.utxoEndpointStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (d.utxoEndpointStatus == "ok") PepepowPrimary else Color.Red)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("UTXO Count", fontSize = 12.sp, color = Color.Gray)
                                        Text("${d.utxoCount}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Spendable Amount", fontSize = 12.sp, color = Color.Gray)
                                        Text("${formatPepewAmount(d.spendableAmountDouble, 4)} PEPEW", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Local Signing", fontSize = 12.sp, color = Color.Gray)
                                        Text(if (d.signingEnabled) "ENABLED" else "BLOCKED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (d.signingEnabled) PepepowPrimary else Color.Red)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Broadcast Endpoint", fontSize = 12.sp, color = Color.Gray)
                                        Text(d.broadcastEndpointStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (d.broadcastEndpointStatus == "ok") PepepowPrimary else Color.Red)
                                    }
                                    if (d.lastSendError != null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Last Send Error:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                        Text(d.lastSendError, fontSize = 11.sp, color = Color.Red, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (disabledReason != null && (recipientAddress.isNotEmpty() || amountStr.isNotEmpty())) {
                    Text(
                        text = disabledReason,
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { sendViewModel.sendPepew(recipientAddress, amountStr) },
                    enabled = sendButtonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(sendProgress ?: "Preparing transaction...", fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Text("SEND PEPEW", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val transactions by historyViewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions) { tx ->
                        TxItem(tx = tx)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    walletViewModel: WalletViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToConsolidate: () -> Unit,
    onResetWallet: () -> Unit
) {
    var showSeedPrompt by remember { mutableStateOf(false) }
    var isMnemonicRevealed by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    
    val mnemonic by walletViewModel.mnemonic.collectAsState()
    val address by walletViewModel.address.collectAsState()
    val isApiMode by walletViewModel.isApiMode.collectAsState()
    val context = LocalContext.current

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Wallet", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure? This will remove the mock wallet from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetWallet()
                    }
                ) {
                    Text("CONFIRM RESET", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Wallet Information
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Wallet Information",
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Wallet Mode", fontSize = 14.sp)
                            Text(
                                text = if (isApiMode) "Read-only API" else "Mock / Prototype",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Non-Custodial", fontSize = 14.sp)
                            Text("Yes (Keys stay local)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Wallet Address", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (address.isBlank()) "No address available" else address,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { openAddressInExplorer(context, address) },
                            enabled = address.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp)
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open in Explorer", fontSize = 13.sp)
                        }
                    }
                }

                // Section 2: Security & Backup
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Security & Backup",
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    showSeedPrompt = !showSeedPrompt
                                    if (!showSeedPrompt) {
                                        isMnemonicRevealed = false 
                                    }
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VpnKey, null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("View Backup Mnemonic", fontSize = 14.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }

                        if (showSeedPrompt) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Warning",
                                            tint = Color(0xFFE65100),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Security Warning",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Ensure no one is looking at your screen. Anyone with this mnemonic phrase can access all your mock PEPEW.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100),
                                        lineHeight = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    if (!isMnemonicRevealed) {
                                        Button(
                                            onClick = { isMnemonicRevealed = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Text("REVEAL MNEMONIC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    } else {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = PepepowSurface),
                                            border = BorderStroke(1.dp, Color(0xFFE65100)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = mnemonic ?: "No mnemonic available",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                modifier = Modifier.padding(12.dp),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (mnemonic != null) {
                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    val clip = ClipData.newPlainText("Mnemonic", mnemonic)
                                                    clipboard.setPrimaryClip(clip)
                                                    Toast.makeText(context, "Mnemonic copied", Toast.LENGTH_SHORT).show()
                                                },
                                                border = BorderStroke(1.dp, Color(0xFFE65100)),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Text("Copy mnemonic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }

                                        TextButton(
                                            onClick = { isMnemonicRevealed = false },
                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                        ) {
                                            Text("HIDE MNEMONIC", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Node Details
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Network Configuration",
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("API Endpoint", fontSize = 14.sp)
                            Text(
                                "https://light.pepepow.net/",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Wallet Mode", fontSize = 14.sp)
                            Text(
                                "Non-custodial",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PepepowPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Security Warning: Only public address data, UTXOs, and signed transactions are sent to the API. Your recovery phrase and private keys never leave this device.",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935),
                            lineHeight = 14.sp
                        )
                    }
                }

                // Section 4: Advanced Tools
                Card(colors = CardDefaults.cardColors(containerColor = PepepowSurface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Advanced Tools",
                            fontWeight = FontWeight.Bold,
                            color = PepepowPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToConsolidate() }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Build, null, tint = Color.Gray)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Consolidate UTXOs", fontSize = 14.sp)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                        }
                    }
                }
            }

            // Reset Wallet Button
            Button(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("RESET WALLET", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiStatusScreen(
    walletViewModel: WalletViewModel,
    apiStatusViewModel: ApiStatusViewModel,
    onNavigateBack: () -> Unit
) {
    val apiState by apiStatusViewModel.apiState.collectAsState()
    val apiMessage by apiStatusViewModel.apiMessage.collectAsState()
    val isApiLoading by apiStatusViewModel.isApiLoading.collectAsState()
    val isApiMode by walletViewModel.isApiMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("API Node Connection", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    color = when (apiState) {
                        ApiState.CONNECTED, ApiState.READY -> Color(0xFFE8F5E9)
                        ApiState.FAILED -> Color(0xFFFFEBEE)
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (apiState) {
                            ApiState.CONNECTED, ApiState.READY -> Icons.Default.CheckCircle
                            ApiState.FAILED -> Icons.Default.Cancel
                        }
                        val tint = when (apiState) {
                            ApiState.CONNECTED, ApiState.READY -> Color(0xFF2E7D32)
                            ApiState.FAILED -> Color(0xFFC62828)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Node Address:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "https://light.pepepow.net/",
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "API Mode:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = if (isApiMode) "Read-only API" else "Mock / Prototype",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val stateLabel = when (apiState) {
                    ApiState.CONNECTED -> "CONNECTED"
                    ApiState.READY -> "READY"
                    ApiState.FAILED -> "FAILED / OFFLINE"
                }
                val stateDesc = when (apiState) {
                    ApiState.CONNECTED -> "The node is connected and authenticating sessions."
                    ApiState.READY -> "Connected and operational. Synchronization complete."
                    ApiState.FAILED -> "Unable to resolve connection to node light.pepepow.net."
                }

                Text(
                    text = "Current State: $stateLabel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (apiState) {
                        ApiState.CONNECTED, ApiState.READY -> Color(0xFF2E7D32)
                        ApiState.FAILED -> Color(0xFFC62828)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stateDesc,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Last Check Result:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = apiMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "API provides blockchain data. Keys stay local.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Retry Button
                Button(
                    onClick = { apiStatusViewModel.retryApiConnection() },
                    enabled = !isApiLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isApiLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("RETRY CONNECTION")
                }
            }

            Text(
                text = "PEPEW non-custodial wallet communicates directly with standard lightweight nodes. Your recovery credentials never traverse the internet.",
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreWalletScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var mnemonicInput by remember { mutableStateOf("") }
    val restoreError by viewModel.restoreError.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearRestoreError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore Wallet", fontWeight = FontWeight.Bold) },
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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter Recovery Phrase",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PepepowPrimary
                )

                Text(
                    text = "Please enter your 12-word mnemonic seed phrase in the correct order to restore your PEPEW wallet.",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = mnemonicInput,
                    onValueChange = {
                        mnemonicInput = it
                        viewModel.clearRestoreError()
                    },
                    label = { Text("Mnemonic Phrase") },
                    placeholder = { Text("Enter your 12-word recovery phrase") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    isError = restoreError != null,
                    supportingText = {
                        if (restoreError != null) {
                            Text(restoreError!!, color = Color.Red)
                        } else {
                            Text("12 words separated by spaces", color = Color.Gray)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PepepowPrimary,
                        focusedLabelColor = PepepowPrimary
                    ),
                    maxLines = 5,
                    singleLine = false
                )
            }

            Button(
                onClick = {
                    val success = viewModel.restoreWallet(mnemonicInput)
                    if (success) {
                        onNavigateToDashboard()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("RESTORE WALLET", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

