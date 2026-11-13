package br.senac.recursos.servico;

import br.senac.recursos.dominio.Notificacao;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.integracao.IntegracaoException;
import br.senac.recursos.integracao.NotificacaoClient;
import br.senac.recursos.repositorio.NotificacaoRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RF11: notifica o solicitante via API externa. Falha na notificação NUNCA desfaz
 * a decisão sobre a reserva: o resultado (ENVIADA/FALHA) fica registrado.
 */
@Service
public class NotificacaoService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoService.class);

    private final NotificacaoClient client;
    private final NotificacaoRepository repositorio;
    private final Clock clock;

    public NotificacaoService(NotificacaoClient client, NotificacaoRepository repositorio, Clock clock) {
        this.client = client;
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public Notificacao notificar(Reserva reserva, String evento) {
        Notificacao registro;
        try {
            String idExterno = client.enviar(
                    new NotificacaoClient.PedidoNotificacao(reserva.getId(), evento, reserva.getSolicitanteId()));
            registro = new Notificacao(reserva.getId(), evento, Notificacao.Status.ENVIADA,
                    "id externo " + idExterno, Instant.now(clock));
        } catch (IntegracaoException e) {
            // Log com correlação (reserva/evento) e tipo do erro; nunca token ou corpo externo.
            LOG.warn("Falha ao notificar reserva={} evento={} tipo={}", reserva.getId(), evento, e.getTipo());
            registro = new Notificacao(reserva.getId(), evento, Notificacao.Status.FALHA,
                    e.getTipo().name(), Instant.now(clock));
        }
        return repositorio.save(registro);
    }
}
