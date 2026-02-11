@file:Suppress("DEPRECATION")

package com.example.hisabapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.hisabapp.ui.theme.HisabappTheme
import com.github.mikephil.charting.charts.BarChart as MPBarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

// region ===== Data Classes =====
data class Transaction(
    val id: Long = 0L,
    val name: String,
    val mobileNumber: String?,
    val amount: Double,
    val type: String,
    val categories: List<String>,
    val categoryQuantities: Map<String, Int>,
    val description: String?,
    val date: String,
    val categoryImageUris: List<String?>? = null,
    val defaultPrice: Double? = null
)

data class Category(
    val name: String,
    val imageUri: String? = null,
    val defaultPrice: Double? = null
)
// endregion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HisabappTheme {
                // Load transactions from SharedPreferences
                val context = LocalContext.current
                val sharedPreferences = remember { context.getSharedPreferences("HisabAppPrefs", Context.MODE_PRIVATE) }
                val gson = remember { Gson() }
                val transactionsState = remember { mutableStateOf(loadTransactions(sharedPreferences, gson)) }

                val defaultCategories = listOf(
                    Category("Food & Dining", defaultPrice = 10.0),
                    Category("Transportation", defaultPrice = 5.0),
                    Category("Shopping", defaultPrice = 20.0),
                    Category("Entertainment", defaultPrice = 15.0),
                    Category("Bills & Utilities", defaultPrice = 50.0),
                    Category("Health & Fitness", defaultPrice = 30.0),
                    Category("Education", defaultPrice = 25.0),
                    Category("Salary"),
                    Category("Gift", defaultPrice = 10.0),
                    Category("Other")
                )

                val loadedCategories = loadCategories(sharedPreferences, gson)
                val categories = remember(loadedCategories) {
                    val list = mutableStateListOf<Category>()
                    if (loadedCategories?.isNotEmpty() == true) {
                        list.addAll(loadedCategories)
                    } else {
                        list.addAll(defaultCategories)
                    }
                    list
                }
                var currentScreen by remember { mutableStateOf(Screen.Main) }
                var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

                when (currentScreen) {
                    Screen.Main -> MainScreenContent(
                        transactions = transactionsState.value,
                        onAddTransaction = { currentScreen = Screen.AddTransaction },
                        onEditTransaction = { transaction ->
                            transactionToEdit = transaction
                            currentScreen = Screen.EditTransaction
                        },
                        onViewReport = { currentScreen = Screen.Report },
                        onManageCategories = { currentScreen = Screen.ManageCategories }
                    )
                    Screen.AddTransaction -> AddTransactionScreen(
                        categories = categories,
                        onBack = { currentScreen = Screen.Main },
                        onSave = { newTransaction, newCategory ->
                            newCategory?.let {
                                if (categories.none { c -> c.name.equals(it.name, ignoreCase = true) }) {
                                    categories.add(it)
                                    saveCategories(sharedPreferences, gson, categories)
                                }
                            }
                            val updatedTransaction = newTransaction.copy(
                                id = (transactionsState.value.maxOfOrNull { it.id } ?: 0L) + 1L
                            )
                            transactionsState.value = transactionsState.value + updatedTransaction
                            saveTransactions(sharedPreferences, gson, transactionsState.value)
                            currentScreen = Screen.Main
                        }
                    )
                    Screen.EditTransaction -> transactionToEdit?.let { transaction ->
                        EditTransactionScreen(
                            transaction = transaction,
                            categories = categories,
                            onBack = { currentScreen = Screen.Main },
                            onSave = { updatedTransaction, newCategory ->
                                newCategory?.let {
                                    if (categories.none { c -> c.name.equals(it.name, ignoreCase = true) }) {
                                        categories.add(it)
                                        saveCategories(sharedPreferences, gson, categories)
                                    }
                                }
                                transactionsState.value = transactionsState.value.map {
                                    if (it.id == updatedTransaction.id) updatedTransaction else it
                                }
                                saveTransactions(sharedPreferences, gson, transactionsState.value)
                                currentScreen = Screen.Main
                                transactionToEdit = null
                            }
                        )
                    }
                    Screen.Report -> ReportScreen(
                        transactions = transactionsState.value,
                        categories = categories,
                        onBack = { currentScreen = Screen.Main }
                    )
                    Screen.ManageCategories -> ManageCategoriesScreen(
                        categories = categories,
                        onBack = { currentScreen = Screen.Main },
                        onAddCategory = { newCategory ->
                            if (categories.none { c -> c.name.equals(newCategory.name, ignoreCase = true) }) {
                                categories.add(newCategory)
                                saveCategories(sharedPreferences, gson, categories)
                            }
                        },
                        onDeleteCategory = { category ->
                            categories.remove(category)
                            saveCategories(sharedPreferences, gson, categories)
                        },
                        onSaveCategories = {
                            saveCategories(sharedPreferences, gson, categories)
                        }
                    )
                }
            }
        }
    }

    private fun saveTransactions(sharedPreferences: SharedPreferences, gson: Gson, transactions: List<Transaction>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(transactions)
        editor.putString("transactions", json)
        editor.apply()
    }

    private fun loadTransactions(sharedPreferences: SharedPreferences, gson: Gson): List<Transaction> {
        val json = sharedPreferences.getString("transactions", null)
        return if (json != null) {
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveCategories(sharedPreferences: SharedPreferences, gson: Gson, categories: List<Category>) {
        val editor = sharedPreferences.edit()
        val json = gson.toJson(categories)
        editor.putString("categories", json)
        editor.apply()
    }

    private fun loadCategories(sharedPreferences: SharedPreferences, gson: Gson): List<Category>? {
        val json = sharedPreferences.getString("categories", null)
        return if (json != null) {
            val type = object : TypeToken<List<Category>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    enum class Screen {
        Main,
        AddTransaction,
        EditTransaction,
        Report,
        ManageCategories
    }

    // region ===== Main Screen =====
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreenContent(
        transactions: List<Transaction>,
        onAddTransaction: () -> Unit,
        onEditTransaction: (Transaction) -> Unit,
        onViewReport: () -> Unit,
        onManageCategories: () -> Unit
    ) {
        var selectedDateFilter by remember { mutableStateOf("All Time") }
        var customDate by remember { mutableStateOf<Date?>(null) }
        var searchQuery by remember { mutableStateOf("") }
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val datePickerDialog = remember {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    customDate = calendar.time
                    selectedDateFilter = "Custom Date"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        val dateOptions = listOf("All Time", "Today", "This Week", "This Month", "This Year", "Custom Date")

        val filteredTransactions = remember(transactions, selectedDateFilter, customDate, searchQuery) {
            val dateFiltered = when (selectedDateFilter) {
                "Today" -> transactions.filter {
                    it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }
                "This Week" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    val startOfWeek = cal.time
                    transactions.filter { transaction ->
                        val transactionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.date)
                        transactionDate?.after(startOfWeek) == true || transactionDate == startOfWeek
                    }
                }
                "This Month" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    val startOfMonth = cal.time
                    transactions.filter { transaction ->
                        val transactionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.date)
                        transactionDate?.after(startOfMonth) == true || transactionDate == startOfMonth
                    }
                }
                "This Year" -> {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    val startOfYear = cal.time
                    transactions.filter { transaction ->
                        val transactionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.date)
                        transactionDate?.after(startOfYear) == true || transactionDate == startOfYear
                    }
                }
                "Custom Date" -> {
                    customDate?.let { selectedDate ->
                        val formattedSelectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
                        transactions.filter { it.date == formattedSelectedDate }
                    } ?: transactions
                }
                else -> transactions
            }

            if (searchQuery.isBlank()) {
                dateFiltered
            } else {
                val query = searchQuery.lowercase()
                dateFiltered.filter { transaction ->
                    transaction.name.lowercase().contains(query) ||
                            (transaction.description?.lowercase()?.contains(query) ?: false) ||
                            transaction.categories.any { it.lowercase().contains(query) }
                }
            }.sortedByDescending { it.date }
        }

        val groupedTransactions = remember(filteredTransactions) {
            filteredTransactions.groupBy { it.date }.toSortedMap(reverseOrder())
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Hisab App") },
                    actions = {
                        IconButton(onClick = onViewReport) {
                            Icon(Icons.Outlined.BarChart, contentDescription = "View Report")
                        }
                        TextButton(onClick = onManageCategories) {
                            Text("Manage Categories")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Transactions") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = "Search")
                    },
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Transaction History",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    TextButton(
                        onClick = { datePickerDialog.show() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.DateRange,
                                contentDescription = "Filter by Date",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (selectedDateFilter) {
                                    "Custom Date" -> customDate?.let {
                                        SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(it)
                                    } ?: "Select Date"
                                    else -> selectedDateFilter
                                }
                            )
                        }
                    }
                }

                if (groupedTransactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions found. Tap '+' to add one.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        groupedTransactions.forEach { (date, transactionsForDate) ->
                            item {
                                Text(
                                    text = formatDisplayDate(date),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(transactionsForDate) { transaction ->
                                TransactionCard(
                                    transaction = transaction,
                                    onEdit = { onEditTransaction(transaction) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun formatDisplayDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            parsedDate?.let { outputFormat.format(it) } ?: date
        } catch (e: Exception) {
            date
        }
    }

    @SuppressLint("DefaultLocale")
    @Composable
    fun TransactionCard(transaction: Transaction, onEdit: () -> Unit = {}) {
        val isIncome = transaction.type == "Income"
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isIncome) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = transaction.categoryImageUris?.firstOrNull() ?: R.drawable.ic_placeholder_category,
                    contentDescription = transaction.categories.joinToString(", "),
                    placeholder = painterResource(id = R.drawable.ic_placeholder_category),
                    error = painterResource(id = R.drawable.ic_placeholder_category),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transaction.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${if (isIncome) "+" else "-"}₹${String.format("%.2f", transaction.amount)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = transaction.categories.mapIndexed { index, category ->
                            val quantity = transaction.categoryQuantities[category] ?: 1
                            "$category${if (quantity > 1) " (x$quantity)" else ""}"
                        }.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    transaction.description?.let {
                        if (it.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDisplayDate(transaction.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        transaction.mobileNumber?.let {
                            if (it.isNotBlank()) {
                                Text(
                                    text = "Ph: $it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit Transaction")
                }
            }
        }
    }
    // endregion

    // region ===== Add Transaction Screen =====
    @SuppressLint("DefaultLocale")
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AddTransactionScreen(
        categories: List<Category>,
        onBack: () -> Unit,
        onSave: (Transaction, Category?) -> Unit
    ) {
        var name by remember { mutableStateOf("") }
        var mobileNumber by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var selectedCategories by remember { mutableStateOf(setOf<Category>()) }
        var categoryQuantities by remember { mutableStateOf(mapOf<String, Int>()) }
        var categoriesExpanded by remember { mutableStateOf(false) }
        var categoryInput by remember { mutableStateOf("") }
        var isIncome by remember { mutableStateOf(false) }
        var suggestedCategories by remember { mutableStateOf(setOf<Category>()) }
        var isAmountManuallySet by remember { mutableStateOf(false) }

        var nameError by remember { mutableStateOf<String?>(null) }
        var amountError by remember { mutableStateOf<String?>(null) }
        var categoryError by remember { mutableStateOf<String?>(null) }

        var showAddCategoryDialog by remember { mutableStateOf(false) }
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryDefaultPrice by remember { mutableStateOf("") }
        var newCategoryImageUri by remember { mutableStateOf<Uri?>(null) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            newCategoryImageUri = uri
        }

        val categoryKeywords = mapOf(
            "Food & Dining" to listOf("food", "dining", "restaurant", "meal", "groceries", "coffee"),
            "Transportation" to listOf("transport", "bus", "train", "taxi", "fuel", "car"),
            "Shopping" to listOf("shop", "clothes", "electronics", "mall", "store"),
            "Entertainment" to listOf("movie", "concert", "game", "event", "ticket"),
            "Bills & Utilities" to listOf("bill", "utility", "electricity", "water", "internet"),
            "Health & Fitness" to listOf("health", "gym", "doctor", "medicine", "fitness"),
            "Education" to listOf("education", "school", "course", "book", "tuition"),
            "Salary" to listOf("salary", "income", "pay", "wage"),
            "Gift" to listOf("gift", "present"),
            "Other" to listOf("misc", "other")
        )

        LaunchedEffect(name, description) {
            val inputText = "$name $description".lowercase()
            val detected = categories.filter { category ->
                categoryKeywords[category.name]?.any { keyword ->
                    inputText.contains(keyword)
                } == true
            }.toSet()
            suggestedCategories = detected
            if (selectedCategories.isEmpty()) {
                selectedCategories = detected
                categoryQuantities = detected.associate { it.name to 1 }
            }
        }

        LaunchedEffect(selectedCategories, categoryQuantities) {
            if (!isAmountManuallySet) {
                val totalAmount = selectedCategories.sumOf { category ->
                    val quantity = categoryQuantities[category.name] ?: 1
                    (category.defaultPrice ?: 0.0) * quantity
                }
                amount = if (totalAmount > 0) {
                    String.format(Locale.US, "%.2f", totalAmount)
                } else {
                    ""
                }
            }
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                newCategoryName = newCategoryName,
                newCategoryDefaultPrice = newCategoryDefaultPrice,
                newCategoryImageUri = newCategoryImageUri,
                onNameChange = { newCategoryName = it },
                onPriceChange = { newCategoryDefaultPrice = it },
                onDismiss = { showAddCategoryDialog = false },
                onSelectImage = { imagePickerLauncher.launch("image/*") },
                onConfirm = {
                    if (newCategoryName.isNotBlank()) {
                        val newCat = Category(
                            name = newCategoryName,
                            imageUri = newCategoryImageUri?.toString(),
                            defaultPrice = newCategoryDefaultPrice.toDoubleOrNull()
                        )
                        selectedCategories = selectedCategories + newCat
                        categoryQuantities = categoryQuantities + (newCat.name to 1)
                        categoryInput = ""
                        showAddCategoryDialog = false
                        newCategoryName = ""
                        newCategoryDefaultPrice = ""
                        newCategoryImageUri = null
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Add Transaction") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            nameError = if (name.isBlank()) "Name is required" else null
                            amountError = when {
                                amount.isBlank() -> "Amount is required"
                                amount.toDoubleOrNull() == null || amount.toDouble() <= 0 -> "Enter a valid amount"
                                else -> null
                            }
                            categoryError = if (selectedCategories.isEmpty() && categoryInput.isBlank()) "At least one category is required" else null

                            if (nameError == null && amountError == null && categoryError == null) {
                                val isNewCategory = categories.none { it.name.equals(categoryInput, ignoreCase = true) } && categoryInput.isNotBlank()
                                val newCategory = if (isNewCategory) Category(
                                    name = categoryInput,
                                    defaultPrice = null
                                ) else null
                                if (isNewCategory) {
                                    selectedCategories = (selectedCategories + newCategory) as Set<Category>
                                    categoryQuantities = categoryQuantities + (categoryInput to 1)
                                }

                                val transaction = Transaction(
                                    id = 0L,
                                    name = name.trim(),
                                    mobileNumber = mobileNumber.trim().takeIf { it.isNotBlank() },
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    type = if (isIncome) "Income" else "Expense",
                                    categories = selectedCategories.map { it.name },
                                    categoryQuantities = categoryQuantities.filterKeys { key -> selectedCategories.any { it.name == key } },
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    categoryImageUris = selectedCategories.map { it.imageUri },
                                    defaultPrice = if (selectedCategories.mapNotNull { it.defaultPrice }.isNotEmpty()) {
                                        selectedCategories.mapNotNull { it.defaultPrice }
                                            .zip(selectedCategories.map { categoryQuantities[it.name] ?: 1 })
                                            .sumOf { (price, qty) -> price * qty }
                                    } else null
                                )
                                onSave(transaction, newCategory)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Save Transaction" },
                        enabled = name.isNotBlank() && amount.isNotBlank() && (selectedCategories.isNotEmpty() || categoryInput.isNotBlank())
                    ) {
                        Text("Save Transaction")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        isAmountManuallySet = it.isNotBlank()
                        amountError = null
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₹ ") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (suggestedCategories.isNotEmpty()) {
                    Text(
                        text = "Suggested Categories:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category),
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                        categoryQuantities = categoryQuantities - category.name
                                        selectedCategories
                                    } else {
                                        selectedCategories + category
                                        categoryQuantities = categoryQuantities + (category.name to 1)
                                        selectedCategories
                                    }
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedCategories.isNotEmpty()) {
                    Text(
                        text = "Selected Categories:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    selectedCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${category.name}${category.defaultPrice?.let { " ($${String.format("%.2f", it)} each)" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (category.defaultPrice != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val currentQty = categoryQuantities[category.name] ?: 1
                                            if (currentQty > 1) {
                                                categoryQuantities = categoryQuantities + (category.name to currentQty - 1)
                                            }
                                        },
                                        enabled = (categoryQuantities[category.name] ?: 1) > 1
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease Quantity")
                                    }
                                    Text(
                                        text = "${categoryQuantities[category.name] ?: 1}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            val currentQty = categoryQuantities[category.name] ?: 1
                                            categoryQuantities = categoryQuantities + (category.name to currentQty + 1)
                                        }
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase Quantity")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val filteredCategories = categories.filter {
                    it.name.contains(categoryInput, ignoreCase = true)
                }

                ExposedDropdownMenuBox(
                    expanded = categoriesExpanded,
                    onExpandedChange = { categoriesExpanded = !categoriesExpanded }
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = {
                            categoryInput = it
                            categoriesExpanded = true
                            categoryError = null
                        },
                        label = { Text("Search or Add Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = categoryError != null,
                        supportingText = { categoryError?.let { Text(it) } },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoriesExpanded,
                        onDismissRequest = { categoriesExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(category.name)
                                        Checkbox(
                                            checked = selectedCategories.contains(category),
                                            onCheckedChange = { isChecked ->
                                                selectedCategories = if (isChecked) {
                                                    selectedCategories + category
                                                } else {
                                                    selectedCategories - category
                                                }
                                                categoryQuantities = if (isChecked) {
                                                    categoryQuantities + (category.name to 1)
                                                } else {
                                                    categoryQuantities - category.name
                                                }
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    selectedCategories = if (selectedCategories.contains(category)) {
                                        selectedCategories - category
                                        categoryQuantities = categoryQuantities - category.name
                                        selectedCategories
                                    } else {
                                        selectedCategories + category
                                        categoryQuantities = categoryQuantities + (category.name to 1)
                                        selectedCategories
                                    }
                                }
                            )
                        }
                        if (filteredCategories.isEmpty() && categoryInput.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Add \"$categoryInput\" as a new category") },
                                onClick = {
                                    newCategoryName = categoryInput
                                    showAddCategoryDialog = true
                                    categoriesExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // region ===== Edit Transaction Screen =====
    @SuppressLint("DefaultLocale")
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EditTransactionScreen(
        transaction: Transaction,
        categories: List<Category>,
        onBack: () -> Unit,
        onSave: (Transaction, Category?) -> Unit
    ) {
        var name by remember { mutableStateOf(transaction.name) }
        var mobileNumber by remember { mutableStateOf(transaction.mobileNumber ?: "") }
        var amount by remember { mutableStateOf(String.format("%.2f", transaction.amount)) }
        var description by remember { mutableStateOf(transaction.description ?: "") }
        var selectedCategories by remember { mutableStateOf(
            categories.filter { transaction.categories.contains(it.name) }.toSet()
        ) }
        var categoryQuantities by remember { mutableStateOf(transaction.categoryQuantities) }
        var categoriesExpanded by remember { mutableStateOf(false) }
        var categoryInput by remember { mutableStateOf("") }
        var isIncome by remember { mutableStateOf(transaction.type == "Income") }
        var suggestedCategories by remember { mutableStateOf(setOf<Category>()) }
        var isAmountManuallySet by remember { mutableStateOf(false) } // Start as false to allow auto-calculation

        var nameError by remember { mutableStateOf<String?>(null) }
        var amountError by remember { mutableStateOf<String?>(null) }
        var categoryError by remember { mutableStateOf<String?>(null) }

        var showAddCategoryDialog by remember { mutableStateOf(false) }
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryDefaultPrice by remember { mutableStateOf("") }
        var newCategoryImageUri by remember { mutableStateOf<Uri?>(null) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            newCategoryImageUri = uri
        }

        val categoryKeywords = mapOf(
            "Food & Dining" to listOf("food", "dining", "restaurant", "meal", "groceries", "coffee"),
            "Transportation" to listOf("transport", "bus", "train", "taxi", "fuel", "car"),
            "Shopping" to listOf("shop", "clothes", "electronics", "mall", "store"),
            "Entertainment" to listOf("movie", "concert", "game", "event", "ticket"),
            "Bills & Utilities" to listOf("bill", "utility", "electricity", "water", "internet"),
            "Health & Fitness" to listOf("health", "gym", "doctor", "medicine", "fitness"),
            "Education" to listOf("education", "school", "course", "book", "tuition"),
            "Salary" to listOf("salary", "income", "pay", "wage"),
            "Gift" to listOf("gift", "present"),
            "Other" to listOf("misc", "other")
        )

        // Auto-suggest categories based on name and description
        LaunchedEffect(name, description) {
            val inputText = "$name $description".lowercase()
            val detected = categories.filter { category ->
                categoryKeywords[category.name]?.any { keyword ->
                    inputText.contains(keyword)
                } == true
            }.toSet()
            suggestedCategories = detected
        }

        // Auto-calculate amount based on selected categories and quantities
        LaunchedEffect(selectedCategories, categoryQuantities) {
            if (!isAmountManuallySet) {
                val totalAmount = selectedCategories.sumOf { category ->
                    val quantity = categoryQuantities[category.name] ?: 1
                    (category.defaultPrice ?: 0.0) * quantity
                }
                amount = if (totalAmount > 0) {
                    String.format(Locale.US, "%.2f", totalAmount)
                } else {
                    ""
                }
            }
        }

        if (showAddCategoryDialog) {
            AddCategoryDialog(
                newCategoryName = newCategoryName,
                newCategoryDefaultPrice = newCategoryDefaultPrice,
                newCategoryImageUri = newCategoryImageUri,
                onNameChange = { newCategoryName = it },
                onPriceChange = { newCategoryDefaultPrice = it },
                onDismiss = { showAddCategoryDialog = false },
                onSelectImage = { imagePickerLauncher.launch("image/*") },
                onConfirm = {
                    if (newCategoryName.isNotBlank()) {
                        val newCat = Category(
                            name = newCategoryName,
                            imageUri = newCategoryImageUri?.toString(),
                            defaultPrice = newCategoryDefaultPrice.toDoubleOrNull()
                        )
                        selectedCategories = selectedCategories + newCat
                        categoryQuantities = categoryQuantities + (newCat.name to 1)
                        categoryInput = ""
                        showAddCategoryDialog = false
                        newCategoryName = ""
                        newCategoryDefaultPrice = ""
                        newCategoryImageUri = null
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Transaction") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = {
                            nameError = if (name.isBlank()) "Name is required" else null
                            amountError = when {
                                amount.isBlank() -> "Amount is required"
                                amount.toDoubleOrNull() == null || amount.toDouble() <= 0 -> "Enter a valid amount"
                                else -> null
                            }
                            categoryError = if (selectedCategories.isEmpty() && categoryInput.isBlank()) "At least one category is required" else null

                            if (nameError == null && amountError == null && categoryError == null) {
                                val isNewCategory = categories.none { it.name.equals(categoryInput, ignoreCase = true) } && categoryInput.isNotBlank()
                                val newCategory = if (isNewCategory) Category(
                                    name = categoryInput,
                                    defaultPrice = null
                                ) else null
                                if (isNewCategory) {
                                    selectedCategories = (selectedCategories + newCategory) as Set<Category>
                                    categoryQuantities = categoryQuantities + (categoryInput to 1)
                                }

                                val updatedTransaction = transaction.copy(
                                    name = name.trim(),
                                    mobileNumber = mobileNumber.trim().takeIf { it.isNotBlank() },
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    type = if (isIncome) "Income" else "Expense",
                                    categories = selectedCategories.map { it.name },
                                    categoryQuantities = categoryQuantities.filterKeys { key -> selectedCategories.any { it.name == key } },
                                    description = description.trim().takeIf { it.isNotBlank() },
                                    date = transaction.date,
                                    categoryImageUris = selectedCategories.map { it.imageUri },
                                    defaultPrice = if (selectedCategories.mapNotNull { it.defaultPrice }.isNotEmpty()) {
                                        selectedCategories.mapNotNull { it.defaultPrice }
                                            .zip(selectedCategories.map { categoryQuantities[it.name] ?: 1 })
                                            .sumOf { (price, qty) -> price * qty }
                                    } else null
                                )
                                onSave(updatedTransaction, newCategory)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .semantics { contentDescription = "Save Transaction" },
                        enabled = name.isNotBlank() && amount.isNotBlank() && (selectedCategories.isNotEmpty() || categoryInput.isNotBlank())
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    supportingText = { nameError?.let { Text(it) } },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { mobileNumber = it },
                    label = { Text("Mobile Number (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        isAmountManuallySet = it.isNotBlank()
                        amountError = null
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = amountError != null,
                    supportingText = { amountError?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = { Text("₹ ") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transaction Type",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isIncome,
                        onClick = { isIncome = true },
                        label = { Text("Income") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isIncome,
                        onClick = { isIncome = false },
                        label = { Text("Expense") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (suggestedCategories.isNotEmpty()) {
                    Text(
                        text = "Suggested Categories:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestedCategories.forEach { category ->
                            FilterChip(
                                selected = selectedCategories.contains(category),
                                onClick = {
                                    val wasSelected = selectedCategories.contains(category)
                                    selectedCategories = if (wasSelected) {
                                        selectedCategories - category
                                        categoryQuantities = categoryQuantities - category.name
                                        selectedCategories
                                    } else {
                                        selectedCategories + category
                                        categoryQuantities = categoryQuantities + (category.name to (categoryQuantities[category.name] ?: 1))
                                        selectedCategories
                                    }
                                    // Reset amount to trigger auto-calculation unless manually set
                                    if (!isAmountManuallySet || wasSelected) {
                                        isAmountManuallySet = false
                                    }
                                },
                                label = { Text(category.name) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (selectedCategories.isNotEmpty()) {
                    Text(
                        text = "Selected Categories:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    selectedCategories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${category.name}${category.defaultPrice?.let { " ($${String.format("%.2f", it)} each)" } ?: ""}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (category.defaultPrice != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val currentQty = categoryQuantities[category.name] ?: 1
                                            if (currentQty > 1) {
                                                categoryQuantities = categoryQuantities + (category.name to currentQty - 1)
                                                if (!isAmountManuallySet) {
                                                    isAmountManuallySet = false // Ensure auto-calculation
                                                }
                                            }
                                        },
                                        enabled = (categoryQuantities[category.name] ?: 1) > 1
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease Quantity")
                                    }
                                    Text(
                                        text = "${categoryQuantities[category.name] ?: 1}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            val currentQty = categoryQuantities[category.name] ?: 1
                                            categoryQuantities = categoryQuantities + (category.name to currentQty + 1)
                                            if (!isAmountManuallySet) {
                                                isAmountManuallySet = false // Ensure auto-calculation
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase Quantity")
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val filteredCategories = categories.filter {
                    it.name.contains(categoryInput, ignoreCase = true)
                }

                ExposedDropdownMenuBox(
                    expanded = categoriesExpanded,
                    onExpandedChange = { categoriesExpanded = !categoriesExpanded }
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = {
                            categoryInput = it
                            categoriesExpanded = true
                            categoryError = null
                        },
                        label = { Text("Search or Add Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        isError = categoryError != null,
                        supportingText = { categoryError?.let { Text(it) } },
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = categoriesExpanded,
                        onDismissRequest = { categoriesExpanded = false }
                    ) {
                        filteredCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(category.name)
                                        Checkbox(
                                            checked = selectedCategories.contains(category),
                                            onCheckedChange = { isChecked ->
                                                selectedCategories = if (isChecked) {
                                                    selectedCategories + category
                                                } else {
                                                    selectedCategories - category
                                                }
                                                categoryQuantities = if (isChecked) {
                                                    categoryQuantities + (category.name to (categoryQuantities[category.name] ?: 1))
                                                } else {
                                                    categoryQuantities - category.name
                                                }
                                                if (!isAmountManuallySet || selectedCategories.contains(category)) {
                                                    isAmountManuallySet = false // Trigger auto-calculation
                                                }
                                            }
                                        )
                                    }
                                },
                                onClick = {
                                    val wasSelected = selectedCategories.contains(category)
                                    selectedCategories = if (wasSelected) {
                                        selectedCategories - category
                                        categoryQuantities = categoryQuantities - category.name
                                        selectedCategories
                                    } else {
                                        selectedCategories + category
                                        categoryQuantities = categoryQuantities + (category.name to (categoryQuantities[category.name] ?: 1))
                                        selectedCategories
                                    }
                                    if (!isAmountManuallySet || wasSelected) {
                                        isAmountManuallySet = false // Trigger auto-calculation
                                    }
                                }
                            )
                        }
                        if (filteredCategories.isEmpty() && categoryInput.isNotBlank()) {
                            DropdownMenuItem(
                                text = { Text("Add \"$categoryInput\" as a new category") },
                                onClick = {
                                    newCategoryName = categoryInput
                                    showAddCategoryDialog = true
                                    categoriesExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
    // endregion

    @Composable
    fun AddCategoryDialog(
        newCategoryName: String,
        newCategoryDefaultPrice: String,
        newCategoryImageUri: Uri?,
        onNameChange: (String) -> Unit,
        onPriceChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSelectImage: () -> Unit,
        onConfirm: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = onNameChange,
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryDefaultPrice,
                        onValueChange = onPriceChange,
                        label = { Text("Default Price (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onSelectImage) {
                            Text("Select Image")
                        }
                        newCategoryImageUri?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Selected category image",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirm, enabled = newCategoryName.isNotBlank()) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    // region ===== Report Screen =====
    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("DefaultLocale")
    @Composable
    fun ReportScreen(transactions: List<Transaction>, categories: List<Category>, onBack: () -> Unit) {
        // Calculate expense data for the bar chart using default price * quantity
        val expenseData = mutableMapOf<String, Double>()
        transactions
            .filter { it.type == "Expense" }
            .forEach { transaction ->
                transaction.categories.forEach { categoryName ->
                    val category = categories.find { it.name == categoryName }
                    val quantity = transaction.categoryQuantities[categoryName] ?: 1
                    val defaultPrice = category?.defaultPrice ?: 0.0
                    expenseData[categoryName] = (expenseData[categoryName] ?: 0.0) + (defaultPrice * quantity)
                }
            }

        // Calculate category summary using default price * quantity
        val categorySummary = mutableMapOf<String, Double>()
        transactions.forEach { transaction ->
            transaction.categories.forEach { categoryName ->
                val category = categories.find { it.name == categoryName }
                val quantity = transaction.categoryQuantities[categoryName] ?: 1
                val defaultPrice = category?.defaultPrice ?: 0.0
                val amount = defaultPrice * quantity
                categorySummary[categoryName] = (categorySummary[categoryName] ?: 0.0) +
                        (if (transaction.type == "Income") amount else -amount)
            }
        }

        // Group transactions by category with amounts based on default price * quantity
        val transactionsByCategory = transactions
            .flatMap { transaction ->
                transaction.categories.map { categoryName ->
                    val category = categories.find { it.name == categoryName }
                    val quantity = transaction.categoryQuantities[categoryName] ?: 1
                    val defaultPrice = category?.defaultPrice ?: 0.0
                    val amount = defaultPrice * quantity
                    categoryName to transaction.copy(
                        amount = amount,
                        type = transaction.type
                    )
                }
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()

        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Transaction Report") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { generateAndDownloadPdf(context, categorySummary, transactionsByCategory) }) {
                        Text("Download PDF")
                    }
                    Button(onClick = { generateAndDownloadExcel(context, categorySummary, transactionsByCategory) }) {
                        Text("Download Excel")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions to report.")
                    }
                    return@Scaffold
                }

                Text("Category Summary:", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                categorySummary.forEach { (category, total) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(category, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            String.format("₹%.2f", total),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (total >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Transactions by Category:", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))

                if (transactionsByCategory.isEmpty()) {
                    Text("No transactions available.")
                } else {
                    transactionsByCategory.forEach { (category, categoryTransactions) ->
                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            items(categoryTransactions) { transaction ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (transaction.type == "Income") {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = transaction.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = formatDisplayDate(transaction.date),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            transaction.description?.let {
                                                if (it.isNotBlank()) {
                                                    Text(
                                                        text = it,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${if (transaction.type == "Income") "+" else "-"}₹${String.format("%.2f", transaction.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (transaction.type == "Income") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (expenseData.isNotEmpty()) {
                    Text("Expenses by Category (Bar Chart):", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    BarChart(barData = expenseData)
                } else {
                    Text("No expense data available for the bar chart.")
                }
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun generateAndDownloadPdf(
        context: Context,
        categorySummary: Map<String, Double>,
        transactionsByCategory: Map<String, List<Transaction>>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 12f
        var y = 20f

        canvas.drawText("Category Summary", 20f, y, paint)
        y += 20f
        categorySummary.forEach { (category, total) ->
            canvas.drawText("$category: ₹${String.format("%.2f", total)}", 20f, y, paint)
            y += 15f
        }

        y += 20f
        canvas.drawText("Transactions by Category", 20f, y, paint)
        y += 20f
        transactionsByCategory.forEach { (category, trans) ->
            canvas.drawText(category, 20f, y, paint)
            y += 15f
            trans.forEach { transaction ->
                canvas.drawText("${transaction.name} - ${transaction.date} - ₹${String.format("%.2f", transaction.amount)}", 30f, y, paint)
                y += 15f
            }
            y += 10f
        }

        pdfDocument.finishPage(page)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy_HH:mm:ss", Locale.getDefault())
        val fileName = dateFormat.format(Date()) + ".pdf"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
        }

        pdfDocument.close()
    }

    private fun generateAndDownloadExcel(
        context: Context,
        categorySummary: Map<String, Double>,
        transactionsByCategory: Map<String, List<Transaction>>
    ) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Report")

        // Category Summary
        var rowIdx = 0
        val summaryHeader = sheet.createRow(rowIdx++)
        summaryHeader.createCell(0).setCellValue("Category Summary")
        val summaryColHeader = sheet.createRow(rowIdx++)
        summaryColHeader.createCell(0).setCellValue("Category")
        summaryColHeader.createCell(1).setCellValue("Total")
        categorySummary.forEach { (category, total) ->
            val row = sheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(category)
            row.createCell(1).setCellValue(total)
        }

        // Transactions by Category
        rowIdx++
        val transHeader = sheet.createRow(rowIdx++)
        transHeader.createCell(0).setCellValue("Transactions by Category")
        transactionsByCategory.forEach { (category, trans) ->
            val catRow = sheet.createRow(rowIdx++)
            catRow.createCell(0).setCellValue(category)
            val colHeader = sheet.createRow(rowIdx++)
            colHeader.createCell(0).setCellValue("Name")
            colHeader.createCell(1).setCellValue("Date")
            colHeader.createCell(2).setCellValue("Amount")
            trans.forEach { transaction ->
                val row = sheet.createRow(rowIdx++)
                row.createCell(0).setCellValue(transaction.name)
                row.createCell(1).setCellValue(transaction.date)
                row.createCell(2).setCellValue(transaction.amount)
            }
            rowIdx++
        }

        val dateFormat = SimpleDateFormat("dd/MM/yyyy_HH:mm:ss", Locale.getDefault())
        val fileName = dateFormat.format(Date()) + ".xlsx"

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                workbook.write(outputStream)
            }
        }

        workbook.close()
    }

    @Composable
    fun BarChart(barData: Map<String, Double>) {
        val context = LocalContext.current
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            factory = {
                MPBarChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = true
                    setDrawValueAboveBar(true)
                    setDrawGridBackground(false)
                    setDrawBarShadow(false)
                }
            },
            update = { chart ->
                val entries = barData.entries.mapIndexed { index, entry ->
                    BarEntry(index.toFloat(), entry.value.toFloat())
                }
                val dataSet = BarDataSet(entries, "Expenses by Category").apply {
                    colors = ColorTemplate.MATERIAL_COLORS.toList()
                    valueTextSize = 12f
                }
                chart.data = BarData(dataSet)
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(barData.keys.toList())
                chart.xAxis.setLabelCount(barData.size, false)
                chart.xAxis.granularity = 1f
                chart.invalidate()
            }
        )
    }
    // endregion

    // region ===== Manage Categories Screen =====
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ManageCategoriesScreen(
        categories: MutableList<Category>,
        onBack: () -> Unit,
        onAddCategory: (Category) -> Unit,
        onDeleteCategory: (Category) -> Unit,
        onSaveCategories: () -> Unit
    ) {
        var showCategoryDialog by remember { mutableStateOf(false) }
        var editingCategory by remember { mutableStateOf<Category?>(null) }
        var newCategoryName by remember { mutableStateOf("") }
        var newCategoryDefaultPrice by remember { mutableStateOf("") }
        var newCategoryImageUri by remember { mutableStateOf<Uri?>(null) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            newCategoryImageUri = uri
        }

        if (showCategoryDialog) {
            CategoryDialog(
                editingCategory = editingCategory,
                newCategoryName = newCategoryName,
                newCategoryDefaultPrice = newCategoryDefaultPrice,
                newCategoryImageUri = newCategoryImageUri,
                categories = categories,
                onNameChange = { newCategoryName = it },
                onPriceChange = { newCategoryDefaultPrice = it },
                onImageUriChange = { newCategoryImageUri = it },
                onDismiss = {
                    showCategoryDialog = false
                    editingCategory = null
                    newCategoryName = ""
                    newCategoryDefaultPrice = ""
                    newCategoryImageUri = null
                },
                onSelectImage = { imagePickerLauncher.launch("image/*") },
                onConfirm = {
                    val trimmedName = newCategoryName.trim()
                    if (trimmedName.isBlank()) {
                        return@CategoryDialog
                    }
                    val newCat = Category(
                        name = trimmedName,
                        imageUri = newCategoryImageUri?.toString(),
                        defaultPrice = newCategoryDefaultPrice.toDoubleOrNull()
                    )
                    val isDuplicate = categories.any { cat ->
                        cat.name.equals(trimmedName, ignoreCase = true) &&
                                (editingCategory == null || !editingCategory!!.name.equals(trimmedName, ignoreCase = true))
                    }
                    if (isDuplicate) {
                        return@CategoryDialog
                    }
                    if (editingCategory != null) {
                        val index = categories.indexOf(editingCategory)
                        if (index != -1) {
                            categories[index] = newCat
                            onSaveCategories()
                        }
                    } else {
                        onAddCategory(newCat)
                    }
                    showCategoryDialog = false
                    editingCategory = null
                    newCategoryName = ""
                    newCategoryDefaultPrice = ""
                    newCategoryImageUri = null
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Categories") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                showCategoryDialog = true
                                editingCategory = null
                                newCategoryName = ""
                                newCategoryDefaultPrice = ""
                                newCategoryImageUri = null
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Category")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    "Categories",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                )

                if (categories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No categories available. Tap '+' to add one.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(categories) { category ->
                            CategoryCard(
                                category = category,
                                onEdit = {
                                    showCategoryDialog = true
                                    editingCategory = category
                                    newCategoryName = category.name
                                    newCategoryDefaultPrice = category.defaultPrice?.toString() ?: ""
                                    newCategoryImageUri = category.imageUri?.let { Uri.parse(it) }
                                },
                                onDelete = { onDeleteCategory(category) }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CategoryDialog(
        editingCategory: Category?,
        newCategoryName: String,
        newCategoryDefaultPrice: String,
        newCategoryImageUri: Uri?,
        categories: List<Category>,
        onNameChange: (String) -> Unit,
        onPriceChange: (String) -> Unit,
        onImageUriChange: (Uri?) -> Unit,
        onDismiss: () -> Unit,
        onSelectImage: () -> Unit,
        onConfirm: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (editingCategory != null) "Edit Category" else "Add New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = onNameChange,
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newCategoryDefaultPrice,
                        onValueChange = onPriceChange,
                        label = { Text("Default Price (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = onSelectImage) {
                            Text("Select Image")
                        }
                        if (newCategoryImageUri != null) {
                            AsyncImage(
                                model = newCategoryImageUri,
                                contentDescription = "Selected category image",
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Button(onClick = { onImageUriChange(null) }) {
                                Text("Clear")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirm, enabled = newCategoryName.isNotBlank()) {
                    Text(if (editingCategory != null) "Update" else "Add")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @SuppressLint("DefaultLocale")
    @Composable
    fun CategoryCard(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = category.imageUri,
                        contentDescription = category.name,
                        placeholder = painterResource(id = R.drawable.ic_placeholder_category),
                        error = painterResource(id = R.drawable.ic_placeholder_category),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        category.defaultPrice?.let {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Default Price: ₹${String.format("%.2f", it)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Category")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Category")
                    }
                }
            }
        }
    }
    // endregion

    @Preview(showBackground = true, device = "id:pixel_4", name = "Pixel 4")
    @Preview(showBackground = true, device = "id:pixel_9a", name = "Pixel 9a")
    @Preview(showBackground = true, device = "id:galaxy_s21", name = "Samsung Galaxy S21")
    @Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=394", name = "Realme Narzo 70 Pro 5G")
    @Preview(showBackground = true, device = "id:pixel_tablet", name = "Pixel Tablet")
    @Preview(showBackground = true, device = "id:vivo_v21e_5g", name = "Vivo v21e 5g")
    @Composable
    fun DefaultPreview() {
        HisabappTheme {
            MainScreenContent(
                transactions = listOf(
                    Transaction(
                        id = 1L,
                        name = "Salary",
                        mobileNumber = null,
                        amount = 1000.0,
                        type = "Income",
                        categories = listOf("Salary"),
                        categoryQuantities = mapOf("Salary" to 1),
                        description = null,
                        date = "2025-10-12",
                        categoryImageUris = listOf(null)
                    ),
                    Transaction(
                        id = 2L,
                        name = "Groceries",
                        mobileNumber = null,
                        amount = 100.0,
                        type = "Expense",
                        categories = listOf("Food & Dining", "Shopping"),
                        categoryQuantities = mapOf("Food & Dining" to 2, "Shopping" to 1),
                        description = "Weekly shopping",
                        date = "2025-10-11",
                        categoryImageUris = listOf(null, null)
                    ),
                    Transaction(
                        id = 3L,
                        name = "Bus Ticket",
                        mobileNumber = null,
                        amount = 20.0,
                        type = "Expense",
                        categories = listOf("Transportation"),
                        categoryQuantities = mapOf("Transportation" to 1),
                        description = "Daily commute",
                        date = "2025-10-10",
                        categoryImageUris = listOf(null)
                    )
                ),
                onAddTransaction = {},
                onEditTransaction = {},
                onViewReport = {},
                onManageCategories = {}
            )
        }
    }

    @Preview(showBackground = true, device = "id:pixel_4", name = "Pixel 4")
    @Preview(showBackground = true, device = "id:pixel_9a", name = "Pixel 9a")
    @Preview(showBackground = true, device = "id:galaxy_s21", name = "Samsung Galaxy S21")
    @Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=394", name = "Realme Narzo 70 Pro 5G")
    @Preview(showBackground = true, device = "id:pixel_tablet", name = "Pixel Tablet")
    @Preview(showBackground = true, device = "id:vivo_v21e_5g", name = "Vivo v21e 5g")
    @Composable
    fun AddTransactionScreenPreview() {
        HisabappTheme {
            AddTransactionScreen(
                categories = listOf(
                    Category("Food & Dining", defaultPrice = 50.0),
                    Category("Transportation", defaultPrice = 20.0),
                    Category("Shopping")
                ),
                onBack = {},
                onSave = { _, _ -> }
            )
        }
    }

    @Preview(showBackground = true, device = "id:pixel_4", name = "Pixel 4")
    @Preview(showBackground = true, device = "id:pixel_9a", name = "Pixel 9a")
    @Preview(showBackground = true, device = "id:galaxy_s21", name = "Samsung Galaxy S21")
    @Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=394", name = "Realme Narzo 70 Pro 5G")
    @Preview(showBackground = true, device = "id:pixel_tablet", name = "Pixel Tablet")
    @Preview(showBackground = true, device = "id:vivo_v21e_5g", name = "Vivo v21e 5g")
    @Composable
    fun EditTransactionScreenPreview() {
        HisabappTheme {
            EditTransactionScreen(
                transaction = Transaction(
                    id = 1L,
                    name = "Groceries",
                    mobileNumber = "1234567890",
                    amount = 100.0,
                    type = "Expense",
                    categories = listOf("Food & Dining", "Shopping"),
                    categoryQuantities = mapOf("Food & Dining" to 2, "Shopping" to 1),
                    description = "Weekly shopping",
                    date = "2025-10-11",
                    categoryImageUris = listOf(null, null)
                ),
                categories = listOf(
                    Category("Food & Dining", defaultPrice = 50.0),
                    Category("Transportation", defaultPrice = 20.0),
                    Category("Shopping")
                ),
                onBack = {},
                onSave = { _, _ -> }
            )
        }
    }

    @Preview(showBackground = true, device = "id:pixel_4", name = "Pixel 4")
    @Preview(showBackground = true, device = "id:pixel_9a", name = "Pixel 9a")
    @Preview(showBackground = true, device = "id:galaxy_s21", name = "Samsung Galaxy S21")
    @Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=394", name = "Realme Narzo 70 Pro 5G")
    @Preview(showBackground = true, device = "id:pixel_tablet", name = "Pixel Tablet")
    @Preview(showBackground = true, device = "id:vivo_v21e_5g", name = "Vivo v21e 5g")
    @Composable
    fun ManageCategoriesScreenPreview() {
        HisabappTheme {
            ManageCategoriesScreen(
                categories = mutableListOf(
                    Category("Food & Dining", defaultPrice = 50.0),
                    Category("Transportation", defaultPrice = 20.0),
                    Category("Shopping")
                ),
                onBack = {},
                onAddCategory = {},
                onDeleteCategory = {},
                onSaveCategories = {}
            )
        }
    }

    @Preview(showBackground = true, device = "id:pixel_4", name = "Pixel 4")
    @Preview(showBackground = true, device = "id:pixel_9a", name = "Pixel 9a")
    @Preview(showBackground = true, device = "id:galaxy_s21", name = "Samsung Galaxy S21")
    @Preview(showBackground = true, device = "spec:width=393dp,height=852dp,dpi=394", name = "Realme Narzo 70 Pro 5G")
    @Preview(showBackground = true, device = "id:pixel_tablet", name = "Pixel Tablet")
    @Preview(showBackground = true, device = "id:vivo_v21e_5g", name = "Vivo v21e 5g")
    @Composable
    fun ReportScreenPreview() {
        HisabappTheme {
            ReportScreen(
                transactions = listOf(
                    Transaction(
                        id = 1L,
                        name = "Salary",
                        mobileNumber = null,
                        amount = 1000.0,
                        type = "Income",
                        categories = listOf("Salary"),
                        categoryQuantities = mapOf("Salary" to 1),
                        description = null,
                        date = "2025-10-12",
                        categoryImageUris = listOf(null)
                    ),
                    Transaction(
                        id = 2L,
                        name = "Groceries",
                        mobileNumber = null,
                        amount = 100.0,
                        type = "Expense",
                        categories = listOf("Food & Dining", "Shopping"),
                        categoryQuantities = mapOf("Food & Dining" to 2, "Shopping" to 1),
                        description = "Weekly shopping",
                        date = "2025-10-11",
                        categoryImageUris = listOf(null, null)
                    ),
                    Transaction(
                        id = 3L,
                        name = "Bus Ticket",
                        mobileNumber = null,
                        amount = 20.0,
                        type = "Expense",
                        categories = listOf("Transportation"),
                        categoryQuantities = mapOf("Transportation" to 1),
                        description = "Daily commute",
                        date = "2025-10-10",
                        categoryImageUris = listOf(null)
                    )
                ),
                categories = listOf(),
                onBack = {}
            )
        }
    }
}