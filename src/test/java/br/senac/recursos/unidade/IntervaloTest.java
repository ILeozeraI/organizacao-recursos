package br.senac.recursos.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.dominio.Intervalo;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** RN-01 (término após início) e RN-02 (sem sobreposição): fronteiras em trinca. */
class IntervaloTest {

    private static Intervalo i(String ini, String fim) {
        return new Intervalo(Instant.parse(ini), Instant.parse(fim));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} deve ser rejeitado")
    @CsvSource({
            "2026-12-01T10:00:00Z, 2026-12-01T10:00:00Z",  // fim == início
            "2026-12-01T10:00:00Z, 2026-12-01T09:59:59Z"   // fim 1 s antes do início
    })
    @DisplayName("RN-01: término deve ser posterior ao início")
    void rejeitaTerminoNaoPosterior(String ini, String fim) {
        assertThatThrownBy(() -> i(ini, fim))
                .isInstanceOf(ErroNegocio.class)
                .extracting("codigo").isEqualTo("INTERVALO_INVALIDO");
    }

    @Test
    void aceitaTerminoUmSegundoDepois() {
        assertThat(i("2026-12-01T10:00:00Z", "2026-12-01T10:00:01Z").fim())
                .isEqualTo(Instant.parse("2026-12-01T10:00:01Z"));
    }

    @Test
    void rejeitaIntervaloIncompleto() {
        assertThatThrownBy(() -> new Intervalo(null, Instant.now()))
                .isInstanceOf(ErroNegocio.class).extracting("codigo").isEqualTo("INTERVALO_INCOMPLETO");
        assertThatThrownBy(() -> new Intervalo(Instant.now(), null))
                .isInstanceOf(ErroNegocio.class).extracting("codigo").isEqualTo("INTERVALO_INCOMPLETO");
    }

    /** Existente: 10:00-12:00. */
    @ParameterizedTest(name = "[{index}] {0}-{1} sobrepõe? {2}")
    @CsvSource({
            "2026-12-01T08:00:00Z, 2026-12-01T10:00:00Z, false", // fim exato no início
            "2026-12-01T08:00:00Z, 2026-12-01T10:00:01Z, true",  // cruza 1 segundo
            "2026-12-01T12:00:00Z, 2026-12-01T13:00:00Z, false", // início exato no fim
            "2026-12-01T11:59:59Z, 2026-12-01T13:00:00Z, true",  // começa 1 s antes do fim
            "2026-12-01T10:30:00Z, 2026-12-01T11:00:00Z, true",  // contido
            "2026-12-01T09:00:00Z, 2026-12-01T13:00:00Z, true",  // contém
            "2026-12-01T10:00:00Z, 2026-12-01T12:00:00Z, true"   // idêntico
    })
    @DisplayName("RN-02: sobreposição com intervalo semiaberto [início, fim)")
    void detectaSobreposicao(String ini, String fim, boolean esperado) {
        Intervalo existente = i("2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z");
        Intervalo novo = i(ini, fim);
        assertThat(novo.sobrepoe(existente)).isEqualTo(esperado);
        assertThat(existente.sobrepoe(novo)).isEqualTo(esperado); // simetria
    }

    @ParameterizedTest(name = "[{index}] reserva {0}-{1} => {2} h dentro do dia 01/12")
    @CsvSource({
            "2026-12-01T10:00:00Z, 2026-12-01T12:00:00Z, 2.0",  // inteira dentro
            "2026-11-30T23:00:00Z, 2026-12-01T01:00:00Z, 1.0",  // cruza a borda inicial
            "2026-12-01T23:30:00Z, 2026-12-02T02:00:00Z, 0.5",  // cruza a borda final
            "2026-11-30T10:00:00Z, 2026-11-30T12:00:00Z, 0.0",  // fora (antes)
            "2026-12-02T00:00:00Z, 2026-12-02T01:00:00Z, 0.0"   // começa exatamente no fim
    })
    void recortaHorasNoPeriodo(String ini, String fim, double horas) {
        Intervalo dia = i("2026-12-01T00:00:00Z", "2026-12-02T00:00:00Z");
        assertThat(i(ini, fim).horasDentroDe(dia)).isEqualTo(horas);
    }
}
