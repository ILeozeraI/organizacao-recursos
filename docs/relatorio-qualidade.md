# Relatório final de qualidade

## 1. Identificação
| Item | Valor |
|---|---|
| Repositório | https://github.com/ILeozeraI/organizacao-recursos (público) |
| Versão avaliada | tag `v1.0.0-rc1` |
| Data | 13/11/2026 |
| Integrante | Leonardo Cavalcanti da Silva — turma STADSCAS4ND (trabalho individual, feito com apoio de agente de IA e revisado pelo autor) |
| Ambiente de medição | GitHub Actions `ubuntu-latest`, Java 21 Temurin, PostgreSQL 16 (Testcontainers / service container), JMeter 5.6.3 |

**Regra deste relatório:** todo número tem origem citada; o que não foi medido está marcado como **não medido**.

## 2. Escopo
- **Avaliado:** RF1–RF14, regras críticas (sobreposição, dupla reserva, aprovação de restrito, manutenção,
  autorização, auditoria), RN-09, RNF de desempenho (RNF-PERF-01), robustez sob concorrência (RNF-ROB-01),
  integração externa (RNF-INT-01) e segurança das respostas de erro.
- **Fora de escopo:** teste E2E em navegador da interface; provedor de notificação real; desempenho em produção.
  Detalhes e riscos em [`testes/plano-de-testes.md`](testes/plano-de-testes.md) §2.

## 3. Estratégia por nível
Unidade (JUnit 5 parametrizado, `Clock` fixo) → WireMock (contrato HTTP) → integração/API com PostgreSQL real
(Testcontainers + MockMvc) → concorrência (20 threads, contagem no banco) → matriz de autorização (130 células)
→ BDD (Cucumber) → TDD (RN-09) → mutação manual (7 mutantes) → carga (JMeter) → análise estática (SonarCloud).
Detalhes em [`testes/plano-de-testes.md`](testes/plano-de-testes.md).

## 4. Resultados × metas
| Meta (especificação) | Medido | Origem | Situação | Interpretação e limite |
|---|---|---|---|---|
| ≥ 80% cobertura de linhas | **94,0%** (685/729) | JaCoCo, CI run `43470422117` (commit `1a75685`, conteúdo de código igual ao da tag), passo "Resumo da cobertura" | Atende | gate do JaCoCo quebra o build abaixo de 80%; cobertura não prova qualidade sozinha → ver mutação |
| ≥ 70% cobertura de branches | **83,3%** (190/228) | idem | Atende | idem |
| 100% dos requisitos críticos na RTM | **7/7 = 100%** | [`rtm.md`](rtm.md) | Atende | cada um com risco, teste localizável, execução no CI e mutante morto |
| 0 bugs / vulnerabilidades críticas | **0 bugs, 0 vulnerabilidades, 0 code smells**; Quality Gate **OK**; duplicação 0,0%; cobertura Sonar 91,4% | SonarCloud, CI run `43470578195` (PR #2), passo "Quality Gate e issues do SonarCloud" | Atende | regras padrão "Sonar way"; análise estática não substitui revisão humana |
| Testes automatizados | **104** unidade + **205** integração/BDD = **309**, 0 falhas | Surefire/Failsafe, CI run `43470422117` | — | — |
| Eficácia dos testes nas regras críticas | **7/7 mutantes mortos** | [`testes/mutacao-manual.md`](testes/mutacao-manual.md), run `43467811234` | — | M6 (auditoria) detectado por um único teste → ponto de melhoria |
| RNF-PERF-01: p95 ≤ 300 ms, 50 usuários | consulta **108 ms**, reserva **224 ms**, login **344 ms** | JMeter, run `43469480829` | **Atende com ressalva** (login fora) | mesma máquina para app, banco e carga; uma rodada; não generalizar para produção |
| Erro real < 1% sob carga | **0,00%** (48.337 amostras; 0 respostas 5xx) | idem | Atende | 409 contado como resposta esperada |
| No máximo 1 reserva ativa no recurso disputado | **1** (consulta no banco após a carga) | idem | Atende | — |
| E2E da interface | **não medido** | — | — | sem ferramenta de navegador no escopo (risco aceito) |

## 5. Defeitos por severidade e situação
| Defeito | Severidade | Prioridade | Situação | Teste de regressão |
|---|---|---|---|---|
| [DEF-001](defeitos/DEF-001.md) timeout classificado como resposta inválida | Média | P2 | Fechado | `HttpNotificacaoClientTest.timeout` |
| [DEF-002](defeitos/DEF-002.md) deadlock → HTTP 500 em reservas simultâneas | **Alta** | **P1** | Fechado (reaberto 1×) | `ConcorrenciaReservaIT` |

Os dois foram encontrados **pelos próprios testes automatizados no CI**, registrados antes da correção e
corrigidos em commits que referenciam o defeito. O DEF-002 mostra o valor de repetir o teste de concorrência:
ele passou na primeira execução e só falhou na segunda (intermitente).

## 6. Riscos residuais
| Risco | Impacto | Por que é aceitável agora | Responsável |
|---|---|---|---|
| Login com p95 de 344 ms (meta 300 ms) | baixo: 1 login por sessão de 8 h | custo do BCrypt é proteção contra força bruta (trade-off registrado) | Leonardo |
| Interface web sem teste E2E automatizado (RF13 parcial) | médio: erro de JS/layout não para o CI | interface só consome a API, que é 100% testada; roteiro manual na demonstração | Leonardo |
| Notificação com falha não é reenviada | baixo: aviso pode não chegar | aprovação vale mesmo assim; registro `FALHA` permite reenvio manual | Leonardo |
| Teste de carga só no runner do CI | médio: números não valem para produção | não existe ambiente compartilhado | Leonardo |
| Revisão por pares limitada (trabalho individual) | médio | PRs com check obrigatório + revisão do autor; mutação e SonarCloud como revisores automáticos | Leonardo |

## 7. Recomendação
**Liberar com ressalvas** a `v1.0.0-rc1` para demonstração e avaliação acadêmica — ver
[`decisao-qualidade.md`](decisao-qualidade.md). **Não** declarar "pronto para produção": as medidas vêm de
ambiente de CI, não de produção.
