// ConfeitaSys - telas: Vendas, Produtos, Estoque, Relatórios, Conta + alerta de estoque
'use strict';

// ====================================================================== utilidades

const $ = (id) => document.getElementById(id);
const conteudo = $('conteudo');

const fmtBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const brl = (v) => fmtBRL.format(v || 0);
const num = (v, casas = 3) => (v || 0).toLocaleString('pt-BR', { maximumFractionDigits: casas });

/** Escapa texto do usuário antes de colocar no HTML. */
function esc(s) {
  return String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/** Converte "1.234,56" ou "1234.56" em número. */
function lerNumero(s) {
  s = String(s ?? '').trim().replace(/\s/g, '').replace('R$', '');
  if (!s) return 0;
  if (s.includes(',')) s = s.replace(/\./g, '').replace(',', '.');
  const n = Number(s);
  return Number.isFinite(n) ? n : NaN;
}

const unGrande = (un) => (un === 'KG' ? 'kg' : 'L');
const unPequena = (un) => (un === 'KG' ? 'g' : 'ml');
const qtdEstoque = (q, un) => `${num(q)} ${unGrande(un)}`;
const qtdReceita = (q, un) => (q >= 1 ? qtdEstoque(q, un) : `${num(q * 1000, 1)} ${unPequena(un)}`);
const semAcento = (s) => String(s || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();

const hojeISO = () => {
  const d = new Date();
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
};
const dataBR = (iso) => (iso ? iso.slice(0, 10).split('-').reverse().join('/') : '');
const dataHoraBR = (iso) => `${dataBR(iso)} ${iso.slice(11, 16)}`;
/** Nº do pedido para a tela e a nota (#0012). O id da venda é um UUID; o número é o campo "numero". */
const numPedido = (numero) => '#' + String(numero).padStart(4, '0');

// Todas as chamadas à API ficam em js/api.js (objeto API, com o número de cada rota).
// Depois de qualquer alteração, confere os alertas de estoque:
API.aoAlterar = () => setTimeout(verificarAlertas, 50);

function aviso(msg, erro = false) {
  const el = document.createElement('div');
  el.className = 'aviso' + (erro ? ' erro' : '');
  el.textContent = msg;
  $('avisos').appendChild(el);
  setTimeout(() => el.remove(), erro ? 6000 : 3500);
}

const icone = {
  lapis: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"/></svg>',
  lixo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/></svg>',
  seta: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M9 6l6 6-6 6"/></svg>',
  x: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>',
  recibo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M6 2h12v20l-3-2-3 2-3-2-3 2z"/><path d="M9 7h6M9 11h6M9 15h4"/></svg>',
  armario: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="4" y="2.5" width="16" height="17" rx="1.5"/><path d="M4 7h16M12 7v12.5M10 4.75h4M9.5 12v2.5M14.5 12v2.5M6 19.5v2M18 19.5v2"/></svg>',
  bolo: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><path d="M4 21h16"/><path d="M5 21v-7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v7"/><path d="M5 16c1.5 1 3 1 4.5 0s3-1 4.5 0 3 1 5 0"/><path d="M12 12V8"/></svg>',
  sirene: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 18v-6a5 5 0 0 1 10 0v6"/><path d="M5 21h14v-3H5z"/><path d="M12 2v2M4.2 5.2l1.4 1.4M19.8 5.2l-1.4 1.4M2 12h2M20 12h2"/></svg>'
};

// ====================================================================== modal

function abrirModal(html, { largo = false } = {}) {
  fecharModal();
  const fundo = document.createElement('div');
  fundo.className = 'fundo-modal';
  fundo.innerHTML = `<div class="modal ${largo ? 'largo' : ''}" role="dialog" aria-modal="true">${html}</div>`;
  fundo.addEventListener('mousedown', (e) => { if (e.target === fundo) fecharModal(); });
  fundo.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', fecharModal));
  $('modais').appendChild(fundo);
  const primeiro = fundo.querySelector('[autofocus], input, select');
  if (primeiro) setTimeout(() => primeiro.focus(), 30);
  return fundo;
}
function fecharModal() { $('modais').innerHTML = ''; }
const modalAberto = () => $('modais').children.length > 0;

function confirmar(titulo, textoHtml, rotuloBotao = 'Excluir', extraHtml = '') {
  return new Promise((resolve) => {
    const m = abrirModal(`
      <div class="modal-cab"><h3>${titulo}</h3><button class="btn-icone" data-fechar aria-label="Fechar">${icone.x}</button></div>
      <div class="modal-corpo"><p style="margin-top:4px">${textoHtml}</p>${extraHtml}</div>
      <div class="modal-rodape">
        <button class="btn" data-fechar>Cancelar</button>
        <button class="btn btn-perigo" id="ok-confirmar">${rotuloBotao}</button>
      </div>`);
    m.querySelector('#ok-confirmar').addEventListener('click', () => {
      const marcado = m.querySelector('input[type=checkbox]');
      resolve({ ok: true, marcado: marcado ? marcado.checked : null });
      fecharModal();
    });
    m.querySelectorAll('[data-fechar]').forEach((b) => b.addEventListener('click', () => resolve({ ok: false })));
  });
}

const cabModal = (titulo) => `<div class="modal-cab"><h3>${titulo}</h3>
  <button type="button" class="btn-icone" data-fechar aria-label="Fechar">${icone.x}</button></div>`;

function mostrarErro(m, sel, msg) {
  const e = m.querySelector(sel);
  e.textContent = msg;
  e.classList.remove('oculto');
}

// ====================================================================== navegação

const TELAS = { vendas: 'Vendas', produtos: 'Produtos do cardápio', estoque: 'Estoque', relatorios: 'Relatórios', conta: 'Minha conta' };
const estado = { tela: 'vendas', data: hojeISO(), busca: '', filtroEstoque: 'todos', abaRelatorio: 'todos', dataRelatorio: hojeISO() };

function irPara(tela) {
  estado.tela = tela;
  document.querySelectorAll('.nav-btn[data-tela]').forEach((b) => {
    if (b.dataset.tela === tela) b.setAttribute('aria-current', 'page'); else b.removeAttribute('aria-current');
  });
  $('titulo').textContent = TELAS[tela];
  $('filtro-data').classList.toggle('oculto', tela !== 'vendas');
  $('busca').closest('.busca').classList.toggle('oculto', !['vendas', 'produtos', 'estoque'].includes(tela));
  $('busca').placeholder = { vendas: 'Buscar: produto, nº ou hora (14h)', produtos: 'Buscar produto', estoque: 'Buscar item do estoque' }[tela] || '';
  estado.busca = '';
  $('busca').value = '';
  $('limpar-busca').classList.add('oculto');
  desenhar();
}

function desenhar() {
  ({ vendas: telaVendas, produtos: telaProdutos, estoque: telaEstoque, relatorios: telaRelatorios, conta: telaConta })[estado.tela]();
}

// ====================================================================== editor de ingredientes
// Usado na receita do produto e nos ingredientes da venda. Quantidade digitada em g ou ml.

function editorIngredientes(m, ingredientes, itensIniciais, { disponivel = null, aoMudar = () => {} } = {}) {
  const porId = Object.fromEntries(ingredientes.map((i) => [i.id, i]));
  const linhasEl = m.querySelector('[data-linhas]');
  let itens = itensIniciais.length ? itensIniciais : [{ ingredienteId: '', qtd: '' }];
  const qtdDisp = (i) => (disponivel ? disponivel(i.id) : i.quantidade);

  const opcoes = (sel) => '<option value="">Escolha…</option>' + ingredientes.map((i) =>
    `<option value="${i.id}" ${String(i.id) === String(sel) ? 'selected' : ''}>${esc(i.nome)} (${qtdEstoque(qtdDisp(i), i.unidade)})</option>`).join('');

  function desenhar() {
    linhasEl.innerHTML = itens.map((it, n) => {
      const ing = porId[it.ingredienteId];
      return `<div class="item-receita" data-n="${n}">
        <select class="entrada" data-campo="ingredienteId" aria-label="Ingrediente">${opcoes(it.ingredienteId)}</select>
        <div class="com-sufixo"><input class="entrada num" data-campo="qtd" inputmode="decimal" placeholder="0" value="${esc(it.qtd)}" aria-label="Quantidade">
          <span class="sufixo">${ing ? unPequena(ing.unidade) : 'g/ml'}</span></div>
        <div class="custo-item num">—</div>
        <button type="button" class="btn-icone" data-remover title="Remover" aria-label="Remover ingrediente">${icone.lixo}</button>
      </div>`;
    }).join('');
    recalcular();
  }

  function custo() {
    return itens.reduce((s, it) => {
      const ing = porId[it.ingredienteId];
      const q = lerNumero(it.qtd) / 1000;
      return s + (ing && q > 0 ? q * ing.custoUnitario : 0);
    }, 0);
  }

  function recalcular() {
    linhasEl.querySelectorAll('.item-receita').forEach((linha) => {
      const it = itens[Number(linha.dataset.n)];
      const ing = porId[it.ingredienteId];
      const q = lerNumero(it.qtd) / 1000;
      linha.querySelector('.custo-item').textContent = ing && q > 0 ? brl(q * ing.custoUnitario) : '—';
    });
    aoMudar(custo());
  }

  linhasEl.addEventListener('input', (e) => {
    const linha = e.target.closest('.item-receita');
    if (!linha) return;
    const it = itens[Number(linha.dataset.n)];
    it[e.target.dataset.campo] = e.target.value;
    if (e.target.dataset.campo === 'ingredienteId') {
      const ing = porId[it.ingredienteId];
      linha.querySelector('.sufixo').textContent = ing ? unPequena(ing.unidade) : 'g/ml';
    }
    recalcular();
  });
  linhasEl.addEventListener('click', (e) => {
    const b = e.target.closest('[data-remover]');
    if (!b) return;
    itens.splice(Number(b.closest('.item-receita').dataset.n), 1);
    if (!itens.length) itens.push({ ingredienteId: '', qtd: '' });
    desenhar();
  });
  m.querySelector('[data-add-item]').onclick = () => {
    itens.push({ ingredienteId: '', qtd: '' });
    desenhar();
    linhasEl.querySelector('.item-receita:last-child select').focus();
  };

  desenhar();
  return {
    custo,
    definir(novos) { itens = novos.length ? novos : [{ ingredienteId: '', qtd: '' }]; desenhar(); },
    /** Valida e devolve [{ingredienteId, quantidade(kg/L)}] ou lança erro com mensagem. */
    ler() {
      const preenchidos = itens.filter((i) => i.ingredienteId || i.qtd);
      if (!preenchidos.length) throw new Error('Adicione pelo menos um ingrediente.');
      if (preenchidos.some((i) => !i.ingredienteId)) throw new Error('Escolha o ingrediente em todas as linhas (ou remova a linha vazia).');
      if (preenchidos.some((i) => !(lerNumero(i.qtd) > 0))) throw new Error('Informe a quantidade de cada ingrediente.');
      return preenchidos.map((i) => ({ ingredienteId: i.ingredienteId, quantidade: lerNumero(i.qtd) / 1000 }));
    }
  };
}

const htmlEditor = `
  <div class="itens-receita" style="margin-top:6px">
    <div class="cab-itens"><span>Ingrediente</span><span>Quantidade</span><span>Custo</span><span></span></div>
    <div data-linhas></div>
  </div>
  <button type="button" class="btn-texto" data-add-item>+ Adicionar ingrediente</button>`;

const paraLinhas = (lista) => lista.map((i) => ({ ingredienteId: i.ingredienteId, qtd: num(i.quantidade * 1000, 1) }));

// ====================================================================== VENDAS

async function telaVendas() {
  const { data, busca } = estado;
  let vendas;
  try { vendas = await API.vendas.pesquisar(data, busca); }
  catch (e) { return aviso(e.message, true); }
  if (estado.tela !== 'vendas') return;

  const faturamento = vendas.reduce((s, v) => s + v.valorVenda, 0);
  const custo = vendas.reduce((s, v) => s + v.custo, 0);
  const lucro = faturamento - custo;
  const rotuloData = data ? (data === hojeISO() ? 'hoje' : 'em ' + dataBR(data)) : 'em todas as datas';
  const mostrarData = !data || /^#?\d+$/.test(busca.trim());

  let html = `
    <section class="resumo" aria-label="Resumo">
      <div class="bloco"><span>Vendas ${busca ? 'encontradas' : rotuloData}</span><strong class="num">${vendas.length}</strong></div>
      <div class="bloco"><span>Faturamento</span><strong class="num">${brl(faturamento)}</strong></div>
      <div class="bloco"><span>Custos</span><strong class="num">${brl(custo)}</strong></div>
      <div class="bloco"><span>Lucro</span><strong class="num ${lucro >= 0 ? 'lucro-pos' : 'lucro-neg'}">${brl(lucro)}</strong></div>
    </section>
    <div class="titulo-lista"><h3>${busca ? `Resultado da busca por “${esc(busca)}”` : `Pedidos ${rotuloData}`}</h3></div>`;

  if (!vendas.length) {
    html += `<div class="vazio">${icone.recibo}
      <p>${busca ? 'Nenhuma venda encontrada para essa busca.' : 'Nenhuma venda registrada ' + rotuloData + '.'}</p>
      ${busca ? '' : '<button class="btn btn-primario" data-acao="nova">Registrar venda</button>'}</div>`;
  } else {
    html += vendas.map((v) => cartaoVenda(v, mostrarData)).join('');
  }
  html += atalhosHtml;
  conteudo.innerHTML = html;
}

const atalhosHtml = `<p class="atalhos">Atalhos: <kbd>N</kbd> nova venda · <kbd>/</kbd> buscar · <kbd>1</kbd> vendas · <kbd>2</kbd> produtos · <kbd>3</kbd> estoque · <kbd>4</kbd> relatórios · <kbd>5</kbd> conta · <kbd>Esc</kbd> fechar</p>`;

function tabelaIngredientes(itens, custoTotal) {
  return `<table class="tabela">
    <thead><tr><th>Ingrediente</th><th class="dir">Quantidade</th><th class="dir">Valor</th></tr></thead>
    <tbody>${itens.map((i) => `<tr><td>${esc(i.nome)}</td><td class="dir num">${qtdReceita(i.quantidade, i.unidade)}</td><td class="dir num">${brl(i.custo)}</td></tr>`).join('')}</tbody>
    <tfoot><tr><td>Custo total</td><td></td><td class="dir num">${brl(custoTotal)}</td></tr></tfoot></table>`;
}

function cartaoVenda(v, mostrarData) {
  return `
  <article class="venda" data-id="${v.id}" data-tipo="venda">
    <div class="venda-linha">
      <div><div class="venda-id">${numPedido(v.numero)}</div>
        <div class="venda-hora num">${mostrarData ? dataBR(v.data) + '<br>' : ''}${v.hora}</div></div>
      <div><div class="venda-produto">${esc(v.produto)}</div>
        <button class="expandir" data-acao="expandir" aria-expanded="false">${icone.seta} Ingredientes (${v.itens.length})</button></div>
      <div class="valor venda-total"><span>Venda</span><strong class="num">${brl(v.valorVenda)}</strong></div>
      <div class="valor"><span>Custo</span><strong class="num">${brl(v.custo)}</strong></div>
      <div class="valor"><span>Lucro</span><strong class="num ${v.lucro >= 0 ? 'lucro-pos' : 'lucro-neg'}">${brl(v.lucro)}</strong></div>
      <div class="menu-acoes"><button class="btn-icone" data-acao="menu" title="Editar ou excluir" aria-label="Editar ou excluir pedido ${v.numero}">${icone.lapis}</button></div>
    </div>
    <div class="venda-ingredientes oculto">${tabelaIngredientes(v.itens, v.custo)}</div>
  </article>`;
}

// cliques nas listas (delegação de eventos)
conteudo.addEventListener('click', async (e) => {
  const alvo = e.target.closest('[data-acao]');
  if (!alvo) return;
  const acao = alvo.dataset.acao;
  const cartao = alvo.closest('[data-id]');
  const id = cartao ? cartao.dataset.id : null;   // UUID (texto)
  const tipo = cartao?.dataset.tipo;

  try {
    if (acao === 'nova') return formularioVenda();
    if (acao === 'expandir') {
      const aberto = alvo.getAttribute('aria-expanded') === 'true';
      alvo.setAttribute('aria-expanded', String(!aberto));
      cartao.querySelector('.venda-ingredientes').classList.toggle('oculto', aberto);
      return;
    }
    if (acao === 'menu') {
      e.stopPropagation();
      const ja = alvo.parentElement.querySelector('.menu-pop');
      fecharMenus();
      if (ja) return;
      const pop = document.createElement('div');
      pop.className = 'menu-pop';
      pop.innerHTML = `<button data-acao="editar">${icone.lapis} Editar</button>
                       <button data-acao="excluir" class="excluir">${icone.lixo} Excluir</button>`;
      alvo.parentElement.appendChild(pop);
      pop.querySelector('button').focus();
      return;
    }
    if (acao === 'editar' || acao === 'excluir') {
      fecharMenus();
      if (tipo === 'venda') return acao === 'editar' ? formularioVenda(await API.vendas.consultar(id)) : excluirVenda(id);
      if (tipo === 'produto') return acao === 'editar' ? formularioProduto(await API.produtos.consultar(id)) : excluirProduto(id);
    }
    if (acao === 'novo-produto') return formularioProduto();
    if (acao === 'novo-item') return formularioIngrediente();
    if (acao === 'editar-item') return formularioIngrediente(estado.ingredientes.find((i) => i.id === id));
    if (acao === 'excluir-item') return excluirIngrediente(estado.ingredientes.find((i) => i.id === id));
    if (acao === 'filtro') { estado.filtroEstoque = alvo.dataset.valor; return telaEstoque(); }
    if (acao === 'ir') return irPara(alvo.dataset.tela);
  } catch (ex) { aviso(ex.message, true); }
});
function fecharMenus() { document.querySelectorAll('.menu-pop').forEach((m) => m.remove()); }
document.addEventListener('click', (e) => { if (!e.target.closest('.menu-acoes')) fecharMenus(); });

async function excluirVenda(id) {
  const v = await API.vendas.consultar(id);
  const r = await confirmar(`Excluir pedido ${numPedido(v.numero)}?`,
    `<strong>${esc(v.produto)}</strong> — ${brl(v.valorVenda)}, registrado em ${dataHoraBR(v.dataHora)}.<br>Esta ação não pode ser desfeita.`,
    'Excluir venda',
    `<label class="marcar"><input type="checkbox" checked> <span>Devolver os ingredientes desta venda ao estoque<br><span class="dica">Desmarque se os ingredientes já foram usados (ex.: devolução de um bolo já feito).</span></span></label>`);
  if (!r.ok) return;
  try {
    await API.vendas.excluir(id, r.marcado);
    aviso(`Pedido ${numPedido(v.numero)} excluído.`);
    desenhar();
  } catch (e) { aviso(e.message, true); }
}

/** Criar / editar venda: escolhe o produto do cardápio; a receita vem preenchida e pode ser ajustada. */
async function formularioVenda(venda = null) {
  let ingredientes, produtos;
  try { [ingredientes, produtos] = await Promise.all([API.estoque.listar(), API.produtos.listar()]); }
  catch (e) { return aviso(e.message, true); }

  if (!produtos.length) {
    const m = abrirModal(`${cabModal('Nova venda')}
      <div class="modal-corpo"><p>Antes de registrar vendas, cadastre ${ingredientes.length ? '' : 'os ingredientes no <strong>Estoque</strong> e '}os <strong>Produtos</strong> do cardápio com suas receitas.</p></div>
      <div class="modal-rodape"><button class="btn" data-fechar>Fechar</button><button class="btn btn-primario" id="ir">Cadastrar ${ingredientes.length ? 'produtos' : 'estoque'}</button></div>`);
    m.querySelector('#ir').onclick = () => { fecharModal(); irPara(ingredientes.length ? 'produtos' : 'estoque'); };
    return;
  }

  const usoAtual = {};
  if (venda) venda.itens.forEach((i) => { if (i.ingredienteId) usoAtual[i.ingredienteId] = (usoAtual[i.ingredienteId] || 0) + i.quantidade; });
  const porId = Object.fromEntries(ingredientes.map((i) => [i.id, i]));
  const disponivel = (id) => (porId[id] ? Math.max(0, porId[id].quantidade) + (usoAtual[id] || 0) : 0);
  const produtoAtual = venda?.produtoId;

  const m = abrirModal(`
    <form id="form-venda" novalidate>
      ${cabModal(venda ? 'Editar pedido ' + numPedido(venda.numero) : 'Nova venda')}
      <div class="modal-corpo">
        <div id="erro-venda" class="erro-form oculto" role="alert"></div>
        <div class="grade-2">
          <div class="campo"><label for="v-produto">Produto vendido</label>
            <select class="entrada" id="v-produto" autofocus>
              <option value="">Escolha o produto…</option>
              ${produtos.map((p) => `<option value="${p.id}" ${p.id === produtoAtual ? 'selected' : ''}>${esc(p.nomeCompleto)} — ${brl(p.precoVenda)}</option>`).join('')}
            </select>
            ${venda && !produtoAtual ? `<span class="dica">Produto original (“${esc(venda.produto)}”) foi excluído do cardápio; escolha outro.</span>` : ''}
          </div>
          <div class="campo"><label for="v-valor">Valor da venda</label>
            <div class="com-prefixo"><span class="prefixo">R$</span>
              <input class="entrada num" id="v-valor" inputmode="decimal" placeholder="0,00" value="${venda ? num(venda.valorVenda, 2) : ''}"></div></div>
        </div>
        <span class="rotulo">Ingredientes usados <span class="dica">(vêm da receita do produto; ajuste se precisar)</span></span>
        ${htmlEditor}
        <div class="conta">
          <div><span>Custo dos ingredientes</span><strong class="num" id="t-custo">R$ 0,00</strong></div>
          <div><span>Valor da venda</span><strong class="num" id="t-venda">R$ 0,00</strong></div>
          <div><span>Lucro</span><strong class="num" id="t-lucro">R$ 0,00</strong></div>
        </div>
      </div>
      <div class="modal-rodape">
        <button type="button" class="btn" data-fechar>Cancelar</button>
        <button type="submit" class="btn btn-primario" id="salvar-venda">Salvar venda</button>
      </div>
    </form>`, { largo: true });

  const atualizarConta = (custo) => {
    const valor = lerNumero(m.querySelector('#v-valor').value) || 0;
    m.querySelector('#t-custo').textContent = brl(custo);
    m.querySelector('#t-venda').textContent = brl(valor);
    const tl = m.querySelector('#t-lucro');
    tl.textContent = brl(valor - custo);
    tl.className = 'num ' + (valor - custo >= 0 ? 'lucro-pos' : 'lucro-neg');
  };
  const editor = editorIngredientes(m, ingredientes, venda ? paraLinhas(venda.itens.filter((i) => porId[i.ingredienteId])) : [], { disponivel, aoMudar: atualizarConta });
  m.querySelector('#v-valor').addEventListener('input', () => atualizarConta(editor.custo()));

  m.querySelector('#v-produto').addEventListener('change', (e) => {
    const p = produtos.find((x) => x.id === e.target.value);
    if (!p) return;
    m.querySelector('#v-valor').value = num(p.precoVenda, 2);
    editor.definir(paraLinhas(p.receita));
  });

  m.querySelector('#form-venda').addEventListener('submit', async (e) => {
    e.preventDefault();
    m.querySelector('#erro-venda').classList.add('oculto');
    const produtoId = m.querySelector('#v-produto').value || null;   // UUID do produto
    const valorVenda = lerNumero(m.querySelector('#v-valor').value);
    let itens;
    try {
      if (!produtoId) throw new Error('Escolha o produto vendido.');
      if (!(valorVenda > 0)) throw new Error('Informe o valor da venda.');
      itens = editor.ler();
    } catch (ex) { return mostrarErro(m, '#erro-venda', ex.message); }

    const botao = m.querySelector('#salvar-venda');
    botao.disabled = true;
    try {
      const corpo = { produtoId, valorVenda, itens };
      const salva = venda ? await API.vendas.editar(venda.id, corpo) : await API.vendas.registrar(corpo);
      fecharModal();
      aviso(venda ? `Pedido ${numPedido(salva.numero)} atualizado.` : `Venda ${numPedido(salva.numero)} registrada. Estoque atualizado.`);
      if (!venda && estado.data && estado.data !== salva.data) { estado.data = salva.data; $('filtro-data').value = salva.data; }
      if (estado.tela === 'vendas') desenhar();
    } catch (ex) { mostrarErro(m, '#erro-venda', ex.message); }
    finally { botao.disabled = false; }
  });
}

// ====================================================================== PRODUTOS (cadastro 3)

async function telaProdutos() {
  let produtos;
  try { produtos = await API.produtos.listar(); } catch (e) { return aviso(e.message, true); }
  if (estado.tela !== 'produtos') return;
  const termo = semAcento(estado.busca);
  const lista = produtos.filter((p) => !termo || semAcento(p.nomeCompleto).includes(termo));

  let html = `<div class="barra-acoes" style="display:flex">
      <p class="dica" style="margin:0">${produtos.length} produto(s) no cardápio. A receita de cada produto é usada para dar baixa no estoque a cada venda.</p>
      <button class="btn btn-primario" data-acao="novo-produto">+ Novo produto</button></div>`;
  if (!lista.length) {
    html += `<div class="vazio">${icone.bolo}<p>${produtos.length ? 'Nenhum produto encontrado.' : 'Nenhum produto cadastrado ainda. Cadastre, por exemplo, “Bolo de chocolate G – recheio Brigadeiro”.'}</p>
      ${produtos.length ? '' : '<button class="btn btn-primario" data-acao="novo-produto">Cadastrar produto</button>'}</div>`;
  } else {
    html += lista.map((p, n) => `
      <article class="venda" data-id="${p.id}" data-tipo="produto">
        <div class="venda-linha">
          <div class="venda-id">P${String(n + 1).padStart(2, '0')}</div>
          <div><div class="venda-produto produto-nome">${esc(p.nome)}
              <small>${[p.tamanho && 'Tamanho ' + esc(p.tamanho), p.recheio && 'Recheio ' + esc(p.recheio)].filter(Boolean).join(' · ') || '&nbsp;'}</small></div>
            <button class="expandir" data-acao="expandir" aria-expanded="false">${icone.seta} Receita (${p.receita.length} ingredientes)</button></div>
          <div class="valor venda-total"><span>Preço</span><strong class="num">${brl(p.precoVenda)}</strong></div>
          <div class="valor"><span>Custo atual</span><strong class="num">${brl(p.custoEstimado)}</strong></div>
          <div class="valor"><span>Lucro est.</span><strong class="num ${p.lucroEstimado >= 0 ? 'lucro-pos' : 'lucro-neg'}">${brl(p.lucroEstimado)}</strong></div>
          <div class="menu-acoes"><button class="btn-icone" data-acao="menu" title="Editar ou excluir" aria-label="Editar ou excluir produto">${icone.lapis}</button></div>
        </div>
        <div class="venda-ingredientes oculto">${tabelaIngredientes(p.receita, p.custoEstimado)}</div>
      </article>`).join('');
  }
  conteudo.innerHTML = html + atalhosHtml;
}

async function formularioProduto(produto = null) {
  let ingredientes;
  try { ingredientes = await API.estoque.listar(); } catch (e) { return aviso(e.message, true); }
  if (!ingredientes.length) {
    const m = abrirModal(`${cabModal('Novo produto')}
      <div class="modal-corpo"><p>Cadastre primeiro os ingredientes no <strong>Estoque</strong> (botão do armário). Depois monte a receita do produto.</p></div>
      <div class="modal-rodape"><button class="btn" data-fechar>Fechar</button><button class="btn btn-primario" id="ir">Ir para o estoque</button></div>`);
    m.querySelector('#ir').onclick = () => { fecharModal(); irPara('estoque'); };
    return;
  }
  const m = abrirModal(`
    <form id="form-produto" novalidate>
      ${cabModal(produto ? 'Editar produto' : 'Novo produto')}
      <div class="modal-corpo">
        <div id="erro-produto" class="erro-form oculto" role="alert"></div>
        <div class="campo"><label for="p-nome">Nome do produto</label>
          <input class="entrada" id="p-nome" autofocus placeholder="Ex.: Bolo de chocolate" value="${esc(produto?.nome)}"></div>
        <div class="grade-2">
          <div class="campo"><label for="p-tamanho">Tamanho</label>
            <input class="entrada" id="p-tamanho" list="lista-tamanhos" placeholder="Ex.: P, M, G" value="${esc(produto?.tamanho)}">
            <datalist id="lista-tamanhos"><option value="P"><option value="M"><option value="G"><option value="GG"><option value="Unidade"></datalist></div>
          <div class="campo"><label for="p-recheio">Recheio <span class="dica">(opcional)</span></label>
            <input class="entrada" id="p-recheio" placeholder="Ex.: Brigadeiro" value="${esc(produto?.recheio)}"></div>
        </div>
        <div class="campo"><label for="p-preco">Preço de venda</label>
          <div class="com-prefixo"><span class="prefixo">R$</span>
            <input class="entrada num" id="p-preco" inputmode="decimal" placeholder="0,00" value="${produto ? num(produto.precoVenda, 2) : ''}"></div></div>
        <span class="rotulo">Receita (ingredientes de 1 unidade)</span>
        ${htmlEditor}
        <div class="conta">
          <div><span>Custo da receita</span><strong class="num" id="t-custo">R$ 0,00</strong></div>
          <div><span>Preço de venda</span><strong class="num" id="t-venda">R$ 0,00</strong></div>
          <div><span>Lucro estimado</span><strong class="num" id="t-lucro">R$ 0,00</strong></div>
        </div>
      </div>
      <div class="modal-rodape"><button type="button" class="btn" data-fechar>Cancelar</button>
        <button type="submit" class="btn btn-primario">Salvar produto</button></div>
    </form>`, { largo: true });

  const atualizar = (custo) => {
    const preco = lerNumero(m.querySelector('#p-preco').value) || 0;
    m.querySelector('#t-custo').textContent = brl(custo);
    m.querySelector('#t-venda').textContent = brl(preco);
    const tl = m.querySelector('#t-lucro');
    tl.textContent = brl(preco - custo);
    tl.className = 'num ' + (preco - custo >= 0 ? 'lucro-pos' : 'lucro-neg');
  };
  const editor = editorIngredientes(m, ingredientes, produto ? paraLinhas(produto.receita) : [], { aoMudar: atualizar });
  m.querySelector('#p-preco').addEventListener('input', () => atualizar(editor.custo()));

  m.querySelector('#form-produto').addEventListener('submit', async (e) => {
    e.preventDefault();
    m.querySelector('#erro-produto').classList.add('oculto');
    let corpo;
    try {
      corpo = {
        nome: m.querySelector('#p-nome').value.trim(),
        tamanho: m.querySelector('#p-tamanho').value.trim(),
        recheio: m.querySelector('#p-recheio').value.trim(),
        precoVenda: lerNumero(m.querySelector('#p-preco').value)
      };
      if (!corpo.nome) throw new Error('Informe o nome do produto.');
      if (!(corpo.precoVenda > 0)) throw new Error('Informe o preço de venda.');
      corpo.receita = editor.ler();
    } catch (ex) { return mostrarErro(m, '#erro-produto', ex.message); }
    try {
      if (produto) await API.produtos.alterar(produto.id, corpo);
      else await API.produtos.incluir(corpo);
      fecharModal();
      aviso(produto ? 'Produto atualizado.' : 'Produto cadastrado no cardápio.');
      if (estado.tela === 'produtos') telaProdutos();
    } catch (ex) { mostrarErro(m, '#erro-produto', ex.message); }
  });
}

async function excluirProduto(id) {
  const p = await API.produtos.consultar(id);
  const r = await confirmar('Excluir produto?', `<strong>${esc(p.nomeCompleto)}</strong> será removido do cardápio. As vendas já registradas continuam guardadas.`, 'Excluir produto');
  if (!r.ok) return;
  try { await API.produtos.excluir(id); aviso('Produto excluído.'); telaProdutos(); }
  catch (e) { aviso(e.message, true); }
}

// ====================================================================== ESTOQUE (cadastro 2)

function seloValidade(i) {
  if (!i.validade) return '<span class="dica">—</span>';
  if (i.vencido) return `<span class="selo selo-vencido">Vencido ${dataBR(i.validade)}</span>`;
  if (i.venceLogo) return `<span class="selo selo-baixo">${i.diasParaVencer === 0 ? 'Vence hoje' : 'Vence em ' + i.diasParaVencer + 'd'}</span>`;
  return `<span class="num">${dataBR(i.validade)}</span>`;
}

async function telaEstoque() {
  try { estado.ingredientes = await API.estoque.listar(); } catch (e) { return aviso(e.message, true); }
  if (estado.tela !== 'estoque') return;
  const todos = estado.ingredientes;
  const termo = semAcento(estado.busca);
  let lista = todos.filter((i) => !termo || semAcento(i.nome).includes(termo));
  if (estado.filtroEstoque === 'falta') lista = lista.filter((i) => i.estoqueBaixo);
  if (estado.filtroEstoque === 'validade') lista = lista.filter((i) => i.vencido || i.venceLogo);
  const baixos = todos.filter((i) => i.estoqueBaixo).length;
  const validade = todos.filter((i) => i.vencido || i.venceLogo).length;
  const total = todos.reduce((s, i) => s + i.valorEmEstoque, 0);
  const selo = (i) => i.quantidade <= 0 ? '<span class="selo selo-falta">Acabou</span>'
    : i.estoqueBaixo ? '<span class="selo selo-falta">Estoque baixo</span>' : '<span class="selo selo-ok">OK</span>';

  let html = `
    <section class="resumo">
      <div class="bloco"><span>Itens cadastrados</span><strong class="num">${todos.length}</strong></div>
      <div class="bloco"><span>Valor em estoque</span><strong class="num">${brl(total)}</strong></div>
      <div class="bloco"><span>Estoque baixo</span><strong class="num ${baixos ? 'lucro-neg' : ''}">${baixos}</strong></div>
      <div class="bloco"><span>Vencidos / vencendo</span><strong class="num ${validade ? 'lucro-neg' : ''}">${validade}</strong></div>
    </section>
    <div class="barra-acoes" style="display:flex">
      <div class="filtros">
        <button class="chip" data-acao="filtro" data-valor="todos" aria-pressed="${estado.filtroEstoque === 'todos'}">Todos</button>
        <button class="chip" data-acao="filtro" data-valor="falta" aria-pressed="${estado.filtroEstoque === 'falta'}">Estoque baixo (${baixos})</button>
        <button class="chip" data-acao="filtro" data-valor="validade" aria-pressed="${estado.filtroEstoque === 'validade'}">Validade (${validade})</button>
      </div>
      <button class="btn btn-primario" data-acao="novo-item">+ Novo item no estoque</button>
    </div>`;

  if (!lista.length) {
    html += `<div class="vazio">${icone.armario}<p>${todos.length ? 'Nenhum item encontrado.' : 'O estoque está vazio. Cadastre farinha, açúcar, leite…'}</p>
      ${todos.length ? '' : '<button class="btn btn-primario" data-acao="novo-item">Cadastrar primeiro item</button>'}</div>`;
  } else {
    html += `<div class="painel"><table class="tabela">
      <thead><tr><th>Item</th><th class="dir">Em estoque</th><th class="dir">Custo</th><th class="dir">Valor</th><th class="dir">Alerta em</th><th>Validade</th><th>Situação</th><th class="dir">Ações</th></tr></thead>
      <tbody>${lista.map((i) => `
        <tr data-id="${i.id}">
          <td><strong>${esc(i.nome)}</strong></td>
          <td class="dir num">${qtdEstoque(i.quantidade, i.unidade)}</td>
          <td class="dir num">${brl(i.custoUnitario)}/${unGrande(i.unidade)}</td>
          <td class="dir num">${brl(i.valorEmEstoque)}</td>
          <td class="dir num">${qtdEstoque(i.quantidadeAlerta, i.unidade)}</td>
          <td>${seloValidade(i)}</td>
          <td>${selo(i)}</td>
          <td class="dir" style="white-space:nowrap">
            <button class="btn-icone" data-acao="editar-item" title="Editar" aria-label="Editar ${esc(i.nome)}">${icone.lapis}</button>
            <button class="btn-icone" data-acao="excluir-item" title="Excluir" aria-label="Excluir ${esc(i.nome)}">${icone.lixo}</button>
          </td>
        </tr>`).join('')}</tbody></table></div>`;
  }
  conteudo.innerHTML = html + atalhosHtml;
}

function formularioIngrediente(item = null) {
  let unidade = item?.unidade || 'KG';
  const m = abrirModal(`
    <form id="form-item" novalidate>
      ${cabModal(item ? 'Editar item do estoque' : 'Novo item no estoque')}
      <div class="modal-corpo">
        <div id="erro-item" class="erro-form oculto" role="alert"></div>
        <div class="campo"><label for="i-nome">Nome do produto</label>
          <input class="entrada" id="i-nome" autofocus placeholder="Ex.: Farinha de trigo" value="${esc(item?.nome)}"></div>
        <div class="campo"><span class="rotulo" id="rot-unidade">Medido em</span>
          <div class="alternador" role="group" aria-labelledby="rot-unidade">
            <button type="button" data-un="KG">Quilogramas (kg)</button>
            <button type="button" data-un="L">Litros (L)</button>
          </div></div>
        <div class="grade-2">
          <div class="campo"><label for="i-qtd">Quantidade</label>
            <div class="com-sufixo"><input class="entrada num" id="i-qtd" inputmode="decimal" placeholder="0" value="${item ? num(item.quantidade) : ''}"><span class="sufixo" data-suf>kg</span></div></div>
          <div class="campo"><label for="i-valor">Valor pago</label>
            <div class="com-prefixo"><span class="prefixo">R$</span><input class="entrada num" id="i-valor" inputmode="decimal" placeholder="0,00" value="${item ? num(item.quantidade * item.custoUnitario, 2) : ''}"></div></div>
        </div>
        <div class="grade-2">
          <div class="campo"><label for="i-alerta">Alertar quando restar</label>
            <div class="com-sufixo"><input class="entrada num" id="i-alerta" inputmode="decimal" placeholder="0" value="${item ? num(item.quantidadeAlerta) : ''}"><span class="sufixo" data-suf>kg</span></div>
            <span class="dica">Ao chegar nessa quantidade toca o alerta de estoque baixo.</span></div>
          <div class="campo"><label for="i-validade">Data de validade</label>
            <input class="entrada" id="i-validade" type="date" value="${esc(item?.validade || '')}"></div>
        </div>
        <p class="dica" id="i-custo">&nbsp;</p>
        ${item ? '<p class="dica">Na edição, informe a quantidade que existe agora e quanto ela custou (ao repor, some a compra nova).</p>' : ''}
      </div>
      <div class="modal-rodape"><button type="button" class="btn" data-fechar>Cancelar</button>
        <button type="submit" class="btn btn-primario">Salvar</button></div>
    </form>`);

  const atualizar = () => {
    m.querySelectorAll('[data-un]').forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.un === unidade)));
    m.querySelectorAll('[data-suf]').forEach((s) => (s.textContent = unGrande(unidade)));
    const q = lerNumero(m.querySelector('#i-qtd').value), v = lerNumero(m.querySelector('#i-valor').value);
    m.querySelector('#i-custo').textContent = q > 0 && v >= 0 ? `Custo calculado: ${brl(v / q)} por ${unGrande(unidade)}` : ' ';
  };
  m.querySelectorAll('[data-un]').forEach((b) => b.addEventListener('click', () => { unidade = b.dataset.un; atualizar(); }));
  m.querySelector('#i-qtd').addEventListener('input', atualizar);
  m.querySelector('#i-valor').addEventListener('input', atualizar);
  atualizar();

  m.querySelector('#form-item').addEventListener('submit', async (e) => {
    e.preventDefault();
    const corpo = {
      nome: m.querySelector('#i-nome').value.trim(),
      unidade,
      quantidade: lerNumero(m.querySelector('#i-qtd').value),
      valorPago: lerNumero(m.querySelector('#i-valor').value),
      quantidadeAlerta: lerNumero(m.querySelector('#i-alerta').value),
      validade: m.querySelector('#i-validade').value
    };
    let msg = '';
    if (!corpo.nome) msg = 'Informe o nome do produto.';
    else if (Number.isNaN(corpo.quantidade) || corpo.quantidade < 0) msg = 'Quantidade inválida.';
    else if (Number.isNaN(corpo.valorPago) || corpo.valorPago < 0) msg = 'Valor pago inválido.';
    else if (Number.isNaN(corpo.quantidadeAlerta) || corpo.quantidadeAlerta < 0) msg = 'Quantidade de alerta inválida.';
    if (msg) return mostrarErro(m, '#erro-item', msg);
    try {
      if (item) await API.estoque.alterar(item.id, corpo);
      else await API.estoque.incluir(corpo);
      fecharModal();
      aviso(item ? 'Item atualizado.' : 'Item cadastrado no estoque.');
      if (estado.tela === 'estoque') telaEstoque();
    } catch (ex) { mostrarErro(m, '#erro-item', ex.message); }
  });
}

