package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.example.model.ChainageSegment
import com.example.model.EstimateParams
import com.example.model.WallParams
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class SegmentQuantities(
    val label: String,
    val excavation: Double,
    val plumMasonry: Double,
    val weepHoles: Double,
    val rrMasonry16Total: Double,
    val rrDryMasonry: Double,
    val hpsFilling: Double,
    val bottomBand: Double,
    val topBand: Double,
    val verticalBand: Double
)

fun calculateQuantities(seg: com.example.model.CrossSection, params: WallParams): SegmentQuantities {
    val excavationQty = 0.5 * seg.length * (seg.bottomWidth + 0.5) * seg.height
    val plumQty = params.plumConcretes.sumOf { ((it.topWidth + it.bottomWidth) / 2.0) * it.height * seg.length }
    val weepHoleQty = 2.0 * (seg.length / 1.5) * seg.bottomWidth
    
    val bottomBandQty = if (params.bottomBandThickness > 0) seg.length * seg.bottomWidth * params.bottomBandThickness else 0.0
    val topBandQty = if (params.topBandThickness > 0) seg.length * seg.topWidth * params.topBandThickness else 0.0
    val verticalBandQty = if (params.midBandThickness > 0) seg.length * ((seg.topWidth + seg.bottomWidth) / 2) * params.midBandThickness else 0.0
    val rrMasonry16Total = bottomBandQty + topBandQty + verticalBandQty
    
    val totalWallVol = seg.length * (seg.topWidth + seg.bottomWidth) / 2 * seg.height
    val rrDryQty = totalWallVol - rrMasonry16Total
    val hpsTopLocal = if (params.isHpsEnabled) params.hpsTopWidth else 0.0
    val hpsBotLocal = if (params.isHpsEnabled) params.hpsBottomWidth else 0.0
    val hpsQty = 0.25 * (hpsTopLocal + hpsBotLocal) * seg.height * seg.length

    return SegmentQuantities(
        label = seg.label,
        excavation = excavationQty,
        plumMasonry = plumQty,
        weepHoles = weepHoleQty,
        rrMasonry16Total = rrMasonry16Total,
        rrDryMasonry = rrDryQty,
        hpsFilling = hpsQty,
        bottomBand = bottomBandQty,
        topBand = topBandQty,
        verticalBand = verticalBandQty
    )
}

object WorkbookGenerator {

    fun exportWorkbook(context: Context, params: WallParams, estimateParams: EstimateParams) {
        try {
            var wb: Workbook = XSSFWorkbook()
            var fileExt = ".xlsx"
            var mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            val segments = estimateParams.chainageSegments

            // Creating/populating our sheets. We check if they exist first.
            if (wb.getSheet("DOM RWall") == null) createDOMRWallSheet(wb, params, segments)
            if (wb.getSheet("8-Analysis") == null) createAnalysisSheet(wb, estimateParams)
            if (wb.getSheet("9-DOM") == null) createDOMSheet(wb, params, segments, estimateParams)
            if (wb.getSheet("10-BOQ") == null) createBOQSheet(wb, params, segments, estimateParams)
            if (wb.getSheet("11-Abstract") == null) createAbstractSheet(wb, params, segments, estimateParams)

            val fileName = "Estimate_${params.projectName}_${params.chainage}$fileExt"
                .replace(" ", "_")
                .replace(":", "")

            // Write to cache directory for sharing
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { wb.write(it) }

            // Share the file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooserIntent = Intent.createChooser(intent, "Share Estimate Workbook")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)

        } catch (e: Throwable) {
            Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun createDOMRWallSheet(wb: Workbook, params: WallParams, segments: List<ChainageSegment>) {
        val sheet = wb.createSheet("DOM RWall")
        val fontBold = wb.createFont().apply { bold = true; fontHeightInPoints = 14 }
        val styleBold = wb.createCellStyle().apply { setFont(fontBold); alignment = HorizontalAlignment.CENTER }
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue("Detail of Quantities of Retaining Wall")
        cell.cellStyle = styleBold
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 10))
    }

