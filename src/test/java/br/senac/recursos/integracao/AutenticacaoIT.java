package br.senac.recursos.integracao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** RF1: login, token, logout e mensagens de erro de autenticação. */
class AutenticacaoIT extends IntegracaoBase {

    @Test
    void loginDevolveTokenEPerfil() throws Exception {
        postJson("/auth/login", null, Map.of("email", RITA, "senha", "senha123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.perfil").value("RESPONSAVEL"))
                .andExpect(jsonPath("$.expiraEm").exists());
    }

    @Test
    void senhaErradaEEmailInexistenteTemAMesmaResposta() throws Exception {
        postJson("/auth/login", null, Map.of("email", ANA, "senha", "errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CREDENCIAIS_INVALIDAS"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        postJson("/auth/login", null, Map.of("email", "ninguem@exemplo.invalid", "senha", "senha123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
    }

    @Test
    void loginComEmailMalFormadoDa400() throws Exception {
        postJson("/auth/login", null, Map.of("email", "nao-e-email", "senha", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }

    @Test
    void euRetornaUsuarioAutenticado() throws Exception {
        mvc.perform(comToken(get("/auth/eu"), login(ANA)))
                .andExpect(jsonPath("$.nome").value("Ana Solicitante"))
                .andExpect(jsonPath("$.perfil").value("SOLICITANTE"));
    }

    @Test
    void tokenRevogadoNoLogoutDeixaDeValer() throws Exception {
        String token = login(ANA);
        mvc.perform(comToken(post("/auth/logout"), token)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/reservas"), token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_REVOGADO"));
    }

    @Test
    void tokenInventadoDa401ESemTokenDa401() throws Exception {
        mvc.perform(comToken(get("/reservas"), "abc123"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_INVALIDO"));
        mvc.perform(get("/reservas"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_AUSENTE"));
    }

    @Test
    void tokenExpiradoDa401() throws Exception {
        String token = login(ANA);
        jdbc.update("update sessao set expira_em = now() - interval '1 second' where token = ?", token);
        mvc.perform(comToken(get("/reservas"), token))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("TOKEN_EXPIRADO"));
    }

    @Test
    void documentacaoDaApiEInterfaceSaoPublicas() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Organização de Recursos - API"));
        mvc.perform(get("/index.html")).andExpect(status().isOk());
    }
}
