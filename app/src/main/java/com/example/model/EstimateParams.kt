package com.example.model
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class EstimateParams(
    val nameOfWork: String = "",
    val departmentName: String = "C.D.P.W.D.",
    val preparedBy: String = "",
    val drawingNo: String = "RW-001",
    val date: String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()),
    
    // Rates
    val rateExcavation: Double = 419.70,
    val ratePlumMasonry: Double = 3931.00,
    val rateWeepHoles: Double = 181.60,
    val rateRRMasonry16: Double = 4000.30,
    val rateRRDryMasonry: Double = 2317.60,
    val rateHPSFilling: Double = 1063.30,
    
    // SOR references
    val sorExcavation: String = "Misc '1-2",
    val sorPlumMasonry: String = "Misc 2--1",
    val sorWeepHoles: String = "Mord 12-9",
    val sorRRMasonry16: String = "Misc '1-4",
    val sorRRDryMasonry: String = "Misc '1-3",
    val sorHPSFilling: String = "Misc '1-5",
    
    // Abstract
    val gstPercent: Double = 18.0,
    val contingencyPercent: Double = 0.0,
    val labourCessPercent: Double = 0.0,
    
    // Custom items
    val customItems: List<CustomEstimateItem> = emptyList(),
    
    // Chainage segments
    val chainageSegments: List<ChainageSegment> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CustomEstimateItem(
    val sno: Int,
    val description: String,
    val unit: String,
    val qty: Double,
    val rate: Double,
    val sorRef: String
)

@JsonClass(generateAdapter = true)
data class ChainageSegment(
    val label: String,        // e.g. "Km 1"
    val crossSections: List<CrossSection> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CrossSection(
    val label: String,        // e.g. "X-S 0/28"
    val length: Double,       // length of this segment
    val height: Double,
    val topWidth: Double,
    val bottomWidth: Double
)
