# ConfeitaSys — Controle de vendas e estoque da confeitaria

Projeto de Estágio Supervisionado I — Gabriel Furlan de Souza e Hallef Felipe Ribeiro.

O sistema é dividido em **três partes separadas**. Cada uma roda sozinha, e o sistema só funciona com as três ligadas:

```
  ┌───────────────────────┐   HTTP + JSON     ┌──────────────────────────────┐   JDBC    ┌──────────────────────┐
  │ 3. FRONTEND (telas)   │ ────────────────► │ 2. BACKEND (API REST)        │ ────────► │ 1. BANCO DE DADOS    │
  │ HTML, CSS, JavaScript │  token de login   │ Java 17 + Spring Boot        │           │ PostgreSQL           │
  │ localhost:5500        │ ◄──────────────── │ localhost:8080 + Swagger     │ ◄──────── │ localhost:5432       │
  └───────────────────────┘                   └──────────────────────────────┘           └──────────────────────┘
        pasta frontend/                              pasta backend/                            pasta banco/
```

| Pasta | O que tem |
|---|---|
| `banco/` | Scripts SQL do PostgreSQL (banco, usuário, 6 tabelas, índices, comentários, views, dados de exemplo) e os `.bat` para **criar**, **ligar** e **desligar** o banco |
| `backend/` | A **API REST**: 27 rotas numeradas em 8 grupos, documentadas no **Swagger** |
| `frontend/` | As telas; **todas** as chamadas à API ficam em `frontend/js/api.js` |
| `postman/` | Coleção do **Postman** com as 27 APIs, numeradas como no Swagger |
| `docs/` | Especificação de Requisitos, documento de diagramas, diagramas UML (PNG e fonte) e prints das telas |
| `MAPA-DAS-APIS.md` | **Onde fica cada API**: tela → rota → arquivo e linha → tabela do banco |

---

## 1. Instalar (uma vez só)

1. **Java 17** — https://adoptium.net/ → *Temurin 17* para Windows (.msi). Marque **"Add to PATH"** e **"Set JAVA_HOME"**.
2. **PostgreSQL** — https://www.postgresql.org/download/windows/ → *Download the installer*.
   Anote a **senha do usuário `postgres`**, deixe a porta **5432** e marque o **pgAdmin 4**.
3. (Opcional) **VS Code** com o *Extension Pack for Java*, para ver e editar o código.

O **Maven** não precisa ser instalado: o `backend\iniciar-backend.bat` baixa sozinho na primeira vez.

## 2. Criar o banco de dados (uma vez só)

Dois cliques em **`banco\criar-banco.bat`** → digite a senha do `postgres`.
Ele cria o usuário e o banco **`confeitasys`**, as tabelas e (se você quiser) os **dados de exemplo**: estoque, cardápio e 3 vendas de hoje, com um item em estoque baixo para ver o alerta.

> **Já tinha criado o banco na versão anterior (ids numéricos)?** Rode `banco\criar-banco.bat` de novo e responda **S** para continuar:
> as tabelas são recriadas com as chaves em **UUID** (os dados antigos são apagados).

