# Organização de Recursos

Sistema para reservar **salas, professores e materiais sem conflito de horário**, desenvolvido como
projeto final da disciplina **Qualidade de Software 2026.2 (Senac — prof. Afonso)**.

- **Aluno:** Leonardo Cavalcanti da Silva (turma STADSCAS4ND · sexta) — trabalho individual
- **Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 16 · Flyway · JUnit 5 · Testcontainers · WireMock ·
  Cucumber · JaCoCo · SonarCloud · GitHub Actions · JMeter

[![qualidade](https://github.com/ILeozeraI/organizacao-recursos/actions/workflows/ci.yml/badge.svg)](https://github.com/ILeozeraI/organizacao-recursos/actions/workflows/ci.yml)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=ILeozeraI_organizacao-recursos&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=ILeozeraI_organizacao-recursos)

---

## 1. Como rodar (do zero)

### Pré-requisitos
| Ferramenta | Para quê | Como conferir |
|---|---|---|
| **Java 21** (JDK) | compilar e rodar | `java -version` |
| **Docker** (Docker Desktop no Windows) | banco da aplicação e Testcontainers nos testes | `docker version` |
| Git | clonar | `git --version` |

> Não é preciso instalar o Maven: o projeto usa o **Maven Wrapper** (`./mvnw`), que baixa a versão certa sozinho.

### Passo a passo
```bash
git clone https://github.com/ILeozeraI/organizacao-recursos.git
cd organizacao-recursos

# 1) Rodar TODOS os testes + gate de cobertura (o mesmo comando do CI)
./mvnw -B verify            # Windows: mvnw.cmd -B verify

# 2) Subir o banco PostgreSQL
docker compose up -d

# 3) Subir a aplicação
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

Acesse:
- **Interface web (responsiva):** http://localhost:8080
- **Documentação da API (Swagger):** http://localhost:8080/swagger-ui.html
- **Contrato OpenAPI (JSON):** http://localhost:8080/v3/api-docs

### Usuários de demonstração (dados **fictícios**, senha `senha123`)
| Perfil | E-mail |
|---|---|
| Solicitante | `ana@exemplo.invalid` |
| Solicitante | `bruno@exemplo.invalid` |
| Responsável | `rita@exemplo.invalid` |
| Administrador | `admin@exemplo.invalid` |

### Variáveis de ambiente (opcionais)
| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | `jdbc:postgresql://localhost:5432/recursos` / `recursos` / `recursos` | banco |
| `NOTIFICACAO_URL` | `http://localhost:8089` | provedor externo de notificação (RF11, simulado) |
| `NOTIFICACAO_TOKEN` | `token-de-desenvolvimento` | chave do provedor — **nunca** versionar a real |
| `PORT` | `8080` | porta HTTP |

Sem provedor de notificação no ar, a aplicação funciona normalmente: a notificação fica registrada como `FALHA`.

---

## 2. Fluxos principais da API (RF14)

Detalhes e exemplos em [`docs/api.md`](docs/api.md) e no Swagger.

```bash
# Login (RF1) -> devolve o token
curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"ana@exemplo.invalid","senha":"senha123"}'

# Pesquisar salas livres com capacidade >= 30 (RF3)
curl -s "localhost:8080/recursos?tipo=SALA&capacidadeMinima=30&disponivelInicio=2026-12-01T10:00:00Z&disponivelFim=2026-12-01T12:00:00Z" \
  -H "Authorization: Bearer $TOKEN"

# Reservar (RF4) — 201, ou 409 se houver sobreposição/manutenção
curl -s -X POST localhost:8080/reservas -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"recursoId":1,"professorId":6,"inicio":"2026-12-01T10:00:00Z","fim":"2026-12-01T12:00:00Z","participantes":30}'
```

| Área | Rotas |
|---|---|
| Autenticação | `POST /auth/login`, `POST /auth/logout`, `GET /auth/eu` |
| Recursos e manutenção | `GET/POST /recursos`, `GET/PUT/DELETE /recursos/{id}`, `GET/POST /recursos/{id}/manutencoes` |
| Reservas | `GET/POST /reservas`, `GET/PUT/DELETE /reservas/{id}` |
| Estados | `POST /reservas/{id}/aprovar · rejeitar · cancelar · iniciar · concluir · nao-compareceu` |
| Materiais | `POST /reservas/{id}/retirada · devolucao`, `GET /reservas/{id}/movimentacoes` |
| Auditoria | `GET /reservas/{id}/historico` |
| Relatório | `GET /relatorios/utilizacao?inicio=&fim=` |

---

## 3. Qualidade: onde está cada evidência

| O que | Onde |
|---|---|
| **Índice do pacote de evidências** | [`docs/evidencias/README.md`](docs/evidencias/README.md) |
| Matriz de rastreabilidade (RTM) | [`docs/rtm.md`](docs/rtm.md) |
| Situação dos 14 requisitos | [`docs/status-requisitos.md`](docs/status-requisitos.md) |
| Arquitetura (C4, sequências, ATAM) e ADRs | [`docs/arquitetura.md`](docs/arquitetura.md), [`docs/adr/`](docs/adr/) |
| Plano de testes | [`docs/testes/plano-de-testes.md`](docs/testes/plano-de-testes.md) |
| Matriz de autorização | [`docs/seguranca/matriz-autorizacao.md`](docs/seguranca/matriz-autorizacao.md) |
| TDD e BDD | [`docs/testes/evidencia-tdd.md`](docs/testes/evidencia-tdd.md), [`docs/testes/bdd-aprovacao-bloqueio.md`](docs/testes/bdd-aprovacao-bloqueio.md) |
| Mutação manual das regras críticas | [`docs/testes/mutacao-manual.md`](docs/testes/mutacao-manual.md) |
| Teste de carga (JMeter) | [`docs/performance/rnf.md`](docs/performance/rnf.md), [`docs/performance/resultado-01.md`](docs/performance/resultado-01.md) |
| Registro de defeitos | [`docs/defeitos/`](docs/defeitos/) |
| Relatório final de qualidade | [`docs/relatorio-qualidade.md`](docs/relatorio-qualidade.md) |
| Decisão de qualidade | [`docs/decisao-qualidade.md`](docs/decisao-qualidade.md) |

### Integração contínua
- Workflow [`qualidade`](.github/workflows/ci.yml): roda em todo pull request e push na `main`.
  Job **`qualidade / test`** = `./mvnw -B verify` (unidade + integração + BDD + gate JaCoCo 80%/70%) e análise
  SonarCloud; job **`qualidade / carga`** = JMeter contra a aplicação subindo dentro do runner.
- Workflow [`mutacao-manual`](.github/workflows/mutacao-manual.yml): quebra cada regra crítica e confirma que algum teste falha.
- Relatórios do Surefire/Failsafe e do JaCoCo ficam como **artifacts** de cada execução; o resumo de cobertura
  aparece na aba *Summary* da execução.

### Estrutura do código
```
src/main/java/br/senac/recursos
├── dominio/      entidades e regras puras (Intervalo, StatusReserva, Reserva...)
├── servico/      casos de uso (ReservaService, RelatorioService, AuditoriaService...)
├── seguranca/    token, filtro e matriz de autorização (SecurityConfig)
├── integracao/   cliente HTTP da notificação externa (RF11)
├── repositorio/  Spring Data JPA
└── web/          controllers, DTOs e tratamento de erros (ProblemDetail)
src/main/resources/db/migration   Flyway (inclui as constraints EXCLUDE anti-sobreposição)
src/test/java/.../unidade         testes de unidade (JUnit 5 parametrizado, Clock fixo, WireMock)
src/test/java/.../integracao      testes de API/integração com PostgreSQL real (Testcontainers)
src/test/java/.../bdd + resources/features   cenários BDD (Cucumber, Dado/Quando/Então)
perf/                             plano JMeter, massa fictícia e script de análise
```
