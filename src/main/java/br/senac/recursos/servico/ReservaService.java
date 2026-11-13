package br.senac.recursos.servico;

import br.senac.recursos.dominio.AuditoriaReserva;
import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.MovimentacaoMaterial;
import br.senac.recursos.dominio.Perfil;
import br.senac.recursos.dominio.Recurso;
import br.senac.recursos.dominio.Reserva;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.dominio.TipoRecurso;
import br.senac.recursos.repositorio.ManutencaoRepository;
import br.senac.recursos.repositorio.MovimentacaoMaterialRepository;
import br.senac.recursos.repositorio.RecursoRepository;
import br.senac.recursos.repositorio.ReservaRepository;
import br.senac.recursos.seguranca.UsuarioAutenticado;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.sql.SQLException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Regras de reserva (RF4 a RF10).
 * A proteção contra dupla reserva tem DUAS camadas:
 *  1) verificação na aplicação (mensagem clara e registro de conflito evitado);
 *  2) constraint EXCLUDE no PostgreSQL, que vale mesmo quando duas requisições
 *     simultâneas passam juntas pela verificação 1 (ver ADR-002).
 */
@Service
public class ReservaService {

    private final ReservaRepository reservas;
    private final RecursoRepository recursos;
    private final ManutencaoRepository manutencoes;
    private final MovimentacaoMaterialRepository movimentacoes;
    private final AuditoriaService auditoria;
    private final ConflitoService conflitos;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final JdbcTemplate jdbc;