async function excluirIngrediente(item) {
  const r = await confirmar('Excluir item do estoque?',
    `<strong>${esc(item.nome)}</strong> será removido do estoque. As vendas antigas continuam com o registro do ingrediente.`, 'Excluir item');
  if (!r.ok) return;
  try { await API.estoque.excluir(item.id); aviso('Item excluído.'); telaEstoque(); }
  catch (e) { aviso(e.message, true); }
}

// ====================================================================== CONTA (cadastro 1)

async function telaConta() {
  let u;
  try { u = await API.usuario.consultar(); } catch (e) { return aviso(e.message, true); }
  if (estado.tela !== 'conta') return;
  conteudo.innerHTML = `
    <div class="secao-conta">
      <section class="painel">
        <h3>Dados do usuário</h3>
        <p class="dica" style="margin-top:-6px">Usuário cadastrado em ${dataHoraBR(u.criadoEm)}. A senha fica guardada criptografada (PBKDF2-SHA256).</p>
        <form id="form-conta" novalidate>
          <div id="erro-conta" class="erro-form oculto" role="alert"></div>
          <div class="campo"><label for="c-nome">Nome de usuário</label><input class="entrada" id="c-nome" value="${esc(u.nome)}" autocomplete="username"></div>
          <div class="grade-2">
            <div class="campo"><label for="c-nova">Nova senha <span class="dica">(deixe vazio para manter)</span></label><input class="entrada" id="c-nova" type="password" autocomplete="new-password"></div>
            <div class="campo"><label for="c-conf">Confirmar nova senha</label><input class="entrada" id="c-conf" type="password" autocomplete="new-password"></div>
          </div>
          <div class="campo"><label for="c-atual">Senha atual (obrigatória para salvar)</label><input class="entrada" id="c-atual" type="password" autocomplete="current-password"></div>
          <button class="btn btn-primario" type="submit">Salvar alterações</button>
        </form>
      </section>
      <section class="painel zona-perigo">
        <h3>Excluir usuário</h3>
        <p class="dica" style="margin-top:-6px">Remove o usuário do sistema. Vendas, produtos e estoque são mantidos. No próximo acesso, será pedido um novo cadastro.</p>
        <form id="form-excluir-conta" novalidate>
          <div id="erro-excluir" class="erro-form oculto" role="alert"></div>
          <div class="campo"><label for="x-senha">Digite sua senha para confirmar</label><input class="entrada" id="x-senha" type="password" autocomplete="current-password"></div>
          <button class="btn btn-perigo" type="submit">Excluir usuário</button>
        </form>
      </section>
    </div>`;

  $('form-conta').addEventListener('submit', async (e) => {
    e.preventDefault();
    const box = conteudo;
    box.querySelector('#erro-conta').classList.add('oculto');
    const nova = $('c-nova').value;
    if (nova && nova !== $('c-conf').value) return mostrarErro(box, '#erro-conta', 'A confirmação da nova senha não confere.');
    if (!$('c-atual').value) return mostrarErro(box, '#erro-conta', 'Informe a senha atual.');
    try {
      await API.usuario.alterar({ nome: $('c-nome').value, senhaAtual: $('c-atual').value, novaSenha: nova });
      aviso('Dados do usuário atualizados.');
      telaConta();
    } catch (ex) { mostrarErro(box, '#erro-conta', ex.message); }
  });
  $('form-excluir-conta').addEventListener('submit', async (e) => {
    e.preventDefault();
    const senha = $('x-senha').value;
    if (!senha) return mostrarErro(conteudo, '#erro-excluir', 'Digite sua senha.');
    const r = await confirmar('Excluir usuário?', 'Você sairá do sistema e precisará criar um novo usuário para entrar.', 'Excluir usuário');
    if (!r.ok) return;
    try {
      await API.usuario.excluir(senha);
      Sessao.limpar();
      location.href = 'index.html';
    } catch (ex) { mostrarErro(conteudo, '#erro-excluir', ex.message); }
  });
}

