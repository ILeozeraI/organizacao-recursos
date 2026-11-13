package br.senac.recursos.web;

import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.seguranca.AutenticacaoService;
import br.senac.recursos.seguranca.UsuarioAutenticado;
import br.senac.recursos.web.Dtos.LoginRequest;
import br.senac.recursos.web.Dtos.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** RF1: autenticação por token. */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AutenticacaoService autenticacao;

    public AuthController(AutenticacaoService autenticacao) {
        this.autenticacao = autenticacao;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var sessao = autenticacao.login(req.email(), req.senha());
        var usuario = autenticacao.carregarUsuario(sessao.getUsuarioId())
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Usuário não encontrado."));
        return new LoginResponse(sessao.getToken(), sessao.getExpiraEm(), usuario.id(), usuario.nome(),
                usuario.perfil().name());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String header) {
        autenticacao.logout(header.substring(7).trim());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eu")
    public UsuarioAutenticado eu(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario;
    }
}
