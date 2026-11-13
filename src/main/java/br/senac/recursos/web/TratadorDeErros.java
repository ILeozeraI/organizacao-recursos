package br.senac.recursos.web;

import br.senac.recursos.dominio.ErroNegocio;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * RF13 + tratamento seguro de erros: toda resposta de erro é um ProblemDetail com
 * "codigo" estável e mensagem compreensível. Nunca há stack trace, SQL ou nome de classe.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    @ExceptionHandler(ErroNegocio.class)
    ResponseEntity<ProblemDetail> negocio(ErroNegocio e) {
        return resposta(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validacao(MethodArgumentNotValidException e) {
        List<String> campos = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .sorted()
                .toList();
        ResponseEntity<ProblemDetail> r = resposta(HttpStatus.BAD_REQUEST, "DADOS_INVALIDOS",
                "Verifique os campos informados.");
        r.getBody().setProperty("campos", campos);
        return r;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    ResponseEntity<ProblemDetail> requisicaoMalFormada(Exception e) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA",
                "Requisição inválida: confira o formato dos dados (datas no padrão ISO-8601, ex.: 2026-11-20T13:00:00Z).");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> metodo(HttpRequestMethodNotSupportedException e) {
        return resposta(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NAO_SUPORTADO", "Operação não suportada nesta rota.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> naoEncontrado(NoResourceFoundException e) {
        return resposta(HttpStatus.NOT_FOUND, "NAO_ENCONTRADO", "Rota não encontrada.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception e) {
        // Detalhe só no log do servidor; o cliente recebe mensagem neutra.
        LOG.error("Erro inesperado", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private static ResponseEntity<ProblemDetail> resposta(HttpStatus status, String codigo, String mensagem) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, mensagem);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("codigo", codigo);
        return ResponseEntity.status(status).body(pd);
    }
}