// ====================================================================== RELATÓRIOS (opcionais)

async function telaRelatorios() {
  const aba = estado.abaRelatorio;
  const chip = (v, t) => `<button class="chip" data-aba="${v}" aria-pressed="${aba === v}">${t}</button>`;
  conteudo.innerHTML = `
    <p class="dica" style="margin-top:0">Os relatórios são opcionais: gere quando quiser.</p>
    <div class="barra-acoes">
      <div class="abas">${chip('todos', 'Todos')}${chip('vendas', 'Vendas')}${chip('estoque', 'Estoque')}${chip('faltando', 'Itens em falta')}</div>
      <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
        <input type="date" class="entrada data-filtro" id="rel-data" value="${estado.dataRelatorio}" aria-label="Data do relatório">
        <button class="btn" id="rel-imprimir">Imprimir</button>
        <button class="btn btn-primario" id="rel-fechar" title="Salva os 3 relatórios em arquivo e faz backup">Salvar e fazer backup</button>
      </div>
    </div>
    <div id="area-relatorio"><p class="dica">Gerando…</p></div>`;

  conteudo.querySelectorAll('[data-aba]').forEach((b) => b.addEventListener('click', () => { estado.abaRelatorio = b.dataset.aba; telaRelatorios(); }));
  $('rel-data').addEventListener('change', (e) => { estado.dataRelatorio = e.target.value || hojeISO(); telaRelatorios(); });
  $('rel-imprimir').addEventListener('click', () => window.print());
  $('rel-fechar').addEventListener('click', salvarRelatorios);
  try { $('area-relatorio').innerHTML = await montarRelatorios(aba, estado.dataRelatorio); }
  catch (e) { aviso(e.message, true); }
}

