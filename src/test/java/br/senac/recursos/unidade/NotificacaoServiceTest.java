package br.senac.recursos.unidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Notificacao;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.integracao.IntegracaoException;
import br.senac.recursos.integracao.NotificacaoClient;
import br.senac.recursos.repositorio.NotificacaoRepository;
import br.senac.recursos.servico.NotificacaoService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Teste de unidade ISOLADO (mock permitido aqui): falha de notificação vira registro FALHA. */
class NotificacaoServiceTest {

    private final NotificacaoClient client = mock(NotificacaoClient.class);
    private final NotificacaoRepository repo = mock(NotificacaoRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private final NotificacaoService servico = new NotificacaoService(client, repo, clock);
    private final Reserva reserva = new Reserva(1L, null, 7L, new Intervalo(Instant.parse("2026-12-01T10:00:00Z"),
            Instant.parse("2026-12-01T11:00:00Z")), null, StatusReserva.APROVADA, Instant.parse("2026-11-01T00:00:00Z"));

    @BeforeEach
    void salvarDevolveOProprioObjeto() {
        when(repo.save(any(Notificacao.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void registraEnviadaQuandoProvedorAceita() {
        when(client.enviar(any())).thenReturn("ntf-9");
        Notificacao n = servico.notificar(reserva, "RESERVA_APROVADA");
        assertThat(n.getStatus()).isEqualTo(Notificacao.Status.ENVIADA);
        assertThat(n.getDetalhe()).contains("ntf-9");
        assertThat(n.getInstante()).isEqualTo(Instant.parse("2026-11-20T10:00:00Z"));
    }

    @Test
    void registraFalhaSemPropagarErro() {
        when(client.enviar(any())).thenThrow(new IntegracaoException(IntegracaoException.Tipo.INDISPONIVEL, "fora"));
        Notificacao n = servico.notificar(reserva, "RESERVA_REJEITADA");
        assertThat(n.getStatus()).isEqualTo(Notificacao.Status.FALHA);
        assertThat(n.getDetalhe()).isEqualTo("INDISPONIVEL");
        assertThat(n.getEvento()).isEqualTo("RESERVA_REJEITADA");
    }
}
