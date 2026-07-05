package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun NumericField(label: String, value: Double, onChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    
    OutlinedTextField(
        value = textValue,
        onValueChange = { 
            textValue = it
            it.toDoubleOrNull()?.let { num -> onChange(num) }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
        singleLine = true
    )
}
