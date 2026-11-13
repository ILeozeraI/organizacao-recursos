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

    public ReservaService(ReservaRepository reservas, RecursoRepository recursos,
                          ManutencaoRepository manutencoes, MovimentacaoMaterialRepository movimentacoes,
                          AuditoriaService auditoria, ConflitoService conflitos,
                          PlatformTransactionManager txManager, Clock clock) {
        this.reservas = reservas;
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.movimentacoes = movimentacoes;
        this.auditoria = auditoria;
        this.conflitos = conflitos;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    // ---------------------------------------------------------------- criação e alteração

    public Reserva criar(UsuarioAutenticado usuario, Long recursoId, Long professorId,
                         Intervalo intervalo, String finalidade) {
        Recurso recurso = recursoReservavel(recursoId);
        validarProfessor(professorId);
        verificarDisponibilidade(recurso.getId(), professorId, intervalo, 0L);

        StatusReserva inicial = recurso.isRestrito() ? StatusReserva.SOLICITADA : StatusReserva.APROVADA;
        String motivo = recurso.isRestrito()
                ? "Reserva solicitada; recurso restrito aguarda aprovação de um Responsável"
                : "Reserva criada e aprovada automaticamente (recurso não restrito)";

        return comProtecaoDeConcorrencia(recurso.getId(), professorId, intervalo, () -> {
            Reserva r = reservas.saveAndFlush(new Reserva(recurso.getId(), professorId, usuario.id(), intervalo,
                    finalidade, inicial, Instant.now(clock)));
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

        return comProtecaoDeConcorrencia(atual.getRecursoId(), atual.getProfessorId(), novo, () -> {
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

    private <T> T comProtecaoDeConcorrencia(Long recursoId, Long professorId, Intervalo intervalo,
                                            Supplier<T> operacao) {
        try {
            return tx.execute(s -> operacao.get());
        } catch (DataAccessException e) {
            if (!violouExclusao(e)) {
                throw e;
            }
            // Camada 2: a constraint do banco barrou uma sobreposição que passou pela camada 1
            // (duas requisições simultâneas). Resposta é 409, nunca 500.
            conflitos.registrar(recursoId, professorId, intervalo);
            throw ErroNegocio.conflito("CONFLITO_HORARIO",
                    "Já existe uma reserva para este recurso ou professor neste horário.");
        }
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
