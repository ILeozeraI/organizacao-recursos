#!/usr/bin/env python3
"""Lê o .jtl (CSV) do JMeter e calcula p50/p95/p99, vazão e taxa de erro por operação.

O 409 (reserva recusada por conflito) é RESPOSTA ESPERADA e não conta como erro;
erro real é qualquer outra falha (5xx, timeout, assertiva quebrada).
Uso: python3 perf/analisar.py perf/resultados/rodada-01.jtl
"""
import csv
import math
import sys
from collections import defaultdict


def percentil(valores, p):
    if not valores:
        return 0
    ordenados = sorted(valores)
    k = max(0, math.ceil(p / 100 * len(ordenados)) - 1)  # método nearest-rank
    return ordenados[k]


def main(caminho):
    linhas = list(csv.DictReader(open(caminho, encoding="utf-8")))
    por_op = defaultdict(list)
    for l in linhas:
        por_op[l["label"]].append(l)
    inicio = min(int(l["timeStamp"]) for l in linhas)
    fim = max(int(l["timeStamp"]) + int(l["elapsed"]) for l in linhas)
    duracao_s = (fim - inicio) / 1000

    print(f"Amostras: {len(linhas)} | Duração medida: {duracao_s:.1f} s")
    print("| Operação | Amostras | p50 (ms) | p95 (ms) | p99 (ms) | Máx (ms) | Vazão (req/s) | 2xx | 409 esperado | Erro real | Taxa de erro real |")
    print("|---|---|---|---|---|---|---|---|---|---|---|")
    total_erros = 0
    for op in sorted(por_op):
        ls = por_op[op]
        t = [int(l["elapsed"]) for l in ls]
        codigos = [l["responseCode"] for l in ls]
        ok2xx = sum(1 for c in codigos if c.startswith("2"))
        c409 = sum(1 for c in codigos if c == "409")
        erros = sum(1 for l in ls if l["success"] != "true")
        total_erros += erros
        print(f"| {op} | {len(ls)} | {percentil(t, 50)} | {percentil(t, 95)} | {percentil(t, 99)} | {max(t)} | "
              f"{len(ls) / duracao_s:.1f} | {ok2xx} | {c409} | {erros} | {100 * erros / len(ls):.2f}% |")
    todos = [int(l["elapsed"]) for l in linhas]
    print(f"\nGERAL: p95={percentil(todos, 95)} ms | vazão={len(linhas) / duracao_s:.1f} req/s | "
          f"erro real={100 * total_erros / len(linhas):.2f}%")
    codigos = defaultdict(int)
    for l in linhas:
        codigos[l["responseCode"]] += 1
    print("Códigos HTTP:", dict(sorted(codigos.items())))


if __name__ == "__main__":
    main(sys.argv[1])
