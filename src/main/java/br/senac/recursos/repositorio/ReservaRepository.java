package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findBySolicitanteIdOrderByInicioAsc(Long solicitanteId);

    List<Reserva> findAllByOrderByInicioAsc();

    @Query("""
            select count(r) > 0 from Reserva r
             where r.recursoId = :recursoId and r.status in :ocupantes
               and r.inicio < :fim and :inicio < r.fim
               and r.id <> :ignorarId
            """)
    boolean existeSobrepostaNoRecurso(Long recursoId, Instant inicio, Instant fim,
                                      Collection<StatusReserva> ocupantes, Long ignorarId);

    @Query("""
            select count(r) > 0 from Reserva r
             where r.professorId = :professorId and r.status in :ocupantes
               and r.inicio < :fim and :inicio < r.fim
               and r.id <> :ignorarId
            """)
    boolean existeSobrepostaNoProfessor(Long professorId, Instant inicio, Instant fim,
                                        Collection<StatusReserva> ocupantes, Long ignorarId);

    @Query("""
            select r from Reserva r
             where r.status in :status and r.inicio < :fim and :inicio < r.fim
            """)
    List<Reserva> findNoPeriodo(Instant inicio, Instant fim, Collection<StatusReserva> status);

    long countByRecursoIdAndStatusIn(Long recursoId, Collection<StatusReserva> status);
}
