# Documentação da API (RF14)

A documentação **viva** fica no Swagger da aplicação: `http://localhost:8080/swagger-ui.html`
(contrato OpenAPI em `/v3/api-docs`). Este arquivo resume os fluxos públicos.

## Convenções
- JSON em UTF-8; datas em **ISO-8601 UTC** (`2026-12-01T10:00:00Z`).
- Autenticação: `Authorization: Bearer <token>` obtido em `POST /auth/login`.
- Erros sempre no formato **ProblemDetail** (RFC 9457) com um `codigo` estável:
```json
{ "status": 409, "title": "Conflict", "codigo": "CONFLITO_HORARIO",
  "detail": "Já existe uma reserva para este recurso neste horário." }
```
Nenhuma resposta de erro contém stack trace, SQL ou nome de classe interna.

## Fluxo 1 — Login
`POST /auth/login` `{"email":"ana@exemplo.invalid","senha":"senha123"}` → `200 {token, expiraEm, usuarioId, nome, perfil}`
Erros: `401 CREDENCIAIS_INVALIDAS` (mesma mensagem para e-mail inexistente e senha errada), `400 DADOS_INVALIDOS`.
Token inválido/expirado/revogado em qualquer rota → `401 TOKEN_INVALIDO | TOKEN_EXPIRADO | TOKEN_REVOGADO`.
`POST /auth/logout` revoga o token (`204`).

## Fluxo 2 — Pesquisar e reservar
1. `GET /recursos?tipo=SALA&capacidadeMinima=30&localizacao=bloco&competencia=&disponivelInicio=...&disponivelFim=...`
2. `POST /reservas` `{"recursoId":1,"professorId":6,"inicio":"...","fim":"...","finalidade":"...","participantes":30}`
   - `201` com `status` = `APROVADA` (recurso comum) ou `SOLICITADA` (recurso restrito)
   - `409 CONFLITO_HORARIO` · `409 CONFLITO_AGENDA_PROFESSOR` · `409 RECURSO_EM_MANUTENCAO`
   - `400 INTERVALO_INVALIDO` (término ≤ início) · `400 CAPACIDADE_EXCEDIDA` · `400 RECURSO_INVALIDO` · `400 PROFESSOR_INVALIDO`
3. `PUT /reservas/{id}` `{"inicio","fim"}` (só o dono; `409 ALTERACAO_NAO_PERMITIDA` se restrita já aprovada)

## Fluxo 3 — Ciclo de vida
```
SOLICITADA --aprovar--> APROVADA --iniciar/retirada--> EM_USO --concluir/devolucao--> CONCLUIDA
    |  \--rejeitar--> REJEITADA       |--cancelar--> CANCELADA
    \--cancelar--> CANCELADA          \--nao-compareceu--> NAO_COMPARECEU
```
Transição inválida → `409 TRANSICAO_INVALIDA`. Toda transição gera registro em `GET /reservas/{id}/historico`.
`DELETE /reservas/{id}` (Administrador) → `409 RESERVA_INICIADA` se já iniciada.

## Fluxo 4 — Relatório
`GET /relatorios/utilizacao?inicio=2026-12-01T00:00:00Z&fim=2026-12-02T00:00:00Z` →
`{recursos:[{recursoId,nome,horasReservadas}], professores:[{professorId,nome,horasAlocadas}], conflitosEvitados}`
Contam APROVADA, EM_USO e CONCLUIDA; reservas que cruzam a borda contam só a parte dentro do período.

## Permissões
Ver [`seguranca/matriz-autorizacao.md`](seguranca/matriz-autorizacao.md).
