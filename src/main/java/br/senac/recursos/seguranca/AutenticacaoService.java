package br.senac.recursos.seguranca;

import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.dominio.Sessao;
import br.senac.recursos.dominio.Usuario;
import br.senac.recursos.repositorio.SessaoRepository;
import br.senac.recursos.repositorio.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final ValidadorToken validador;
    private final Duration validade;

    public AutenticacaoService(UsuarioRepository usuarios, SessaoRepository sessoes, PasswordEncoder encoder,
                               Clock clock, @Value("${app.sessao.validade:PT8H}") Duration validade) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
        this.encoder = encoder;
        this.clock = clock;
        this.validador = new ValidadorToken(clock);
        this.validade = validade;
    }

    @Transactional
    public Sessao login(String email, String senha) {
        Usuario u = usuarios.findByEmailIgnoreCase(email.trim())
                .filter(x -> encoder.matches(senha, x.getSenhaHash()))
                // Mesma mensagem para e-mail inexistente e senha errada (não revela qual existe).
                .orElseThrow(() -> new ErroNegocio(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS",
                        "E-mail ou senha incorretos."));
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        Instant agora = Instant.now(clock);
        return sessoes.save(new Sessao(HexFormat.of().formatHex(bytes), u.getId(), agora, agora.plus(validade)));
    }

    @Transactional
    public void logout(String token) {
        sessoes.findById(token).ifPresent(Sessao::revogar);
    }

    @Transactional(readOnly = true)
    public DecisaoToken avaliar(String token) {
        Optional<Sessao> sessao = (token == null || token.isBlank()) ? Optional.empty() : sessoes.findById(token);
        return validador.avaliar(token, sessao);
    }

    @Transactional(readOnly = true)
    public Optional<UsuarioAutenticado> carregarUsuario(Long id) {
        return usuarios.findById(id).map(u -> new UsuarioAutenticado(u.getId(), u.getNome(), u.getPerfil()));
    }
}
