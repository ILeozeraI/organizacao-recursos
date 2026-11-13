package br.senac.recursos.servico;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Reserva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Cálculo puro do RF12 (sem banco), para ser testado com valores calculados à mão.
 * Regras: só contam APROVADA, EM_USO e CONCLUIDA; reservas que cruzam a borda do
 * período contam apenas a parte de dentro.
 */
public final class CalculadoraUtilizacao {

    private CalculadoraUtilizacao() {
    }

    public static Map<Long, Double> horasPorRecurso(List<Reserva> reservas, Intervalo periodo) {
        return somar(reservas, periodo, Reserva::getRecursoId);
    }

    public static Map<Long, Double> horasPorProfessor(List<Reserva> reservas, Intervalo periodo) {
        return somar(reservas.stream().filter(r -> r.getProfessorId() != null).toList(), periodo,
                Reserva::getProfessorId);
    }

    private static Map<Long, Double> somar(List<Reserva> reservas, Intervalo periodo, Function<Reserva, Long> chave) {
        Map<Long, Double> total = new TreeMap<>();
        for (Reserva r : reservas) {
            if (!r.getStatus().contaComoUso()) {
                continue;
            }
            double horas = r.intervalo().horasDentroDe(periodo);
            if (horas > 0) {
                total.merge(chave.apply(r), horas, Double::sum);
            }
        }
        total.replaceAll((k, v) -> arredondar(v));
        return total;
    }

    static double arredondar(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
