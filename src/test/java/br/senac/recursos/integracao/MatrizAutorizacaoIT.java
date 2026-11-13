package br.senac.recursos.integracao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * RF1: percorre CADA célula da matriz perfil x operação lendo as MESMAS linhas do
 * documento docs/seguranca/matriz-autorizacao.md (fonte única: se alguém mudar a
 * matriz, o teste muda junto; nenhuma célula pode ser apagada para o teste passar).
 *
 * Fixture de cada célula (banco limpo antes de cada uma):
 *  {R} = reserva da Ana no Laboratório 3 (restrito), status SOLICITADA.
 */
class MatrizAutorizacaoIT extends IntegracaoBase {

    static Stream<Arguments> celulas() throws Exception {
        List<String> linhas = Files.readAllLines(Path.of("docs/seguranca/matriz-autorizacao.md"),
                StandardCharsets.UTF_8);
        String[] perfis = {"ANONIMO", "SOLICITANTE_DONO", "OUTRO_SOLICITANTE", "RESPONSAVEL", "ADMINISTRADOR"};
        return linhas.stream()
                .filter(l -> l.startsWith("| ") && l.contains("`"))
                .flatMap(l -> {
                    String[] c = l.split("\\|");
                    String metodo = c[1].trim();
                    String rota = c[2].trim().replace("`", "");
                    String corpo = c[3].trim().replace("`", "");
                    return Stream.of(0, 1, 2, 3, 4).map(i -> Arguments.of(metodo, rota,
                            corpo.equals("-") ? null : corpo, perfis[i], Integer.parseInt(c[4 + i].trim())));
                });
    }

    @ParameterizedTest(name = "{0} {1} como {3} -> {4}")
    @MethodSource("celulas")
    void celulaDaMatriz(String metodo, String rota, String corpo, String perfil, int esperado) throws Exception {
        long reserva = idCriado(reservar(login(ANA), LAB_3, null, "2026-12-01T10:00:00Z", "2026-12-01T11:00:00Z"));
        String token = switch (perfil) {
            case "ANONIMO" -> null;
            case "SOLICITANTE_DONO" -> login(ANA);
            case "OUTRO_SOLICITANTE" -> login(BRUNO);
            case "RESPONSAVEL" -> login(RITA);
            default -> login(ADMIN);
        };
        MockHttpServletRequestBuilder req = comToken(
                request(HttpMethod.valueOf(metodo), rota.replace("{R}", String.valueOf(reserva))), token);
        if (corpo != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(corpo);
        }
        MockHttpServletResponse resp = mvc.perform(req).andReturn().getResponse();

        assertThat(resp.getStatus()).as(resp.getContentAsString()).isEqualTo(esperado);
        // Nenhuma resposta (inclusive as de erro) expõe stack trace, SQL ou classe interna.
        assertThat(resp.getContentAsString()).doesNotContain("Exception", "at br.senac", "SELECT ", "org.hibernate");
    }
}
