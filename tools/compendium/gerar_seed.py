"""
Gera o seed de exercícios do app (SPEC §8.1).

- familias.py diz como as atividades aparecem no app: cada exercício uma vez só, com até
  3 níveis (Leve / Moderada / Intensa).
- O MET de cada nível vem da lista aprovada do Compendium (docs/EXERCICIOS.md), pelo
  código + nome.

Escreve app/src/main/java/com/projeto/wellio/data/seed/ExerciseSeed.kt (uma linha por
nível) e o resumo "Como aparecem no app" no topo de docs/EXERCICIOS.md.

O id é "cmp.<código>"; quando o mesmo código aparece em duas atividades (aproximações),
ganha o ícone: "cmp.15430.capoeira".

Uso: python3 tools/compendium/gerar_seed.py
"""
import re
from collections import Counter
from pathlib import Path

from familias import FAMILIAS

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "docs/EXERCICIOS.md"
TARGET = ROOT / "app/src/main/java/com/projeto/wellio/data/seed/ExerciseSeed.kt"
LEVELS = {"Leve": "Light", "Moderada": "Moderate", "Intensa": "Vigorous"}
START, END = "<!-- familias:inicio -->", "<!-- familias:fim -->"


def approved_mets():
    """(código, nome) → MET, das tabelas da lista aprovada."""
    mets = {}
    for line in SOURCE.read_text().splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) == 4 and re.fullmatch(r"\d+,\d", cells[1]):
            mets[(cells[2], cells[0].replace("⚠️", "").strip())] = float(cells[1].replace(",", "."))
    return mets


def kotlin_string(text):
    return "null" if text is None else '"' + text.replace("\\", "\\\\").replace('"', '\\"') + '"'


def main():
    mets = approved_mets()
    codes = Counter(code for *_, variants in FAMILIAS for _, code, _, _ in variants)
    lines, summary = [], []
    category = None
    for cat, name, icon, variants in FAMILIAS:
        if cat != category:
            category = cat
            summary.append(f"\n**{cat}**\n")
        levels = []
        for level, code, approved_name, hint in variants:
            if (code, approved_name) not in mets:
                raise SystemExit(f"Não está na lista aprovada: {code} / {approved_name}")
            met = mets[(code, approved_name)]
            exercise_id = f"cmp.{code}" if codes[code] == 1 else f"cmp.{code}.{icon}"
            level_kt = f"Intensity.{LEVELS[level]}" if level else "null"
            lines.append(
                f"    ExerciseType({kotlin_string(exercise_id)}, {kotlin_string(name)}, {kotlin_string(cat)}, {met}, "
                f"{kotlin_string('exercise.' + icon)}, compendiumCode = {kotlin_string(code)}, "
                f"level = {level_kt}, levelHint = {kotlin_string(hint)}),"
            )
            met_text = str(met).replace(".", ",")
            levels.append(f"{level} {met_text}" + (f" ({hint})" if hint else "") if level else f"{met_text} MET")
        summary.append(f"- {name}: " + " · ".join(levels))

    total = sum(len(v) for *_, v in FAMILIAS)
    TARGET.write_text(
        "// Gerado por tools/compendium/gerar_seed.py a partir de familias.py e docs/EXERCICIOS.md: edite lá, não aqui.\n"
        "package com.projeto.wellio.data.seed\n\n"
        "import com.projeto.wellio.domain.exercise.Intensity\n"
        "import com.projeto.wellio.domain.model.ExerciseType\n\n"
        f"/** {len(FAMILIAS)} atividades ({total} níveis) do Compendium 2024 (SPEC §8.1), gravadas no banco na primeira abertura. */\n"
        "val exerciseSeed = listOf(\n" + "\n".join(lines) + "\n)\n"
    )

    section = (
        f"{START}\n## Como aparecem no app ({len(FAMILIAS)} atividades)\n\n"
        "Cada exercício aparece uma vez só; os que têm variações ganham níveis (Leve / Moderada / Intensa) "
        "no registro, cada um com o MET abaixo. Gerado por `tools/compendium/gerar_seed.py` a partir de "
        "`tools/compendium/familias.py`; as tabelas mais abaixo são a base do Compendium de onde saem os METs.\n"
        + "\n".join(summary) + f"\n{END}"
    )
    doc = SOURCE.read_text()
    if START in doc:
        doc = re.sub(re.escape(START) + ".*?" + re.escape(END), lambda _: section, doc, flags=re.S)
    else:
        first = doc.index("\n## ")
        doc = doc[:first] + "\n\n" + section + "\n" + doc[first:]
    SOURCE.write_text(doc)
    print(f"{len(FAMILIAS)} atividades, {total} níveis")


if __name__ == "__main__":
    main()
