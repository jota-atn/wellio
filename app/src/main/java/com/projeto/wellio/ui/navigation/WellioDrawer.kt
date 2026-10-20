package com.projeto.wellio.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.projeto.wellio.ui.format.formatHeight
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.profile.PROFILE_PHOTO_KEY
import com.projeto.wellio.ui.profile.ProfileAvatar
import com.projeto.wellio.ui.profile.ProfileUiState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.projeto.wellio.ui.module.Modules
import com.projeto.wellio.ui.module.WellioModule
import com.projeto.wellio.ui.theme.WellioTheme

private val menuItems = listOf("Estatísticas", "Conquistas", "Anotações", "Receitas", "Metas", "Configurações")

/**
 * Sidebar com a estrutura da SPEC §2.2; o painel rápido vem depois (M4). A seção Registros
 * sai dos módulos (cada um com ícone e cor). [photoVisible]: a foto só aparece com a sidebar
 * aberta, para voar até o Perfil na ida e não voar para fora da tela na volta.
 */
@Composable
fun WellioDrawerContent(
    profile: ProfileUiState,
    photoVisible: Boolean,
    onProfileClick: () -> Unit,
    onModuleClick: (WellioModule) -> Unit,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp)) {
            ProfileHeader(profile, photoVisible, onProfileClick)

            Text(
                text = "Registros",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
            )
            Modules.all.forEach { module ->
                NavigationDrawerItem(
                    label = { Text(module.title) },
                    icon = {
                        Icon(
                            painterResource(module.icon),
                            contentDescription = null,
                            tint = module.colors(WellioTheme.colors).accent
                        )
                    },
                    selected = false,
                    onClick = { onModuleClick(module) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp, horizontal = 16.dp))
            menuItems.forEach { DrawerItem(it, onItemClick) }
        }
    }
}

@Composable
private fun DrawerItem(label: String, onItemClick: (String) -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        onClick = { onItemClick(label) },
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

/** Cabeçalho (SPEC §9.0): foto, nome e "72,4 kg · 1,78 m · 27 anos"; tudo abre o Perfil. */
@Composable
private fun ProfileHeader(profile: ProfileUiState, photoVisible: Boolean, onClick: () -> Unit) {
    val colors = WellioTheme.colors.base
    val name = profile.profile.name
    val info = listOfNotNull(
        profile.weightKg?.let(::formatKg),
        profile.profile.heightCm?.let(::formatHeight),
        profile.age?.let { "$it anos" }
    ).joinToString(" · ")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // O lugar da foto fica reservado mesmo com ela escondida
        Box(Modifier.size(96.dp)) {
            DrawerPhoto(profile, photoVisible, onClick)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            name.ifBlank { "Seu perfil" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = colors.container,
            contentColor = colors.onContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                info.ifBlank { "Complete seu perfil" },
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

/** A foto, com a animação que a liga à do Perfil. */
@Composable
private fun DrawerPhoto(profile: ProfileUiState, visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
            ProfileAvatar(
                profile.photo,
                profile.profile.name,
                size = 96.dp,
                modifier = Modifier
                    .sharedHero(PROFILE_PHOTO_KEY)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Abrir perfil", onClick = onClick)
            )
        }
    }
}
