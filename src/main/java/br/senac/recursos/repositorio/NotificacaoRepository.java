package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.Notificacao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByReservaIdOrderByIdAsc(Long reservaId);
}
