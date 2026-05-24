package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Expense
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double, currency: String): String {
    val symbol = if (currency == "INR") "₹" else "$"
    if (amount.isNaN()) return "${symbol}0.00"
    if (amount.isInfinite()) return if (amount < 0) "-${symbol}∞" else "${symbol}∞"
    return "$symbol${String.format(Locale.US, "%,.2f", amount)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val expenses by viewModel.expenses.collectAsState()
    val sheetsUrl by viewModel.sheetsUrl.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val isTesting by viewModel.isTestingConnection.collectAsState()
    val connectionResult by viewModel.connectionResult.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Alert for Sync messages
    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        }
    }

    // Alert for Connection tests
    LaunchedEffect(connectionResult) {
        connectionResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearConnectionResult()
        }
    }

    // Expense calculations for the current month
    val currentMonthCalendar = Calendar.getInstance()
    val currentMonthIndex = currentMonthCalendar.get(Calendar.MONTH)
    val currentYearIndex = currentMonthCalendar.get(Calendar.YEAR)

    val currentMonthExpenses = expenses.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.MONTH) == currentMonthIndex && cal.get(Calendar.YEAR) == currentYearIndex
    }

    val totalSpent = currentMonthExpenses.sumOf { it.amount }
    val remainingBudget = monthlyBudget - totalSpent
    val percentSpent = if (monthlyBudget > 0) (totalSpent / monthlyBudget).toFloat() else 0f

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant Top Welcome & Balance Widget
            HeaderWidget(
                totalSpent = totalSpent,
                remainingBudget = remainingBudget,
                sheetsUrlSet = sheetsUrl.isNotBlank()
            )

            // Dynamic Segmented Sliding Tabs
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val tabs = listOf(
                    Triple("Overview", Icons.Default.PieChart, 0),
                    Triple("Transactions", Icons.Default.History, 1),
                    Triple("Sheets Sync", Icons.Default.CloudSync, 2)
                )
                tabs.forEach { (title, icon, idx) ->
                    Tab(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    fontWeight = if (activeTab == idx) FontWeight.Bold else FontWeight.Medium,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> OverviewTabContent(
                        currentMonthExpenses = currentMonthExpenses,
                        totalSpent = totalSpent,
                        percentSpent = percentSpent,
                        monthlyBudget = monthlyBudget,
                        sheetsUrl = sheetsUrl,
                        currency = currency,
                        onBudgetLimitUpdate = { viewModel.updateMonthlyBudget(it) }
                    )
                    1 -> HistoryTabContent(
                        expenses = expenses,
                        currency = currency,
                        onDeleteExpense = { viewModel.deleteExpense(it) }
                    )
                    2 -> SettingsTabContent(
                        sheetsUrl = sheetsUrl,
                        isSyncing = isSyncing,
                        isTesting = isTesting,
                        lastSyncTime = viewModel.getLastSyncTime(),
                        currency = currency,
                        onCurrencyChange = { viewModel.updateCurrency(it) },
                        onTestConnection = { url -> viewModel.testConnection(url) },
                        onExportSync = { viewModel.syncExport() },
                        onImportSync = { viewModel.syncImport() },
                        onClearAll = { viewModel.deleteLocalDatabase() },
                        onUrlSave = { viewModel.updateSheetsUrl(it) }
                    )
                }
            }
        }
    }

    // Add Expense custom elegant floating popup Dialog
    if (showAddDialog) {
        AddExpenseDialog(
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, description, paymentMethod ->
                viewModel.addExpense(amount, category, description, paymentMethod)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun HeaderWidget(
    totalSpent: Double,
    remainingBudget: Double,
    sheetsUrlSet: Boolean
) {
    val curDate = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()).uppercase()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = curDate,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.6.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Daily Spending",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Sync Badge styled as an Elegant round status profile icon according to Bento HTML
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (sheetsUrlSet) Icons.Default.Sync else Icons.Default.WifiOff,
                    contentDescription = if (sheetsUrlSet) "Sheets Sync Connected" else "Local Only Mode",
                    tint = if (sheetsUrlSet) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun OverviewTabContent(
    currentMonthExpenses: List<Expense>,
    totalSpent: Double,
    percentSpent: Float,
    monthlyBudget: Double,
    sheetsUrl: String,
    currency: String,
    onBudgetLimitUpdate: (Double) -> Unit
) {
    var showBudgetEditor by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(monthlyBudget.toString()) }

    val remainingBudget = monthlyBudget - totalSpent

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bento Hero: Available Balance & Budget Progress Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatCurrency(remainingBudget, currency),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(
                            onClick = { showBudgetEditor = !showBudgetEditor },
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.35f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Edit Budget",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Budget Custom Editor Inline Panel
                    if (showBudgetEditor) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .padding(10.dp)
                        ) {
                            OutlinedTextField(
                                value = budgetInput,
                                onValueChange = { budgetInput = it },
                                label = { Text("Budget Limit") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val newCap = budgetInput.toDoubleOrNull() ?: 1000.0
                                    onBudgetLimitUpdate(newCap)
                                    showBudgetEditor = false
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Translucent goal container matching bento_blur structure
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.45f))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MONTHLY BUDGET GOAL",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                    letterSpacing = 0.8.sp
                                )
                                Text(
                                    text = "${(percentSpent * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Elegant primary progress line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(percentSpent.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Spent: ${formatCurrency(totalSpent, currency)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "Cap: ${formatCurrency(monthlyBudget, currency)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bento Symmetrical Status Grid Row: Sheets Sync & Record Count
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Sheets Sync Bento Cell
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SHEETS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = if (sheetsUrl.isNotBlank()) "Synced" else "Local Only",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Count Bento Cell
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(84.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BentoRose),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = BentoOnPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "COUNT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${currentMonthExpenses.size} Items",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Bento Symmetrical Category Insights Row
        item {
            val categorySums = currentMonthExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            if (categorySums.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Bento Cell 1 (Dining/Shopping - Rose style)
                    val firstCat = categorySums.getOrNull(0)
                    if (firstCat != null) {
                        val catInfo = CategoryConfig.getCategoryInfo(firstCat.first)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BentoRose),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(124.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = catInfo.icon,
                                        contentDescription = null,
                                        tint = BentoPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = catInfo.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = BentoOnPrimaryContainer.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatCurrency(firstCat.second, currency),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BentoOnPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Category Bento Cell 2 (Transit/Other - Mint style)
                    val secondCat = categorySums.getOrNull(1)
                    val catInfo = if (secondCat != null) {
                        CategoryConfig.getCategoryInfo(secondCat.first)
                    } else {
                        CategoryConfig.getCategoryInfo("Transit")
                    }
                    val secondCatAmt = secondCat?.second ?: 0.0

                    Card(
                        colors = CardDefaults.cardColors(containerColor = BentoMint),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(124.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = catInfo.icon,
                                    contentDescription = null,
                                    tint = Color(0xFF137333),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = catInfo.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF137333).copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatCurrency(secondCatAmt, currency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D1B20)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Native Donut Category Distribution Chart wrapped in elegant Bento Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Category Allocation",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (currentMonthExpenses.isEmpty()) {
                        EmptyStateWidget("No expenses loaded for this month yet. Tap the '+' button below to create your first transaction!")
                    } else {
                        // Gather category-wise sums
                        val categorySums = currentMonthExpenses.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }
                            .toList()
                            .sortedByDescending { it.second }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Donut Canvas
                            DonutChartWidget(
                                categorySums = categorySums,
                                total = totalSpent,
                                modifier = Modifier.size(140.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Custom Legend Rows
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                categorySums.take(4).forEach { (catName, amt) ->
                                    val catInfo = CategoryConfig.getCategoryInfo(catName)
                                    val percent = if (totalSpent > 0.0) ((amt / totalSpent) * 100).toInt() else 0
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(catInfo.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = catName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "$percent%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Detailed Listing of Category Breakdown
        if (currentMonthExpenses.isNotEmpty()) {
            val categorySums = currentMonthExpenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }

            item {
                Text(
                    text = "Spending Breakdown",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(categorySums) { (catName, capSpent) ->
                CategoryRowProgress(
                    categoryName = catName,
                    amount = capSpent,
                    total = totalSpent,
                    currency = currency
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DonutChartWidget(
    categorySums: List<Pair<String, Double>>,
    total: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (total <= 0.0 || total.isNaN() || total.isInfinite()) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            return@Canvas
        }
        var runningAngle = -90f // Start from the top
        for ((catName, amt) in categorySums) {
            val angle = ((amt / total) * 360f).toFloat()
            if (angle.isNaN() || angle.isInfinite() || angle <= 0f) continue
            val catInfo = CategoryConfig.getCategoryInfo(catName)
            
            drawArc(
                color = catInfo.color,
                startAngle = runningAngle,
                sweepAngle = angle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
            )
            runningAngle += angle
        }
    }
}

@Composable
fun CategoryRowProgress(
    categoryName: String,
    amount: Double,
    total: Double,
    currency: String
) {
    val catInfo = CategoryConfig.getCategoryInfo(categoryName)
    val percentage = if (total > 0) (amount / total).toFloat() else 0f

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge Box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catInfo.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catInfo.icon,
                    contentDescription = catInfo.name,
                    tint = catInfo.color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = catInfo.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = formatCurrency(amount, currency),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Smooth Bar indicator representing relative share of categories
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(percentage)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(catInfo.color)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(percentage * 100).toInt()}% of overall monthly allocation",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun HistoryTabContent(
    expenses: List<Expense>,
    currency: String,
    onDeleteExpense: (Expense) -> Unit
) {
    var searchFilter by remember { mutableStateOf("") }
    var selectCategoryFilter by remember { mutableStateOf<String?>(null) }

    val filteredExpenses = expenses.filter {
        val matchesSearch = it.description.contains(searchFilter, ignoreCase = true) ||
                it.category.contains(searchFilter, ignoreCase = true)
        val matchesCategory = selectCategoryFilter == null || it.category.equals(selectCategoryFilter, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Search & Category row Filters
        OutlinedTextField(
            value = searchFilter,
            onValueChange = { searchFilter = it },
            placeholder = { Text("Search description...") },
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = selectCategoryFilter == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                        .clickable { selectCategoryFilter = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All Categories",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(CategoryConfig.categories) { cat ->
                val isSelected = selectCategoryFilter == cat.name
                val borderCol = animateColorAsState(if (isSelected) cat.color else Color.Transparent)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) cat.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) cat.color else MaterialTheme.colorScheme.outline,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectCategoryFilter = cat.name }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (filteredExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateWidget("No expenses found matching the active filter. Tap the button or slide to overview to begin tracking.")
            }
        } else {
            // Group expenses by Date Day in descending order
            val DateFormatDay = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
            val groupedExpenses = filteredExpenses.groupBy {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                // Clear time elements to group cleanly by pure calendar day
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedExpenses.keys.forEach { dayTimestamp ->
                    val dayExpenses = groupedExpenses[dayTimestamp] ?: emptyList()
                    val dailyTotal = dayExpenses.sumOf { it.amount }
                    val dayFormatted = DateFormatDay.format(Date(dayTimestamp))

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayFormatted,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Daily Total: ${formatCurrency(dailyTotal, currency)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    items(dayExpenses) { exp ->
                        ExpenseListRow(expense = exp, currency = currency, onDelete = { onDeleteExpense(exp) })
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun ExpenseListRow(
    expense: Expense,
    currency: String,
    onDelete: () -> Unit
) {
    val catInfo = CategoryConfig.getCategoryInfo(expense.category)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(catInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catInfo.icon,
                    contentDescription = null,
                    tint = catInfo.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.description.ifBlank { "Untitled Expense" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = expense.paymentMethod,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (expense.synced) "Synced with Sheet" else "Local Unsynced",
                        fontSize = 11.sp,
                        color = if (expense.synced) MaterialTheme.colorScheme.primary else Color(0xFFFF9100)
                    )
                }
            }

            Text(
                text = "-${formatCurrency(expense.amount, currency)}",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_expense_button")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete transaction",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsTabContent(
    sheetsUrl: String,
    isSyncing: Boolean,
    isTesting: Boolean,
    lastSyncTime: Long,
    currency: String,
    onCurrencyChange: (String) -> Unit,
    onTestConnection: (String) -> Unit,
    onExportSync: () -> Unit,
    onImportSync: () -> Unit,
    onClearAll: () -> Unit,
    onUrlSave: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf(sheetsUrl) }
    val clipboard = LocalClipboardManager.current
    var showSnippet by remember { mutableStateOf(false) }

    val lastSyncFormatted = if (lastSyncTime > 0) {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(lastSyncTime))
    } else {
        "Never Synced"
    }

    // Google Apps Script Deployable Code snippet
    val appsScriptSnippet = """
function doPost(e) {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  var params = JSON.parse(e.postData.contents);
  var action = params.action;
  
  if (action === "test") {
    return ContentService.createTextOutput(JSON.stringify({ 
      success: true, 
      message: "Ready: " + SpreadsheetApp.getActiveSpreadsheet().getName() 
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "sync") {
    var expenses = params.expenses;
    if (sheet.getLastRow() === 0) {
      sheet.appendRow(["ID", "Date", "Amount", "Category", "Description", "Payment Method"]);
    }
    var existingIds = {};
    if (sheet.getLastRow() > 1) {
      var ids = sheet.getRange(2, 1, sheet.getLastRow() - 1, 1).getValues();
      for (var i = 0; i < ids.length; i++) {
        existingIds[ids[i][0]] = true;
      }
    }
    var addedCount = 0;
    for (var i = 0; i < expenses.length; i++) {
      var exp = expenses[i];
      if (!existingIds[exp.id]) {
        var d = new Date(exp.date).toISOString().split('T')[0];
        sheet.appendRow([exp.id, d, exp.amount, exp.category, exp.description, exp.paymentMethod]);
        addedCount++;
      }
    }
    return ContentService.createTextOutput(JSON.stringify({ 
      success: true, 
      message: "Synced " + addedCount + " items." 
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (action === "get") {
    var expenses = [];
    if (sheet.getLastRow() > 1) {
      var d = sheet.getRange(2, 1, sheet.getLastRow() - 1, 6).getValues();
      for (var i = 0; i < d.length; i++) {
        var row = d[i];
        expenses.push({
          id: Number(row[0]),
          date: new Date(row[1]).getTime() || Date.now(),
          amount: Number(row[2]),
          category: String(row[3]),
          description: String(row[4]),
          paymentMethod: String(row[5])
        });
      }
    }
    return ContentService.createTextOutput(JSON.stringify({ 
      success: true, 
      expenses: expenses 
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
""".trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Preferred Currency Selection Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Preferred Currency",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select your preferred currency style. The entire app (overview, breakouts, history, addition) will instantly switch to display the chosen currency.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("INR" to "₹ Rupee (INR)", "USD" to "$ Dollar (USD)").forEach { (code, label) ->
                            val isSelected = currency == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary 
                                        else Color.Transparent
                                    )
                                    .clickable { onCurrencyChange(code) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // App Script configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Google Sheets Backend Connection",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter your Google Apps Script Web App Deploy URL to store and sync raw transactions.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            onUrlSave(it)
                        },
                        label = { Text("Apps Script Web App URL") },
                        placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onTestConnection(urlInput) },
                            enabled = !isTesting && urlInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text("Test URL")
                            }
                        }
                    }
                }
            }
        }

        // Action controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Synchronize Database",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Synced Action:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = lastSyncFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // EXPORT BUTTON
                        Button(
                            onClick = onExportSync,
                            enabled = !isSyncing && sheetsUrl.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export to Sheet", fontSize = 12.sp)
                            }
                        }

                        // IMPORT BUTTON
                        Button(
                            onClick = onImportSync,
                            enabled = !isSyncing && sheetsUrl.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onSecondary)
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import from Sheet", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Deploy tutorial Card Accordion
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSnippet = !showSnippet },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sheets Apps Script Code Setup",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Icon(
                            imageVector = if (showSnippet) Icons.Default.Info else Icons.Default.Settings,
                            contentDescription = "Show script details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (showSnippet) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "1. Create a Google Spreadsheet.\n" +
                                    "2. Click 'Extensions' -> 'Apps Script' at the top menu.\n" +
                                    "3. Replace the entire boilerplate Script with the code snippet below.\n" +
                                    "4. Click 'Deploy' (top-right) -> 'New Deployment'.\n" +
                                    "5. Choose 'Web App'. Set execute as: 'Me', Access: 'Anyone'.\n" +
                                    "6. Deploy and copy the resulting Web App URL directly in settings above!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(appsScriptSnippet))
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Apps Script Code", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Danger zone Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reset Operations",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Local SQLite Data", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateWidget(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AddExpenseDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, category: String, description: String, paymentMethod: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var descriptionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryConfig.categories.first().name) }
    var selectedPaymentMethod by remember { mutableStateOf("Card") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Text(
                    text = "Track New Spending",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // AMOUNT input box
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (${if (currency == "INR") "₹ INR" else "$ USD"})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // MERCHANT DESCRIPTION input box
                OutlinedTextField(
                    value = descriptionText,
                    onValueChange = { descriptionText = it },
                    label = { Text("Description / Merchant") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_desc_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CATEGORY Selector Title
                Text(
                    text = "Category Allocation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CategoryConfig.categories) { cat ->
                        val isSelected = selectedCategory == cat.name
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) cat.color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) cat.color else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat.name }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.color,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // PAYMENT METHOD selector
                Text(
                    text = "Payment Source",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val methods = listOf("Card", "Cash", "Mobile Pay")
                    methods.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = method,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // SAVE & CANCEL Action actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSave(amt, selectedCategory, descriptionText.trim(), selectedPaymentMethod)
                            }
                        },
                        enabled = amountText.toDoubleOrNull() != null && (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Text("Save Entry", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}
