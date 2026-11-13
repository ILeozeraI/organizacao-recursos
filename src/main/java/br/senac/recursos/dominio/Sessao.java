package br.senac.recursos.dominio;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sessao")
public class Sessao {
    @Id
    private String token;
    @Column(name = "usuario_id")
    private Long usuarioId;
    @Column(name = "criada_em")
    private Instant criadaEm;
    @Column(name = "expira_em")
    private Instant expiraEm;
    private boolean revogada;

    protected Sessao() {
    }

    public Sessao(String token, Long usuarioId, Instant criadaEm, Instant expiraEm) {
        this.token = token;
        this.usuarioId = usuarioId;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
    }

    public void revogar() { this.revogada = true; }

    public String getToken() { return token; }
    public Long getUsuarioId() { return usuarioId; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isRevogada() { return revogada; }
}
