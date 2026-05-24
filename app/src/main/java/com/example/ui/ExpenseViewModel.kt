package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.ExpenseRepository
import com.example.data.NetworkService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Observe local SQLite entries reactively
    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Sync States
    private val _sheetsUrl = MutableStateFlow(repository.getSheetsUrl())
    val sheetsUrl: StateFlow<String> = _sheetsUrl.asStateFlow()

    private val _monthlyBudget = MutableStateFlow(repository.getMonthlyBudget())
    val monthlyBudget: StateFlow<Double> = _monthlyBudget.asStateFlow()

    private val _currency = MutableStateFlow(repository.getCurrency())
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionResult = MutableStateFlow<String?>(null)
    val connectionResult: StateFlow<String?> = _connectionResult.asStateFlow()

    fun updateSheetsUrl(url: String) {
        repository.saveSheetsUrl(url)
        _sheetsUrl.value = url
    }

    fun updateMonthlyBudget(budget: Double) {
        repository.saveMonthlyBudget(budget)
        _monthlyBudget.value = budget
    }

    fun updateCurrency(curr: String) {
        repository.saveCurrency(curr)
        _currency.value = curr
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun clearConnectionResult() {
        _connectionResult.value = null
    }

    fun addExpense(
        amount: Double,
        category: String,
        description: String,
        paymentMethod: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val newExpense = Expense(
                amount = amount,
                category = category,
                description = description,
                paymentMethod = paymentMethod,
                date = date,
                synced = false // Marked as unsynced initially
            )
            repository.insertExpense(newExpense)
            // Effortlessly auto-sync export if Google Sheet URL is configured! Optional micro-convenience
            if (sheetsUrl.value.isNotBlank()) {
                syncExport()
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteLocalDatabase() {
        viewModelScope.launch {
            repository.clearLocal()
        }
    }

    fun testConnection(url: String) {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionResult.value = null
            val result = repository.testSheetsConnection(url)
            _isTestingConnection.value = false
            if (result.isSuccess) {
                _connectionResult.value = "Success: ${result.getOrNull()}"
                updateSheetsUrl(url) // Auto-save if connection succeeds!
            } else {
                _connectionResult.value = "Failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun syncExport() {
        if (sheetsUrl.value.isBlank()) {
            _syncMessage.value = "Sync skipped: Sheets App Script URL not set"
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncExport()
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Exported unsynced expenses to Google Sheets!"
            } else {
                _syncMessage.value = "Export failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun syncImport() {
        if (sheetsUrl.value.isBlank()) {
            _syncMessage.value = "Import failed: Sheets App Script URL not set"
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncImport()
            _isSyncing.value = false
            if (result.isSuccess) {
                val count = result.getOrNull() ?: 0
                _syncMessage.value = "Imported $count entries from Google Sheets successfully!"
            } else {
                _syncMessage.value = "Import failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun getLastSyncTime(): Long {
        return repository.getLastSyncTime()
    }
}

class ExpenseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val networkService = NetworkService()
            val repository = ExpenseRepository(database.expenseDao(), networkService, context)
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
