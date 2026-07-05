package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.WallDesignEntity
import com.example.model.*
import com.example.export.WorkbookGenerator
import com.example.ui.components.NumericField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstimateScreen(
    savedLogs: List<WallDesignEntity>,
    currentParams: WallParams,
    onSaveRates: (Rates) -> Unit // Keeping this for backward compatibility if needed
) {
    val context = LocalContext.current
    var estimateParams by remember { mutableStateOf(EstimateParams(
        nameOfWork = currentParams.projectName,
        chainageSegments = listOf(ChainageSegment("Km 1", listOf(CrossSection(currentParams.chainage, currentParams.length, currentParams.height, currentParams.topWidth, currentParams.bottomWidth))))
    )) }
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Estimate Generator", style = MaterialTheme.typography.headlineSmall)
        
        // SECTION A - Project Info
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("SECTION A — Project Info", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = estimateParams.nameOfWork, onValueChange = { estimateParams = estimateParams.copy(nameOfWork = it) }, label = { Text("Name of Work") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estimateParams.drawingNo, onValueChange = { estimateParams = estimateParams.copy(drawingNo = it) }, label = { Text("Drawing No") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estimateParams.preparedBy, onValueChange = { estimateParams = estimateParams.copy(preparedBy = it) }, label = { Text("Prepared By") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = estimateParams.date, onValueChange = { estimateParams = estimateParams.copy(date = it) }, label = { Text("Date") }, modifier = Modifier.fillMaxWidth())
            }
        }

        // SECTION B & C - Serial Number & Rates
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SECTIONS B & C — Rates & SOR", style = MaterialTheme.typography.titleMedium)
                
                ItemRateSORForm("Item 1 (Excavation)", estimateParams.rateExcavation, estimateParams.sorExcavation, {r,s -> estimateParams = estimateParams.copy(rateExcavation = r, sorExcavation = s)})
                ItemRateSORForm("Item 2 (Plum Masonry)", estimateParams.ratePlumMasonry, estimateParams.sorPlumMasonry, {r,s -> estimateParams = estimateParams.copy(ratePlumMasonry = r, sorPlumMasonry = s)})
                ItemRateSORForm("Item 3 (Weep Holes)", estimateParams.rateWeepHoles, estimateParams.sorWeepHoles, {r,s -> estimateParams = estimateParams.copy(rateWeepHoles = r, sorWeepHoles = s)})
                ItemRateSORForm("Item 4 (RR 1:6)", estimateParams.rateRRMasonry16, estimateParams.sorRRMasonry16, {r,s -> estimateParams = estimateParams.copy(rateRRMasonry16 = r, sorRRMasonry16 = s)})
                ItemRateSORForm("Item 5 (RR Dry)", estimateParams.rateRRDryMasonry, estimateParams.sorRRDryMasonry, {r,s -> estimateParams = estimateParams.copy(rateRRDryMasonry = r, sorRRDryMasonry = s)})
                ItemRateSORForm("Item 6 (HPS Fill)", estimateParams.rateHPSFilling, estimateParams.sorHPSFilling, {r,s -> estimateParams = estimateParams.copy(rateHPSFilling = r, sorHPSFilling = s)})
                
                // Custom Items... ignored for brevity but can add a button.
            }
        }

        // SECTION D - Abstract Percentages
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SECTION D — Abstract Percentages", style = MaterialTheme.typography.titleMedium)
                NumericField("GST %", estimateParams.gstPercent, onChange = { estimateParams = estimateParams.copy(gstPercent = it) })
                NumericField("Contingency %", estimateParams.contingencyPercent, onChange = { estimateParams = estimateParams.copy(contingencyPercent = it) })
                NumericField("Labour Cess %", estimateParams.labourCessPercent, onChange = { estimateParams = estimateParams.copy(labourCessPercent = it) })
            }
        }

        // SECTION E - Chainage Segments
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("SECTION E — Chainage Segments", style = MaterialTheme.typography.titleMedium)
                estimateParams.chainageSegments.forEachIndexed { i, seg ->
                    OutlinedCard {
                        Column(modifier = Modifier.padding(8.dp)) {
                            OutlinedTextField(value = seg.label, onValueChange = {
                                val mut = estimateParams.chainageSegments.toMutableList()
                                mut[i] = seg.copy(label = it)
                                estimateParams = estimateParams.copy(chainageSegments = mut)
                            }, label = { Text("Segment Label") })
                            
                            seg.crossSections.forEachIndexed { j, cs ->
                                NumericField("Length (m)", cs.length, onChange = {
                                    val newCs = seg.crossSections.toMutableList().apply { set(j, cs.copy(length = it)) }
                                    val mut = estimateParams.chainageSegments.toMutableList()
                                    mut[i] = seg.copy(crossSections = newCs)
                                    estimateParams = estimateParams.copy(chainageSegments = mut)
                                })
                            }
                        }
                    }
                }
                TextButton(onClick = {
                     val mut = estimateParams.chainageSegments.toMutableList()
                     mut.add(ChainageSegment("Km ${mut.size + 1}", listOf(CrossSection("X-S ${mut.size+1}", currentParams.length, currentParams.height, currentParams.topWidth, currentParams.bottomWidth))))
                     estimateParams = estimateParams.copy(chainageSegments = mut)
                }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Segment")
                }
            }
        }

        Button(
            onClick = {
                WorkbookGenerator.exportWorkbook(context, currentParams, estimateParams)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Export")
            Spacer(Modifier.width(8.dp))
            Text("GENERATE ESTIMATE WORKBOOK")
        }
    }
}

@Composable
fun ItemRateSORForm(title: String, rate: Double, sor: String, onChange: (Double, String) -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericField("Rate", rate, onChange = { onChange(it, sor) }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = sor, onValueChange = { onChange(rate, it) }, label = { Text("SOR") }, modifier = Modifier.weight(1f))
        }
    }
}

