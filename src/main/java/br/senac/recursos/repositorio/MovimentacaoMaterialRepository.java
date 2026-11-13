package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.MovimentacaoMaterial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimentacaoMaterialRepository extends JpaRepository<MovimentacaoMaterial, Long> {
    List<MovimentacaoMaterial> findByReservaIdOrderByIdAsc(Long reservaId);
}
