-- =====================================================================
--  ConfeitaSys - Script do Banco de Dados (SQLite 3)
--  Executado automaticamente pelo sistema na primeira inicialização.
--  Arquivo do banco: dados/confeitasys.db
-- =====================================================================

-- ---------------------------------------------------------------------
-- CADASTRO 1: Usuário do sistema (um único usuário: dono/gerente)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuario (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    senha_hash  TEXT    NOT NULL,                 -- PBKDF2-SHA256: algoritmo$iteracoes$salt$hash
    criado_em   TEXT    NOT NULL                  -- AAAA-MM-DDTHH:MM:SS
);

-- ---------------------------------------------------------------------
-- CADASTRO 2: Ingredientes (itens do estoque)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ingrediente (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    nome              TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    unidade           TEXT    NOT NULL CHECK (unidade IN ('KG', 'L')),
    quantidade        REAL    NOT NULL CHECK (quantidade >= 0),   -- em kg ou L
    custo_unitario    REAL    NOT NULL DEFAULT 0,                 -- R$ por kg ou L (valor pago / quantidade)
    quantidade_alerta REAL    NOT NULL DEFAULT 0,                 -- abaixo disso: alerta de estoque baixo
    validade          TEXT,                                       -- AAAA-MM-DD
    atualizado_em     TEXT    NOT NULL
);

-- ---------------------------------------------------------------------
-- CADASTRO 3: Produtos do cardápio (ex.: Bolo de chocolate G - Brigadeiro)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS produto (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    nome        TEXT    NOT NULL COLLATE NOCASE,
    tamanho     TEXT    NOT NULL DEFAULT '' COLLATE NOCASE,
    recheio     TEXT    NOT NULL DEFAULT '' COLLATE NOCASE,
    preco_venda REAL    NOT NULL CHECK (preco_venda >= 0),
    UNIQUE (nome, tamanho, recheio)
);

-- Receita do produto: quais ingredientes e quanto de cada (em kg ou L)
CREATE TABLE IF NOT EXISTS produto_ingrediente (
    produto_id     INTEGER NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    ingrediente_id INTEGER NOT NULL REFERENCES ingrediente(id) ON DELETE RESTRICT,
    quantidade     REAL    NOT NULL CHECK (quantidade > 0),
    PRIMARY KEY (produto_id, ingrediente_id)
);

-- ---------------------------------------------------------------------
-- MOVIMENTO: Vendas
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS venda (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,   -- nº do pedido (vai na nota do cliente)
    data_hora    TEXT    NOT NULL,
    produto_id   INTEGER REFERENCES produto(id) ON DELETE SET NULL,
    produto_nome TEXT    NOT NULL,                    -- cópia do nome no momento da venda
    valor_venda  REAL    NOT NULL CHECK (valor_venda > 0)
);

-- Ingredientes efetivamente usados em cada venda (baixa no estoque)
CREATE TABLE IF NOT EXISTS item_venda (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    venda_id         INTEGER NOT NULL REFERENCES venda(id) ON DELETE CASCADE,
    ingrediente_id   INTEGER REFERENCES ingrediente(id) ON DELETE SET NULL,
    ingrediente_nome TEXT    NOT NULL,                -- cópia do nome no momento da venda
    unidade          TEXT    NOT NULL,
    quantidade       REAL    NOT NULL CHECK (quantidade > 0),
    custo            REAL    NOT NULL                 -- quantidade x custo_unitario na hora da venda
);

CREATE INDEX IF NOT EXISTS idx_venda_data_hora   ON venda (data_hora);
CREATE INDEX IF NOT EXISTS idx_venda_produto     ON venda (produto_nome);
CREATE INDEX IF NOT EXISTS idx_item_venda_venda  ON item_venda (venda_id);
