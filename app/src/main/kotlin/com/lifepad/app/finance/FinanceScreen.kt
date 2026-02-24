package com.lifepad.app.finance

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifepad.app.components.CategoryIcon
import com.lifepad.app.components.ErrorSnackbarHost
import com.lifepad.app.components.HashtagChip
import com.lifepad.app.components.categoryIconForName
import com.lifepad.app.data.local.entity.CategoryEntity
import com.lifepad.app.data.local.entity.CategoryType
import com.lifepad.app.data.local.entity.TransactionEntity
import com.lifepad.app.data.local.entity.TransactionType
import com.lifepad.app.settings.FinanceIntervalSetting
import com.lifepad.app.ui.theme.ExpenseColor
import com.lifepad.app.ui.theme.IncomeColor
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FinanceScreen(
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onCreateTransaction: () -> Unit,
    onManageBudgets: () -> Unit = {},
    onBudgetClick: (Long) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToBills: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToNetWorth: () -> Unit = {},
    onNavigateToSafeToSpend: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToBudgetTemplates: () -> Unit = {},
    onNavigateToSearch: (String?) -> Unit = {},
    onCreateCategory: () -> Unit = {},
    onEditCategory: (Long) -> Unit = {},
    viewModel: FinanceHomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState()

    val intervalLabel = uiState.interval.label

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance") },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                    IconButton(onClick = { onNavigateToSearch(null) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Statistics") },
                            onClick = {
                                showMenu = false
                                onNavigateToStats()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Budgets") },
                            onClick = {
                                showMenu = false
                                onManageBudgets()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Recurring Bills") },
                            onClick = {
                                showMenu = false
                                onNavigateToBills()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Goals") },
                            onClick = {
                                showMenu = false
                                onNavigateToGoals()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Net Worth") },
                            onClick = {
                                showMenu = false
                                onNavigateToNetWorth()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Safe to Spend") },
                            onClick = {
                                showMenu = false
                                onNavigateToSafeToSpend()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Insights") },
                            onClick = {
                                showMenu = false
                                onNavigateToInsights()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Budget Templates") },
                            onClick = {
                                showMenu = false
                                onNavigateToBudgetTemplates()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
    when (selectedTab) {
        0 -> {
            androidx.compose.material3.FloatingActionButton(
                onClick = onCreateTransaction,
                modifier = Modifier.testTag("fab_create_transaction")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add record")
            }
        }
        1 -> {
            androidx.compose.material3.FloatingActionButton(
                onClick = onCreateCategory,
                modifier = Modifier.testTag("fab_create_category")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
    }
        },
        snackbarHost = {
            ErrorSnackbarHost(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("screen_finance")
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Records",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Categories",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = "Stats",
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp)
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    RecordsTab(
                        uiState = uiState,
                        currencyFormat = currencyFormat,
                        intervalLabel = intervalLabel,
                        onIntervalChange = { viewModel.setInterval(it) },
                        onCustomRange = { showCustomRangePicker = true },
                        onTransactionClick = onTransactionClick,
                        onTransactionEdit = onEditTransaction,
                        onHashtagClick = { onNavigateToSearch("#$it") }
                    )
                }
                1 -> {
                    CategoriesTab(
                        uiState = uiState,
                        onEditCategory = onEditCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        onReorder = viewModel::updateCategoryOrder
                    )
                }
                2 -> {
                    StatsTab()
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = filterSheetState
            ) {
                FilterSheet(
                    uiState = uiState,
                    onToggleCategory = viewModel::toggleCategorySelection,
                    onToggleHashtag = viewModel::toggleHashtagSelection,
                    onToggleLogic = viewModel::toggleCategoryTagLogic,
                    onClearFilters = viewModel::clearFilters,
                    onDismiss = { showFilterSheet = false }
                )
            }
        }

        if (showCustomRangePicker) {
            CustomRangePickerDialog(
                startDate = uiState.customStart,
                endDate = uiState.customEnd,
                onDismiss = { showCustomRangePicker = false },
                onConfirm = { start, end ->
                    viewModel.setCustomRange(start, end)
                    showCustomRangePicker = false
                }
            )
        }
    }
}

@Composable
private fun RecordsTab(
    uiState: FinanceHomeUiState,
    currencyFormat: NumberFormat,
    intervalLabel: String,
    onIntervalChange: (FinanceIntervalSetting) -> Unit,
    onCustomRange: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onTransactionEdit: (Long) -> Unit,
    onHashtagClick: (String) -> Unit
) {
    val grouped = remember(uiState.transactions) { groupTransactionsByDay(uiState.transactions) }
    val intervalMenuExpanded = remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = intervalLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box {
                        TextButton(onClick = { intervalMenuExpanded.value = true }) {
                            Text("Change")
                        }
                        DropdownMenu(
                            expanded = intervalMenuExpanded.value,
                            onDismissRequest = { intervalMenuExpanded.value = false }
                        ) {
                            FinanceIntervalSetting.entries.forEach { interval ->
                                DropdownMenuItem(
                                    text = { Text(interval.label) },
                                    onClick = {
                                        intervalMenuExpanded.value = false
                                        if (interval == FinanceIntervalSetting.CUSTOM) {
                                            onCustomRange()
                                        } else {
                                            onIntervalChange(interval)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Net Balance", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(uiState.netBalance),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.netBalance >= 0) IncomeColor else ExpenseColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("Income", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(uiState.totalIncome),
                                    color = IncomeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Expenses", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(uiState.totalExpense),
                                    color = ExpenseColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (grouped.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No records for this range", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            grouped.forEach { dayGroup ->
                item {
                    DayHeader(dayGroup, currencyFormat)
                }
                items(dayGroup.transactions, key = { it.id }) { transaction ->
                    RecordRow(
                        transaction = transaction,
                        category = uiState.categories.firstOrNull { it.id == transaction.categoryId },
                        showNotes = uiState.showNotes,
                        showTags = uiState.showTags,
                        onClick = { onTransactionClick(transaction.id) },
                        onEdit = { onTransactionEdit(transaction.id) },
                        onHashtagClick = onHashtagClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dayGroup: DayGroup, currencyFormat: NumberFormat) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dayGroup.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            text = currencyFormat.format(dayGroup.net),
            style = MaterialTheme.typography.labelLarge,
            color = if (dayGroup.net >= 0) IncomeColor else ExpenseColor
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RecordRow(
    transaction: TransactionEntity,
    category: CategoryEntity?,
    showNotes: Boolean,
    showTags: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onHashtagClick: (String) -> Unit
) {
    val amountColor = if (transaction.type == TransactionType.INCOME) IncomeColor else ExpenseColor
    val amountPrefix = if (transaction.type == TransactionType.INCOME) "+" else "-"
    val hashtags = if (showTags) {
        com.lifepad.app.domain.parser.HashtagParser.extractHashtags(transaction.description)
    } else {
        emptyList()
    }
    var rowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { rowMenu = true })
            .testTag("transaction_item")
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                icon = categoryIconForName(category?.icon),
                tint = category?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showNotes) {
                Text(
                    text = category?.name ?: "Uncategorized",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (hashtags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(hashtags) { tag ->
                        HashtagChip(name = tag, onClick = { onHashtagClick(tag) })
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix${NumberFormat.getCurrencyInstance().format(transaction.amount)}",
                style = MaterialTheme.typography.bodyLarge,
                color = amountColor,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(transaction.transactionDate)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = { rowMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = rowMenu,
                onDismissRequest = { rowMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        rowMenu = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = {
                        rowMenu = false
                        onClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun CategoriesTab(
    uiState: FinanceHomeUiState,
    onEditCategory: (Long) -> Unit,
    onDeleteCategory: (Long) -> Unit,
    onReorder: (List<CategoryEntity>) -> Unit
) {
    var selectedFilter by rememberSaveable { mutableStateOf(CategoryFilter.EXPENSE) }
    val categories = when (selectedFilter) {
        CategoryFilter.EXPENSE -> uiState.categories.filter { it.type == CategoryType.EXPENSE && !it.isArchived }
        CategoryFilter.INCOME -> uiState.categories.filter { it.type == CategoryType.INCOME && !it.isArchived }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CategoryFilter.entries) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter.label) }
                )
            }
        }
        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No categories")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        canMoveUp = category != categories.first(),
                        canMoveDown = category != categories.last(),
                        onEdit = { onEditCategory(category.id) },
                        onMoveUp = {
                            val updated = categories.toMutableList()
                            val index = updated.indexOfFirst { it.id == category.id }
                            if (index > 0) {
                                updated.add(index - 1, updated.removeAt(index))
                                onReorder(updated)
                            }
                        },
                        onMoveDown = {
                            val updated = categories.toMutableList()
                            val index = updated.indexOfFirst { it.id == category.id }
                            if (index in 0 until updated.lastIndex) {
                                updated.add(index + 1, updated.removeAt(index))
                                onReorder(updated)
                            }
                        },
                        onDelete = { onDeleteCategory(category.id) },
                        isArchived = false
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    isArchived: Boolean
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(
            icon = categoryIconForName(category.icon),
            tint = Color(category.color),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(category.name, style = MaterialTheme.typography.bodyLarge)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (category.type == CategoryType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = IncomeColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    category.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = onMoveUp, enabled = canMoveUp && !isArchived) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown && !isArchived) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Category menu")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        menuExpanded = false
                        onEdit()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsTab(
    viewModel: FinanceStatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FinanceStatsContent(
        uiState = uiState,
        onPeriodChange = viewModel::onPeriodChange,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    )
}


@Composable
private fun FilterSheet(
    uiState: FinanceHomeUiState,
    onToggleCategory: (Long) -> Unit,
    onToggleHashtag: (String) -> Unit,
    onToggleLogic: () -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Categories", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        val categoriesByType = uiState.categories.groupBy { it.type }
        categoriesByType.forEach { (type, categories) ->
            Text(
                if (type == CategoryType.EXPENSE) "Expense" else "Income",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowChips(
                items = categories.filter { !it.isArchived },
                selectedIds = uiState.selectedCategoryIds,
                onToggle = { onToggleCategory(it.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Hashtags", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        FlowTags(
            tags = uiState.hashtags.map { it.name },
            selected = uiState.selectedHashtags,
            onToggle = onToggleHashtag
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Categories vs Tags")
                Text(
                    if (uiState.categoryTagOrLogic) "Match categories OR tags" else "Match categories AND tags",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Switch(checked = uiState.categoryTagOrLogic, onCheckedChange = { onToggleLogic() })
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onClearFilters) { Text("Clear") }
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowChips(
    items: List<CategoryEntity>,
    selectedIds: Set<Long>,
    onToggle: (CategoryEntity) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { category ->
            FilterChip(
                selected = selectedIds.contains(category.id),
                onClick = { onToggle(category) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryIcon(
                            icon = categoryIconForName(category.icon),
                            tint = Color(category.color),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(category.name)
                    }
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FlowTags(
    tags: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            FilterChip(
                selected = selected.contains(tag),
                onClick = { onToggle(tag) },
                label = { Text("#$tag") }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomRangePickerDialog(
    startDate: Long?,
    endDate: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?, Long?) -> Unit
) {
    var currentStart by remember { mutableStateOf(startDate) }
    var currentEnd by remember { mutableStateOf(endDate) }
    var showStartPicker by remember { mutableStateOf(true) }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = if (showStartPicker) currentStart else currentEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = pickerState.selectedDateMillis
                if (showStartPicker) {
                    currentStart = selected
                    showStartPicker = false
                } else {
                    currentEnd = selected
                    val start = currentStart
                    val end = currentEnd
                    if (start != null && end != null && end < start) {
                        onConfirm(end, start)
                    } else {
                        onConfirm(start, end)
                    }
                }
            }) {
                Text(if (showStartPicker) "Next" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionEntity>,
    val net: Double
)

private fun groupTransactionsByDay(transactions: List<TransactionEntity>): List<DayGroup> {
    val grouped = transactions.groupBy { tx ->
        Instant.ofEpochMilli(tx.transactionDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }
    return grouped.entries
        .sortedByDescending { it.key }
        .map { (date, list) ->
            val income = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            DayGroup(date, list.sortedByDescending { it.transactionDate }, income - expense)
        }
}

private enum class CategoryFilter(val label: String) {
    EXPENSE("Expense"),
    INCOME("Income")
}