    private fun createAnalysisSheet(wb: Workbook, estimateParams: EstimateParams) {
        val sheet = wb.createSheet("8-Analysis")
        val fontBold = wb.createFont().apply { bold = true; fontHeightInPoints = 16 }
        val styleBold = wb.createCellStyle().apply { setFont(fontBold); alignment = HorizontalAlignment.CENTER }
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue("ANALYSIS OF RATES")
        cell.cellStyle = styleBold
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 4))
        
        // Headers
        val hRow = sheet.createRow(2)
        hRow.createCell(0).setCellValue("S.N.")
        hRow.createCell(1).setCellValue("Item of Work")
        hRow.createCell(2).setCellValue("UNIT")
        hRow.createCell(3).setCellValue("RATE")
        hRow.createCell(4).setCellValue("SOR")
        
        val items = listOf(
            Triple(1, "Excavation in foundation...", estimateParams.rateExcavation),
            Triple(2, "Cement Plum Masonry...", estimateParams.ratePlumMasonry),
            Triple(3, "Providing weepholes...", estimateParams.rateWeepHoles),
            Triple(4, "Random Rubble Stone Masonry 1:6...", estimateParams.rateRRMasonry16),
            Triple(5, "Random Rubble Stone Masonry Dry...", estimateParams.rateRRDryMasonry),
            Triple(6, "Hand Packed stone filling...", estimateParams.rateHPSFilling)
        )
        items.forEachIndexed { i, (sn, desc, rate) ->
            val r = sheet.createRow(3 + i)
            r.createCell(0).setCellValue(sn.toString())
            r.createCell(1).setCellValue(desc)
            r.createCell(2).setCellValue("Cum")
            r.createCell(3).setCellValue(rate)
            r.createCell(4).setCellValue("SOR-XX")
        }
    }

    private fun createDOMSheet(wb: Workbook, params: WallParams, segments: List<ChainageSegment>, estimateParams: EstimateParams) {
        val sheet = wb.createSheet("9-DOM")
        sheet.createRow(0).createCell(0).setCellValue("Detail of measurement")
    }

    private fun createBOQSheet(wb: Workbook, params: WallParams, segments: List<ChainageSegment>, estimateParams: EstimateParams) {
        val sheet = wb.createSheet("10-BOQ")
        val fontBold = wb.createFont().apply { bold = true; fontHeightInPoints = 16 }
        val styleBold = wb.createCellStyle().apply { setFont(fontBold); alignment = HorizontalAlignment.CENTER }
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue("Bill of Quantity")
        cell.cellStyle = styleBold
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 6))
        
        val hRow = sheet.createRow(2)
        arrayOf("S.No.", "Item", "Qty", "Unit", "Rate", "Amount").forEachIndexed { i, s ->
            hRow.createCell(i).setCellValue(s)
        }
        
        var totalExcavation = 0.0
        var totalPlum = 0.0
        var totalWeep = 0.0
        var totalRR16 = 0.0
        var totalRRDry = 0.0
        var totalHPS = 0.0
        
        segments.forEach { seg ->
            seg.crossSections.forEach { cs ->
                val q = calculateQuantities(cs, params)
                totalExcavation += q.excavation
                totalPlum += q.plumMasonry
                totalWeep += q.weepHoles
                totalRR16 += q.rrMasonry16Total
                totalRRDry += q.rrDryMasonry
                totalHPS += q.hpsFilling
            }
        }
        
        var rIdx = 3
        var grandTotal = 0.0
        fun addBoqRow(sno: Int, desc: String, qty: Double, unit: String, rate: Double) {
            val r = sheet.createRow(rIdx++)
            r.createCell(0).setCellValue(sno.toString())
            r.createCell(1).setCellValue(desc)
            r.createCell(2).setCellValue(qty)
            r.createCell(3).setCellValue(unit)
            r.createCell(4).setCellValue(rate)
            val amt = qty * rate
            r.createCell(5).setCellValue(amt)
            grandTotal += amt
        }
        
        addBoqRow(1, "Excavation in foundation...", totalExcavation, "Cum", estimateParams.rateExcavation)
        addBoqRow(2, "Cement Plum Masonry...", totalPlum, "Cum", estimateParams.ratePlumMasonry)
        addBoqRow(3, "Providing weepholes...", totalWeep, "Rmt", estimateParams.rateWeepHoles)
        addBoqRow(4, "Random Rubble Stone Masonry 1:6...", totalRR16, "Cum", estimateParams.rateRRMasonry16)
        addBoqRow(5, "Random Rubble Stone Masonry Dry...", totalRRDry, "Cum", estimateParams.rateRRDryMasonry)
        addBoqRow(6, "Hand Packed stone filling...", totalHPS, "Cum", estimateParams.rateHPSFilling)
        
        val tRow = sheet.createRow(rIdx++)
        tRow.createCell(1).setCellValue("G TOTAL")
        tRow.createCell(5).setCellValue(grandTotal)
    }

    private fun createAbstractSheet(wb: Workbook, params: WallParams, segments: List<ChainageSegment>, estimateParams: EstimateParams) {
        val sheet = wb.createSheet("11-Abstract")
        val fontBold = wb.createFont().apply { bold = true; fontHeightInPoints = 18 }
        val styleBold = wb.createCellStyle().apply { setFont(fontBold); alignment = HorizontalAlignment.CENTER }
        
        val row = sheet.createRow(0)
        val cell = row.createCell(0)
        cell.setCellValue("ABSTRACT OF COST")
        cell.cellStyle = styleBold
        sheet.addMergedRegion(CellRangeAddress(0, 0, 0, 2))
        
        val row1 = sheet.createRow(1)
        row1.createCell(0).setCellValue(estimateParams.nameOfWork)
        sheet.addMergedRegion(CellRangeAddress(1, 1, 0, 2))
        
        val hRow = sheet.createRow(3)
        hRow.createCell(0).setCellValue("S.N.")
        hRow.createCell(1).setCellValue("ITEM OF WORK")
        hRow.createCell(2).setCellValue("AMOUNT (in Rs)")
        
        // Calculate BOQ Total
        var grandTotal = 0.0
        segments.forEach { seg ->
            seg.crossSections.forEach { cs ->
                val q = calculateQuantities(cs, params)
                grandTotal += q.excavation * estimateParams.rateExcavation +
                              q.plumMasonry * estimateParams.ratePlumMasonry +
                              q.weepHoles * estimateParams.rateWeepHoles +
                              q.rrMasonry16Total * estimateParams.rateRRMasonry16 +
                              q.rrDryMasonry * estimateParams.rateRRDryMasonry +
                              q.hpsFilling * estimateParams.rateHPSFilling
            }
        }
        
        var rIdx = 5
        val r1 = sheet.createRow(rIdx++)
        r1.createCell(0).setCellValue("1")
        r1.createCell(1).setCellValue("Total Value of Work")
        r1.createCell(2).setCellValue(grandTotal)
        
        var currentTotal = grandTotal
        
        if (estimateParams.contingencyPercent > 0) {
            val amt = grandTotal * (estimateParams.contingencyPercent / 100.0)
            val r = sheet.createRow(rIdx++)
            r.createCell(1).setCellValue("Add ${estimateParams.contingencyPercent}% Contingency")
            r.createCell(2).setCellValue(amt)
            currentTotal += amt
        }
        
        if (estimateParams.labourCessPercent > 0) {
            val amt = grandTotal * (estimateParams.labourCessPercent / 100.0)
            val r = sheet.createRow(rIdx++)
            r.createCell(1).setCellValue("Add ${estimateParams.labourCessPercent}% Labour Cess")
            r.createCell(2).setCellValue(amt)
            currentTotal += amt
        }
        
        val gstAmt = currentTotal * (estimateParams.gstPercent / 100.0)
        val rGst = sheet.createRow(rIdx++)
        rGst.createCell(1).setCellValue("Add ${estimateParams.gstPercent}% G.S.T.")
        rGst.createCell(2).setCellValue(gstAmt)
        currentTotal += gstAmt
        
        val rGT = sheet.createRow(rIdx++)
        rGT.createCell(1).setCellValue("G Total")
        rGT.createCell(2).setCellValue(currentTotal)
        
        val lacs = currentTotal / 100000.0
        val rSay = sheet.createRow(rIdx++)
        rSay.createCell(0).setCellValue("SAY Rs in Lacs")
        rSay.createCell(2).setCellValue(String.format("%.2f", lacs))
        sheet.addMergedRegion(CellRangeAddress(rIdx-1, rIdx-1, 0, 1))
        
        val words = sheet.createRow(rIdx++)
        words.createCell(0).setCellValue("Rupees ${amountInWords(currentTotal)}")
        sheet.addMergedRegion(CellRangeAddress(rIdx-1, rIdx-1, 0, 2))
    }

    fun amountInWords(amount: Double): String {
        return "Rupees Only"
    }
}
