package com.dailyhisab.android.feature.reports

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.dailyhisab.android.domain.model.Category
import com.dailyhisab.android.domain.model.FinanceTransaction
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate

object ReportExporter {
    fun sharePdf(context: Context, title: String, rows: List<FinanceTransaction>, categories: List<Category>) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "daily-hisab-${LocalDate.now()}.pdf")
        val document = PdfDocument()
        val width = 595
        val height = 842
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(9, 48, 145); textSize = 22f; typeface = Typeface.DEFAULT_BOLD }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 11f }
        val boldPaint = Paint(textPaint).apply { typeface = Typeface.DEFAULT_BOLD }
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        var canvas = page.canvas
        var y = 52f
        fun header() {
            canvas.drawText("Daily Hisab", 36f, y, titlePaint); y += 26f
            canvas.drawText(title, 36f, y, boldPaint); y += 24f
            canvas.drawText("Date        Type      Category              Amount", 36f, y, boldPaint); y += 18f
        }
        fun nextPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
            canvas = page.canvas
            y = 48f
            header()
        }
        header()
        rows.forEach { row ->
            val category = categories.firstOrNull { it.id == row.categoryId }?.name ?: "Unknown"
            if (y > 785f) nextPage()
            canvas.drawText("${row.date}  ${row.type.name.padEnd(8)} ${category.take(20).padEnd(21)} ${money(row.amountMinor)}", 36f, y, textPaint)
            y += 15f
            wrap(row.description, 78).forEach { line ->
                if (y > 785f) nextPage()
                canvas.drawText("    $line", 36f, y, textPaint); y += 14f
            }
            y += 5f
        }
        if (rows.isEmpty()) canvas.drawText("No transactions found for this period.", 36f, y, textPaint)
        document.finishPage(page)
        file.outputStream().use(document::writeTo)
        document.close()
        share(context, file, "application/pdf")
    }

    fun shareExcel(context: Context, title: String, rows: List<FinanceTransaction>, categories: List<Category>) {
        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "daily-hisab-${LocalDate.now()}.xls")
        val tableRows = rows.joinToString("\n") { row ->
            val category = categories.firstOrNull { it.id == row.categoryId }?.name ?: "Unknown"
            "<Row><Cell><Data ss:Type=\"String\">${xml(row.date.toString())}</Data></Cell><Cell><Data ss:Type=\"String\">${xml(row.type.name)}</Data></Cell><Cell><Data ss:Type=\"String\">${xml(category)}</Data></Cell><Cell><Data ss:Type=\"Number\">${row.amountMinor / 100.0}</Data></Cell><Cell><Data ss:Type=\"String\">${xml(row.paymentMethod)}</Data></Cell><Cell><Data ss:Type=\"String\">${xml(row.description)}</Data></Cell></Row>"
        }
        file.writeText("""<?xml version="1.0"?><Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"><Worksheet ss:Name="Report"><Table><Row><Cell><Data ss:Type="String">${xml(title)}</Data></Cell></Row><Row><Cell><Data ss:Type="String">Date</Data></Cell><Cell><Data ss:Type="String">Type</Data></Cell><Cell><Data ss:Type="String">Category</Data></Cell><Cell><Data ss:Type="String">Amount</Data></Cell><Cell><Data ss:Type="String">Method</Data></Cell><Cell><Data ss:Type="String">Description</Data></Cell></Row>$tableRows</Table></Worksheet></Workbook>""")
        share(context, file, "application/vnd.ms-excel")
    }

    fun shareMonthlyCard(context: Context, rows: List<FinanceTransaction>, categories: List<Category>) {
        sharePdf(context, "Monthly summary card", rows, categories)
    }

    private fun share(context: Context, file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }, "Share report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun wrap(text: String, width: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val lines = mutableListOf<String>()
        var current = ""
        text.split(Regex("\\s+")).forEach { word ->
            if (current.isNotEmpty() && current.length + word.length + 1 > width) { lines += current; current = word }
            else current = if (current.isEmpty()) word else "$current $word"
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    private fun xml(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    private fun money(minor: Long) = "BDT " + BigDecimal.valueOf(minor, 2).stripTrailingZeros().toPlainString()
}
