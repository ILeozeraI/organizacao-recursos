package br.senac.recursos.unidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.dominio.Sessao;
import br.senac.recursos.seguranca.DecisaoToken;
import br.senac.recursos.seguranca.MotivoNegacao;
import br.senac.recursos.seguranca.ValidadorToken;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** RN-08 (cenário E2): decisões de token com relógio fixo (sem Thread.sleep). */
class ValidadorTokenTest {

    private static final Instant AGORA = Instant.parse("2026-11-20T10:00:00Z");
    private final ValidadorToken validador = new ValidadorToken(Clock.fixed(AGORA, ZoneOffset.UTC));

    private static Sessao sessao(Instant expira, boolean revogada) {
        Sessao s = new Sessao("tok", 42L, AGORA.minusSeconds(3600), expira);
        if (revogada) {
            s.revogar();
        }
        return s;
    }

    @ParameterizedTest(name = "expira em {0}s -> válido? {1}")
    @CsvSource({"-1, false", "0, false", "1, true"})
    @DisplayName("limite da expiração: no instante exato o token já é inválido")
    void respeitaLimite(long deslocamento, boolean esperado) {
        assertThat(validador.estaVigente(AGORA.plusSeconds(deslocamento))).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void negaTokenAusente(String token) {
        assertThat(validador.avaliar(token, Optional.empty()).motivo()).isEqualTo(MotivoNegacao.TOKEN_AUSENTE);
    }

    @Test
    void negaTokenInexistente() {
        DecisaoToken d = validador.avaliar("nao-existe", Optional.empty());
        assertThat(d.permitido()).isFalse();
        assertThat(d.motivo()).isEqualTo(MotivoNegacao.TOKEN_INVALIDO);
    }

    @Nested
    @DisplayName("quando o token está expirado")
    class TokenExpirado {
        @Test
        void negaMesmoSemRevogacao() {
            DecisaoToken d = validador.avaliar("tok", Optional.of(sessao(AGORA.minusSeconds(1), false)));
            assertThat(d.permitido()).isFalse();
            assertThat(d.motivo()).isEqualTo(MotivoNegacao.TOKEN_EXPIRADO);
        }

        @Test
        void expiracaoTemPrecedenciaSobreRevogacao() {
            DecisaoToken d = validador.avaliar("tok", Optional.of(sessao(AGORA, true)));
            assertThat(d.motivo()).isEqualTo(MotivoNegacao.TOKEN_EXPIRADO);
        }
    }

    @Test
    void negaTokenRevogadoVigente() {
        DecisaoToken d = validador.avaliar("tok", Optional.of(sessao(AGORA.plusSeconds(60), true)));
        assertThat(d.motivo()).isEqualTo(MotivoNegacao.TOKEN_REVOGADO);
    }

    @Test
    void permiteTokenValidoEVigente() {
        DecisaoToken d = validador.avaliar("tok", Optional.of(sessao(AGORA.plusSeconds(60), false)));
        assertThat(d.permitido()).isTrue();
        assertThat(d.usuarioId()).isEqualTo(42L);
        assertThat(d.motivo()).isNull();
    }
}
