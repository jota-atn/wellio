package com.projeto.wellio.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projeto.wellio.domain.home.CardSize
import com.projeto.wellio.ui.module.Summary
import com.projeto.wellio.ui.theme.ModuleColors

/**
 * Card de meta simples (Alimentação e Exercício até serem refeitos, SPEC §11 M1.5):
 * ícone, valor/meta, barra e "Registrar". Tocar no card também registra, já que esses
 * módulos ainda não têm tela.
 */
@Composable
fun GoalCard(
    title: String,
    @DrawableRes icon: Int,
    colors: ModuleColors,
    summary: Summary,
    size: CardSize,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onRegister,
        shape = RoundedCornerShape(28.dp),
        color = colors.container,
        contentColor = colors.onContainer,
        modifier = modifier
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = colors.accent, contentColor = colors.onAccent, modifier = Modifier.size(30.dp)) {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(6.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.semantics(mergeDescendants = true) { contentDescription = summary.spoken },
                verticalAlignment = Alignment.Bottom
            ) {
                Text(summary.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                summary.secondary?.let {
                    Spacer(Modifier.width(4.dp))
                    Text("$it kcal", style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.track)
                ) {
                    Box(Modifier.fillMaxWidth(summary.progress ?: 0f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.accent))
                }
                if (size == CardSize.Full) {
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onRegister,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Registrar")
                    }
                }
            }
        }
    }
}
