# language: pt
Funcionalidade: Aprovação de recursos restritos e bloqueio por manutenção
  Como Solicitante, quero reservar recursos sabendo quando preciso de aprovação
  e quando o recurso está em manutenção, para não planejar uma aula que não vai acontecer.

  # Dados fictícios: recurso 1 = Sala 101 (livre), recurso 2 = Laboratório 3 (restrito)

  Cenário: Recurso restrito fica aguardando aprovação
    Quando "ana@exemplo.invalid" reserva o recurso 2 de "2026-12-10T10:00:00Z" até "2026-12-10T11:00:00Z"
    Então a resposta tem status 201
    E a reserva fica com status "SOLICITADA"

  Cenário: Responsável aprova a reserva de recurso restrito
    Dado que "ana@exemplo.invalid" reservou o recurso 2 de "2026-12-10T10:00:00Z" até "2026-12-10T11:00:00Z"
    Quando "rita@exemplo.invalid" aprova a reserva
    Então a resposta tem status 200
    E a reserva fica com status "APROVADA"

  Cenário: Solicitante não aprova reserva de recurso restrito
    Dado que "ana@exemplo.invalid" reservou o recurso 2 de "2026-12-10T10:00:00Z" até "2026-12-10T11:00:00Z"
    Quando "ana@exemplo.invalid" aprova a reserva
    Então a resposta tem status 403
    E a reserva fica com status "SOLICITADA"

  Cenário: Recurso em manutenção não pode ser reservado
    Dado que o recurso 1 está em manutenção de "2026-12-10T10:00:00Z" até "2026-12-10T12:00:00Z"
    Quando "ana@exemplo.invalid" reserva o recurso 1 de "2026-12-10T11:00:00Z" até "2026-12-10T13:00:00Z"
    Então a resposta tem status 409
    E o código do erro é "RECURSO_EM_MANUTENCAO"

  Esquema do Cenário: Fronteiras do período de manutenção
    Dado que o recurso 1 está em manutenção de "2026-12-10T10:00:00Z" até "2026-12-10T12:00:00Z"
    Quando "ana@exemplo.invalid" reserva o recurso 1 de "<inicio>" até "<fim>"
    Então a resposta tem status <status>

    Exemplos:
      | inicio               | fim                  | status | caso                                  |
      | 2026-12-10T09:00:00Z | 2026-12-10T10:00:00Z | 201    | termina exatamente no início          |
      | 2026-12-10T12:00:00Z | 2026-12-10T13:00:00Z | 201    | começa exatamente no fim              |
      | 2026-12-10T09:00:00Z | 2026-12-10T10:00:01Z | 409    | cruza um segundo no início            |
      | 2026-12-10T11:59:59Z | 2026-12-10T13:00:00Z | 409    | cruza um segundo no fim               |
