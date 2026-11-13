# Matriz de autorização (perfil × operação)

Fonte única da verdade da autorização. O teste `MatrizAutorizacaoIT` lê **estas mesmas linhas**
e executa cada célula (método, rota, perfil → status esperado) com MockMvc e tokens reais.
Se uma célula mudar aqui, o teste muda junto; nenhuma célula pode ser removida para o teste passar.

**Perfis:** Anônimo (sem token) · Solicitante dono da reserva (Ana) · Outro Solicitante (Bruno) ·
Responsável (Rita) · Administrador.

**Fixture de cada célula:** banco limpo e `{R}` = reserva da Ana no *Laboratório 3* (recurso restrito),
status `SOLICITADA`.

**Legenda:** 200/201/204 = permitido · 401 = sem autenticação · 403 = perfil ou posse não permitem ·
400/409 = autorizado, mas a regra de negócio recusa (ex.: transição inválida a partir de SOLICITADA).

| Método | Rota | Corpo | Anônimo | Solicitante (dono) | Outro Solicitante | Responsável | Administrador |
|---|---|---|---|---|---|---|---|
| GET | `/recursos` | - | 401 | 200 | 200 | 200 | 200 |
| GET | `/recursos/1` | - | 401 | 200 | 200 | 200 | 200 |
| POST | `/recursos` | `{"nome":"Sala Nova","tipo":"SALA","capacidade":10}` | 401 | 403 | 403 | 403 | 201 |
| PUT | `/recursos/1` | `{"nome":"Sala 101","tipo":"SALA","capacidade":40}` | 401 | 403 | 403 | 403 | 200 |
| DELETE | `/recursos/5` | - | 401 | 403 | 403 | 403 | 204 |
| POST | `/recursos/1/manutencoes` | `{"inicio":"2026-12-05T08:00:00Z","fim":"2026-12-05T12:00:00Z","motivo":"Limpeza"}` | 401 | 403 | 403 | 403 | 201 |
| GET | `/recursos/1/manutencoes` | - | 401 | 200 | 200 | 200 | 200 |
| POST | `/reservas` | `{"recursoId":1,"inicio":"2026-12-02T10:00:00Z","fim":"2026-12-02T11:00:00Z"}` | 401 | 201 | 201 | 403 | 403 |
| GET | `/reservas` | - | 401 | 200 | 200 | 200 | 200 |
| GET | `/reservas/{R}` | - | 401 | 200 | 403 | 200 | 200 |
| PUT | `/reservas/{R}` | `{"inicio":"2026-12-01T14:00:00Z","fim":"2026-12-01T15:00:00Z"}` | 401 | 200 | 403 | 403 | 403 |
| POST | `/reservas/{R}/cancelar` | - | 401 | 200 | 403 | 200 | 200 |
| POST | `/reservas/{R}/aprovar` | - | 401 | 403 | 403 | 200 | 403 |
| POST | `/reservas/{R}/rejeitar` | - | 401 | 403 | 403 | 200 | 403 |
| POST | `/reservas/{R}/iniciar` | - | 401 | 409 | 403 | 409 | 403 |
| POST | `/reservas/{R}/concluir` | - | 401 | 409 | 403 | 409 | 403 |
| POST | `/reservas/{R}/nao-compareceu` | - | 401 | 403 | 403 | 409 | 403 |
| POST | `/reservas/{R}/retirada` | - | 401 | 403 | 403 | 400 | 403 |
| POST | `/reservas/{R}/devolucao` | - | 401 | 403 | 403 | 400 | 403 |
| GET | `/reservas/{R}/movimentacoes` | - | 401 | 200 | 403 | 200 | 200 |
| GET | `/reservas/{R}/historico` | - | 401 | 403 | 403 | 200 | 200 |
| DELETE | `/reservas/{R}` | - | 401 | 403 | 403 | 403 | 204 |
| GET | `/relatorios/utilizacao?inicio=2026-12-01T00:00:00Z&fim=2026-12-02T00:00:00Z` | - | 401 | 403 | 403 | 200 | 200 |
| GET | `/auth/eu` | - | 401 | 200 | 200 | 200 | 200 |
| POST | `/auth/logout` | - | 401 | 204 | 204 | 204 | 204 |
| GET | `/v3/api-docs` | - | 200 | 200 | 200 | 200 | 200 |

## Regras de posse (aplicadas no `ReservaService`)

- O Solicitante só consulta, altera e cancela as **próprias** reservas (`NAO_E_DONO`, 403).
- Só o Responsável aprova recurso restrito; o solicitante nunca aprova a própria reserva (`APROVACAO_PROPRIA`).
- O Administrador pode cancelar qualquer reserva, mas não inicia nem conclui uso.

## Rotas sem regra específica de segurança

Nenhuma rota fica aberta por esquecimento: tudo que não está listado na `SecurityConfig` cai em
`anyRequest().authenticated()`. Rotas **públicas de propósito**: `POST /auth/login`, interface web
(`/`, `/index.html`, `/app.js`, `/app.css`) e documentação (`/v3/api-docs/**`, `/swagger-ui/**`).

## Divergências entre código e matriz

Registradas em `docs/defeitos/` quando o teste da matriz encontrou célula vermelha
(cada defeito foi registrado antes da correção). Situação atual: nenhuma divergência aberta.
