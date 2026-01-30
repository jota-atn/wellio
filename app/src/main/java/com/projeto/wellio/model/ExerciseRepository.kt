package com.projeto.wellio.model

object ExerciseRepository {
    val list = listOf(
        ExerciseBlueprint("c1", "Caminhada (Passeio)", "Cardio", 2.5, "🚶"),
        ExerciseBlueprint("c2", "Caminhada Rápida", "Cardio", 4.0, "🚶‍♀️"),
        ExerciseBlueprint("c3", "Corrida (Trote/Leve)", "Cardio", 7.0, "🏃"),
        ExerciseBlueprint("c4", "Corrida (Moderada 10km/h)", "Cardio", 10.0, "🏃‍♂️"),
        ExerciseBlueprint("c5", "Corrida (Tiro/Sprint)", "Cardio", 14.0, "⚡"),
        ExerciseBlueprint("c6", "Bicicleta (Passeio)", "Cardio", 4.0, "🚲"),
        ExerciseBlueprint("c7", "Bicicleta (Intenso/Spinning)", "Cardio", 8.5, "🚴"),
        ExerciseBlueprint("c8", "Elíptico / Transport", "Cardio", 5.0, "🏃"),
        ExerciseBlueprint("c9", "Pular Corda (Moderado)", "Cardio", 10.0, "🪢"),
        ExerciseBlueprint("c10", "Remo (Máquina)", "Cardio", 7.0, "🚣"),
        ExerciseBlueprint("c11", "Subir Escadas", "Cardio", 9.0, "🪜"),
        ExerciseBlueprint("c12", "Polichinelos", "Cardio", 8.0, "🙆"),

        ExerciseBlueprint("m1", "Musculação (Tradicional)", "Musculação", 3.5, "🏋️"),
        ExerciseBlueprint("m2", "Musculação (Intensa/Power)", "Musculação", 6.0, "😤"),
        ExerciseBlueprint("m3", "CrossFit (WOD)", "Musculação", 10.0, "📦"),
        ExerciseBlueprint("m4", "Calistenia (Peso do corpo)", "Musculação", 4.5, "🤸"),
        ExerciseBlueprint("m5", "Treino Funcional", "Musculação", 8.0, "🔥"),
        ExerciseBlueprint("m6", "Abdominais / Core", "Musculação", 3.0, "🍫"),
        ExerciseBlueprint("m7", "Levantamento de Peso Olímpico", "Musculação", 6.0, "🏋️‍♂️"),

        ExerciseBlueprint("l1", "Boxe (Saco de Pancada)", "Lutas", 6.0, "🥊"),
        ExerciseBlueprint("l2", "Boxe (Sparring/Luta)", "Lutas", 12.0, "🥊"),
        ExerciseBlueprint("l3", "Jiu-Jitsu / Judo", "Lutas", 10.0, "🥋"),
        ExerciseBlueprint("l4", "Muay Thai / Kickboxing", "Lutas", 10.0, "🦵"),
        ExerciseBlueprint("l5", "Capoeira", "Lutas", 6.0, "🤸‍♂️"),
        ExerciseBlueprint("l6", "MMA / Vale-tudo", "Lutas", 12.0, "🤼"),
        ExerciseBlueprint("l7", "Karatê / Taekwondo", "Lutas", 10.0, "🥋"),

        ExerciseBlueprint("e1", "Futebol (Lazer/Pelada)", "Esportes", 7.0, "⚽"),
        ExerciseBlueprint("e2", "Futebol (Competitivo)", "Esportes", 10.0, "🏃⚽"),
        ExerciseBlueprint("e3", "Futsal / Salão", "Esportes", 8.0, "👟"),
        ExerciseBlueprint("e4", "Basquete (Jogo)", "Esportes", 8.0, "🏀"),
        ExerciseBlueprint("e5", "Basquete (Arremessos)", "Esportes", 4.5, "⛹️"),
        ExerciseBlueprint("e6", "Vôlei de Quadra", "Esportes", 4.0, "🏐"),
        ExerciseBlueprint("e7", "Vôlei de Praia / Futevôlei", "Esportes", 8.0, "🏖️"),
        ExerciseBlueprint("e8", "Tênis (Simples)", "Esportes", 7.0, "🎾"),
        ExerciseBlueprint("e9", "Beach Tennis", "Esportes", 6.0, "🏖️🎾"),
        ExerciseBlueprint("e10", "Tênis de Mesa / Ping Pong", "Esportes", 4.0, "🏓"),
        ExerciseBlueprint("e11", "Handebol", "Esportes", 12.0, "🤾"),

        ExerciseBlueprint("a1", "Natação (Lazer)", "Aquáticos", 6.0, "🏊"),
        ExerciseBlueprint("a2", "Natação (Crawl/Intenso)", "Aquáticos", 10.0, "🏊‍♂️"),
        ExerciseBlueprint("a3", "Hidroginástica", "Aquáticos", 4.0, "💧"),
        ExerciseBlueprint("a4", "Surf", "Aquáticos", 3.0, "🏄"),
        ExerciseBlueprint("a5", "Stand Up Paddle", "Aquáticos", 2.5, "🏄‍♂️"),

        ExerciseBlueprint("d1", "Yoga (Hatha/Alongamento)", "Zen/Dança", 2.5, "🧘"),
        ExerciseBlueprint("d2", "Yoga (Power/Vinyasa)", "Zen/Dança", 4.0, "🧘‍♂️"),
        ExerciseBlueprint("d3", "Pilates", "Zen/Dança", 3.0, "🧘‍♀️"),
        ExerciseBlueprint("d4", "Zumba / FitDance", "Zen/Dança", 7.0, "💃"),
        ExerciseBlueprint("d5", "Ballet / Jazz", "Zen/Dança", 5.0, "🩰"),
        ExerciseBlueprint("d6", "Forró / Sertanejo / Salsa", "Zen/Dança", 4.5, "🕺"),
        ExerciseBlueprint("d7", "Alongamento", "Zen/Dança", 2.3, "🙆‍♂️"),

        ExerciseBlueprint("o1", "Skate", "Outros", 5.0, "🛹"),
        ExerciseBlueprint("o2", "Patins / Roller", "Outros", 7.0, "🛼"),
        ExerciseBlueprint("o3", "Escalada / Boulder", "Outros", 8.0, "🧗"),
        ExerciseBlueprint("o4", "Limpeza Pesada (Casa)", "Outros", 3.5, "🧹"),
        ExerciseBlueprint("o5", "Sexo (Ativo)", "Outros", 4.0, "❤️") // Sim, cientificamente conta! (Opcional)
    )


    val categories = list.map { it.category }.distinct().sorted()
}