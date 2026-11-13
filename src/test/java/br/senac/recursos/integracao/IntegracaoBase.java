package br.senac.recursos.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Base dos testes de integração/API (caixa-preta via HTTP com MockMvc).
 * Dados de demonstração (V2): usuários 1=Ana(Solicitante) 2=Bruno(Solicitante)
 * 3=Rita(Responsável) 4=Admin; recursos 1=Sala 101, 2=Laboratório 3 (restrito),
 * 3=Auditório (restrito), 4=Projetor, 5=Notebook, 6=Prof. Carla, 7=Prof. Diego.
 */
@SpringBootTest(properties = {
        // Provedor de notificação inexistente: a falha de notificação não pode afetar a reserva.
        "app.notificacao.base-url=http://127.0.0.1:9",
        "spring.datasource.hikari.maximum-pool-size=30"})
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
public abstract class IntegracaoBase {

    public static final String ANA = "ana@exemplo.invalid";
    public static final String BRUNO = "bruno@exemplo.invalid";
    public static final String RITA = "rita@exemplo.invalid";
    public static final String ADMIN = "admin@exemplo.invalid";
    public static final long SALA_101 = 1, LAB_3 = 2, AUDITORIO = 3, PROJETOR = 4, NOTEBOOK = 5,
            PROF_CARLA = 6, PROF_DIEGO = 7;

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE reserva, auditoria_reserva, movimentacao_material, conflito_evitado, "
                + "notificacao, manutencao, sessao RESTART IDENTITY CASCADE");
        jdbc.update("DELETE FROM recurso WHERE id > 7");
        jdbc.update("UPDATE recurso SET ativo = TRUE");
    }

    protected String login(String email) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "senha", "senha123"))))
                .andExpect(status().isOk()).andReturn();
        return ler(r).get("token").asText();
    }

    protected MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req, String token) {
        return token == null ? req : req.header("Authorization", "Bearer " + token);
    }

    protected ResultActions postJson(String url, String token, Object corpo) throws Exception {
        return mvc.perform(comToken(post(url), token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(corpo)));
    }

    protected ResultActions reservar(String token, long recursoId, Long professorId, String inicio, String fim)
            throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("recursoId", recursoId);
        corpo.put("professorId", professorId);
        corpo.put("inicio", inicio);
        corpo.put("fim", fim);
        corpo.put("finalidade", "Aula de teste");
        return postJson("/reservas", token, corpo);
    }

    protected long idCriado(ResultActions r) throws Exception {
        return ler(r.andExpect(status().isCreated()).andReturn()).get("id").asLong();
    }

    protected JsonNode ler(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected static Instant t(String iso) {
        return Instant.parse(iso);
    }
}
