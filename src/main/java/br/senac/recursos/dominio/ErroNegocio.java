package br.senac.recursos.dominio;

import org.springframework.http.HttpStatus;

/**
 * Erro de negócio com código estável (usado em testes e na RTM) e status HTTP.
 * A mensagem é sempre compreensível e nunca carrega detalhes internos.
 */
public class ErroNegocio extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ErroNegocio(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public static ErroNegocio invalido(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }

    public static ErroNegocio naoEncontrado(String mensagem) {
        return new ErroNegocio(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", mensagem);
    }

    public static ErroNegocio conflito(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.CONFLICT, codigo, mensagem);
    }

    public static ErroNegocio proibido(String codigo, String mensagem) {
        return new ErroNegocio(HttpStatus.FORBIDDEN, codigo, mensagem);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
