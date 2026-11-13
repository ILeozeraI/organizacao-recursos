package br.senac.recursos.unidade;

import static br.senac.recursos.dominio.StatusReserva.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/** RN-05: máquina de estados. Testa TODAS as 49 combinações origem x destino. */
class StatusReservaTest {

    private static final Set<String> VALIDAS = Set.of(
            "SOLICITADA>APROVADA", "SOLICITADA>REJEITADA", "SOLICITADA>CANCELADA",
            "APROVADA>EM_USO", "APROVADA>CANCELADA", "APROVADA>NAO_COMPARECEU",
            "EM_USO>CONCLUIDA");

    static Stream<Arguments> todasAsTransicoes() {
        return Arrays.stream(StatusReserva.values()).flatMap(de -> Arrays.stream(StatusReserva.values())
                .map(para -> Arguments.of(de, para, VALIDAS.contains(de + ">" + para))));
    }

    @ParameterizedTest(name = "{0} -> {1} permitido? {2}")
    @MethodSource("todasAsTransicoes")
    void respeitaMatrizDeTransicoes(StatusReserva de, StatusReserva para, boolean permitido) {
        assertThat(de.podeTransitarPara(para)).isEqualTo(permitido);
    }

    @ParameterizedTest
    @EnumSource(StatusReserva.class)
    void classificaEstados(StatusReserva s) {
        assertThat(s.ocupaRecurso()).isEqualTo(s == SOLICITADA || s == APROVADA || s == EM_USO);
        assertThat(s.jaIniciada()).isEqualTo(s == EM_USO || s == CONCLUIDA || s == NAO_COMPARECEU);
        assertThat(s.contaComoUso()).isEqualTo(s == APROVADA || s == EM_USO || s == CONCLUIDA);
    }

    @Test
    void ocupantesSaoOsEstadosQueBloqueiamOHorario() {
        assertThat(StatusReserva.ocupantes()).containsExactlyInAnyOrder(SOLICITADA, APROVADA, EM_USO);
    }

    @Test
    void reservaRejeitaTransicaoInvalidaComCodigoEstavel() {
        Reserva r = new Reserva(1L, null, 10L, new Intervalo(Instant.parse("2026-12-01T10:00:00Z"),
                Instant.parse("2026-12-01T11:00:00Z")), "x", CONCLUIDA, Instant.parse("2026-11-01T00:00:00Z"));
        assertThatThrownBy(() -> r.transitarPara(EM_USO))
                .isInstanceOf(ErroNegocio.class).extracting("codigo").isEqualTo("TRANSICAO_INVALIDA");
        assertThat(r.getStatus()).isEqualTo(CONCLUIDA);
    }

    @Test
    void reservaTransitaERetornaEstadoAnterior() {
        Reserva r = new Reserva(1L, 6L, 10L, new Intervalo(Instant.parse("2026-12-01T10:00:00Z"),
                Instant.parse("2026-12-01T11:00:00Z")), "x", SOLICITADA, Instant.parse("2026-11-01T00:00:00Z"));
        assertThat(r.transitarPara(APROVADA)).isEqualTo(SOLICITADA);
        assertThat(r.getStatus()).isEqualTo(APROVADA);
        assertThat(r.pertenceA(10L)).isTrue();
        assertThat(r.pertenceA(11L)).isFalse();
    }
}
