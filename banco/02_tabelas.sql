-- =====================================================================
--  ConfeitaSys - 02: TABELAS, ÍNDICES, COMENTÁRIOS E VIEWS (PostgreSQL)
--
--  Rode conectado ao banco "confeitasys" com o usuário "confeitasys":
--    psql -U confeitasys -d confeitasys -f 02_tabelas.sql
--
--  CHAVES PRIMÁRIAS: todas são UUID, geradas pelo próprio banco com
--  gen_random_uuid() (função nativa do PostgreSQL 13 ou mais novo).
--  A venda tem também o "numero" do pedido (1, 2, 3...), que é só
--  para mostrar na nota do cliente - a chave continua sendo o UUID.
--
--  O script pode ser rodado de novo: ele apaga e recria tudo
--  (CUIDADO: os dados existentes são perdidos).
-- =====================================================================

BEGIN;

DROP VIEW  IF EXISTS vw_estoque_alerta;
DROP VIEW  IF EXISTS vw_venda_resumo;
DROP TABLE IF EXISTS item_venda;
DROP TABLE IF EXISTS venda;
DROP TABLE IF EXISTS produto_ingrediente;
DROP TABLE IF EXISTS produto;
DROP TABLE IF EXISTS ingrediente;
DROP TABLE IF EXISTS usuario;

-- ---------------------------------------------------------------------
-- CADASTRO 1: Usuário do sistema (um único usuário: dono/gerente)
-- ---------------------------------------------------------------------
CREATE TABLE usuario (
    id          UUID         DEFAULT gen_random_uuid(),
    nome        VARCHAR(60)  NOT NULL,
    senha_hash  VARCHAR(200) NOT NULL,
    criado_em   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario PRIMARY KEY (id)
);
-- nome único, sem diferenciar maiúsculas de minúsculas
CREATE UNIQUE INDEX uq_usuario_nome  ON usuario (LOWER(nome));
-- sistema monousuário: no máximo UMA linha nesta tabela
CREATE UNIQUE INDEX uq_usuario_unico ON usuario ((TRUE));

COMMENT ON TABLE  usuario            IS 'Cadastro 1 - usuário do sistema (dono/gerente). Sistema monousuário.';
COMMENT ON COLUMN usuario.id         IS 'Identificador (UUID gerado automaticamente)';
COMMENT ON COLUMN usuario.nome       IS 'Nome de login (único)';
COMMENT ON COLUMN usuario.senha_hash IS 'Senha criptografada: pbkdf2_sha256$iteracoes$salt$hash';
COMMENT ON COLUMN usuario.criado_em  IS 'Data e hora do cadastro';

-- ---------------------------------------------------------------------
-- CADASTRO 2: Ingredientes (itens do estoque)
-- ---------------------------------------------------------------------
CREATE TABLE ingrediente (
    id                UUID          DEFAULT gen_random_uuid(),
    nome              VARCHAR(80)   NOT NULL,
    unidade           VARCHAR(2)    NOT NULL,
    quantidade        NUMERIC(12,3) NOT NULL DEFAULT 0,
    custo_unitario    NUMERIC(14,6) NOT NULL DEFAULT 0,
    quantidade_alerta NUMERIC(12,3) NOT NULL DEFAULT 0,
    validade          DATE,
    atualizado_em     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_ingrediente          PRIMARY KEY (id),
    CONSTRAINT ck_ingrediente_unidade  CHECK (unidade IN ('KG', 'L')),
    CONSTRAINT ck_ingrediente_qtd      CHECK (quantidade >= 0),
    CONSTRAINT ck_ingrediente_custo    CHECK (custo_unitario >= 0),
    CONSTRAINT ck_ingrediente_alerta   CHECK (quantidade_alerta >= 0)
);
CREATE UNIQUE INDEX uq_ingrediente_nome ON ingrediente (LOWER(nome));

