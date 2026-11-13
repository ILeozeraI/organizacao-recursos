package br.senac.recursos.bdd;

import br.senac.recursos.integracao.PostgresTestcontainersConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Mesmo contexto Spring dos testes de integração (PostgreSQL real via Testcontainers). */
@CucumberContextConfiguration
@SpringBootTest(properties = {
        "app.notificacao.base-url=http://127.0.0.1:9",
        "spring.datasource.hikari.maximum-pool-size=30"})
@AutoConfigureMockMvc
@Import(PostgresTestcontainersConfig.class)
public class CucumberSpringConfig {
}
