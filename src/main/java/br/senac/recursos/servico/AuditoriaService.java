package br.senac.recursos.servico;

import br.senac.recursos.dominio.AuditoriaReserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.repositorio.AuditoriaReservaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * RF10: todo registro de auditoria é gravado na MESMA transação da mudança de estado
 * (Propagation.MANDATORY falha se alguém chamar fora de uma transação).
 * Se a mudança sofrer rollback, a auditoria também sofre: não existe auditoria órfã.
 */
@Service
public class AuditoriaService {

    private final AuditoriaReservaRepository repositorio;
    private final Clock clock;

    public AuditoriaService(AuditoriaReservaRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Long reservaId, Long usuarioId, StatusReserva anterior, StatusReserva novo, String motivo) {
        repositorio.save(new AuditoriaReserva(reservaId, usuarioId, Instant.now(clock), anterior, novo, motivo));
    }

    @Transactional(readOnly = true)
    public List<AuditoriaReserva> historico(Long reservaId) {
        return repositorio.findByReservaIdOrderByIdAsc(reservaId);
    }
}
