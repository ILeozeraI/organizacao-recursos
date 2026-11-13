// Interface web simples (RF13): consome a mesma API documentada no Swagger.
const estado = { token: null, usuario: null, recursos: [] };
const $ = (s) => document.querySelector(s);

function aviso(texto, tipo = 'ok') {
  const el = $('#aviso');
  el.textContent = texto;
  el.className = 'aviso ' + tipo;
  el.hidden = false;
  clearTimeout(aviso.t);
  aviso.t = setTimeout(() => { el.hidden = true; }, 6000);
}

async function api(metodo, url, corpo) {
  const opcoes = { method: metodo, headers: {} };
  if (estado.token) opcoes.headers.Authorization = 'Bearer ' + estado.token;
  if (corpo !== undefined) {
    opcoes.headers['Content-Type'] = 'application/json';
    opcoes.body = JSON.stringify(corpo);
  }
  const resp = await fetch(url, opcoes);
  const texto = await resp.text();
  const dados = texto ? JSON.parse(texto) : null;
  if (!resp.ok) {
    // Mensagem compreensível vinda da API (ProblemDetail.detail)
    const extra = dados && dados.campos ? ' (' + dados.campos.join('; ') + ')' : '';
    throw new Error((dados && dados.detail ? dados.detail : 'Erro ' + resp.status) + extra);
  }
  return dados;
}

const paraIso = (local) => (local ? new Date(local).toISOString() : null);
const formatar = (iso) => new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' });
const nomeRecurso = (id) => (estado.recursos.find((r) => r.id === id) || { nome: '#' + id }).nome;

async function entrar(ev) {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    const r = await api('POST', '/auth/login', { email: f.get('email'), senha: f.get('senha') });
    estado.token = r.token;
    estado.usuario = r;
    $('#quem').textContent = r.nome + ' · ' + r.perfil;
    $('#sessao').hidden = false;
    $('#tela-login').hidden = true;
    $('#tela-app').hidden = false;
    $('#bloco-nova').hidden = r.perfil !== 'SOLICITANTE';
    $('#bloco-relatorio').hidden = r.perfil === 'SOLICITANTE';
    $('#titulo-reservas').textContent = r.perfil === 'SOLICITANTE' ? 'Minhas reservas' : 'Todas as reservas';
    await Promise.all([carregarRecursos(), carregarReservas()]);
  } catch (e) { aviso(e.message, 'erro'); }
}

async function sair() {
  try { await api('POST', '/auth/logout'); } catch (e) { /* sessão já inválida */ }
  estado.token = null;
  location.reload();
}

async function carregarRecursos(filtros = {}) {
  const qs = new URLSearchParams(Object.entries(filtros).filter(([, v]) => v)).toString();
  const lista = await api('GET', '/recursos' + (qs ? '?' + qs : ''));
  if (!qs) estado.recursos = lista;
  $('#lista-recursos').innerHTML = lista.map((r) => `
    <li><span><b>${r.nome}</b> <span class="etiqueta">${r.tipo}</span>
      ${r.restrito ? '<span class="etiqueta SOLICITADA">exige aprovação</span>' : ''}</span>
      <span class="sub">${r.capacidade ? r.capacidade + ' lugares · ' : ''}${r.localizacao || r.competencia || ''}</span></li>`).join('')
    || '<li>Nenhum recurso encontrado.</li>';
  if (!qs) {
    $('#form-reserva [name=recursoId]').innerHTML = estado.recursos.filter((r) => r.tipo !== 'PROFESSOR')
      .map((r) => `<option value="${r.id}">${r.nome}${r.restrito ? ' (exige aprovação)' : ''}</option>`).join('');
    $('#form-reserva [name=professorId]').innerHTML = '<option value="">Nenhum</option>' + estado.recursos
      .filter((r) => r.tipo === 'PROFESSOR').map((r) => `<option value="${r.id}">${r.nome}</option>`).join('');
  }
}

