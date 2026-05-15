package com.android.carinvestmanagement.data

data class Person(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val patronymic: String = "",
    val dateOfBirth: String? = null,
    val phoneNumber: String = "",
    val address: String = "",
    val documentNumber: String = "",
    val dateOfIssue: String = "",
    val issuingAuthority: String = "",
    val documentScan: String = "",
    val ownerDescription: String = "",
    val messenger: Map<String, String> = emptyMap()
)