COMMENT ON TABLE  ingrediente                   IS 'Cadastro 2 - itens do estoque (ingredientes)';
COMMENT ON COLUMN ingrediente.id                IS 'Identificador (UUID gerado automaticamente)';
COMMENT ON COLUMN ingrediente.nome              IS 'Nome do ingrediente (único)';
COMMENT ON COLUMN ingrediente.unidade           IS 'Unidade de medida: KG (quilogramas) ou L (litros)';
COMMENT ON COLUMN ingrediente.quantidade        IS 'Quantidade em estoque, em KG ou L';
COMMENT ON COLUMN ingrediente.custo_unitario    IS 'Custo por KG ou L = valor pago / quantidade comprada';
COMMENT ON COLUMN ingrediente.quantidade_alerta IS 'Quando a quantidade fica menor ou igual a este valor, o sistema emite o alerta de estoque baixo (definido por item)';
COMMENT ON COLUMN ingrediente.validade          IS 'Data de validade (opcional)';
COMMENT ON COLUMN ingrediente.atualizado_em     IS 'Data e hora da última alteração ou movimentação';

-- ---------------------------------------------------------------------
-- CADASTRO 3: Produtos do cardápio (ex.: Bolo de chocolate G - Brigadeiro)
-- ---------------------------------------------------------------------
CREATE TABLE produto (
    id          UUID          DEFAULT gen_random_uuid(),
    nome        VARCHAR(80)   NOT NULL,
    tamanho     VARCHAR(20)   NOT NULL DEFAULT '',
    recheio     VARCHAR(60)   NOT NULL DEFAULT '',
    preco_venda NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_produto       PRIMARY KEY (id),
    CONSTRAINT ck_produto_preco CHECK (preco_venda > 0)
);
CREATE UNIQUE INDEX uq_produto ON produto (LOWER(nome), LOWER(tamanho), LOWER(recheio));

COMMENT ON TABLE  produto             IS 'Cadastro 3 - produtos do cardápio';
COMMENT ON COLUMN produto.id          IS 'Identificador (UUID gerado automaticamente)';
COMMENT ON COLUMN produto.tamanho     IS 'Tamanho (ex.: P, M, G)';
COMMENT ON COLUMN produto.recheio     IS 'Recheio / sabor';
COMMENT ON COLUMN produto.preco_venda IS 'Preço de venda sugerido (R$)';

-- Receita do produto: quais ingredientes e quanto de cada (em KG ou L)
CREATE TABLE produto_ingrediente (
    produto_id     UUID          NOT NULL,
    ingrediente_id UUID          NOT NULL,
    quantidade     NUMERIC(12,4) NOT NULL,
    CONSTRAINT pk_produto_ingrediente PRIMARY KEY (produto_id, ingrediente_id),
    CONSTRAINT fk_receita_produto     FOREIGN KEY (produto_id)     REFERENCES produto (id)     ON DELETE CASCADE,
    CONSTRAINT fk_receita_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE RESTRICT,
    CONSTRAINT ck_receita_qtd         CHECK (quantidade > 0)
);
CREATE INDEX ix_receita_ingrediente ON produto_ingrediente (ingrediente_id);

COMMENT ON TABLE  produto_ingrediente            IS 'Receita do produto: ingredientes e quantidades. Ingrediente usado em receita não pode ser excluído (RESTRICT).';
COMMENT ON COLUMN produto_ingrediente.quantidade IS 'Quantidade do ingrediente na receita, em KG ou L';

-- ---------------------------------------------------------------------
-- MOVIMENTO: Vendas
-- ---------------------------------------------------------------------
CREATE TABLE venda (
    id           UUID          DEFAULT gen_random_uuid(),
    numero       INTEGER       GENERATED BY DEFAULT AS IDENTITY,
    data_hora    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    produto_id   UUID,
    produto_nome VARCHAR(170)  NOT NULL,
    valor_venda  NUMERIC(10,2) NOT NULL,
    CONSTRAINT pk_venda         PRIMARY KEY (id),
    CONSTRAINT uq_venda_numero  UNIQUE (numero),
    CONSTRAINT fk_venda_produto FOREIGN KEY (produto_id) REFERENCES produto (id) ON DELETE SET NULL,
    CONSTRAINT ck_venda_valor   CHECK (valor_venda > 0)
);
CREATE INDEX ix_venda_data_hora ON venda (data_hora);

