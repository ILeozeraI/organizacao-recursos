package br.senac.recursos.unidade;

import static org.assertj.core.api.Assertions.assertThat;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.servico.CalculadoraUtilizacao;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** RF12: oráculo calculado à mão (a conta está em cada comentário). */
class CalculadoraUtilizacaoTest {

    private static final Intervalo DIA = new Intervalo(Instant.parse("2026-12-01T00:00:00Z"),
            Instant.parse("2026-12-02T00:00:00Z"));

    private static Reserva r(long recurso, Long prof, String ini, String fim, StatusReserva st) {
        return new Reserva(recurso, prof, 1L, new Intervalo(Instant.parse(ini), Instant.parse(fim)), null, st,
                Instant.parse("2026-11-01T00:00:00Z"));
    }

    @Test
    void somaHorasPorRecursoEProfessorRecortandoBordasEIgnorandoNaoUso() {
        List<Reserva> reservas = List.of(
                r(1, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z", StatusReserva.APROVADA),   // 2h
                r(1, 6L, "2026-12-01T13:00:00Z", "2026-12-01T14:30:00Z", StatusReserva.CONCLUIDA),    // 1,5h
                r(4, 6L, "2026-11-30T23:00:00Z", "2026-12-01T01:00:00Z", StatusReserva.EM_USO),       // 1h dentro
                r(1, 7L, "2026-12-01T15:00:00Z", "2026-12-01T16:00:00Z", StatusReserva.CANCELADA),    // não conta
                r(2, 7L, "2026-12-01T08:00:00Z", "2026-12-01T09:00:00Z", StatusReserva.SOLICITADA),   // não conta
                r(2, null, "2026-12-03T08:00:00Z", "2026-12-03T09:00:00Z", StatusReserva.APROVADA));  // fora
        // Recurso 1 = 2 + 1,5 = 3,5h ; Recurso 4 = 1h (23h-01h, só 00h-01h dentro do dia)
        assertThat(CalculadoraUtilizacao.horasPorRecurso(reservas, DIA)).isEqualTo(Map.of(1L, 3.5, 4L, 1.0));
        // Professor 6 = 1,5 + 1 = 2,5h ; professor 7 só tem reservas que não contam
        assertThat(CalculadoraUtilizacao.horasPorProfessor(reservas, DIA)).isEqualTo(Map.of(6L, 2.5));
    }

    @Test
    void periodoVazioNaoGeraLinhas() {
        assertThat(CalculadoraUtilizacao.horasPorRecurso(List.of(), DIA)).isEmpty();
        assertThat(CalculadoraUtilizacao.horasPorProfessor(List.of(), DIA)).isEmpty();
    }

    @Test
    void arredondaParaDuasCasas() {
        // 20 min = 0,3333h -> 0,33
        var reservas = List.of(r(1, null, "2026-12-01T10:00:00Z", "2026-12-01T10:20:00Z", StatusReserva.APROVADA));
        assertThat(CalculadoraUtilizacao.horasPorRecurso(reservas, DIA)).containsEntry(1L, 0.33);
    }
}
