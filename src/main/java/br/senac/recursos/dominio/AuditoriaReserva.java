package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

/** Registro imutável: sem setters e sem endpoint de alteração/exclusão. */
@Entity
@Table(name = "auditoria_reserva")
public class AuditoriaReserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reserva_id", updatable = false)
    private Long reservaId;
    @Column(name = "usuario_id", updatable = false)
    private Long usuarioId;
    @Column(updatable = false)
    private Instant instante;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", updatable = false)
    private StatusReserva estadoAnterior;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_novo", updatable = false)
    private StatusReserva estadoNovo;
    @Column(updatable = false)
    private String motivo;

    protected AuditoriaReserva() {
    }

    public AuditoriaReserva(Long reservaId, Long usuarioId, Instant instante,
                            StatusReserva estadoAnterior, StatusReserva estadoNovo, String motivo) {
        this.reservaId = reservaId;
        this.usuarioId = usuarioId;
        this.instante = instante;
        this.estadoAnterior = estadoAnterior;
        this.estadoNovo = estadoNovo;
        this.motivo = motivo;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getInstante() { return instante; }
    public StatusReserva getEstadoAnterior() { return estadoAnterior; }
    public StatusReserva getEstadoNovo() { return estadoNovo; }
    public String getMotivo() { return motivo; }
}
