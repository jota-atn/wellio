package com.projeto.wellio.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.time.nextDay
import com.projeto.wellio.domain.time.previousDay
import com.projeto.wellio.ui.format.dayLabel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** "‹ Qui, 1 out ›": setas trocam o dia e tocar na data abre o calendário. Nunca passa de hoje. */
@Composable
fun DayNavigator(
    day: LocalDate,
    today: LocalDate,
    onDayChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var pickingDate by remember { mutableStateOf(false) }
    val next = nextDay(day, today)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDayChange(previousDay(day)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Dia anterior")
        }
        TextButton(
            onClick = { pickingDate = true },
            modifier = Modifier.semantics { contentDescription = "${dayLabel(day, today)}. Escolher outro dia" }
        ) {
            Text(dayLabel(day, today), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = { next?.let(onDayChange) }, enabled = next != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo dia")
        }
    }

    if (pickingDate) {
        DayPickerDialog(day, today, onDismiss = { pickingDate = false }, onPick = {
            pickingDate = false
            onDayChange(it)
        })
    }
}

/** Deslizar para a direita volta um dia; para a esquerda avança (até hoje). */
fun Modifier.swipeBetweenDays(day: LocalDate, today: LocalDate, onDayChange: (LocalDate) -> Unit): Modifier =
    pointerInput(day, today) {
        var dragged = 0f
        val threshold = 80.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = {
                when {
                    dragged > threshold -> onDayChange(previousDay(day))
                    dragged < -threshold -> nextDay(day, today)?.let(onDayChange)
                }
            },
            onHorizontalDrag = { _, amount -> dragged += amount }
        )
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(
    day: LocalDate,
    today: LocalDate,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    // O DatePicker trabalha com meia-noite UTC
    fun LocalDate.utcMillis() = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val todayMillis = today.utcMillis()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = day.utcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            override fun isSelectableYear(year: Int) = year <= today.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis
                    ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    ?.let(onPick) ?: onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = state)
    }
}
