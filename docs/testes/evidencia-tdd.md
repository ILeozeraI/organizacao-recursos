# Evidência de TDD — RN-09 (participantes ≤ capacidade da sala)

**Regra nova:** ao reservar, o Solicitante pode informar `participantes`; o número não pode exceder a
capacidade do recurso (`400 CAPACIDADE_EXCEDIDA`) e precisa ser maior que zero (`400 DADOS_INVALIDOS`).
Fronteira testada em trinca: 39 / 40 / 41 na Sala 101 (capacidade 40).

Desenvolvida no pull request **#1** (`feature/capacidade-tdd`), com um commit por fase e o CI rodando em cada uma.

| Fase | Commit | O que mudou | CI | Saída do teste (trecho) |
|---|---|---|---|---|
| **Red** | `test(red): RN-09 ...` | só o teste `CapacidadeSalaIT` (nenhum código de produção) | run `43467898634` — **falhou** (esperado) | `Response status expected:<400> but was:<201>` — falha de **asserção**, não de compilação |
| **Green** | `feat(green): RN-09 ...` | campo `participantes` (DTO, entidade, migration `V3`), validação mínima no serviço | run `43468500871` — **verde** | `Tests run: 5, Failures: 0` em `CapacidadeSalaIT` |
| **Refactor** | `refactor: move a regra RN-09 para o domínio` | regra movida para `Recurso.validarLotacao`, mensagem com nome e capacidade; comportamento igual | ver PR #1 (último check) | mesma suíte verde |

**Regras do TDD respeitadas:** um teste por vez; o teste não foi alterado para passar; a fase vermelha não
foi pulada (o CI registrou a falha); a refatoração só aconteceu com tudo verde.

**Observação honesta:** o arquivo da migration `V3__participantes.sql` acabou indo junto num commit de
documentação da `main` (por um `git add -A`), antes do commit *green*. Ele só adiciona uma coluna opcional
e não muda nenhum comportamento — o teste continuou vermelho até o commit *green*, como mostra o CI.
