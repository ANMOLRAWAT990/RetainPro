package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.WallParams
import com.example.model.generateWallLayers
import com.example.model.PlumConcrete
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.random.Random

@Composable
fun WallCanvas(
    params: WallParams,
    isBlueprintMode: Boolean,
    modifier: Modifier = Modifier,
    previewMode: String = "BOTH"
) {
    val bgColor = if (isBlueprintMode) Color(0xFF003366) else Color.White
    val lineColor = if (isBlueprintMode) Color(0xFF00FFFF) else Color.Black

    var zoomScale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Canvas(modifier = modifier
        .fillMaxSize()
        .background(bgColor)
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                zoomScale = (zoomScale * zoom).coerceIn(0.2f, 10f)
                offset += pan
            }
        }
        .graphicsLayer {
            scaleX = zoomScale
            scaleY = zoomScale
            translationX = offset.x
            translationY = offset.y
        }) {

        val canvasWidth = size.width
        val canvasHeight = size.height

        val plumTotalMeters = params.plumConcretes.sumOf { it.height }.toFloat() + params.wireCrates.sumOf { it.height }.toFloat() + (if (params.isToeWallEnabled) params.toeHeight.toFloat() else 0f)
        
        // Width calculation
        val sumOffsets = params.plumConcretes.sumOf { it.offsetX }.toFloat() + params.wireCrates.sumOf { it.offsetX }.toFloat() + (if (params.isToeWallEnabled) params.toeOffsetX.toFloat() else 0f)
        val maxBlockWidth = listOf(params.bottomWidth.toFloat())
            .plus(params.plumConcretes.map { maxOf(it.bottomWidth, it.topWidth).toFloat() })
            .plus(params.wireCrates.map { maxOf(it.bottomWidth, it.topWidth).toFloat() })
            .plus(if (params.isToeWallEnabled) maxOf(params.toeBottomWidth, params.toeTopWidth).toFloat() else 0f)
            .maxOrNull() ?: params.bottomWidth.toFloat()

        val hpsMax = maxOf(params.hpsTopWidth, params.hpsBottomWidth).toFloat()
        val approximateMaxRightExt = sumOffsets + maxBlockWidth
        val csWidthM = 4.0f + hpsMax + approximateMaxRightExt + 1.0f 
        val gapM = if (previewMode == "BOTH") 6.0f else 0.0f
        val feLengthDraw = if (params.length > 30.0) 6.0f else params.length.toFloat()
        val feWidthM = if (previewMode == "BOTH") feLengthDraw + 1.0f else 0.0f
        val totalMetersW = csWidthM + gapM + feWidthM
        
        val totalMetersH = 3.5f + params.height.toFloat() + plumTotalMeters + 2.0f 

        val pxPerMeter = minOf(canvasWidth / totalMetersW, canvasHeight / totalMetersH) * 0.95f
        
        // Let's center according to previewMode
        val drawingTotalWPx = totalMetersW * pxPerMeter
        // If CS_ONLY, we want to visually center the structural mass.
        val startX = if (previewMode == "BOTH") {
            (canvasWidth - drawingTotalWPx) / 2f + (2.5f + hpsMax) * pxPerMeter
        } else {
            canvasWidth / 2f - (params.bottomWidth.toFloat() * pxPerMeter) / 2f
        }
        
        val baseY = (canvasHeight + (params.height.toFloat() + plumTotalMeters) * pxPerMeter) / 2f - (plumTotalMeters + 1.5f)*pxPerMeter

        drawFullSheet(this, params, pxPerMeter, startX, baseY, lineColor, bgColor, isBlueprintMode, previewMode)
    }
}

fun drawFullSheet(
    drawScope: DrawScope,
    params: WallParams,
    pxPerMeter: Float,
    csBaseX: Float,
    baseY: Float,
    lineColor: Color,
    bgColor: Color,
    isBlueprintMode: Boolean,
    previewMode: String = "BOTH",
    paperSize: String = "A4"
) {
    val h = params.height.toFloat() * pxPerMeter
    val backSlopeW = if (params.backSlopeRatio > 0.0) h / params.backSlopeRatio.toFloat() else 0f
    val hpsWidthPx = if (params.isHpsEnabled) params.hpsWidth.toFloat() * pxPerMeter else 0f

    val bwPx = params.bottomWidth.toFloat() * pxPerMeter
    val gapPx = (if (previewMode == "BOTH") 6.0f else 0.0f) * pxPerMeter

    val maxPlumExtent = if (params.plumConcretes.isEmpty() && params.wireCrates.isEmpty() && !params.isToeWallEnabled) {
        0f
    } else {
        val toeExt = if (params.isToeWallEnabled) params.toeOffsetX.toFloat() * pxPerMeter + params.toeBottomWidth.toFloat() * pxPerMeter else 0f
        val wireExt = if (params.wireCrates.isNotEmpty()) params.wireCrates.maxOf { it.offsetX.toFloat() * pxPerMeter + Math.max(it.topWidth.toFloat(), it.bottomWidth.toFloat()) * pxPerMeter } else 0f
        val plumExt = if (params.plumConcretes.isNotEmpty()) params.plumConcretes.maxOf { it.offsetX.toFloat() * pxPerMeter + Math.max(it.topWidth.toFloat(), it.bottomWidth.toFloat()) * pxPerMeter } else 0f
        maxOf(toeExt, wireExt, plumExt)
    }
    val csMaxRightRelative = backSlopeW + bwPx + maxPlumExtent

    // BaseX for front elevation = csBaseX + max width of CS + gapPx
    val feBaseX = csBaseX + csMaxRightRelative + gapPx
    
    val plumTotalMeters = params.plumConcretes.sumOf { it.height }.toFloat() + params.wireCrates.sumOf { it.height }.toFloat() + (if (params.isToeWallEnabled) params.toeHeight.toFloat() else 0f)
    val plumTotalPx = plumTotalMeters * pxPerMeter

    val bw = params.bottomWidth.toFloat()
    val feLengthDraw = if (previewMode == "CS_ONLY") 0f else (if (params.length > 30.0) 6.0f else params.length.toFloat())
    val minX = csBaseX - maxOf(bw, backSlopeW/pxPerMeter) * pxPerMeter - hpsWidthPx - 6.0f * pxPerMeter
    val maxX = if (previewMode == "CS_ONLY") csBaseX + csMaxRightRelative + 4.5f * pxPerMeter else feBaseX + feLengthDraw * pxPerMeter + 4.5f * pxPerMeter
    val minY = baseY - h - 5.5f * pxPerMeter
    val maxY = baseY + plumTotalPx + 3.5f * pxPerMeter
    
    val widthReq = if (previewMode == "CS_ONLY") maxOf(8.0f * pxPerMeter, maxX - minX) else maxOf(16.0f * pxPerMeter, maxX - minX)
    val heightReq = maxY - minY
    
    val aspectA4 = if (paperSize == "A3") {
        if (previewMode == "CS_ONLY") 297f / 420f else 420f / 297f
    } else {
        if (previewMode == "CS_ONLY") 210f / 297f else 297f / 210f
    }
    
    var a4W = widthReq
    var a4H = a4W / aspectA4
    
    if (a4H < heightReq) {
        a4H = heightReq
        a4W = a4H * aspectA4
    }
    
    val cx = (minX + maxX) / 2f
    val cy = (minY + maxY) / 2f
    val a4MinX = cx - a4W / 2f
    val a4MaxX = cx + a4W / 2f
    val a4MinY = cy - a4H / 2f
    val a4MaxY = cy + a4H / 2f

    // Draw A4 Border
    drawScope.drawRect(
        color = lineColor,
        topLeft = Offset(a4MinX, a4MinY),
        size = Size(a4W, a4H),
        style = Stroke(width = 2f)
    )

    // Title block at bottom right of A4
    val tbW = a4W
    val tbH = 1.6f * pxPerMeter
    val tbX = a4MinX
    val tbY = a4MaxY - tbH

    // Title Block Style Fix
    val tbBg = Color.White
    val tbLine = Color.Black
    
    drawScope.drawRect(
        color = tbBg,
        topLeft = Offset(tbX, tbY),
        size = Size(tbW, tbH)
    )
    drawScope.drawRect(
        color = tbLine,
        topLeft = Offset(tbX, tbY),
        size = Size(tbW, tbH),
        style = Stroke(width = 2f)
    )
    
    // inner divider lines
    drawScope.drawLine(tbLine, Offset(tbX, tbY + tbH*0.4f), Offset(tbX + tbW, tbY + tbH*0.4f), strokeWidth = 1f)
    drawScope.drawLine(tbLine, Offset(tbX + tbW*0.75f, tbY + tbH*0.4f), Offset(tbX + tbW*0.75f, tbY + tbH), strokeWidth = 1f)
    
    // Create text paints for calculating text scale if needed
    val titleText = "${params.projectName} — CH: ${params.chainage}"
    drawText(drawScope, titleText, tbX + tbW/2f, tbY + tbH*0.25f, tbLine, bold = true, size = 16f, align = Paint.Align.CENTER)
    
    drawText(drawScope, "Drawing No: RW-001 | Scale: N.T.S.", tbX + 0.15f*pxPerMeter, tbY + tbH*0.65f, tbLine, bold = false, size = 12f)
    drawText(drawScope, "Drawings Not to be Scaled", tbX + 0.15f*pxPerMeter, tbY + tbH*0.9f, tbLine, bold = false, size = 12f)
    
    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())
    drawText(drawScope, "Date: $dateStr", tbX + tbW*0.75f + 0.15f*pxPerMeter, tbY + tbH*0.65f, tbLine, bold = false, size = 12f)
    drawText(drawScope, "$paperSize Size", tbX + tbW*0.75f + 0.15f*pxPerMeter, tbY + tbH*0.9f, tbLine, bold = false, size = 12f)

    drawCrossSection(drawScope, params, pxPerMeter, csBaseX, baseY, lineColor, bgColor, isBlueprintMode)
    if (previewMode == "BOTH") {
        drawFrontElevation(drawScope, params, pxPerMeter, feBaseX, baseY, lineColor, bgColor, isBlueprintMode)
    }
}

