package com.android.carinvestmanagement.data

data class Vehicle(
    val id: String,
    val name: String,
    val plateNumber: String,
    val status: Boolean,
    val revenue: String,
    val purchasePrice: Int,
    val entranceFee: Int,
    val rateType: String = "",
    val rentPrice: Int = 0,
    val serviceFee: Int = 0,
    val leaserName: String = "",
    val leaserPhone: String = ""
)
