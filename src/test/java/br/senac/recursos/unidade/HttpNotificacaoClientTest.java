package br.senac.recursos.unidade;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.recursos.integracao.HttpNotificacaoClient;
import br.senac.recursos.integracao.IntegracaoException;
import br.senac.recursos.integracao.NotificacaoClient.PedidoNotificacao;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * RF11 / RNF-INT-01: contrato HTTP da notificação isolado com WireMock
 * (servidor HTTP local, porta aleatória, roda offline e é determinístico).
 */
class HttpNotificacaoClientTest {

    private static final String TOKEN = "token-de-teste";

    @RegisterExtension
    static WireMockExtension provedor = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private HttpNotificacaoClient client;
    private final PedidoNotificacao pedido = new PedidoNotificacao(42L, "RESERVA_APROVADA", 7L);

    @BeforeEach
    void configurar() {
        client = new HttpNotificacaoClient(provedor.baseUrl(), TOKEN, Duration.ofMillis(500),
                Duration.ofMillis(800));
    }

    @Test
    @DisplayName("INT-01: 200 válido -> devolve id externo e respeita o contrato do request")
    void caminhoFeliz() {
        provedor.stubFor(post("/notificacoes").withHeader("X-Api-Key", equalTo(TOKEN))
                .willReturn(okJson("{\"id\":\"ntf-1\",\"status\":\"ACEITA\"}")));

        assertThat(client.enviar(pedido)).isEqualTo("ntf-1");

        provedor.verify(1, postRequestedFor(urlEqualTo("/notificacoes"))
                .withHeader("X-Api-Key", equalTo(TOKEN))
                .withRequestBody(matchingJsonPath("$.reservaId", equalTo("42")))
                .withRequestBody(matchingJsonPath("$.evento", equalTo("RESERVA_APROVADA"))));
    }

    @ParameterizedTest(name = "INT-02: HTTP {0} -> REJEITADA, sem retry")
    @ValueSource(ints = {400, 401, 404})
    void erro4xxNaoERepetido(int status) {
        provedor.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .extracting("tipo").isEqualTo(IntegracaoException.Tipo.REJEITADA);
        provedor.verify(1, postRequestedFor(urlEqualTo("/notificacoes")));
    }

    @ParameterizedTest(name = "INT-03: HTTP {0} -> INDISPONIVEL")
    @ValueSource(ints = {500, 503})
    void erro5xxViraIndisponivel(int status) {
        provedor.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> client.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .extracting("tipo").isEqualTo(IntegracaoException.Tipo.INDISPONIVEL);
    }

    @Test
    @DisplayName("INT-04: resposta lenta estoura o timeout e é interrompida")
    void timeout() {
        provedor.stubFor(post("/notificacoes").willReturn(okJson("{\"id\":\"x\",\"status\":\"ACEITA\"}")
                .withFixedDelay(2000)));

        long inicio = System.nanoTime();
        assertThatThrownBy(() -> client.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .extracting("tipo").isEqualTo(IntegracaoException.Tipo.INDISPONIVEL);
        // Tolerância folgada: só garante que não esperou os 2 s inteiros.
        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1900));
    }

    @ParameterizedTest(name = "INT-05: corpo inválido [{0}] -> RESPOSTA_INVALIDA")
    @ValueSource(strings = {
            "{\"id\":\"ntf-1\"",                      // JSON malformado
            "{\"status\":\"ACEITA\"}",                // campo obrigatório ausente
            "{\"id\":\"ntf-1\",\"status\":\"OUTRO\"}", // valor de enum desconhecido
            "{\"id\":\"\",\"status\":\"ACEITA\"}"     // id vazio
    })
    void corpoInvalidoERejeitado(String corpo) {
        provedor.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody(corpo)));

        assertThatThrownBy(() -> client.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .extracting("tipo").isEqualTo(IntegracaoException.Tipo.RESPOSTA_INVALIDA);
    }

    @Test
    @DisplayName("Segurança: corpo de erro com o token não vaza na mensagem da exceção")
    void naoVazaSegredoDoProvedor() {
        provedor.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(500)
                .withBody("{\"erro\":\"falha\",\"debug\":\"X-Api-Key=" + TOKEN + "\"}")));

        assertThatThrownBy(() -> client.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(TOKEN).doesNotContain("debug"));
    }

    @Test
    @DisplayName("Provedor fora do ar (conexão recusada) -> INDISPONIVEL")
    void provedorForaDoAr() {
        HttpNotificacaoClient semServidor = new HttpNotificacaoClient("http://127.0.0.1:9", TOKEN,
                Duration.ofMillis(300), Duration.ofMillis(300));
        assertThatThrownBy(() -> semServidor.enviar(pedido))
                .isInstanceOf(IntegracaoException.class)
                .extracting("tipo").isEqualTo(IntegracaoException.Tipo.INDISPONIVEL);
    }
}
