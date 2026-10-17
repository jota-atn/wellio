package com.projeto.wellio.domain.model

import java.time.LocalDate

/** Sexo para a fórmula da meta calórica (SPEC §9.1); [Undisclosed] não tem sugestão. */
enum class Sex(val label: String) {
    Male("Masculino"),
    Female("Feminino"),
    Undisclosed("Prefiro não informar")
}

/** Nível de atividade e o fator que multiplica o metabolismo basal (SPEC §9.1). */
enum class ActivityLevel(val label: String, val factor: Double) {
    Sedentary("Sedentário", 1.2),
    Light("Levemente ativo", 1.375),
    Moderate("Moderado", 1.55),
    VeryActive("Muito ativo", 1.725),
    Athlete("Atleta", 1.9)
}

/**
 * Perfil do usuário (SPEC §9.1). O peso fica nos registros de peso e o objetivo sai do
 * peso alvo; aqui só o que é do perfil. [photoFile] é o nome do recorte salvo no app.
 */
data class Profile(
    val name: String = "",
    val photoFile: String? = null,
    val birthDate: LocalDate? = null,
    val sex: Sex? = null,
    val heightCm: Int? = null,
    val activity: ActivityLevel? = null,
    val onboardingDone: Boolean = false
)
