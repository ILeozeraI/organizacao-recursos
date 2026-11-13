# Mutação manual das regras críticas (Aula 12, Prompt 3)

**Como foi feito:** o workflow [`mutacao-manual`](../../.github/workflows/mutacao-manual.yml) cria um job por
regra crítica. Cada job **quebra a regra no código de produção** (só na cópia do runner — o repositório não é
alterado, equivalente a fazer a quebra e desfazer com `git restore`), roda a suíte inteira e registra quais
testes falharam. Mutante **morto** = algum teste detectou a quebra. Mutante **vivo** = lacuna de teste.

**Execução:** GitHub Actions run `43467811234` (workflow_dispatch na `main`, 13/11/2026).

| ID | Regra crítica | Quebra feita | Testes que falharam (detectaram) | Resultado |
|---|---|---|---|---|
| M1 | Sobreposição (intervalo semiaberto) | `Intervalo.sobrepoe` passou a considerar intervalos encostados como sobrepostos (`!isAfter` no lugar de `isBefore`) | `IntervaloTest.detectaSobreposicao` [1] e [3] (fim exato no início / início exato no fim) | **Morto** |
| M2 | Dupla reserva | Removida a constraint `EXCLUDE` do recurso na migration `V1` | `ConcorrenciaReservaIT.mesmaSalaMesmoHorario` — falhou nas **5 de 5** repetições (mais de uma reserva aceita) | **Morto** |
| M3 | Aprovação de recurso restrito | Status inicial sempre `APROVADA` | `ReservaApiIT` (3), `AuditoriaIT` (2), `RelatorioUtilizacaoIT` (1), `MatrizAutorizacaoIT` (6 células), 3 cenários BDD | **Morto** |
| M4 | Bloqueio por manutenção | Verificação de manutenção desligada (`if (false && ...)`) | `ReservaApiIT.naoReservaRecursoEmManutencao...`, 3 cenários BDD (incl. exemplos de fronteira) | **Morto** |
| M5 | Autorização | Removida a regra "só Solicitante cria reserva" da `SecurityConfig` | `MatrizAutorizacaoIT` células [39] e [40] (Responsável e Administrador criando reserva) | **Morto** |
| M6 | Auditoria | Removido o registro de auditoria nas transições de estado | `AuditoriaIT.cadaMudancaDeEstadoGeraRegistro...` | **Morto** |
| M7 | Posse | Removida a checagem "Solicitante só vê as próprias reservas" | `ReservaApiIT.solicitanteNaoAlteraNemCancela...`, `MatrizAutorizacaoIT` [48] e [98] | **Morto** |

**Resultado:** 7 de 7 mutantes mortos → **100% das regras críticas têm teste que falha quando a regra é quebrada.**

**Observações honestas:**
- O M2 só é detectado pelo teste de **concorrência**: com a constraint removida, a verificação na aplicação
  ainda barra pedidos sequenciais. Isso confirma que a constraint do banco é a única proteção real contra corrida.
- O M6 foi detectado por um único teste. Ponto de atenção: um teste a mais sobre auditoria de `rejeitar`
  e de retirada/devolução reduziria esse risco (registrado como melhoria no relatório de qualidade).
