package com.projeto.wellio.domain.model

import java.time.LocalDate

/** Sexo para a fórmula da meta calórica (SPEC §9.1); [Undisclosed] não tem sugestão. */
enum class Sex(val label: String) {
    Male("Masculino"),
    Female("Feminino"),
    Undisclosed("Prefiro não informar")
}

/** Nível de atividade e o fator que multiplica o metabolismo basal (SPEC §9.1). */
enum class ActivityLevel(val label: String, val hint: String, val factor: Double) {
    Sedentary("Sedentário", "Pouco ou nenhum exercício", 1.2),
    Light("Levemente ativo", "Exercício leve 1 a 3 dias por semana", 1.375),
    Moderate("Moderado", "Exercício 3 a 5 dias por semana", 1.55),
    VeryActive("Muito ativo", "Exercício intenso 6 a 7 dias por semana", 1.725),
    Athlete("Atleta", "Treino pesado todo dia ou trabalho físico", 1.9)
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
    val onboardingDone: Boolean = false,
    /** Capa do perfil: uma cor do app (nome) ou uma foto (arquivo); nenhuma = azul-marinho. */
    val coverColor: String? = null,
    val coverPhoto: String? = null
)