    public ReservaService(ReservaRepository reservas, RecursoRepository recursos,
                          ManutencaoRepository manutencoes, MovimentacaoMaterialRepository movimentacoes,
                          AuditoriaService auditoria, ConflitoService conflitos,
                          PlatformTransactionManager txManager, Clock clock, JdbcTemplate jdbc) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.movimentacoes = movimentacoes;
        this.auditoria = auditoria;
        this.conflitos = conflitos;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
        this.jdbc = jdbc;
    }

    // ---------------------------------------------------------------- criação e alteração

    public Reserva criar(UsuarioAutenticado usuario, Long recursoId, Long professorId,
                         Intervalo intervalo, String finalidade, Integer participantes) {
        Recurso recurso = recursoReservavel(recursoId);
        if (participantes != null && recurso.getCapacidade() != null && participantes > recurso.getCapacidade()) {
            throw ErroNegocio.invalido("CAPACIDADE_EXCEDIDA",
                    "O número de participantes excede a capacidade do recurso.");
        }
        validarProfessor(professorId);
        verificarDisponibilidade(recurso.getId(), professorId, intervalo, 0L);

        StatusReserva inicial = recurso.isRestrito() ? StatusReserva.SOLICITADA : StatusReserva.APROVADA;
        String motivo = recurso.isRestrito()
                ? "Reserva solicitada; recurso restrito aguarda aprovação de um Responsável"
                : "Reserva criada e aprovada automaticamente (recurso não restrito)";

        return comProtecaoDeConcorrencia(recurso.getId(), professorId, intervalo, 0L, () -> {
            Reserva nova = new Reserva(recurso.getId(), professorId, usuario.id(), intervalo,
                    finalidade, inicial, Instant.now(clock));
            nova.definirParticipantes(participantes);
            Reserva r = reservas.saveAndFlush(nova);
            auditoria.registrar(r.getId(), usuario.id(), null, inicial, motivo);
            return r;
        });
    }

    public Reserva alterar(UsuarioAutenticado usuario, Long reservaId, Intervalo novo) {
        Reserva atual = obterParaEscrita(usuario, reservaId);
        boolean restrito = recursos.findById(atual.getRecursoId()).map(Recurso::isRestrito).orElse(false);
        boolean alteravel = atual.getStatus() == StatusReserva.SOLICITADA
                || (atual.getStatus() == StatusReserva.APROVADA && !restrito);
        if (!alteravel) {
            // Recurso restrito já aprovado: o Responsável aprovou AQUELE horário.
            // Para outro horário, cancele e faça nova solicitação.
            throw ErroNegocio.conflito("ALTERACAO_NAO_PERMITIDA",
                    "Esta reserva não pode mais ser alterada. Cancele e faça uma nova solicitação.");
        }
        verificarDisponibilidade(atual.getRecursoId(), atual.getProfessorId(), novo, atual.getId());

        return comProtecaoDeConcorrencia(atual.getRecursoId(), atual.getProfessorId(), novo, reservaId, () -> {
            Reserva r = buscar(reservaId);
            r.reagendar(novo);
            auditoria.registrar(r.getId(), usuario.id(), r.getStatus(), r.getStatus(),
                    "Horário alterado pelo solicitante");
            return reservas.saveAndFlush(r);
        });
    }

    // ---------------------------------------------------------------- transições

    public Reserva cancelar(UsuarioAutenticado usuario, Long reservaId, String motivo) {
        return transitar(usuario, reservaId, StatusReserva.CANCELADA, textoOuPadrao(motivo, "Cancelada"), true);
    }

    public Reserva aprovar(UsuarioAutenticado responsavel, Long reservaId) {
        Reserva r = buscar(reservaId);
        if (r.pertenceA(responsavel.id())) {
            throw ErroNegocio.proibido("APROVACAO_PROPRIA", "O solicitante não pode aprovar a própria reserva.");
        }
        return transitar(responsavel, reservaId, StatusReserva.APROVADA, "Aprovada pelo Responsável", false);
    }

    public Reserva rejeitar(UsuarioAutenticado responsavel, Long reservaId, String motivo) {
        return transitar(responsavel, reservaId, StatusReserva.REJEITADA, textoOuPadrao(motivo, "Rejeitada"), false);
    }

    public Reserva iniciar(UsuarioAutenticado usuario, Long reservaId) {
        return transitar(usuario, reservaId, StatusReserva.EM_USO, "Uso iniciado", true);
    }

    public Reserva concluir(UsuarioAutenticado usuario, Long reservaId) {
        return transitar(usuario, reservaId, StatusReserva.CONCLUIDA, "Uso concluído", true);
    }

    public Reserva registrarNaoComparecimento(UsuarioAutenticado responsavel, Long reservaId) {
        return transitar(responsavel, reservaId, StatusReserva.NAO_COMPARECEU, "Solicitante não compareceu", false);
    }

    /** RF9: retirada de material = início do uso; devolução = conclusão. */
    public Reserva registrarRetirada(UsuarioAutenticado responsavel, Long reservaId, String observacao) {
        exigirMaterial(reservaId);
        return tx.execute(s -> {
            Reserva r = aplicarTransicao(responsavel, reservaId, StatusReserva.EM_USO, "Material retirado");
            movimentacoes.save(new MovimentacaoMaterial(reservaId, MovimentacaoMaterial.Tipo.RETIRADA,
                    responsavel.id(), Instant.now(clock), observacao));
            return r;
        });
    }

    public Reserva registrarDevolucao(UsuarioAutenticado responsavel, Long reservaId, String observacao) {
        exigirMaterial(reservaId);
        return tx.execute(s -> {
            Reserva r = aplicarTransicao(responsavel, reservaId, StatusReserva.CONCLUIDA, "Material devolvido");
            movimentacoes.save(new MovimentacaoMaterial(reservaId, MovimentacaoMaterial.Tipo.DEVOLUCAO,
                    responsavel.id(), Instant.now(clock), observacao));
            return r;
        });
    }

    @Transactional
    public void excluir(UsuarioAutenticado admin, Long reservaId) {
        Reserva r = buscar(reservaId);
        if (r.getStatus().jaIniciada()) {
            throw ErroNegocio.conflito("RESERVA_INICIADA",
                    "Reservas já iniciadas não podem ser excluídas.");
        }
        auditoria.registrar(r.getId(), admin.id(), r.getStatus(), r.getStatus(), "Reserva excluída pelo Administrador");
        reservas.delete(r);
    }

    // ---------------------------------------------------------------- consultas

    @Transactional(readOnly = true)
    public Reserva obter(UsuarioAutenticado usuario, Long reservaId) {
        Reserva r = buscar(reservaId);
        exigirPosseSeSolicitante(usuario, r);
        return r;
    }

    @Transactional(readOnly = true)
    public List<Reserva> listar(UsuarioAutenticado usuario) {
        if (usuario.eh(Perfil.SOLICITANTE)) {
            return reservas.findBySolicitanteIdOrderByInicioAsc(usuario.id());
        }
        return reservas.findAllByOrderByInicioAsc();
    }

    @Transactional(readOnly = true)
    public List<AuditoriaReserva> historico(Long reservaId) {
        if (!reservas.existsById(reservaId) && auditoria.historico(reservaId).isEmpty()) {
            throw ErroNegocio.naoEncontrado("Reserva não encontrada.");
        }
        return auditoria.historico(reservaId);
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoMaterial> movimentacoes(UsuarioAutenticado usuario, Long reservaId) {
        obter(usuario, reservaId);
        return movimentacoes.findByReservaIdOrderByIdAsc(reservaId);
    }

    // ---------------------------------------------------------------- apoio

    private Reserva transitar(UsuarioAutenticado usuario, Long reservaId, StatusReserva destino, String motivo,
                              boolean exigePosseOuResponsavel) {
        return tx.execute(s -> {
            if (exigePosseOuResponsavel) {
                Reserva r = buscar(reservaId);
                boolean dono = r.pertenceA(usuario.id());
                boolean podeGerir = usuario.eh(Perfil.RESPONSAVEL)
                        || (usuario.eh(Perfil.ADMINISTRADOR) && destino == StatusReserva.CANCELADA);
                if (!dono && !podeGerir) {
                    throw ErroNegocio.proibido("NAO_E_DONO", "Você só pode alterar as suas próprias reservas.");
                }
            }
            return aplicarTransicao(usuario, reservaId, destino, motivo);
        });
    }

    private Reserva aplicarTransicao(UsuarioAutenticado usuario, Long reservaId, StatusReserva destino,
                                     String motivo) {
        Reserva r = buscar(reservaId);
        StatusReserva anterior = r.transitarPara(destino);
        auditoria.registrar(r.getId(), usuario.id(), anterior, destino, motivo);
        return reservas.saveAndFlush(r);
    }

    /**
     * Executa a gravação numa transação que, ANTES de tudo, pega travas consultivas do PostgreSQL
     * (pg_advisory_xact_lock) do recurso e do professor, SEMPRE nessa ordem. Assim, pedidos
     * concorrentes para o mesmo recurso/professor ficam em fila, sem deadlock (DEF-002), e a
     * reverificação dentro da transação enxerga as reservas já confirmadas.
     * A constraint EXCLUDE continua como última barreira (23P01 -> 409).
     */
    private <T> T comProtecaoDeConcorrencia(Long recursoId, Long professorId, Intervalo intervalo,
                                            Long ignorarId, Supplier<T> operacao) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return tx.execute(s -> {
                    travar(TRAVA_RECURSO, recursoId);
                    if (professorId != null) {
                        travar(TRAVA_PROFESSOR, professorId);
                    }
                    if (haSobreposicao(recursoId, professorId, intervalo, ignorarId)) {
                        throw new ConflitoDetectado();
                    }
                    return operacao.get();
                });
            } catch (ConflitoDetectado e) {
                return falharComConflito(recursoId, professorId, intervalo);
            } catch (DataAccessException e) {
                if (falhaTransitoria(e) && tentativa < MAX_TENTATIVAS) {
                    esperar(tentativa);
                    continue;
                }
                if (!violouExclusao(e) && !falhaTransitoria(e)) {
                    throw e;
                }
                return falharComConflito(recursoId, professorId, intervalo);
            }
        }
    }

    private static final int TRAVA_RECURSO = 1;
    private static final int TRAVA_PROFESSOR = 2;

    private void travar(int tipo, Long id) {
        jdbc.queryForList("select pg_advisory_xact_lock(?, ?)::text", tipo, id.intValue());
    }

    private boolean haSobreposicao(Long recursoId, Long professorId, Intervalo intervalo, Long ignorarId) {
        return reservas.existeSobrepostaNoRecurso(recursoId, intervalo.inicio(), intervalo.fim(),
                StatusReserva.ocupantes(), ignorarId)
                || (professorId != null && reservas.existeSobrepostaNoProfessor(professorId, intervalo.inicio(),
                intervalo.fim(), StatusReserva.ocupantes(), ignorarId));
    }

    private <T> T falharComConflito(Long recursoId, Long professorId, Intervalo intervalo) {
        // Registro em transação própria, DEPOIS do rollback (não segura duas conexões ao mesmo tempo).
        conflitos.registrar(recursoId, professorId, intervalo);
        throw ErroNegocio.conflito("CONFLITO_HORARIO",
                "Já existe uma reserva para este recurso ou professor neste horário.");
    }

    /** Sinaliza, de dentro da transação, que a reverificação encontrou conflito. */
    private static final class ConflitoDetectado extends RuntimeException {
        ConflitoDetectado() {
            super(null, null, false, false);
        }
    }

    private static final int MAX_TENTATIVAS = 3;

    private static void esperar(int tentativa) {
        try {
            Thread.sleep(20L * tentativa);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** SQLState 40P01 = deadlock_detected; 40001 = serialization_failure (ambos transitórios). */
    static boolean falhaTransitoria(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql
                    && ("40P01".equals(sql.getSQLState()) || "40001".equals(sql.getSQLState()))) {
                return true;
            }
        }
        return false;
    }

    /** SQLState 23P01 = exclusion_violation do PostgreSQL (constraint EXCLUDE de sobreposição). */
    static boolean violouExclusao(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && "23P01".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private void verificarDisponibilidade(Long recursoId, Long professorId, Intervalo intervalo, Long ignorarId) {
        if (manutencoes.existeSobreposta(recursoId, intervalo.inicio(), intervalo.fim())) {
            throw ErroNegocio.conflito("RECURSO_EM_MANUTENCAO",
                    "O recurso está em manutenção neste período e não pode ser reservado.");
        }
        if (reservas.existeSobrepostaNoRecurso(recursoId, intervalo.inicio(), intervalo.fim(),
                StatusReserva.ocupantes(), ignorarId)) {
            conflitos.registrar(recursoId, professorId, intervalo);
            throw ErroNegocio.conflito("CONFLITO_HORARIO",
                    "Já existe uma reserva para este recurso neste horário.");
        }
        if (professorId != null && reservas.existeSobrepostaNoProfessor(professorId, intervalo.inicio(),
                intervalo.fim(), StatusReserva.ocupantes(), ignorarId)) {
            conflitos.registrar(recursoId, professorId, intervalo);
            throw ErroNegocio.conflito("CONFLITO_AGENDA_PROFESSOR",
                    "O professor já tem outra reserva neste horário.");
        }
    }

    private Recurso recursoReservavel(Long recursoId) {
        Recurso recurso = recursos.findById(recursoId).filter(Recurso::isAtivo)
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Recurso não encontrado."));
        if (recurso.getTipo() == TipoRecurso.PROFESSOR) {
            throw ErroNegocio.invalido("RECURSO_INVALIDO",
                    "Professor não é reservado sozinho: informe-o no campo professorId.");
        }
        return recurso;
    }

    private void validarProfessor(Long professorId) {
        if (professorId == null) {
            return;
        }
        recursos.findById(professorId)
                .filter(p -> p.isAtivo() && p.getTipo() == TipoRecurso.PROFESSOR)
                .orElseThrow(() -> ErroNegocio.invalido("PROFESSOR_INVALIDO", "Professor não encontrado."));
    }

    private void exigirMaterial(Long reservaId) {
        Reserva r = buscar(reservaId);
        boolean material = recursos.findById(r.getRecursoId())
                .map(x -> x.getTipo() == TipoRecurso.MATERIAL).orElse(false);
        if (!material) {
            throw ErroNegocio.invalido("NAO_E_MATERIAL", "Retirada e devolução valem apenas para materiais.");
        }
    }

    private Reserva obterParaEscrita(UsuarioAutenticado usuario, Long reservaId) {
        Reserva r = buscar(reservaId);
        if (!r.pertenceA(usuario.id())) {
            throw ErroNegocio.proibido("NAO_E_DONO", "Você só pode alterar as suas próprias reservas.");
        }
        return r;
    }

    private static void exigirPosseSeSolicitante(UsuarioAutenticado usuario, Reserva r) {
        if (usuario.eh(Perfil.SOLICITANTE) && !r.pertenceA(usuario.id())) {
            throw ErroNegocio.proibido("NAO_E_DONO", "Você só pode consultar as suas próprias reservas.");
        }
    }

    private Reserva buscar(Long id) {
        return reservas.findById(id).orElseThrow(() -> ErroNegocio.naoEncontrado("Reserva não encontrada."));
    }

    private static String textoOuPadrao(String texto, String padrao) {
        return (texto == null || texto.isBlank()) ? padrao : texto.trim();
    }
}
