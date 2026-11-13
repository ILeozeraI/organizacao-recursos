package br.senac.recursos.web;

import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.seguranca.UsuarioAutenticado;
import br.senac.recursos.servico.NotificacaoService;
import br.senac.recursos.servico.ReservaService;
import br.senac.recursos.web.Dtos.AlterarReservaRequest;
import br.senac.recursos.web.Dtos.AuditoriaResponse;
import br.senac.recursos.web.Dtos.MotivoRequest;
import br.senac.recursos.web.Dtos.MovimentacaoResponse;
import br.senac.recursos.web.Dtos.ReservaRequest;
import br.senac.recursos.web.Dtos.ReservaResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF4 a RF10: reservas, aprovação, estados, materiais e histórico. */
@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService servico;
    private final NotificacaoService notificacoes;

    public ReservaController(ReservaService servico, NotificacaoService notificacoes) {
        this.servico = servico;
        this.notificacoes = notificacoes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaResponse criar(@AuthenticationPrincipal UsuarioAutenticado u, @Valid @RequestBody ReservaRequest req) {
        Reserva r = servico.criar(u, req.recursoId(), req.professorId(), new Intervalo(req.inicio(), req.fim()),
                req.finalidade(), req.participantes());
        return ReservaResponse.de(r);
    }

    @GetMapping
    public List<ReservaResponse> listar(@AuthenticationPrincipal UsuarioAutenticado u) {
        return servico.listar(u).stream().map(ReservaResponse::de).toList();
    }

    @GetMapping("/{id}")
    public ReservaResponse obter(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        return ReservaResponse.de(servico.obter(u, id));
    }

    @PutMapping("/{id}")
    public ReservaResponse alterar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                                   @Valid @RequestBody AlterarReservaRequest req) {
        return ReservaResponse.de(servico.alterar(u, id, new Intervalo(req.inicio(), req.fim())));
    }

    @PostMapping("/{id}/cancelar")
    public ReservaResponse cancelar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                                    @Valid @RequestBody(required = false) MotivoRequest req) {
        return ReservaResponse.de(servico.cancelar(u, id, req == null ? null : req.motivo()));
    }

    @PostMapping("/{id}/aprovar")
    public ReservaResponse aprovar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        Reserva r = servico.aprovar(u, id);
        notificacoes.notificar(r, "RESERVA_APROVADA");
        return ReservaResponse.de(r);
    }

    @PostMapping("/{id}/rejeitar")
    public ReservaResponse rejeitar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                                    @Valid @RequestBody(required = false) MotivoRequest req) {
        Reserva r = servico.rejeitar(u, id, req == null ? null : req.motivo());
        notificacoes.notificar(r, "RESERVA_REJEITADA");
        return ReservaResponse.de(r);
    }

    @PostMapping("/{id}/iniciar")
    public ReservaResponse iniciar(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        return ReservaResponse.de(servico.iniciar(u, id));
    }

    @PostMapping("/{id}/concluir")
    public ReservaResponse concluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        return ReservaResponse.de(servico.concluir(u, id));
    }

    @PostMapping("/{id}/nao-compareceu")
    public ReservaResponse naoCompareceu(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        return ReservaResponse.de(servico.registrarNaoComparecimento(u, id));
    }

    @PostMapping("/{id}/retirada")
    public ReservaResponse retirada(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                                    @Valid @RequestBody(required = false) MotivoRequest req) {
        return ReservaResponse.de(servico.registrarRetirada(u, id, req == null ? null : req.motivo()));
    }

    @PostMapping("/{id}/devolucao")
    public ReservaResponse devolucao(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id,
                                     @Valid @RequestBody(required = false) MotivoRequest req) {
        return ReservaResponse.de(servico.registrarDevolucao(u, id, req == null ? null : req.motivo()));
    }

    @GetMapping("/{id}/movimentacoes")
    public List<MovimentacaoResponse> movimentacoes(@AuthenticationPrincipal UsuarioAutenticado u,
                                                    @PathVariable Long id) {
        return servico.movimentacoes(u, id).stream().map(MovimentacaoResponse::de).toList();
    }

    @GetMapping("/{id}/historico")
    public List<AuditoriaResponse> historico(@PathVariable Long id) {
        return servico.historico(id).stream().map(AuditoriaResponse::de).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal UsuarioAutenticado u, @PathVariable Long id) {
        servico.excluir(u, id);
        return ResponseEntity.noContent().build();
    }
}
