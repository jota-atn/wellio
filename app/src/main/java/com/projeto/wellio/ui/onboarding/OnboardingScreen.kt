package com.projeto.wellio.ui.onboarding

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
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
import com.projeto.wellio.ui.theme.NeedleRed
import com.projeto.wellio.ui.theme.WellioTheme
import com.projeto.wellio.ui.water.WaterDrop
import com.projeto.wellio.ui.weight.BathroomScale
import com.projeto.wellio.ui.weight.ScaleTape
import com.projeto.wellio.ui.weight.TargetSheet
import com.projeto.wellio.ui.weight.WeightPicker
import com.projeto.wellio.ui.weight.WeightPickerState
import kotlinx.coroutines.launch
import java.time.LocalDate

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

/**
 * Onboarding (SPEC §5.0): uma etapa por página, com a ilustração no topo e os campos
 * abaixo. Continuar só com os obrigatórios; deslizar volta e avança entre etapas já
 * liberadas; estilo de vida e metas têm Pular.
 */
@Composable
fun OnboardingScreen(
    draft: OnboardingDraft,
    today: LocalDate,
    photo: java.io.File?,
    onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onPhotoCropped: (android.graphics.Bitmap) -> Unit,
    onSkipLifestyle: () -> Unit,
    onFinish: (saveGoals: Boolean) -> Unit
) {
    val steps = OnboardingStep.entries
    var unlocked by rememberSaveable { mutableIntStateOf(0) }
    val pager = rememberPagerState { unlocked + 1 }
    val scope = rememberCoroutineScope()
    val step = steps[pager.currentPage]
    val colors = WellioTheme.colors.base

    fun goTo(index: Int) {
        unlocked = maxOf(unlocked, index)
        scope.launch { pager.animateScrollToPage(index) }
    }

    fun finish(saveGoals: Boolean) {
        val missing = draft.firstIncomplete
        if (missing != null) goTo(missing.ordinal) else onFinish(saveGoals)
    }

    // Chegando nas metas, quem é novo recebe as sugestões
    LaunchedEffect(step) {
        if (step == OnboardingStep.Goals) onEdit { it.withSuggestedGoals(today) }
    }
    BackHandler(enabled = pager.currentPage > 0) { goTo(pager.currentPage - 1) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (pager.currentPage > 0) {
                IconButton(onClick = { goTo(pager.currentPage - 1) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                }) { Text("Pular") }
            }
        }

        HorizontalPager(pager, modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (steps[page]) {
                    OnboardingStep.Welcome -> WelcomePage(draft.returning)
                    OnboardingStep.You -> YouPage(draft, photo, onEdit, onPhotoCropped)
                    OnboardingStep.Body -> BodyPage(draft, today, onEdit)
                    OnboardingStep.Lifestyle -> LifestylePage(draft, onEdit)
                    OnboardingStep.Goals -> GoalsPage(draft, today, onEdit)
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            StepDots(count = steps.size, current = pager.currentPage)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (step == OnboardingStep.Goals) finish(saveGoals = true) else goTo(pager.currentPage + 1)
                },
                enabled = draft.canContinue(step),
                colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer),
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
private fun StepDots(count: Int, current: Int) {
    val colors = WellioTheme.colors.base
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.semantics { contentDescription = "Etapa ${current + 1} de $count" }
    ) {
        repeat(count) { index ->
            val width by animateDpAsState(if (index == current) 24.dp else 8.dp, label = "bolinha")
            val color by animateColorAsState(
                if (index <= current) colors.accent else MaterialTheme.colorScheme.outlineVariant,
                label = "cor da bolinha"
            )
            Box(Modifier.size(width, 8.dp).clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
}

/** Boas-vindas: a gota, a fogueira e a balança juntas. */
@Composable
private fun WelcomePage(returning: Boolean) {
    Spacer(Modifier.height(32.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
        WaterDrop(totalMl = 650, goalMl = 1000, additions = 0, modifier = Modifier.size(width = 88.dp, height = 110.dp))
        Campfire(total = 3, goal = 5, additions = 0, modifier = Modifier.size(width = 100.dp, height = 120.dp))
        MiniScale(Modifier.size(88.dp))
    }
    Spacer(Modifier.height(40.dp))
    if (returning) {
        StepTitle(
            "Agora o Wellio sabe quem você é",
            "Complete seu perfil em um minuto e as metas passam a ser do seu tamanho."
        )
    } else {
        StepTitle(
            "Bem-vindo ao Wellio",
            "Água, comida, treino e peso num lugar só, com metas do seu tamanho. Bora montar o seu em um minuto."
        )
    }
}

/** A balança em miniatura: base azul-marinho com o visor e a agulha. */
@Composable
private fun MiniScale(modifier: Modifier = Modifier) {
    val colors = WellioTheme.colors.base
    Box(modifier.clip(RoundedCornerShape(22.dp)).background(colors.container).padding(12.dp)) {
        ScaleTape(
            valueKg = 70f,
            tick = colors.onContainer,
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

/** Você: a própria foto é a ilustração, com a inicial aparecendo enquanto digita. */
@Composable
private fun YouPage(
    draft: OnboardingDraft,
    photo: java.io.File?,
    onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit,
    onPhotoCropped: (android.graphics.Bitmap) -> Unit
) {
    val colors = WellioTheme.colors.base
    var cropping by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropping = uri
    }
    val pick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Spacer(Modifier.height(16.dp))
    Box {
        ProfileAvatar(
            photo,
            draft.name,
            size = 160.dp,
            modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Escolher foto", onClick = pick)
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable(onClickLabel = "Escolher foto", onClick = pick),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_profile_camera), contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(22.dp))
        }
    }
    Spacer(Modifier.height(24.dp))
    StepTitle("Como te chamamos?", "A foto é opcional; dá para trocar depois no perfil.")
    OutlinedTextField(
        value = draft.name,
        onValueChange = { text -> onEdit { it.copy(name = text.take(40)) } },
        label = { Text("Seu nome") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )

    cropping?.let { uri ->
        PhotoCropDialog(uri, onDismiss = { cropping = null }, onCropped = {
            onPhotoCropped(it)
            cropping = null
        })
    }
}

private enum class BodyField { Weight, BirthDate, Sex, Height }

/** Corpo: a balança mostra o peso e abre o seletor; nascimento, sexo e altura abaixo. */
@Composable
private fun BodyPage(draft: OnboardingDraft, today: LocalDate, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    val colors = WellioTheme.colors.base
    var editing by remember { mutableStateOf<BodyField?>(null) }

    StepTitle("Seu corpo", "Só o peso é obrigatório; o resto deixa as sugestões mais certeiras.")
    BathroomScale(
        currentKg = draft.weightKg,
        subtitle = if (draft.weightKg == null) "Quanto você pesa hoje?" else "Peso de hoje",
        detail = null,
        goalReached = false,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            onClick = { editing = BodyField.Weight },
            shape = RoundedCornerShape(50),
            color = colors.onContainer.copy(alpha = 0.1f),
            contentColor = colors.onContainer
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (draft.weightKg == null) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (draft.weightKg == null) "Informar peso" else "Mudar peso", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 8.dp)) {
            ProfileFieldRow("Nascimento", draft.birthDate?.format(birthFormatter)) { editing = BodyField.BirthDate }
            ProfileFieldRow("Sexo", draft.sex?.label) { editing = BodyField.Sex }
            ProfileFieldRow("Altura", draft.heightCm?.let(::formatHeight)) { editing = BodyField.Height }
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

/** Só o peso, com o mesmo seletor da tela de Peso. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightOnlySheet(weightKg: Double, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    val colors = WellioTheme.colors.base
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
                    colors = ButtonDefaults.buttonColors(containerColor = colors.container, contentColor = colors.onContainer)
                ) { Text("Salvar") }
            }
        }
    }
}

/** Estilo de vida: a fogueira cresce com o nível de atividade; abaixo, o peso alvo. */
@Composable
private fun LifestylePage(draft: OnboardingDraft, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    var editingTarget by remember { mutableStateOf(false) }
    // Subir de nível aviva a fogueira
    var raises by remember { mutableIntStateOf(0) }
    val level = draft.activity?.ordinal?.plus(1) ?: 0

    Campfire(total = level, goal = ActivityLevel.entries.size, additions = raises, modifier = Modifier.size(width = 140.dp, height = 160.dp))
    Spacer(Modifier.height(16.dp))
    StepTitle("Seu ritmo", "Quanto você se mexe entra na conta da meta de calorias.")
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.selectableGroup().padding(vertical = 8.dp)) {
            ActivityLevel.entries.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = option == draft.activity, onClick = {
                            if (option.ordinal + 1 > level) raises++
                            onEdit { it.copy(activity = option) }
                        })
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = option == draft.activity, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(option.label, style = MaterialTheme.typography.bodyLarge)
                        Text(option.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        ProfileFieldRow(
            "Peso alvo",
            draft.targetKg?.let { "${formatKg(it)} · ${draft.objective.label.lowercase()}" }
        ) { editingTarget = true }
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

/** Metas: água, kcal e exercício, cada uma com o seu elemento pequeno e −/+. */
@Composable
private fun GoalsPage(draft: OnboardingDraft, today: LocalDate, onEdit: ((OnboardingDraft) -> OnboardingDraft) -> Unit) {
    val food = WellioTheme.colors.food
    StepTitle(
        "Suas metas diárias",
        if (draft.returning) "Estas são as suas metas de agora. Ajuste se quiser." else "Já vêm sugeridas pelo que você contou. Ajuste se quiser."
    )
    GoalStepper(
        title = "Água",
        value = formatMl(draft.waterMl),
        suggestion = draft.waterSuggestion?.takeIf { it != draft.waterMl }?.let { s -> formatMl(s) to { onEdit { it.copy(waterMl = s) } } },
        onMinus = { onEdit { it.copy(waterMl = (it.waterMl - 100).coerceAtLeast(500)) } },
        onPlus = { onEdit { it.copy(waterMl = (it.waterMl + 100).coerceAtMost(8000)) } },
        illustration = { WaterDrop(totalMl = 1, goalMl = 1, additions = 0, modifier = Modifier.size(width = 44.dp, height = 56.dp)) }
    )
    Spacer(Modifier.height(12.dp))
    val kcal = (draft.suggestion(today) as? CalorieSuggestion.Ready)?.kcal
    GoalStepper(
        title = "Calorias consumidas",
        value = "${formatInt(draft.kcal)} kcal",
        suggestion = kcal?.takeIf { it != draft.kcal }?.let { s -> "${formatInt(s)} kcal" to { onEdit { it.copy(kcal = s) } } },
        onMinus = { onEdit { it.copy(kcal = (it.kcal - 50).coerceAtLeast(1000)) } },
        onPlus = { onEdit { it.copy(kcal = (it.kcal + 50).coerceAtMost(6000)) } },
        illustration = {
            Box(Modifier.size(52.dp).clip(CircleShape).background(food.container), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_module_food), contentDescription = null, tint = food.accent, modifier = Modifier.size(28.dp))
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    val minutes = draft.exerciseUnit == ExerciseUnit.Minutes
    GoalStepper(
        title = "Exercício",
        value = if (minutes) "${draft.exercise} min" else "${formatInt(draft.exercise)} kcal",
        suggestion = null,
        onMinus = { onEdit { it.copy(exercise = (it.exercise - if (minutes) 5 else 50).coerceAtLeast(if (minutes) 5 else 50)) } },
        onPlus = { onEdit { it.copy(exercise = (it.exercise + if (minutes) 5 else 50).coerceAtMost(if (minutes) 300 else 5000)) } },
        illustration = { Campfire(total = 1, goal = 1, additions = 0, modifier = Modifier.size(width = 48.dp, height = 56.dp)) }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ExerciseUnit.entries.forEach { unit ->
                FilterChip(
                    selected = draft.exerciseUnit == unit,
                    onClick = {
                        onEdit { if (it.exerciseUnit == unit) it else it.copy(exerciseUnit = unit, exercise = unit.defaultDaily) }
                    },
                    label = { Text(if (unit == ExerciseUnit.Minutes) "Minutos ativos" else "kcal queimadas") }
                )
            }
        }
    }
}

@Composable
private fun GoalStepper(
    title: String,
    value: String,
    suggestion: Pair<String, () -> Unit>?,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    illustration: @Composable () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) { illustration() }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                FilledTonalIconButton(onClick = onMinus) { Icon(WellioIcons.Minus, contentDescription = "Diminuir $title") }
                Spacer(Modifier.width(4.dp))
                FilledTonalIconButton(onClick = onPlus) { Icon(Icons.Default.Add, contentDescription = "Aumentar $title") }
            }
            suggestion?.let { (text, apply) ->
                TextButton(onClick = apply, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Usar a sugestão: $text")
                }
            }
            extra()
        }
    }
}
