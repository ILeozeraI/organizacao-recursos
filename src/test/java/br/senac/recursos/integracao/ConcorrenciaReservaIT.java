package br.senac.recursos.integracao;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

/**
 * RF6 (obrigatório): 20 solicitações SIMULTÂNEAS para o mesmo recurso e horário
 * resultam em exatamente 1 reserva aceita (201) e 19 recusadas (409), nunca 500.
 * A contagem final vem do BANCO, não só das respostas HTTP.
 * Repetido 5 vezes para reduzir a chance de passar por sorte.
 */
class ConcorrenciaReservaIT extends IntegracaoBase {

    private static final int SOLICITACOES = 20;

    @RepeatedTest(5)
    @DisplayName("mesma sala e mesmo horário: 1 aceita, 19 recusadas")
    void mesmaSalaMesmoHorario() throws Exception {
        String[] tokens = {login(ANA), login(BRUNO)};
        Map<Integer, Integer> respostas = disparar(i -> new Object[]{tokens[i % 2], SALA_101, null});

        assertThat(respostas).containsOnlyKeys(201, 409);
        assertThat(respostas.get(201)).isEqualTo(1);
        assertThat(respostas.get(409)).isEqualTo(SOLICITACOES - 1);
        assertThat(jdbc.queryForObject("select count(*) from reserva where recurso_id = ? and status in "
                + "('SOLICITADA','APROVADA','EM_USO')", Long.class, SALA_101)).isEqualTo(1L);
    }

    @RepeatedTest(5)
    @DisplayName("agenda do professor: 20 salas diferentes, mesmo professor e horário -> 1 aceita")
    void mesmoProfessorEmSalasDiferentes() throws Exception {
        List<Long> salas = new ArrayList<>();
        for (int i = 0; i < SOLICITACOES; i++) {
            salas.add(jdbc.queryForObject("insert into recurso (nome, tipo, capacidade, restrito) "
                    + "values (?, 'SALA', 30, false) returning id", Long.class, "Sala extra " + i));
        }
        String[] tokens = {login(ANA), login(BRUNO)};
        Map<Integer, Integer> respostas = disparar(i -> new Object[]{tokens[i % 2], salas.get(i), PROF_CARLA});

        assertThat(respostas).containsOnlyKeys(201, 409);
        assertThat(respostas.get(201)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from reserva where professor_id = ?", Long.class,
                PROF_CARLA)).isEqualTo(1L);
    }

    /** Dispara todas as requisições ao mesmo tempo (CountDownLatch) e conta os status HTTP. */
    private Map<Integer, Integer> disparar(IntFunction<Object[]> parametros) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(SOLICITACOES);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        try {
            for (int i = 0; i < SOLICITACOES; i++) {
                Object[] p = parametros.apply(i);
                futuros.add(pool.submit(() -> {
                    largada.await();
                    return reservar((String) p[0], (Long) p[1], (Long) p[2],
                            "2026-12-01T10:00:00Z", "2026-12-01T12:00:00Z")
                            .andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();
            Map<Integer, Integer> contagem = new java.util.TreeMap<>();
            for (Future<Integer> f : futuros) {
                contagem.merge(f.get(30, TimeUnit.SECONDS), 1, Integer::sum);
            }
            return contagem;
        } finally {
            pool.shutdownNow();
        }
    }
}
