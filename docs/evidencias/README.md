# Pacote de evidências — `v1.0.0-rc1`

Todas as evidências apontam para o **commit da tag `v1.0.0-rc1`** (ou para execuções anteriores citadas
explicitamente, quando a evidência é histórica — ex.: a fase vermelha do TDD).

| Evidência | Onde |
|---|---|
| **Execução do CI no commit da tag** (testes, gate JaCoCo, carga, wrapper Windows) | https://github.com/ILeozeraI/organizacao-recursos/actions?query=branch%3Av1.0.0-rc1 |
| Relatório do **JaCoCo** | artifact `jacoco-<n>` dessa execução (HTML + CSV + XML); resumo na aba *Summary* |
| Relatórios de teste (Surefire/Failsafe) | artifact `relatorios-testes-<n>` dessa execução |
| Resultado da carga (`.jtl`, HTML, `app.log`) | artifact `carga-<n>` dessa execução |
| **Quality Gate do SonarCloud** | https://sonarcloud.io/summary/overall?id=ILeozeraI_organizacao-recursos&branch=main |
| Resultado de desempenho analisado | [`../performance/resultado-01.md`](../performance/resultado-01.md) |
| RTM | [`../rtm.md`](../rtm.md) |
| Defeitos | [`../defeitos/`](../defeitos/) |
| Relatório de qualidade | [`../relatorio-qualidade.md`](../relatorio-qualidade.md) |
| Decisão de qualidade | [`../decisao-qualidade.md`](../decisao-qualidade.md) |
| Mutação manual (run `43467811234`) | [`../testes/mutacao-manual.md`](../testes/mutacao-manual.md) |
| TDD (PR #1, runs vermelho/verde) | [`../testes/evidencia-tdd.md`](../testes/evidencia-tdd.md) |
| BDD | [`../testes/bdd-aprovacao-bloqueio.md`](../testes/bdd-aprovacao-bloqueio.md) |
| Matriz de autorização | [`../seguranca/matriz-autorizacao.md`](../seguranca/matriz-autorizacao.md) |
| Status dos requisitos | [`../status-requisitos.md`](../status-requisitos.md) |
| Arquitetura e ADRs | [`../arquitetura.md`](../arquitetura.md), [`../adr/`](../adr/) |
| Banco de perguntas da prova oral | [`../prova-oral/perguntas.md`](../prova-oral/perguntas.md) |
| Changelog | [`../../CHANGELOG.md`](../../CHANGELOG.md) |

Regra de versões: correções depois da tag vão para `v1.0.0-rc2`; a tag `v1.0.0-rc1` nunca é movida.
