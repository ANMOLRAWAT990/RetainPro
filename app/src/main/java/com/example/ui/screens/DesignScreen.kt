package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import java.io.File

import com.example.model.PlumConcrete
import com.example.model.WallParams

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.clip

import com.example.ui.components.NumericField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    params: WallParams,
    onParamChange: (WallParams) -> Unit,
    onAnalyze: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var capturingUri by remember { mutableStateOf<Uri?>(null) }
    
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && capturingUri != null) {
            val newList = params.photoUris.toMutableList()
            newList.add(capturingUri.toString())
            onParamChange(params.copy(photoUris = newList))
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val newList = params.photoUris.toMutableList()
            newList.addAll(uris.map { it.toString() })
            onParamChange(params.copy(photoUris = newList))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Project Details", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onReset, modifier = Modifier.testTag("reset_button")) {
                    Text("Reset")
                }
                Button(onClick = onSave) {
                    Text("Save Log")
                }
            }
        }
        
        Text("Project Attributes", style = MaterialTheme.typography.titleLarge)
        
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = params.projectName,
                    onValueChange = { onParamChange(params.copy(projectName = it)) },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = params.chainage,
                    onValueChange = { onParamChange(params.copy(chainage = it)) },
                    label = { Text("Chainage") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Toe Wall Geometry", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enable Toe", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = params.isToeWallEnabled, onCheckedChange = { onParamChange(params.copy(isToeWallEnabled = it)) }, modifier = Modifier.scale(0.8f))
            }
        }
        
        if (params.isToeWallEnabled) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericField(label = "Toe Height (m)", value = params.toeHeight, onChange = { onParamChange(params.copy(toeHeight = it)) }, modifier = Modifier.weight(1f))
                        NumericField(label = "Toe Length (m)", value = params.toeLength, onChange = { onParamChange(params.copy(toeLength = it)) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericField(label = "Toe Top Width (m)", value = params.toeTopWidth, onChange = { onParamChange(params.copy(toeTopWidth = it)) }, modifier = Modifier.weight(1f))
                        NumericField(label = "Toe Bot Width (m)", value = params.toeBottomWidth, onChange = { onParamChange(params.copy(toeBottomWidth = it)) }, modifier = Modifier.weight(1f))
                        NumericField(label = "Offset X (m)", value = params.toeOffsetX, onChange = { onParamChange(params.copy(toeOffsetX = it)) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Wall Geometry", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enable Wall", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = params.isWallEnabled, onCheckedChange = { onParamChange(params.copy(isWallEnabled = it)) }, modifier = Modifier.scale(0.8f))
            }
        }
        
        if (params.isWallEnabled) {
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericField(label = "Wall Height (m)", value = params.height, onChange = { onParamChange(params.copy(height = it)) })
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericField(label = "Top Width (m)", value = params.topWidth, onChange = { onParamChange(params.copy(topWidth = it)) }, modifier = Modifier.weight(1f))
                        NumericField(label = "Length (m)", value = params.length, onChange = { onParamChange(params.copy(length = it)) }, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumericField(label = "Front Slope (1:X)", value = params.slopeRatio, onChange = { onParamChange(params.copy(slopeRatio = it)) }, modifier = Modifier.weight(1f))
                        NumericField(label = "Back Slope (1:X) [0=vertical]", value = params.backSlopeRatio, onChange = { onParamChange(params.copy(backSlopeRatio = it)) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Zone Parameters", style = MaterialTheme.typography.titleLarge)
            }
            Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Weep Holes Enabled In", style = MaterialTheme.typography.titleMedium)
                    @OptIn(ExperimentalMaterial3Api::class)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = params.isWeepHoleWallEnabled,
                            onClick = { onParamChange(params.copy(isWeepHoleWallEnabled = !params.isWeepHoleWallEnabled)) },
                            label = { Text("Wall") }
                        )
                        FilterChip(
                            selected = params.isWeepHoleToeWallEnabled,
                            onClick = { onParamChange(params.copy(isWeepHoleToeWallEnabled = !params.isWeepHoleToeWallEnabled)) },
                            label = { Text("Toe") }
                        )
                        FilterChip(
                            selected = params.isWeepHoleWireCrateEnabled,
                            onClick = { onParamChange(params.copy(isWeepHoleWireCrateEnabled = !params.isWeepHoleWireCrateEnabled)) },
                            label = { Text("WireCrate") }
                        )
                        FilterChip(
                            selected = params.isWeepHolePlumEnabled,
                            onClick = { onParamChange(params.copy(isWeepHolePlumEnabled = !params.isWeepHolePlumEnabled)) },
                            label = { Text("Plum") }
                        )
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("HPS Filling")
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enable HPS", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = params.isHpsEnabled, onCheckedChange = { onParamChange(params.copy(isHpsEnabled = it)) }, modifier = Modifier.scale(0.8f))
                        }
                    }
                    if (params.isHpsEnabled) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumericField(
                                label = "HPS Top Width (m)",
                                value = params.hpsTopWidth,
                                onChange = { onParamChange(params.copy(hpsTopWidth = it, hpsWidth = maxOf(it, params.hpsBottomWidth))) },
                                modifier = Modifier.weight(1f)
                            )
                            NumericField(
                                label = "HPS Bottom Width (m)",
                                value = params.hpsBottomWidth,
                                onChange = { onParamChange(params.copy(hpsBottomWidth = it, hpsWidth = maxOf(params.hpsTopWidth, it))) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                    Text("Wall Output Components", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Draw RR Band")
                        Switch(checked = params.isRrBandEnabled, onCheckedChange = { onParamChange(params.copy(isRrBandEnabled = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Draw RR 1:6")
                        Switch(checked = params.isRr16Enabled, onCheckedChange = { onParamChange(params.copy(isRr16Enabled = it)) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Draw RR Dry")
                        Switch(checked = params.isRrDryEnabled, onCheckedChange = { onParamChange(params.copy(isRrDryEnabled = it)) })
                    }
                    HorizontalDivider()
                    Text("Labels and Patterns", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show RR Band Label")
                        Switch(
                            checked = params.showRrBandLabel,
                            onCheckedChange = { onParamChange(params.copy(showRrBandLabel = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show RR 1:6 Label")
                        Switch(
                            checked = params.showRr16Label,
                            onCheckedChange = { onParamChange(params.copy(showRr16Label = it)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show RR Dry Label")
                        Switch(
                            checked = params.showRrDryLabel,
                            onCheckedChange = { onParamChange(params.copy(showRrDryLabel = it)) }
                        )
                    }
                    PatternSelector("RR Band Pattern", params.rrBandPatternType) { onParamChange(params.copy(rrBandPatternType = it)) }
                    PatternSelector("RR 1:6 Pattern", params.rr16PatternType) { onParamChange(params.copy(rr16PatternType = it)) }
                    PatternSelector("RR Dry Pattern", params.rrDryPatternType) { onParamChange(params.copy(rrDryPatternType = it)) }
                    PatternSelector("Plum Pattern", params.plumPatternType) { onParamChange(params.copy(plumPatternType = it)) }
                    PatternSelector("Toe Pattern", params.toePatternType) { onParamChange(params.copy(toePatternType = it)) }
                    
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Alternate Layers")
                        Switch(
                            checked = params.isAutoAlternateBands,
                            onCheckedChange = { onParamChange(params.copy(isAutoAlternateBands = it)) }
                        )
                    }
                    if (params.isAutoAlternateBands) {
                        Text("Alternating Configuration", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Alternate Scheme")
                            val options = listOf("RR Band + RR Dry", "RR 1:6 + RR Dry", "RR Band + RR 1:6 + RR Dry")
                            androidx.compose.material3.TextButton(onClick = { 
                                val next = ((params.alternateSequence.toIntOrNull() ?: 1) + 1) % 3
                                onParamChange(params.copy(alternateSequence = next.toString())) 
                            }) {
                                Text(options.getOrNull(params.alternateSequence.toIntOrNull() ?: 1) ?: "RR 1:6 + RR Dry")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumericField(label = "RR Band Thick (m)", value = params.autoBandThickness, onChange = { onParamChange(params.copy(autoBandThickness = it)) }, modifier = Modifier.weight(1f))
                            NumericField(label = "RR 1:6 Thick (m)", value = params.autoRr16Thickness, onChange = { onParamChange(params.copy(autoRr16Thickness = it)) }, modifier = Modifier.weight(1f))
                            NumericField(label = "RR Dry Thick (m)", value = params.autoDryThickness, onChange = { onParamChange(params.copy(autoDryThickness = it)) }, modifier = Modifier.weight(1f))
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumericField(label = "Top Band (m)", value = params.topBandThickness, onChange = { onParamChange(params.copy(topBandThickness = it)) }, modifier = Modifier.weight(1f))
                            NumericField(label = "Mid Band (m)", value = params.midBandThickness, onChange = { onParamChange(params.copy(midBandThickness = it)) }, modifier = Modifier.weight(1f))
                            NumericField(label = "Bottom Band (m)", value = params.bottomBandThickness, onChange = { onParamChange(params.copy(bottomBandThickness = it)) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
             // Let length be customized still
             Card {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumericField(label = "Length (m)", value = params.length, onChange = { onParamChange(params.copy(length = it)) })
                }
             }
        }

        Text("Wire Crates", style = MaterialTheme.typography.titleLarge)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val newSteps = params.wireCrates.toMutableList()
                    newSteps.add(PlumConcrete(1.0, 1.0, 1.0, 0.25))
                    onParamChange(params.copy(wireCrates = newSteps))
                }) { Text("Add Wire Crate") }
                
                params.wireCrates.forEachIndexed { index, wc ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${index+1}")
                        Column(modifier = Modifier.weight(1f)) {
                            NumericField(label = "Top W", value = wc.topWidth, onChange = { 
                                val list = params.wireCrates.toMutableList()
                                list[index] = wc.copy(topWidth = it)
                                onParamChange(params.copy(wireCrates = list))
                            }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            NumericField(label = "Bot W", value = wc.bottomWidth, onChange = { 
                                val list = params.wireCrates.toMutableList()
                                list[index] = wc.copy(bottomWidth = it)
                                onParamChange(params.copy(wireCrates = list))
                            }, modifier = Modifier.fillMaxWidth())
                        }
                        NumericField(label = "H", value = wc.height, onChange = { 
                            val list = params.wireCrates.toMutableList()
                            list[index] = wc.copy(height = it)
                            onParamChange(params.copy(wireCrates = list))
                        }, modifier = Modifier.weight(1f))
                        NumericField(label = "OffsetX", value = wc.offsetX, onChange = { 
                            val list = params.wireCrates.toMutableList()
                            list[index] = wc.copy(offsetX = it)
                            onParamChange(params.copy(wireCrates = list))
                        }, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val list = params.wireCrates.toMutableList()
                            list.removeAt(index)
                            onParamChange(params.copy(wireCrates = list))
                        }) {
                            Icon(Icons.Default.Delete, "Remove")
                        }
                    }
                }
            }
        }

        Text("Plum Concrete Check Dams", style = MaterialTheme.typography.titleLarge)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val newSteps = params.plumConcretes.toMutableList()
                    newSteps.add(PlumConcrete(1.5, 1.5, 1.5, 0.25))
                    onParamChange(params.copy(plumConcretes = newSteps))
                }) { Text("Add Plum Concrete Step") }
                
                params.plumConcretes.forEachIndexed { index, pc ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${index+1}")
                        Column(modifier = Modifier.weight(1f)) {
                            NumericField(label = "Top W", value = pc.topWidth, onChange = { 
                                val list = params.plumConcretes.toMutableList()
                                list[index] = pc.copy(topWidth = it)
                                onParamChange(params.copy(plumConcretes = list))
                            }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            NumericField(label = "Bot W", value = pc.bottomWidth, onChange = { 
                                val list = params.plumConcretes.toMutableList()
                                list[index] = pc.copy(bottomWidth = it)
                                onParamChange(params.copy(plumConcretes = list))
                            }, modifier = Modifier.fillMaxWidth())
                        }
                        NumericField(label = "H", value = pc.height, onChange = { 
                            val list = params.plumConcretes.toMutableList()
                            list[index] = pc.copy(height = it)
                            onParamChange(params.copy(plumConcretes = list))
                        }, modifier = Modifier.weight(1f))
                        NumericField(label = "OffsetX", value = pc.offsetX, onChange = { 
                            val list = params.plumConcretes.toMutableList()
                            list[index] = pc.copy(offsetX = it)
                            onParamChange(params.copy(plumConcretes = list))
                        }, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val list = params.plumConcretes.toMutableList()
                            list.removeAt(index)
                            onParamChange(params.copy(plumConcretes = list))
                        }) {
                            Icon(Icons.Default.Delete, "Remove")
                        }
                    }
                }
            }
        }

        Text("Soil & Load", style = MaterialTheme.typography.titleLarge)
        Card {
             Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NumericField(label = "Soil Unit Weight (kN/m³)", value = params.soilUnitWeight, onChange = { onParamChange(params.copy(soilUnitWeight = it)) })
                NumericField(label = "Surcharge Load (kN/m²)", value = params.surcharge, onChange = { onParamChange(params.copy(surcharge = it)) })
                NumericField(label = "Internal Friction Angle φ (°)", value = params.frictionAngle, onChange = { onParamChange(params.copy(frictionAngle = it)) })
             }
        }

        Text("Material", style = MaterialTheme.typography.titleLarge)
        var expanded by remember { mutableStateOf(false) }
        val materials = listOf(
            Pair("Rubble Masonry", 22.0),
            Pair("Concrete", 24.0),
            Pair("Stone Masonry", 23.0)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = params.materialName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Wall Material") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                materials.forEach { labelModel ->
                    DropdownMenuItem(
                        text = { Text(labelModel.first) },
                        onClick = {
                            onParamChange(params.copy(materialName = labelModel.first, materialUnitWeight = labelModel.second))
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text("Site Photos", style = MaterialTheme.typography.titleLarge)
        Card {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Text("Add Photos")
                    }
                    Button(onClick = {
                        val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                        capturingUri = uri
                        takePhotoLauncher.launch(uri)
                    }) {
                        Text("Capture Photo")
                    }
                }
                
                if (params.photoUris.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(params.photoUris) { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Site Photo",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.medium),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("analyze_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
             Text("🔍 ANALYZE & GENERATE DRAWING", color = Color.White)
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternSelector(label: String, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Stone", "Concrete", "Plum", "Solid")
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