COMMENT ON TABLE  venda              IS 'Movimento - vendas registradas';
COMMENT ON COLUMN venda.id           IS 'Identificador da venda (UUID gerado automaticamente)';
COMMENT ON COLUMN venda.numero       IS 'Número do pedido (sequencial, vai na nota do cliente). Não é a chave primária.';
COMMENT ON COLUMN venda.produto_id   IS 'Produto vendido (fica nulo se o produto for excluído do cardápio)';
COMMENT ON COLUMN venda.produto_nome IS 'Cópia do nome do produto no momento da venda (histórico)';
COMMENT ON COLUMN venda.valor_venda  IS 'Valor cobrado do cliente (R$)';

-- Ingredientes efetivamente baixados do estoque em cada venda
CREATE TABLE item_venda (
    id               UUID          DEFAULT gen_random_uuid(),
    venda_id         UUID          NOT NULL,
    ingrediente_id   UUID,
    ingrediente_nome VARCHAR(80)   NOT NULL,
    unidade          VARCHAR(2)    NOT NULL,
    quantidade       NUMERIC(12,4) NOT NULL,
    custo            NUMERIC(12,2) NOT NULL,
    CONSTRAINT pk_item_venda             PRIMARY KEY (id),
    CONSTRAINT fk_item_venda_venda       FOREIGN KEY (venda_id)       REFERENCES venda (id)       ON DELETE CASCADE,
    CONSTRAINT fk_item_venda_ingrediente FOREIGN KEY (ingrediente_id) REFERENCES ingrediente (id) ON DELETE SET NULL,
    CONSTRAINT ck_item_venda_qtd         CHECK (quantidade > 0),
    CONSTRAINT ck_item_venda_custo       CHECK (custo >= 0)
);
CREATE INDEX ix_item_venda_venda ON item_venda (venda_id);

COMMENT ON TABLE  item_venda       IS 'Ingredientes usados em cada venda (baixa no estoque)';
COMMENT ON COLUMN item_venda.id    IS 'Identificador (UUID gerado automaticamente)';
COMMENT ON COLUMN item_venda.custo IS 'quantidade x custo_unitario no momento da venda (R$)';

-- ---------------------------------------------------------------------
-- VIEWS (consultas prontas, úteis no pgAdmin / DBeaver)
-- ---------------------------------------------------------------------

-- Vendas com custo e lucro calculados
CREATE VIEW vw_venda_resumo AS
SELECT v.numero                               AS numero_pedido,
       v.id,
       v.data_hora,
       v.produto_nome,
       v.valor_venda,
       COALESCE(SUM(i.custo), 0)              AS custo,
       v.valor_venda - COALESCE(SUM(i.custo), 0) AS lucro
  FROM venda v
  LEFT JOIN item_venda i ON i.venda_id = v.id
 GROUP BY v.id, v.numero, v.data_hora, v.produto_nome, v.valor_venda;

COMMENT ON VIEW vw_venda_resumo IS 'Vendas com custo e lucro calculados';

-- Ingredientes que estão em alerta (estoque baixo, vencidos ou vencendo em até 3 dias)
CREATE VIEW vw_estoque_alerta AS
SELECT id, nome, unidade, quantidade, quantidade_alerta, validade,
       (quantidade <= quantidade_alerta)                     AS estoque_baixo,
       (validade IS NOT NULL AND validade < CURRENT_DATE)    AS vencido,
       (validade IS NOT NULL AND validade BETWEEN CURRENT_DATE AND CURRENT_DATE + 3) AS vence_logo
  FROM ingrediente
 WHERE quantidade <= quantidade_alerta
    OR (validade IS NOT NULL AND validade <= CURRENT_DATE + 3);

COMMENT ON VIEW vw_estoque_alerta IS 'Ingredientes com estoque baixo, vencidos ou vencendo em até 3 dias';

COMMIT;
