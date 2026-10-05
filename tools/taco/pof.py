"""Leitura das fontes: composição da TACO e medidas caseiras da POF (IBGE)."""
import csv
from pathlib import Path

FONTE = Path(__file__).resolve().parent / "fonte"


def taco():
    """número TACO → linha da composição."""
    return {int(r["numero_alimento"]): r for r in csv.DictReader(open(FONTE / "taco_composicao.csv"))}


def pof_rows():
    return list(csv.DictReader(open(FONTE / "pof_medidas_caseiras.csv")))


def measure_grams(rows, food, prep, measure):
    """Gramas de uma medida da POF: preparo exato se dado, senão o primeiro que tiver a medida."""
    matches = [r for r in rows if r["descricao_alimento"] == food and r["descricao_medida"] == measure]
    if prep:
        exact = [r for r in matches if r["descricao_preparacao"] == prep]
        matches = exact or matches
    return float(matches[0]["quantidade_g"]) if matches else None
