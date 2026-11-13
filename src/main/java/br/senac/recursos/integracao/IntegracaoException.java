package br.senac.recursos.integracao;

/**
 * Erro de integração traduzido para o domínio. A mensagem é sempre genérica:
 * NUNCA carrega o corpo da resposta externa (que pode conter segredos).
 */
public class IntegracaoException extends RuntimeException {

    public enum Tipo { INDISPONIVEL, REJEITADA, RESPOSTA_INVALIDA }

    private final Tipo tipo;

    public IntegracaoException(Tipo tipo, String mensagem) {
        super(mensagem);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }
}
