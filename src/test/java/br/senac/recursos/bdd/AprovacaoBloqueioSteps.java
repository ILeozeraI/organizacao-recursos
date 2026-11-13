package br.senac.recursos.bdd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Então;
import io.cucumber.java.pt.Quando;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/** Os passos chamam a API real (MockMvc) com token de cada perfil; usam as mesmas palavras do cenário. */
public class AprovacaoBloqueioSteps {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JdbcTemplate jdbc;

    private MockHttpServletResponse ultimaResposta;
    private Long reservaId;

    @Before
    public void limparBanco() {
        jdbc.execute("TRUNCATE reserva, auditoria_reserva, movimentacao_material, conflito_evitado, "
                + "notificacao, manutencao, sessao RESTART IDENTITY CASCADE");
        reservaId = null;
    }

    @Dado("que {string} reservou o recurso {long} de {string} até {string}")
    public void queReservou(String email, long recurso, String inicio, String fim) throws Exception {
        reserva(email, recurso, inicio, fim);
        assertThat(ultimaResposta.getStatus()).isEqualTo(201);
    }

    @Dado("que o recurso {long} está em manutenção de {string} até {string}")
    public void emManutencao(long recurso, String inicio, String fim) throws Exception {
        enviar("/recursos/" + recurso + "/manutencoes", token("admin@exemplo.invalid"),
                Map.of("inicio", inicio, "fim", fim, "motivo", "Manutenção preventiva"));
        assertThat(ultimaResposta.getStatus()).isEqualTo(201);
    }

    @Quando("{string} reserva o recurso {long} de {string} até {string}")
    public void reserva(String email, long recurso, String inicio, String fim) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("recursoId", recurso);
        corpo.put("inicio", inicio);
        corpo.put("fim", fim);
        enviar("/reservas", token(email), corpo);
        if (ultimaResposta.getStatus() == 201) {
            reservaId = ler().get("id").asLong();
        }
    }

    @Quando("{string} aprova a reserva")
    public void aprova(String email) throws Exception {
        ultimaResposta = mvc.perform(post("/reservas/" + reservaId + "/aprovar")
                .header("Authorization", "Bearer " + token(email))).andReturn().getResponse();
    }

    @Então("a resposta tem status {int}")
    public void status(int esperado) throws Exception {
        assertThat(ultimaResposta.getStatus()).as(ultimaResposta.getContentAsString()).isEqualTo(esperado);
    }

    @Então("a reserva fica com status {string}")
    public void statusDaReserva(String esperado) {
        assertThat(jdbc.queryForObject("select status from reserva where id = ?", String.class, reservaId))
                .isEqualTo(esperado);
    }

    @Então("o código do erro é {string}")
    public void codigoDoErro(String codigo) throws Exception {
        assertThat(ler().get("codigo").asText()).isEqualTo(codigo);
    }

    private String token(String email) throws Exception {
        MockHttpServletResponse r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "senha", "senha123"))))
                .andReturn().getResponse();
        return json.readTree(r.getContentAsString()).get("token").asText();
    }

    private void enviar(String url, String token, Object corpo) throws Exception {
        ultimaResposta = mvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(corpo)))
                .andReturn().getResponse();
    }

    private JsonNode ler() throws Exception {
        return json.readTree(ultimaResposta.getContentAsString());
    }
}