fun drawCrossSection(
    drawScope: DrawScope, params: WallParams, pxPerMeter: Float, 
    startX: Float, baseY: Float, lineColor: Color, bgColor: Color, isBlueprintMode: Boolean
) {
    val dimColor = if (isBlueprintMode) Color.Yellow else Color.Blue
    val h = params.height.toFloat() * pxPerMeter
    val tw = params.topWidth.toFloat() * pxPerMeter
    val bw = params.bottomWidth.toFloat() * pxPerMeter
    val hpsTopWidthPx = params.hpsTopWidth.toFloat() * pxPerMeter
    val hpsBottomWidthPx = params.hpsBottomWidth.toFloat() * pxPerMeter
    val hpsWidthPx = maxOf(hpsTopWidthPx, hpsBottomWidthPx)
    val hpsHeightPx = (params.height.toFloat() / 2f) * pxPerMeter

    val backSlopeW = if (params.backSlopeRatio > 0.0) h / params.backSlopeRatio.toFloat() else 0f

    val tL = Offset(startX, baseY - h)
    val tR = Offset(startX + tw, baseY - h)
    val bL = Offset(startX + backSlopeW, baseY)
    val bR = Offset(startX + backSlopeW + bw, baseY)

    fun getLeftX(y: Float): Float {
        val p = (y - (baseY - h)) / h
        return tL.x + p * (bL.x - tL.x)
    }

    fun getRightX(y: Float): Float {
        val p = (y - (baseY - h)) / h
        return tR.x + p * (bR.x - tR.x)
    }

    data class LayerSlice(val type: String, val hM: Double, val topY: Float, val botY: Float, val tL: Offset, val tR: Offset, val bL: Offset, val bR: Offset)
    val layerSlices = mutableListOf<LayerSlice>()
    var cy = baseY - h
    for (layer in generateWallLayers(params)) {
        val hPx = layer.second.toFloat() * pxPerMeter
        val nextCy = cy + hPx
        layerSlices.add(LayerSlice(
            layer.first, layer.second,
            cy, nextCy,
            Offset(getLeftX(cy), cy), Offset(getRightX(cy), cy),
            Offset(getLeftX(nextCy), nextCy), Offset(getRightX(nextCy), nextCy)
        ))
        cy = nextCy
    }

    val hpsTopL = Offset(tL.x - hpsTopWidthPx, baseY - h)
    val hpsBottomInner = Offset(getLeftX(baseY - h + hpsHeightPx), baseY - h + hpsHeightPx)
    val hpsBotLeft = Offset(hpsBottomInner.x - hpsBottomWidthPx, hpsBottomInner.y)

    val ignoreOffset = !params.isWallEnabled

    if (params.isWallEnabled) {
        drawText(drawScope, "CROSS SECTION", startX - 1.0f*pxPerMeter, baseY - h - 2.2f*pxPerMeter, lineColor, bold = true, size = 0.35f*pxPerMeter)

        // Fills
        if (params.height > 0.0) {
            val minCsX = minOf(tL.x, bL.x) - hpsWidthPx - 10f
            val maxCsX = maxOf(tR.x, bR.x) + 10f
            val totalW = maxCsX - minCsX
            
            for (slice in layerSlices) {
                val path = Path().apply { moveTo(slice.tL.x, slice.tL.y); lineTo(slice.tR.x, slice.tR.y); lineTo(slice.bR.x, slice.bR.y); lineTo(slice.bL.x, slice.bL.y); close() }
                val pattern = when (slice.type) {
                    "RR Band" -> params.rrBandPatternType
                    "RR 1:6" -> params.rr16PatternType
                    else -> params.rrDryPatternType
                }
                val isEnabled = when (slice.type) {
                    "RR Band" -> params.isRrBandEnabled
                    "RR 1:6" -> params.isRr16Enabled
                    else -> params.isRrDryEnabled
                }
                if (isEnabled) {
                    drawScope.clipPath(path) {
                        drawSelectedPattern(this, pattern, totalW, h * 1.5f, minCsX, slice.topY, pxPerMeter, lineColor)
                    }
                }
                drawScope.drawPath(path, color = lineColor, style = Stroke(2f))
            }

            if (params.isHpsEnabled) {
                val hpsPath = Path().apply {
                    moveTo(hpsTopL.x, hpsTopL.y)
                    lineTo(tL.x, tL.y)
                    lineTo(hpsBottomInner.x, hpsBottomInner.y)
                    lineTo(hpsBotLeft.x, hpsBotLeft.y)
                    close()
                }
                drawScope.clipPath(hpsPath) {
                    val minX = minOf(hpsTopL.x, hpsBotLeft.x)
                    val maxX = maxOf(tL.x, hpsBottomInner.x)
                    val w = (maxX - minX) * 1.2f
                    drawHpsPattern(this, w, hpsHeightPx + 10f, minX, hpsTopL.y, pxPerMeter, lineColor)
                }
                drawScope.drawPath(hpsPath, color = lineColor, style = Stroke(2f))
            }

            val outlinePath = Path().apply { moveTo(tL.x, tL.y); lineTo(tR.x, tR.y); lineTo(bR.x, bR.y); lineTo(bL.x, bL.y); close() }
            drawScope.drawPath(outlinePath, color = lineColor, style = Stroke(3f))
        }
    }

    // Plum concrete / Wire Crates
    var bottomY = baseY
    var currentRightEdge = if (ignoreOffset) startX else bR.x
    
    val toeBlock = if (params.isToeWallEnabled) listOf(Pair("TOE", PlumConcrete(params.toeTopWidth, params.toeBottomWidth, params.toeHeight, params.toeOffsetX))) else emptyList<Pair<String, PlumConcrete>>()
    val allBlocks: List<Pair<String, PlumConcrete>> = toeBlock + params.wireCrates.map { Pair("WIRECATE", it) } + params.plumConcretes.map { Pair("PLUM", it) }
    
    allBlocks.forEachIndexed { index, (type, pc) ->
        val pH = pc.height.toFloat() * pxPerMeter
        val pTopW = pc.topWidth.toFloat() * pxPerMeter
        val pBotW = pc.bottomWidth.toFloat() * pxPerMeter
        val actualOffsetX = pc.offsetX.toFloat() * pxPerMeter
        
        val topRightEdge = currentRightEdge + actualOffsetX
        val actualPx = topRightEdge - pTopW
        
        if (type == "WIRECATE") {
            drawWireCratePattern(drawScope, pBotW, pH, actualPx, bottomY, lineColor)
            drawLeader(drawScope, "GABION WIRE CRATE", actualPx + pBotW, bottomY + pH/2f, actualPx + pBotW + 1.5f*pxPerMeter, bottomY + pH/2f, dimColor, pxPerMeter)
        } else if (type == "TOE") {
            val tW = params.toeTopWidth.toFloat() * pxPerMeter
            val bW = params.toeBottomWidth.toFloat() * pxPerMeter
            val path = Path().apply {
                moveTo(actualPx, bottomY)
                lineTo(actualPx + tW, bottomY)
                lineTo(actualPx + bW, bottomY + pH)
                lineTo(actualPx, bottomY + pH)
                close()
            }
            drawScope.clipPath(path) {
                drawSelectedPattern(this, params.toePatternType, maxOf(tW, bW), pH, actualPx, bottomY, pxPerMeter, lineColor)
            }
            drawScope.drawPath(path, color = lineColor, style = Stroke(2f))
            drawLeader(drawScope, "TOE WALL", actualPx + maxOf(tW, bW), bottomY + pH/2f, actualPx + maxOf(tW, bW) + 1.5f*pxPerMeter, bottomY + pH/2f, dimColor, pxPerMeter)
            drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.toeBottomWidth), actualPx, actualPx + bW, bottomY + pH + 0.4f * pxPerMeter, dimColor, true, pxPerMeter, textBgColor = bgColor)
            if (params.toeTopWidth != params.toeBottomWidth) {
                drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.toeTopWidth), actualPx, actualPx + tW, bottomY - 0.4f * pxPerMeter, dimColor, true, pxPerMeter, textBgColor = bgColor)
            }
        } else {
            val path = Path().apply {
                moveTo(actualPx, bottomY)
                lineTo(actualPx + pTopW, bottomY)
                lineTo(actualPx + pBotW, bottomY + pH)
                lineTo(actualPx, bottomY + pH)
                close()
            }
            drawScope.clipPath(path) {
                drawSelectedPattern(drawScope, params.plumPatternType, maxOf(pTopW, pBotW), pH, actualPx, bottomY, pxPerMeter, lineColor)
            }
            drawScope.drawPath(path, color = lineColor, style = Stroke(2f))
            drawLeader(drawScope, "PLUM CONCRETE 1:3:6", actualPx + maxOf(pTopW, pBotW), bottomY + pH/2f, actualPx + maxOf(pTopW, pBotW) + 1.5f*pxPerMeter, bottomY + pH/2f, dimColor, pxPerMeter)
        }
        
        if (type != "TOE") {
            drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", pc.bottomWidth), actualPx, actualPx + pBotW, bottomY + pH + 0.4f * pxPerMeter, dimColor, true, pxPerMeter, textBgColor = bgColor)
            if (pc.topWidth != pc.bottomWidth) {
                drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", pc.topWidth), actualPx, actualPx + pTopW, bottomY - 0.4f * pxPerMeter, dimColor, true, pxPerMeter, textBgColor = bgColor)
            }
        }
        drawDimVertical(drawScope, String.format(Locale.US, "%.2f", pc.height), actualPx + maxOf(pTopW, pBotW) + 0.4f * pxPerMeter, bottomY, bottomY + pH, dimColor, pxPerMeter, textLeft = false, textBgColor = bgColor)
        
        currentRightEdge = actualPx + pBotW
        bottomY += pH
    }
    
    if (params.isWeepHoleWallEnabled || params.isWeepHoleWireCrateEnabled || params.isWeepHolePlumEnabled || params.isWeepHoleToeWallEnabled) {
        val totalStructureHeight = bottomY - (baseY - h)
        if (totalStructureHeight > 1.0f * pxPerMeter) {
            val vSpace = 1.0f * pxPerMeter
            var yObj = bottomY - vSpace / 2f
            while (yObj > baseY - h) {
                var leftX = 0f
                var rightX = 0f
                var isWeepHereAllowed = false
                if (yObj <= baseY && params.isWallEnabled) {
                    leftX = getLeftX(yObj)
                    rightX = getRightX(yObj)
                    isWeepHereAllowed = params.isWeepHoleWallEnabled
                } else {
                    // find which block it is in
                    var blockTop = baseY
                    for (i in allBlocks.indices) {
                        val type = allBlocks[i].first
                        val pc = allBlocks[i].second
                        val pH = pc.height.toFloat() * pxPerMeter
                        if (yObj >= blockTop && yObj <= blockTop + pH) {
                            if (type == "WIRECATE" && params.isWeepHoleWireCrateEnabled) isWeepHereAllowed = true
                            if (type == "PLUM" && params.isWeepHolePlumEnabled) isWeepHereAllowed = true
                            if (type == "TOE" && params.isWeepHoleToeWallEnabled) isWeepHereAllowed = true
                            val pBotW = pc.bottomWidth.toFloat() * pxPerMeter
                            val pTopW = pc.topWidth.toFloat() * pxPerMeter
                            // For simplicity, just use max base width for weep holes
                            val pW = maxOf(pBotW, pTopW)
                            var currentRightEdgeForWeep = if (ignoreOffset) startX else bR.x
                            var bLeftPx = 0f
                            var bRightPx = 0f
                            for (j in 0..i) {
                                val b = allBlocks[j].second
                                val pTopWCurrent = b.topWidth.toFloat() * pxPerMeter
                                val pBotWCurrent = b.bottomWidth.toFloat() * pxPerMeter
                                val aOffset = b.offsetX.toFloat() * pxPerMeter
                                val topRightEdge = currentRightEdgeForWeep + aOffset
                                bLeftPx = topRightEdge - pTopWCurrent
                                bRightPx = bLeftPx + pBotWCurrent
                                currentRightEdgeForWeep = bRightPx
                            }
                            leftX = bLeftPx
                            rightX = bRightPx
                            break
                        }
                        blockTop += pH
                    }
                }
                
                if (isWeepHereAllowed && leftX != 0f && rightX != 0f) {
                    // slope 1:20 upwards towards left (so right side is lower, i.e., larger Y)
                    val drop = (rightX - leftX) / 20f
                    val dashColor = if (isBlueprintMode) Color(0xFF00FFFF) else Color.Blue
                    
                    val pipeEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    drawScope.drawLine(
                        color = dashColor,
                        start = Offset(leftX, yObj),
                        end = Offset(rightX, yObj + drop),
                        strokeWidth = 4f,
                        pathEffect = pipeEffect
                    )
                }
                yObj -= vSpace
            }
        }
    }

    if (params.isWallEnabled && params.height > 0.0) {
        // Dimensions
        val minLeftX = minOf(tL.x, bL.x)
        val dimLeftX = minLeftX - hpsWidthPx - 1.2f * pxPerMeter
        var dimLeftY = baseY - h
        
        for (slice in layerSlices) {
            drawDimVertical(drawScope, String.format(Locale.US, "%.2f", slice.hM), dimLeftX, slice.topY, slice.botY, dimColor, pxPerMeter, textLeft = true)
            if (slice.type == "RR Band") {
                // Draw bottom horizontal dimension only if not at very bottom of wall
                if (slice.botY < baseY) {
                    drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", (slice.bR.x - slice.bL.x)/pxPerMeter), slice.bL.x, slice.bR.x, slice.botY, dimColor, false, pxPerMeter, 0.15f, bgColor)
                }
            }
        }

        drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.topWidth), tL.x, tR.x, baseY - h - 0.4f*pxPerMeter, dimColor, true, pxPerMeter)
        
        if (params.isHpsEnabled) {
            drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.hpsTopWidth), hpsTopL.x, tL.x, baseY - h - 0.4f*pxPerMeter, dimColor, true, pxPerMeter)
            if (params.hpsBottomWidth > 0.0) {
                drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.hpsBottomWidth), hpsBotLeft.x, hpsBottomInner.x, hpsBottomInner.y + 0.3f*pxPerMeter, dimColor, false, pxPerMeter, textBgColor = bgColor)
            }
            drawDimVertical(drawScope, String.format(Locale.US, "%.2f", params.height/2f), minOf(hpsTopL.x, hpsBotLeft.x) - 0.4f*pxPerMeter, baseY - h, hpsBottomInner.y, dimColor, pxPerMeter, textLeft = true)
        }
        
        // Total Height right side
        drawDimVertical(drawScope, String.format(Locale.US, "%.2f", params.height), bR.x + 0.8f*pxPerMeter, tR.y, bR.y, dimColor, pxPerMeter)
        drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.bottomWidth), bL.x, bR.x, baseY + 0.4f*pxPerMeter, dimColor, false, pxPerMeter)

        // Leaders (Clean Labeling)
        val labelX = bL.x - hpsWidthPx - 4.0f * pxPerMeter
        
        if (params.isHpsEnabled) {
            drawHorizontalLabel(drawScope, "HPS FILLING", baseY - h + hpsHeightPx/3f, labelX, getLeftX(baseY - h + hpsHeightPx/3f) - hpsWidthPx/2f, dimColor, pxPerMeter)
        }

        for (slice in layerSlices) {
            val midY = (slice.topY + slice.botY) / 2f
            val midX = getLeftX(midY) + (getRightX(midY) - getLeftX(midY)) / 2f
            if (slice.type == "RR Band" && params.showRrBandLabel) {
                drawHorizontalLabel(drawScope, "RR 1:6 BAND", midY, labelX, midX, dimColor, pxPerMeter)
            } else if (slice.type == "RR Dry" && params.showRrDryLabel) {
                drawHorizontalLabel(drawScope, "RR DRY", midY, labelX, midX, dimColor, pxPerMeter)
            }
        }
    }
}