async function montarRelatorios(aba, data) {
  const partes = [];
  if (aba === 'todos' || aba === 'vendas') partes.push(relVendas(await API.relatorios.vendas(data)));
  if (aba === 'todos' || aba === 'estoque') partes.push(relEstoque(await API.relatorios.estoque()));
  if (aba === 'todos' || aba === 'faltando') partes.push(relFaltando(await API.relatorios.faltando()));
  return partes.join('');
}

const cabRel = (titulo, sub, geradoEm) => `
  <div class="cab"><div><h1>${titulo}</h1><small>${sub}</small></div>
  <small>ConfeitaSys · gerado em ${dataHoraBR(geradoEm)}</small></div>`;

function relVendas(r) {
  const linhas = r.vendas.map((v) => `<tr><td class="num">${numPedido(v.numero)}</td><td class="num">${v.hora}</td><td>${esc(v.produto)}</td>
    <td class="dir num">${brl(v.valorVenda)}</td><td class="dir num">${brl(v.custo)}</td><td class="dir num">${brl(v.lucro)}</td></tr>`).join('');
  const usados = r.ingredientesUsados.map((i) => `<tr><td>${esc(i.nome)}</td><td class="dir num">${qtdEstoque(i.quantidade, i.unidade)}</td><td class="dir num">${brl(i.custo)}</td></tr>`).join('');
  return `<section class="folha">
    ${cabRel('Relatório de Vendas', 'Dia ' + dataBR(r.data), r.geradoEm)}
    <div class="totais">
      <div><span>Vendas</span><strong class="num">${r.quantidadeVendas}</strong></div>
      <div><span>Faturamento</span><strong class="num">${brl(r.faturamento)}</strong></div>
      <div><span>Custos</span><strong class="num">${brl(r.custo)}</strong></div>
      <div><span>Lucro</span><strong class="num">${brl(r.lucro)}</strong></div>
    </div>
    <h2>Pedidos do dia</h2>
    ${r.vendas.length ? `<table class="tabela"><thead><tr><th>Pedido</th><th>Hora</th><th>Produto</th><th class="dir">Venda</th><th class="dir">Custo</th><th class="dir">Lucro</th></tr></thead>
      <tbody>${linhas}</tbody><tfoot><tr><td colspan="3">Total</td><td class="dir num">${brl(r.faturamento)}</td><td class="dir num">${brl(r.custo)}</td><td class="dir num">${brl(r.lucro)}</td></tr></tfoot></table>`
      : '<p class="dica">Nenhuma venda neste dia.</p>'}
    <h2>Ingredientes usados no dia</h2>
    ${usados ? `<table class="tabela"><thead><tr><th>Ingrediente</th><th class="dir">Quantidade</th><th class="dir">Custo</th></tr></thead><tbody>${usados}</tbody></table>` : '<p class="dica">—</p>'}
  </section>`;
}

