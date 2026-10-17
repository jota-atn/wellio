"""
Base de alimentos do app (SPEC §7.0 e §7.5).

Lê familias.py (alimentos e preparos, com o número TACO ou um extra), pratos.py (pratos
prontos do app) e as fontes em fonte/ (composição da TACO 4ª ed. e medidas caseiras da POF
2008-2009), e escreve docs/ALIMENTOS.md para revisão.

Uso: python3 tools/taco/gerar_base.py
"""
from pathlib import Path

from familias import EXTRAS, FAMILIAS
from pof import measure_grams, pof_rows, taco
from pratos import PRATOS

ROOT = Path(__file__).resolve().parents[2]
DOC = ROOT / "docs/ALIMENTOS.md"

# Nome da medida na POF → como aparece no app
MEDIDAS = {
    "COLHER DE SOPA": "colher de sopa", "COLHER DE ARROZ/SERVIR": "colher de servir", "COLHER DE CHA": "colher de chá",
    "ESCUMADEIRA": "escumadeira", "CONCHA": "concha", "UNIDADE": "unidade", "UNIDADE PEQUENA": "unidade pequena",
    "FATIA": "fatia", "BIFE": "bife", "FILE": "filé", "PEDACO": "pedaço", "POSTA": "posta", "ESPETINHO": "espetinho",
    "COPO AMERICANO": "copo americano", "COPO MEDIO": "copo", "COPO GRANDE": "copo grande", "COPO TULIPA": "copo de cerveja",
    "XICARA DE CHA": "xícara de chá", "XICARA DE CAFE": "xícara de café", "LATA (350 ML)": "lata", "LATA (N. E.)": "lata",
    "GARRAFA (200 ML)": "caixinha (200 ml)", "GARRAFA (500 ML)": "garrafa (500 ml)", "TACA": "taça", "PEGADOR": "pegador",
    "PRATO RASO": "prato raso", "PRATO DE SOBREMESA": "prato de sobremesa", "CUMBUCA": "cumbuca", "TIGELA": "tigela",
    "POTE": "pote", "PONTA DE FACA": "ponta de faca", "BARRA": "barra", "TABLETE": "tablete", "BOLA": "bola",
    "PACOTE": "pacote", "PORCAO": "porção", "RODELA": "rodela", "RAMO": "ramo", "FOLHA": "folha", "PUNHADO": "punhado",
    "CACHO": "cacho",
}


def plural(word):
    if word.endswith("ão"):
        return word[:-2] + "ões"
    if word.endswith(("r", "z")):
        return word + "es"
    return word + "s"


def measure_label(measure, qty):
    """Medida com a quantidade concordando: "1 colher de sopa", "4 colheres de sopa"."""
    text = MEDIDAS[measure]
    if qty <= 1:
        return text
    # Pluraliza o nome e os adjetivos colados a ele, até o primeiro "de" ou parêntese
    words = text.split(" ")
    for i, word in enumerate(words):
        if word == "de" or word.startswith("("):
            break
        words[i] = plural(word)
    return " ".join(words)


def nutrients(ref, table):
    """(kcal, proteína, carboidrato, gordura, é extra?) por 100 g."""
    if isinstance(ref, str):
        kcal, p, c, g, _ = EXTRAS[ref]
        return kcal, p, c, g, True
    row = table[ref]
    num = lambda k: float(row[k] or 0)
    return num("energia_kcal"), num("proteina_g"), num("carboidrato_g"), num("lipideos_g"), False


def fmt(value, decimals=0):
    return f"{value:.{decimals}f}".replace(".", ",")


def main():
    table, rows = taco(), pof_rows()
    families = {}
    lines = [
        "# Wellio — Alimentos (base do app)",
        "",
        "Base de alimentos para revisão (SPEC §7.0 e §7.5). Cada alimento aparece uma vez no app; o tipo ou",
        "preparo é escolhido nos detalhes do registro.",
        "",
        "- **Valores** por 100 g (ou 100 ml): TACO 4ª edição (NEPA/Unicamp). P = proteína, C = carboidrato, G = gordura.",
        "- **Medidas caseiras**: POF 2008-2009 (IBGE), em gramas. Todo alimento aceita gramas também.",
        "- ⚠️ = **extra**: a TACO não tem (ou veio sem valor); valor de referência, a revisar.",
        "- Gerado por `tools/taco/gerar_base.py` a partir de `tools/taco/familias.py` e `tools/taco/pratos.py`.",
        "",
        "**Para revisar:** marque o que tirar, adicionar, juntar ou renomear.",
    ]
    category = None
    total_variants = 0
    for cat, name, icon, variants, (food, prep), measures in FAMILIAS:
        if cat != category:
            category = cat
            count = sum(1 for f in FAMILIAS if f[0] == cat)
            lines += ["", f"## {cat} ({count})", ""]
        grams = {m: measure_grams(rows, food, prep, m) for m in measures}
        families[name] = (variants, grams)
        measure_text = ", ".join(f"{MEDIDAS[m]} {fmt(g)} g" for m, g in grams.items())
        lines.append(f"- **{name}** — {measure_text}")
        for label, ref in variants:
            total_variants += 1
            kcal, p, c, g, extra = nutrients(ref, table)
            title = label or "—"
            lines.append(f"  - {title}{' ⚠️' if extra else ''}: {fmt(kcal)} kcal · P {fmt(p, 1)} · C {fmt(c, 1)} · G {fmt(g, 1)}")

    lines += ["", f"## Pratos prontos do app ({len(PRATOS)})", "", "Entram no prato de uma vez; cada item pode ser ajustado.", ""]
    for dish, meal, items in PRATOS:
        parts, total = [], 0.0
        for food_name, label, measure, qty, *shown in items:
            variants, grams = families[food_name]
            ref = dict(variants)[label]
            kcal = nutrients(ref, table)[0] * grams[measure] * qty / 100
            total += kcal
            what = shown[0] if shown else f"{food_name} {label}".strip().lower()
            parts.append(f"{fmt(qty) if qty == int(qty) else fmt(qty, 1)} {measure_label(measure, qty)} de {what}")
        lines.append(f"- **{dish}** ({meal}, ~{fmt(total)} kcal): " + "; ".join(parts))

    lines += ["", "## Extras ⚠️ e de onde vieram os valores", ""]
    for key, (kcal, p, c, g, source) in EXTRAS.items():
        used = [f"{n} {l}".strip() for _, n, _, vs, _, _ in FAMILIAS for l, r in vs if r == key]
        lines.append(f"- {', '.join(used) or key}: {fmt(kcal)} kcal · P {fmt(p, 1)} · C {fmt(c, 1)} · G {fmt(g, 1)} — {source}")

    DOC.write_text("\n".join(lines) + "\n")
    print(f"{len(FAMILIAS)} alimentos, {total_variants} tipos/preparos, {len(PRATOS)} pratos → {DOC.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
