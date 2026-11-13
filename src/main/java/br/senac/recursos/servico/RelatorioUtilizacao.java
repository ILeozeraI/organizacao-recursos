package br.senac.recursos.servico;

import java.time.Instant;
import java.util.List;

public record RelatorioUtilizacao(Instant inicio, Instant fim, List<LinhaRecurso> recursos,
                                  List<LinhaProfessor> professores, long conflitosEvitados) {

    public record LinhaRecurso(Long recursoId, String nome, double horasReservadas) {
    }

    public record LinhaProfessor(Long professorId, String nome, double horasAlocadas) {
    }
}
