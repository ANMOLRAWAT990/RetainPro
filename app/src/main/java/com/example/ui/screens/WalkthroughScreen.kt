package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Walkthrough") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Welcome to Retaining Wall Designer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "This walkthrough explains the functionalities of this app, the connectivity between different modules, and how to effectively generate retaining wall designs.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }

            item {
                WalkthroughSection(
                    icon = Icons.Default.DesignServices,
                    title = "1. Design Module",
                    description = "The Design tab is the central hub where you input the geometric and structural parameters of your retaining wall." +
                            "\n\n• Wall Geometry: Configure the base structure including height, top width, bottom width, and slope." +
                            "\n• Sub-Parts Options: You can toggle specific structural components using the switches:" +
                            "\n   - Retaining Wall (RR Dry Stone Masonry with 1:6 bands)" +
                            "\n   - HPS Filling (Hand Packed Stone Filling behind the wall)" +
                            "\n   - Weep Holes (Individually toggleable for Wall, Wire Crates, and CC Plum, calculating precise locations at 1.5m horizontal & 1m vertical spacing, sloped at 1:20)" +
                            "\n   - Wire Crates (Gabion boxes at the foundation)" +
                            "\n   - CC Plum (1:3:6 plum concrete elements in the foundation)" +
                            "\n\nAny parameters changed here instantly flow to the Analysis and Drawing modules without requiring manual synchronization."
                )
            }

            item {
                WalkthroughSection(
                    icon = Icons.Default.Calculate,
                    title = "2. Analysis & Estimation",
                    description = "Once your parameters are set, navigating to the Analysis tab calculates building estimates and costs dynamically." +
                            "\n\n• Seamless Connectivity: The estimation engine pulls geometry directly from the Design tab in real-time." +
                            "\n• Volume Calculation: It automatically computes excavation earthwork volume, RR Masonry volume (with separate band vs. dry capacities), HPS filling volume, plum concrete, wire crates, and total weep holes length." +
                            "\n• Cost Breakdown: Uses standard rates (which can be tweaked in the design module) to multiply against capacities and give a final estimated cost." +
                            "\n• Export: You can export this detailed estimation as a CSV file to share or review on your computer."
                )
            }

            item {
                WalkthroughSection(
                    icon = Icons.Default.Architecture,
                    title = "3. Drawings & Visualizer",
                    description = "The Drawing tab is a powerful 2D parametric visualizer that constructs to-scale CAD-like diagrams directly from the math model (WallParams)." +
                            "\n\nHow the Drawings are Constructed:" +
                            "\n• Scale Mapping: The app calculates the real-world geometry bounding box (e.g. max height and width in meters) and scales it dynamically to fit the screen dimensions using a 'pixels per meter' multiplier. This guarantees elements are always perfectly proportional." +
                            "\n• Procedural Generation: Shapes are not static images. The Cross Section creates a `Path` drawn from bottom-left `(0, 0)` equivalent, progressing upwards to map the slope angles, top widths, and back slope offsets. The front elevation uses a simple rectangle with height * length, but applies procedural patterns for stone masonry." +
                            "\n• Component Stacking: In foundation blocks (Plum/Wire Crates), the canvas loop checks `ignore offsets` which defines if the foundation has a vertical back face or stepped back face. Blocks are stacked iteratively downwards starting from the base level of the wall. Separation lines step every 2 meters horizontally." +
                            "\n• Weep Holes Routing: For every meter vertically, and 1.5 meters horizontally, the app computes if the exact coordinate falls inside a valid structural component (Wall, or WireCrate, or Plum - matching your specific toggles). If valid, it plots a Weep Hole. On the side view, it projects a line angled precisely at a 1:20 drainage slope pointing outwards." +
                            "\n• Dimensions & Leaders: Extension lines and texts calculate text boundaries automatically and place themselves explicitly offset from the structural edges, rendering clean structural drafting aesthetics." +
                            "\n• DXF Translation Engine: Exporting DXF doesn't capture an image; it generates a true CAD text file. It translates Android `drawPath` points directly to DXF `LINE`, and `drawCircle` loops directly into DXF `CIRCLE` blocks. It even bounds the drawing in a standard A4 Title-Block layout automatically."
                )
            }

            item {
                WalkthroughSection(
                    icon = Icons.Default.Save,
                    title = "4. Project Logging",
                    description = "The Logs tab allows for saving iteration stages. By clicking 'Save Design' in the Design tab, the complete app state (geometry, toggled sub-parts, and computed lengths) is snapshotted." +
                            "\n\n• Restore State: Tapping a log restores the snapshot entirely across Design, Analysis, and Drawing modules seamlessly, making it easy to create multiple design variations."
                )
            }

            item {
                WalkthroughSection(
                    icon = Icons.Default.AutoAwesome,
                    title = "System Interconnectivity",
                    description = "All tabs operate on a unified data state. Toggling off a component like 'CC Plum' in the Design Tab immediately eliminates its volume from the Estimator, re-adjusts the DXF plotting boundaries, and removes it from the screen Visualizer. This live reactivity prevents out-of-sync errors between cost sheets and drawings."
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun WalkthroughSection(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
