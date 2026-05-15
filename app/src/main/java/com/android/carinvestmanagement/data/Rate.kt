package com.android.carinvestmanagement.data

data class Rate(
    val startDate: String,
    val rentPrice: Int,
    val serviceFee: Int,
    val rateDescription: String,
    val freeDay: String,
    val rateType: String,
    val rateId: String,
    val rentRecordId: String,
    val leaser: String
)
