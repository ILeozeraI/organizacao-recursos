package br.senac.recursos.dominio;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Máquina de estados da reserva (regra crítica "Estados e auditoria").
 * Fluxo principal: SOLICITADA -> APROVADA -> EM_USO -> CONCLUIDA.
 * Alternativos: REJEITADA, CANCELADA, NAO_COMPARECEU.
 */
public enum StatusReserva {
    SOLICITADA, APROVADA, EM_USO, CONCLUIDA, REJEITADA, CANCELADA, NAO_COMPARECEU;

    private static final Map<StatusReserva, Set<StatusReserva>> TRANSICOES = Map.of(
            SOLICITADA, EnumSet.of(APROVADA, REJEITADA, CANCELADA),
            APROVADA, EnumSet.of(EM_USO, CANCELADA, NAO_COMPARECEU),
            EM_USO, EnumSet.of(CONCLUIDA),
            CONCLUIDA, EnumSet.noneOf(StatusReserva.class),
            REJEITADA, EnumSet.noneOf(StatusReserva.class),
            CANCELADA, EnumSet.noneOf(StatusReserva.class),
            NAO_COMPARECEU, EnumSet.noneOf(StatusReserva.class));

    public boolean podeTransitarPara(StatusReserva destino) {
        return TRANSICOES.get(this).contains(destino);
    }

    /** Estados que ocupam o recurso (entram na verificação de sobreposição). */
    public boolean ocupaRecurso() {
        return this == SOLICITADA || this == APROVADA || this == EM_USO;
    }

    /** Reserva iniciada (ou já encerrada após uso) não pode ser excluída. */
    public boolean jaIniciada() {
        return this == EM_USO || this == CONCLUIDA || this == NAO_COMPARECEU;
    }

    /** Estados que contam como uso no relatório de utilização. */
    public boolean contaComoUso() {
        return this == APROVADA || this == EM_USO || this == CONCLUIDA;
    }

    public static Set<StatusReserva> ocupantes() {
        return EnumSet.of(SOLICITADA, APROVADA, EM_USO);
    }
}
