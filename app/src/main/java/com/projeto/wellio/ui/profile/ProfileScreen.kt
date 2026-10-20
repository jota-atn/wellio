package com.projeto.wellio.ui.profile

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import com.projeto.wellio.ui.format.formatInt
import com.projeto.wellio.ui.format.formatKg
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
        onClearTarget = viewModel::clearTarget
    )
}

/** Campo do perfil sendo editado num sheet. */
private enum class ProfileField { Photo, Name, BirthDate, Sex, Height, Objective, Activity }

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
    onClearTarget: () -> Unit
) {
    val profile = state.profile
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<ProfileField?>(null) }
    var cropping by remember { mutableStateOf<Uri?>(null) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropping = uri
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
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "foto") {
                PhotoHeader(
                    state = state,
                    onPhotoClick = { if (state.photo == null) launchPicker() else editing = ProfileField.Photo },
                    onNameClick = { editing = ProfileField.Name }
                )
            }
            item(key = "ficha") {
                ProfileSheetCard(state, onUse = { kcal ->
                    onUseSuggestion(kcal)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar("Meta de kcal agora é ${formatInt(kcal)}")
                    }
                })
            }
            item(key = "campos") {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        FieldRow("Nascimento", profile.birthDate?.format(birthFormatter)) { editing = ProfileField.BirthDate }
                        FieldRow("Sexo", profile.sex?.label) { editing = ProfileField.Sex }
                        FieldRow("Altura", profile.heightCm?.let(::formatHeight)) { editing = ProfileField.Height }
                        FieldRow("Peso", state.weightKg?.let(::formatKg) ?: "Registrar", onClick = onOpenWeight)
                        FieldRow(
                            "Objetivo",
                            listOfNotNull(state.objective.label, state.weightGoal?.let { "alvo ${formatKg(it.targetKg)}" }).joinToString(" · ")
                        ) { editing = ProfileField.Objective }
                        FieldRow("Atividade", profile.activity?.label) { editing = ProfileField.Activity }
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
}

private val birthFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun formatHeight(cm: Int): String = String.format(Locale.forLanguageTag("pt-BR"), "%.2f m", cm / 100.0)

/** Foto grande com o selo de câmera e o nome logo abaixo. */
@Composable
private fun PhotoHeader(state: ProfileUiState, onPhotoClick: () -> Unit, onNameClick: () -> Unit) {
    val colors = WellioTheme.colors.base
    val name = state.profile.name
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            ProfileAvatar(
                state.photo,
                name,
                size = 136.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClickLabel = if (state.photo == null) "Escolher foto" else "Trocar foto", onClick = onPhotoClick)
            )
            // Selo da câmera sobre a borda da foto
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
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
        Spacer(Modifier.height(8.dp))
        Text(
            name.ifBlank { "Toque para pôr seu nome" },
            style = if (name.isBlank()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            fontWeight = if (name.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
            color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = "Editar nome", onClick = onNameClick)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Cor do ponto do IMC: azul abaixo, verde normal, âmbar sobrepeso, vermelho obesidade. */
@Composable
private fun bmiColor(range: BmiRange): Color = when (range) {
    BmiRange.Under -> WellioTheme.colors.water.accent
    BmiRange.Normal -> Color(0xFF6FCF7A)
    BmiRange.Over -> Color(0xFFF5B940)
    BmiRange.Obese -> Color(0xFFFF6B6B)
}

/** A ficha: idade, IMC e meta calórica sugerida, com o botão "Usar" (SPEC §9.1). */
@Composable
private fun ProfileSheetCard(state: ProfileUiState, onUse: (Int) -> Unit) {
    val colors = WellioTheme.colors.base
    val suggestion = state.suggestion
    Surface(shape = RoundedCornerShape(24.dp), color = colors.container, contentColor = colors.onContainer) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 8.dp)) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                SheetStat(
                    value = state.age?.toString() ?: "—",
                    caption = if (state.age != null) "anos" else "informe o nascimento",
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = colors.onContainer.copy(alpha = 0.2f))
                val bmi = state.bmi
                SheetStat(
                    value = bmi?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.1f", it) } ?: "—",
                    caption = state.bmiRange?.let { "IMC · ${it.label.lowercase()}" }
                        ?: if (state.profile.heightCm == null) "informe a altura" else "registre o peso",
                    dot = state.bmiRange?.let { bmiColor(it) },
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = colors.onContainer.copy(alpha = 0.2f))
                SheetStat(
                    value = (suggestion as? CalorieSuggestion.Ready)?.let { formatInt(it.kcal) } ?: "—",
                    caption = when (suggestion) {
                        is CalorieSuggestion.Ready -> if (suggestion.atFloor) "kcal · mínimo recomendado" else "kcal sugeridas"
                        is CalorieSuggestion.Missing -> "falta " + suggestion.fields.joinToString(", ") { it.label }
                        CalorieSuggestion.SexUndisclosed -> "a fórmula depende do sexo"
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            if (suggestion is CalorieSuggestion.Ready) {
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val current = state.foodGoal.daily
                    Text(
                        if (current == suggestion.kcal) "É a sua meta diária de kcal" else "Sua meta hoje: ${formatInt(current)} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onContainer.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    if (current != suggestion.kcal) {
                        Button(
                            onClick = { onUse(suggestion.kcal) },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
                        ) { Text("Usar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetStat(value: String, caption: String, modifier: Modifier = Modifier, dot: Color? = null) {
    Column(
        modifier.padding(horizontal = 6.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            dot?.let {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.size(10.dp).clip(CircleShape).background(it))
            }
        }
        Text(
            caption,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = LocalContentColor.current.copy(alpha = 0.8f)
        )
    }
}

/** Uma linha de campo: rótulo, valor (ou "Não informado") e a setinha. */
@Composable
private fun FieldRow(label: String, value: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Editar ${label.lowercase()}", onClick = onClick)
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: ${value ?: "não informado"}" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            value ?: "Não informado",
            style = MaterialTheme.typography.bodyLarge,
            color = if (value == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (value == null) FontWeight.Normal else FontWeight.Medium
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
