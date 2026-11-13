package br.senac.recursos.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Testes de API em caixa-preta das regras de reserva (RF4 a RF9), com PostgreSQL real. */
class ReservaApiIT extends IntegracaoBase {

    @Nested
    @DisplayName("RF4/RF7: criação e aprovação")
    class Criacao {
        @Test
        void recursoNaoRestritoEhAprovadoAutomaticamente() throws Exception {
            String ana = login(ANA);
            reservar(ana, SALA_101, PROF_CARLA, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("APROVADA"))
                    .andExpect(jsonPath("$.professorId").value(PROF_CARLA));
        }

        @Test
        void recursoRestritoFicaSolicitadoAteAprovacaoDoResponsavel() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z"));
            mvc.perform(comToken(get("/reservas/" + id), ana)).andExpect(jsonPath("$.status").value("SOLICITADA"));

            mvc.perform(comToken(post("/reservas/" + id + "/aprovar"), login(RITA)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APROVADA"));
            // Provedor de notificação está fora do ar nos testes: a aprovação continua valendo.
            assertThat(jdbc.queryForObject("select status from notificacao where reserva_id = ?", String.class, id))
                    .isEqualTo("FALHA");
        }

        @Test
        void responsavelRejeitaComMotivo() throws Exception {
            long id = idCriado(reservar(login(ANA), AUDITORIO, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z"));
            postJson("/reservas/" + id + "/rejeitar", login(RITA), Map.of("motivo", "Evento institucional"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJEITADA"));
        }

        @Test
        void naoAprovaReservaJaAprovada() throws Exception {
            long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/aprovar"), login(RITA)))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        }

        @Test
        void naoReservaProfessorComoRecursoPrincipal() throws Exception {
            reservar(login(ANA), PROF_CARLA, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("RECURSO_INVALIDO"));
        }

        @Test
        void professorInformadoPrecisaSerProfessor() throws Exception {
            reservar(login(ANA), SALA_101, PROJETOR, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z")
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("PROFESSOR_INVALIDO"));
        }

        @Test
        void recursoInexistenteDa404() throws Exception {
            reservar(login(ANA), 999, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z")
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("NAO_ENCONTRADO"));
        }
    }

    @Nested
    @DisplayName("RF5: sobreposição de horários (recurso e agenda do professor)")
    class Sobreposicao {
        @Test
        void recusaSobreposicaoNoMesmoRecursoERegistraConflitoEvitado() throws Exception {
            reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                    .andExpect(status().isCreated());
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T11:59:59Z", "2026-12-01T13:00:00Z")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_HORARIO"));
            assertThat(jdbc.queryForObject("select count(*) from conflito_evitado", Long.class)).isEqualTo(1L);
        }

        @Test
        void aceitaReservaQueComecaExatamenteNoFimDaOutra() throws Exception {
            reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                    .andExpect(status().isCreated());
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T12:00:00Z", "2026-12-01T13:00:00Z")
                    .andExpect(status().isCreated());
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z")
                    .andExpect(status().isCreated());
        }

        @Test
        void recusaProfessorEmDuasSalasNoMesmoHorario() throws Exception {
            reservar(login(ANA), SALA_101, PROF_CARLA, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                    .andExpect(status().isCreated());
            reservar(login(BRUNO), LAB_3, PROF_CARLA, "2026-12-01T11:00:00Z", "2026-12-01T12:30:00Z")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("CONFLITO_AGENDA_PROFESSOR"));
        }

        @Test
        void reservaCanceladaLiberaOHorario() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/cancelar"), ana))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELADA"));
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("RF8: bloqueio por manutenção")
    class Manutencao {
        @Test
        void naoReservaRecursoEmManutencaoEFronteirasSaoSemiabertas() throws Exception {
            postJson("/recursos/" + SALA_101 + "/manutencoes", login(ADMIN), Map.of(
                    "inicio", "2026-12-01T10:00:00Z", "fim", "2026-12-01T12:00:00Z", "motivo", "Pintura"))
                    .andExpect(status().isCreated());
            String ana = login(ANA);
            // cruza 1 segundo -> bloqueado
            reservar(ana, SALA_101, null, "2026-12-01T09:00:00Z", "2026-12-01T10:00:01Z")
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RECURSO_EM_MANUTENCAO"));
            // fim exato no início da manutenção -> permitido
            reservar(ana, SALA_101, null, "2026-12-01T09:00:00Z", "2026-12-01T10:00:00Z")
                    .andExpect(status().isCreated());
            // início exato no fim da manutenção -> permitido
            reservar(ana, SALA_101, null, "2026-12-01T12:00:00Z", "2026-12-01T13:00:00Z")
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("RF4: alteração e cancelamento (posse)")
    class Alteracao {
        @Test
        void donoAlteraHorarioDeReservaAprovadaNaoRestrita() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(put("/reservas/" + id), ana).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inicio\":\"2026-12-01T14:00:00Z\",\"fim\":\"2026-12-01T15:00:00Z\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.inicio").value("2026-12-01T14:00:00Z"));
        }

        @Test
        void alteracaoNaoPodeCriarSobreposicao() throws Exception {
            String ana = login(ANA);
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T14:00:00Z", "2026-12-01T15:00:00Z")
                    .andExpect(status().isCreated());
            long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(put("/reservas/" + id), ana).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inicio\":\"2026-12-01T14:30:00Z\",\"fim\":\"2026-12-01T15:30:00Z\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        void reservaRestritaJaAprovadaNaoPodeSerAlterada() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/aprovar"), login(RITA))).andExpect(status().isOk());
            mvc.perform(comToken(put("/reservas/" + id), ana).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inicio\":\"2026-12-01T14:00:00Z\",\"fim\":\"2026-12-01T15:00:00Z\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ALTERACAO_NAO_PERMITIDA"));
        }

        @Test
        void solicitanteNaoAlteraNemCancelaReservaDeOutro() throws Exception {
            long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            String bruno = login(BRUNO);
            mvc.perform(comToken(put("/reservas/" + id), bruno).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"inicio\":\"2026-12-01T14:00:00Z\",\"fim\":\"2026-12-01T15:00:00Z\"}"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("NAO_E_DONO"));
            mvc.perform(comToken(post("/reservas/" + id + "/cancelar"), bruno))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("NAO_E_DONO"));
            mvc.perform(comToken(get("/reservas/" + id), bruno)).andExpect(status().isForbidden());
        }

        @Test
        void solicitanteListaSoAsProprias() throws Exception {
            reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z");
            reservar(login(BRUNO), SALA_101, null, "2026-12-01T12:00:00Z", "2026-12-01T13:00:00Z");
            mvc.perform(comToken(get("/reservas"), login(ANA))).andExpect(jsonPath("$", hasSize(1)));
            mvc.perform(comToken(get("/reservas"), login(RITA))).andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("Ciclo de vida e exclusão")
    class CicloDeVida {
        @Test
        void fluxoPrincipalAteConcluir() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/iniciar"), ana)).andExpect(jsonPath("$.status").value("EM_USO"));
            mvc.perform(comToken(post("/reservas/" + id + "/concluir"), ana)).andExpect(jsonPath("$.status").value("CONCLUIDA"));
            mvc.perform(comToken(post("/reservas/" + id + "/cancelar"), ana))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("TRANSICAO_INVALIDA"));
        }

        @Test
        void responsavelRegistraNaoComparecimento() throws Exception {
            long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/nao-compareceu"), login(RITA)))
                    .andExpect(jsonPath("$.status").value("NAO_COMPARECEU"));
        }

        @Test
        void reservaIniciadaNaoPodeSerExcluida() throws Exception {
            String ana = login(ANA);
            long id = idCriado(reservar(ana, SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/iniciar"), ana)).andExpect(status().isOk());
            mvc.perform(comToken(delete("/reservas/" + id), login(ADMIN)))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("RESERVA_INICIADA"));
        }

        @Test
        void reservaNaoIniciadaPodeSerExcluidaEOHistoricoPermanece() throws Exception {
            long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(delete("/reservas/" + id), login(ADMIN))).andExpect(status().isNoContent());
            mvc.perform(comToken(get("/reservas/" + id + "/historico"), login(ADMIN)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
            mvc.perform(comToken(get("/reservas/" + id), login(ADMIN))).andExpect(status().isNotFound());
        }

        @Test
        void historicoDeReservaInexistenteDa404() throws Exception {
            mvc.perform(comToken(get("/reservas/9999/historico"), login(ADMIN))).andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("RF9: retirada e devolução de materiais")
    class Materiais {
        @Test
        void retiradaEDevolucaoMudamEstadoERegistramMovimentacao() throws Exception {
            String ana = login(ANA);
            String rita = login(RITA);
            long id = idCriado(reservar(ana, PROJETOR, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            postJson("/reservas/" + id + "/retirada", rita, Map.of("motivo", "Retirado com cabo HDMI"))
                    .andExpect(jsonPath("$.status").value("EM_USO"));
            mvc.perform(comToken(post("/reservas/" + id + "/devolucao"), rita))
                    .andExpect(jsonPath("$.status").value("CONCLUIDA"));
            mvc.perform(comToken(get("/reservas/" + id + "/movimentacoes"), ana))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].tipo").value("RETIRADA"))
                    .andExpect(jsonPath("$[0].observacao").value("Retirado com cabo HDMI"))
                    .andExpect(jsonPath("$[1].tipo").value("DEVOLUCAO"));
        }

        @Test
        void retiradaSoValeParaMaterial() throws Exception {
            long id = idCriado(reservar(login(ANA), SALA_101, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
            mvc.perform(comToken(post("/reservas/" + id + "/retirada"), login(RITA)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("NAO_E_MATERIAL"));
        }
    }

    @Nested
    @DisplayName("RF13 + segurança: validação de entrada e erros sem vazamento")
    class Validacao {
        @Test
        void terminoAntesDoInicioDa400ComMensagemCompreensivel() throws Exception {
            reservar(login(ANA), SALA_101, null, "2026-12-01T12:00:00Z", "2026-12-01T10:00:00Z")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("INTERVALO_INVALIDO"))
                    .andExpect(jsonPath("$.detail").value("O horário de término deve ser posterior ao de início."));
        }

        @Test
        void camposAusentesListamOsCampos() throws Exception {
            postJson("/reservas", login(ANA), Map.of())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"))
                    .andExpect(jsonPath("$.campos", hasSize(3)));
        }

        @Test
        void jsonMalformadoEDataInvalidaNaoVazamDetalhesInternos() throws Exception {
            String ana = login(ANA);
            mvc.perform(comToken(post("/reservas"), ana).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"recursoId\": 1, \"inicio\": \"amanhã\""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                    .andExpect(content().string(not(containsString("Exception"))))
                    .andExpect(content().string(not(containsString("jackson"))))
                    .andExpect(content().string(not(containsString("at br.senac"))));
        }

        @Test
        void rotaInexistenteEMetodoNaoSuportado() throws Exception {
            String ana = login(ANA);
            mvc.perform(comToken(get("/nao-existe"), ana)).andExpect(status().isNotFound());
            mvc.perform(comToken(patch("/reservas"), ana)).andExpect(status().isMethodNotAllowed());
        }
    }
}