fun drawFrontElevation(drawScope: DrawScope, params: WallParams, pxPerMeter: Float, feStartX: Float, baseY: Float, lineColor: Color, bgColor: Color, isBlueprintMode: Boolean) {
    val dimColor = if (isBlueprintMode) Color.Yellow else Color.Blue
    val h = params.height.toFloat() * pxPerMeter
    val isTruncated = params.length > 30.0
    val drawLenM = if (isTruncated) 6.0f else params.length.toFloat()
    val drawToeLenM = if (isTruncated) minOf(drawLenM, params.toeLength.toFloat()) else params.toeLength.toFloat()
    val len = drawLenM * pxPerMeter
    val feEndX = feStartX + len
    val feTopY = baseY - h
    
    val titleY = if (params.isWallEnabled) feTopY else baseY
    
    drawText(drawScope, "FRONT ELEVATION", feStartX + len/2f - 2.0f*pxPerMeter, titleY - 2.2f*pxPerMeter, lineColor, bold = true, size = 0.35f*pxPerMeter)
    drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.length), feStartX, feEndX, titleY - 0.8f*pxPerMeter, dimColor, true, pxPerMeter)

    if (params.isWallEnabled) {
        data class FeSlice(val type: String, val topY: Float, val botY: Float)
        val feSlices = mutableListOf<FeSlice>()
        var feCy = feTopY
        for (layer in generateWallLayers(params)) {
            val hPx = layer.second.toFloat() * pxPerMeter
            feSlices.add(FeSlice(layer.first, feCy, feCy + hPx))
            feCy += hPx
        }

        val stoneLineColor = if(isBlueprintMode) bgColor else Color.White
        val stoneBgColor = if(isBlueprintMode) lineColor else Color(0xFF2C2C2C)

        fun drawPanels(panelType: String, yTop: Float, yBot: Float) {
            val stripH = yBot - yTop
            if (stripH <= 0f) return
            
            val pType = if (panelType == "RR 1:6") params.rr16PatternType else params.rrDryPatternType
            val pEnabled = if (panelType == "RR 1:6") params.isRr16Enabled else params.isRrDryEnabled
            
            val bandWM = if (params.topBandThickness > 0.0) params.topBandThickness.toFloat() else params.autoBandThickness.toFloat()
            if (bandWM <= 0f || (!params.isAutoAlternateBands && params.topBandThickness <= 0.0)) {
                if (pEnabled) {
                    drawScope.clipRect(feStartX, yTop, feEndX, yBot) {
                        drawSelectedPattern(this, pType, len, stripH, feStartX, yTop, pxPerMeter, lineColor)
                    }
                }
                drawScope.drawRect(color = stoneLineColor, topLeft = Offset(feStartX, yTop), size = Size(len, stripH), style = Stroke(2f))
                return
            }

            var numBands = java.lang.Math.round(drawLenM / 2.5).toInt() + 1
            var panelWM = (drawLenM - numBands * bandWM) / (numBands - 1)
            var actualNumBands = numBands
            if (panelWM < 0f) {
                actualNumBands = kotlin.math.max(2, (drawLenM / bandWM).toInt())
                panelWM = 0f
            }
            val numPanels = actualNumBands - 1
            
            val bandWPx = bandWM * pxPerMeter
            val panelWPx = panelWM * pxPerMeter
            
            var currentX = feStartX
            for (i in 0 until actualNumBands) {
                // Draw column (band)
                if (params.isRrBandEnabled) {
                    drawSelectedPattern(drawScope, params.rrBandPatternType, bandWPx, stripH, currentX, yTop, pxPerMeter, lineColor)
                }
                drawScope.drawRect(
                    color = lineColor,
                    topLeft = Offset(currentX, yTop),
                    size = Size(bandWPx, stripH),
                    style = Stroke(2f)
                )
                currentX += bandWPx
                
                if (i < numPanels && panelWPx > 0) {
                    if (pEnabled) {
                        drawScope.clipRect(currentX, yTop, currentX + panelWPx, yBot) {
                            drawSelectedPattern(this, pType, panelWPx, stripH, currentX, yTop, pxPerMeter, lineColor)
                        }
                    }
                    drawScope.drawRect(color = stoneLineColor, topLeft = Offset(currentX, yTop), size = Size(panelWPx, stripH), style = Stroke(2f))
                    currentX += panelWPx
                }
            }
        }
        
        for ((idx, slice) in feSlices.withIndex()) {
            val stripH = slice.botY - slice.topY
            if (stripH <= 0f) continue
            if (slice.type == "RR Band") {
                if (params.isRrBandEnabled) {
                    drawScope.clipRect(feStartX, slice.topY, feEndX, slice.botY) {
                        drawSelectedPattern(this, params.rrBandPatternType, len, stripH, feStartX, slice.topY, pxPerMeter, lineColor)
                    }
                }
                drawScope.drawRect(color = lineColor, topLeft = Offset(feStartX, slice.topY), size = Size(len, stripH), style = Stroke(2f))
                
                var topDimCheck = if (!params.isAutoAlternateBands && params.topBandThickness <= 0.0) false else slice.topY == feTopY
                if (topDimCheck) {
                    val bandWM = if (params.topBandThickness > 0.0) params.topBandThickness.toFloat() else params.autoBandThickness.toFloat()
                    var dimX = feStartX
                    var numBands = java.lang.Math.round(drawLenM / 2.5).toInt() + 1
                    var panelWM = (drawLenM - numBands * bandWM) / (numBands - 1)
                    var actualNumBands = numBands
                    if (panelWM < 0f) {
                        actualNumBands = kotlin.math.max(2, (drawLenM / bandWM).toInt())
                        panelWM = 0f
                    }
                    val numPanels = actualNumBands - 1
                    val bandWPx = bandWM * pxPerMeter
                    val panelWPx = panelWM * pxPerMeter
                    for (i in 0 until actualNumBands) {
                        drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", bandWM), dimX, dimX + bandWPx, feTopY - 0.3f*pxPerMeter, dimColor, true, pxPerMeter, 0.15f)
                        dimX += bandWPx
                        if (i < numPanels && panelWPx > 0) {
                            drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", panelWM), dimX, dimX + panelWPx, feTopY - 0.3f*pxPerMeter, dimColor, true, pxPerMeter, 0.15f)
                            dimX += panelWPx
                        }
                    }
                }
            } else {
                drawPanels(slice.type, slice.topY, slice.botY)
            }
        }

        drawScope.drawRect(lineColor, Offset(feStartX, feTopY), Size(len, h), style = Stroke(4f))

        var rDimTop = feTopY
        for (slice in feSlices) {
            drawDimVertical(drawScope, String.format(Locale.US, "%.2f", slice.botY - slice.topY), feEndX + 0.4f*pxPerMeter, slice.topY, slice.botY, dimColor, pxPerMeter)
        }
        drawDimVertical(drawScope, String.format(Locale.US, "%.2f", params.height), feEndX + 1.0f*pxPerMeter, feTopY, baseY, dimColor, pxPerMeter)
    }

    // Plums in front elevation
    var fePlumY = baseY
    val toeBlock = if (params.isToeWallEnabled) listOf(Pair("TOE", PlumConcrete(params.toeTopWidth, params.toeBottomWidth, params.toeHeight, params.toeOffsetX))) else emptyList<Pair<String, PlumConcrete>>()
    val allBlocks: List<Pair<String, PlumConcrete>> = toeBlock + params.wireCrates.map { Pair("WIRECATE", it) } + params.plumConcretes.map { Pair("PLUM", it) }
    
    allBlocks.forEach { (type, pc) ->
        val pH = pc.height.toFloat() * pxPerMeter
        val blockLen = if (type == "TOE") drawToeLenM * pxPerMeter else len
        val blockStartX = if (type == "TOE") feStartX + (len - blockLen) / 2f else feStartX
        val blockEndX = blockStartX + blockLen
        
        if (type == "WIRECATE") {
            drawWireCratePattern(drawScope, blockLen, pH, blockStartX, fePlumY, lineColor)
        } else if (type == "TOE") {
            drawSelectedPattern(drawScope, params.toePatternType, blockLen, pH, blockStartX, fePlumY, pxPerMeter, lineColor)
            drawScope.drawRect(lineColor, Offset(blockStartX, fePlumY), Size(blockLen, pH), style = Stroke(2f)) // frame it
        } else {
            drawSelectedPattern(drawScope, params.plumPatternType, blockLen, pH, blockStartX, fePlumY, pxPerMeter, lineColor)
            drawScope.drawRect(lineColor, Offset(blockStartX, fePlumY), Size(blockLen, pH), style = Stroke(2f)) // frame it
        }
        
        drawDimVertical(drawScope, String.format(Locale.US, "%.2f", pc.height), feEndX + 0.4f*pxPerMeter, fePlumY, fePlumY + pH, dimColor, pxPerMeter)
        
        if (type == "TOE" && params.toeLength < params.length) {
            drawDimHorizontal(drawScope, String.format(Locale.US, "%.2f", params.toeLength), blockStartX, blockEndX, fePlumY + pH + 0.4f*pxPerMeter, dimColor, false, pxPerMeter)
        }
        
        // vertical Separation Lines
        if (params.isWallEnabled && params.topBandThickness > 0) {
            val bandWM = params.topBandThickness.toFloat()
            var numBands = Math.round(drawLenM / 2.5).toInt() + 1
            var panelWM = (drawLenM - numBands * bandWM) / (numBands - 1)
            var actualNumBands = numBands
            if (panelWM < 0f) {
                actualNumBands = kotlin.math.max(2, (drawLenM / bandWM).toInt())
                panelWM = 0f
            }
            val bandWPx = bandWM * pxPerMeter
            val panelWPx = panelWM * pxPerMeter
            
            var currentX = feStartX
            for (i in 0 until actualNumBands) {
                if (currentX >= blockStartX - 1f && currentX <= blockEndX + 1f) {
                    val midX = currentX + bandWPx / 2f
                    if (midX >= blockStartX && midX <= blockEndX) {
                        drawScope.drawLine(lineColor, Offset(midX, fePlumY), Offset(midX, fePlumY + pH), strokeWidth = 2f)
                    }
                }
                currentX += bandWPx + panelWPx
            }
        } else {
            var sepX = blockStartX + 2.0f * pxPerMeter
            while (sepX < blockEndX) {
                drawScope.drawLine(lineColor, Offset(sepX, fePlumY), Offset(sepX, fePlumY + pH), strokeWidth = 2f)
                sepX += 2.0f * pxPerMeter
            }
        }
        
        fePlumY += pH
    }
    
    if (isTruncated) {
        val midX = feStartX + 3.0f * pxPerMeter
        val startY = if (params.isWallEnabled) feTopY else baseY
        val totalH = fePlumY - startY
        
        // Single vertical red line
        val y1 = startY - 0.5f * pxPerMeter
        val y2 = startY + totalH + 0.5f * pxPerMeter
        
        val path = Path().apply {
            moveTo(midX, y1)
            lineTo(midX, y2)
        }
        
        // Halo effect to make the line readable over hatchings
        drawScope.drawPath(path, bgColor, style = Stroke(6f))
        drawScope.drawPath(path, Color.Red, style = Stroke(3f))
    }
    
    if (params.isWeepHoleWallEnabled || params.isWeepHoleWireCrateEnabled || params.isWeepHolePlumEnabled || params.isWeepHoleToeWallEnabled) {
        val totalStructureHeight = fePlumY - (if (params.isWallEnabled) feTopY else baseY)
        if (totalStructureHeight > 1.0f * pxPerMeter && params.length > 0.0) {
            val vSpace = 1.0f * pxPerMeter
            val hSpace = 1.0f * pxPerMeter
            val radius = 0.05f * pxPerMeter
            
            var yObj = fePlumY - vSpace / 2f
            while (yObj > (if (params.isWallEnabled) feTopY else baseY) + radius) {
                var isWeepHereAllowed = false
                var blockStartX = feStartX
                var blockEndX = feEndX
                if (yObj <= baseY && params.isWallEnabled) {
                    isWeepHereAllowed = params.isWeepHoleWallEnabled
                } else {
                    var blockTop = baseY
                    for (i in allBlocks.indices) {
                        val type = allBlocks[i].first
                        val pc = allBlocks[i].second
                        val pH = pc.height.toFloat() * pxPerMeter
                        if (yObj >= blockTop && yObj <= blockTop + pH) {
                            if (type == "WIRECATE" && params.isWeepHoleWireCrateEnabled) isWeepHereAllowed = true
                            if (type == "PLUM" && params.isWeepHolePlumEnabled) isWeepHereAllowed = true
                            if (type == "TOE" && params.isWeepHoleToeWallEnabled) {
                                isWeepHereAllowed = true
                                val toeL = params.toeLength.toFloat() * pxPerMeter
                                blockStartX = feStartX + (len - toeL) / 2f
                                blockEndX = blockStartX + toeL
                            }
                            break
                        }
                        blockTop += pH
                    }
                }

                if (isWeepHereAllowed) {
                    var xObj = blockStartX + hSpace / 2f
                    while (xObj < blockEndX - radius) {
                        val outC = if (isBlueprintMode) Color(0xFF00FFFF) else Color.Blue
                        
                        // Just an 'O' circle
                        drawScope.drawCircle(color = outC, radius = radius, center = Offset(xObj, yObj), style = Stroke(2f))
                        
                        xObj += hSpace
                    }
                }
                yObj -= vSpace
            }
        }
    }
}

