# Plano de testes (modelo ISO/IEC/IEEE 29119-3)

## 1. Identificação
- **Sistema:** Organização de Recursos — repositório `ILeozeraI/organizacao-recursos`
- **Responsável:** Leonardo Cavalcanti da Silva (trabalho individual)
- **Comando único de execução:** `./mvnw -B verify` (local e CI)

## 2. Escopo
**Itens a testar:** RF1–RF14 e as regras críticas (sobreposição, dupla reserva, aprovação de restrito,
manutenção, autorização/posse, auditoria), além de RNF de desempenho, segurança de erros e integração externa.

**Itens que NÃO serão testados (decisão registrada):**
| Item | Motivo | Risco aceito |
|---|---|---|
| Interface web em navegador real (E2E com Selenium/Playwright) | prazo; a interface só consome a API, que é testada em caixa-preta | layout/JS podem quebrar sem o CI perceber; mitigado por roteiro manual na demonstração |
| Provedor de notificação **real** | é simulado por especificação (RF11) | contrato real pode divergir do simulado |
| Carga em ambiente de produção | não existe ambiente compartilhado; JMeter roda só contra a aplicação no runner | resultados não generalizam para produção |

## 3. Estratégia por nível
| Nível | Ferramenta | O que cobre | Onde |
|---|---|---|---|
| Unidade | JUnit 5 (`@ParameterizedTest`, `@Nested`, `Clock.fixed`) | intervalo semiaberto, máquina de estados (49 transições), token E2, cálculo do relatório | `src/test/java/.../unidade` |
| Unidade isolada com mock | Mockito | `NotificacaoService` (mock só aqui, como pede a especificação) | `NotificacaoServiceTest` |
| Integração HTTP externa | WireMock | INT-01..05: 200, 4xx, 5xx, timeout, corpo inválido, não vazar token | `HttpNotificacaoClientTest` |
| Integração / API caixa-preta | MockMvc + PostgreSQL real (Testcontainers) | regras de reserva, recursos, auditoria, relatório, autenticação | `src/test/java/.../integracao` |
| Concorrência | `ExecutorService` + `CountDownLatch`, contagem no banco | RF6 (20 pedidos simultâneos, 5 repetições) | `ConcorrenciaReservaIT` |
| Autorização | `@ParameterizedTest @MethodSource` lendo a matriz | 26 operações × 5 perfis = 130 células | `MatrizAutorizacaoIT` |
| BDD | Cucumber (Dado/Quando/Então em português) | aprovação de restrito e fronteiras da manutenção | `features/aprovacao-bloqueio.feature` |
| TDD | ciclo red → green → refactor em commits separados | RN-09 capacidade da sala | `docs/testes/evidencia-tdd.md` |
| Mutação manual | workflow que quebra cada regra crítica | eficácia dos testes (7 mutantes) | `docs/testes/mutacao-manual.md` |
| Desempenho | JMeter 5.6.3 (CLI) | RNF-PERF-01 | `docs/performance/` |
| Análise estática | SonarCloud | bugs, vulnerabilidades, code smells, duplicação | Quality Gate no PR |

## 4. Critérios
- **Entrada:** código compila; banco sobe via Testcontainers; requisito tem identificador na RTM.
- **Saída (para liberar):** `verify` verde no CI; cobertura ≥ 80% linhas e ≥ 70% branches (gate JaCoCo);
  100% dos requisitos críticos com teste e evidência na RTM; 0 bugs/vulnerabilidades críticas no SonarCloud;
  nenhum defeito P1 aberto.
- **Suspensão:** pipeline vermelho na `main` (corrigir antes de qualquer outra entrega); teste intermitente
  (investigar a causa — ver DEF-002 — em vez de repetir até passar).

