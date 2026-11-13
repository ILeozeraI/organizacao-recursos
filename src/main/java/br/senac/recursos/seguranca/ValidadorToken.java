package br.senac.recursos.seguranca;

import br.senac.recursos.dominio.Sessao;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Regras de decisão do token (cenário E2 da Aula 07).
 * Token expirado NUNCA libera acesso. No instante exato da expiração o token já é inválido.
 * O relógio é injetado (Clock) para os testes serem determinísticos.
 */
public class ValidadorToken {

    private final Clock clock;

    public ValidadorToken(Clock clock) {
        this.clock = clock;
    }

    public DecisaoToken avaliar(String tokenInformado, Optional<Sessao> sessao) {
        if (tokenInformado == null || tokenInformado.isBlank()) {
            return DecisaoToken.negar(MotivoNegacao.TOKEN_AUSENTE);
        }
        if (sessao.isEmpty()) {
            return DecisaoToken.negar(MotivoNegacao.TOKEN_INVALIDO);
        }
        Sessao s = sessao.get();
        if (!estaVigente(s.getExpiraEm())) {
            return DecisaoToken.negar(MotivoNegacao.TOKEN_EXPIRADO);
        }
        if (s.isRevogada()) {
            return DecisaoToken.negar(MotivoNegacao.TOKEN_REVOGADO);
        }
        return DecisaoToken.permitir(s.getUsuarioId());
    }

    /** Válido somente se a expiração for estritamente posterior a "agora". */
    public boolean estaVigente(Instant expiraEm) {
        return expiraEm.isAfter(Instant.now(clock));
    }
}
