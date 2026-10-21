package com.projeto.wellio.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.projeto.wellio.ui.theme.DarkWellioColors
import com.projeto.wellio.ui.theme.ModuleColors
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.R
import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.Profile
import com.projeto.wellio.domain.model.Sex
import com.projeto.wellio.domain.profile.BmiRange
import com.projeto.wellio.domain.profile.CalorieSuggestion
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.ui.components.DayPickerDialog
import com.projeto.wellio.ui.format.formatHeight
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.navigation.LocalNavAnimatedScope
import com.projeto.wellio.ui.navigation.sharedHero
import com.projeto.wellio.ui.theme.WellioTheme
import com.projeto.wellio.ui.weight.TargetSheet
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onOpenWeight: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        state = state,
        onBack = onBack,
        onOpenWeight = onOpenWeight,
        onUpdate = viewModel::update,
        onPhotoCropped = viewModel::setPhoto,
        onRemovePhoto = viewModel::removePhoto,
        onUseSuggestion = viewModel::useSuggestion,
        onSetTarget = viewModel::setTarget,
        onClearTarget = viewModel::clearTarget,
        onCoverColor = viewModel::setCoverColor,
        onCoverPhoto = viewModel::setCoverPhoto,
        onResetCover = viewModel::resetCover
    )
}

/** Campo do perfil sendo editado num sheet. */
private enum class ProfileField { Photo, Cover, Name, BirthDate, Sex, Height, Objective, Activity }

/** Proporção da foto da capa (largura / altura). */
private const val COVER_ASPECT = 2.4f

/**
 * Tela de Perfil (SPEC §9.1): a foto e a ficha com idade, IMC e meta sugerida no topo;
 * abaixo, uma linha por campo, cada uma abre um sheet só dela. O peso abre a tela de Peso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onBack: () -> Unit,
    onOpenWeight: () -> Unit,
    onUpdate: ((Profile) -> Profile) -> Unit,
    onPhotoCropped: (Bitmap) -> Unit,
    onRemovePhoto: () -> Unit,
    onUseSuggestion: (Int) -> Unit,
    onSetTarget: (Double) -> Unit,
    onClearTarget: () -> Unit,
    onCoverColor: (CoverColor) -> Unit = {},
    onCoverPhoto: (Bitmap) -> Unit = {},
    onResetCover: () -> Unit = {}
) {
    val profile = state.profile
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ProfileField?>(null) }
    var cropping by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropping = uri
    }

    var croppingCover by remember { mutableStateOf<Uri?>(null) }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) croppingCover = uri
    }

    fun launchPicker() {
        editing = null
        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun save(transform: (Profile) -> Profile) {
        onUpdate(transform)
        editing = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        // A capa vai até o topo da tela, por baixo da barra de status
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp) + WindowInsets.navigationBars.asPaddingValues(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "capa") {
                ProfileCover(
                    state = state,
                    onBack = onBack,
                    onPhotoClick = { if (state.photo == null) launchPicker() else editing = ProfileField.Photo },
                    onNameClick = { editing = ProfileField.Name },
                    onCoverClick = { editing = ProfileField.Cover }
                )
            }
            item(key = "ficha") {
                ProfileSheetCard(state, Modifier.padding(horizontal = 16.dp), onUse = { kcal ->
                    onUseSuggestion(kcal)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar("Meta de kcal agora é ${formatInt(kcal)}")
                    }
                })
            }
            item(key = "campos") {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        ProfileFieldRow("Nascimento", profile.birthDate?.format(birthFormatter), R.drawable.ic_profile_cake) { editing = ProfileField.BirthDate }
                        ProfileFieldRow("Sexo", profile.sex?.label, R.drawable.ic_profile_person) { editing = ProfileField.Sex }
                        ProfileFieldRow("Altura", profile.heightCm?.let(::formatHeight), R.drawable.ic_profile_ruler) { editing = ProfileField.Height }
                        ProfileFieldRow("Peso", state.weightKg?.let(::formatKg) ?: "Registrar", R.drawable.ic_module_weight, onClick = onOpenWeight)
                        // O objetivo sai do alvo: o alvo à direita, o objetivo embaixo do rótulo
                        ProfileFieldRow(
                            "Objetivo",
                            state.weightGoal?.let { formatKg(it.targetKg) } ?: "Definir alvo",
                            R.drawable.ic_profile_target,
                            detail = state.objective.label
                        ) { editing = ProfileField.Objective }
                        ProfileFieldRow("Atividade", profile.activity?.label, R.drawable.ic_profile_sneaker) { editing = ProfileField.Activity }
                    }
                }
            }
        }
    }

    when (editing) {
        ProfileField.Photo -> PhotoOptionsSheet(
            hasPhoto = state.photo != null,
            onDismiss = { editing = null },
            onPick = ::launchPicker,
            onRemove = {
                onRemovePhoto()
                editing = null
            }
        )
        ProfileField.Cover -> CoverSheet(
            selected = CoverColor.of(profile.coverColor),
            hasPhoto = state.coverPhoto != null,
            custom = profile.coverColor != null || profile.coverPhoto != null,
            onDismiss = { editing = null },
            onColor = {
                onCoverColor(it)
                editing = null
            },
            onPhoto = {
                editing = null
                pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onReset = {
                onResetCover()
                editing = null
            }
        )
        ProfileField.Name -> NameSheet(profile.name, onDismiss = { editing = null }, onSave = { name -> save { it.copy(name = name) } })
        ProfileField.BirthDate -> DayPickerDialog(
            day = profile.birthDate ?: state.today.minusYears(25),
            today = state.today,
            onDismiss = { editing = null },
            onPick = { day -> save { it.copy(birthDate = day) } }
        )
        ProfileField.Sex -> OptionSheet(
            title = "Sexo",
            options = Sex.entries,
            selected = profile.sex,
            label = { it.label },
            onDismiss = { editing = null },
            onPick = { sex -> save { it.copy(sex = sex) } },
            onClear = profile.sex?.let { { save { it.copy(sex = null) } } }
        )
        ProfileField.Height -> HeightSheet(
            heightCm = profile.heightCm,
            onDismiss = { editing = null },
            onSave = { cm -> save { it.copy(heightCm = cm) } },
            onClear = profile.heightCm?.let { { save { it.copy(heightCm = null) } } }
        )
        ProfileField.Objective -> TargetSheet(
            targetKg = state.weightGoal?.targetKg,
            currentKg = state.weightKg ?: DEFAULT_WEIGHT_KG,
            startKg = state.weightGoal?.startKg ?: state.weightKg ?: DEFAULT_WEIGHT_KG,
            onDismiss = { editing = null },
            onSave = {
                onSetTarget(it)
                editing = null
            },
            onRemove = state.weightGoal?.let {
                {
                    onClearTarget()
                    editing = null
                }
            }
        )
        ProfileField.Activity -> OptionSheet(
            title = "Nível de atividade",
            options = ActivityLevel.entries,
            selected = profile.activity,
            label = { it.label },
            hint = { it.hint },
            onDismiss = { editing = null },
            onPick = { level -> save { it.copy(activity = level) } },
            onClear = profile.activity?.let { { save { it.copy(activity = null) } } }
        )
        null -> Unit
    }

    cropping?.let { uri ->
        PhotoCropDialog(uri, onDismiss = { cropping = null }, onCropped = {
            onPhotoCropped(it)
            cropping = null
        })
    }
    croppingCover?.let { uri ->
        PhotoCropDialog(uri, aspect = COVER_ASPECT, onDismiss = { croppingCover = null }, onCropped = {
            onCoverPhoto(it)
            croppingCover = null
        })
    }
}

internal val birthFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** A foto do cabeçalho da sidebar voa e cresce até a do Perfil (SPEC §9.1). */
const val PROFILE_PHOTO_KEY = "profile-photo"

