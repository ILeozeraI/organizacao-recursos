package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.AuditoriaReserva;
import java.util.List;
import org.springframework.data.repository.Repository;

/**
 * Repository "somente inserção e leitura": não expõe delete nem update (RF10).
 */
public interface AuditoriaReservaRepository extends Repository<AuditoriaReserva, Long> {
    AuditoriaReserva save(AuditoriaReserva registro);

    List<AuditoriaReserva> findByReservaIdOrderByIdAsc(Long reservaId);

    long count();
}
