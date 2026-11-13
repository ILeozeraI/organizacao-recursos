# Resultado da rodada 01 — teste de carga (JMeter)

| Item | Valor |
|---|---|
| Plano | `perf/reservas.jmx` (modelo em [`rnf.md`](rnf.md)) |
| Commit medido | `8ec11cb` (main, com a correção do DEF-002) |
| Execução | GitHub Actions run `43469480829`, job `qualidade / carga` (13/11/2026) |
| Ambiente | runner `ubuntu-latest`; aplicação (`java -jar`) e PostgreSQL 16 no mesmo runner; JMeter 5.6.3 CLI |
| Carga | 50 usuários, ramp-up 10 s, 60 s de duração |
| Arquivos | `.jtl`, relatório HTML e `app.log` no artifact `carga-<n>` do run |
| Cálculo | `perf/analisar.py` (percentil *nearest-rank* sobre o `.jtl`) |

## Métricas por operação
| Operação | Amostras | p50 (ms) | p95 (ms) | p99 (ms) | Máx (ms) | Vazão (req/s) | 2xx | 409 esperado | Erro real |
|---|---|---|---|---|---|---|---|---|---|
| `GET /recursos` (disponibilidade) | 24.154 | 29 | **108** | 226 | 860 | 404,4 | 24.154 | 0 | 0 (0,00%) |
| `POST /auth/login` | 50 | 196 | **344** | 479 | 479 | 0,8 | 50 | 0 | 0 (0,00%) |
| `POST /reservas` (disputada) | 24.133 | 47 | **224** | 388 | 928 | 404,1 | **1** | 24.132 | 0 (0,00%) |
| **Geral** | 48.337 | — | 190 | — | 928 | 809,3 | — | — | **0,00%** |

Códigos HTTP: `200` = 24.204 · `201` = **1** · `409` = 24.132 · nenhum 5xx.

## Verificação no banco (pelo estado, não pela resposta)
`select count(*) from reserva where recurso_id = 1 and inicio = '2027-03-01T10:00:00Z' and status in ('SOLICITADA','APROVADA','EM_USO')`
→ **1**. Linhas `ERROR` no log da aplicação: **0**.

O 409 foi contado como **resposta esperada** (quem perdeu a disputa), não como erro.

## Decisão: **atende com ressalva**
| Critério do RNF-PERF-01 | Medido | Situação |
|---|---|---|
| p95 ≤ 300 ms — consulta | 108 ms | atende |
| p95 ≤ 300 ms — criação de reserva | 224 ms | atende |
| p95 ≤ 300 ms — login | **344 ms** | **não atende** |
| Erro real < 1% | 0,00% | atende |
| No máximo 1 reserva ativa no banco | 1 | atende |

## Hipótese de gargalo (sustentada pelos dados)
O login é a única operação acima da meta, e é a única que executa **BCrypt** (custo 10, propositalmente
lento contra força bruta). Os 50 logins acontecem todos dentro do ramp-up de 10 s, competindo por CPU com
as primeiras iterações de consulta e reserva no mesmo runner (p50 196 ms já é ~4× o p50 da reserva).
Como o login acontece uma vez por sessão (8 h), o impacto para o usuário é pequeno.
**Próximo passo:** medir o login isolado e decidir entre manter o custo do BCrypt (segurança) ou
aceitar a meta de 500 ms só para o login (trade-off segurança × desempenho — ver ATAM).

## Limites desta conclusão
- Aplicação, banco e gerador de carga estavam **na mesma máquina** do CI: os números servem para comparar
  versões entre si, **não** para afirmar o desempenho em produção.
- Uma única rodada; não há medida de variância entre rodadas.
- A reserva disputada mede o caminho de **conflito** (99,99% das respostas foram 409); o caminho de criação
  bem-sucedida sob carga não foi medido isoladamente.