private val CoverHeight = 152.dp
private val PhotoSize = 128.dp
private val RingWidth = 5.dp

/**
 * Capa (SPEC §9.1): azul-marinho de canto a canto, com a gota, a fogueira e a balança em
 * marca-d'água; a foto metade sobre ela, num anel com as três cores; o nome e há quanto
 * tempo a pessoa usa o app.
 */
@Composable
private fun ProfileCover(
    state: ProfileUiState,
    onBack: () -> Unit,
    onPhotoClick: () -> Unit,
    onNameClick: () -> Unit,
    onCoverClick: () -> Unit
) {
    val colors = WellioTheme.colors.base
    val name = state.profile.name
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val photoBox = PhotoSize + RingWidth * 4
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBar + CoverHeight)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(CoverColor.of(state.profile.coverColor).brush)
        ) {
            val coverImage by produceState<ImageBitmap?>(null, state.coverPhoto) {
                value = state.coverPhoto?.let { file ->
                    withContext(Dispatchers.IO) { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }
                }
            }
            val image = coverImage
            if (image != null) {
                Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                // Escurecido leve para o título e os botões continuarem legíveis
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.35f)))
            } else {
                CoverWatermark(Modifier.matchParentSize())
            }
            IconButton(onClick = onBack, modifier = Modifier.padding(top = statusBar + 4.dp, start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
            }
            Text(
                "Perfil",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = statusBar + 14.dp)
            )
            IconButton(onClick = onCoverClick, modifier = Modifier.align(Alignment.TopEnd).padding(top = statusBar + 4.dp, end = 4.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Mudar a capa", tint = Color.White)
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = statusBar + CoverHeight - photoBox / 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(photoBox), contentAlignment = Alignment.Center) {
                TricolorRing(Modifier.matchParentSize())
                ProfileAvatar(
                    state.photo,
                    name,
                    size = PhotoSize,
                    modifier = Modifier
                        .sharedHero(PROFILE_PHOTO_KEY)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clip(CircleShape)
                        .clickable(onClickLabel = if (state.photo == null) "Escolher foto" else "Trocar foto", onClick = onPhotoClick)
                )
                // Selo da câmera sobre a borda da foto; chegando pela sidebar, só aparece depois
                // que a foto pousa (durante o voo ela fica por cima de tudo e o cobriria)
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .popInAfterArrival()
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable(onClickLabel = "Trocar foto", onClick = onPhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_profile_camera), contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                name.ifBlank { "Toque para pôr seu nome" },
                style = if (name.isBlank()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = if (name.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Editar nome", onClick = onNameClick)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
            Text(
                journeyText(state.daysInApp, state.records),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "No Wellio há 23 dias · 412 registros". */
private fun journeyText(days: Int?, records: Int): String {
    if (days == null) return "Começando a jornada no Wellio"
    val since = if (days == 1) "No Wellio desde hoje" else "No Wellio há $days dias"
    return "$since · ${formatInt(records)} ${if (records == 1) "registro" else "registros"}"
}

/** Gota, fogueira e balança espalhadas na capa, bem apagadas. */
@Composable
private fun CoverWatermark(modifier: Modifier = Modifier) {
    val tint = Color.White.copy(alpha = 0.1f)
    BoxWithConstraints(modifier) {
        listOf(
            Triple(R.drawable.ic_module_water, Offset(0.08f, 0.35f), -14f),
            Triple(R.drawable.ic_module_exercise, Offset(0.78f, 0.22f), 12f),
            Triple(R.drawable.ic_module_weight, Offset(0.62f, 0.62f), -8f),
            Triple(R.drawable.ic_module_water, Offset(0.88f, 0.7f), 18f),
            Triple(R.drawable.ic_module_exercise, Offset(0.2f, 0.72f), 6f)
        ).forEach { (icon, at, angle) ->
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .offset(x = maxWidth * at.x, y = maxHeight * at.y)
                    .size(56.dp)
                    .graphicsLayer { rotationZ = angle }
            )
        }
    }
}

/** Anel com as cores da água, das calorias e do exercício, que se desenha ao abrir. */
@Composable
private fun TricolorRing(modifier: Modifier = Modifier) {
    val colors = listOf(DarkWellioColors.water.accent, DarkWellioColors.food.accent, DarkWellioColors.exercise.accent)
    val sweep = remember { Animatable(0f) }
    LaunchedEffect(Unit) { sweep.animateTo(1f, tween(durationMillis = 900, delayMillis = 250, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val width = RingWidth.toPx()
        val arc = Size(size.width - width, size.height - width)
        val topLeft = Offset(width / 2, width / 2)
        colors.forEachIndexed { index, color ->
            // Três arcos de 120° com um respiro entre eles
            drawArc(color, -90f + index * 120f + 6f, (120f - 12f) * sweep.value, false, topLeft, arc, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}

/** Cor da faixa do IMC: azul abaixo, verde normal, âmbar sobrepeso, vermelho obesidade. */
@Composable
private fun bmiColor(range: BmiRange): Color = when (range) {
    BmiRange.Under -> WellioTheme.colors.water.accent
    BmiRange.Normal -> Color(0xFF6FCF7A)
    BmiRange.Over -> Color(0xFFF5B940)
    BmiRange.Obese -> Color(0xFFFF6B6B)
}

/** Número que conta de 0 até [value] ao aparecer (e anima ao mudar). */
@Composable
private fun CountUp(value: Double, format: (Double) -> String, style: androidx.compose.ui.text.TextStyle) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(value) { shown.animateTo(value.toFloat(), tween(durationMillis = 900, easing = FastOutSlowInEasing)) }
    Text(format(shown.value.toDouble()), style = style, fontWeight = FontWeight.Bold)
}

/**
 * A ficha (SPEC §9.1): idade, IMC na régua das faixas e a meta calórica sugerida, com o
 * botão "Usar". Os números contam ao abrir.
 */
@Composable
private fun ProfileSheetCard(state: ProfileUiState, modifier: Modifier = Modifier, onUse: (Int) -> Unit) {
    val colors = WellioTheme.colors.base
    val suggestion = state.suggestion
    val numberStyle = MaterialTheme.typography.headlineSmall
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SheetTile(R.drawable.ic_profile_cake, WellioTheme.colors.water, Modifier.weight(1f)) {
                    val age = state.age
                    if (age != null) CountUp(age.toDouble(), { it.roundToInt().toString() }, numberStyle) else Missing()
                    TileCaption(if (age != null) "anos" else "informe o nascimento")
                }
                SheetTile(R.drawable.ic_profile_ruler, WellioTheme.colors.food, Modifier.weight(1.3f)) {
                    val bmi = state.bmi
                    if (bmi != null) {
                        CountUp(bmi, { String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it) }, numberStyle)
                        Spacer(Modifier.height(6.dp))
                        BmiRuler(bmi)
                        Spacer(Modifier.height(4.dp))
                        TileCaption("IMC · ${state.bmiRange?.label?.lowercase()}")
                    } else {
                        Missing()
                        TileCaption(if (state.profile.heightCm == null) "informe a altura" else "registre o peso")
                    }
                }
                SheetTile(R.drawable.ic_profile_flame, WellioTheme.colors.exercise, Modifier.weight(1f)) {
                    if (suggestion is CalorieSuggestion.Ready) {
                        CountUp(suggestion.kcal.toDouble(), { formatInt(it.roundToInt()) }, numberStyle)
                    } else {
                        Missing()
                    }
                    TileCaption(
                        when (suggestion) {
                            is CalorieSuggestion.Ready -> if (suggestion.atFloor) "kcal · mínimo recomendado" else "kcal sugeridas"
                            is CalorieSuggestion.Missing -> "falta " + suggestion.fields.joinToString(", ") { it.label }
                            CalorieSuggestion.SexUndisclosed -> "a fórmula depende do sexo"
                        }
                    )
                }
            }
            if (suggestion is CalorieSuggestion.Ready) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val current = state.foodGoal.daily
                    Text(
                        if (current == suggestion.kcal) "É a sua meta diária de kcal" else "Sua meta hoje: ${formatInt(current)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    if (current != suggestion.kcal) {
                        Button(
                            onClick = { onUse(suggestion.kcal) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) { Text("Usar") }
                    }
                }
            }
        }
    }
}

/** Uma peça da ficha: ícone num círculo na cor do módulo e o conteúdo abaixo. */
@Composable
private fun SheetTile(icon: Int, palette: ModuleColors, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), modifier = modifier.fillMaxHeight()) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 8.dp).semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(palette.container), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun Missing() {
    Text("—", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TileCaption(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Faixas do IMC na régua: de 15 a 35, nas larguras de cada faixa. */
private val BmiBands = listOf(BmiRange.Under to 3.5f, BmiRange.Normal to 6.5f, BmiRange.Over to 5f, BmiRange.Obese to 5f)

/** Régua colorida das faixas, com o marcador deslizando até o IMC (SPEC §9.1). */
@Composable
private fun BmiRuler(bmi: Double) {
    val target = ((bmi - 15) / 20).toFloat().coerceIn(0f, 1f)
    val position = remember { Animatable(0f) }
    LaunchedEffect(target) { position.animateTo(target, tween(durationMillis = 1100, delayMillis = 150, easing = FastOutSlowInEasing)) }
    val bandColors = BmiBands.map { (range, width) -> bmiColor(range) to width }
    val marker = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.fillMaxWidth().height(16.dp)) {
        val barHeight = 6.dp.toPx()
        val top = size.height - barHeight
        var x = 0f
        val total = bandColors.sumOf { it.second.toDouble() }.toFloat()
        bandColors.forEach { (color, width) ->
            val w = size.width * width / total
            drawRect(color, Offset(x, top), Size(w, barHeight))
            x += w
        }
        // Marcador: um triângulo apontando para a régua
        val mx = size.width * position.value
        val path = Path().apply {
            moveTo(mx - 5.dp.toPx(), 0f)
            lineTo(mx + 5.dp.toPx(), 0f)
            lineTo(mx, top - 1.dp.toPx())
            close()
        }
        drawPath(path, marker)
    }
}

/**
 * Uma linha de campo: ícone, rótulo (com um [detail] menor embaixo), valor (ou "Não
 * informado") numa linha só e a setinha. Também no onboarding.
 */
@Composable
fun ProfileFieldRow(label: String, value: String?, icon: Int? = null, detail: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar ${label.lowercase()}", onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(label, detail, value ?: "não informado").joinToString(": ")
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(it), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            value ?: "Não informado",
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Surge com uma mola depois da transição da tela (nada fora do NavHost, como nas prévias). */
@Composable
private fun Modifier.popInAfterArrival(): Modifier {
    val animated = LocalNavAnimatedScope.current ?: return this
    return with(animated) {
        this@popInAfterArrival.animateEnterExit(
            enter = fadeIn(tween(durationMillis = 120, delayMillis = 380)) +
                scaleIn(tween(durationMillis = 260, delayMillis = 380, easing = FastOutSlowInEasing), initialScale = 0.3f),
            exit = ExitTransition.None
        )
    }
}
