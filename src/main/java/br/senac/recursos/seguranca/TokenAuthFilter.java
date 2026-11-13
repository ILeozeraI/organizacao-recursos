package br.senac.recursos.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lê "Authorization: Bearer <token>". Sem cabeçalho: segue como anônimo (as regras
 * de rota decidem). Com token inválido, expirado ou revogado: 401 com o motivo.
 */
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AutenticacaoService autenticacao;
    private final RespostaErroSeguranca respostas;

    public TokenAuthFilter(AutenticacaoService autenticacao, RespostaErroSeguranca respostas) {
        this.autenticacao = autenticacao;
        this.respostas = respostas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        DecisaoToken decisao = autenticacao.avaliar(header.substring(7).trim());
        if (!decisao.permitido()) {
            respostas.escrever(response, HttpStatus.UNAUTHORIZED, decisao.motivo().name(),
                    "Sessão inválida ou expirada. Faça login novamente.");
            return;
        }
        var usuario = autenticacao.carregarUsuario(decisao.usuarioId());
        if (usuario.isEmpty()) {
            respostas.escrever(response, HttpStatus.UNAUTHORIZED, MotivoNegacao.TOKEN_INVALIDO.name(),
                    "Sessão inválida ou expirada. Faça login novamente.");
            return;
        }
        var auth = new UsernamePasswordAuthenticationToken(usuario.get(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.get().perfil().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
    }
}
