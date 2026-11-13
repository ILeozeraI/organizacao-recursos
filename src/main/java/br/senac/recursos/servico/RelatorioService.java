package br.senac.recursos.servico;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Recurso;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.repositorio.ConflitoEvitadoRepository;
import br.senac.recursos.repositorio.RecursoRepository;
import br.senac.recursos.repositorio.ReservaRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF12: relatório de utilização por recurso, carga horária por professor e conflitos evitados. */
@Service
public class RelatorioService {

    private final ReservaRepository reservas;
    private final RecursoRepository recursos;
    private final ConflitoEvitadoRepository conflitos;

    public RelatorioService(ReservaRepository reservas, RecursoRepository recursos,
                            ConflitoEvitadoRepository conflitos) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.conflitos = conflitos;
    }

    @Transactional(readOnly = true)
    public RelatorioUtilizacao utilizacao(Intervalo periodo) {
        List<Reserva> noPeriodo = reservas.findNoPeriodo(periodo.inicio(), periodo.fim(),
                EnumSet.of(StatusReserva.APROVADA, StatusReserva.EM_USO, StatusReserva.CONCLUIDA));
        Map<Long, String> nomes = recursos.findAll().stream()
                .collect(Collectors.toMap(Recurso::getId, Recurso::getNome, (a, b) -> a));
        Function<Long, String> nome = id -> nomes.getOrDefault(id, "#" + id);

        var porRecurso = CalculadoraUtilizacao.horasPorRecurso(noPeriodo, periodo).entrySet().stream()
                .map(e -> new RelatorioUtilizacao.LinhaRecurso(e.getKey(), nome.apply(e.getKey()), e.getValue()))
                .toList();
        var porProfessor = CalculadoraUtilizacao.horasPorProfessor(noPeriodo, periodo).entrySet().stream()
                .map(e -> new RelatorioUtilizacao.LinhaProfessor(e.getKey(), nome.apply(e.getKey()), e.getValue()))
                .toList();
        long totalConflitos = conflitos.contarNoPeriodo(periodo.inicio(), periodo.fim());
        return new RelatorioUtilizacao(periodo.inicio(), periodo.fim(), porRecurso, porProfessor, totalConflitos);
    }
}
