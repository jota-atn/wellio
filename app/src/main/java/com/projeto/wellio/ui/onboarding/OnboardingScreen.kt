package com.projeto.wellio.ui.onboarding

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projeto.wellio.R
import com.projeto.wellio.domain.model.ActivityLevel
import com.projeto.wellio.domain.model.ExerciseUnit
import com.projeto.wellio.domain.model.Sex
import com.projeto.wellio.domain.profile.CalorieSuggestion
import com.projeto.wellio.domain.profile.OnboardingDraft
import com.projeto.wellio.domain.profile.OnboardingStep
import com.projeto.wellio.domain.weight.DEFAULT_WEIGHT_KG
import com.projeto.wellio.ui.components.DayPickerDialog
import com.projeto.wellio.ui.exercise.Campfire
import com.projeto.wellio.ui.format.formatHeight
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
import com.projeto.wellio.ui.format.formatMl
import com.projeto.wellio.ui.icons.WellioIcons
import com.projeto.wellio.ui.profile.HeightSheet
import com.projeto.wellio.ui.profile.OptionSheet
import com.projeto.wellio.ui.profile.PhotoCropDialog
import com.projeto.wellio.ui.profile.ProfileAvatar
import com.projeto.wellio.ui.profile.ProfileFieldRow
import com.projeto.wellio.ui.profile.birthFormatter
import com.projeto.wellio.ui.theme.Navy
import com.projeto.wellio.ui.theme.NeedleRed
import com.projeto.wellio.ui.water.WaterDrop
import com.projeto.wellio.ui.weight.ScaleTape
import com.projeto.wellio.ui.weight.TargetSheet
import com.projeto.wellio.ui.weight.WeightPicker
import com.projeto.wellio.ui.weight.WeightPickerState
import java.io.File
import java.time.LocalDate
import kotlin.math.abs

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = viewModel(factory = OnboardingViewModel.Factory)) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val current = draft
    if (current == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    OnboardingScreen(
        draft = current,
        today = viewModel.today,
        photo = viewModel.photo(current.photoFile),
        onEdit = viewModel::edit,
        onPhotoCropped = viewModel::setPhoto,
        onSkipLifestyle = viewModel::resetLifestyle,
        onFinish = viewModel::finish
    )
}

/** Espaço no pé de cada etapa, debaixo das bolinhas e do botão. */
private val BottomBarSpace = 150.dp

/**
 * Onboarding (SPEC §5.0): uma etapa por vez, que entra deslizando. Boas-vindas e "Você" em
 * tela cheia colorida, com tudo no centro; Corpo, Ritmo e Metas com uma capa colorida no
 * topo e os campos embaixo, cabendo sem rolar. Continuar só com os obrigatórios; arrastar
 * para os lados volta e avança entre etapas já liberadas; Ritmo e Metas têm Pular.
 */
