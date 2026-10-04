"""
Gera o seed de exercícios do app a partir da lista aprovada (docs/EXERCICIOS.md, SPEC §8.1).

Lê nome, MET e código de cada tabela (uma por categoria), cruza com selecao.py pelo
código + nome para pegar o ícone, e escreve
app/src/main/java/com/projeto/wellio/data/seed/ExerciseSeed.kt.

O id é "cmp.<código>"; quando duas atividades usam o mesmo código (aproximações, ⚠️),
o id ganha o ícone: "cmp.15430.capoeira".

Uso: python3 tools/compendium/gerar_seed.py
"""
import re
from collections import Counter
from pathlib import Path

from selecao import SEL

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs/EXERCICIOS.md"
TARGET = ROOT / "app/src/main/java/com/projeto/wellio/data/seed/ExerciseSeed.kt"


def rows():
    category = None
    for line in SOURCE.read_text().splitlines():
        heading = re.match(r"^## (.+?) \(\d+\)$", line)
        if heading:
            category = heading.group(1)
            continue
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if category and len(cells) == 4 and re.fullmatch(r"\d+,\d", cells[1]):
            name = cells[0].replace("⚠️", "").strip()
            yield category, name, float(cells[1].replace(",", ".")), cells[2]


def icon_for(category, code, name):
    matches = [s for s in SEL if s[0] == category and s[1] == code and s[2] == name]
    if len(matches) != 1:
        raise SystemExit(f"Sem correspondência única em selecao.py: {category} / {code} / {name}")
    return matches[0][3]


def kotlin_string(text):
    return '"' + text.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    items = list(rows())
    repeated = Counter(code for _, _, _, code in items)
    lines = []
    for category, name, mets, code in items:
        icon = icon_for(category, code, name)
        exercise_id = f"cmp.{code}" if repeated[code] == 1 else f"cmp.{code}.{icon}"
        lines.append(
            f"    ExerciseType({kotlin_string(exercise_id)}, {kotlin_string(name)}, {kotlin_string(category)}, "
            f"{mets}, {kotlin_string('exercise.' + icon)}, compendiumCode = {kotlin_string(code)}),"
        )
    TARGET.write_text(
        "// Gerado por tools/compendium/gerar_seed.py a partir de docs/EXERCICIOS.md: edite lá, não aqui.\n"
        "package com.projeto.wellio.data.seed\n\n"
        "import com.projeto.wellio.domain.model.ExerciseType\n\n"
        f"/** As {len(items)} atividades aprovadas do Compendium 2024 (SPEC §8.1), gravadas no banco na primeira abertura. */\n"
        "val exerciseSeed = listOf(\n" + "\n".join(lines) + "\n)\n"
    )
    print(f"{len(items)} atividades em {TARGET.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
