package com.android.carinvestmanagement.data

data class DashboardStatRecord(
    val carId: String,
    val date: String,
    val daysInPeriod: Int,
    val rent: Int,
    val expenses: Int,
    val serviceFees: Int,
    val rateReductions: Int,
    val serviceFeeReductions: Int,
    val totalProfit: Int
)