function relEstoque(r) {
  const linhas = r.itens.map((i) => `<tr><td>${esc(i.nome)}</td><td class="dir num">${qtdEstoque(i.quantidade, i.unidade)}</td>
    <td class="dir num">${brl(i.custoUnitario)}/${unGrande(i.unidade)}</td><td class="dir num">${brl(i.valorEmEstoque)}</td>
    <td class="dir num">${qtdEstoque(i.quantidadeAlerta, i.unidade)}</td><td>${i.validade ? dataBR(i.validade) : '—'}</td>
    <td>${i.vencido ? 'Vencido' : i.estoqueBaixo ? 'Estoque baixo' : 'OK'}</td></tr>`).join('');
  return `<section class="folha">
    ${cabRel('Relatório do Estoque', 'Posição atual do estoque', r.geradoEm)}
    ${r.itens.length ? `<table class="tabela"><thead><tr><th>Item</th><th class="dir">Quantidade</th><th class="dir">Custo</th><th class="dir">Valor</th><th class="dir">Alerta</th><th>Validade</th><th>Situação</th></tr></thead>
      <tbody>${linhas}</tbody><tfoot><tr><td colspan="3">Valor total em estoque</td><td class="dir num">${brl(r.valorTotal)}</td><td colspan="3"></td></tr></tfoot></table>`
      : '<p class="dica">Estoque vazio.</p>'}
  </section>`;
}

