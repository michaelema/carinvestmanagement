package com.android.carinvestmanagement.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.android.carinvestmanagement.data.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class VehicleReportData(
    val vehicle: Vehicle,
    val rate: Rate?,
    val totalStats: TotalStats?,
    val incomeStats: List<StatRecord>,
    val managementStats: List<StatRecord>,
    val expenses: List<Expense>
)

object VehicleReportGenerator {

    fun generateCommonReport(
        context: Context,
        reportsData: List<VehicleReportData>,
        startDate: Date,
        endDate: Date
    ): File? {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 18f
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
        }
        val textPaint = Paint().apply {
            typeface = Typeface.DEFAULT
            textSize = 10f
        }
        val boldTextPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
        }
        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }

        val dotSdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val isoSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }

        var y = 40f
        val margin = 40f

        fun checkNewPage(needed: Float) {
            if (y + needed > pageHeight - margin) {
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
        }

        // Main Title
        canvas.drawText("Общий отчет по автопарку", margin, y, titlePaint)
        y += 25f
        canvas.drawText("Период: ${dotSdf.format(startDate)} - ${dotSdf.format(endDate)}", margin, y, textPaint)
        y += 40f

        // SUMMARY TABLE
        checkNewPage(60f)
        canvas.drawText("СВОДНАЯ ТАБЛИЦА", margin, y, headerPaint)
        y += 20f

        // Header
        canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
        canvas.drawText("Автомобиль", margin, y, boldTextPaint)
        canvas.drawText("Аренда", margin + 140f, y, boldTextPaint)
        canvas.drawText("Управление", margin + 210f, y, boldTextPaint)
        canvas.drawText("Расходы", margin + 280f, y, boldTextPaint)
        canvas.drawText("Прибыль собств.", margin + 360f, y, boldTextPaint)
        y += 5f
        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
        y += 15f

        var grandTotalIncome = 0
        var grandTotalService = 0
        var grandTotalExpenses = 0
        var grandTotalOwnerRevenue = 0

        reportsData.forEach { data ->
            checkNewPage(20f)
            
            var vehicleIncome = 0
            var vehicleService = 0
            val allDates = (data.incomeStats.map { it.date } + data.managementStats.map { it.date }).distinct()
            allDates.forEach { dateStr ->
                val inc = data.incomeStats.find { it.date == dateStr }
                val mgmt = data.managementStats.find { it.date == dateStr }
                vehicleIncome += (inc?.amount ?: 0) - (inc?.reduction ?: 0)
                vehicleService += (mgmt?.amount ?: 0) - (mgmt?.reduction ?: 0)
            }
            
            val vehicleExpenses = data.expenses.sumOf { it.amount }
            val ownerRevenue = vehicleIncome - vehicleService - vehicleExpenses

            canvas.drawText(data.vehicle.name, margin, y, textPaint)
            canvas.drawText("$vehicleIncome ₽", margin + 140f, y, textPaint)
            canvas.drawText("$vehicleService ₽", margin + 210f, y, textPaint)
            canvas.drawText("$vehicleExpenses ₽", margin + 280f, y, textPaint)
            canvas.drawText("$ownerRevenue ₽", margin + 360f, y, textPaint)

            grandTotalIncome += vehicleIncome
            grandTotalService += vehicleService
            grandTotalExpenses += vehicleExpenses
            grandTotalOwnerRevenue += ownerRevenue
            y += 15f
        }

        canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
        canvas.drawText("ИТОГО:", margin, y + 5f, boldTextPaint)
        canvas.drawText("$grandTotalIncome ₽", margin + 140f, y + 5f, boldTextPaint)
        canvas.drawText("$grandTotalService ₽", margin + 210f, y + 5f, boldTextPaint)
        canvas.drawText("$grandTotalExpenses ₽", margin + 280f, y + 5f, boldTextPaint)
        canvas.drawText("$grandTotalOwnerRevenue ₽", margin + 360f, y + 5f, boldTextPaint)
        y += 50f

        // INDIVIDUAL VEHICLE DETAILS
        reportsData.forEachIndexed { index, data ->
            checkNewPage(100f)
            if (y > 40f) {
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 30f
            }

            canvas.drawText("Автомобиль: ${data.vehicle.name} (${data.vehicle.plateNumber})", margin, y, headerPaint)
            y += 25f

            // Financial Totals
            data.totalStats?.let {
                checkNewPage(80f)
                canvas.drawText("ФИНАНСОВЫЕ ПОКАЗАТЕЛИ ЗА ВЕСЬ ПЕРИОД", margin, y, boldTextPaint)
                y += 15f
                canvas.drawText("ВЫРУЧКА: ${it.revenue} ₽ | РАСХОДЫ: ${it.expenses} ₽ | ЧИСТАЯ ПРИБЫЛЬ: ${it.profit} ₽", margin, y, textPaint)
                y += 25f
            }

            // Expenses Table
            checkNewPage(60f)
            canvas.drawText("РАСХОДЫ", margin, y, boldTextPaint)
            y += 15f

            // Header
            canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
            canvas.drawText("Дата", margin, y, boldTextPaint)
            canvas.drawText("Тип", margin + 80f, y, boldTextPaint)
            canvas.drawText("Описание", margin + 180f, y, boldTextPaint)
            canvas.drawText("Сумма", pageWidth - margin - 60f, y, boldTextPaint)
            y += 5f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 15f

            var totalExp = 0
            data.expenses.forEach { exp ->
                checkNewPage(20f)
                val formattedDate = try {
                    val d = isoSdf.parse(exp.date)
                    if (d != null) dotSdf.format(d) else exp.date
                } catch (e: Exception) { exp.date }

                canvas.drawText(formattedDate, margin, y, textPaint)
                canvas.drawText(exp.category, margin + 80f, y, textPaint)
                val desc = if (exp.title.length > 30) exp.title.substring(0, 27) + "..." else exp.title
                canvas.drawText(desc, margin + 180f, y, textPaint)
                canvas.drawText("${exp.amount} ₽", pageWidth - margin - 60f, y, textPaint)
                totalExp += exp.amount
                y += 15f
            }
            canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
            canvas.drawText("ИТОГО РАСХОДЫ:", margin + 180f, y + 5f, boldTextPaint)
            canvas.drawText("$totalExp ₽", pageWidth - margin - 60f, y + 5f, boldTextPaint)
            y += 40f

            // Income & Management Table
            val allDates = (data.incomeStats.map { it.date } + data.managementStats.map { it.date }).distinct().sorted()
            checkNewPage(60f)
            canvas.drawText("ДОХОДЫ И УПРАВЛЕНИЕ", margin, y, boldTextPaint)
            y += 20f

            // Header
            canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
            canvas.drawText("Дата", margin, y, boldTextPaint)
            canvas.drawText("Аренда", margin + 150f, y, boldTextPaint)
            canvas.drawText("Управление", margin + 300f, y, boldTextPaint)
            y += 5f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 15f

            var totalInc = 0
            var totalMgmt = 0

            allDates.forEach { dateStr ->
                checkNewPage(20f)
                val inc = data.incomeStats.find { it.date == dateStr }
                val mgmt = data.managementStats.find { it.date == dateStr }

                val incAmount = (inc?.amount ?: 0) - (inc?.reduction ?: 0)
                val mgmtAmount = (mgmt?.amount ?: 0) - (mgmt?.reduction ?: 0)
                val dayOfWeek = inc?.dayOfWeek ?: mgmt?.dayOfWeek ?: ""

                val formattedDate = try {
                    val d = isoSdf.parse(dateStr)
                    val datePart = if (d != null) dotSdf.format(d) else dateStr
                    "$datePart ($dayOfWeek)"
                } catch (e: Exception) { "$dateStr ($dayOfWeek)" }

                canvas.drawText(formattedDate, margin, y, textPaint)
                canvas.drawText("$incAmount ₽", margin + 150f, y, textPaint)
                canvas.drawText("$mgmtAmount ₽", margin + 300f, y, textPaint)

                totalInc += incAmount
                totalMgmt += mgmtAmount
                y += 15f
            }
            canvas.drawLine(margin, y - 10f, pageWidth - margin, y - 10f, linePaint)
            canvas.drawText("ИТОГО:", margin, y + 5f, boldTextPaint)
            canvas.drawText("$totalInc ₽", margin + 150f, y + 5f, boldTextPaint)
            canvas.drawText("$totalMgmt ₽", margin + 300f, y + 5f, boldTextPaint)
            y += 30f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Fleet_Report_${System.currentTimeMillis()}.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
        } catch (e: Exception) {
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }

    fun generateReport(
        context: Context,
        vehicle: Vehicle,
        rate: Rate?,
        totalStats: TotalStats?,
        incomeStats: List<StatRecord>,
        managementStats: List<StatRecord>,
        expenses: List<Expense>,
        startDate: Date,
        endDate: Date
    ): File? {
        val data = VehicleReportData(vehicle, rate, totalStats, incomeStats, managementStats, expenses)
        return generateCommonReport(context, listOf(data), startDate, endDate)
    }
}
