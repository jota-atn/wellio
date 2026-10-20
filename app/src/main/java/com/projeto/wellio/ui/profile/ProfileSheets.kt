package com.projeto.wellio.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.profile.MAX_HEIGHT_CM
import com.projeto.wellio.domain.profile.MIN_HEIGHT_CM
import com.projeto.wellio.domain.profile.parseHeight
import com.projeto.wellio.ui.theme.WellioTheme

/** Sheet de um campo do perfil (SPEC §9.1): título, conteúdo e, se houver, Remover e Salvar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldSheet(
    title: String,
    onDismiss: () -> Unit,
    onSave: (() -> Unit)? = null,
    saveEnabled: Boolean = true,
    onClear: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = WellioTheme.colors.base
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            content()
            if (onSave != null || onClear != null) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onClear != null) TextButton(onClick = onClear) { Text("Remover") }
                    Spacer(Modifier.weight(1f))
                    if (onSave != null) {
                        Button(
                            onClick = onSave,
                            enabled = saveEnabled,
                            colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                        ) { Text("Salvar") }
                    }
                }
            }
        }
    }
}

/** Campo de texto que já abre com o teclado e o cursor no fim. */
@Composable
private fun FocusedField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    isError: Boolean = false,
    supportingText: String? = null
) {
    val focus = remember { FocusRequester() }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        singleLine = true,
        modifier = Modifier.fillMaxWidth().focusRequester(focus)
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

private fun valueAtEnd(text: String) = TextFieldValue(text, TextRange(text.length))

@Composable
fun NameSheet(name: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(valueAtEnd(name)) }
    val trimmed = value.text.trim()
    FieldSheet("Nome", onDismiss, onSave = { onSave(trimmed) }, saveEnabled = trimmed.isNotEmpty()) {
        FocusedField(
            value = value,
            onValueChange = { value = it.copy(text = it.text.take(40)) },
            label = "Como quer ser chamado",
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )
    }
}

@Composable
fun HeightSheet(heightCm: Int?, onDismiss: () -> Unit, onSave: (Int) -> Unit, onClear: (() -> Unit)?) {
    var value by remember { mutableStateOf(valueAtEnd(heightCm?.toString() ?: "")) }
    val parsed = parseHeight(value.text)
    FieldSheet("Altura", onDismiss, onSave = { parsed?.let(onSave) }, saveEnabled = parsed != null, onClear = onClear) {
        FocusedField(
            value = value,
            onValueChange = { value = it.copy(text = it.text.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(5)) },
            label = "Altura (cm ou m)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = value.text.isNotEmpty() && parsed == null,
            supportingText = if (value.text.isNotEmpty() && parsed == null) "Entre $MIN_HEIGHT_CM e $MAX_HEIGHT_CM cm" else "Ex.: 178 ou 1,78"
        )
    }
}

/** Escolher uma entre [options]; tocar já salva. */
@Composable
fun <T> OptionSheet(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
    onClear: (() -> Unit)?,
    hint: ((T) -> String)? = null
) {
    FieldSheet(title, onDismiss, onClear = onClear) {
        Column(Modifier.selectableGroup()) {
            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = option == selected, role = Role.RadioButton, onClick = { onPick(option) })
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = option == selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                        hint?.let {
                            Text(it(option), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Trocar ou remover a foto. */
@Composable
fun PhotoOptionsSheet(hasPhoto: Boolean, onDismiss: () -> Unit, onPick: () -> Unit, onRemove: () -> Unit) {
    FieldSheet("Foto", onDismiss) {
        TextButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text(if (hasPhoto) "Escolher outra foto" else "Escolher foto")
        }
        if (hasPhoto) {
            TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                Text("Remover foto", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
