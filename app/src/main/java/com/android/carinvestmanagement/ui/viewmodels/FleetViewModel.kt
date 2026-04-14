package com.android.carinvestmanagement.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.carinvestmanagement.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FleetViewModel(val repository: VehicleRepository = VehicleRepository()) : ViewModel() {
    private val _uiState = MutableStateFlow<FleetUiState>(FleetUiState.Loading)
    val uiState: StateFlow<FleetUiState> = _uiState.asStateFlow()

    private val _selectedVehicle = MutableStateFlow<Vehicle?>(null)
    val selectedVehicle: StateFlow<Vehicle?> = _selectedVehicle.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isExpensesLoading = MutableStateFlow(false)
    val isExpensesLoading: StateFlow<Boolean> = _isExpensesLoading.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _selectedVehicleRate = MutableStateFlow<Rate?>(null)
    val selectedVehicleRate: StateFlow<Rate?> = _selectedVehicleRate.asStateFlow()

    private val _isRateLoading = MutableStateFlow(false)
    val isRateLoading: StateFlow<Boolean> = _isRateLoading.asStateFlow()

    private val _incomeStats = MutableStateFlow<List<StatRecord>>(emptyList())
    val incomeStats: StateFlow<List<StatRecord>> = _incomeStats.asStateFlow()

    private val _managementStats = MutableStateFlow<List<StatRecord>>(emptyList())
    val managementStats: StateFlow<List<StatRecord>> = _managementStats.asStateFlow()

    private val _isStatsLoading = MutableStateFlow(false)
    val isStatsLoading: StateFlow<Boolean> = _isStatsLoading.asStateFlow()

    private val _totalStats = MutableStateFlow<TotalStats?>(null)
    val totalStats: StateFlow<TotalStats?> = _totalStats.asStateFlow()

    private val _incomeDetailRate = MutableStateFlow<Rate?>(null)
    val incomeDetailRate: StateFlow<Rate?> = _incomeDetailRate.asStateFlow()

    private val _incomeDetailReduction = MutableStateFlow<RateReduction?>(null)
    val incomeDetailReduction: StateFlow<RateReduction?> = _incomeDetailReduction.asStateFlow()

    private val _isIncomeDetailLoading = MutableStateFlow(false)
    val isIncomeDetailLoading: StateFlow<Boolean> = _isIncomeDetailLoading.asStateFlow()

    // Dashboard State
    private val _dashboardStats = MutableStateFlow<List<DashboardStatRecord>>(emptyList())
    val dashboardStats: StateFlow<List<DashboardStatRecord>> = _dashboardStats.asStateFlow()

    private val _isDashboardLoading = MutableStateFlow(false)
    val isDashboardLoading: StateFlow<Boolean> = _isDashboardLoading.asStateFlow()

    init {
        fetchVehicles()
    }

    fun fetchVehicles() {
        viewModelScope.launch {
            _uiState.value = FleetUiState.Loading
            val vehicles = repository.getVehicles()
            _uiState.value = if (vehicles.isEmpty()) {
                FleetUiState.Error("No vehicles found or error occurred")
            } else {
                FleetUiState.Success(vehicles)
            }
        }
    }

    fun getVehicleById(id: String) {
        val currentState = _uiState.value
        if (currentState is FleetUiState.Success) {
            _selectedVehicle.value = currentState.vehicles.find { it.id == id }
        }
    }

    fun fetchExpenses(vehicleId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isExpensesLoading.value = true
            val expenseList = repository.getExpensesByCar(vehicleId, startDate, endDate)
            _expenses.value = expenseList
            _isExpensesLoading.value = false
        }
    }

    fun fetchRate(vehicleId: String) {
        viewModelScope.launch {
            _isRateLoading.value = true
            _selectedVehicleRate.value = repository.getRate(vehicleId)
            _isRateLoading.value = false
        }
    }

    fun fetchStats(vehicleId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isStatsLoading.value = true
            val stats = repository.getStats(vehicleId, startDate, endDate)
            _incomeStats.value = stats.income
            _managementStats.value = stats.management
            _isStatsLoading.value = false
        }
    }

    fun fetchTotalStats(vehicleId: String) {
        viewModelScope.launch {
            _totalStats.value = repository.getTotalStats(vehicleId)
        }
    }

    fun generateAndRefreshStats(vehicleId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isStatsLoading.value = true
            repository.generateDailyStats(vehicleId, startDate, endDate)
            val stats = repository.getStats(vehicleId, startDate, endDate)
            _incomeStats.value = stats.income
            _managementStats.value = stats.management
            fetchTotalStats(vehicleId)
            _isStatsLoading.value = false
        }
    }

    fun fetchIncomeDetails(rentRecordId: String, rateReductionId: String) {
        viewModelScope.launch {
            _isIncomeDetailLoading.value = true
            
            // Clear previous states
            _incomeDetailRate.value = null
            _incomeDetailReduction.value = null
            
            if (rentRecordId.isNotEmpty() && rentRecordId != "null" && rentRecordId != "none") {
                _incomeDetailRate.value = repository.getRateById(rentRecordId)
            }
            
            if (rateReductionId.isNotEmpty() && rateReductionId != "null" && rateReductionId != "none") {
                _incomeDetailReduction.value = repository.getRateReduction(rateReductionId)
            }

            _isIncomeDetailLoading.value = false
        }
    }

    fun deleteReductionAndRefresh(vehicleId: String, reduction: RateReduction, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val success = repository.deleteRateReduction(reduction.id)
            if (success) {
                repository.generateDailyStats(vehicleId, reduction.date, reduction.date)
                onSuccess()
            }
            _isActionLoading.value = false
        }
    }

    fun addExpense(
        vehicleId: String,
        type: String,
        amount: Int,
        description: String,
        date: String,
        documentName: String? = null,
        fileBase64: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val success = repository.createExpense(
                vehicleId, type, amount, description, date, documentName, fileBase64
            )
            _isActionLoading.value = false
            if (success) {
                onSuccess()
            } else {
                onError("Ошибка при сохранении расхода")
            }
        }
    }

    fun deleteExpenseAndRefresh(expenseId: String, vehicleId: String, startDate: String, endDate: String) {
        viewModelScope.launch {
            _isExpensesLoading.value = true
            val success = repository.deleteExpense(expenseId)
            if (success) {
                fetchExpenses(vehicleId, startDate, endDate)
                fetchTotalStats(vehicleId)
            }
            _isExpensesLoading.value = false
        }
    }

    fun changeTariff(
        rentRecordId: String,
        rateId: String,
        rateDate: String,
        freeDay: String,
        rateDescription: String,
        rateType: String,
        rentPrice: Int,
        serviceFee: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val success = repository.createOrUpdateRateRecord(
                rentRecordId, rateId, rateDate, freeDay, rateDescription, rateType, rentPrice, serviceFee
            )
            _isActionLoading.value = false
            if (success) {
                fetchRate("")
                onSuccess()
            } else {
                onError("Ошибка при сохранении тарифа")
            }
        }
    }

    fun createReductionAndRefresh(
        vehicleId: String,
        date: String,
        rateReduction: Int,
        serviceFeeReduction: Int,
        description: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isActionLoading.value = true
            val success = repository.createRateReduction(
                vehicleId, date, rateReduction, serviceFeeReduction, description
            )
            if (success) {
                repository.generateDailyStats(vehicleId, date, date)
                onSuccess()
            }
            _isActionLoading.value = false
        }
    }

    fun fetchDashboardStats(carId: String?, period: String, startDate: String?, endDate: String?) {
        viewModelScope.launch {
            _isDashboardLoading.value = true
            _dashboardStats.value = repository.getDashboardStats(carId, period, startDate, endDate)
            _isDashboardLoading.value = false
        }
    }
}

sealed class FleetUiState {
    object Loading : FleetUiState()
    data class Success(val vehicles: List<Vehicle>) : FleetUiState()
    data class Error(val message: String) : FleetUiState()
}
