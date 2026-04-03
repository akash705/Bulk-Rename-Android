package com.bulkrenamer.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bulkrenamer.data.model.NumberPosition
import com.bulkrenamer.data.model.RenameRule

enum class RuleType(val label: String) {
    PREFIX("Prefix"),
    SUFFIX("Suffix"),
    EXTENSION("Extension"),
    BASE_NAME("Base Name"),
    REPLACE("Replace"),
    NUMBERING("Number")
}

@Composable
fun RenameRuleDialog(
    onConfirm: (RenameRule) -> Unit,
    onDismiss: () -> Unit
) {
    var expandedType by remember { mutableStateOf<RuleType?>(null) }

    // Per-section state
    var prefixInput by remember { mutableStateOf("") }
    var suffixInput by remember { mutableStateOf("") }
    var beforeExtension by remember { mutableStateOf(true) }
    var extensionInput by remember { mutableStateOf("") }
    var baseNameInput by remember { mutableStateOf("") }
    var findInput by remember { mutableStateOf("") }
    var replaceInput by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var useRegex by remember { mutableStateOf(false) }
    var numStartAt by remember { mutableIntStateOf(1) }
    var numStep by remember { mutableIntStateOf(1) }
    var numPadWidth by remember { mutableIntStateOf(0) }
    var numPosition by remember { mutableStateOf(NumberPosition.PREFIX) }
    var numSeparator by remember { mutableStateOf("_") }

    fun buildRule(): RenameRule? = when (expandedType) {
        RuleType.PREFIX -> RenameRule.AddPrefix(prefixInput.trim())
        RuleType.SUFFIX -> RenameRule.AddSuffix(suffixInput.trim(), beforeExtension)
        RuleType.EXTENSION -> RenameRule.ChangeExtension(extensionInput.trim())
        RuleType.BASE_NAME -> RenameRule.SetBaseName(baseNameInput.trim())
        RuleType.REPLACE -> {
            val find = findInput.trim()
            if (find.isEmpty()) null else RenameRule.ReplaceText(find, replaceInput.trim(), caseSensitive, useRegex)
        }
        RuleType.NUMBERING -> RenameRule.AddNumbering(
            startAt = numStartAt,
            step = numStep,
            padWidth = numPadWidth,
            position = numPosition,
            separator = numSeparator
        )
        null -> null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title row with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Configure Rename Rule",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable sections
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    RuleType.entries.forEachIndexed { index, type ->
                        val isExpanded = expandedType == type

                        // Section header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedType = if (isExpanded) null else type
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = if (isExpanded) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Collapsible content
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                when (type) {
                                    RuleType.PREFIX -> {
                                        OutlinedTextField(
                                            value = prefixInput,
                                            onValueChange = { prefixInput = it },
                                            label = { Text("Prefix") },
                                            placeholder = { Text("e.g. 2024_") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    RuleType.SUFFIX -> {
                                        OutlinedTextField(
                                            value = suffixInput,
                                            onValueChange = { suffixInput = it },
                                            label = { Text("Suffix") },
                                            placeholder = { Text("e.g. _backup") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = beforeExtension,
                                                    onClick = { beforeExtension = !beforeExtension },
                                                    role = Role.Checkbox
                                                )
                                                .padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = beforeExtension, onCheckedChange = null)
                                            Text("Insert before extension", modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }

                                    RuleType.EXTENSION -> {
                                        OutlinedTextField(
                                            value = extensionInput,
                                            onValueChange = { extensionInput = it },
                                            label = { Text("New extension") },
                                            placeholder = { Text("e.g. png (leave empty to strip)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    RuleType.BASE_NAME -> {
                                        OutlinedTextField(
                                            value = baseNameInput,
                                            onValueChange = { baseNameInput = it },
                                            label = { Text("Base name") },
                                            placeholder = { Text("e.g. vacation") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    RuleType.REPLACE -> {
                                        OutlinedTextField(
                                            value = findInput,
                                            onValueChange = { findInput = it },
                                            label = { Text("Find") },
                                            placeholder = { Text("Text to find") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = replaceInput,
                                            onValueChange = { replaceInput = it },
                                            label = { Text("Replace with") },
                                            placeholder = { Text("Replacement (empty to delete)") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = caseSensitive,
                                                    onClick = { caseSensitive = !caseSensitive },
                                                    role = Role.Checkbox
                                                )
                                                .padding(top = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = caseSensitive, onCheckedChange = null)
                                            Text("Case sensitive", modifier = Modifier.padding(start = 8.dp))
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .selectable(
                                                    selected = useRegex,
                                                    onClick = { useRegex = !useRegex },
                                                    role = Role.Checkbox
                                                )
                                                .padding(top = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = useRegex, onCheckedChange = null)
                                            Text("Use regex", modifier = Modifier.padding(start = 8.dp))
                                        }
                                    }

                                    RuleType.NUMBERING -> {
                                        Text(
                                            text = "Position",
                                            style = MaterialTheme.typography.labelMedium,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            NumberPosition.entries.forEach { pos ->
                                                Row(
                                                    modifier = Modifier
                                                        .selectable(
                                                            selected = numPosition == pos,
                                                            onClick = { numPosition = pos },
                                                            role = Role.RadioButton
                                                        )
                                                        .padding(end = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(selected = numPosition == pos, onClick = null)
                                                    Text(
                                                        text = pos.name.lowercase().replaceFirstChar { it.uppercase() },
                                                        modifier = Modifier.padding(start = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = numStartAt.toString(),
                                                onValueChange = { numStartAt = it.toIntOrNull()?.coerceAtLeast(0) ?: numStartAt },
                                                label = { Text("Start") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = numStep.toString(),
                                                onValueChange = { numStep = it.toIntOrNull()?.coerceAtLeast(1) ?: numStep },
                                                label = { Text("Step") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = if (numPadWidth == 0) "" else numPadWidth.toString(),
                                                onValueChange = { numPadWidth = it.toIntOrNull()?.coerceAtLeast(0) ?: 0 },
                                                label = { Text("Pad width") },
                                                placeholder = { Text("0 = none") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = numSeparator,
                                                onValueChange = { numSeparator = it },
                                                label = { Text("Separator") },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Divider between sections (not after the last one)
                        if (index < RuleType.entries.size - 1) {
                            HorizontalDivider()
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(
                        onClick = { buildRule()?.let(onConfirm) },
                        enabled = buildRule() != null
                    ) {
                        Text("Preview")
                    }
                }
            }
        }
    }
}
