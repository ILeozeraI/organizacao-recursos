package br.senac.recursos.repositorio;

import br.senac.recursos.dominio.Sessao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<Sessao, String> {
}
