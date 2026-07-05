package com.example.export

import com.example.model.WallParams
import com.example.model.PlumConcrete
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import android.content.Context
import java.util.Locale

import androidx.core.content.FileProvider
import android.content.Intent
import android.widget.Toast

object EstimateGenerator {
    fun shareCSV(context: Context, params: WallParams) {
        try {
            val file = generateEstimateCSV(context, params)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Cost Estimate"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generateEstimateCSV(context: Context, params: WallParams): File {
        val file = File(context.cacheDir, "Estimate_${params.chainage.replace("+", "_")}.csv")
        val writer = OutputStreamWriter(FileOutputStream(file))
        
        writer.write("S.No,Description of Work,Unit,Quantity,Rate,Amount\n")
        
        var slNo = 1
        var totalCost = 0.0
        
        fun addRow(desc: String, unit: String, qty: Double, rate: Double) {
            if (qty <= 0) return
            val amount = qty * rate
            totalCost += amount
            writer.write("${slNo++},\"$desc\",\"$unit\",${String.format(Locale.US, "%.2f", qty)},${String.format(Locale.US, "%.2f", rate)},${String.format(Locale.US, "%.2f", amount)}\n")
        }
        
        val plumH = params.plumConcretes.sumOf { it.height }
        val wireCratesH = params.wireCrates.sumOf { it.height }
        val toeH = if (params.isToeWallEnabled) params.toeHeight else 0.0
        val maxPlumW = params.plumConcretes.maxOfOrNull { Math.max(it.topWidth, it.bottomWidth) } ?: 0.0
        val maxWireCrateW = params.wireCrates.maxOfOrNull { Math.max(it.topWidth, it.bottomWidth) } ?: 0.0
        val maxBaseW = Math.max(Math.max(maxPlumW, maxWireCrateW), if (params.isToeWallEnabled) params.toeBottomWidth else 0.0)
        
        val bw = params.bottomWidth
        val excW = (if (params.isHpsEnabled) params.hpsWidth else 0.0) + Math.max(bw, maxBaseW) + 1.0
        val excDepth = 1.0 + plumH + wireCratesH + toeH
        val excQty = excW * excDepth * params.length
        addRow("Earthwork in excavation", "Cum", excQty, params.rates.earthwork)

        if (params.isToeWallEnabled) {
            val toeQty = ((params.toeTopWidth + params.toeBottomWidth) / 2.0) * params.toeHeight * params.length
            addRow("Toe Wall (Concrete)", "Cum", toeQty, params.rates.plumConcrete)
        }
        
        val wireCratesQty = params.wireCrates.sumOf { ((it.topWidth + it.bottomWidth) / 2.0) * it.height * params.length }
        addRow("Gabion Wire Crates", "Cum", wireCratesQty, params.rates.wireCrate)

        val plumQty = params.plumConcretes.sumOf { ((it.topWidth + it.bottomWidth) / 2.0) * it.height * params.length }
        addRow("Plum Concrete 1:3:6", "Cum", plumQty, params.rates.plumConcrete)
        
        var bandQty = 0.0
        val h = params.height
        val tw = params.topWidth
        fun widthAt(y: Double): Double {
            val p = y / h
            return tw + p * (bw - tw)
        }
        
        if (params.isWallEnabled) {
            if (params.topBandThickness > 0) {
                val ytb = params.topBandThickness
                bandQty += ((widthAt(0.0) + widthAt(ytb)) / 2.0) * params.topBandThickness * params.length
            }
            if (params.bottomBandThickness > 0) {
                val ybt = h - params.bottomBandThickness
                bandQty += ((widthAt(h) + widthAt(ybt)) / 2.0) * params.bottomBandThickness * params.length
            }
            if (params.midBandThickness > 0) {
                val midY = params.height / 2.0
                val ymt = midY - params.midBandThickness/2.0
                val ymb = midY + params.midBandThickness/2.0
                bandQty += ((widthAt(ymt) + widthAt(ymb)) / 2.0) * params.midBandThickness * params.length
            }
            addRow("RR Masonry 1:6 in Bands", "Cum", bandQty, params.rates.rrBand)
            
            val totWallQty = ((tw + bw) / 2.0) * params.height * params.length
            val rrDryQty = Math.max(0.0, totWallQty - bandQty)
            addRow("RR Dry Stone Masonry", "Cum", rrDryQty, params.rates.rrDry)
        }
        
        if (params.isHpsEnabled) {
            val hpsQty = 0.25 * (params.hpsTopWidth + params.hpsBottomWidth) * params.height * params.length
            addRow("Hand Packed Stone Filling", "Cum", hpsQty, params.rates.hps)
        }
        
        if (params.isWeepHoleWallEnabled || params.isWeepHoleWireCrateEnabled || params.isWeepHolePlumEnabled || params.isWeepHoleToeWallEnabled) {
            var weepHoleLength = 0.0
            val totalStructureHeight = h + plumH + wireCratesH + toeH
            if (totalStructureHeight > 1.0) {
                var yObj = 1.0
                while (yObj < totalStructureHeight - 0.05) {
                    var xObj = 1.5 / 2.0
                    while (xObj < params.length - 0.05) {
                        var isWeepHereAllowed = false
                        var blockWidthFound = 0.0
                        
                        if (yObj <= plumH + wireCratesH + toeH) {
                            // in foundation
                            val invertedY = (plumH + wireCratesH + toeH) - yObj
                            var currentY = 0.0
                            val toeBlock = if (params.isToeWallEnabled) listOf(Pair("TOE", PlumConcrete(params.toeTopWidth, params.toeBottomWidth, params.toeHeight, params.toeOffsetX))) else emptyList<Pair<String, PlumConcrete>>()
                            val allBlocks: List<Pair<String, PlumConcrete>> = toeBlock + params.wireCrates.map { Pair("WIRECATE", it) } + params.plumConcretes.map { Pair("PLUM", it) }
                            for (b in allBlocks) {
                                val type = b.first
                                val pc = b.second
                                if (invertedY >= currentY && invertedY <= currentY + pc.height) {
                                    if (type == "TOE" && params.isWeepHoleToeWallEnabled) isWeepHereAllowed = true
                                    if (type == "WIRECATE" && params.isWeepHoleWireCrateEnabled) isWeepHereAllowed = true
                                    if (type == "PLUM" && params.isWeepHolePlumEnabled) isWeepHereAllowed = true
                                    blockWidthFound = Math.max(pc.topWidth, pc.bottomWidth)
                                    break
                                }
                                currentY += pc.height
                            }
                        } else {
                            if (params.isWallEnabled && params.isWeepHoleWallEnabled) {
                                isWeepHereAllowed = true
                                val wallY = yObj - (plumH + wireCratesH + toeH)
                                blockWidthFound = widthAt(params.height - wallY)
                            }
                        }
                        
                        if (isWeepHereAllowed) {
                            weepHoleLength += blockWidthFound
                        }
                        
                        xObj += 1.5
                    }
                    yObj += 1.0
                }
                if (weepHoleLength > 0.0) {
                    addRow("100mm dia PVC Weep Holes", "Rmt", weepHoleLength, params.rates.pvcPipe)
                }
            }
        }
        
        writer.write(",,,,,, \n")
        writer.write(",\"TOTAL ABSTRACT COST\",,,,${String.format(Locale.US, "%.2f", totalCost)}\n")
        
        writer.flush()
        writer.close()
        
        return file
    }
}
