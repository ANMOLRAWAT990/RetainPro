package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.AnalysisResult
import com.example.ui.theme.Success
import com.example.ui.theme.Error
import java.util.Locale

@Composable
fun AnalysisScreen(result: AnalysisResult?) {
    if (result == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("No results to display. Please analyze first.")
        }
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (result.overallSafe) {
            Card(colors = CardDefaults.cardColors(containerColor = Success)) {
                Text("✅ DESIGN IS SAFE", modifier = Modifier.padding(16.dp).fillMaxWidth(), color = Color.White)
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = Error)) {
                Text("⚠️ REDESIGN REQUIRED", modifier = Modifier.padding(16.dp).fillMaxWidth(), color = Color.White)
            }
        }

        ResultCard("Earth Pressure", "Ka = ${String.format(Locale.getDefault(), "%.3f", result.ka)}\nPa = ${String.format(Locale.getDefault(), "%.1f", result.pa)} kN/m", null)
        ResultCard("Wall Self Weight", "W = ${String.format(Locale.getDefault(), "%.1f", result.wallWeight)} kN/m", null)
        ResultCard("Factor of Safety: Sliding", "FOS = ${String.format(Locale.getDefault(), "%.2f", result.fosSliding)}\nRequired: ≥ 1.5", result.isSlidingSafe)
        ResultCard("Factor of Safety: Overturning", "FOS = ${String.format(Locale.getDefault(), "%.2f", result.fosOverturning)}\nRequired: ≥ 2.0", result.isOverturningSafe)
        ResultCard("Base Pressure", "q_toe = ${String.format(Locale.getDefault(), "%.1f", result.qToe)} kN/m²\nq_heel = ${String.format(Locale.getDefault(), "%.1f", result.qHeel)} kN/m²\nRequired: q_toe < 150", result.isBasePressureSafe)
        ResultCard("Resultant Eccentricity", "e = ${String.format(Locale.getDefault(), "%.3f", result.eccentricity)} m\nRequired: Middle Third Rule", result.isEccentricitySafe)

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun ResultCard(title: String, content: String, isSafe: Boolean?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }
            if (isSafe != null) {
                AssistChip(
                    onClick = { },
                    label = { Text(if (isSafe) "PASS" else "FAIL", color = Color.White) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = if (isSafe) Success else Error)
                )
            }
        }
    }
}
