package br.senac.recursos.integracao;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * RN-09 (desenvolvida com TDD): o número de participantes informado na reserva não pode
 * exceder a capacidade da sala. Sala 101 tem capacidade 40. Fronteira em trinca: 39, 40, 41.
 */
class CapacidadeSalaIT extends IntegracaoBase {

    @ParameterizedTest(name = "RN-09: {0} participantes na Sala 101 (capacidade 40) -> HTTP {1}")
    @CsvSource({"39, 201", "40, 201", "41, 400"})
    void respeitaCapacidadeDaSala(int participantes, int statusEsperado) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("recursoId", SALA_101);
        corpo.put("inicio", "2026-12-01T10:00:00Z");
        corpo.put("fim", "2026-12-01T11:00:00Z");
        corpo.put("participantes", participantes);
        var resposta = postJson("/reservas", login(ANA), corpo).andExpect(status().is(statusEsperado));
        if (statusEsperado == 400) {
            resposta.andExpect(jsonPath("$.codigo").value("CAPACIDADE_EXCEDIDA"));
        } else {
            resposta.andExpect(jsonPath("$.participantes").value(participantes));
        }
    }

    @ParameterizedTest(name = "RN-09: participantes = {0} -> 400")
    @CsvSource({"0", "-1"})
    void participantesPrecisaSerPositivo(int participantes) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("recursoId", SALA_101);
        corpo.put("inicio", "2026-12-01T10:00:00Z");
        corpo.put("fim", "2026-12-01T11:00:00Z");
        corpo.put("participantes", participantes);
        postJson("/reservas", login(ANA), corpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }
}
