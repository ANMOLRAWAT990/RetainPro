package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.WallParams
import com.example.ui.components.WallCanvas
import com.example.export.DXFGenerator
import com.example.export.PDFExporter
import com.example.export.EstimateGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(params: WallParams) {
    var isBlueprintMode by remember { mutableStateOf(false) }
    var previewMode by remember { mutableStateOf("BOTH") }
    var paperSize by remember { mutableStateOf("A4") }
    var showShareMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            WallCanvas(params = params, isBlueprintMode = isBlueprintMode, previewMode = previewMode)
            
            FloatingActionButton(
                onClick = { showShareMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
        
        Surface(shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = previewMode == "BOTH",
                        onClick = { previewMode = "BOTH" },
                        label = { Text("Both (CS+FE)") }
                    )
                    FilterChip(
                        selected = previewMode == "CS_ONLY",
                        onClick = { previewMode = "CS_ONLY" },
                        label = { Text("Cross Section Only") }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { isBlueprintMode = !isBlueprintMode }) {
                    Text(if (isBlueprintMode) "🌙 Switch to Normal Mode" else "🌙 Switch to Blueprint Mode")
                }
            }
        }
    }

    if (showShareMenu) {
        ModalBottomSheet(onDismissRequest = { showShareMenu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Share Document", style = MaterialTheme.typography.titleLarge)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Paper Size:")
                    Spacer(modifier = Modifier.width(16.dp))
                    FilterChip(
                        selected = paperSize == "A4",
                        onClick = { paperSize = "A4" },
                        label = { Text("A4") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = paperSize == "A3",
                        onClick = { paperSize = "A3" },
                        label = { Text("A3") }
                    )
                }
                
                Button(
                    onClick = {
                        showShareMenu = false
                        PDFExporter.sharePDF(context, params, previewMode, paperSize)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share as PDF")
                }
                
                Button(
                    onClick = {
                        showShareMenu = false
                        DXFGenerator.shareDXF(context, params)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share as DXF (AutoCAD)")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
