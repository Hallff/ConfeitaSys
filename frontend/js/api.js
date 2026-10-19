// =====================================================================
//  ConfeitaSys - TODAS as chamadas do frontend para a API (backend)
//
//  Cada função tem o MESMO NÚMERO da API no Swagger
//  (http://localhost:8080/swagger-ui.html), no backend
//  (comentário "API x.y" em backend/.../controle/*Controle.java)
//  e no arquivo MAPA-DAS-APIS.md.
//
//  Exemplo: a tela de Vendas, ao clicar em "Salvar venda", chama
//  API.vendas.registrar(...)  ->  5.2 POST /api/vendas  ->  VendaControle.registrar()
//
//  Os ids ({id}, produtoId, ingredienteId) são UUID, ex.:
//  "3fa85f64-5717-4562-b3fc-2c963f66afa6". O nº do pedido (#0012) é o
//  campo "numero" da venda, e não o id.
// =====================================================================

/**
 * Faz a requisição HTTP para o backend, com o token de login.
 * Devolve o JSON da resposta ou lança um Error com a mensagem do servidor.
 */
async function requisicao(metodo, caminho, corpo, { publica = false } = {}) {
  const headers = {};
  if (corpo !== undefined) headers['Content-Type'] = 'application/json';
  const token = Sessao.token();
  if (token) headers.Authorization = 'Bearer ' + token;

  let resposta;
  try {
    resposta = await fetch(API_URL + caminho, {
      method: metodo,
      headers,
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined
    });
  } catch {
    throw new Error(MSG_SEM_API);                       // backend desligado
  }
  const dados = await resposta.json().catch(() => ({}));
  if (resposta.status === 401 && !publica) {          // token vencido -> volta ao login
    Sessao.limpar();
    location.href = 'index.html';
    throw new Error('Sessão expirada');
  }
  if (!resposta.ok) throw new Error(dados.erro || 'Erro ao comunicar com o sistema.');
  if (metodo !== 'GET' && API.aoAlterar) API.aoAlterar(); // ex.: conferir alertas de estoque
  return dados;
}

const q = encodeURIComponent;