@Composable
fun OnboardingScreen(
    draft: OnboardingDraft,
    today: LocalDate,
    photo: File?,
    onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onPhotoCropped: (Bitmap) -> Unit,
    onSkipLifestyle: () -> Unit,
    onFinish: (saveGoals: Boolean) -> Unit
) {
    val steps = OnboardingStep.entries
    var current by rememberSaveable { mutableIntStateOf(0) }
    var unlocked by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[current]
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun goTo(index: Int) {
        // Trocar de etapa fecha o teclado
        focusManager.clearFocus()
        keyboard?.hide()
        unlocked = maxOf(unlocked, index)
        current = index
    }

    fun finish(saveGoals: Boolean) {
        val missing = draft.firstIncomplete
        if (missing != null) goTo(missing.ordinal) else onFinish(saveGoals)
    }

    fun next() {
        if (step == OnboardingStep.Goals) finish(saveGoals = true) else if (draft.canContinue(step)) goTo(current + 1)
    }

    // Chegando nas metas, quem é novo recebe as sugestões
    LaunchedEffect(step) {
        if (step == OnboardingStep.Goals) onEdit { it.withSuggestedGoals(today) }
    }
    BackHandler(enabled = current > 0) { goTo(current - 1) }

    val look = lookOf(step)
    val buttonColor by animateColorAsState(look.button, label = "botão da etapa")
    val onButtonColor by animateColorAsState(look.onButton, label = "texto do botão da etapa")
    // O gesto lê sempre os valores de agora (o pointerInput guarda os da primeira composição)
    val latestCurrent by rememberUpdatedState(current)
    val latestUnlocked by rememberUpdatedState(unlocked)
    val latestGoTo by rememberUpdatedState(::goTo)

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .pointerInput(Unit) {
                // Arrastar para os lados: volta, ou avança até a última etapa liberada
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { dragged = 0f },
                    onHorizontalDrag = { _, amount -> dragged += amount },
                    onDragEnd = {
                        if (abs(dragged) > 80.dp.toPx()) {
                            if (dragged < 0 && latestCurrent < latestUnlocked) latestGoTo(latestCurrent + 1)
                            if (dragged > 0 && latestCurrent > 0) latestGoTo(latestCurrent - 1)
                        }
                    }
                )
            }
    ) {
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut())
            },
            label = "etapa"
        ) { index ->
            when (steps[index]) {
                OnboardingStep.Welcome -> WelcomePage(draft.returning)
                OnboardingStep.You -> YouPage(draft, photo, onEdit, onPhotoCropped, onDone = ::next)
                OnboardingStep.Body -> BodyPage(draft, today, onEdit)
                OnboardingStep.Lifestyle -> LifestylePage(draft, onEdit)
                OnboardingStep.Goals -> GoalsPage(draft, today, onEdit)
            }
        }

        // Voltar e Pular ficam fixos no topo, sobre a cor
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (current > 0) {
                IconButton(onClick = { goTo(current - 1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
            }
            Spacer(Modifier.weight(1f))
            if (step.skippable) {
                TextButton(onClick = {
                    if (step == OnboardingStep.Lifestyle) {
                        onSkipLifestyle()
                        goTo(OnboardingStep.Goals.ordinal)
                    } else {
                        finish(saveGoals = false)
                    }
                }) { Text("Pular", color = Color.White) }
            }
        }

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StepDots(count = steps.size, current = current, color = buttonColor)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = ::next,
                enabled = draft.canContinue(step),
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = onButtonColor),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    when (step) {
                        OnboardingStep.Welcome -> "Começar"
                        OnboardingStep.Goals -> "Concluir"
                        else -> "Continuar"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
    }
}

/** Bolinhas de progresso; a atual se alonga. */
@Composable
private fun StepDots(count: Int, current: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = "Etapa ${current + 1} de $count" }
    ) {
        repeat(count) { index ->
            val width by animateDpAsState(if (index == current) 24.dp else 8.dp, label = "bolinha")
            val dot by animateColorAsState(
                if (index <= current) color else Color.White.copy(alpha = 0.3f),
                label = "cor da bolinha"
            )
            Box(Modifier.size(width, 8.dp).clip(CircleShape).background(dot))
        }
    }
}

/** Cores de cada etapa (SPEC §5.0): o fundo (ou a capa) e o botão. */
private class StepLook(val color: Brush, val button: Color, val onButton: Color)

private val NavyBrush = Brush.verticalGradient(listOf(Navy, Color(0xFF2C3E80)))

private fun lookOf(step: OnboardingStep): StepLook = when (step) {
    OnboardingStep.Welcome -> StepLook(NavyBrush, Color(0xFF8C9EFF), Navy)
    OnboardingStep.You -> StepLook(Brush.verticalGradient(listOf(Color(0xFF1E4FD8), Color(0xFF3B82F6))), Color.White, Color(0xFF1E4FD8))
    OnboardingStep.Body -> StepLook(NavyBrush, Color(0xFF8C9EFF), Navy)
    OnboardingStep.Lifestyle -> StepLook(Brush.verticalGradient(listOf(Color(0xFFB23C14), Color(0xFFFF8A3D))), Color(0xFFFF8A62), Color(0xFF3A1206))
    OnboardingStep.Goals -> StepLook(
        Brush.linearGradient(listOf(Color(0xFF1E4FD8), Color(0xFF2E7D32), Color(0xFFC4471B))),
        Color(0xFF6FCF7A),
        Color(0xFF0B2410)
    )
}

