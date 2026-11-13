package br.senac.recursos.web;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.TipoRecurso;
import br.senac.recursos.servico.FiltroRecurso;
import br.senac.recursos.servico.RecursoService;
import br.senac.recursos.web.Dtos.ManutencaoRequest;
import br.senac.recursos.web.Dtos.ManutencaoResponse;
import br.senac.recursos.web.Dtos.RecursoRequest;
import br.senac.recursos.web.Dtos.RecursoResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** RF2 (cadastro/consulta), RF3 (pesquisa) e RF8 (manutenção). */
@RestController
@RequestMapping("/recursos")
public class RecursoController {

    private final RecursoService servico;

    public RecursoController(RecursoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<RecursoResponse> pesquisar(@RequestParam(required = false) TipoRecurso tipo,
                                           @RequestParam(required = false) Integer capacidadeMinima,
                                           @RequestParam(required = false) String localizacao,
                                           @RequestParam(required = false) String competencia,
                                           @RequestParam(required = false) Instant disponivelInicio,
                                           @RequestParam(required = false) Instant disponivelFim) {
        return servico.pesquisar(new FiltroRecurso(tipo, capacidadeMinima, localizacao, competencia,
                disponivelInicio, disponivelFim)).stream().map(RecursoResponse::de).toList();
    }

    @GetMapping("/{id}")
    public RecursoResponse obter(@PathVariable Long id) {
        return RecursoResponse.de(servico.obter(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoResponse criar(@Valid @RequestBody RecursoRequest req) {
        return RecursoResponse.de(servico.criar(req.nome(), req.tipo(), req.capacidade(), req.localizacao(),
                req.competencia(), req.restrito()));
    }

    @PutMapping("/{id}")
    public RecursoResponse atualizar(@PathVariable Long id, @Valid @RequestBody RecursoRequest req) {
        return RecursoResponse.de(servico.atualizar(id, req.nome(), req.tipo(), req.capacidade(),
                req.localizacao(), req.competencia(), req.restrito()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        servico.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/manutencoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ManutencaoResponse adicionarManutencao(@PathVariable Long id, @Valid @RequestBody ManutencaoRequest req) {
        return ManutencaoResponse.de(servico.adicionarManutencao(id, new Intervalo(req.inicio(), req.fim()),
                req.motivo()));
    }

    @GetMapping("/{id}/manutencoes")
    public List<ManutencaoResponse> listarManutencoes(@PathVariable Long id) {
        return servico.listarManutencoes(id).stream().map(ManutencaoResponse::de).toList();
    }
}
