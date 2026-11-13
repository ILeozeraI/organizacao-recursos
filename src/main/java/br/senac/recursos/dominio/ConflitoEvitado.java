package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "conflito_evitado")
public class ConflitoEvitado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recurso_id")
    private Long recursoId;
    @Column(name = "professor_id")
    private Long professorId;
    private Instant inicio;
    private Instant fim;
    @Column(name = "registrado_em")
    private Instant registradoEm;

    protected ConflitoEvitado() {
    }

    public ConflitoEvitado(Long recursoId, Long professorId, Intervalo intervalo, Instant registradoEm) {
        this.recursoId = recursoId;
        this.professorId = professorId;
        this.inicio = intervalo.inicio();
        this.fim = intervalo.fim();
        this.registradoEm = registradoEm;
    }

    public Long getId() { return id; }
    public Long getRecursoId() { return recursoId; }
    public Long getProfessorId() { return professorId; }
    public Instant getRegistradoEm() { return registradoEm; }
}
