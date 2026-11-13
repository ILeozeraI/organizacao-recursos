package br.senac.recursos.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** RF2 (cadastro/consulta), RF3 (pesquisa) e RF8 (manutenção). */
class RecursoApiIT extends IntegracaoBase {

    @Test
    void adminCadastraConsultaAtualizaEDesativa() throws Exception {
        String admin = login(ADMIN);
        long id = idCriado(postJson("/recursos", admin, Map.of("nome", "Sala 202", "tipo", "SALA",
                "capacidade", 35, "localizacao", "Bloco A - 2º andar", "restrito", false)));
        mvc.perform(comToken(get("/recursos/" + id), login(ANA)))
                .andExpect(jsonPath("$.nome").value("Sala 202")).andExpect(jsonPath("$.capacidade").value(35));
        mvc.perform(comToken(put("/recursos/" + id), admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Sala 202B\",\"tipo\":\"SALA\",\"capacidade\":30,\"restrito\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.restrito").value(true));
        mvc.perform(comToken(delete("/recursos/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/recursos/" + id), admin)).andExpect(status().isNotFound());
    }

    @Test
    void naoDesativaRecursoComReservaAtiva() throws Exception {
        reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z");
        mvc.perform(comToken(delete("/recursos/" + SALA_101), login(ADMIN)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RECURSO_COM_RESERVAS"));
    }

    @Test
    void validaCapacidadeNegativaENomeAusente() throws Exception {
        postJson("/recursos", login(ADMIN), Map.of("tipo", "SALA", "capacidade", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos", hasItem("capacidade: a capacidade não pode ser negativa")))
                .andExpect(jsonPath("$.campos", hasItem("nome: informe o nome")));
    }

    @Test
    void pesquisaPorTipoCapacidadeLocalizacaoECompetencia() throws Exception {
        String ana = login(ANA);
        mvc.perform(comToken(get("/recursos?tipo=SALA&capacidadeMinima=30"), ana))
                .andExpect(jsonPath("$", hasSize(2))) // Sala 101 (40) e Auditório (200); Lab 3 tem 25
                .andExpect(jsonPath("$[*].nome", not(hasItem("Laboratório 3"))));
        mvc.perform(comToken(get("/recursos?localizacao=almoxarifado"), ana)).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(comToken(get("/recursos?competencia=qualidade"), ana))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].nome").value("Prof. Carla Mendes"));
        mvc.perform(comToken(get("/recursos"), ana)).andExpect(jsonPath("$", hasSize(7)));
    }

    @Test
    void pesquisaPorDisponibilidadeExcluiReservadosEEmManutencao() throws Exception {
        String ana = login(ANA);
        reservar(ana, SALA_101, PROF_CARLA, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                .andExpect(status().isCreated());
        postJson("/recursos/" + AUDITORIO + "/manutencoes", login(ADMIN), Map.of(
                "inicio", "2026-12-01T00:00:00Z", "fim", "2026-12-02T00:00:00Z", "motivo", "Reforma"))
                .andExpect(status().isCreated());
        mvc.perform(comToken(get("/recursos/" + AUDITORIO + "/manutencoes"), ana))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].motivo").value("Reforma"));

        String janela = "disponivelInicio=2026-12-01T11:00:00Z&disponivelFim=2026-12-01T11:30:00Z";
        mvc.perform(comToken(get("/recursos?tipo=SALA&" + janela), ana))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].nome").value("Laboratório 3"));
        mvc.perform(comToken(get("/recursos?tipo=PROFESSOR&" + janela), ana))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].nome").value("Prof. Diego Rocha"));
    }

    @Test
    void manutencaoComFimAntesDoInicioDa400() throws Exception {
        postJson("/recursos/" + SALA_101 + "/manutencoes", login(ADMIN), Map.of(
                "inicio", "2026-12-02T00:00:00Z", "fim", "2026-12-01T00:00:00Z", "motivo", "x"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("INTERVALO_INVALIDO"));
    }

    @Test
    void parametroComTipoInvalidoDa400() throws Exception {
        mvc.perform(comToken(get("/recursos?tipo=NAVE"), login(ANA)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }
}
