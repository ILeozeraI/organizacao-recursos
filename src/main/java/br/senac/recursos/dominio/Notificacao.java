package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notificacao")
public class Notificacao {
    public enum Status { ENVIADA, FALHA }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reserva_id")
    private Long reservaId;
    private String evento;
    @Enumerated(EnumType.STRING)
    private Status status;
    private String detalhe;
    private Instant instante;

    protected Notificacao() {
    }

    public Notificacao(Long reservaId, String evento, Status status, String detalhe, Instant instante) {
        this.reservaId = reservaId;
        this.evento = evento;
        this.status = status;
        this.detalhe = detalhe;
        this.instante = instante;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public String getEvento() { return evento; }
    public Status getStatus() { return status; }
    public String getDetalhe() { return detalhe; }
    public Instant getInstante() { return instante; }
}
