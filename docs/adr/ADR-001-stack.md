# ADR-001 — Stack: Java 21, Spring Boot 3.5, PostgreSQL 16

- **Status:** aceito
- **Contexto:** a especificação do projeto exige Java 21, Spring Boot 3.x, Maven, JUnit 5, Testcontainers, WireMock, JaCoCo, SonarCloud, GitHub Actions e JMeter. Precisamos de um banco que ofereça garantias fortes para a regra de não sobreposição.
- **Alternativas:** (a) H2 em memória; (b) MySQL; (c) PostgreSQL.
- **Decisão:** PostgreSQL 16 com Flyway; nos testes, PostgreSQL real via Testcontainers (sem H2).
- **Consequências:** + testes rodam no mesmo banco de produção (constraints e tipos iguais); + `EXCLUDE USING gist` disponível (ADR-002). − exige Docker para rodar os testes de integração localmente.
- **Riscos:** máquina sem Docker não roda `verify` completo (mitigação: CI roda sempre).
- **Evidência:** `pom.xml`, `PostgresTestcontainersConfig`, CI verde.
- **Condição de revisão:** se o projeto precisar rodar sem Docker em ambiente acadêmico.
