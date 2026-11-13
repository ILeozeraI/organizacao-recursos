package br.senac.recursos.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Relógio injetável: produção usa o relógio do sistema; testes usam Clock.fixed. */
@Configuration
public class RelogioConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
