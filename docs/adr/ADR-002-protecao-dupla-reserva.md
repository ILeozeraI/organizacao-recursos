# ADR-002 — Proteção contra dupla reserva no banco (constraint EXCLUDE)

- **Status:** aceito (revisado após DEF-002)
- **Contexto:** RF6 exige que duas solicitações simultâneas resultem em uma única reserva aceita. Uma verificação só na aplicação ("existe reserva sobreposta?") tem condição de corrida: duas requisições podem passar juntas pela verificação.
- **Alternativas:**
  1. Só verificação na aplicação — **rejeitada**: falha sob concorrência.
  2. Lock pessimista (`SELECT ... FOR UPDATE`) no recurso — funciona, mas serializa todas as reservas do recurso e precisa lembrar de travar também o professor.
  3. `UNIQUE (recurso_id, inicio)` — **rejeitada**: não detecta sobreposição parcial.
  4. **Constraint `EXCLUDE USING gist (recurso_id WITH =, tstzrange(inicio, fim, '[)') WITH &&)`** filtrada pelos estados ativos, uma para o recurso e outra para o professor.
- **Decisão:** alternativa 4 + verificação prévia na aplicação (mensagem clara e registro de "conflito evitado"). Violação `23P01` vira **409**. **Revisão após DEF-002:** a gravação pega travas consultivas (`pg_advisory_xact_lock`) do recurso e do professor, sempre nessa ordem, e reverifica a sobreposição dentro da trava — isso eliminou os deadlocks entre as duas constraints; vítimas de deadlock (`40P01`) ainda são repetidas como rede de segurança.
- **Consequências:** + garantia no banco, vale para qualquer caminho de escrita; + intervalo semiaberto `[)` permite reservas encostadas (10h–12h e 12h–13h). − dependência do PostgreSQL (`btree_gist`); − pedidos para o mesmo recurso/professor são serializados (fila curta por recurso), o que adiciona latência sob disputa intensa.
- **Riscos:** latência extra nas repetições sob disputa intensa (medido no teste de carga).
- **Evidência:** `V1__schema.sql`; `ConcorrenciaReservaIT` (20 requisições simultâneas, 5 repetições, contagem no banco); mutante M2 em `docs/testes/mutacao-manual.md` (remover a constraint faz o teste falhar).
- **Condição de revisão:** troca de banco de dados ou necessidade de reservas recorrentes.
