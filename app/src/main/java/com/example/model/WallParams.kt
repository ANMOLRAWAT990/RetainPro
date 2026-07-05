package com.example.model
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Rates(
    val earthwork: Double = 350.0,
    val plumConcrete: Double = 4500.0,
    val wireCrate: Double = 2500.0,
    val rrDry: Double = 1800.0,
    val rrBand: Double = 3200.0,
    val hps: Double = 1200.0,
    val pvcPipe: Double = 150.0 // added for weep holes if needed
)

@JsonClass(generateAdapter = true)
data class WallParams(
    val projectName: String = "Retaining Wall",
    val chainage: String = "0+000",
    val isWallEnabled: Boolean = true,
    val isToeWallEnabled: Boolean = false,
    val toeTopWidth: Double = 0.5,
    val toeBottomWidth: Double = 1.0,
    val toeHeight: Double = 1.0,
    val toeLength: Double = 5.0,
    val toeOffsetX: Double = 0.0,
    val height: Double = 3.00,
    val topWidth: Double = 0.60,
    val length: Double = 5.00,
    val slopeRatio: Double = 3.09,
    val backSlopeRatio: Double = 0.0,
    val isHpsEnabled: Boolean = true,
    val hpsWidth: Double = 0.60,
    val hpsTopWidth: Double = 0.60,
    val hpsBottomWidth: Double = 0.60,
    val isWeepHoleWallEnabled: Boolean = false,
    val isWeepHoleToeWallEnabled: Boolean = false,
    val isWeepHoleWireCrateEnabled: Boolean = false,
    val isWeepHolePlumEnabled: Boolean = false,
    val topBandThickness: Double = 0.6,
    val midBandThickness: Double = 0.6,
    val bottomBandThickness: Double = 0.6,
    val isAutoAlternateBands: Boolean = false,
    val alternateSequence: String = "1", // 0: RR Band+RR Dry, 1: RR 1:6+RR Dry, 2: RR Band+RR 1:6+RR Dry
    val autoBandThickness: Double = 0.6,
    val autoRr16Thickness: Double = 0.6,
    val autoDryThickness: Double = 1.0,
    val isRrBandEnabled: Boolean = true,
    val isRr16Enabled: Boolean = true,
    val isRrDryEnabled: Boolean = true,
    val showRrBandLabel: Boolean = true,
    val showRr16Label: Boolean = true,
    val showRrDryLabel: Boolean = true,
    val rrBandPatternType: String = "Concrete", 
    val rr16PatternType: String = "Stone",
    val rrDryPatternType: String = "Stone", // "Stone", "Concrete", "Plum", "Solid"
    val plumPatternType: String = "Plum",
    val toePatternType: String = "Plum",
    val soilUnitWeight: Double = 18.0,
    val surcharge: Double = 10.0,
    val frictionAngle: Double = 30.0,
    val materialUnitWeight: Double = 22.0,
    val materialName: String = "Rubble Masonry",
    val plumConcretes: List<PlumConcrete> = emptyList(),
    val wireCrates: List<PlumConcrete> = emptyList(),
    val rates: Rates = Rates(),
    val photoUris: List<String> = emptyList()
) {
    val bottomWidth: Double
        get() = topWidth + (height / slopeRatio) - if (backSlopeRatio > 0) (height / backSlopeRatio) else 0.0
}

@JsonClass(generateAdapter = true)
data class PlumConcrete(
    val topWidth: Double = 1.50,
    val bottomWidth: Double = 1.50,
    val height: Double = 1.50,
    val offsetX: Double = 0.0
)

fun generateWallLayers(params: WallParams): List<Pair<String, Double>> {
    val layers = mutableListOf<Pair<String, Double>>()
    if (!params.isAutoAlternateBands) {
        val top = params.topBandThickness
        val mid = params.midBandThickness
        val bot = params.bottomBandThickness
        
        var remaining = params.height
        if (top > 0.0) {
            layers.add("RR Band" to top)
            remaining -= top
        }
        val drySpace = remaining - mid - bot
        if (mid > 0.0) {
            if (drySpace > 0.0) {
                layers.add("RR Dry" to (drySpace / 2.0))
                remaining -= (drySpace / 2.0)
            }
            layers.add("RR Band" to mid)
            remaining -= mid
        }
        if (bot > 0.0) {
            if (remaining - bot > 0.0) {
                layers.add("RR Dry" to (remaining - bot))
                remaining -= (remaining - bot)
            }
            layers.add("RR Band" to bot)
            remaining -= bot
        }
        if (remaining > 0.0) {
            layers.add("RR Dry" to remaining)
        }
        return layers
    }

    var remaining = params.height
    
    val seq = when (params.alternateSequence) {
        "0" -> listOf("RR Band" to params.autoBandThickness, "RR Dry" to params.autoDryThickness)
        "1" -> listOf("RR 1:6" to params.autoRr16Thickness, "RR Dry" to params.autoDryThickness)
        "2" -> listOf("RR Band" to params.autoBandThickness, "RR 1:6" to params.autoRr16Thickness, "RR Dry" to params.autoDryThickness)
        else -> listOf("RR Band" to params.autoBandThickness, "RR Dry" to params.autoDryThickness)
    }
    
    if (seq.all { it.second <= 0.0 }) return layers

    var i = 0
    while (remaining > 0.0) {
        val (layerType, thick) = seq[i % seq.size]
        i++
        if (thick <= 0.0) continue
        val actual = minOf(remaining, thick)
        layers.add(layerType to actual)
        remaining -= actual
    }
    
    return layers
}

data class AnalysisResult(
    val ka: Double,
    val pa: Double,
    val wallWeight: Double,
    val fosSliding: Double,
    val fosOverturning: Double,
    val qToe: Double,
    val qHeel: Double,
    val eccentricity: Double,
    val isSlidingSafe: Boolean,
    val isOverturningSafe: Boolean,
    val isBasePressureSafe: Boolean,
    val isEccentricitySafe: Boolean,
    val overallSafe: Boolean
)
