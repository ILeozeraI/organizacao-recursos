# Decisão de qualidade — `v1.0.0-rc1`

## Decisão: **LIBERAR COM RESSALVAS**
Liberada para demonstração e avaliação acadêmica. **Não** é uma declaração de "pronto para produção":
as evidências vêm do CI e de uma rodada de carga no próprio runner.

## Critérios usados e evidência de cada um
| Critério | Resultado | Evidência |
|---|---|---|
| Aplicação executa a partir do README (clone limpo) | Sim | [`entrega/checklist.md`](entrega/checklist.md), job `qualidade / carga` sobe o `.jar` do zero |
| Pipeline de CI existe e está verde no commit da tag | Sim | [execução do CI da tag](https://github.com/ILeozeraI/organizacao-recursos/actions?query=branch%3Av1.0.0-rc1) |
| Sem falha de autorização | Sim | `MatrizAutorizacaoIT` 130/130, mutantes M5 e M7 mortos |
| Dupla reserva impossível | Sim | `ConcorrenciaReservaIT` (5 repetições × 2 cenários), carga: 1 reserva ativa no banco, mutante M2 morto |
| Cobertura ≥ 80% linhas / ≥ 70% branches | 94,0% / 83,3% | JaCoCo (gate no build) |
| 100% dos requisitos críticos rastreados | 7/7 | [`rtm.md`](rtm.md) |
| 0 bugs/vulnerabilidades críticas | 0 / 0, Quality Gate OK | SonarCloud |
| Nenhum defeito P1 aberto | Sim (DEF-002 fechado) | [`defeitos/`](defeitos/) |
| RNF-PERF-01 | atende com ressalva (login 344 ms) | [`performance/resultado-01.md`](performance/resultado-01.md) |

## Riscos aceitos
| Risco | Impacto | Justificativa | Responsável |
|---|---|---|---|
| p95 do login acima da meta (344 ms × 300 ms) | baixo | 1 login por sessão; BCrypt protege contra força bruta | Leonardo Cavalcanti da Silva |
| RF13 sem teste E2E de navegador | médio | API 100% testada em caixa-preta; roteiro manual na demonstração | Leonardo Cavalcanti da Silva |
| Notificações com falha sem reenvio automático | baixo | decisão sobre a reserva não depende da notificação | Leonardo Cavalcanti da Silva |
| Medidas de desempenho só no runner do CI | médio | não há ambiente compartilhado; limites documentados | Leonardo Cavalcanti da Silva |

## Integrante e declaração de autoria
| Integrante | O que declara ter feito |
|---|---|
| **Leonardo Cavalcanti da Silva** (STADSCAS4ND) | Trabalho individual. Conduziu o projeto com apoio de um agente de IA (como orientado nas aulas, com prompts por etapa): definiu o escopo a partir das aulas e da especificação, configurou GitHub, SonarCloud, secrets e proteção da branch, acompanhou o CI, revisou os PRs (#1 TDD, #2 documentação, #3 release) e as correções dos defeitos DEF-001 e DEF-002, e assume a defesa técnica de todas as partes na prova oral. |

---
*Envio no formulário do Hub: um único envio, com o link público do repositório e a tag avaliada (abaixo).*

```
Turma: STADSCAS4ND · Sexta
Integrante: Leonardo Cavalcanti da Silva
Repositório: https://github.com/ILeozeraI/organizacao-recursos
Tag avaliada: v1.0.0-rc1
```
