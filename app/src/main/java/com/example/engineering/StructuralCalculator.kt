package com.example.engineering

import com.example.model.AnalysisResult
import com.example.model.WallParams
import kotlin.math.sin
import kotlin.math.PI

object StructuralCalculator {
    fun calculate(p: WallParams): AnalysisResult {
        // Active Earth Pressure Coefficient
        val phiRad = p.frictionAngle * PI / 180.0
        val ka = (1.0 - sin(phiRad)) / (1.0 + sin(phiRad))

        // Total active earth pressure (Pa) = 0.5 * Ka * gamma_soil * H^2
        val pa = 0.5 * ka * p.soilUnitWeight * p.height * p.height
        
        // Surcharge pressure to be conservative, wait, instructions say:
        // Pa = 0.5 * Ka * gamma_soil * H^2 for Card 1
        // (Wait, do we add surcharge? The card just says Pa = 0.5 * Ka * gamma_soil * H^2. I will follow instructions exactly to avoid deviations).

        // Wall Self Weight (W)
        val frontSlopeW = p.height / p.slopeRatio
        val backSlopeW = if (p.backSlopeRatio > 0.0) p.height / p.backSlopeRatio else 0.0
        
        val backTriArea = 0.5 * backSlopeW * p.height
        val rectArea = p.topWidth * p.height
        val frontTriArea = 0.5 * frontSlopeW * p.height
        
        val area = backTriArea + rectArea + frontTriArea
        val w = area * p.materialUnitWeight

        // Sliding Factor of Safety
        val mu = 0.5
        val fosSliding = (mu * w) / pa

        // Overturning Factor of Safety
        val overturningMoment = pa * (p.height / 3.0)
        
        // Stabilizing moment requires centroid of the wall from the toe
        // Let Heel = 0, Toe = bottomWidth
        val xBackTriCentroid = (2.0 / 3.0) * backSlopeW
        val xRectCentroid = backSlopeW + (p.topWidth / 2.0)
        val xFrontTriCentroid = backSlopeW + p.topWidth + (1.0 / 3.0) * frontSlopeW
        
        val xCentroidFromHeel = (backTriArea * xBackTriCentroid + rectArea * xRectCentroid + frontTriArea * xFrontTriCentroid) / area
        val xCentroidFromToe = p.bottomWidth - xCentroidFromHeel
        
        val stabilizingMoment = w * xCentroidFromToe
        val fosOverturning = stabilizingMoment / overturningMoment

        // Base Pressure
        val e = (p.bottomWidth / 2.0) - ((stabilizingMoment - overturningMoment) / w)
        val qToe = (w / p.bottomWidth) * (1.0 + 6.0 * e / p.bottomWidth)
        val qHeel = (w / p.bottomWidth) * (1.0 - 6.0 * e / p.bottomWidth)

        val isSlidingSafe = fosSliding >= 1.5
        val isOverturningSafe = fosOverturning >= 2.0
        val isBasePressureSafe = qToe < 150.0 // AND qHeel > 0
        val isEccentricitySafe = Math.abs(e) < (p.bottomWidth / 6.0)

        val overallSafe = isSlidingSafe && isOverturningSafe && isBasePressureSafe && isEccentricitySafe

        return AnalysisResult(
            ka = ka,
            pa = pa,
            wallWeight = w,
            fosSliding = fosSliding,
            fosOverturning = fosOverturning,
            qToe = qToe,
            qHeel = qHeel,
            eccentricity = e,
            isSlidingSafe = isSlidingSafe,
            isOverturningSafe = isOverturningSafe,
            isBasePressureSafe = isBasePressureSafe,
            isEccentricitySafe = isEccentricitySafe,
            overallSafe = overallSafe
        )
    }
}
