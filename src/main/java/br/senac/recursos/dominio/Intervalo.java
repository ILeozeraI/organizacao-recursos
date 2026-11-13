package br.senac.recursos.dominio;

import java.time.Duration;
import java.time.Instant;

/**
 * Intervalo de tempo semiaberto [inicio, fim).
 * Regra: o término deve ser posterior ao início.
 * Dois intervalos que apenas se encostam (fim de um == início do outro) NÃO se sobrepõem.
 */
public record Intervalo(Instant inicio, Instant fim) {

    public Intervalo {
        if (inicio == null || fim == null) {
            throw ErroNegocio.invalido("INTERVALO_INCOMPLETO", "Informe início e término.");
        }
        if (!fim.isAfter(inicio)) {
            throw ErroNegocio.invalido("INTERVALO_INVALIDO", "O horário de término deve ser posterior ao de início.");
        }
    }

    public boolean sobrepoe(Intervalo outro) {
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }

    /** Horas deste intervalo que caem dentro do período informado (recorte nas bordas). */
    public double horasDentroDe(Intervalo periodo) {
        Instant ini = inicio.isAfter(periodo.inicio) ? inicio : periodo.inicio;
        Instant f = fim.isBefore(periodo.fim) ? fim : periodo.fim;
        if (!f.isAfter(ini)) {
            return 0.0;
        }
        return Duration.between(ini, f).toMinutes() / 60.0;
    }
}
