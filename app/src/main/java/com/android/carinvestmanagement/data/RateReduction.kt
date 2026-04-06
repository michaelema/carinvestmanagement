package com.android.carinvestmanagement.data

data class RateReduction(
    val id: String,
    val date: String,
    val rateReduction: Int,
    val description: String,
    val serviceFeeReduction: Int
)
