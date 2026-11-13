package br.senac.recursos.servico;

import br.senac.recursos.dominio.TipoRecurso;
import java.time.Instant;

/** RF3: filtros de pesquisa (todos opcionais). */
public record FiltroRecurso(TipoRecurso tipo, Integer capacidadeMinima, String localizacao,
                            String competencia, Instant disponivelInicio, Instant disponivelFim) {
}
