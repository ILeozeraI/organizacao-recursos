package br.senac.recursos.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/** Escreve 401/403 no formato ProblemDetail, sem detalhes internos. */
@Component
public class RespostaErroSeguranca {

    private final ObjectMapper mapper;

    public RespostaErroSeguranca(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void escrever(HttpServletResponse response, HttpStatus status, String codigo, String mensagem)
            throws IOException {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, mensagem);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("codigo", codigo);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), pd);
    }
}