fun DrawScope.drawStonePolygons(
    areaLeft: Float,
    areaTop: Float, 
    areaRight: Float,
    areaBottom: Float,
    stoneFill: Color,
    stoneOutline: Color,
    outlineWidth: Float = 1.5f,
    seed: Int = 0,
    pxPerMeter: Float,
    smallStones: Boolean = false
) {
    val stoneSize = if (smallStones) 
        0.22f * pxPerMeter
    else 
        0.35f * pxPerMeter
    
    val random = java.util.Random(seed.toLong())
    val cols = kotlin.math.ceil((areaRight - areaLeft) / stoneSize).toInt() + 1
    val rows = kotlin.math.ceil((areaBottom - areaTop) / stoneSize).toInt() + 1
    
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            val cx = areaLeft + col * stoneSize + 
                     random.nextFloat() * stoneSize * 0.25f
            val cy = areaTop + row * stoneSize + 
                     random.nextFloat() * stoneSize * 0.25f
            
            val sides = if (smallStones) 
                6 + random.nextInt(3)
            else 
                5 + random.nextInt(4)
            
            val path = Path()
            for (i in 0 until sides) {
                val angle = (2.0 * Math.PI * i / sides).toFloat() +
                            random.nextFloat() * 0.3f
                val radiusVariation = if (smallStones)
                    0.75f + random.nextFloat() * 0.25f
                else
                    0.55f + random.nextFloat() * 0.45f
                val radius = stoneSize * 0.42f * radiusVariation
                val px = cx + kotlin.math.cos(angle) * radius
                val py = cy + kotlin.math.sin(angle) * radius
                if (i == 0) path.moveTo(px, py)
                else path.lineTo(px, py)
            }
            path.close()
            
            drawPath(path, stoneFill, style = androidx.compose.ui.graphics.drawscope.Fill)
            drawPath(path, stoneOutline, 
                     style = Stroke(width = outlineWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round))
        }
    }
}

