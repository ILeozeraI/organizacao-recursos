package br.senac.recursos.dominio;

import jakarta.persistence.*;

@Entity
@Table(name = "recurso")
public class Recurso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nome;
    @Enumerated(EnumType.STRING)
    private TipoRecurso tipo;
    private Integer capacidade;
    private String localizacao;
    private String competencia;
    private boolean restrito;
    private boolean ativo = true;

    protected Recurso() {
    }

    public Recurso(String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
                   String competencia, boolean restrito) {
        atualizar(nome, tipo, capacidade, localizacao, competencia, restrito);
    }

    public final void atualizar(String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
                                String competencia, boolean restrito) {
        this.nome = nome;
        this.tipo = tipo;
        this.capacidade = capacidade;
        this.localizacao = localizacao;
        this.competencia = competencia;
        this.restrito = restrito;
    }

    public void desativar() { this.ativo = false; }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public TipoRecurso getTipo() { return tipo; }
    public Integer getCapacidade() { return capacidade; }
    public String getLocalizacao() { return localizacao; }
    public String getCompetencia() { return competencia; }
    public boolean isRestrito() { return restrito; }
    public boolean isAtivo() { return ativo; }
}