function relFaltando(r) {
  const total = r.itens.reduce((s, i) => s + i.custoEstimado, 0);
  const linhas = r.itens.map((i) => `<tr><td>${esc(i.nome)}</td><td>${i.vencido ? 'Vencido' : 'Estoque baixo'}</td><td class="dir num">${qtdEstoque(i.quantidade, i.unidade)}</td>
    <td class="dir num">${qtdEstoque(i.quantidadeAlerta, i.unidade)}</td><td class="dir num">${qtdEstoque(i.sugestaoCompra, i.unidade)}</td>
    <td class="dir num">${brl(i.custoEstimado)}</td></tr>`).join('');
  return `<section class="folha">
    ${cabRel('Relatório de Itens em Falta', 'Itens com estoque baixo ou vencidos', r.geradoEm)}
    ${r.itens.length ? `<table class="tabela"><thead><tr><th>Item</th><th>Motivo</th><th class="dir">Tem</th><th class="dir">Alerta em</th><th class="dir">Comprar (mín.)</th><th class="dir">Custo estimado</th></tr></thead>
      <tbody>${linhas}</tbody><tfoot><tr><td colspan="5">Total estimado da reposição</td><td class="dir num">${brl(total)}</td></tr></tfoot></table>`
      : '<p class="dica">Nenhum item em falta.</p>'}
  </section>`;
}

