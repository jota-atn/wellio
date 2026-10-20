package com.projeto.wellio.ui.profile

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.projeto.wellio.R
import com.projeto.wellio.ui.theme.WellioTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Foto do perfil num círculo (SPEC §9.0, §9.1). Sem foto, a inicial do nome sobre a cor
 * base do app; sem nome, o ícone de pessoa.
 */
@Composable
fun ProfileAvatar(photo: File?, name: String, size: Dp, modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    val image by produceState<ImageBitmap?>(null, photo) {
        value = photo?.let { file ->
            withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
        }
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(colors.container),
        contentAlignment = Alignment.Center
    ) {
        val current = image
        val initial = name.trim().firstOrNull()?.uppercaseChar()
        when {
            current != null -> Image(current, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            initial != null -> Text(
                initial.toString(),
                color = colors.onContainer,
                fontSize = (size.value * 0.42f).sp,
                fontWeight = FontWeight.SemiBold
            )
            else -> Icon(
                painterResource(R.drawable.ic_profile_person),
                contentDescription = null,
                tint = colors.onContainer,
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}
