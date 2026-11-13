# Banco de perguntas da banca (específicas deste projeto)

Para estudar para a arguição (Semana 47). Para cada pergunta: **onde está a resposta** e como seria uma
**resposta fraca**. Responda sem ler; depois confira no arquivo.

| # | Pergunta | Onde está a resposta | Resposta fraca seria… |
|---|---|---|---|
| 1 | Qual mecanismo impede a dupla reserva e qual teste prova isso? | `V1__schema.sql` (constraints `EXCLUDE`), `ReservaService.comProtecaoDeConcorrencia`, `ConcorrenciaReservaIT`, ADR-002 | "o sistema verifica se já existe reserva" (isso falha sob concorrência) |
| 2 | Por que a verificação na aplicação não basta? | ADR-002; mutante M2 em `docs/testes/mutacao-manual.md` | não citar a condição de corrida entre "verificar" e "gravar" |
| 3 | O que foi o DEF-002 e por que a primeira correção não resolveu? | `docs/defeitos/DEF-002.md` | "era um bug e eu corrigi" (sem causa raiz, sem falar da ordem de travamento) |
| 4 | Por que 10h–12h e 12h–13h não conflitam? | `Intervalo` (semiaberto `[)`), `IntervaloTest`, `tstzrange(...,'[)')` | "porque são horários diferentes" |
| 5 | Por que mock no `NotificacaoServiceTest`, WireMock no cliente HTTP e Testcontainers no resto? | `docs/testes/plano-de-testes.md` §3; especificação (mocks só em unidade isolada) | "porque sim" / não saber o que cada dublê prova |
| 6 | Qual teste falha se a aprovação de recurso restrito for removida? | mutante M3 (`mutacao-manual.md`): `ReservaApiIT`, BDD, `AuditoriaIT`... | "acho que algum teste pega" |
| 7 | O que o p95 de 224 ms da reserva permite e NÃO permite afirmar? | `docs/performance/resultado-01.md` (limites) | "o sistema aguenta produção" |
| 8 | Por que o 409 não foi contado como erro no JMeter? | `docs/performance/rnf.md` | não diferenciar resposta esperada de falha |
| 9 | Por que o login ficou acima de 300 ms e o que você faria? | `resultado-01.md` (hipótese BCrypt) e trade-off no ATAM | "o servidor é lento" |
| 10 | Como a auditoria garante que não fica registro órfão? | `AuditoriaService` (`Propagation.MANDATORY`), `AuditoriaIT.rollbackDaMudancaNaoDeixaAuditoriaOrfa` | "eu salvo a auditoria depois" |
| 11 | Como o teste de autorização garante que nenhuma célula foi apagada para passar? | `MatrizAutorizacaoIT` lê `docs/seguranca/matriz-autorizacao.md` | "testei algumas rotas" |
| 12 | No instante exato da expiração, o token vale? Como você testou tempo sem `sleep`? | `ValidadorToken`, `ValidadorTokenTest` (−1/0/+1 s, `Clock.fixed`) | "usei Thread.sleep" |
| 13 | Como você chegou em 93,9% de linhas e por que isso não prova qualidade sozinho? | resumo JaCoCo no CI; mutação manual; Aula 07 | "cobertura alta = sem bugs" |
| 14 | Qual o risco residual que você aceitou e quem é o responsável? | `docs/decisao-qualidade.md` | "não tem risco" |
| 15 | Como foi o ciclo de TDD da RN-09? Mostre o commit vermelho. | `docs/testes/evidencia-tdd.md`, PR #1 | não saber mostrar o run vermelho |

## Roteiro de 8 minutos
1. **Problema e risco** (1 min): conflito de horário; limitadores de nota.
2. **Arquitetura** (1 min): C4 em `docs/arquitetura.md`, duas camadas contra dupla reserva.
3. **Demonstração** (2 min): login como Ana → reservar Lab 3 (fica SOLICITADA) → Rita aprova → tentar sobrepor (409).
4. **Testes e CI** (1,5 min): run verde da tag, 304 testes, cobertura, SonarCloud.
5. **RTM e relatório** (1,5 min): uma linha da RTM até o teste; DEF-002.
6. **Limitações** (1 min): RF13 sem E2E, login acima da meta, carga só no runner.
