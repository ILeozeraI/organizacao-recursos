package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.Recurso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecursoRepository extends JpaRepository<Recurso, Long> {
    List<Recurso> findByAtivoTrueOrderByNomeAsc();
}