async function salvarRelatorios() {
  const data = estado.dataRelatorio;
  try {
    const [corpo, css] = await Promise.all([montarRelatorios('todos', data), fetch('css/estilo.css').then((r) => r.text()).catch(() => '')]);
    const html = `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><title>ConfeitaSys - Relatórios ${dataBR(data)}</title>
      <style>${css} body{background:#fff;padding:24px} .folha{box-shadow:none;margin-bottom:24px}</style></head><body>${corpo}</body></html>`;
    const r = await API.backup.salvarRelatorios(data, html);
    abrirModal(`${cabModal('Relatórios salvos')}
      <div class="modal-corpo">
        <p>Os 3 relatórios foram salvos em:</p><p class="dica" style="word-break:break-all"><strong>${esc(r.arquivo)}</strong></p>
        <p>Backup do banco de dados:</p><p class="dica" style="word-break:break-all"><strong>${esc(r.backup)}</strong></p>
      </div>
      <div class="modal-rodape"><button class="btn btn-primario" data-fechar>OK</button></div>`);
  } catch (e) { aviso(e.message, true); }
}

// ====================================================================== ALERTA DE ESTOQUE (todas as telas)
// Consulta /api/alertas a cada 20 s e depois de cada alteração. Se algum item
// chegou na quantidade de alerta, mostra a mensagem no topo e toca uma sirene.

