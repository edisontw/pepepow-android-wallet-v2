package net.pepepow.wallet.ui.screens

import androidx.compose.foundation.background
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

// Custom Pepepow Theme Green colors
val PepepowPrimary = Color(0xFF2E7D32) // Froggy Green
val PepepowSecondary = Color(0xFFC8E6C9)
val PepepowBackground = Color(0xFFF1F8E9)
val PepepowSurface = Color(0xFFFFFFFF)

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
    val apiState by apiStatusViewModel.apiState.collectAsState()
    val transactions by historyViewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🐸 PEPEW", fontWeight = FontWeight.Bold, color = PepepowPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToApiStatus) {
                        val color = when (apiState) {
                            ApiState.READY, ApiState.CONNECTED -> Color(0xFF4CAF50)
                            ApiState.FAILED -> Color(0xFFF44336)
                        }
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = "API Status",
                            tint = color
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.DarkGray
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
                        text = "%,.4f PEPEW".format(balance),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "~ $%,.2f USD".format(balance * 0.10), // Mock conversion rate
                        color = PepepowSecondary,
                        fontSize = 14.sp
                    )
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
                    icon = Icons.Default.ArrowOutward,
                    label = "Send",
                    onClick = onNavigateToSend
                )
                ActionButton(
                    icon = Icons.Default.QrCode,
                    label = "Receive",
                    onClick = onNavigateToReceive
                )
                ActionButton(
                    icon = Icons.Default.History,
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
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            color = PepepowSurface,
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.size(56.dp)
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
                Surface(
                    color = if (tx.isSend) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (tx.isSend) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = if (tx.isSend) Color(0xFFC62828) else Color(0xFF2E7D32),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = if (tx.isSend) "Sent PEPEW" else "Received PEPEW",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (tx.isPending) "Pending..." else dateStr,
                        fontSize = 11.sp,
                        color = if (tx.isPending) PepepowPrimary else Color.Gray,
                        fontWeight = if (tx.isPending) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (tx.isSend) "-" else "+"} ${tx.amount} PEPEW",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (tx.isSend) Color.Black else PepepowPrimary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = tx.address.take(12) + "..." + tx.address.takeLast(6),
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit
) {
    val address by viewModel.address.collectAsState()

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

                // Mock QR Code Box
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
                        // Drawing a beautiful custom vector QR Code placeholder
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code",
                                tint = Color.Black,
                                modifier = Modifier.size(160.dp)
                            )
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
                            onClick = { /* Copy address action standard */ },
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
                        text = "This address only accepts PEPEW. Sending any other cryptocurrency will result in permanent loss.",
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

    LaunchedEffect(sendSuccess) {
        if (sendSuccess == true) {
            sendViewModel.resetSendState()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send PEPEW", fontWeight = FontWeight.Bold) },
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
                            "%,.4f PEPEW".format(balance),
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
                    isError = addressError != null,
                    supportingText = {
                        if (addressError != null) {
                            Text(addressError!!, color = Color.Red)
                        } else {
                            Text("Must begin with a 'P'", color = Color.Gray)
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
                    isError = amountError != null,
                    supportingText = {
                        if (amountError != null) {
                            Text(amountError!!, color = Color.Red)
                        }
                    },
                    trailingIcon = {
                        TextButton(onClick = { amountStr = (balance - 0.001).coerceAtLeast(0.0).toString() }) {
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
            }

            Button(
                onClick = { sendViewModel.sendPepew(recipientAddress, amountStr) },
                colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("SEND PEPEW", fontWeight = FontWeight.Bold, color = Color.White)
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
                    Text("No transactions found", color = Color.Gray)
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
    viewModel: WalletViewModel,
    onNavigateBack: () -> Unit,
    onResetWallet: () -> Unit
) {
    var showSeedPrompt by remember { mutableStateOf(false) }
    val mnemonic by viewModel.mnemonic.collectAsState()

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
                // Section 1: Security Info
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
                                .clickable { showSeedPrompt = !showSeedPrompt }
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
                            Card(colors = CardDefaults.cardColors(containerColor = PepepowBackground)) {
                                Text(
                                    text = mnemonic ?: "No mnemonic available",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Section 2: Node Details
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("API Endpoint", fontSize = 14.sp)
                            Text(
                                "https://light.pepepow.net/",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // Reset Wallet Button
            Button(
                onClick = onResetWallet,
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

                Spacer(modifier = Modifier.height(32.dp))

                // Toggle for failure simulation
                Button(
                    onClick = {
                        if (apiState == ApiState.FAILED) {
                            apiStatusViewModel.retryApiConnection()
                        } else {
                            apiStatusViewModel.simulateApiFailure()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PepepowPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (apiState == ApiState.FAILED) "RETRY CONNECTION" else "SIMULATE DISCONNECT")
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