/** Etapa de abertura (boas-vindas e "Você"): a tela inteira na cor da etapa, tudo no centro. */
@Composable
private fun CenteredStep(step: OnboardingStep, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(lookOf(step).color)) {
        Column(
            Modifier
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(top = 56.dp, bottom = BottomBarSpace, start = 32.dp, end = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun CenteredTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White)
    Spacer(Modifier.height(10.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.88f))
}

/**
 * Etapa de formulário (Corpo, Ritmo, Metas): capa colorida e baixa no topo, com a
 * ilustração e o título em branco; os campos embaixo. Rola só se não couber (fonte grande).
 */
@Composable
private fun CoverStep(
    step: OnboardingStep,
    title: String,
    subtitle: String,
    illustration: @Composable () -> Unit,
    fields: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(lookOf(step).color)
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            illustration()
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = Color.White.copy(alpha = 0.88f))
        }
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), content = fields)
        Spacer(Modifier.height(BottomBarSpace))
    }
}

/** Boas-vindas: a gota, a fogueira e a balança juntas, no centro. */
@Composable
private fun WelcomePage(returning: Boolean) {
    CenteredStep(OnboardingStep.Welcome) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            WaterDrop(totalMl = 650, goalMl = 1000, additions = 0, modifier = Modifier.size(width = 88.dp, height = 110.dp))
            Campfire(total = 3, goal = 5, additions = 0, modifier = Modifier.size(width = 100.dp, height = 120.dp))
            MiniScale(Modifier.size(88.dp))
        }
        Spacer(Modifier.height(40.dp))
        CenteredTitle(
            if (returning) "Agora o Wellio sabe quem você é" else "Bem-vindo ao Wellio",
            if (returning) "Complete seu perfil em um minuto e as metas passam a ser do seu tamanho."
            else "Água, comida, treino e peso num lugar só, com metas do seu tamanho. Bora montar o seu em um minuto."
        )
    }
}

/** A balança em miniatura: o visor com a agulha numa base clara. */
@Composable
private fun MiniScale(modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.16f)).padding(12.dp)) {
        ScaleTape(
            valueKg = 70f,
            tick = Color.White,
            needle = NeedleRed,
            compact = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.28f))
        )
    }
}