fun drawHpsPattern(drawScope: DrawScope, width: Float, height: Float, px: Float, py: Float, pxPerMeter: Float, lineColor: Color) {
    val isBlueprint = lineColor != Color.Black
    val bg = if (isBlueprint) Color(0xFF0D1B2A) else Color.White
    val sFill = if (isBlueprint) Color(0xFF1A3A5C) else Color(0xFFEFEFEF)
    val sOutline = if (isBlueprint) Color(0xFF00FFFF) else Color.Gray
    
    drawScope.clipRect(px, py, px + width, py + height) {
        drawRect(color = bg, topLeft = Offset(px, py), size = Size(width, height))
        drawStonePolygons(px, py, px+width, py+height, sFill, sOutline, 1.5f, (px+py).toInt().hashCode() + 3, pxPerMeter, true) 
    }
}

fun drawSelectedPattern(
    drawScope: DrawScope,
    patternType: String,
    width: Float,
    height: Float,
    px: Float,
    py: Float,
    pxPerMeter: Float,
    lineColor: Color,
    bgColor: Color? = null
) {
    val isBlueprintMode = lineColor != Color.Black
    val stoneLineColor = if(isBlueprintMode && bgColor != null) bgColor else lineColor
    val stoneBgColor = if(isBlueprintMode) lineColor else bgColor
    
    when (patternType) {
        "Stone" -> drawStonePattern(drawScope, width, height, px, py, 0.4f * pxPerMeter, stoneLineColor, stoneBgColor)
        "Concrete" -> drawConcreteHatch(drawScope, width, height, px, py, lineColor)
        "Plum" -> drawPlumHatch(drawScope, width, height, px, py, lineColor)
        "Solid" -> drawScope.drawRect(color = stoneBgColor ?: Color.LightGray, topLeft = Offset(px, py), size = Size(width, height))
        else -> drawStonePattern(drawScope, width, height, px, py, 0.4f * pxPerMeter, stoneLineColor, stoneBgColor) // fallback
    }
}

