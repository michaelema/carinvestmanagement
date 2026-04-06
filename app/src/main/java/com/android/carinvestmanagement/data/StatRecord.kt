package com.android.carinvestmanagement.data

data class StatRecord(
    val date: String,
    val amount: Int,
    val reduction: Int,
    val dayOfWeek: String,
    val rentRecordId: String,
    val rateReductionId: String
)

data class VehicleStats(
    val income: List<StatRecord>,
    val management: List<StatRecord>
)