/** Você: a foto (com a inicial aparecendo enquanto digita) e o nome, no centro. */
@Composable
private fun YouPage(
    draft: OnboardingDraft,
    photo: File?,
    onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onPhotoCropped: (Bitmap) -> Unit,
    onDone: () -> Unit
) {
    var cropping by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropping = uri
    }
    val pick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    CenteredStep(OnboardingStep.You) {
        Box {
            ProfileAvatar(
                photo,
                draft.name,
                size = 136.dp,
                modifier = Modifier
                    .border(4.dp, Color.White, CircleShape)
                    .clip(CircleShape)
                    .clickable(onClickLabel = "Escolher foto", onClick = pick)
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable(onClickLabel = "Escolher foto", onClick = pick),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_profile_camera), contentDescription = null, tint = Color(0xFF1E4FD8), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        CenteredTitle("Como te chamamos?", "A foto é opcional; dá para trocar depois no perfil.")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = draft.name,
            onValueChange = { text -> onEdit { it.copy(name = text.take(40)) } },
            placeholder = { Text("Seu nome", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.White.copy(alpha = 0.6f),
                unfocusedPlaceholderColor = Color.White.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }

    cropping?.let { uri ->
        PhotoCropDialog(uri, onDismiss = { cropping = null }, onCropped = {
            onPhotoCropped(it)
            cropping = null
        })
    }
}

private enum class BodyField { Weight, BirthDate, Sex, Height }

/** Corpo: visor compacto da balança (tocar muda o peso) e nascimento, sexo e altura. */
@Composable
private fun BodyPage(draft: OnboardingDraft, today: LocalDate, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    var editing by remember { mutableStateOf<BodyField?>(null) }

    CoverStep(
        OnboardingStep.Body,
        "Seu corpo",
        "Só o peso é obrigatório; o resto deixa as sugestões mais certeiras.",
        illustration = {
            Icon(painterResource(R.drawable.ic_profile_ruler), contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }
    ) {
        CompactScale(draft.weightKg, onClick = { editing = BodyField.Weight })
        Spacer(Modifier.height(12.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(vertical = 4.dp)) {
                ProfileFieldRow("Nascimento", draft.birthDate?.format(birthFormatter), R.drawable.ic_profile_cake) { editing = BodyField.BirthDate }
                ProfileFieldRow("Sexo", draft.sex?.label, R.drawable.ic_profile_person) { editing = BodyField.Sex }
                ProfileFieldRow("Altura", draft.heightCm?.let(::formatHeight), R.drawable.ic_profile_ruler) { editing = BodyField.Height }
            }
        }
    }

    when (editing) {
        BodyField.Weight -> WeightOnlySheet(
            weightKg = draft.weightKg ?: DEFAULT_WEIGHT_KG,
            onDismiss = { editing = null },
            onSave = { kg ->
                onEdit { it.copy(weightKg = kg) }
                editing = null
            }
        )
        BodyField.BirthDate -> DayPickerDialog(
            day = draft.birthDate ?: today.minusYears(25),
            today = today,
            onDismiss = { editing = null },
            onPick = { day ->
                onEdit { it.copy(birthDate = day) }
                editing = null
            }
        )
        BodyField.Sex -> OptionSheet(
            title = "Sexo",
            options = Sex.entries,
            selected = draft.sex,
            label = { it.label },
            onDismiss = { editing = null },
            onPick = { sex ->
                onEdit { it.copy(sex = sex) }
                editing = null
            },
            onClear = draft.sex?.let {
                {
                    onEdit { it.copy(sex = null) }
                    editing = null
                }
            }
        )
        BodyField.Height -> HeightSheet(
            heightCm = draft.heightCm,
            onDismiss = { editing = null },
            onSave = { cm ->
                onEdit { it.copy(heightCm = cm) }
                editing = null
            },
            onClear = draft.heightCm?.let {
                {
                    onEdit { it.copy(heightCm = null) }
                    editing = null
                }
            }
        )
        null -> Unit
    }
}

/** O peso num visor de balança compacto: a fita com a agulha e o número grande; tocar muda. */
@Composable
private fun CompactScale(weightKg: Double?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = Navy,
        contentColor = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = weightKg?.let { "Peso: ${formatKg(it)}. Tocar para mudar" } ?: "Informar peso" }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ScaleTape(
                valueKg = (weightKg ?: DEFAULT_WEIGHT_KG).toFloat(),
                tick = Color.White,
                needle = NeedleRed,
                compact = true,
                modifier = Modifier
                    .width(120.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.28f))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(weightKg?.let(::formatKg) ?: "Seu peso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    if (weightKg == null) "Toque para informar" else "Toque para mudar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
            if (weightKg == null) Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}

/** Só o peso, com o mesmo seletor da tela de Peso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightOnlySheet(weightKg: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    val picker = remember { WeightPickerState(weightKg) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text("Peso", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
            WeightPicker(picker)
            Row(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { picker.weight?.let(onSave) },
                    enabled = picker.weight != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy, contentColor = Color.White)
                ) { Text("Salvar") }
            }
        }
    }
}

/** Ritmo: a fogueira cresce com o nível de atividade (em pílulas); abaixo, o peso alvo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifestylePage(draft: OnboardingDraft, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    var editingTarget by remember { mutableStateOf(false) }
    // Subir de nível aviva a fogueira
    var raises by remember { mutableIntStateOf(0) }
    val level = draft.activity?.ordinal?.plus(1) ?: 0

    CoverStep(
        OnboardingStep.Lifestyle,
        "Seu ritmo",
        "Quanto você se mexe entra na conta da meta de calorias.",
        illustration = { Campfire(total = level, goal = ActivityLevel.entries.size, additions = raises, modifier = Modifier.size(width = 90.dp, height = 100.dp)) }
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ActivityLevel.entries.forEach { option ->
                val selected = option == draft.activity
                FilterChip(
                    selected = selected,
                    onClick = {
                        if (option.ordinal + 1 > level) raises++
                        onEdit { it.copy(activity = option) }
                    },
                    label = { Text(option.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF8A62),
                        selectedLabelColor = Color(0xFF3A1206)
                    )
                )
            }
        }
        Text(
            draft.activity?.hint ?: "Escolha o que mais parece com a sua semana",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(4.dp))
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            ProfileFieldRow(
                "Peso alvo",
                draft.targetKg?.let(::formatKg),
                R.drawable.ic_profile_target,
                detail = draft.targetKg?.let { draft.objective.label }
            ) { editingTarget = true }
        }
    }

    if (editingTarget) {
        val current = draft.weightKg ?: DEFAULT_WEIGHT_KG
        TargetSheet(
            targetKg = draft.targetKg,
            currentKg = current,
            startKg = current,
            onDismiss = { editingTarget = false },
            onSave = { kg ->
                onEdit { it.copy(targetKg = kg) }
                editingTarget = false
            },
            onRemove = draft.targetKg?.let {
                {
                    onEdit { it.copy(targetKg = null) }
                    editingTarget = false
                }
            }
        )
    }
}

/** Metas: água, kcal e exercício com −/+, compactos. */
@Composable
private fun GoalsPage(draft: OnboardingDraft, today: LocalDate, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    val kcal = (draft.suggestion(today) as? CalorieSuggestion.Ready)?.kcal
    val minutes = draft.exerciseUnit == ExerciseUnit.Minutes
    CoverStep(
        OnboardingStep.Goals,
        "Suas metas diárias",
        if (draft.returning) "Estas são as suas metas de agora. Ajuste se quiser." else "Já vêm sugeridas pelo que você contou. Ajuste se quiser.",
        illustration = {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Bottom) {
                WaterDrop(totalMl = 1, goalMl = 1, additions = 0, modifier = Modifier.size(width = 44.dp, height = 54.dp))
                Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_module_food), contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                }
                Campfire(total = 1, goal = 1, additions = 0, modifier = Modifier.size(width = 48.dp, height = 56.dp))
            }
        }
    ) {
        GoalStepper(
            title = "Água",
            value = formatMl(draft.waterMl),
            suggestion = draft.waterSuggestion?.takeIf { it != draft.waterMl }?.let { s -> formatMl(s) to { onEdit { it.copy(waterMl = s) } } },
            onMinus = { onEdit { it.copy(waterMl = (it.waterMl - 100).coerceAtLeast(500)) } },
            onPlus = { onEdit { it.copy(waterMl = (it.waterMl + 100).coerceAtMost(8000)) } }
        )
        Spacer(Modifier.height(10.dp))
        GoalStepper(
            title = "Calorias consumidas",
            value = "${formatInt(draft.kcal)} kcal",
            suggestion = kcal?.takeIf { it != draft.kcal }?.let { s -> "${formatInt(s)} kcal" to { onEdit { it.copy(kcal = s) } } },
            onMinus = { onEdit { it.copy(kcal = (it.kcal - 50).coerceAtLeast(1000)) } },
            onPlus = { onEdit { it.copy(kcal = (it.kcal + 50).coerceAtMost(6000)) } }
        )
        Spacer(Modifier.height(10.dp))
        GoalStepper(
            title = "Exercício",
            value = if (minutes) "${draft.exercise} min" else "${formatInt(draft.exercise)} kcal",
            suggestion = null,
            onMinus = { onEdit { it.copy(exercise = (it.exercise - if (minutes) 5 else 50).coerceAtLeast(if (minutes) 5 else 50)) } },
            onPlus = { onEdit { it.copy(exercise = (it.exercise + if (minutes) 5 else 50).coerceAtMost(if (minutes) 300 else 5000)) } },
            unitToggle = {
                // kcal ou min, compacto ao lado do título
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExerciseUnit.entries.forEach { unit ->
                        val selected = draft.exerciseUnit == unit
                        Surface(
                            onClick = { onEdit { if (it.exerciseUnit == unit) it else it.copy(exerciseUnit = unit, exercise = unit.defaultDaily) } },
                            shape = RoundedCornerShape(50),
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.semantics { this.selected = selected }
                        ) {
                            Text(unit.label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        )
    }
}

/** Uma meta com −/+ e, se houver, a sugestão para usar com um toque, tudo compacto. */
@Composable
private fun GoalStepper(
    title: String,
    value: String,
    suggestion: Pair<String, () -> Unit>?,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    unitToggle: @Composable () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    unitToggle()
                }
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                suggestion?.let { (text, apply) ->
                    Text(
                        "Usar a sugestão: $text",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = apply).padding(vertical = 2.dp)
                    )
                }
            }
            FilledTonalIconButton(onClick = onMinus) { Icon(WellioIcons.Minus, contentDescription = "Diminuir $title") }
            FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Aumentar $title") }
        }
    }
}
