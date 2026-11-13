package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.Manutencao;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ManutencaoRepository extends JpaRepository<Manutencao, Long> {

    List<Manutencao> findByRecursoIdOrderByInicioAsc(Long recursoId);

    /** Sobreposição semiaberta: [inicio, fim) x [m.inicio, m.fim). */
    @Query("""
            select count(m) > 0 from Manutencao m
             where m.recursoId = :recursoId and m.inicio < :fim and :inicio < m.fim
            """)
    boolean existeSobreposta(Long recursoId, Instant inicio, Instant fim);
}
