package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.ConflitoEvitado;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConflitoEvitadoRepository extends JpaRepository<ConflitoEvitado, Long> {

    /** Tentativas recusadas cujo horário pedido cai (mesmo que parcialmente) no período. */
    @Query("select count(c) from ConflitoEvitado c where c.inicio < :fim and :inicio < c.fim")
    long contarNoPeriodo(Instant inicio, Instant fim);
}