fun drawStonePattern(drawScope: DrawScope, width: Float, height: Float, px: Float, py: Float, stoneSizePx: Float, lineColor: Color, fillColor: Color?) {
    val pxPerMeter = stoneSizePx / 0.4f
    
    val isBlueprint = lineColor != Color.Black
    val bg = if (isBlueprint) Color(0xFF0D1B2A) else Color.White
    val sFill = if (isBlueprint) Color(0xFF1A3A5C) else Color.White
    val sOutline = if (isBlueprint) Color(0xFF00FFFF) else Color.Black
    
    drawScope.clipRect(px, py, px + width, py + height) {
        if (fillColor != null) {
            val fePanelBg = if (isBlueprint) Color(0xFF111111) else Color.White
            val feStoneFill = if (isBlueprint) Color(0xFF162032) else Color.White
            val feStoneOut = if (isBlueprint) Color.White else Color.Black
            drawRect(color = fePanelBg, topLeft = Offset(px, py), size = Size(width, height))
            drawStonePolygons(px, py, px+width, py+height, feStoneFill, feStoneOut, 1.5f, (px+py).toInt().hashCode(), pxPerMeter, false)
        } else {
            val rrDryBg = if (isBlueprint) Color(0xFF0D1B2A) else Color.White
            drawRect(color = rrDryBg, topLeft = Offset(px, py), size = Size(width, height))
            drawStonePolygons(px, py, px+width, py+height, sFill, sOutline, 2f, (px+py).toInt().hashCode(), pxPerMeter, false)
        }
    }
}