**Chaves primárias em UUID:** todos os `id` são UUID (ex.: `3fa85f64-5717-4562-b3fc-2c963f66afa6`), gerados pelo próprio banco
com `gen_random_uuid()`. A venda tem também a coluna `numero`, que é o **nº do pedido** (#0001, #0002...) impresso na nota e usado na busca;
ele **não** é a chave primária.

## 3. Iniciar o sistema

**Tudo de uma vez:** dois cliques em **`INICIAR-SISTEMA.bat`**. Ele:
1. liga o **banco** (se estiver desligado);
2. abre a janela da **API** (backend) — na primeira vez demora alguns minutos baixando as bibliotecas;
3. abre a janela das **telas** (frontend);
4. abre no navegador o **Swagger** e o **sistema**.

**Ou cada parte separada, nesta ordem:**

| Ordem | Parte | Como iniciar | Endereço |
|---|---|---|---|
| 1 | Banco de dados | `banco\iniciar-banco.bat` (se pedir, *Executar como administrador*) | `localhost:5432` |
| 2 | API (backend) | `backend\iniciar-backend.bat` — ou, no VS Code, *Run* em `ConfeitaSysApplication.java` | http://localhost:8080 |
| 3 | Telas (frontend) | `frontend\iniciar-frontend.bat` | http://localhost:5500 |

> Para mostrar que o sistema depende do banco: com tudo rodando, execute `banco\parar-banco.bat` (como administrador).
> A API passa a responder *"Não foi possível conectar ao banco de dados PostgreSQL"*. Depois, `iniciar-banco.bat` e tudo volta a funcionar.

## 4. Abrir o Swagger (apresentação das APIs)

Com a API rodando: dois cliques em **`ABRIR-SWAGGER.url`** (ou http://localhost:8080/swagger-ui.html).

As APIs aparecem em 8 grupos, na ordem dos passos do sistema:

| Grupo | O que faz | Arquivo da API |
|---|---|---|
| 1. Acesso (login) | entrar, sair, situação do sistema | `controle/AcessoControle.java` |
| 2. Cadastro 1 - Usuário | incluir (primeiro acesso), consultar, alterar, excluir | `controle/UsuarioControle.java` |
| 3. Cadastro 2 - Estoque | ingredientes com quantidade de alerta e validade | `controle/IngredienteControle.java` |
| 4. Cadastro 3 - Produtos | cardápio e receitas | `controle/ProdutoControle.java` |
| 5. Vendas | **registrar venda com baixa no estoque**, pesquisar, editar, excluir | `controle/VendaControle.java` |
| 6. Alertas | estoque baixo (sirene) e validade | `controle/AlertaControle.java` |
| 7. Relatórios | estoque, itens em falta, vendas do dia | `controle/RelatorioControle.java` |
| 8. Backup | cópia de segurança e relatórios em arquivo | `controle/BackupControle.java` |

(`controle/` = `backend/src/main/java/br/com/confeitasys/controle/`)

Cada rota mostra **de qual tela e botão ela é chamada**, a **função do frontend** que a chama e o **caminho no backend até a tabela**.

**Testar uma API no Swagger:**
1. **2.3** `POST /api/usuario` (primeiro acesso) ou **1.1** `POST /api/login` → *Try it out* → nome e senha → *Execute*.
2. Copie o `token` da resposta → botão **Authorize** (cadeado) → cole → *Authorize*.
3. Agora qualquer rota funciona. Os ids são **UUID**: copie o `id` de uma lista (ex.: **4.1** `GET /api/produtos`) e cole onde pedir.
   Exemplo: **5.2** `POST /api/vendas` com `{"produtoId": "<id do produto copiado>", "valorVenda": 95}` registra uma venda;
   **6.1** `GET /api/alertas` mostra o estoque baixo; **3.1** `GET /api/ingredientes` mostra o estoque já com a baixa.

### Roteiro sugerido para a apresentação
1. Mostrar as 3 pastas separadas (`banco`, `backend`, `frontend`) e iniciar na ordem.
2. No Swagger, abrir o grupo **5. Vendas → 5.2 Registrar venda**: ler "Tela / Frontend / Backend / Tabelas" e executar.
3. Abrir **3.1 Listar o estoque** e mostrar que o ingrediente baixou; abrir **6.1 Alertas**.
4. No sistema (telas), registrar a mesma venda pelo botão **+** e mostrar a faixa de alerta com a sirene.
5. Se perguntarem "onde fica no código": `MAPA-DAS-APIS.md` tem o arquivo e a linha de cada API
   (ou no VS Code: **Ctrl+Shift+F** → `API 5.2`).

## 5. Testar as APIs no Postman

1. Instale o Postman (https://www.postman.com/downloads/) e abra. Pode usar sem criar conta (*Skip* / *Lightweight API Client*).
2. **Import** (canto superior esquerdo) → arraste o arquivo `postman/ConfeitaSys.postman_collection.json`.
3. Aparece a coleção **ConfeitaSys API** com as mesmas 8 pastas e os mesmos números do Swagger.
4. Com o banco e a API ligados, rode primeiro **2.3 Cadastrar usuário** (primeiro acesso) ou **1.1 Entrar** → **Send**.
   O token é guardado sozinho; não precisa copiar.
5. Rode qualquer outra API. Exemplo: **5.2 Registrar venda** → **Send** → depois **3.1 Listar o estoque** para ver a baixa.

Usuário e senha usados no login ficam nas variáveis da coleção (clique na coleção → aba **Variables**: `usuario` = gabriel, `senha` = confeita123).
As APIs que precisam de um id (ex.: 3.3, 4.3, 5.3) usam as variáveis `ingredienteId`, `produtoId` e `vendaId` (UUID). Elas são
preenchidas sozinhas: pelas listas (3.1, 4.1, 5.1), pelos cadastros (3.2, 4.2, 5.2) ou, se ainda estiverem vazias, a coleção busca na API
antes de enviar.

## 6. Ver os dados no banco

pgAdmin, DBeaver ou outra ferramenta: host `localhost`, porta `5432`, banco `confeitasys`, usuário `confeitasys`, senha `confeitasys`.
Consultas prontas: `SELECT * FROM vw_venda_resumo;` e `SELECT * FROM vw_estoque_alerta;`.
Para alterar um registro pelo id, use o UUID entre aspas simples: `UPDATE produto SET preco_venda = 99 WHERE id = '3fa85f64-...';`.
Tudo o que o usuário faz nas telas é gravado na hora pela API — não é preciso alterar nada no banco manualmente.

---

## Organização do código

```
ConfeitaSys/
├── INICIAR-SISTEMA.bat            ← liga banco + API + telas e abre o Swagger
├── ABRIR-SWAGGER.url  ABRIR-SISTEMA.url
├── MAPA-DAS-APIS.md               ← onde fica cada API (arquivo e linha)
├── banco/
│   ├── 01_criar_banco.sql         ← usuário e banco
│   ├── 02_tabelas.sql             ← 6 tabelas (chaves UUID), índices, comentários, 2 views
│   ├── 03_dados_exemplo.sql       ← dados para teste (opcional)
│   └── criar-banco.bat  iniciar-banco.bat  parar-banco.bat
├── backend/                       ← API REST (Spring Boot)
│   ├── pom.xml  iniciar-backend.bat
│   └── src/main/
│       ├── resources/application.properties   ← porta, banco, Swagger
│       └── java/br/com/confeitasys/
│           ├── controle/   ← AS APIs (uma classe por grupo do Swagger)
│           ├── servico/    ← regras de negócio e transações
│           ├── dao/        ← SQL no PostgreSQL (JdbcTemplate)
│           ├── modelo/     ← Usuario, Ingrediente, Produto, ItemReceita, Venda, ItemVenda
│           ├── dto/        ← formato do JSON de entrada e saída
│           ├── seguranca/  ← senha (PBKDF2), token de login, filtro de autenticação
│           ├── config/     ← Swagger, CORS, verificação do banco ao iniciar
│           └── excecao/    ← mensagens de erro em JSON
├── frontend/                      ← telas
│   ├── index.html (login)  app.html (sistema)
│   ├── js/config.js   ← endereço da API
│   ├── js/api.js      ← TODAS as chamadas à API, numeradas como no Swagger
│   ├── js/login.js  js/app.js  css/estilo.css  img/icone.svg
│   └── ServidorFrontend.java  iniciar-frontend.bat
└── docs/                          ← documentação do estágio e diagramas
```

**Segurança e integridade:** senha com PBKDF2-SHA256 (210.000 iterações, salt de 16 bytes). Token de 64 caracteres que vale 12 h.
Login bloqueado por 60 s após 5 senhas erradas. Venda e baixa no estoque na mesma transação, com `SELECT ... FOR UPDATE`
para que duas vendas simultâneas não gastem o mesmo estoque. Chaves estrangeiras, `CHECK` e índices únicos no banco.

## Problemas comuns

| Sintoma | Solução |
|---|---|
| `iniciar-banco.bat`: *Nao foi possivel ligar o servico* | Botão direito → **Executar como administrador**. |
| Janela da API: *NAO FOI POSSIVEL USAR O BANCO* | Banco desligado (`iniciar-banco.bat`), não criado (`criar-banco.bat`) ou senha diferente em `backend/src/main/resources/application.properties`. |
| Telas: *Não foi possível falar com a API* | A API não está rodando: inicie o backend e recarregue a página. |
| `password authentication failed for user "postgres"` | É a senha definida na instalação do PostgreSQL. |
| `role "confeitasys" already exists` | O banco já existia; responda **S** para continuar. |
| Janela da API: *BANCO NA VERSAO ANTIGA (ids numericos)* | Rode `banco\criar-banco.bat` de novo (responda **S**): as tabelas são recriadas com UUID. |
| Swagger/Postman: *Id inválido: o id é um UUID* | Copie o `id` completo de uma lista (3.1, 4.1 ou 5.1); números como `1` não valem mais. |
| *Port 8080 already in use* | Outra cópia da API está aberta: feche a janela antiga. |
| Primeira execução da API muito demorada | Normal: o Maven baixa as bibliotecas do Spring uma única vez. |
