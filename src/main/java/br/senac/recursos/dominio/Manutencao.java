package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "manutencao")
public class Manutencao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recurso_id")
    private Long recursoId;
    private Instant inicio;
    private Instant fim;
    private String motivo;

    protected Manutencao() {
    }

    public Manutencao(Long recursoId, Intervalo intervalo, String motivo) {
        this.recursoId = recursoId;
        this.inicio = intervalo.inicio();
        this.fim = intervalo.fim();
        this.motivo = motivo;
    }

    public Intervalo intervalo() { return new Intervalo(inicio, fim); }

    public Long getId() { return id; }
    public Long getRecursoId() { return recursoId; }
    public Instant getInicio() { return inicio; }
    public Instant getFim() { return fim; }
    public String getMotivo() { return motivo; }
}
