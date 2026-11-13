# Evidência de TDD — RN-09 (participantes ≤ capacidade da sala)

**Regra nova:** ao reservar, o Solicitante pode informar `participantes`; o número não pode exceder a
capacidade do recurso (`400 CAPACIDADE_EXCEDIDA`) e precisa ser maior que zero (`400 DADOS_INVALIDOS`).
Fronteira testada em trinca: 39 / 40 / 41 na Sala 101 (capacidade 40).

Desenvolvida no pull request **#1** (`feature/capacidade-tdd`), com um commit por fase e o CI rodando em cada uma.
O PR foi mesclado na `main` em `1a75685` depois do check `qualidade / test` verde. Como a branch foi rebaseada
sobre a correção do DEF-002, os commits na `main` têm hashes novos (`1fb4ee0` red, `b84e9c5` green, `7ab316a` refactor);
os hashes da tabela são os originais, que são os que o CI executou em cada fase.

| Fase | Commit | O que mudou | CI | Saída do teste (trecho) |
|---|---|---|---|---|
| **Red** | `f257d52` `test(red): RN-09 ...` | só o teste `CapacidadeSalaIT` (nenhum código de produção) | run `43467898634` — **falhou** (esperado) | `Response status expected:<400> but was:<201>` — falha de **asserção**, não de compilação |
| **Green** | `52acb5d` `feat(green): RN-09 ...` | campo `participantes` (DTO, entidade, migration `V3`), validação mínima no serviço | run `43468500871` — **verde** | `Tests run: 5, Failures: 0` em `CapacidadeSalaIT` |
| **Refactor** | `857c32d` `refactor: move a regra RN-09 para o domínio` | regra movida para `Recurso.validarLotacao`, mensagem com nome e capacidade; comportamento igual | run `43468796735` falhou **por outro motivo** (DEF-002, concorrência, ainda não corrigido na `main`); após rebase sobre a correção, run `43470055291` **verde** | `CapacidadeSalaIT` verde em ambos |

**Regras do TDD respeitadas:** um teste por vez; o teste não foi alterado para passar; a fase vermelha não
foi pulada (o CI registrou a falha); a refatoração só aconteceu com tudo verde.

**Observação honesta:** o arquivo da migration `V3__participantes.sql` acabou indo junto num commit de
documentação da `main` (por um `git add -A`), antes do commit *green*. Ele só adiciona uma coluna opcional
e não muda nenhum comportamento — o teste continuou vermelho até o commit *green*, como mostra o CI.
