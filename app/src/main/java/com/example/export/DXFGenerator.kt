package com.example.export

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.model.WallParams
import com.example.model.generateWallLayers
import com.example.model.PlumConcrete
import java.io.File
import java.util.Locale

object DXFGenerator {
    
    fun shareDXF(context: Context, params: WallParams) {
        try {
            val dxfContent = generate(params)
            val file = File(context.cacheDir, "RetainingWall_RW001.dxf")
            file.writeText(dxfContent, Charsets.US_ASCII)
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/dxf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share DXF Document"))
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to generate DXF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun generate(params: WallParams): String {
        val sb = StringBuilder()
        
        // FORMAT
        fun fd(d: Double): String = String.format(Locale.US, "%.5f", d)

        // HEADER
        sb.append("  0\nSECTION\n  2\nHEADER\n")
        sb.append("  9\n\$ACADVER\n  1\nAC1009\n")
        sb.append("  0\nENDSEC\n")
        
        // TABLES with LAYERS
        sb.append("  0\nSECTION\n  2\nTABLES\n")
        sb.append("  0\nTABLE\n  2\nLAYER\n 70\n5\n")
        sb.append(layerDef("WALL_OUTLINE", 7))   // color 7 = white/black
        sb.append(layerDef("DIMENSIONS", 3))      // color 3 = green
        sb.append(layerDef("HATCHING", 8))        // color 8 = gray
        sb.append(layerDef("LABELS", 2))          // color 2 = yellow
        sb.append(layerDef("TITLE_BLOCK", 7))
        sb.append("  0\nENDTAB\n  0\nENDSEC\n")
        
        // BLOCKS
        sb.append("  0\nSECTION\n  2\nBLOCKS\n")
        sb.append("  0\nENDSEC\n")
        
        // ENTITIES
        sb.append("  0\nSECTION\n  2\nENTITIES\n")
        
        // Trapezoid
        val h = params.height
        val tw = params.topWidth
        val bw = params.bottomWidth
        val hpsTop = if (params.isHpsEnabled) params.hpsTopWidth else 0.0
        val hpsBotWidth = if (params.isHpsEnabled) params.hpsBottomWidth else 0.0
        val hps = maxOf(hpsTop, hpsBotWidth)
        val hpsH = h / 2.0
        val backSlopeW = if (params.backSlopeRatio > 0.0) h / params.backSlopeRatio else 0.0
        
        val tL = Pair(0.0, h)
        val tR = Pair(tw, h)
        val bL = Pair(-backSlopeW, 0.0)
        val bR = Pair(-backSlopeW + bw, 0.0)
        
        fun getLeftX(y: Double): Double {
            val p = y / h
            return bL.first + p * (tL.first - bL.first)
        }
        
        fun getRightX(y: Double): Double {
            val p = y / h
            return bR.first + p * (tR.first - bR.first)
        }

        val hpsTopL = Pair(tL.first - hpsTop, h)
        val hpsBotInner = Pair(getLeftX(h - hpsH), h - hpsH)
        val hpsBot = Pair(hpsBotInner.first - hpsBotWidth, h - hpsH)

        // Wall Outline
        val ignoreWall = !params.isWallEnabled
        val ignoreOffset = ignoreWall
        val layers = generateWallLayers(params)
        
        if (!ignoreWall) {
            sb.append(textEntity("LABELS", "CROSS SECTION", tL.first - 1.0, tR.second + 2.2, 0.35))
            sb.append(lineEntity("WALL_OUTLINE", bL.first, bL.second, bR.first, bR.second))
            sb.append(lineEntity("WALL_OUTLINE", bR.first, bR.second, tR.first, tR.second))
            sb.append(lineEntity("WALL_OUTLINE", tR.first, tR.second, tL.first, tL.second))
            sb.append(lineEntity("WALL_OUTLINE", tL.first, tL.second, bL.first, bL.second))
            
            var currentY = h
            for (layer in layers) {
                val layerH = layer.second
                val nextY = currentY - layerH
                if (nextY > 0.0) {
                    sb.append(lineEntity("WALL_OUTLINE", getLeftX(nextY), nextY, getRightX(nextY), nextY))
                }
                currentY = nextY
            }
            
            if (params.isHpsEnabled) {
                // HPS Outline (Quad)
                sb.append(lineEntity("WALL_OUTLINE", hpsTopL.first, hpsTopL.second, tL.first, tL.second))
                sb.append(lineEntity("WALL_OUTLINE", tL.first, tL.second, hpsBotInner.first, hpsBotInner.second))
                sb.append(lineEntity("WALL_OUTLINE", hpsBotInner.first, hpsBotInner.second, hpsBot.first, hpsBot.second))
                sb.append(lineEntity("WALL_OUTLINE", hpsBot.first, hpsBot.second, hpsTopL.first, hpsTopL.second))
            }
        }

        // Plum Concrete and Wire Crates
        var pY = 0.0
        var currentRightEdge = if (ignoreOffset) 0.0 else bR.first
        
        val toeBlock = if (params.isToeWallEnabled) listOf(Pair("TOE", PlumConcrete(params.toeTopWidth, params.toeBottomWidth, params.toeHeight, params.toeOffsetX))) else emptyList<Pair<String, PlumConcrete>>()
        val allBlocks: List<Pair<String, PlumConcrete>> = toeBlock + params.wireCrates.map { Pair("WIRECATE", it) } + params.plumConcretes.map { Pair("PLUM", it) }
        
        allBlocks.forEachIndexed { index, (type, pc) ->
            val actualOffsetX = pc.offsetX
            val topRightEdge = currentRightEdge + actualOffsetX
            val pTopW = pc.topWidth
            val pBotW = pc.bottomWidth
            val maxW = Math.max(pTopW, pBotW)
            val pX = topRightEdge - pTopW
            val pBottom = pY - pc.height
            
            if (type == "TOE") {
                val tW = params.toeTopWidth
                val bW = params.toeBottomWidth
                sb.append(lineEntity("WALL_OUTLINE", pX, pY, pX + tW, pY))
                sb.append(lineEntity("WALL_OUTLINE", pX + tW, pY, pX + bW, pBottom))
                sb.append(lineEntity("WALL_OUTLINE", pX + bW, pBottom, pX, pBottom))
                sb.append(lineEntity("WALL_OUTLINE", pX, pBottom, pX, pY))
            } else {
                sb.append(lineEntity("WALL_OUTLINE", pX, pY, pX + pTopW, pY))
                sb.append(lineEntity("WALL_OUTLINE", pX + pTopW, pY, pX + pBotW, pBottom))
                sb.append(lineEntity("WALL_OUTLINE", pX + pBotW, pBottom, pX, pBottom))
                sb.append(lineEntity("WALL_OUTLINE", pX, pBottom, pX, pY))
            }
            
            val labelStr = if (type == "WIRECATE") "GABION WIRE CRATE" else if (type == "TOE") "TOE WALL" else "PLUM CONCRETE 1:3:6"
            sb.append(textEntity("LABELS", labelStr, pX - 3.5, pBottom + pc.height/2, 0.20))
            
            pY -= pc.height
            currentRightEdge = pX + pBotW
        }
        
        if (params.isWeepHoleWallEnabled || params.isWeepHoleWireCrateEnabled || params.isWeepHolePlumEnabled || params.isWeepHoleToeWallEnabled) {
            val totalStructureHeight = h - pY
            if (totalStructureHeight > 1.0) {
                var yObj = pY + 0.5
                while (yObj < h) {
                    var leftX = 0.0
                    var rightX = 0.0
                    var isWeepHereAllowed = false
                    if (yObj > 0.0 && params.isWallEnabled) {
                        leftX = getLeftX(yObj)
                        rightX = getRightX(yObj)
                        isWeepHereAllowed = params.isWeepHoleWallEnabled
                    } else {
                        var blockTop = 0.0
                        for (i in allBlocks.indices) {
                            val type = allBlocks[i].first
                            val pc = allBlocks[i].second
                            if (yObj <= blockTop && yObj >= blockTop - pc.height) {
                                if (type == "TOE" && params.isWeepHoleToeWallEnabled) isWeepHereAllowed = true
                                if (type == "WIRECATE" && params.isWeepHoleWireCrateEnabled) isWeepHereAllowed = true
                                if (type == "PLUM" && params.isWeepHolePlumEnabled) isWeepHereAllowed = true
                                var currentRightEdgeForWeep = if (ignoreOffset) 0.0 else bR.first
                                var bLeftPx = 0.0
                                var bRightPx = 0.0
                                for (j in 0..i) {
                                    val b = allBlocks[j].second
                                    val pTopWCurrent = b.topWidth
                                    val pBotWCurrent = b.bottomWidth
                                    val aOffset = b.offsetX
                                    val topRightEdge = currentRightEdgeForWeep + aOffset
                                    bLeftPx = topRightEdge - pTopWCurrent
                                    bRightPx = bLeftPx + pBotWCurrent
                                    currentRightEdgeForWeep = bRightPx
                                }
                                leftX = bLeftPx
                                rightX = bRightPx
                                break
                            }
                            blockTop -= pc.height
                        }
                    }
                    
                    if (isWeepHereAllowed && leftX != 0.0 && rightX != 0.0) {
                        val drop = (rightX - leftX) / 20.0
                        // DXF has Y pointing UP. Cross section Y is inverted originally?
                        // Wait, DXF cross section Y goes from pY (e.g. -2) up to h (e.g. 5). So y pointer is UP.
                        // We want water to drain out the face (right side). So left side is HIGHER than right side.
                        // Y goes UP, so right side Y should be smaller than left side Y.
                        sb.append(lineEntity("WALL_OUTLINE", leftX, yObj, rightX, yObj - drop))
                    }
                    yObj += 1.0
                }
            }
        }

        fun addDimLine(text: String, x1: Double, y1: Double, x2: Double, y2: Double, textOffset: Double, horizontal: Boolean) {
            sb.append(lineEntity("DIMENSIONS", x1, y1, x2, y2))
            sb.append(arrowEntity("DIMENSIONS", x1, y1, x2 - x1, y2 - y1))
            sb.append(arrowEntity("DIMENSIONS", x2, y2, x1 - x2, y1 - y2))
            
            // Text position
            val midX = (x1 + x2) / 2
            val midY = (y1 + y2) / 2
            
            if (horizontal) {
                sb.append(lineEntity("DIMENSIONS", x1, y1 - 0.2, x1, y1 + 0.2))
                sb.append(lineEntity("DIMENSIONS", x2, y2 - 0.2, x2, y2 + 0.2))
                sb.append(textEntity("DIMENSIONS", text, midX - text.length * 0.05, midY + textOffset, 0.15))
            } else {
                sb.append(lineEntity("DIMENSIONS", x1 - 0.2, y1, x1 + 0.2, y1))
                sb.append(lineEntity("DIMENSIONS", x2 - 0.2, y2, x2 + 0.2, y2))
                sb.append(textEntity("DIMENSIONS", text, midX + textOffset, midY, 0.15))
            }
        }
        
        // Add dimension lines mirror-ing canvas
        if (!ignoreWall) {
            addDimLine("${fd(tw)}", tL.first, tL.second + 0.5, tR.first, tR.second + 0.5, 0.1, true)
            addDimLine("${fd(bw)}", bL.first, bL.second - 0.5, bR.first, bR.second - 0.5, -0.3, true)
            addDimLine("${fd(h)}", bR.first + 1.0, tR.second, bR.first + 1.0, bR.second, 0.2, false)
            
            var currentYDim = h
            for (layer in layers) {
                val layerH = layer.second
                val nextY = currentYDim - layerH
                addDimLine("${fd(layerH)}", bL.first - 1.0, currentYDim, bL.first - 1.0, nextY, -0.4, false)
                if (layer.first == "RR Band") {
                    // Bottom of band
                    if (nextY > 0.0) {
                        addDimLine("${fd(getRightX(nextY) - getLeftX(nextY))}", getLeftX(nextY), nextY, getRightX(nextY), nextY, 0.1, true)
                    }
                }
                currentYDim = nextY
            }
            
            if (params.isHpsEnabled) {
                // HPS Top Width dimension
                addDimLine("${fd(params.hpsTopWidth)}", hpsTopL.first, hpsTopL.second + 0.4, tL.first, tL.second + 0.4, 0.2, true)
                // HPS Bottom Width dimension
                addDimLine("${fd(params.hpsBottomWidth)}", hpsBot.first, hpsBot.second - 0.3, hpsBotInner.first, hpsBotInner.second - 0.3, -0.2, true)
                // HPS Height dimension
                addDimLine("${fd(hpsH)}", minOf(hpsTopL.first, hpsBot.first) - 0.5, hpsTopL.second, minOf(hpsTopL.first, hpsBot.first) - 0.5, hpsBot.second, -0.4, false)
            }
        }

        var dimPy = 0.0
        var dimRightEdge = if (ignoreOffset) 0.0 else bR.first
        allBlocks.forEach { (_, pc) ->
            val actualOffsetX = if (ignoreOffset) 0.0 else pc.offsetX
            val pTopW = pc.topWidth
            val pBotW = pc.bottomWidth
            val maxW = Math.max(pTopW, pBotW)
            val topRightEdge = if (ignoreOffset && dimPy == 0.0) pTopW else dimRightEdge + actualOffsetX
            val pX = topRightEdge - pTopW // base width
            val botRightEdge = pX + pBotW
            addDimLine("${fd(pc.bottomWidth)}", pX, dimPy - pc.height - 0.5, botRightEdge, dimPy - pc.height - 0.5, -0.3, true)
            addDimLine("${fd(pc.height)}", botRightEdge + 0.5, dimPy, botRightEdge + 0.5, dimPy - pc.height, 0.2, false)
            dimPy -= pc.height
            dimRightEdge = botRightEdge
        }

        // Zone labels with leader lines (Clean Labeling)
        fun addHorizontalLabel(text: String, tgtY: Double, textX: Double, endX: Double) {
            if (text == "HPS FILLING") {
                sb.append(lineEntity("LABELS", textX + 2.5, tgtY, endX, tgtY))
            }
            sb.append(textEntity("LABELS", text, textX, tgtY + 0.15, 0.25))
        }

        if (!ignoreWall) {
            val lx = Math.min(hpsTopL.first, bL.first) - 4.0
            
            if (params.isHpsEnabled) {
                addHorizontalLabel("HPS FILLING", h - hps / 3.0, lx, hpsTopL.first + params.hpsTopWidth / 2.0)
            }

            var yLabel = h
            for (layer in layers) {
                val layerH = layer.second
                val nextY = yLabel - layerH
                val midY = (yLabel + nextY) / 2.0
                if (layer.first == "RR Band") {
                    addHorizontalLabel("RR 1:6 BAND", midY, lx, tL.first + tw / 2.0)
                } else {
                    addHorizontalLabel("RR DRY", midY, lx, tL.first + tw / 2.0)
                }
                yLabel = nextY
            }
        }
        
        // FRONT ELEVATION
        val feStartX = bR.first + 7.0
        val l = params.length
        val feEndX = feStartX + l
        
        // Wall
        if (!ignoreWall) {
            sb.append(lineEntity("ELEVATION", feStartX, tR.second, feEndX, tR.second))
            sb.append(lineEntity("ELEVATION", feEndX, tR.second, feEndX, bR.second))
            sb.append(lineEntity("ELEVATION", feEndX, bR.second, feStartX, bR.second))
            sb.append(lineEntity("ELEVATION", feStartX, bR.second, feStartX, tR.second))

            // Bands
            var feLayerY = tR.second
            for (layer in layers) {
                val layerH = layer.second
                val botY = feLayerY - layerH
                if (botY > 0.0) {
                   sb.append(lineEntity("ELEVATION", feStartX, botY, feEndX, botY))
                }
                
                if (layer.first != "RR Band") {
                    val bandWM = if (params.topBandThickness > 0.0) params.topBandThickness else params.autoBandThickness
                    if (bandWM > 0.0 && (params.isAutoAlternateBands || params.topBandThickness > 0.0)) {
                        var numBands = Math.round(params.length / 2.5).toInt() + 1
                        var panelW = (params.length - numBands * bandWM) / (numBands - 1)
                        if (panelW < 0.0) {
                            numBands = Math.max(2, (params.length / bandWM).toInt())
                            panelW = 0.0
                        }
                        var currX = feStartX
                        for (i in 0 until numBands) {
                            sb.append(lineEntity("ELEVATION", currX, feLayerY, currX, botY))
                            sb.append(lineEntity("ELEVATION", currX + bandWM, feLayerY, currX + bandWM, botY))
                            currX += bandWM
                            if (i < numBands - 1 && panelW > 0) {
                                currX += panelW
                            }
                        }
                    }
                }
                
                // Panel dims on top layer
                if (feLayerY == tR.second) {
                   val topDimCheck = if (!params.isAutoAlternateBands && params.topBandThickness <= 0.0) false else true
                   if (topDimCheck) {
                        val bandWM = if (params.topBandThickness > 0.0) params.topBandThickness else params.autoBandThickness
                        if (bandWM > 0.0) {
                            var numBands = Math.round(params.length / 2.5).toInt() + 1
                            var panelW = (params.length - numBands * bandWM) / (numBands - 1)
                            if (panelW < 0.0) {
                                numBands = Math.max(2, (params.length / bandWM).toInt())
                                panelW = 0.0
                            }
                            var currX = feStartX
                            for (i in 0 until numBands) {
                                addDimLine("${fd(bandWM)}", currX, tR.second + 0.4, currX + bandWM, tR.second + 0.4, 0.15, true)
                                currX += bandWM
                                if (i < numBands - 1 && panelW > 0) {
                                    addDimLine("${fd(panelW)}", currX, tR.second + 0.4, currX + panelW, tR.second + 0.4, 0.15, true)
                                    currX += panelW
                                }
                            }
                        }
                   }
                }
                feLayerY = botY
            }

            var rDimTop = tR.second
            for (layer in layers) {
                val botY = rDimTop - layer.second
                addDimLine("${fd(layer.second)}", feEndX + 0.4, rDimTop, feEndX + 0.4, botY, 0.2, false)
                rDimTop = botY
            }
            // Side Dims
            addDimLine("${fd(params.height)}", feEndX + 1.0, tR.second, feEndX + 1.0, bR.second, 0.2, false)
        }

        val topElvY = if (!ignoreWall) tR.second else 0.0
        sb.append(textEntity("LABELS", "FRONT ELEVATION", feStartX + l/2 - 1.5, topElvY + 2.2, 0.35))
        addDimLine("${fd(l)}", feStartX, topElvY + 0.8, feEndX, topElvY + 0.8, 0.2, true)
        
        var fePlumY = if (!ignoreWall) bR.second else 0.0
        allBlocks.forEach { (type, pc) ->
            val blockL = if (type == "TOE") params.toeLength else l
            val blockStartX = if (type == "TOE") feStartX + (l - blockL) / 2.0 else feStartX
            val blockEndX = blockStartX + blockL

            sb.append(lineEntity("ELEVATION", blockStartX, fePlumY, blockStartX, fePlumY - pc.height))
            sb.append(lineEntity("ELEVATION", blockEndX, fePlumY, blockEndX, fePlumY - pc.height))
            if (ignoreWall || pc != allBlocks.first().second) {
                sb.append(lineEntity("ELEVATION", blockStartX, fePlumY, blockEndX, fePlumY))
            }
            sb.append(lineEntity("ELEVATION", blockStartX, fePlumY - pc.height, blockEndX, fePlumY - pc.height))
            
            // Vertical separation lines every 2.0m
            var sepX = blockStartX + 2.0
            while (sepX < blockEndX - 0.1) {
                sb.append(lineEntity("ELEVATION", sepX, fePlumY, sepX, fePlumY - pc.height))
                sepX += 2.0
            }
            
            fePlumY -= pc.height
        }
        
        if (params.isWeepHoleWallEnabled || params.isWeepHoleWireCrateEnabled || params.isWeepHolePlumEnabled || params.isWeepHoleToeWallEnabled) {
            val totalStructureHeight = topElvY - fePlumY
            if (totalStructureHeight > 1.0 && params.length > 0.0) {
                val vSpace = 1.0
                val hSpace = 1.0
                val radius = 0.05
                var yObj = fePlumY + vSpace / 2.0
                while (yObj < topElvY - radius) {
                    var isWeepHereAllowed = false
                    var blockStartX = feStartX
                    var blockEndX = feEndX
                    if (yObj >= 0.0 && params.isWallEnabled) {
                        isWeepHereAllowed = params.isWeepHoleWallEnabled
                    } else {
                        var blockTop = 0.0
                        for (i in allBlocks.indices) {
                            val type = allBlocks[i].first
                            val pc = allBlocks[i].second
                            if (yObj <= blockTop && yObj >= blockTop - pc.height) {
                                if (type == "TOE" && params.isWeepHoleToeWallEnabled) {
                                    isWeepHereAllowed = true
                                    blockStartX = feStartX + (l - params.toeLength) / 2.0
                                    blockEndX = blockStartX + params.toeLength
                                }
                                if (type == "WIRECATE" && params.isWeepHoleWireCrateEnabled) isWeepHereAllowed = true
                                if (type == "PLUM" && params.isWeepHolePlumEnabled) isWeepHereAllowed = true
                                break
                            }
                            blockTop -= pc.height
                        }
                    }

                    if (isWeepHereAllowed) {
                        var xObj = blockStartX + hSpace / 2.0
                        while (xObj < blockEndX - radius) {
                            // DXF Circles
                            val circle = "  0\nCIRCLE\n  8\nELEVATION\n 10\n${xObj}\n 20\n${yObj}\n 30\n0.0\n 40\n${radius}\n"
                            sb.append(circle)
                            xObj += hSpace
                        }
                    }
                    yObj += vSpace
                }
            }
        }
        
        // A4 Bounding Box (297x210 aspect ratio landscape or 210x297 portrait)
        // Let's create a scalable box that encloses the whole drawing
        val lx = if (!ignoreWall) Math.min(hpsTopL.first, bL.first) - 4.0 else bL.first - 4.0
        val minX = lx - 0.5
        val maxX = feEndX + 2.5
        val minY = Math.min(pY, fePlumY) - 2.5
        val maxY = topElvY + 2.5
        
        val widthReq = Math.max(maxX - minX, 16.0)
        val heightReq = maxY - minY
        
        val aspectA4 = 297.0 / 210.0 // Landscape
        
        var a4W = widthReq
        var a4H = a4W / aspectA4
        
        if (a4H < heightReq) {
            a4H = heightReq
            a4W = a4H * aspectA4
        }
        
        // Center the A4 box around the drawing
        val cx = (minX + maxX) / 2.0
        val cy = (minY + maxY) / 2.0
        val a4MinX = cx - a4W / 2.0
        val a4MaxX = cx + a4W / 2.0
        val a4MinY = cy - a4H / 2.0
        val a4MaxY = cy + a4H / 2.0
        
        // Draw A4 Border
        sb.append(lineEntity("TITLE_BLOCK", a4MinX, a4MinY, a4MaxX, a4MinY))
        sb.append(lineEntity("TITLE_BLOCK", a4MaxX, a4MinY, a4MaxX, a4MaxY))
        sb.append(lineEntity("TITLE_BLOCK", a4MaxX, a4MaxY, a4MinX, a4MaxY))
        sb.append(lineEntity("TITLE_BLOCK", a4MinX, a4MaxY, a4MinX, a4MinY))

        // Title block resting on A4 bottom right
        val tbW = Math.max(15.0, l)
        val tbH = 1.5
        val tbStartX = a4MaxX - tbW
        val tbStartY = a4MinY + tbH
        sb.append(lineEntity("TITLE_BLOCK", tbStartX, a4MinY, tbStartX, tbStartY))
        sb.append(lineEntity("TITLE_BLOCK", tbStartX, tbStartY, a4MaxX, tbStartY))
        sb.append(textEntity("TITLE_BLOCK", "${params.projectName} - CH: ${params.chainage}", tbStartX + 0.5, a4MinY + 0.8, 0.30))
        sb.append(textEntity("TITLE_BLOCK", "Drawing No: RW-001 | Scale: N.T.S | A4", tbStartX + 0.5, a4MinY + 0.3, 0.20))

        sb.append("  0\nENDSEC\n  0\nEOF\n")
        return sb.toString()
    }

    private fun layerDef(name: String, color: Int): String {
        return "  0\nLAYER\n  2\n$name\n 70\n0\n 62\n$color\n  6\nCONTINUOUS\n"
    }

    private fun lineEntity(layer: String, x1: Double, y1: Double, x2: Double, y2: Double): String {
        return "  0\nLINE\n  8\n$layer\n 10\n${fd(x1)}\n 20\n${fd(y1)}\n 30\n0.00000\n 11\n${fd(x2)}\n 21\n${fd(y2)}\n 31\n0.00000\n"
    }

    private fun arrowEntity(layer: String, tipX: Double, tipY: Double, dirX: Double, dirY: Double): String {
        val len = Math.hypot(dirX, dirY)
        if (len == 0.0) return ""
        val dx = dirX / len
        val dy = dirY / len
        val nx = -dy
        val ny = dx
        val arrowSize = 0.2
        val p1x = tipX - dx * arrowSize + nx * arrowSize * 0.4
        val p1y = tipY - dy * arrowSize + ny * arrowSize * 0.4
        val p2x = tipX - dx * arrowSize - nx * arrowSize * 0.4
        val p2y = tipY - dy * arrowSize - ny * arrowSize * 0.4
        return lineEntity(layer, tipX, tipY, p1x, p1y) + lineEntity(layer, tipX, tipY, p2x, p2y) + lineEntity(layer, p1x, p1y, p2x, p2y)
    }

    private fun textEntity(layer: String, text: String, x: Double, y: Double, height: Double): String {
        return "  0\nTEXT\n  8\n$layer\n 10\n${fd(x)}\n 20\n${fd(y)}\n 30\n0.00000\n 40\n${fd(height)}\n  1\n$text\n"
    }

    private fun fd(d: Double): String = String.format(Locale.US, "%.5f", d)
}
