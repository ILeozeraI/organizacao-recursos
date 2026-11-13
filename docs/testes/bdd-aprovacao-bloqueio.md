# BDD — Aprovação de recursos restritos e bloqueio por manutenção

Arquivo executável: [`src/test/resources/features/aprovacao-bloqueio.feature`](../../src/test/resources/features/aprovacao-bloqueio.feature)
(Cucumber-JVM 7, palavras-chave em português). Os passos (`AprovacaoBloqueioSteps`) chamam a **API real**
via MockMvc com token de cada perfil, sobre PostgreSQL em container, e usam as mesmas palavras do cenário.
Rodam no `verify` e no CI pelo `RunCucumberIT` (relatório HTML em `target/cucumber-report.html`).

| Cenário (Dado / Quando / Então) | Regra | Resultado no CI |
|---|---|---|
| Recurso restrito fica aguardando aprovação | RN-03 / RF7 | passou |
| Responsável aprova a reserva de recurso restrito | RN-03 / RF7 | passou |
| Solicitante não aprova reserva de recurso restrito | RN-03 / RF1 | passou |
| Recurso em manutenção não pode ser reservado | RN-04 / RF8 | passou |
| Fronteiras da manutenção (4 exemplos: fim exato no início, início exato no fim, cruzamento de 1 s em cada ponta) | RN-04 / RF8 | passou |

Cada cenário tem **um único Quando** e um **Então observável** (status HTTP, código do erro ou estado no banco).

**Prova de que os cenários detectam quebra:** no workflow de mutação manual, os mutantes M3 (aprovação) e M4
(manutenção) fizeram cenários desta feature falharem (ver [`mutacao-manual.md`](mutacao-manual.md)).
