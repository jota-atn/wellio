"""
Ícones dos recipientes de água (SPEC §6): fonte única dos desenhos.

Gera os vector drawables em app/src/main/res/drawable e SVGs de prévia em
tools/icons/svg. Traço de 1,75 em grade 24x24, pontas arredondadas, sem
preenchimento; a cor vem do tint no app.

Uso: python3 tools/icons/recipientes.py
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DRAWABLE = ROOT / "app/src/main/res/drawable"
SVG = ROOT / "tools/icons/svg"
STROKE = 1.75

ICONS = {
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
<!-- Gerado por tools/icons/recipientes.py: edite lá, não aqui. -->
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
    SVG.mkdir(parents=True, exist_ok=True)
    for name, paths in ICONS.items():
        (DRAWABLE / f"ic_container_{name}.xml").write_text(vector_drawable(paths))
        (SVG / f"{name}.svg").write_text(svg(paths))
    print(f"{len(ICONS)} ícones gerados")
