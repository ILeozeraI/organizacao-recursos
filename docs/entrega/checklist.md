# Checklist de entrega (avaliador externo, clone limpo)

**Como foi feito:** clone novo e raso (`git clone --depth 1`) em diretório temporário, sem reaproveitar nada
da máquina de desenvolvimento, conferindo cada item da especificação. A execução seguindo **só o README**
(`./mvnw -B verify`, `java -jar`) é feita a cada push por runners limpos do GitHub Actions:
job `test` (Linux), job `carga` (sobe o `.jar` + PostgreSQL do zero) e job `wrapper-windows` (`mvnw.cmd`).

Formato: item → existe → atualizado → caminho.

| # | Item da especificação | Existe | Atualizado | Caminho |
|---|---|---|---|---|
| 1 | Repositório com revisão por pares | Sim (parcial: individual) | Sim | PRs #1, #2, #3 com check `test` obrigatório na `main` (proteção de branch) |
| 2 | Aplicação executável | Sim | Sim | `./mvnw spring-boot:run` + `docker-compose.yml`; job `carga` |
| 3 | README e documentação da API | Sim | Sim | `README.md`, `docs/api.md`, Swagger `/swagger-ui.html` |
| 4 | RTM | Sim | Sim | `docs/rtm.md` |
| 5 | Diagramas de contexto, componentes e sequências críticas | Sim | Sim | `docs/arquitetura.md` (§2–§5) |
| 6 | Plano e relatório de testes | Sim | Sim | `docs/testes/plano-de-testes.md`, `docs/relatorio-qualidade.md`, relatórios Surefire/Failsafe (artifact) |
| 7 | Pipeline de CI | Sim | Sim | `.github/workflows/ci.yml` |
| 8 | Relatórios SonarCloud e JaCoCo | Sim | Sim | SonarCloud (badge no README) e resumo no CI; artifact `jacoco-<n>` |
| 9 | Plano e resultado do teste de carga | Sim | Sim | `perf/reservas.jmx`, `docs/performance/rnf.md`, `docs/performance/resultado-01.md` |
| 10 | Registro de defeitos | Sim | Sim | `docs/defeitos/` (DEF-001, DEF-002) |
| 11 | Relatório final de qualidade | Sim | Sim | `docs/relatorio-qualidade.md` |
| — | Pacote de evidências e decisão | Sim | Sim | `docs/evidencias/README.md`, `docs/decisao-qualidade.md`, `CHANGELOG.md` |

## Bloqueadores encontrados (em ordem)
1. ~~`docs/relatorio-qualidade.md` ausente no clone de `cd51634`~~ → incluído no PR #3 (release).
2. ~~`mvnw.cmd` (Windows) nunca executado~~ → job `wrapper-windows` adicionado ao CI no PR #3.

Nenhum bloqueador restante após o PR #3.
