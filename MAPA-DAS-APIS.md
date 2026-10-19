# Mapa das APIs do ConfeitaSys — onde fica cada coisa

Este arquivo responde à pergunta **"onde fica a API que faz X?"**. Cada API tem um **número** (ex.: **5.2**) que é o mesmo em quatro lugares:

| Onde | Como aparece |
|---|---|
| **Swagger** (http://localhost:8080/swagger-ui.html) | título da rota: `5.2 Registrar venda (com baixa no estoque)` |
| **Backend** (`backend/.../controle/*Controle.java`) | comentário acima do método: `/** API 5.2 - REGISTRAR VENDA */` |
| **Frontend** (`frontend/js/api.js`) | comentário da função: `/** 5.2 POST /api/vendas ... */ registrar: ...` |
| **Este mapa** | tabelas abaixo, com arquivo e linha |

> Dica: no VS Code, **Ctrl+Shift+F** e busque `API 5.2` para cair direto no método do backend.

## Caminho de uma requisição (exemplo: registrar uma venda)

```
TELA  frontend/app.html + js/app.js         usuário clica em "Salvar venda"
  │    js/api.js  →  API.vendas.registrar()   (5.2)  monta o JSON e envia com o token
  ▼  HTTP POST http://localhost:8080/api/vendas   Authorization: Bearer <token>
API   seguranca/AutenticacaoInterceptor        confere o token (senão 401)
  │    controle/VendaControle.registrar()       recebe o JSON (VendaRequisicao)
  ▼    servico/VendaServico.registrar()         regras + transação: confere estoque, calcula custo/lucro
SQL   dao/VendaDAO.inserir()                   INSERT venda + item_venda
  │    dao/IngredienteDAO.movimentar()          UPDATE ingrediente (baixa no estoque)
  ▼
BANCO PostgreSQL  →  tabelas venda, item_venda, ingrediente
```

| Camada | Pasta / arquivo | Papel |
|---|---|---|
| Telas | `frontend/*.html`, `frontend/js/app.js` | o que o usuário vê e clica |
| Chamadas à API | `frontend/js/api.js` | **todas** as chamadas HTTP, uma função por API |
| API REST | `backend/.../controle/` | recebe a requisição, devolve JSON — **é aqui que ficam as APIs** |
| Regras de negócio | `backend/.../servico/` | validações, cálculos e transações |
| SQL | `backend/.../dao/` | comandos SQL no PostgreSQL |
| Formatos JSON | `backend/.../dto/` | o que entra e sai de cada API (aparece no Swagger) |
| Banco de dados | `banco/02_tabelas.sql` | tabelas, chaves e regras do PostgreSQL |

`backend/...` = `backend/src/main/java/br/com/confeitasys`

## 1. Acesso (login)

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/AcessoControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **1.1** | Entrar | Login → botão "Entrar" | `POST /api/login` | `API.acesso.login()` linha 57 | `AcessoControle.login()` **linha 69** → UsuarioServico.login() → UsuarioDAO.buscar() | usuario |
| **1.2** | Sair | menu lateral → "Sair" | `POST /api/logout` | `API.acesso.logout()` linha 59 | `AcessoControle.logout()` **linha 95** → Sessoes.encerrar() | — |
| **1.3** | Situação do sistema | Login (ao abrir) decide entre "primeiro acesso" e "entrar"; telas internas conferem se o login vale. | `GET /api/status` | `API.acesso.status()` linha 61 | `AcessoControle.status()` **linha 114** → UsuarioServico.existeUsuario() → UsuarioDAO.buscar() | usuario |

## 2. Cadastro 1 - Usuário

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/UsuarioControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **2.1** | Consultar usuário | Conta (ao abrir) | `GET /api/usuario` | `API.usuario.consultar()` linha 67 | `UsuarioControle.consultar()` **linha 59** → UsuarioServico.consultar() → UsuarioDAO.buscar() | usuario |
| **2.2** | Alterar usuário | Conta → botão "Salvar alterações" | `PUT /api/usuario` | `API.usuario.alterar()` linha 69 | `UsuarioControle.alterar()` **linha 75** → UsuarioServico.alterar() → UsuarioDAO.alterar() | usuario |
| **2.3** | Cadastrar usuário (primeiro acesso) | Login (primeiro acesso) → botão "Criar usuário e entrar" | `POST /api/usuario` | `API.usuario.cadastrar()` linha 71 | `UsuarioControle.cadastrar()` **linha 94** → UsuarioServico.cadastrar() → UsuarioDAO.inserir() | usuario |
| **2.4** | Excluir usuário | Conta → botão "Excluir usuário" | `DELETE /api/usuario` | `API.usuario.excluir()` linha 73 | `UsuarioControle.excluir()` **linha 112** → UsuarioServico.excluir() → UsuarioDAO.excluir() | usuario |

## 3. Cadastro 2 - Estoque

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/IngredienteControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **3.1** | Listar estoque | Estoque (ao abrir); também carrega a lista de ingredientes nas janelas de produto e de venda. | `GET /api/ingredientes` | `API.estoque.listar()` linha 79 | `IngredienteControle.listar()` **linha 58** → IngredienteServico.listar() → IngredienteDAO.listar() | ingrediente |
| **3.2** | Incluir ingrediente | Estoque → "+ Novo item no estoque" → "Salvar" | `POST /api/ingredientes` | `API.estoque.incluir()` linha 81 | `IngredienteControle.incluir()` **linha 76** → IngredienteServico.incluir() → IngredienteDAO.inserir() | ingrediente |
| **3.3** | Alterar ingrediente | Estoque → lápis (Editar) → "Salvar" | `PUT /api/ingredientes/{id}` | `API.estoque.alterar()` linha 83 | `IngredienteControle.alterar()` **linha 91** → IngredienteServico.alterar() → IngredienteDAO.alterar() | ingrediente |
| **3.4** | Excluir ingrediente | Estoque → lixeira (Excluir) → "Excluir item" | `DELETE /api/ingredientes/{id}` | `API.estoque.excluir()` linha 85 | `IngredienteControle.excluir()` **linha 108** → IngredienteServico.excluir() → IngredienteDAO.excluir() | ingrediente (confere produto_ingrediente) |

## 4. Cadastro 3 - Produtos

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/ProdutoControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **4.1** | Listar cardápio | Produtos (ao abrir); também carrega a lista de produtos na janela de Nova venda. | `GET /api/produtos` | `API.produtos.listar()` linha 91 | `ProdutoControle.listar()` **linha 59** → ProdutoServico.listar() → ProdutoDAO.listar() | produto, produto_ingrediente, ingrediente |
| **4.2** | Incluir produto | Produtos → "+ Novo produto" → "Salvar produto" | `POST /api/produtos` | `API.produtos.incluir()` linha 93 | `ProdutoControle.incluir()` **linha 76** → ProdutoServico.incluir() → ProdutoDAO.inserir() | produto, produto_ingrediente |
| **4.3** | Consultar produto | Produtos → lápis → "Editar" (carrega os dados na janela) | `GET /api/produtos/{id}` | `API.produtos.consultar()` linha 95 | `ProdutoControle.consultar()` **linha 91** → ProdutoServico.consultar() → ProdutoDAO.buscar() | produto, produto_ingrediente, ingrediente |
| **4.4** | Alterar produto | Produtos → lápis → "Editar" → "Salvar produto" | `PUT /api/produtos/{id}` | `API.produtos.alterar()` linha 97 | `ProdutoControle.alterar()` **linha 106** → ProdutoServico.alterar() → ProdutoDAO.alterar() | produto, produto_ingrediente (a receita é regravada) |
| **4.5** | Excluir produto | Produtos → lápis → "Excluir" → "Excluir produto" | `DELETE /api/produtos/{id}` | `API.produtos.excluir()` linha 99 | `ProdutoControle.excluir()` **linha 122** → ProdutoServico.excluir() → ProdutoDAO.excluir() | produto (a receita sai junto; as vendas antigas guardam o nome do produto) |

## 5. Vendas

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/VendaControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **5.1** | Pesquisar vendas | Vendas (lista do dia escolhido) e campo de busca no canto superior direito. | `GET /api/vendas` | `API.vendas.pesquisar()` linha 105 | `VendaControle.pesquisar()` **linha 61** → VendaServico.pesquisar() → VendaDAO.listar() | venda, item_venda |
| **5.2** | Registrar venda | Vendas → botão "+" no centro do topo (ou tecla N) → "Salvar venda" | `POST /api/vendas` | `API.vendas.registrar()` linha 107 | `VendaControle.registrar()` **linha 85** → VendaServico.registrar() → VendaDAO.inserir() + IngredienteDAO.movimentar() | venda, item_venda, ingrediente (baixa) |
| **5.3** | Consultar venda | Vendas → lápis do pedido → "Editar" (carrega o pedido na janela) | `GET /api/vendas/{id}` | `API.vendas.consultar()` linha 109 | `VendaControle.consultar()` **linha 100** → VendaServico.consultar() → VendaDAO.buscar() | venda, item_venda |
| **5.4** | Editar venda | Vendas → lápis → "Editar" → "Salvar venda" | `PUT /api/vendas/{id}` | `API.vendas.editar()` linha 111 | `VendaControle.editar()` **linha 117** → VendaServico.editar() → VendaDAO.alterar() + IngredienteDAO.movimentar() | venda, item_venda, ingrediente |
| **5.5** | Excluir venda / devolução | Vendas → lápis → "Excluir" → "Excluir venda" (com a opção de devolver ao estoque) | `DELETE /api/vendas/{id}` | `API.vendas.excluir()` linha 113 | `VendaControle.excluir()` **linha 134** → VendaServico.excluir() → VendaDAO.excluir() (+ IngredienteDAO.movimentar() se devolver) | venda, item_venda, ingrediente |

## 6. Alertas

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/AlertaControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **6.1** | Alertas | todas (faixa vermelha no topo + sirene), consultado a cada 20 s e após cada alteração. | `GET /api/alertas` | `API.alertas.consultar()` linha 119 | `AlertaControle.alertas()` **linha 42** → IngredienteServico.alertas() → IngredienteDAO.listar() | ingrediente |

## 7. Relatórios

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/RelatorioControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **7.1** | Relatório do estoque | Relatórios → aba "Estoque" | `GET /api/relatorios/estoque` | `API.relatorios.estoque()` linha 125 | `RelatorioControle.estoque()` **linha 48** → RelatorioServico.estoque() | ingrediente |
| **7.2** | Relatório de itens em falta | Relatórios → aba "Itens em falta" | `GET /api/relatorios/faltando` | `API.relatorios.faltando()` linha 127 | `RelatorioControle.faltando()` **linha 64** → RelatorioServico.faltando() | ingrediente |
| **7.3** | Relatório de vendas do dia | Relatórios → aba "Vendas do dia" | `GET /api/relatorios/vendas` | `API.relatorios.vendas()` linha 129 | `RelatorioControle.vendas()` **linha 80** → RelatorioServico.vendasDoDia() | venda, item_venda |

## 8. Backup

Arquivo da API: **`backend/src/main/java/br/com/confeitasys/controle/BackupControle.java`**

| Nº | O que faz | Tela → botão | Rota | Frontend (`js/api.js`) | Backend (método → regras → SQL) | Tabelas |
|---|---|---|---|---|---|---|
| **8.1** | Fazer backup | menu lateral → "Backup" | `POST /api/backup` | `API.backup.fazer()` linha 135 | `BackupControle.fazerBackup()` **linha 50** → BackupServico.fazerBackup() | todas |
| **8.2** | Salvar relatórios + backup | Relatórios → botão "Salvar e fazer backup" | `POST /api/relatorios/salvar` | `API.backup.salvarRelatorios()` linha 137 | `BackupControle.salvarRelatorios()` **linha 65** → RelatorioServico.salvarHtml() + BackupServico.fazerBackup() | — |

## Perguntas rápidas (para a apresentação)

- **Onde a venda é gravada e o estoque baixado na mesma transação?**  
  `backend/.../servico/VendaServico.java:88` (`@Transactional`) → `backend/.../dao/VendaDAO.java:75` e `backend/.../dao/IngredienteDAO.java:76`
- **Onde o sistema confere se há estoque suficiente?**  
  `backend/.../servico/VendaServico.java:153`
- **Como duas vendas ao mesmo tempo não gastam o mesmo estoque?**  
  `SELECT ... FOR UPDATE` em `backend/.../dao/IngredienteDAO.java:48`
- **Onde é decidido que o estoque está baixo?**  
  `backend/.../modelo/Ingrediente.java:30` (quantidade ≤ quantidade de alerta de cada item)
- **Onde é montada a mensagem do alerta ("O estoque de X está baixo! Disponível: Y")?**  
  `backend/.../servico/IngredienteServico.java:77`
- **Onde a tela mostra o alerta e toca a sirene?**  
  `frontend/js/app.js:927` (a cada 20 s) e a sirene em `frontend/js/app.js:864`
- **Onde a senha é criptografada?**  
  `backend/.../seguranca/Senha.java:32` (PBKDF2-SHA256, 210.000 iterações, salt)
- **Onde o login é conferido em toda requisição?**  
  `backend/.../seguranca/AutenticacaoInterceptor.java:27`
- **Onde o token de login é criado e quanto tempo vale?**  
  `backend/.../seguranca/Sessoes.java:40` (12 horas, em `application.properties`)
- **Onde fica a configuração do banco (endereço, usuário, senha)?**  
  `backend/src/main/resources/application.properties:10`
- **Onde é gerado o id (UUID) de cada registro?**  
  No banco: `DEFAULT gen_random_uuid()` em `banco/02_tabelas.sql:31`; o DAO recebe o id gerado com `RETURNING id` (ex.: `backend/.../dao/ProdutoDAO.java:71`)
- **O nº do pedido (#0012) é a chave da venda?**  
  Não: a chave é o `id` (UUID). O nº do pedido é a coluna `numero` (`banco/02_tabelas.sql:118`), usada na nota e na busca por `#12` (`backend/.../dao/VendaDAO.java:63`)
- **Onde as tabelas são criadas?**  
  `banco/02_tabelas.sql:116` (e as demais no mesmo arquivo)
- **Onde o frontend tem permissão para chamar a API (CORS)?**  
  `backend/.../config/WebConfig.java:40`
- **Onde os erros viram a mensagem `{"erro": ...}`?**  
  `backend/.../excecao/TratadorDeErros.java:25`
- **Onde fica a configuração do Swagger?**  
  `backend/.../config/OpenApiConfig.java:53` e as anotações `@Operation` em cada controle
- **Onde o frontend define o endereço da API?**  
  `frontend/js/config.js:7`

## Lista completa (ordem do Swagger)

| Nº | Método | Rota | operationId | Arquivo:linha |
|---|---|---|---|---|
| 1.1 | POST | `/api/login` | `entrar` | `controle/AcessoControle.java:69` |
| 1.2 | POST | `/api/logout` | `sair` | `controle/AcessoControle.java:95` |
| 1.3 | GET | `/api/status` | `statusSistema` | `controle/AcessoControle.java:114` |
| 2.1 | GET | `/api/usuario` | `consultarUsuario` | `controle/UsuarioControle.java:59` |
| 2.2 | PUT | `/api/usuario` | `alterarUsuario` | `controle/UsuarioControle.java:75` |
| 2.3 | POST | `/api/usuario` | `cadastrarUsuario` | `controle/UsuarioControle.java:94` |
| 2.4 | DELETE | `/api/usuario` | `excluirUsuario` | `controle/UsuarioControle.java:112` |
| 3.1 | GET | `/api/ingredientes` | `listarIngredientes` | `controle/IngredienteControle.java:58` |
| 3.2 | POST | `/api/ingredientes` | `incluirIngrediente` | `controle/IngredienteControle.java:76` |
| 3.3 | PUT | `/api/ingredientes/{id}` | `alterarIngrediente` | `controle/IngredienteControle.java:91` |
| 3.4 | DELETE | `/api/ingredientes/{id}` | `excluirIngrediente` | `controle/IngredienteControle.java:108` |
| 4.1 | GET | `/api/produtos` | `listarProdutos` | `controle/ProdutoControle.java:59` |
| 4.2 | POST | `/api/produtos` | `incluirProduto` | `controle/ProdutoControle.java:76` |
| 4.3 | GET | `/api/produtos/{id}` | `consultarProduto` | `controle/ProdutoControle.java:91` |
| 4.4 | PUT | `/api/produtos/{id}` | `alterarProduto` | `controle/ProdutoControle.java:106` |
| 4.5 | DELETE | `/api/produtos/{id}` | `excluirProduto` | `controle/ProdutoControle.java:122` |
| 5.1 | GET | `/api/vendas` | `pesquisarVendas` | `controle/VendaControle.java:61` |
| 5.2 | POST | `/api/vendas` | `registrarVenda` | `controle/VendaControle.java:85` |
| 5.3 | GET | `/api/vendas/{id}` | `consultarVenda` | `controle/VendaControle.java:100` |
| 5.4 | PUT | `/api/vendas/{id}` | `editarVenda` | `controle/VendaControle.java:117` |
| 5.5 | DELETE | `/api/vendas/{id}` | `excluirVenda` | `controle/VendaControle.java:134` |
| 6.1 | GET | `/api/alertas` | `consultarAlertas` | `controle/AlertaControle.java:42` |
| 7.1 | GET | `/api/relatorios/estoque` | `relatorioEstoque` | `controle/RelatorioControle.java:48` |
| 7.2 | GET | `/api/relatorios/faltando` | `relatorioFaltando` | `controle/RelatorioControle.java:64` |
| 7.3 | GET | `/api/relatorios/vendas` | `relatorioVendas` | `controle/RelatorioControle.java:80` |
| 8.1 | POST | `/api/backup` | `fazerBackup` | `controle/BackupControle.java:50` |
| 8.2 | POST | `/api/relatorios/salvar` | `salvarRelatorios` | `controle/BackupControle.java:65` |

*Este arquivo foi gerado a partir do código: os números de linha correspondem à versão entregue.*
