package br.senac.recursos.integracao;

/** Porta de saída para o serviço externo de notificação (RF11). */
public interface NotificacaoClient {
    /** @return id do envio no provedor externo */
    String enviar(PedidoNotificacao pedido);

    record PedidoNotificacao(Long reservaId, String evento, Long destinatarioId) {
    }
}
