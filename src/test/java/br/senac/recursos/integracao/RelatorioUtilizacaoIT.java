package br.senac.recursos.integracao;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * RF12 em caixa-preta. Massa pequena semeada pelo próprio teste e valores esperados
 * calculados À MÃO (a conta está nos comentários), nunca copiados da saída da aplicação.
 */
class RelatorioUtilizacaoIT extends IntegracaoBase {

    private static final String DIA_01_12 = "/relatorios/utilizacao?inicio=2026-12-01T00:00:00Z&fim=2026-12-02T00:00:00Z";

    @Test
    void calculaHorasPorRecursoCargaPorProfessorEConflitosEvitados() throws Exception {
        String ana = login(ANA);
        String bruno = login(BRUNO);
        // Sala 101, 10h-12h = 2h (aprovada automaticamente)
        reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z").andExpect(status().isCreated());
        // Sala 101, 13h-14h30 = 1,5h, com a Prof. Carla
        reservar(ana, SALA_101, PROF_CARLA, "2026-12-01T13:00:00Z", "2026-12-01T14:30:00Z").andExpect(status().isCreated());
        // Projetor, 30/11 23h -> 01/12 01h: cruza a borda, conta só 00h-01h = 1h
        reservar(bruno, PROJETOR, null, "2026-11-30T23:00:00Z", "2026-12-01T01:00:00Z").andExpect(status().isCreated());
        // Cancelada: não conta como uso
        long cancelada = idCriado(reservar(ana, SALA_101, null, "2026-12-01T15:00:00Z", "2026-12-01T16:00:00Z"));
        mvc.perform(comToken(post("/reservas/" + cancelada + "/cancelar"), ana)).andExpect(status().isOk());
        // Solicitada (recurso restrito, sem aprovação): não conta como uso
        reservar(ana, LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z").andExpect(status().isCreated());
        // Tentativa recusada por sobreposição no período: 1 conflito evitado
        reservar(bruno, SALA_101, null, "2026-12-01T11:00:00Z", "2026-12-01T12:00:00Z").andExpect(status().isConflict());

        // Esperado: Sala 101 = 2 + 1,5 = 3,5h ; Projetor = 1h ; Prof. Carla = 1,5h ; conflitos = 1
        mvc.perform(comToken(get(DIA_01_12), login(RITA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos", hasSize(2)))
                .andExpect(jsonPath("$.recursos[0].recursoId").value(SALA_101))
                .andExpect(jsonPath("$.recursos[0].nome").value("Sala 101"))
                .andExpect(jsonPath("$.recursos[0].horasReservadas").value(3.5))
                .andExpect(jsonPath("$.recursos[1].recursoId").value(PROJETOR))
                .andExpect(jsonPath("$.recursos[1].horasReservadas").value(1.0))
                .andExpect(jsonPath("$.professores", hasSize(1)))
                .andExpect(jsonPath("$.professores[0].nome").value("Prof. Carla Mendes"))
                .andExpect(jsonPath("$.professores[0].horasAlocadas").value(1.5))
                .andExpect(jsonPath("$.conflitosEvitados").value(1));
    }

    @Test
    void periodoVazioRetornaListasVaziasEZeroConflitos() throws Exception {
        mvc.perform(comToken(get("/relatorios/utilizacao?inicio=2027-01-01T00:00:00Z&fim=2027-01-02T00:00:00Z"),
                        login(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos", hasSize(0)))
                .andExpect(jsonPath("$.professores", hasSize(0)))
                .andExpect(jsonPath("$.conflitosEvitados").value(0));
    }

    @Test
    void periodoInvertidoDa400() throws Exception {
        mvc.perform(comToken(get("/relatorios/utilizacao?inicio=2026-12-02T00:00:00Z&fim=2026-12-01T00:00:00Z"),
                        login(ADMIN)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("INTERVALO_INVALIDO"));
    }

    @Test
    void semTokenDa401ESolicitanteDa403() throws Exception {
        mvc.perform(get(DIA_01_12)).andExpect(status().isUnauthorized());
        mvc.perform(comToken(get(DIA_01_12), login(ANA))).andExpect(status().isForbidden());
    }
}
