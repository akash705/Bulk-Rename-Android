package com.bulkrenamer.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bulkrenamer.data.model.RenameRule

enum class RuleType(val label: String) {
    PREFIX("Prefix"),
    SUFFIX("Suffix"),
    EXTENSION("Extension"),
    BASE_NAME("Base Name"),
    REPLACE("Replace")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameRuleDialog(
    onConfirm: (RenameRule) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(RuleType.PREFIX) }
    var input1 by remember { mutableStateOf("") }
    var input2 by remember { mutableStateOf("") }
    var beforeExtension by remember { mutableStateOf(true) }
    var caseSensitive by remember { mutableStateOf(false) }

    fun buildRule(): RenameRule? = when (selectedType) {
        RuleType.PREFIX -> RenameRule.AddPrefix(input1)
        RuleType.SUFFIX -> RenameRule.AddSuffix(input1, beforeExtension)
        RuleType.EXTENSION -> RenameRule.ChangeExtension(input1)
        RuleType.BASE_NAME -> RenameRule.SetBaseName(input1)
        RuleType.REPLACE -> if (input1.isEmpty()) null else RenameRule.ReplaceText(input1, input2, caseSensitive)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Rename Rule") },
        text = {
            Column {
                // Rule type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RuleType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = {
                                selectedType = type
                                input1 = ""
                                input2 = ""
                            },
                            label = { Text(type.label) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input fields based on type
                when (selectedType) {
                    RuleType.PREFIX -> OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Prefix") },
                        placeholder = { Text("e.g. 2024_") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    RuleType.SUFFIX -> {
                        OutlinedTextField(
                            value = input1,
                            onValueChange = { input1 = it },
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
                            Text(
                                text = "Insert before extension",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    RuleType.EXTENSION -> OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("New extension") },
                        placeholder = { Text("e.g. png (leave empty to strip)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    RuleType.BASE_NAME -> OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Base name") },
                        placeholder = { Text("e.g. vacation") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    RuleType.REPLACE -> {
                        OutlinedTextField(
                            value = input1,
                            onValueChange = { input1 = it },
                            label = { Text("Find") },
                            placeholder = { Text("Text to find") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = input2,
                            onValueChange = { input2 = it },
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
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { buildRule()?.let(onConfirm) },
                enabled = buildRule() != null
            ) {
                Text("Preview")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
