# ADR-003 — Autenticação por token opaco revogável

- **Status:** aceito
- **Contexto:** RF1 exige autenticação e autorização por perfil. A API é consumida pela interface web e por testes/ferramentas (JMeter). Logout precisa invalidar a sessão.
- **Alternativas:** (a) HTTP Basic; (b) JWT assinado; (c) token opaco aleatório guardado no banco.
- **Decisão:** (c) token de 256 bits (`SecureRandom`), tabela `sessao` com expiração (8 h) e flag de revogação. Regras de decisão do cenário E2: ausente, inválido, **expirado (expiração ≤ agora)** e revogado negam acesso.
- **Consequências:** + revogação imediata no logout; + regra de tempo testável com `Clock` fixo; − uma consulta ao banco por requisição.
- **Evidência:** `ValidadorTokenTest` (limites −1/0/+1 s), `AutenticacaoIT`.
- **Condição de revisão:** escala que torne a consulta por requisição um gargalo.