fun drawWireCratePattern(drawScope: DrawScope, width: Float, height: Float, px: Float, py: Float, lineColor: Color) {
    val isBlueprint = lineColor != Color.Black
    val bg = if (isBlueprint) Color(0xFF0D1B2A) else Color.White
    drawScope.withTransform({ translate(px, py) }) {
        clipRect(0f, 0f, width, height) {
            drawRect(color = bg, size = Size(width, height))
            drawRect(color = lineColor, size = Size(width, height), style = Stroke(2f))
            val spacing = 12f
            
            var currentY = 0f
            while (currentY < height) {
                var currentX = 0f
                while (currentX < width) {
                    drawLine(lineColor.copy(0.5f), Offset(currentX, currentY), Offset(currentX + spacing, currentY + spacing), strokeWidth = 1.5f)
                    drawLine(lineColor.copy(0.5f), Offset(currentX + spacing, currentY), Offset(currentX, currentY + spacing), strokeWidth = 1.5f)
                    currentX += spacing
                }
                currentY += spacing
            }
        }
    }
}

fun drawConcreteHatch(drawScope: DrawScope, width: Float, height: Float, px: Float, py: Float, lineColor: Color) {
    val isBlueprint = lineColor != Color.Black
    val bg = if (isBlueprint) Color(0xFF112233) else Color.LightGray.copy(alpha=0.3f)
    drawScope.withTransform({ translate(px, py) }) {
        clipRect(0f, 0f, width, height) {
            drawRect(color = bg, size = Size(width, height))
            drawRect(color = lineColor, size = Size(width, height), style = Stroke(2f))
            val spacing = 8f
            val extension = maxOf(width, height) * 1.5f
            var startX = -extension
            while (startX < width + extension) {
                drawLine(
                    color = lineColor.copy(alpha = 0.6f),
                    start = Offset(startX, height),
                    end = Offset(startX + height, 0f),
                    strokeWidth = 1.5f
                )
                startX += spacing
            }
        }
    }
}

fun drawPlumHatch(drawScope: DrawScope, width: Float, height: Float, px: Float, py: Float, lineColor: Color) {
    val isBlueprint = lineColor != Color.Black
    val bg = if (isBlueprint) Color(0xFF111111) else Color(0xFFEEEEEE)
    drawScope.withTransform({ translate(px, py) }) {
        clipRect(0f, 0f, width, height) {
            drawRect(color = bg, size = Size(width, height))
            drawRect(color = lineColor, size = Size(width, height), style = Stroke(2f))
            
            val rnd = Random((px+py).toInt().hashCode() + 42)
            val path = Path()
            val countDots = Math.max(15, (width * height / 600f).toInt())
            val countTriangles = Math.max(8, (width * height / 800f).toInt())
            
            for (i in 0 until countDots) {
                val hx = rnd.nextFloat() * width
                val hy = rnd.nextFloat() * height
                drawCircle(color = lineColor, radius = 2f, center = Offset(hx, hy))
            }
            for (i in 0 until countTriangles) {
                val hx = rnd.nextFloat() * width
                val hy = rnd.nextFloat() * height
                path.moveTo(hx, hy - 4f)
                path.lineTo(hx + 4f, hy + 4f)
                path.lineTo(hx - 4f, hy + 4f)
                path.close()
            }
            drawPath(path, color = lineColor, style = Stroke(1.5f))
            
            val spacing = 20f
            val extension = maxOf(width, height) * 1.5f
            var startX = -extension
            while (startX < width + extension) {
                drawLine(
                    color = lineColor.copy(alpha = 0.4f),
                    start = Offset(startX, height),
                    end = Offset(startX + height, 0f),
                    strokeWidth = 1.5f
                )
                startX += spacing
            }
        }
    }
}

