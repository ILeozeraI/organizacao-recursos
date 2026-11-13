#!/usr/bin/env python3
"""Imprime o Quality Gate e as issues abertas do SonarCloud (evidência no log/Summary do CI).
Uso no CI: SONAR_TOKEN=... python3 scripts/sonar_resumo.py <projectKey> <branch|pr:NUM>"""
import base64
import json
import os
import sys
import time
import urllib.request

API = "https://sonarcloud.io/api"


def get(caminho):
    req = urllib.request.Request(API + caminho)
    token = os.environ.get("SONAR_TOKEN", "")
    req.add_header("Authorization", "Basic " + base64.b64encode((token + ":").encode()).decode())
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)


def main(projeto, alvo):
    filtro = f"pullRequest={alvo[3:]}" if alvo.startswith("pr:") else f"branch={alvo}"
    status = None
    for _ in range(12):  # espera o processamento da análise (até ~2 min)
        try:
            status = get(f"/qualitygates/project_status?projectKey={projeto}&{filtro}")["projectStatus"]
            break
        except Exception:
            time.sleep(10)
    print(f"### SonarCloud ({alvo})")
    if status:
        print(f"- **Quality Gate:** {status['status']}")
        for c in status.get("conditions", []):
            print(f"  - {c['metricKey']}: {c.get('actualValue', '-')} ({c['status']})")
    medidas = get(f"/measures/component?component={projeto}&{filtro}&metricKeys="
                  "bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density,security_hotspots")
    for m in medidas["component"]["measures"]:
        print(f"- {m['metric']}: {m.get('value', '-')}")
    issues = get(f"/issues/search?componentKeys={projeto}&{filtro}&resolved=false&ps=100")
    print(f"\n**Issues abertas: {issues['total']}**\n")
    if issues["issues"]:
        print("| Severidade | Tipo | Arquivo:linha | Regra | Mensagem |")
        print("|---|---|---|---|---|")
        for i in issues["issues"]:
            arquivo = i["component"].split(":", 1)[-1]
            print(f"| {i.get('severity')} | {i.get('type')} | {arquivo}:{i.get('line', '-')} | {i['rule']} | {i['message']} |")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
