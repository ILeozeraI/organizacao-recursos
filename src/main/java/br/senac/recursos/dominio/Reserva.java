package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "recurso_id")
    private Long recursoId;
    @Column(name = "professor_id")
    private Long professorId;
    @Column(name = "solicitante_id")
    private Long solicitanteId;
    private Instant inicio;
    private Instant fim;
    @Enumerated(EnumType.STRING)
    private StatusReserva status;
    private String finalidade;
    private Integer participantes;
    @Column(name = "criada_em")
    private Instant criadaEm;
    @Version
    private long versao;

    protected Reserva() {
    }

    public Reserva(Long recursoId, Long professorId, Long solicitanteId, Intervalo intervalo,
                   String finalidade, StatusReserva statusInicial, Instant criadaEm) {
        this.recursoId = recursoId;
        this.professorId = professorId;
        this.solicitanteId = solicitanteId;
        this.inicio = intervalo.inicio();
        this.fim = intervalo.fim();
        this.finalidade = finalidade;
        this.status = statusInicial;
        this.criadaEm = criadaEm;
    }

    /** Aplica a transição de estado; transição proibida gera 409 com código estável. */
    public StatusReserva transitarPara(StatusReserva destino) {
        if (!status.podeTransitarPara(destino)) {
            throw ErroNegocio.conflito("TRANSICAO_INVALIDA",
                    "Não é possível mudar a reserva de " + status + " para " + destino + ".");
        }
        StatusReserva anterior = status;
        status = destino;
        return anterior;
    }

    public void reagendar(Intervalo novo) {
        this.inicio = novo.inicio();
        this.fim = novo.fim();
    }

    public boolean pertenceA(Long usuarioId) {
        return solicitanteId.equals(usuarioId);
    }

    public void definirParticipantes(Integer participantes) {
        this.participantes = participantes;
    }

    public Intervalo intervalo() { return new Intervalo(inicio, fim); }

    public Long getId() { return id; }
    public Long getRecursoId() { return recursoId; }
    public Long getProfessorId() { return professorId; }
    public Long getSolicitanteId() { return solicitanteId; }
    public Instant getInicio() { return inicio; }
    public Instant getFim() { return fim; }
    public StatusReserva getStatus() { return status; }
    public String getFinalidade() { return finalidade; }
    public Integer getParticipantes() { return participantes; }
    public Instant getCriadaEm() { return criadaEm; }
}
