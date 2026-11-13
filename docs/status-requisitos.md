# Situação dos requisitos obrigatórios (RF1–RF14)

Critério (Aula 11): **"feito"** só quando existe teste que **falharia** se a regra fosse quebrada.
Evidência de execução: CI do commit avaliado (ver [`evidencias/README.md`](evidencias/README.md)).

| Requisito | Situação | Evidência (arquivo · teste) | Risco se ficasse de fora |
|---|---|---|---|
| RF1 Autenticação e autorização por perfil | Feito | `AutenticacaoIT` (8), `ValidadorTokenTest` (11), `MatrizAutorizacaoIT` (130 células) | **limitador de nota**: acesso indevido |
| RF2 Cadastro e consulta de salas, professores e materiais | Feito | `RecursoApiIT.adminCadastraConsultaAtualizaEDesativa`, `naoDesativaRecursoComReservaAtiva` | sem recursos não há reserva |
| RF3 Pesquisa por tipo, capacidade, localização, competência e disponibilidade | Feito | `RecursoApiIT.pesquisaPorTipoCapacidade...`, `pesquisaPorDisponibilidade...` | usuário não encontra recurso livre |
| RF4 Criação, alteração e cancelamento | Feito | `ReservaApiIT` (Criacao, Alteracao, CicloDeVida) | função central ausente |
| RF5 Sobreposição (inclui agenda do professor) | Feito | `IntervaloTest.detectaSobreposicao` (7 fronteiras), `ReservaApiIT.Sobreposicao` (4) | conflito de horário |
| RF6 Dupla reserva em solicitações simultâneas | Feito | `ConcorrenciaReservaIT` (2 cenários × 5 repetições, contagem no banco) | **limitador de nota**: dupla reserva |
| RF7 Aprovação obrigatória para recursos restritos | Feito | `ReservaApiIT.recursoRestritoFicaSolicitado...`, BDD `aprovacao-bloqueio.feature` | uso de laboratório sem autorização |
| RF8 Bloqueio por manutenção | Feito | `ReservaApiIT.naoReservaRecursoEmManutencao...`, BDD (4 fronteiras) | reserva de sala interditada |
| RF9 Retirada e devolução de materiais | Feito | `ReservaApiIT.Materiais` (2) | perda de controle de equipamentos |
| RF10 Histórico auditável | Feito | `AuditoriaIT` (5, inclui rollback sem auditoria órfã) | sem rastreabilidade |
| RF11 Notificação simulada / API externa | Feito | `HttpNotificacaoClientTest` (INT-01..05 + vazamento de token), `NotificacaoServiceTest` | solicitante não é avisado |
| RF12 Relatório de utilização | Feito | `RelatorioUtilizacaoIT` (oráculo calculado à mão), `CalculadoraUtilizacaoTest` | gestão sem dados |
| RF13 Interface responsiva e mensagens compreensíveis | **Parcial** | mensagens: `ReservaApiIT.Validacao`, `MatrizAutorizacaoIT` (sem vazamento). Interface: `src/main/resources/static` (CSS responsivo) — **sem teste automatizado de navegador** | layout quebrar sem o CI perceber |
| RF14 Documentação da API | Feito | Swagger `/swagger-ui.html`; `AutenticacaoIT.documentacaoDaApiEInterfaceSaoPublicas`; `docs/api.md`; README | integração difícil |

**Regras críticas da especificação:** sobreposição, dupla reserva, aprovação de restrito, manutenção,
autorização e auditoria → **6 de 6 com teste e mutação morta** ([`testes/mutacao-manual.md`](testes/mutacao-manual.md)).

**Pendências (ordenadas pelos limitadores de nota):** nenhuma pendência nos limitadores (aplicação executa,
pipeline existe, autorização testada, dupla reserva impossível). Pendência restante: RF13 sem teste E2E de
interface (risco aceito, ver decisão de qualidade).