const Sirene = (() => {
  let ctx = null;
  let pendente = false;
  function contexto() {
    if (!ctx) { const AC = window.AudioContext || window.webkitAudioContext; if (AC) ctx = new AC(); }
    return ctx;
  }
  /** Sirene de emergência: frequência sobe e desce (600 Hz ↔ 1400 Hz) por ~4 segundos. */
  function tocarAgora() {
    const c = contexto();
    if (!c) return;
    const t0 = c.currentTime;
    const osc = c.createOscillator();
    const osc2 = c.createOscillator();
    const ganho = c.createGain();
    osc.type = 'sawtooth';
    osc2.type = 'square';
    for (let k = 0; k < 4; k++) {
      const t = t0 + k * 1.0;
      osc.frequency.setValueAtTime(600, t);
      osc.frequency.linearRampToValueAtTime(1400, t + 0.5);
      osc.frequency.linearRampToValueAtTime(600, t + 1.0);
      osc2.frequency.setValueAtTime(603, t);
      osc2.frequency.linearRampToValueAtTime(1403, t + 0.5);
      osc2.frequency.linearRampToValueAtTime(603, t + 1.0);
    }
    ganho.gain.setValueAtTime(0.0001, t0);
    ganho.gain.exponentialRampToValueAtTime(0.18, t0 + 0.05);
    ganho.gain.setValueAtTime(0.18, t0 + 3.8);
    ganho.gain.exponentialRampToValueAtTime(0.0001, t0 + 4.0);
    osc.connect(ganho); osc2.connect(ganho); ganho.connect(c.destination);
    osc.start(t0); osc2.start(t0); osc.stop(t0 + 4.05); osc2.stop(t0 + 4.05);
  }
  function tocar() {
    const c = contexto();
    if (!c) return false;
    if (c.state === 'running') { tocarAgora(); return true; }
    // navegadores só liberam som depois de um clique/tecla: toca assim que acontecer
    pendente = true;
    c.resume().then(() => { if (c.state === 'running' && pendente) { pendente = false; tocarAgora(); } }).catch(() => {});
    return false;
  }
  function liberar() {
    const c = contexto();
    if (c && c.state !== 'running') c.resume().then(() => { if (pendente) { pendente = false; tocarAgora(); atualizarAvisoSom(); } });
  }
  ['pointerdown', 'keydown'].forEach((ev) => document.addEventListener(ev, liberar, { capture: true }));
  return { tocar, liberado: () => contexto()?.state === 'running' };
})();

function chavesAlerta(a) {
  const hoje = hojeISO();
  return [...a.estoqueBaixo.map((i) => `b:${i.id}:${i.quantidade}:${i.quantidadeAlerta}`), ...a.validade.map((i) => `v:${i.id}:${i.validade}:${hoje}`)];
}
function lerVistos() { try { return JSON.parse(sessionStorage.getItem('alertasVistos') || '[]'); } catch { return []; } }
function gravarVistos(v) { try { sessionStorage.setItem('alertasVistos', JSON.stringify(v)); } catch { /* sem armazenamento */ } }

function atualizarAvisoSom() {
  const el = document.querySelector('.alerta-global .som');
  if (el) el.textContent = Sirene.liberado() ? '' : '🔇 Clique em qualquer lugar para ouvir a sirene.';
}

let ultimoAlerta = null;
async function verificarAlertas() {
  let a;
  try {
    a = await API.alertas.consultar();
  } catch { return; }
  const chaves = chavesAlerta(a);
  const vistos = lerVistos();
  const novos = chaves.filter((k) => !vistos.includes(k));
  const caixa = $('alerta-global');
  if (!novos.length) { if (!chaves.length) caixa.innerHTML = ''; return; }
  const assinatura = chaves.join('|');
  if (assinatura === ultimoAlerta && caixa.innerHTML) return; // já está na tela
  ultimoAlerta = assinatura;

  const baixo = a.estoqueBaixo;
  caixa.innerHTML = `
    <div class="alerta-global" role="alertdialog" aria-label="Alerta de estoque">
      ${baixo.length ? `<div class="faixa">${icone.sirene}<div>
        <strong>ALERTA: ESTOQUE BAIXO</strong>
        <ul>${baixo.map((i) => `<li>${esc(i.mensagem)}</li>`).join('')}</ul></div></div>` : ''}
      ${a.validade.length ? `<div class="validade"><strong>Atenção à validade</strong>
        <ul>${a.validade.map((i) => `<li>${esc(i.mensagem)}</li>`).join('')}</ul></div>` : ''}
      <div class="acoes"><span class="som"></span>
        <button class="btn" id="alerta-ver">Ver estoque</button>
        <button class="btn btn-primario" id="alerta-ok">Entendi</button></div>
    </div>`;
  const fechar = () => { gravarVistos(chaves); caixa.innerHTML = ''; };
  $('alerta-ok').onclick = fechar;
  $('alerta-ver').onclick = () => { fechar(); estado.filtroEstoque = baixo.length ? 'falta' : 'validade'; irPara('estoque'); };
  // sirene só quando há item NOVO com estoque baixo
  if (novos.some((k) => k.startsWith('b:'))) Sirene.tocar();
  atualizarAvisoSom();
}

// ====================================================================== topo, atalhos, início

$('btn-nova-venda').addEventListener('click', () => formularioVenda());
document.querySelectorAll('.nav-btn[data-tela]').forEach((b) => b.addEventListener('click', () => irPara(b.dataset.tela)));

$('filtro-data').value = estado.data;
$('filtro-data').addEventListener('change', (e) => { estado.data = e.target.value; desenhar(); });

let esperaBusca;
$('busca').addEventListener('input', (e) => {
  clearTimeout(esperaBusca);
  $('limpar-busca').classList.toggle('oculto', !e.target.value);
  esperaBusca = setTimeout(() => { estado.busca = e.target.value.trim(); desenhar(); }, 250);
});
$('limpar-busca').addEventListener('click', () => {
  $('busca').value = ''; estado.busca = ''; $('limpar-busca').classList.add('oculto'); desenhar(); $('busca').focus();
});

$('btn-backup').addEventListener('click', async () => {
  try { const r = await API.backup.fazer(); aviso('Backup salvo em: ' + r.arquivo); }
  catch (e) { aviso(e.message, true); }
});
$('btn-sair').addEventListener('click', async () => {
  await API.acesso.logout().catch(() => null);
  Sessao.limpar();
  try { sessionStorage.removeItem('alertasVistos'); } catch { /* ok */ }
  location.href = 'index.html';
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') { if (modalAberto()) fecharModal(); fecharMenus(); return; }
  const digitando = /^(INPUT|SELECT|TEXTAREA)$/.test(document.activeElement?.tagName);
  if (digitando || modalAberto() || e.ctrlKey || e.altKey || e.metaKey) return;
  const mapa = { 1: 'vendas', 2: 'produtos', 3: 'estoque', 4: 'relatorios', 5: 'conta' };
  if (e.key === 'n' || e.key === 'N') { e.preventDefault(); formularioVenda(); }
  else if (e.key === '/' || e.key === 'F3') {
    e.preventDefault();
    if (!['vendas', 'produtos', 'estoque'].includes(estado.tela)) irPara('vendas');
    $('busca').focus();
  } else if (mapa[e.key]) irPara(mapa[e.key]);
});

API.acesso.status().then((st) => {
  if (!st.logado) { Sessao.limpar(); location.href = 'index.html'; return; }
  irPara('vendas');
  verificarAlertas();
  setInterval(verificarAlertas, 20000);
}).catch(() => aviso(MSG_SEM_API, true));
