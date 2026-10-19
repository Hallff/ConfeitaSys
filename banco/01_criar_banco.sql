-- =====================================================================
--  ConfeitaSys - 01: criar o USUÁRIO e o BANCO DE DADOS (PostgreSQL)
--
--  Rode este script UMA VEZ, conectado como o superusuário "postgres".
--    Pelo terminal:  psql -U postgres -f 01_criar_banco.sql
--    Ou use o arquivo criar-banco.bat, que roda os 3 scripts em ordem.
--
--  Atenção: CREATE DATABASE não pode rodar dentro de uma transação.
--  No pgAdmin, execute cada comando separadamente (selecione e F5).
-- =====================================================================

-- Usuário (role) que o backend usa para conectar.
-- Troque a senha aqui E no backend (src/main/resources/application.properties).
CREATE ROLE confeitasys WITH LOGIN PASSWORD 'confeitasys';

-- Banco de dados do sistema, pertencente ao usuário acima.
CREATE DATABASE confeitasys
    WITH OWNER = confeitasys
         ENCODING = 'UTF8'
         TEMPLATE = template0;

COMMENT ON DATABASE confeitasys IS 'ConfeitaSys - controle de vendas e estoque da confeitaria';