function acoes(r) {
  const p = estado.usuario.perfil;
  const b = (acao, rotulo) => `<button class="botao pequeno secundario" data-acao="${acao}" data-id="${r.id}">${rotulo}</button>`;
  const lista = [];
  if (p === 'RESPONSAVEL' && r.status === 'SOLICITADA') lista.push(b('aprovar', 'Aprovar'), b('rejeitar', 'Rejeitar'));
  if (['SOLICITADA', 'APROVADA'].includes(r.status) && p !== 'RESPONSAVEL') lista.push(b('cancelar', 'Cancelar'));
  if (r.status === 'APROVADA' && p !== 'ADMINISTRADOR') lista.push(b('iniciar', 'Iniciar uso'));
  if (r.status === 'EM_USO' && p !== 'ADMINISTRADOR') lista.push(b('concluir', 'Concluir'));
  return lista.join('');
}

async function carregarReservas() {
  const lista = await api('GET', '/reservas');
  $('#lista-reservas').innerHTML = lista.map((r) => `
    <tr><td>${r.id}</td><td>${nomeRecurso(r.recursoId)}${r.professorId ? ' + ' + nomeRecurso(r.professorId) : ''}</td>
      <td>${formatar(r.inicio)}</td><td>${formatar(r.fim)}</td>
      <td><span class="etiqueta ${r.status}">${r.status}</span></td><td>${acoes(r)}</td></tr>`).join('')
    || '<tr><td colspan="6">Nenhuma reserva ainda.</td></tr>';
}

async function reservar(ev) {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    const r = await api('POST', '/reservas', {
      recursoId: Number(f.get('recursoId')),
      professorId: f.get('professorId') ? Number(f.get('professorId')) : null,
      inicio: paraIso(f.get('inicio')), fim: paraIso(f.get('fim')), finalidade: f.get('finalidade'),
    });
    aviso(r.status === 'SOLICITADA' ? 'Reserva solicitada: aguarde a aprovação do Responsável.' : 'Reserva confirmada!');
    await carregarReservas();
  } catch (e) { aviso(e.message, 'erro'); }
}

async function executarAcao(ev) {
  const alvo = ev.target.closest('button[data-acao]');
  if (!alvo) return;
  try {
    await api('POST', `/reservas/${alvo.dataset.id}/${alvo.dataset.acao}`);
    aviso('Reserva atualizada.');
    await carregarReservas();
  } catch (e) { aviso(e.message, 'erro'); }
}

async function buscar(ev) {
  ev.preventDefault();
  const f = Object.fromEntries(new FormData(ev.target));
  f.disponivelInicio = paraIso(f.disponivelInicio);
  f.disponivelFim = paraIso(f.disponivelFim);
  try { await carregarRecursos(f); } catch (e) { aviso(e.message, 'erro'); }
}

async function relatorio(ev) {
  ev.preventDefault();
  const f = new FormData(ev.target);
  try {
    const r = await api('GET', `/relatorios/utilizacao?inicio=${f.get('inicio')}T00:00:00Z&fim=${f.get('fim')}T00:00:00Z`);
    $('#relatorio').innerHTML = `
      <p><b>Conflitos evitados:</b> ${r.conflitosEvitados}</p>
      <h3>Horas por recurso</h3><ul>${r.recursos.map((x) => `<li>${x.nome}: ${x.horasReservadas} h</li>`).join('') || '<li>Sem uso no período.</li>'}</ul>
      <h3>Carga horária por professor</h3><ul>${r.professores.map((x) => `<li>${x.nome}: ${x.horasAlocadas} h</li>`).join('') || '<li>Sem alocação no período.</li>'}</ul>`;
  } catch (e) { aviso(e.message, 'erro'); }
}

$('#form-login').addEventListener('submit', entrar);
$('#sair').addEventListener('click', sair);
$('#form-reserva').addEventListener('submit', reservar);
$('#form-busca').addEventListener('submit', buscar);
$('#form-relatorio').addEventListener('submit', relatorio);
$('#lista-reservas').addEventListener('click', executarAcao);
