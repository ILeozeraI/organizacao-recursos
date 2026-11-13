# RTM — Matriz de rastreabilidade (requisito → risco → teste → evidência)

**Commit avaliado:** tag `v1.0.0-rc1` · **Evidência de execução:** [execução do CI da tag `v1.0.0-rc1`](https://github.com/ILeozeraI/organizacao-recursos/actions?query=branch%3Av1.0.0-rc1) (todos os testes abaixo
rodam no `./mvnw -B verify` desse run). Caminhos de teste relativos a `src/test/java/br/senac/recursos/`.

## Requisitos críticos (100% com teste localizável, execução no CI e mutação morta)
| ID | Regra crítica | Risco | Teste(s) localizável(is) | Nível | Mutação | Status |
|---|---|---|---|---|---|---|
| RN-01 | Término posterior ao início | intervalo vazio/invertido gera reserva inválida | `unidade/IntervaloTest.rejeitaTerminoNaoPosterior`, `integracao/ReservaApiIT$Validacao.terminoAntesDoInicioDa400...` | unidade, API | — | Passou |
| RN-02 / RF5 | **Sobreposição** (recurso e professor), intervalo `[início, fim)` | conflito de horário | `unidade/IntervaloTest.detectaSobreposicao` (7 fronteiras), `integracao/ReservaApiIT$Sobreposicao` (4) | unidade, API | M1 morto | Passou |
| RF6 | **Dupla reserva** sob concorrência | duas reservas aceitas no mesmo horário (limitador de nota) | `integracao/ConcorrenciaReservaIT.mesmaSalaMesmoHorario`, `.mesmoProfessorEmSalasDiferentes` (5× cada) | integração/concorrência | M2 morto | Passou |
| RN-03 / RF7 | **Aprovação de restrito** só pelo Responsável; solicitante não aprova a própria | uso não autorizado de laboratório | `integracao/ReservaApiIT$Criacao.recursoRestritoFicaSolicitado...`, BDD `features/aprovacao-bloqueio.feature` (3 cenários) | API, BDD | M3 morto | Passou |
| RN-04 / RF8 | **Manutenção** bloqueia reserva (fronteiras semiabertas) | reserva de sala interditada | `integracao/ReservaApiIT$Manutencao...`, BDD (cenário + 4 exemplos) | API, BDD | M4 morto | Passou |
| RF1 | **Autorização** por perfil e **posse** | acesso indevido (limitador de nota) | `integracao/MatrizAutorizacaoIT.celulaDaMatriz` (130 células lidas de `docs/seguranca/matriz-autorizacao.md`), `integracao/ReservaApiIT$Alteracao.solicitanteNaoAlteraNemCancela...`, `integracao/AutenticacaoIT` | API | M5, M7 mortos | Passou |
| RN-05 / RF10 | **Auditoria** de toda mudança de estado, mesma transação | perda de rastreabilidade; auditoria órfã | `integracao/AuditoriaIT` (5, inclui rollback) | integração | M6 morto | Passou |

## Demais requisitos
| ID | Requisito | Risco | Teste(s) | Nível | Status |
|---|---|---|---|---|---|
| RN-06 | Máquina de estados (49 combinações) | transição proibida aceita | `unidade/StatusReservaTest.respeitaMatrizDeTransicoes` | unidade | Passou |
| RN-07 | Reserva iniciada não pode ser excluída | perda de histórico de uso | `integracao/ReservaApiIT$CicloDeVida.reservaIniciadaNaoPodeSerExcluida` | API | Passou |
| RN-08 | Token: ausente/inválido/expirado(≤ agora)/revogado | sessão vencida liberando acesso | `unidade/ValidadorTokenTest` (−1/0/+1 s), `integracao/AutenticacaoIT` | unidade, API | Passou |
| RN-09 | Participantes ≤ capacidade (TDD) | superlotação | `integracao/CapacidadeSalaIT` (39/40/41, 0/−1) | API | Passou |
| RF2 | Cadastro/consulta de recursos | — | `integracao/RecursoApiIT.adminCadastraConsultaAtualizaEDesativa` | API | Passou |
| RF3 | Pesquisa por filtros e disponibilidade | — | `integracao/RecursoApiIT.pesquisaPor...` (2) | API | Passou |
| RF4 | Criar, alterar, cancelar | — | `integracao/ReservaApiIT$Alteracao`, `$CicloDeVida` | API | Passou |
| RF9 | Retirada e devolução | material sem controle | `integracao/ReservaApiIT$Materiais` | API | Passou |
| RF11 / RNF-INT-01 | Notificação externa e degradação | provedor fora derruba a API; vazamento de token | `unidade/HttpNotificacaoClientTest` (INT-01..05), `unidade/NotificacaoServiceTest`, `integracao/ReservaApiIT$Criacao.recursoRestritoFicaSolicitado...` (aprovação vale com provedor fora) | integração (WireMock) | Passou |
| RF12 | Relatório de utilização | número errado para a gestão | `integracao/RelatorioUtilizacaoIT` (oráculo à mão), `unidade/CalculadoraUtilizacaoTest` | API, unidade | Passou |
| RF13 | Mensagens compreensíveis / erros sem vazamento | exposição de detalhes internos | `integracao/ReservaApiIT$Validacao` (4), asserção anti-vazamento em todas as 130 células da matriz | API | Passou (interface: parcial, sem E2E) |
| RF14 | Documentação da API | — | `integracao/AutenticacaoIT.documentacaoDaApiEInterfaceSaoPublicas` | API | Passou |
| RNF-PERF-01 | p95 ≤ 300 ms, 50 usuários, erro real < 1% | lentidão no pico | `perf/reservas.jmx` → [`performance/resultado-01.md`](performance/resultado-01.md) | carga | ver resultado |
| RNF-ROB-01 | Criação de reserva responde só 201/409, nunca 500 | erro interno sob concorrência | `ConcorrenciaReservaIT` (`containsOnlyKeys(201, 409)`), assertiva do JMeter | concorrência, carga | Passou (após DEF-002) |

## Defeitos ligados à RTM
| Defeito | Requisito | Teste de regressão |
|---|---|---|
| [DEF-001](defeitos/DEF-001.md) | RF11 | `HttpNotificacaoClientTest.timeout` (INT-04) |
| [DEF-002](defeitos/DEF-002.md) | RF5, RF6, RNF-ROB-01 | `ConcorrenciaReservaIT` |

**Cobertura da RTM:** 7 de 7 requisitos críticos com risco, teste localizável e evidência de CI = **100%**.
