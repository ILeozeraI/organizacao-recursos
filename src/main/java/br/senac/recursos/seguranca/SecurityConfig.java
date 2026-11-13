package br.senac.recursos.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Matriz de autorização (ver docs/seguranca/matriz-autorizacao.md).
 * Regras de POSSE (Solicitante só mexe nas próprias reservas) ficam no ReservaService.
 */
@Configuration
public class SecurityConfig {

    private static final String SOLICITANTE = "SOLICITANTE";
    private static final String RESPONSAVEL = "RESPONSAVEL";
    private static final String ADMIN = "ADMINISTRADOR";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, AutenticacaoService autenticacao,
                                    RespostaErroSeguranca respostas) throws Exception {
        http
            .csrf(c -> c.disable()) // API stateless com token Bearer (sem cookie de sessão)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .addFilterBefore(new TokenAuthFilter(autenticacao, respostas), UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> respostas.escrever(res, HttpStatus.UNAUTHORIZED,
                        "TOKEN_AUSENTE", "Autenticação necessária."))
                .accessDeniedHandler((req, res, ex) -> respostas.escrever(res, HttpStatus.FORBIDDEN,
                        "ACESSO_NEGADO", "Seu perfil não tem permissão para esta operação.")))
            .authorizeHttpRequests(a -> a
                // Públicos: interface web, documentação e login
                .requestMatchers(HttpMethod.GET, "/", "/index.html", "/app.js", "/app.css", "/favicon.ico").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                .requestMatchers("/error").permitAll()
                // Recursos e manutenção
                .requestMatchers(HttpMethod.GET, "/recursos/**").authenticated()
                .requestMatchers("/recursos/**").hasRole(ADMIN)
                // Reservas
                .requestMatchers(HttpMethod.POST, "/reservas").hasRole(SOLICITANTE)
                .requestMatchers(HttpMethod.POST, "/reservas/*/aprovar", "/reservas/*/rejeitar",
                        "/reservas/*/nao-compareceu", "/reservas/*/retirada", "/reservas/*/devolucao")
                    .hasRole(RESPONSAVEL)
                .requestMatchers(HttpMethod.GET, "/reservas/*/historico").hasAnyRole(RESPONSAVEL, ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/reservas/*").hasRole(ADMIN)
                .requestMatchers(HttpMethod.PUT, "/reservas/*").hasRole(SOLICITANTE)
                // Relatórios
                .requestMatchers("/relatorios/**").hasAnyRole(RESPONSAVEL, ADMIN)
                // Todo o resto exige autenticação (nenhuma rota fica aberta por esquecimento)
                .anyRequest().authenticated());
        return http.build();
    }
}
