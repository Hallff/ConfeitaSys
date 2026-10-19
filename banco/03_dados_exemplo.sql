-- =====================================================================
--  ConfeitaSys - 03: DADOS DE EXEMPLO (opcional)
--
--  Preenche o estoque, o cardápio e 3 vendas de hoje para testar o
--  sistema e o Swagger. O USUÁRIO não é criado aqui: ele é cadastrado
--  no primeiro acesso (tela de login ou POST /api/usuario no Swagger).
--
--    psql -U confeitasys -d confeitasys -f 03_dados_exemplo.sql
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- Estoque (Cadastro 2). custo_unitario = R$ por KG ou L.
-- "Chocolate em pó" já começa ABAIXO da quantidade de alerta e o
-- "Leite integral" vence em 2 dias, para demonstrar os alertas.
-- ---------------------------------------------------------------------
INSERT INTO ingrediente (nome, unidade, quantidade, custo_unitario, quantidade_alerta, validade) VALUES
 ('Farinha de trigo',     'KG', 6.900, 5.50,  2.000, CURRENT_DATE + 90),
 ('Açúcar refinado',      'KG', 4.100, 4.80,  1.500, CURRENT_DATE + 180),
 ('Chocolate em pó 50%',  'KG', 0.400, 32.00, 0.500, CURRENT_DATE + 120),
 ('Leite integral',       'L',  5.400, 5.20,  2.000, CURRENT_DATE + 2),
 ('Manteiga',             'KG', 1.600, 42.00, 0.500, CURRENT_DATE + 20),
 ('Ovos',                 'KG', 2.200, 14.00, 1.000, CURRENT_DATE + 10),
 ('Leite condensado',     'KG', 1.580, 18.00, 0.800, CURRENT_DATE + 200),
 ('Cenoura',              'KG', 1.600, 6.00,  0.500, CURRENT_DATE + 5);

-- ---------------------------------------------------------------------
-- Cardápio (Cadastro 3) e receitas (quantidades em KG ou L)
-- Os ids (UUID) são gerados pelo banco; por isso as receitas e as
-- vendas abaixo encontram o produto e o ingrediente pelo NOME.
-- ---------------------------------------------------------------------
INSERT INTO produto (nome, tamanho, recheio, preco_venda) VALUES
 ('Bolo de chocolate',  'G', 'Brigadeiro',             95.00),
 ('Bolo de cenoura',    'M', 'Cobertura de chocolate', 60.00),
 ('Brigadeiro (cento)', '',  '',                      120.00);

INSERT INTO produto_ingrediente (produto_id, ingrediente_id, quantidade)
SELECT p.id, i.id, r.quantidade
  FROM (VALUES
        -- Bolo de chocolate G - Brigadeiro
        ('Bolo de chocolate', 'Farinha de trigo',    0.5000),
        ('Bolo de chocolate', 'Açúcar refinado',     0.4000),
        ('Bolo de chocolate', 'Chocolate em pó 50%', 0.1500),
        ('Bolo de chocolate', 'Leite integral',      0.3000),
        ('Bolo de chocolate', 'Manteiga',            0.1000),
        ('Bolo de chocolate', 'Ovos',                0.3000),
        ('Bolo de chocolate', 'Leite condensado',    0.3950),
        -- Bolo de cenoura M - Cobertura de chocolate
        ('Bolo de cenoura',   'Farinha de trigo',    0.3000),
        ('Bolo de cenoura',   'Açúcar refinado',     0.3000),
        ('Bolo de cenoura',   'Cenoura',             0.4000),
        ('Bolo de cenoura',   'Ovos',                0.2000),
        ('Bolo de cenoura',   'Chocolate em pó 50%', 0.0800),
        -- Brigadeiro (cento)
        ('Brigadeiro (cento)', 'Leite condensado',   1.5800),
        ('Brigadeiro (cento)', 'Chocolate em pó 50%', 0.2000),
        ('Brigadeiro (cento)', 'Manteiga',           0.1000)
       ) AS r (produto, ingrediente, quantidade)
  JOIN produto     p ON p.nome = r.produto
  JOIN ingrediente i ON i.nome = r.ingrediente;

-- ---------------------------------------------------------------------
-- 3 vendas de hoje (o estoque acima já está com a baixa delas)
-- numero = número do pedido (1, 2, 3); o id (UUID) é gerado pelo banco.
-- custo de cada item = quantidade x custo_unitario
-- ---------------------------------------------------------------------
INSERT INTO venda (numero, data_hora, produto_id, produto_nome, valor_venda)
SELECT r.numero, r.data_hora, p.id, r.produto_nome, r.valor_venda
  FROM (VALUES
        (1, CURRENT_DATE + TIME '08:30', 'Bolo de chocolate', 'Bolo de chocolate G - recheio Brigadeiro',            95.00),
        (2, CURRENT_DATE + TIME '10:15', 'Bolo de cenoura',   'Bolo de cenoura M - recheio Cobertura de chocolate',  60.00),
        (3, CURRENT_DATE + TIME '11:40', 'Bolo de chocolate', 'Bolo de chocolate G - recheio Brigadeiro',           100.00)
       ) AS r (numero, data_hora, produto, produto_nome, valor_venda)
  JOIN produto p ON p.nome = r.produto;

INSERT INTO item_venda (venda_id, ingrediente_id, ingrediente_nome, unidade, quantidade, custo)
SELECT v.id, i.id, i.nome, i.unidade, r.quantidade, r.custo
  FROM (VALUES
        (1, 'Farinha de trigo',    0.5000, 2.75),
        (1, 'Açúcar refinado',     0.4000, 1.92),
        (1, 'Chocolate em pó 50%', 0.1500, 4.80),
        (1, 'Leite integral',      0.3000, 1.56),
        (1, 'Manteiga',            0.1000, 4.20),
        (1, 'Ovos',                0.3000, 4.20),
        (1, 'Leite condensado',    0.3950, 7.11),

        (2, 'Farinha de trigo',    0.3000, 1.65),
        (2, 'Açúcar refinado',     0.3000, 1.44),
        (2, 'Cenoura',             0.4000, 2.40),
        (2, 'Ovos',                0.2000, 2.80),
        (2, 'Chocolate em pó 50%', 0.0800, 2.56),

        (3, 'Farinha de trigo',    0.5000, 2.75),
        (3, 'Açúcar refinado',     0.4000, 1.92),
        (3, 'Chocolate em pó 50%', 0.1500, 4.80),
        (3, 'Leite integral',      0.3000, 1.56),
        (3, 'Manteiga',            0.1000, 4.20),
        (3, 'Ovos',                0.3000, 4.20),
        (3, 'Leite condensado',    0.3950, 7.11)
       ) AS r (numero_pedido, ingrediente, quantidade, custo)
  JOIN venda       v ON v.numero = r.numero_pedido
  JOIN ingrediente i ON i.nome = r.ingrediente;

-- Como os números dos pedidos foram informados à mão, ajusta o contador
-- para a próxima venda continuar do número certo (pedido 4).
SELECT setval(pg_get_serial_sequence('venda', 'numero'), (SELECT MAX(numero) FROM venda));

COMMIT;
