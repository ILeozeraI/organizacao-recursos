package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "movimentacao_material")
public class MovimentacaoMaterial {
    public enum Tipo { RETIRADA, DEVOLUCAO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "reserva_id")
    private Long reservaId;
    @Enumerated(EnumType.STRING)
    private Tipo tipo;
    @Column(name = "usuario_id")
    private Long usuarioId;
    private Instant instante;
    private String observacao;

    protected MovimentacaoMaterial() {
    }

    public MovimentacaoMaterial(Long reservaId, Tipo tipo, Long usuarioId, Instant instante, String observacao) {
        this.reservaId = reservaId;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.instante = instante;
        this.observacao = observacao;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Tipo getTipo() { return tipo; }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getInstante() { return instante; }
    public String getObservacao() { return observacao; }
}