## 5. Riscos, recursos e ambiente
| Risco | Tipo | Mitigação |
|---|---|---|
| Condição de corrida na reserva | produto | constraint no banco + teste de concorrência repetido |
| Teste dependente do relógio | produto | `Clock` injetado, sem `Thread.sleep` nas regras |
| Testes dependentes entre si | projeto | banco truncado antes de cada teste |
| Trabalho individual (sem par para revisar) | projeto | PRs com checks obrigatórios e revisão do próprio autor registrada; CI como revisor automático |

**Ambiente:** GitHub Actions `ubuntu-latest`, Java 21 Temurin, Docker, PostgreSQL 16 (Testcontainers),
dados 100% fictícios (domínio `.invalid`).

## 6. Utility tree → cenários → casos de teste
Ver [`docs/arquitetura.md`](../arquitetura.md#6-atam--ciclo-01) (utility tree e cenários em 6 partes).

### Casos de teste dos cenários (A, A) e (A, M) — 7 campos
| ID | Título | Pré-condições | Dados de entrada | Passos | Resultado esperado | Rastreabilidade |
|---|---|---|---|---|---|---|
| CT-01 | Dupla reserva simultânea na mesma sala | banco limpo; Sala 101 livre; 2 solicitantes logados | 20× `{"recursoId":1,"inicio":"2026-12-01T10:00:00Z","fim":"2026-12-01T12:00:00Z"}` | 1. preparar 20 threads; 2. liberar todas juntas (latch); 3. contar status; 4. contar no banco | exatamente 1×201 e 19×409; nenhum 500; `count(*)` ativas = 1 | CEN-01, RF6 → `ConcorrenciaReservaIT.mesmaSalaMesmoHorario` |
| CT-02 | Agenda do professor em 20 salas diferentes | 20 salas livres; Prof. Carla sem reservas | 20 pedidos, salas distintas, `professorId=6`, mesmo horário | igual ao CT-01 | 1×201, demais 409; `count(*)` professor = 1 | CEN-01, RF5/RF6 → `ConcorrenciaReservaIT.mesmoProfessorEmSalasDiferentes` |
| CT-03 | Fronteiras da sobreposição | reserva existente 10:00–12:00 na Sala 101 | 12:00–13:00; 09:00–10:00; 11:59:59–13:00 | criar cada reserva | 201, 201, 409 `CONFLITO_HORARIO` | RF5 → `ReservaApiIT.aceitaReservaQueComecaExatamenteNoFimDaOutra`, `recusaSobreposicao...` |
| CT-04 | Recurso restrito exige aprovação | Laboratório 3 restrito | reserva 10:00–11:00 pela Ana | 1. criar; 2. consultar; 3. Rita aprova | 201 `SOLICITADA` → 200 `APROVADA` | CEN-02, RF7 → `ReservaApiIT.recursoRestritoFicaSolicitado...` + BDD |
| CT-05 | Solicitante altera reserva de outro | reserva da Ana | token do Bruno, `PUT /reservas/{id}` | enviar | 403 `NAO_E_DONO` | CEN-03, RF1 → `ReservaApiIT.solicitanteNaoAlteraNemCancela...` |
| CT-06 | Erro não vaza detalhes | — | JSON malformado e data "amanhã" | `POST /reservas` | 400 `REQUISICAO_INVALIDA`, corpo sem "Exception", "jackson", stack | CEN-04, RF13 → `ReservaApiIT.jsonMalformado...` + `MatrizAutorizacaoIT` |
| CT-07 | Carga com reserva disputada | app + PostgreSQL no runner | 50 usuários, ramp-up 10 s, 60 s, `perf/usuarios.csv` | `jmeter -n -t perf/reservas.jmx` | p95 ≤ 300 ms; erro real < 1%; 1 reserva ativa no banco | CEN-05, RNF-PERF-01 → `docs/performance/resultado-01.md` |
| CT-08 | Provedor de notificação lento | WireMock com atraso 2 s; read-timeout 800 ms | pedido de notificação | `enviar` | `INDISPONIVEL` em < 1,9 s | CEN-06, RF11 → `HttpNotificacaoClientTest.timeout` (INT-04) |
