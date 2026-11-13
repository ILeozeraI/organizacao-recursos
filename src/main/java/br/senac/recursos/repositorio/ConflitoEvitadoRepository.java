package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.ConflitoEvitado;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConflitoEvitadoRepository extends JpaRepository<ConflitoEvitado, Long> {
    List<ConflitoEvitado> findByRegistradoEmGreaterThanEqualAndRegistradoEmLessThan(Instant inicio, Instant fim);
}
