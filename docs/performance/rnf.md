# RNF de desempenho e modelo de carga

> Requisito escrito **antes** da execução, como **proposta** (trabalho individual: validado pelo próprio
> autor; deve ser revisto com o cliente/professor). Base: exemplo da Aula 10.

## RNF-PERF-01
Com **50 usuários simultâneos** usando a API (login, consulta de disponibilidade e criação de reserva
disputando o mesmo recurso), o **p95 do tempo de resposta deve ser ≤ 300 ms** por operação e a
**taxa de erro real < 1%**.

- **409 (conflito de horário) NÃO é erro**: é a resposta correta para quem perdeu a disputa.
- **Erro real** = qualquer 5xx, timeout ou resposta fora do contrato (assertiva do JMeter).
- **Invariante de integridade:** ao final, no **banco**, existe **no máximo 1** reserva ativa para o
  recurso e horário disputados.

## Modelo de carga (`perf/reservas.jmx`)
| Parâmetro | Valor | Propriedade JMeter |
|---|---|---|
| Usuários simultâneos | 50 | `-JusuariosSimultaneos=50` |
| Ramp-up | 10 s | `-Jrampup=10` |
| Duração | 60 s | `-Jduracao=60` |
| Massa | `perf/usuarios.csv` (2 solicitantes fictícios, reaproveitados) | `-Jusuarios=...` |
| Mistura por iteração | 1× `GET /recursos?tipo=SALA&disponivel...` + 1× `POST /reservas` (mesmo recurso e horário) | — |
| Login | 1 vez por usuário virtual (`Once Only Controller`) + JSON Extractor do token | — |
| Assertivas | login 200; consulta 200; criação **somente 201 ou 409** (nunca 500) | — |

## Ambiente
Aplicação e PostgreSQL 16 sobem **dentro do runner** do GitHub Actions (job `qualidade / carga`), nunca
contra ambiente compartilhado. Máquina: runner padrão `ubuntu-latest` (4 vCPU, 16 GB). Os números valem
para esse ambiente e **não** podem ser generalizados para produção.

## Como reproduzir localmente
```bash
docker compose up -d && ./mvnw -B -DskipTests package
java -jar target/organizacao-recursos-*.jar &
jmeter -n -t perf/reservas.jmx -l perf/resultados/rodada-01.jtl -e -o perf/relatorio-01
python3 perf/analisar.py perf/resultados/rodada-01.jtl
```
O plano e o CSV são versionados; o `.jtl` e o relatório HTML não (ficam como artifact do CI).
