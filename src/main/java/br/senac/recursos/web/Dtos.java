package br.senac.recursos.web;

import br.senac.recursos.dominio.AuditoriaReserva;
import br.senac.recursos.dominio.Manutencao;
import br.senac.recursos.dominio.MovimentacaoMaterial;
import br.senac.recursos.dominio.Recurso;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.dominio.TipoRecurso;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Contratos de entrada e saída da API (entidades nunca são expostas diretamente). */
public final class Dtos {

    private Dtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String senha) {
    }

    public record LoginResponse(String token, Instant expiraEm, Long usuarioId, String nome, String perfil) {
    }

    public record RecursoRequest(
            @NotBlank(message = "informe o nome") @Size(max = 120) String nome,
            @NotNull(message = "informe o tipo (SALA, MATERIAL ou PROFESSOR)") TipoRecurso tipo,
            @PositiveOrZero(message = "a capacidade não pode ser negativa") Integer capacidade,
            @Size(max = 120) String localizacao,
            @Size(max = 120) String competencia,
            boolean restrito) {
    }

    public record RecursoResponse(Long id, String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
                                  String competencia, boolean restrito) {
        static RecursoResponse de(Recurso r) {
            return new RecursoResponse(r.getId(), r.getNome(), r.getTipo(), r.getCapacidade(), r.getLocalizacao(),
                    r.getCompetencia(), r.isRestrito());
        }
    }

    public record ManutencaoRequest(
            @NotNull(message = "informe o início") Instant inicio,
            @NotNull(message = "informe o término") Instant fim,
            @NotBlank(message = "informe o motivo") @Size(max = 255) String motivo) {
    }

    public record ManutencaoResponse(Long id, Long recursoId, Instant inicio, Instant fim, String motivo) {
        static ManutencaoResponse de(Manutencao m) {
            return new ManutencaoResponse(m.getId(), m.getRecursoId(), m.getInicio(), m.getFim(), m.getMotivo());
        }
    }

    public record ReservaRequest(
            @NotNull(message = "informe o recurso") Long recursoId,
            Long professorId,
            @NotNull(message = "informe o início") Instant inicio,
            @NotNull(message = "informe o término") Instant fim,
            @Size(max = 255) String finalidade) {
    }

    public record AlterarReservaRequest(
            @NotNull(message = "informe o início") Instant inicio,
            @NotNull(message = "informe o término") Instant fim) {
    }

    public record MotivoRequest(@Size(max = 255) String motivo) {
    }

    public record ReservaResponse(Long id, Long recursoId, Long professorId, Long solicitanteId, Instant inicio,
                                  Instant fim, StatusReserva status, String finalidade) {
        static ReservaResponse de(Reserva r) {
            return new ReservaResponse(r.getId(), r.getRecursoId(), r.getProfessorId(), r.getSolicitanteId(),
                    r.getInicio(), r.getFim(), r.getStatus(), r.getFinalidade());
        }
    }

    public record AuditoriaResponse(Long id, Long reservaId, Long usuarioId, Instant instante,
                                    StatusReserva estadoAnterior, StatusReserva estadoNovo, String motivo) {
        static AuditoriaResponse de(AuditoriaReserva a) {
            return new AuditoriaResponse(a.getId(), a.getReservaId(), a.getUsuarioId(), a.getInstante(),
                    a.getEstadoAnterior(), a.getEstadoNovo(), a.getMotivo());
        }
    }

    public record MovimentacaoResponse(Long id, MovimentacaoMaterial.Tipo tipo, Long usuarioId, Instant instante,
                                       String observacao) {
        static MovimentacaoResponse de(MovimentacaoMaterial m) {
            return new MovimentacaoResponse(m.getId(), m.getTipo(), m.getUsuarioId(), m.getInstante(),
                    m.getObservacao());
        }
    }
}
