package br.senac.recursos.integracao;

import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adapter HTTP da notificação. URL, token e timeouts vêm de configuração
 * (variáveis de ambiente), nunca fixos no código.
 */
@Component
public class HttpNotificacaoClient implements NotificacaoClient {

    private final RestClient http;

    public HttpNotificacaoClient(@Value("${app.notificacao.base-url}") String baseUrl,
                                 @Value("${app.notificacao.token}") String token,
                                 @Value("${app.notificacao.connect-timeout:500ms}") Duration connectTimeout,
                                 @Value("${app.notificacao.read-timeout:1500ms}") Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("X-Api-Key", token)
                .build();
    }

    @Override
    public String enviar(PedidoNotificacao pedido) {
        RespostaNotificacao resposta;
        try {
            resposta = http.post()
                    .uri("/notificacoes")
                    .body(pedido)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IntegracaoException(IntegracaoException.Tipo.REJEITADA,
                                "Notificação recusada pelo provedor (HTTP " + res.getStatusCode().value() + ").");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new IntegracaoException(IntegracaoException.Tipo.INDISPONIVEL,
                                "Provedor de notificação indisponível (HTTP " + res.getStatusCode().value() + ").");
                    })
                    .body(RespostaNotificacao.class);
        } catch (IntegracaoException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new IntegracaoException(IntegracaoException.Tipo.INDISPONIVEL,
                    "Provedor de notificação não respondeu dentro do tempo limite.");
        } catch (RestClientException e) {
            if (causadoPorIo(e)) {
                // DEF-001: timeout durante a leitura do corpo chega embrulhado em RestClientException.
                throw new IntegracaoException(IntegracaoException.Tipo.INDISPONIVEL,
                        "Provedor de notificação não respondeu dentro do tempo limite.");
            }
            throw new IntegracaoException(IntegracaoException.Tipo.RESPOSTA_INVALIDA,
                    "Resposta do provedor de notificação em formato inválido.");
        }
        return validar(resposta);
    }

    private static boolean causadoPorIo(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    /** Status 200 não garante dado válido: valida o contrato antes de usar. */
    static String validar(RespostaNotificacao resposta) {
        if (resposta == null || resposta.id() == null || resposta.id().isBlank()
                || !"ACEITA".equals(resposta.status())) {
            throw new IntegracaoException(IntegracaoException.Tipo.RESPOSTA_INVALIDA,
                    "Resposta do provedor de notificação sem os campos obrigatórios.");
        }
        return resposta.id();
    }

    record RespostaNotificacao(String id, String status) {
    }
}
