package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val networkService: NetworkService,
    context: Context
) {
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SHEETS_URL = "sheets_url"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_CURRENCY = "currency_setting"
    }

    // SharedPreferences Accessors
    fun getCurrency(): String {
        return sharedPrefs.getString(KEY_CURRENCY, "INR") ?: "INR"
    }

    fun saveCurrency(currency: String) {
        sharedPrefs.edit().putString(KEY_CURRENCY, currency).apply()
    }

    fun getSheetsUrl(): String {
        return sharedPrefs.getString(KEY_SHEETS_URL, "") ?: ""
    }

    fun saveSheetsUrl(url: String) {
        sharedPrefs.edit().putString(KEY_SHEETS_URL, url.trim()).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPrefs.getFloat(KEY_MONTHLY_BUDGET, 1000f).toDouble()
    }

    fun saveMonthlyBudget(budget: Double) {
        sharedPrefs.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    fun getLastSyncTime(): Long {
        return sharedPrefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun saveLastSyncTime(time: Long) {
        sharedPrefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }

    // Local DB Observables & Actions
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun insertSyncExpenses(expenses: List<Expense>) {
        expenseDao.insertExpenses(expenses)
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun clearLocal() {
        expenseDao.clearAllExpenses()
    }

    // Two-Way Sync Operations
    suspend fun testSheetsConnection(url: String): Result<String> {
        return networkService.testConnection(url)
    }

    /**
     * Pushes all local unsynced expenses to Google Sheets and updates local state as 'synced'.
     */
    suspend fun syncExport(): Result<String> {
        val url = getSheetsUrl()
        if (url.isBlank()) {
            return Result.failure(Exception("Configure Google Sheets Web App URL in settings first."))
        }

        val unsynced = expenseDao.getUnsyncedExpenses()
        if (unsynced.isEmpty()) {
            return Result.success("Local records already synced with the Sheet.")
        }

        val result = networkService.exportExpenses(url, unsynced)
        if (result.isSuccess) {
            // Update the locally stored sync status
            for (expense in unsynced) {
                expenseDao.updateSyncStatus(expense.id, true)
            }
            saveLastSyncTime(System.currentTimeMillis())
        }
        return result
    }

    /**
     * Pulls full transaction records from Google Sheets, replaces/merges into local Room DB.
     */
    suspend fun syncImport(): Result<Int> {
        val url = getSheetsUrl()
        if (url.isBlank()) {
            return Result.failure(Exception("Configure Google Sheets Web App URL in settings first."))
        }

        val result = networkService.importExpenses(url)
        return if (result.isSuccess) {
            val remoteExpenses = result.getOrNull() ?: emptyList()
            if (remoteExpenses.isNotEmpty()) {
                // To perform a clean sync import, we insert/overwrite remote entries
                expenseDao.insertExpenses(remoteExpenses)
            }
            saveLastSyncTime(System.currentTimeMillis())
            Result.success(remoteExpenses.size)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Failed to pull rows"))
        }
    }
}
