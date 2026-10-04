"""
Ícones desenhados do Wellio (SPEC §2.3): fonte única dos desenhos.

Gera os vector drawables em app/src/main/res/drawable (ic_<grupo>_<nome>.xml)
e SVGs de prévia em tools/icons/svg/<grupo>. Traço de 1,75 em grade 24x24,
pontas arredondadas, sem preenchimento; a cor vem do tint no app.

Grupos:
- container: recipientes de água (SPEC §6)
- module: ícone de cada funcionalidade (sidebar, FAB, resumos)
- exercise: categoria de exercício (SPEC §8.0); cada atividade usa o da sua categoria

Uso: python3 tools/icons/icones.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app/src/main/res/drawable"
SVG = ROOT / "tools/icons/svg"
STROKE = 1.75

CONTAINER = {
    "copo": [
        "M6,4 H18 L16.5,19.9 Q16.4,21 15.3,21 H8.7 Q7.6,21 7.5,19.9 Z",
        "M6.6,10 H17.4",
    ],
    "caneca": [
        "M5,6 H15 V17 A3,3 0 0 1 12,20 H8 A3,3 0 0 1 5,17 Z",
        "M15,9 H16.5 A2.5,2.5 0 0 1 16.5,14 H15",
        "M5,10 H15",
    ],
    "xicara": [
        "M5,10 H16 V12 A5.5,5.5 0 0 1 5,12 Z",
        "M16,11 H17 A2,2 0 0 1 17,15 H15.3",
        "M3.5,20.5 H19.5",
        "M8.5,3 C7.7,3.8 9.3,4.7 8.5,5.5 C7.7,6.3 9.3,7.2 8.5,8",
        "M12.5,3 C11.7,3.8 13.3,4.7 12.5,5.5 C11.7,6.3 13.3,7.2 12.5,8",
    ],
    "garrafa": [
        "M10,2.5 H14 V5 H10 Z",
        "M10,5 V6.5 C10,7.5 7,8.5 7,10.5 V20 A1.5,1.5 0 0 0 8.5,21.5 H15.5 A1.5,1.5 0 0 0 17,20 V10.5 C17,8.5 14,7.5 14,6.5 V5",
        "M7,13 H17",
    ],
    "squeeze": [
        "M7.5,9 H16.5 C16.5,11.5 15.5,12.5 15.5,14.5 C15.5,16.5 16.5,17 16.5,19.5 A2,2 0 0 1 14.5,21.5 H9.5 A2,2 0 0 1 7.5,19.5 C7.5,17 8.5,16.5 8.5,14.5 C8.5,12.5 7.5,11.5 7.5,9 Z",
        "M8,9 V7.5 A1.5,1.5 0 0 1 9.5,6 H14.5 A1.5,1.5 0 0 1 16,7.5 V9",
        "M11,6 L11.4,2.5 H12.6 L13,6",
    ],
    "taca": [
        "M7,3 H17 V7 A5,5 0 0 1 7,7 Z",
        "M7,7 H17",
        "M12,12 V20.5",
        "M8,20.5 H16",
    ],
    "termica": [
        "M9,2.5 H15 V5 H9 Z",
        "M8,5 H16 A1,1 0 0 1 17,6 V20 A1.5,1.5 0 0 1 15.5,21.5 H8.5 A1.5,1.5 0 0 1 7,20 V6 A1,1 0 0 1 8,5 Z",
        "M7,9 H17",
        "M7,17.5 H17",
    ],
    "galao": [
        "M10.5,2.5 H13.5 V5",
        "M10.5,2.5 V5 C8,5 6,6 6,8.5 V19.5 A2,2 0 0 0 8,21.5 H16 A2,2 0 0 0 18,19.5 V8.5 C18,6 16,5 13.5,5",
        "M6,12 H18",
        "M6,16 H18",
    ],
}

MODULE = {
    "water": [
        "M12,3 C12,3 5.5,10 5.5,14.5 A6.5,6.5 0 0 0 18.5,14.5 C18.5,10 12,3 12,3 Z",
        "M9,15 A3,3 0 0 0 11.5,17.8",
    ],
    "weight": [
        "M5,4 H19 A2,2 0 0 1 21,6 V19 A2,2 0 0 1 19,21 H5 A2,2 0 0 1 3,19 V6 A2,2 0 0 1 5,4 Z",
        "M7.5,11.5 A4.5,4.5 0 0 1 16.5,11.5 Z",
        "M12,11.5 L13.6,8.6",
    ],
    "exercise": [
        "M12,3 C12,3 18,8 18,14 A6,6 0 0 1 6,14 C6,10.5 8.5,8.5 8.5,8.5 C8.5,11 10,12 10,12 C10,8 12,3 12,3 Z",
        "M12,20 A2.5,2.5 0 0 1 9.5,17.5 C9.5,15.5 12,13.5 12,13.5 C12,13.5 14.5,15.5 14.5,17.5 A2.5,2.5 0 0 1 12,20 Z",
    ],
    "food": [
        "M12,7.5 C10,6 5,6 5,11.5 C5,16 8,21 10,21 C11,21 11.5,20.5 12,20.5 C12.5,20.5 13,21 14,21 C16,21 19,16 19,11.5 C19,6 14,6 12,7.5 Z",
        "M12,7.5 C12,5.5 13,4 14.5,3",
        "M12.6,5.2 C13.6,3.7 16,3.6 17,4.6 C16,5.9 13.9,6.1 12.6,5.2 Z",
    ],
}

EXERCISE = {
    # Tênis de corrida, de lado
    "cardio": [
        "M3,16.5 H20.5 A1.5,1.5 0 0 1 22,18 A1.5,1.5 0 0 1 20.5,19.5 H4.5 A1.5,1.5 0 0 1 3,18 Z",
        "M3,16.5 V8.5 A1.5,1.5 0 0 1 4.5,7 H7.5 L10,10.5 L13.5,11.2 C17.5,12 20.8,13.6 21.3,16.5",
        "M9.2,9.6 L10.8,8.4",
        "M11.6,10.9 L13.2,9.7",
    ],
    # Halter
    "strength": [
        "M6,6.5 H8.5 V17.5 H6 Z",
        "M3.5,9 H6 V15 H3.5 Z",
        "M15.5,6.5 H18 V17.5 H15.5 Z",
        "M18,9 H20.5 V15 H18 Z",
        "M8.5,12 H15.5",
    ],
    # Luva de boxe
    "fight": [
        "M7,12.5 V8.5 A4.5,4.5 0 0 1 11.5,4 H13.5 A4.5,4.5 0 0 1 18,8.5 V13 A4,4 0 0 1 14,17 H11 A4,4 0 0 1 7,13",
        "M7,12.5 C5.3,12.5 4.5,11 5,9.8 C5.5,8.8 6.5,8.6 7,9",
        "M7,12.5 C8.5,11.3 10.5,11.3 11.5,12.5",
        "M9,17 V20.5 H16 V17",
    ],
    # Bola (vôlei)
    "sports": [
        "M12,3 A9,9 0 1 1 11.99,3 Z",
        "M12,3 C12.5,8 10.5,12 4.5,16.5",
        "M12,12 C16.5,11 20.5,12.5 20.8,14",
        "M12,12 C9.5,14.5 9.5,18 11,21",
    ],
    # Ondas
    "water": [
        "M3,8 C5,6.5 7,6.5 9,8 C11,9.5 13,9.5 15,8 C17,6.5 19,6.5 21,8",
        "M3,13 C5,11.5 7,11.5 9,13 C11,14.5 13,14.5 15,13 C17,11.5 19,11.5 21,13",
        "M3,18 C5,16.5 7,16.5 9,18 C11,19.5 13,19.5 15,18 C17,16.5 19,16.5 21,18",
    ],
    # Flor de lótus
    "zen": [
        "M12,5.5 C14.5,8 14.5,12.5 12,16 C9.5,12.5 9.5,8 12,5.5 Z",
        "M12,16 C8.5,16 5,13.5 4.5,10 C7.5,10 10.5,12 12,16",
        "M12,16 C15.5,16 19,13.5 19.5,10 C16.5,10 13.5,12 12,16",
        "M6,19 C9,18 15,18 18,19",
    ],
    # Estrela
    "other": [
        "M12,3.5 L14.5,9 L20.5,9.6 L16,13.6 L17.3,19.5 L12,16.5 L6.7,19.5 L8,13.6 L3.5,9.6 L9.5,9 Z",
    ],
}

GROUPS = {"container": CONTAINER, "module": MODULE, "exercise": EXERCISE}


def vector_drawable(paths: list[str]) -> str:
    body = "\n".join(
        f'''    <path
        android:pathData="{d}"
        android:strokeColor="#FF000000"
        android:strokeWidth="{STROKE}"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />'''
        for d in paths
    )
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Gerado por tools/icons/icones.py: edite lá, não aqui. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
{body}
</vector>
'''


def svg(paths: list[str]) -> str:
    body = "".join(f'<path d="{d}"/>' for d in paths)
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" width="24" height="24" '
        f'fill="none" stroke="#000" stroke-width="{STROKE}" stroke-linecap="round" stroke-linejoin="round">'
        f"{body}</svg>\n"
    )


if __name__ == "__main__":
    total = 0
    for group, icons in GROUPS.items():
        (SVG / group).mkdir(parents=True, exist_ok=True)
        for name, paths in icons.items():
            (DRAWABLE / f"ic_{group}_{name}.xml").write_text(vector_drawable(paths))
            (SVG / group / f"{name}.svg").write_text(svg(paths))
            total += 1
    print(f"{total} ícones gerados")
