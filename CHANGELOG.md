# Changelog

## [1.0.0-rc1] — 2026-11-13
Primeira release candidate do sistema de Organização de Recursos.

### Funcionalidades
- RF1 Autenticação por token opaco revogável e autorização por perfil (Solicitante, Responsável, Administrador).
- RF2/RF3 Cadastro e pesquisa de salas, professores e materiais (tipo, capacidade, localização, competência, disponibilidade).
- RF4 Criação, alteração e cancelamento de reservas, com regras de posse.
- RF5/RF6 Detecção de sobreposição (recurso e agenda do professor) e proteção contra dupla reserva concorrente
  (constraint `EXCLUDE` + trava consultiva em ordem fixa).
- RF7 Aprovação obrigatória de recursos restritos pelo Responsável.
- RF8 Bloqueio por período de manutenção.
- RF9 Retirada e devolução de materiais.
- RF10 Histórico auditável de todas as mudanças de estado (mesma transação).
- RF11 Notificação ao solicitante via API externa simulada, com timeout e degradação graciosa.
- RF12 Relatório de utilização por recurso, carga horária por professor e conflitos evitados.
- RF13 Interface web responsiva e erros em ProblemDetail com mensagens compreensíveis.
- RF14 Documentação OpenAPI/Swagger e `docs/api.md`.
- RN-09 Número de participantes limitado à capacidade da sala (desenvolvida com TDD).

### Qualidade
- Suíte com testes de unidade, integração (PostgreSQL via Testcontainers), WireMock, concorrência, matriz de
  autorização e BDD (Cucumber); gate JaCoCo 80% linhas / 70% branches; SonarCloud; JMeter no CI.

### Defeitos corrigidos
- DEF-001 Timeout da notificação classificado como resposta inválida.
- DEF-002 Deadlock em reservas simultâneas do mesmo professor gerava HTTP 500 (reaberto uma vez).