fun drawDimHorizontal(drawScope: DrawScope, text: String, x1: Float, x2: Float, y: Float, color: Color, textAbove: Boolean = true, pxPerMeter: Float, textSize: Float = 0.2f, textBgColor: Color? = null) {
    drawScope.drawLine(color = color, start = Offset(x1, y), end = Offset(x2, y), strokeWidth = 1.2f)
    // witness lines
    drawScope.drawLine(color = color, start = Offset(x1, y - 8f), end = Offset(x1, y + 8f), strokeWidth = 0.8f)
    drawScope.drawLine(color = color, start = Offset(x2, y - 8f), end = Offset(x2, y + 8f), strokeWidth = 0.8f)
    
    drawArrowHead(drawScope, x1, y, color)
    drawArrowHead(drawScope, x2, y, color)
    
    val midX = (x1 + x2) / 2f
    val tY = if (textAbove) y - 10f else y + textSize*1.1f*pxPerMeter
    val bg = textBgColor ?: if (color == Color.Yellow) Color(0xFF003366) else Color.White
    drawText(drawScope, text, midX, tY, color, size = textSize*pxPerMeter, bgColor = bg, align = android.graphics.Paint.Align.CENTER)
}

fun drawDimVertical(drawScope: DrawScope, text: String, x: Float, y1: Float, y2: Float, color: Color, pxPerMeter: Float, textSize: Float = 0.2f, textLeft: Boolean = false, textBgColor: Color? = null) {
    drawScope.drawLine(color = color, start = Offset(x, y1), end = Offset(x, y2), strokeWidth = 1.2f)
    drawScope.drawLine(color = color, start = Offset(x - 8f, y1), end = Offset(x + 8f, y1), strokeWidth = 0.8f)
    drawScope.drawLine(color = color, start = Offset(x - 8f, y2), end = Offset(x + 8f, y2), strokeWidth = 0.8f)
    
    drawArrowHead(drawScope, x, y1, color)
    drawArrowHead(drawScope, x, y2, color)
    
    val midY = (y1 + y2) / 2f
    val textX = if (textLeft) x - 0.15f * pxPerMeter else x + 0.15f * pxPerMeter
    val bg = textBgColor ?: if (color == Color.Yellow) Color(0xFF003366) else Color.White
    drawText(drawScope, text, textX, midY + textSize*0.4f*pxPerMeter, color, size = textSize*pxPerMeter, bgColor = bg, align = if(textLeft) android.graphics.Paint.Align.RIGHT else android.graphics.Paint.Align.LEFT)
}

fun drawArrowHead(drawScope: DrawScope, x: Float, y: Float, color: Color) {
    // 45 degree diagonal slash
    drawScope.drawLine(
        color = color, 
        start = Offset(x - 6f, y + 6f), 
        end = Offset(x + 6f, y - 6f), 
        strokeWidth = 1.2f
    )
}

fun drawHorizontalLabel(drawScope: DrawScope, text: String, tgtY: Float, textX: Float, endX: Float, color: Color, pxPerMeter: Float) {
    if (text == "HPS FILLING") {
        drawScope.drawLine(color = color, start = Offset(textX + 2.5f*pxPerMeter, tgtY), end = Offset(endX, tgtY), strokeWidth = 1.5f)
    }
    val bg = if (color == Color.Yellow) Color(0xFF003366) else Color.White
    drawText(drawScope, text, textX, tgtY + 0.1f*pxPerMeter, color, size = 0.22f * pxPerMeter, bgColor = bg)
}

fun drawLeader(drawScope: DrawScope, text: String, tgtX: Float, tgtY: Float, outX: Float, outY: Float, color: Color, pxPerMeter: Float) {
    val landingX = if(outX < tgtX) outX + 30f else outX - 30f
    drawScope.drawLine(color = color, start = Offset(tgtX, tgtY), end = Offset(landingX, outY), strokeWidth = 1.2f)
    drawScope.drawLine(color = color, start = Offset(landingX, outY), end = Offset(outX, outY), strokeWidth = 1.2f)
    
    // Leader Arrowhead (points into tgtX, tgtY)
    val dx = tgtX - landingX
    val dy = tgtY - outY
    val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
    if (len > 0f) {
        val dirX = dx / len
        val dirY = dy / len
        val normX = -dirY
        val normY = dirX
        val arrowSize = 10f
        
        val p1x = tgtX - dirX * arrowSize + normX * arrowSize * 0.25f
        val p1y = tgtY - dirY * arrowSize + normY * arrowSize * 0.25f
        
        val p2x = tgtX - dirX * arrowSize - normX * arrowSize * 0.25f
        val p2y = tgtY - dirY * arrowSize - normY * arrowSize * 0.25f
        
        val path = Path().apply { moveTo(tgtX, tgtY); lineTo(p1x, p1y); lineTo(p2x, p2y); close() }
        drawScope.drawPath(path, color = color, style = androidx.compose.ui.graphics.drawscope.Fill)
    }
    
    val tX = if (outX < tgtX) outX - (text.length * 0.18f * pxPerMeter * 0.55f) else outX + 5f
    val bg = if (color == Color.Yellow) Color(0xFF003366) else Color.White
    drawText(drawScope, text, tX, outY - 5f, color, size = 0.18f * pxPerMeter, bgColor = bg)
}

fun drawText(drawScope: DrawScope, text: String, x: Float, y: Float, color: Color, bold: Boolean = false, size: Float = 24f, bgColor: Color? = null, align: Paint.Align = Paint.Align.LEFT) {
    drawScope.drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = android.graphics.Color.argb(color.alpha, color.red, color.green, color.blue)
            this.textSize = size
            this.typeface = if (bold) Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) else Typeface.MONOSPACE
            this.textAlign = align
            this.isAntiAlias = true
        }
        if (bgColor != null) {
            val bgPaint = Paint().apply {
                this.color = android.graphics.Color.argb(bgColor.alpha, bgColor.red, bgColor.green, bgColor.blue)
                this.style = android.graphics.Paint.Style.FILL
            }
            val textWidth = paint.measureText(text)
            val fm = paint.fontMetrics
            val left = when(align) {
                Paint.Align.CENTER -> x - textWidth/2f - 2f
                Paint.Align.RIGHT -> x - textWidth - 2f
                else -> x - 2f
            }
            canvas.nativeCanvas.drawRect(left, y + fm.ascent - 2f, left + textWidth + 4f, y + fm.descent + 2f, bgPaint)
        }
        canvas.nativeCanvas.drawText(text, x, y, paint)
    }
}
