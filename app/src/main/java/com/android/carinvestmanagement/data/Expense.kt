package com.android.carinvestmanagement.data

data class Expense(
    val id: String,
    val title: String,
    val date: String,
    val amount: Int,
    val category: String,
    val document: String? = null
)
