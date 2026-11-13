package br.senac.recursos.servico;

import br.senac.recursos.dominio.ErroNegocio;
import br.senac.recursos.dominio.Intervalo;
import br.senac.recursos.dominio.Manutencao;
import br.senac.recursos.dominio.Recurso;
import br.senac.recursos.dominio.StatusReserva;
import br.senac.recursos.dominio.TipoRecurso;
import br.senac.recursos.repositorio.ManutencaoRepository;
import br.senac.recursos.repositorio.RecursoRepository;
import br.senac.recursos.repositorio.ReservaRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecursoService {

    private final RecursoRepository recursos;
    private final ManutencaoRepository manutencoes;
    private final ReservaRepository reservas;

    public RecursoService(RecursoRepository recursos, ManutencaoRepository manutencoes, ReservaRepository reservas) {
        this.recursos = recursos;
        this.manutencoes = manutencoes;
        this.reservas = reservas;
    }

    @Transactional
    public Recurso criar(String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
                         String competencia, boolean restrito) {
        return recursos.save(new Recurso(nome.trim(), tipo, capacidade, localizacao, competencia, restrito));
    }

    @Transactional
    public Recurso atualizar(Long id, String nome, TipoRecurso tipo, Integer capacidade, String localizacao,
                             String competencia, boolean restrito) {
        Recurso r = obter(id);
        r.atualizar(nome.trim(), tipo, capacidade, localizacao, competencia, restrito);
        return r;
    }

    @Transactional
    public void desativar(Long id) {
        Recurso r = obter(id);
        if (reservas.countByRecursoIdAndStatusIn(id, StatusReserva.ocupantes()) > 0) {
            throw ErroNegocio.conflito("RECURSO_COM_RESERVAS",
                    "O recurso possui reservas ativas e não pode ser desativado.");
        }
        r.desativar();
    }

    @Transactional(readOnly = true)
    public Recurso obter(Long id) {
        return recursos.findById(id).filter(Recurso::isAtivo)
                .orElseThrow(() -> ErroNegocio.naoEncontrado("Recurso não encontrado."));
    }

    @Transactional(readOnly = true)
    public List<Recurso> pesquisar(FiltroRecurso f) {
        Intervalo janela = null;
        if (f.disponivelInicio() != null || f.disponivelFim() != null) {
            janela = new Intervalo(f.disponivelInicio(), f.disponivelFim());
        }
        final Intervalo periodo = janela;
        return recursos.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(r -> f.tipo() == null || r.getTipo() == f.tipo())
                .filter(r -> f.capacidadeMinima() == null
                        || (r.getCapacidade() != null && r.getCapacidade() >= f.capacidadeMinima()))
                .filter(r -> contem(r.getLocalizacao(), f.localizacao()))
                .filter(r -> contem(r.getCompetencia(), f.competencia()))
                .filter(r -> periodo == null || estaDisponivel(r, periodo))
                .toList();
    }

    private boolean estaDisponivel(Recurso r, Intervalo periodo) {
        boolean emManutencao = manutencoes.existeSobreposta(r.getId(), periodo.inicio(), periodo.fim());
        boolean reservado = r.getTipo() == TipoRecurso.PROFESSOR
                ? reservas.existeSobrepostaNoProfessor(r.getId(), periodo.inicio(), periodo.fim(),
                        StatusReserva.ocupantes(), 0L)
                : reservas.existeSobrepostaNoRecurso(r.getId(), periodo.inicio(), periodo.fim(),
                        StatusReserva.ocupantes(), 0L);
        return !emManutencao && !reservado;
    }

    private static boolean contem(String valor, String busca) {
        if (busca == null || busca.isBlank()) {
            return true;
        }
        return valor != null && valor.toLowerCase(Locale.ROOT).contains(busca.toLowerCase(Locale.ROOT).trim());
    }

    @Transactional
    public Manutencao adicionarManutencao(Long recursoId, Intervalo intervalo, String motivo) {
        obter(recursoId);
        return manutencoes.save(new Manutencao(recursoId, intervalo, motivo.trim()));
    }

    @Transactional(readOnly = true)
    public List<Manutencao> listarManutencoes(Long recursoId) {
        obter(recursoId);
        return manutencoes.findByRecursoIdOrderByInicioAsc(recursoId);
    }
}
