package br.senac.recursos.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.repositorio.ReservaRepository;
import br.senac.recursos.servico.AuditoriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** RF10: histórico auditável, gravado na mesma transação da mudança de estado. */
class AuditoriaIT extends IntegracaoBase {

    @Autowired
    AuditoriaService auditoria;
    @Autowired
    ReservaRepository reservas;
    @Autowired
    PlatformTransactionManager txManager;

    @Test
    void cadaMudancaDeEstadoGeraRegistroComUsuarioInstanteEMotivo() throws Exception {
        String ana = login(ANA);
        long id = idCriado(reservar(ana, LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
        mvc.perform(comToken(post("/reservas/" + id + "/aprovar"), login(RITA))).andExpect(status().isOk());
        mvc.perform(comToken(post("/reservas/" + id + "/cancelar"), ana)).andExpect(status().isOk());

        mvc.perform(comToken(get("/reservas/" + id + "/historico"), login(RITA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].estadoAnterior").doesNotExist())
                .andExpect(jsonPath("$[0].estadoNovo").value("SOLICITADA"))
                .andExpect(jsonPath("$[0].usuarioId").value(1))
                .andExpect(jsonPath("$[1].estadoAnterior").value("SOLICITADA"))
                .andExpect(jsonPath("$[1].estadoNovo").value("APROVADA"))
                .andExpect(jsonPath("$[1].usuarioId").value(3))
                .andExpect(jsonPath("$[2].estadoNovo").value("CANCELADA"))
                .andExpect(jsonPath("$[2].instante").exists())
                .andExpect(jsonPath("$[2].motivo").value("Cancelada"));
    }

    @Test
    void rollbackDaMudancaNaoDeixaAuditoriaOrfa() throws Exception {
        long id = idCriado(reservar(login(ANA), LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
        long antes = jdbc.queryForObject("select count(*) from auditoria_reserva", Long.class);

        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            var r = reservas.findById(id).orElseThrow();
            StatusReserva anterior = r.transitarPara(StatusReserva.APROVADA);
            auditoria.registrar(id, 3L, anterior, StatusReserva.APROVADA, "teste de rollback");
            reservas.saveAndFlush(r);
            throw new IllegalStateException("falha simulada depois de gravar");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("select count(*) from auditoria_reserva", Long.class)).isEqualTo(antes);
        assertThat(jdbc.queryForObject("select status from reserva where id = ?", String.class, id))
                .isEqualTo("SOLICITADA");
    }

    @Test
    void auditoriaForaDeTransacaoEhProibida() {
        assertThatThrownBy(() -> auditoria.registrar(1L, 1L, null, StatusReserva.SOLICITADA, "x"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void historicoNaoTemRotaDeAlteracaoNemExclusao() throws Exception {
        long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
        String admin = login(ADMIN);
        mvc.perform(comToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/reservas/" + id + "/historico"), admin)).andExpect(status().isMethodNotAllowed());
        mvc.perform(comToken(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/reservas/" + id + "/historico"), admin)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void solicitanteNaoConsultaHistorico() throws Exception {
        String ana = login(ANA);
        long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
        mvc.perform(comToken(get("/reservas/" + id + "/historico"), ana)).andExpect(status().isForbidden());
    }
}
