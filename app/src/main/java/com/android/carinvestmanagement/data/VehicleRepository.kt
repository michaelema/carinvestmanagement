package com.android.carinvestmanagement.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

data class TotalStats(
    val revenue: Int,
    val expenses: Int,
    val profit: Int
)

class VehicleRepository {
    private val functions = FirebaseFunctions.getInstance()

    suspend fun getVehicles(): List<Vehicle> {
        return try {
            val result = functions
                .getHttpsCallable("get_all_cars_oncall")
                .call()
                .await()

            @Suppress("UNCHECKED_CAST")
            val carsRootElement = result.data as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val data = carsRootElement["cars"] as? List<Map<String, Any>> ?: emptyList()
            
            data.map { map ->
                val rawRevenue = map["revenue"]
                val revenueStr = when (rawRevenue) {
                    is Number -> rawRevenue.toInt().toString()
                    is String -> rawRevenue
                    else -> "0"
                }
                
                Vehicle(
                    id = map["id"] as? String ?: "",
                    name = (map["carBrand"] as? String ?: "Unknown") + " " + (map["model"] as? String ?: "Unknown"),
                    plateNumber = map["registrationNumber"] as? String ?: "",
                    status = map["isActive"] as? Boolean ?: false,
                    revenue = revenueStr,
                    purchasePrice = (map["purchasePrice"] as? Number)?.toInt() ?: 0,
                    entranceFee = (map["entranceFee"] as? Number)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getExpensesByCar(vehicleId: String, startDate: String, endDate: String): List<Expense> {
        return try {
            val result = functions
                .getHttpsCallable("get_expense_by_car_oncall")
                .call(mapOf(
                    "car_id" to vehicleId,
                    "start_date" to startDate,
                    "end_date" to endDate
                ))
                .await()

            @Suppress("UNCHECKED_CAST")
            val expensesRootElement = result.data as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val data = expensesRootElement["expenses"] as? List<Map<String, Any>> ?: emptyList()
            
            data.map { map ->
                Expense(
                    id = map["id"] as? String ?: "",
                    category = map["type"] as? String ?: "Unknown",
                    title = map["description"] as? String ?: "Unknown Expense",
                    date = map["date"] as? String ?: "",
                    amount = (map["cost"] as? Number)?.toInt() ?: 0,
                    document = map["document"] as? String
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createExpense(
        vehicleId: String,
        type: String,
        amount: Int,
        description: String,
        date: String,
        documentName: String? = null,
        fileBase64: String? = null
    ): Boolean {
        return try {
            val params = mutableMapOf<String, Any>(
                "car_id" to vehicleId,
                "type" to type,
                "cost" to amount,
                "description" to description,
                "date" to date
            )
            
            if (documentName != null) params["document"] = documentName
            if (fileBase64 != null) params["file_base64"] = fileBase64

            functions
                .getHttpsCallable("create_expense_oncall")
                .call(params)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteExpense(expenseId: String): Boolean {
        return try {
            functions
                .getHttpsCallable("delete_expense_oncall")
                .call(mapOf("expense_id" to expenseId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getRate(vehicleId: String): Rate? {
        return try {
            val result = functions
                .getHttpsCallable("get_rate_oncall")
                .call(mapOf("car_id" to vehicleId))
                .await()

            createRate(result)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRateById(rentRecordId: String): Rate? {
        if (rentRecordId.isEmpty() || rentRecordId == "null" || rentRecordId == "none") return null
        return try {
            val result = functions
                .getHttpsCallable("get_rate_by_id_oncall")
                .call(mapOf("rent_record_id" to rentRecordId))
                .await()

            createRate(result)
        } catch (e: Exception) {
            null
        }
    }

    private fun createRate(result: HttpsCallableResult): Rate {
        @Suppress("UNCHECKED_CAST")
        val responseRootElement = result.data as? Map<String, Any> ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rate = responseRootElement["rate"] as? Map<String, Any> ?: emptyMap()

        @Suppress("UNCHECKED_CAST")
        val rentRecord = responseRootElement["rent_record"] as? Map<String, Any> ?: emptyMap()

        return Rate(
            startDate = rentRecord["startDate"] as? String ?: "",
            rentPrice = (rate["rentPrice"] as? Number)?.toInt() ?: 0,
            serviceFee = (rate["serviceFee"] as? Number)?.toInt() ?: 0,
            rateDescription = rate["rateDescription"] as? String ?: "",
            rateType = rate["rateType"] as? String ?: "",
            freeDay = (rate["freeDay"] as? Number)?.toString() ?: "",
            rateId = rate["id"] as? String ?: "",
            rentRecordId = rentRecord["id"] as? String ?: ""
        )
    }

    suspend fun getRateReduction(rateReductionId: String): RateReduction? {
        if (rateReductionId.isEmpty() || rateReductionId == "null" || rateReductionId == "none") return null
        return try {
            val result = functions
                .getHttpsCallable("get_rate_reduction_oncall")
                .call(mapOf("rate_reduction_id" to rateReductionId))
                .await()

            @Suppress("UNCHECKED_CAST")
            val responseRootElement = result.data as? Map<String, Any> ?: emptyMap()

            @Suppress("UNCHECKED_CAST")
            val data = responseRootElement["rateReduction"] as? Map<String, Any> ?: emptyMap()
            if (data.isEmpty()) return null

            RateReduction(
                id = data["id"] as? String ?: "",
                date = data["rateReductionDate"] as? String ?: "",
                rateReduction = (data["rateReduction"] as? Number)?.toInt() ?: 0,
                serviceFeeReduction = (data["serviceFeeReduction"] as? Number)?.toInt() ?: 0,
                description = data["description"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createRateReduction(
        vehicleId: String,
        date: String,
        rateReduction: Int,
        serviceFeeReduction: Int,
        description: String
    ): Boolean {
        return try {
            val inputSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            val formattedDate = try {
                val parsedDate = inputSdf.parse(date)
                if (parsedDate != null) isoSdf.format(parsedDate) else date
            } catch (e: Exception) {
                date
            }

            functions
                .getHttpsCallable("create_rate_reduction_oncall")
                .call(mapOf(
                    "car_id" to vehicleId,
                    "rate_reduction_date" to formattedDate,
                    "rate_reduction" to rateReduction,
                    "service_fee_reduction" to serviceFeeReduction,
                    "description" to description
                ))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteRateReduction(rateReductionId: String): Boolean {
        return try {
            functions
                .getHttpsCallable("delete_rate_reduction_oncall")
                .call(mapOf("rate_reduction_id" to rateReductionId))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createOrUpdateRateRecord(
        rentRecordId: String,
        rateId: String,
        rateDate: String,
        freeDay: String,
        rateDescription: String,
        rateType: String,
        rentPrice: Int,
        serviceFee: Int
    ): Boolean {
        return try {
            functions
                .getHttpsCallable("create_or_update_rate_record_oncall")
                .call(mapOf(
                    "rent_record_id" to rentRecordId,
                    "rate_id" to rateId,
                    "rate_date" to rateDate,
                    "freeDay" to freeDay,
                    "rateDescription" to rateDescription,
                    "rateType" to rateType,
                    "rentPrice" to rentPrice,
                    "serviceFee" to serviceFee
                ))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStats(vehicleId: String, startDate: String, endDate: String): VehicleStats {
        return try {
            val result = functions
                .getHttpsCallable("get_stats_oncall")
                .call(mapOf(
                    "car_id" to vehicleId,
                    "start_date" to startDate,
                    "end_date" to endDate
                ))
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultList = result.data as? List<List<Any>> ?: emptyList()

            val incomeRecords = mutableListOf<StatRecord>()
            val managementRecords = mutableListOf<StatRecord>()

            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val sdfDayOfWeek = SimpleDateFormat("EEE", Locale("ru"))

            for (item in resultList) {
                if (item.size < 5) continue
                
                val dateStr = item[1] as? String ?: ""
                val incomeAmount = (item[2] as? Number)?.toInt() ?: 0
                val managementAmount = (item[4] as? Number)?.toInt() ?: 0
                
                val rateReduction = if (item.size > 5) (item[5] as? Number)?.toInt() ?: 0 else 0
                val serviceFeeReduction = if (item.size > 6) (item[6] as? Number)?.toInt() ?: 0 else 0
                val rentRecordId = if (item.size > 7) (item[7] as? String).let { if (it.isNullOrEmpty()) "none" else it } else "none"
                val rateReductionId = if (item.size > 8) (item[8] as? String).let { if (it.isNullOrEmpty()) "none" else it } else "none"
                
                val dayOfWeek = try {
                    val date = sdfInput.parse(dateStr)
                    if (date != null) {
                        sdfDayOfWeek.format(date).replaceFirstChar { it.uppercase() }
                    } else ""
                } catch (e: Exception) {
                    ""
                }

                incomeRecords.add(StatRecord(date = dateStr, amount = incomeAmount, reduction = rateReduction, dayOfWeek = dayOfWeek, rentRecordId, rateReductionId))
                managementRecords.add(StatRecord(
                    date = dateStr,
                    amount = managementAmount,
                    reduction = serviceFeeReduction,
                    dayOfWeek = dayOfWeek,
                    rentRecordId,
                    rateReductionId
                ))
            }

            VehicleStats(
                income = incomeRecords,
                management = managementRecords
            )
        } catch (e: Exception) {
            VehicleStats(emptyList(), emptyList())
        }
    }

    suspend fun generateDailyStats(vehicleId: String, startDate: String, endDate: String): Boolean {
        return try {
            val inputSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            val formattedStart = try {
                val d = inputSdf.parse(startDate)
                if (d != null) isoSdf.format(d) else startDate
            } catch (e: Exception) { startDate }

            val formattedEnd = try {
                val d = inputSdf.parse(endDate)
                if (d != null) isoSdf.format(d) else endDate
            } catch (e: Exception) { endDate }

            functions
                .getHttpsCallable("generate_daily_stats_oncall")
                .call(mapOf(
                    "car_id" to vehicleId,
                    "start_date" to formattedStart,
                    "end_date" to formattedEnd
                ))
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTotalStats(vehicleId: String): TotalStats? {
        return try {
            val result = functions
                .getHttpsCallable("get_total_stats_oncall")
                .call(mapOf("car_id" to vehicleId))
                .await()

            @Suppress("UNCHECKED_CAST")
            val resultList = result.data as? List<List<Number>> ?: emptyList()
            if (resultList.isEmpty()) return null
            val list = resultList[0]
            if (list.size < 5) return null

            val val1 = list[0].toInt()
            val val2 = list[1].toInt()
            val val3 = list[2].toInt()
            val val4 = list[3].toInt()
            val val5 = list[4].toInt()

            val revenue = val1 - val3 - val4 + val5
            val expenses = val2
            val profit = revenue - expenses

            TotalStats(revenue, expenses, profit)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getDashboardStats(carId: String?, period: String, startDate: String?, endDate: String?): List<DashboardStatRecord> {
        return try {
            val params = mutableMapOf<String, Any>(
                "period" to period
            )
            if (carId != null) params["car_id"] = carId
            if (startDate != null) params["start_date"] = startDate
            if (endDate != null) params["end_date"] = endDate

            val result = functions
                .getHttpsCallable("get_total_stats_by_period_oncall")
                .call(params)
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? List<List<Any>> ?: emptyList()

            data.map { item ->
                DashboardStatRecord(
                    carId = item[0] as? String ?: "",
                    date = item[1] as? String ?: "",
                    daysInPeriod = (item[2] as? Number)?.toInt() ?: 0,
                    rent = (item[3] as? Number)?.toInt() ?: 0,
                    expenses = (item[4] as? Number)?.toInt() ?: 0,
                    serviceFees = (item[5] as? Number)?.toInt() ?: 0,
                    rateReductions = (item[6] as? Number)?.toInt() ?: 0,
                    serviceFeeReductions = (item[7] as? Number)?.toInt() ?: 0,
                    totalProfit = (item[8] as? Number)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
