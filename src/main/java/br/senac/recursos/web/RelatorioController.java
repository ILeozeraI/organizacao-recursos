package br.senac.recursos.web;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.servico.RelatorioService;
import br.senac.recursos.servico.RelatorioUtilizacao;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** RF12: GET /relatorios/utilizacao?inicio=...&fim=... (Responsável e Administrador). */
@RestController
@RequestMapping("/relatorios")
public class RelatorioController {

    private final RelatorioService servico;

    public RelatorioController(RelatorioService servico) {
        this.servico = servico;
    }

    @GetMapping("/utilizacao")
    public RelatorioUtilizacao utilizacao(@RequestParam Instant inicio, @RequestParam Instant fim) {
        return servico.utilizacao(new Intervalo(inicio, fim));
    }
}
