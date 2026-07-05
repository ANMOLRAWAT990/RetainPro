package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.WallParams
import com.example.ui.components.drawFullSheet
import java.io.File
import java.io.FileOutputStream

object PDFExporter {
    fun sharePDF(context: Context, params: WallParams, previewMode: String = "BOTH", paperSize: String = "A4") {
        val pdfDocument = PdfDocument()
        
        // Dimensions in PostScript points (1/72 inch). 
        val a4W_pt = 595
        val a4H_pt = 842
        val a3W_pt = 842
        val a3H_pt = 1191
        
        val isPortrait = (previewMode == "CS_ONLY")
        
        val w = if (paperSize == "A3") (if (isPortrait) a3W_pt else a3H_pt) else (if (isPortrait) a4W_pt else a4H_pt)
        val h = if (paperSize == "A3") (if (isPortrait) a3H_pt else a3W_pt) else (if (isPortrait) a4H_pt else a4W_pt)
        
        val pageInfo = PdfDocument.PageInfo.Builder(w, h, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val composeCanvas = androidx.compose.ui.graphics.Canvas(canvas)
        val drawScope = androidx.compose.ui.graphics.drawscope.CanvasDrawScope()
        val size = androidx.compose.ui.geometry.Size(w.toFloat(), h.toFloat())
        
        drawScope.draw(androidx.compose.ui.unit.Density(1f), androidx.compose.ui.unit.LayoutDirection.Ltr, composeCanvas, size) {
            val ht = params.height.toFloat()
            val plumTotalMeters = params.plumConcretes.sumOf { it.height }.toFloat() + params.wireCrates.sumOf { it.height }.toFloat() + (if (params.isToeWallEnabled) params.toeHeight.toFloat() else 0f)
            val bw = params.bottomWidth.toFloat()
            val backSlopeW = if (params.backSlopeRatio > 0.0) ht / params.backSlopeRatio.toFloat() else 0f
            
            val hpsWidth = if (params.isHpsEnabled) maxOf(params.hpsTopWidth, params.hpsBottomWidth).toFloat() else 0f
            
            val toeExt = if (params.isToeWallEnabled) params.toeOffsetX.toFloat() + Math.max(params.toeTopWidth.toFloat(), params.toeBottomWidth.toFloat()) else 0f
            val wireExt = if (params.wireCrates.isNotEmpty()) params.wireCrates.maxOf { it.offsetX.toFloat() + Math.max(it.topWidth.toFloat(), it.bottomWidth.toFloat()) } else 0f
            val plumExt = if (params.plumConcretes.isNotEmpty()) params.plumConcretes.maxOf { it.offsetX.toFloat() + Math.max(it.topWidth.toFloat(), it.bottomWidth.toFloat()) } else 0f
            val maxPlumExtent = maxOf(toeExt, wireExt, plumExt)
            
            val csMaxRightRelative = backSlopeW + bw + maxPlumExtent
            
            val feLengthDraw = if (previewMode == "CS_ONLY") 0f else (if (params.length > 30.0) 6.0f else params.length.toFloat())
            val gapM = 6.0f
            val feBaseXRelative = csMaxRightRelative + gapM
            
            val minX = - maxOf(bw, backSlopeW) - hpsWidth - 6.0f
            val maxX = if (previewMode == "CS_ONLY") csMaxRightRelative + 4.5f else feBaseXRelative + feLengthDraw + 4.5f
            val minY = - ht - 5.5f
            val maxY = plumTotalMeters + 3.5f
            
            val widthReq = if (previewMode == "CS_ONLY") maxOf(8.0f, maxX - minX) else maxOf(16.0f, maxX - minX)
            val heightReq = maxY - minY
            
            val aspectA4 = w.toFloat() / h.toFloat()
            
            var a4W_u = widthReq
            var a4H_u = a4W_u / aspectA4
            
            if (a4H_u < heightReq) {
                a4H_u = heightReq
                a4W_u = a4H_u * aspectA4
            }
            
            val cx_u = (minX + maxX) / 2f
            val cy_u = (minY + maxY) / 2f
            
            val margin = 20f
            val pxPerMeter = minOf((size.width - margin * 2) / a4W_u, (size.height - margin * 2) / a4H_u)
            
            val startX = size.width / 2f - cx_u * pxPerMeter
            val baseY = size.height / 2f - cy_u * pxPerMeter
            
            drawFullSheet(this, params, pxPerMeter, startX, baseY, androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.White, false, previewMode, paperSize)
        }
        
        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "RetainingWall_RW001.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share PDF Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}
