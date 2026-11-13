package br.senac.recursos.servico;

import br.senac.recursos.dominio.ConflitoEvitado;
import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.repositorio.ConflitoEvitadoRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra tentativas recusadas por sobreposição (alimenta "conflitos evitados" do RF12).
 * Usa transação própria (REQUIRES_NEW) porque a transação da reserva recusada sofre rollback.
 */
@Service
public class ConflitoService {

    private final ConflitoEvitadoRepository repositorio;
    private final Clock clock;

    public ConflitoService(ConflitoEvitadoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long recursoId, Long professorId, Intervalo intervalo) {
        repositorio.save(new ConflitoEvitado(recursoId, professorId, intervalo, Instant.now(clock)));
    }
}