const API = {
  /** Chamado depois de qualquer alteração (POST/PUT/DELETE). Definido em app.js. */
  aoAlterar: null,

  // ------------------------------------------------ 1. ACESSO (login)
  acesso: {
    /** 1.1 POST /api/login  - Tela Login -> "Entrar" */
    login: (nome, senha) => requisicao('POST', '/api/login', { nome, senha }, { publica: true }),
    /** 1.2 POST /api/logout - menu lateral -> "Sair" */
    logout: () => requisicao('POST', '/api/logout', undefined, { publica: true }),
    /** 1.3 GET  /api/status - ao abrir qualquer tela */
    status: () => requisicao('GET', '/api/status', undefined, { publica: true })
  },

  // ------------------------------------------------ 2. CADASTRO 1 - USUÁRIO
  usuario: {
    /** 2.1 GET    /api/usuario - Tela Conta (ao abrir) */
    consultar: () => requisicao('GET', '/api/usuario'),
    /** 2.2 PUT    /api/usuario - Tela Conta -> "Salvar alterações" */
    alterar: (dados) => requisicao('PUT', '/api/usuario', dados),
    /** 2.3 POST   /api/usuario - Tela Login (primeiro acesso) -> "Criar usuário e entrar" */
    cadastrar: (nome, senha) => requisicao('POST', '/api/usuario', { nome, senha }, { publica: true }),
    /** 2.4 DELETE /api/usuario - Tela Conta -> "Excluir usuário" */
    excluir: (senha) => requisicao('DELETE', '/api/usuario', { senha })
  },

  // ------------------------------------------------ 3. CADASTRO 2 - ESTOQUE
  estoque: {
    /** 3.1 GET    /api/ingredientes      - Tela Estoque (e janelas de produto/venda) */
    listar: () => requisicao('GET', '/api/ingredientes'),
    /** 3.2 POST   /api/ingredientes      - "+ Novo item no estoque" -> "Salvar" */
    incluir: (dados) => requisicao('POST', '/api/ingredientes', dados),
    /** 3.3 PUT    /api/ingredientes/{id} - lápis -> "Salvar" */
    alterar: (id, dados) => requisicao('PUT', '/api/ingredientes/' + id, dados),
    /** 3.4 DELETE /api/ingredientes/{id} - lixeira -> "Excluir item" */
    excluir: (id) => requisicao('DELETE', '/api/ingredientes/' + id)
  },

  // ------------------------------------------------ 4. CADASTRO 3 - PRODUTOS
  produtos: {
    /** 4.1 GET    /api/produtos      - Tela Produtos (e janela Nova venda) */
    listar: () => requisicao('GET', '/api/produtos'),
    /** 4.2 POST   /api/produtos      - "+ Novo produto" -> "Salvar produto" */
    incluir: (dados) => requisicao('POST', '/api/produtos', dados),
    /** 4.3 GET    /api/produtos/{id} - lápis -> "Editar" (carrega) */
    consultar: (id) => requisicao('GET', '/api/produtos/' + id),
    /** 4.4 PUT    /api/produtos/{id} - "Editar" -> "Salvar produto" */
    alterar: (id, dados) => requisicao('PUT', '/api/produtos/' + id, dados),
    /** 4.5 DELETE /api/produtos/{id} - lápis -> "Excluir" */
    excluir: (id) => requisicao('DELETE', '/api/produtos/' + id)
  },

  // ------------------------------------------------ 5. VENDAS
  vendas: {
    /** 5.1 GET    /api/vendas?data=&busca= - Tela Vendas e busca no topo */
    pesquisar: (data, busca) => requisicao('GET', `/api/vendas?data=${q(data || '')}&busca=${q(busca || '')}`),
    /** 5.2 POST   /api/vendas      - botão "+" -> "Salvar venda" (baixa no estoque) */
    registrar: (dados) => requisicao('POST', '/api/vendas', dados),
    /** 5.3 GET    /api/vendas/{id} - lápis -> "Editar" (carrega o pedido) */
    consultar: (id) => requisicao('GET', '/api/vendas/' + id),
    /** 5.4 PUT    /api/vendas/{id} - "Editar" -> "Salvar venda" */
    editar: (id, dados) => requisicao('PUT', '/api/vendas/' + id, dados),
    /** 5.5 DELETE /api/vendas/{id}?devolver=sim|nao - lápis -> "Excluir" */
    excluir: (id, devolverAoEstoque) => requisicao('DELETE', `/api/vendas/${id}?devolver=${devolverAoEstoque ? 'sim' : 'nao'}`)
  },

  // ------------------------------------------------ 6. ALERTAS
  alertas: {
    /** 6.1 GET /api/alertas - todas as telas, a cada 20 s (faixa vermelha + sirene) */
    consultar: () => requisicao('GET', '/api/alertas')
  },

  // ------------------------------------------------ 7. RELATÓRIOS
  relatorios: {
    /** 7.1 GET /api/relatorios/estoque  - aba "Estoque" */
    estoque: () => requisicao('GET', '/api/relatorios/estoque'),
    /** 7.2 GET /api/relatorios/faltando - aba "Itens em falta" */
    faltando: () => requisicao('GET', '/api/relatorios/faltando'),
    /** 7.3 GET /api/relatorios/vendas   - aba "Vendas do dia" */
    vendas: (data) => requisicao('GET', '/api/relatorios/vendas?data=' + q(data || ''))
  },

  // ------------------------------------------------ 8. BACKUP
  backup: {
    /** 8.1 POST /api/backup            - menu lateral -> "Backup" */
    fazer: () => requisicao('POST', '/api/backup'),
    /** 8.2 POST /api/relatorios/salvar - Relatórios -> "Salvar e fazer backup" */
    salvarRelatorios: (data, html) => requisicao('POST', '/api/relatorios/salvar', { data, html })
  }
};
