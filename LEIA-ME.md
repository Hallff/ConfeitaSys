# ConfeitaSys — Controle de vendas e estoque da padaria

Projeto de Estágio Supervisionado I — Gabriel Furlan de Souza e Hallef Felipe Ribeiro.
Projeto completo: **frontend** (HTML/CSS/JS) + **backend** (Java 17, API REST) + **banco de dados** (SQLite).

## 1. Como rodar (Windows 8 a 11)

1. Instale o **Java 17 ou mais novo**: https://adoptium.net/ → *Temurin JDK* para Windows (.msi). Na instalação marque **"Set JAVA_HOME"** e **"Add to PATH"**.
2. Dê dois cliques em **`iniciar.bat`** (a pasta `lib` precisa ficar ao lado do `ConfeitaSys.jar`).
3. O navegador abre em `http://localhost:8080`. **Primeiro acesso:** crie o usuário e a senha.
4. Para encerrar, feche a janela preta do `iniciar.bat`.

## 2. Os 3 cadastros (incluir, consultar, alterar, excluir)

| Cadastro | Onde | Campos |
|---|---|---|
| **1 – Usuário** | Login (primeiro acesso) e botão **Conta** | nome, senha (criptografada). Alterar exige a senha atual; excluir pede a senha. |
| **2 – Ingredientes (Estoque)** | Botão do **armário** | nome, **kg ou litros**, quantidade, valor pago (R$), **quantidade de alerta** (individual por item), **data de validade** |
| **3 – Produtos (Cardápio)** | Botão **Produtos** | nome, tamanho, recheio, preço de venda e **receita** (ingredientes em g/ml) |

Movimento: **Vendas** — botão **+** no centro do topo; busca no canto superior direito (nome, nº do pedido ou hora); seta "Ingredientes" mostra ingredientes, quantidade e valor; lápis → Editar/Excluir. Cada venda desconta os ingredientes do estoque.

## 3. Alerta de estoque baixo (em qualquer tela)

Quando um ingrediente chega na **quantidade de alerta** definida para ele, aparece no topo de qualquer tela a mensagem
**"O estoque de X está baixo! Disponível: Y (alerta em Z)"** e toca uma **sirene de emergência**.
Itens vencidos ou que vencem em até 3 dias também aparecem no alerta. O sistema confere a cada 20 segundos e logo após cada alteração.
"Entendi" silencia aquele alerta; ele volta se a quantidade baixar de novo.
> Os navegadores só liberam som depois do primeiro clique na página; se o alerta aparecer antes disso, a sirene toca no primeiro clique.

## 4. Relatórios (opcionais)
Vendas do dia, estoque e itens em falta. Podem ser impressos ou salvos (com backup). O usuário não é obrigado a gerar.

## 5. Tecnologias

| Item | Escolha |
|---|---|
| Frontend | HTML5, CSS3, JavaScript |
| Backend / API | Java 17 — API REST própria (`com.sun.net.httpserver`), JSON |
| Banco de dados | **SQLite 3** — arquivo `dados/confeitasys.db`, driver JDBC `lib/sqlite-jdbc-3.46.1.3.jar` |
| Senha | PBKDF2WithHmacSHA256, 210.000 iterações, salt de 16 bytes |
| Sessão | Cookie HttpOnly/SameSite, expira em 12 h; bloqueio de 60 s após 5 senhas erradas |
| Integridade | Chaves estrangeiras; transações (venda + baixa no estoque juntas); ingrediente usado em receita não pode ser excluído |
| Backup | `VACUUM INTO` → pasta `backup/` |

Para ver o banco: programa gratuito **DB Browser for SQLite** (sqlitebrowser.org) → abrir `dados/confeitasys.db`.

## 6. Organização do código (MVC em camadas)

```
src/
├── db/schema.sql                 ← script do banco (criado automaticamente)
├── web/                          ← FRONTEND: login.html, app.html, app.js, estilo.css
└── confeitasys/
    ├── Main.java                 ← inicia tudo
    ├── controle/Servidor.java    ← API REST (rotas, login, sessão)
    ├── servico/                  ← REGRAS: Usuario, Ingrediente, Produto, Venda, Relatorio
    ├── dao/                      ← SQL: Conexao, UsuarioDAO, IngredienteDAO, ProdutoDAO, VendaDAO
    ├── modelo/                   ← Usuario, Ingrediente, Produto, ItemReceita, Venda, ItemVenda
    └── util/                     ← Senha (criptografia), Json, Util, RegraException
docs/
├── ConfeitaSys - Diagramas, Telas e Banco.docx   ← todos os diagramas + telas + dicionário de dados
├── diagramas/png  e  diagramas/fonte (.puml)
├── telas/                        ← prints das telas e relatórios
└── script-do-banco.sql
```
Recompilar depois de alterar o código: `compilar.bat`.

## 7. Rotas da API

| Método | Rota | Função |
|---|---|---|
| GET | `/api/status` | existe usuário? está logado? |
| POST | `/api/usuario` | cadastrar usuário (primeiro acesso) |
| GET · PUT · DELETE | `/api/usuario` | consultar / alterar / excluir usuário |
| POST | `/api/login` · `/api/logout` | entrar / sair |
| GET · POST | `/api/ingredientes` | listar / incluir ingrediente |
| PUT · DELETE | `/api/ingredientes/{id}` | alterar / excluir ingrediente |
| GET · POST | `/api/produtos` | listar / incluir produto |
| GET · PUT · DELETE | `/api/produtos/{id}` | consultar / alterar / excluir produto |
| GET | `/api/vendas?data=&busca=` | pesquisar vendas |
| POST | `/api/vendas` | registrar venda (baixa no estoque) |
| GET · PUT | `/api/vendas/{id}` | consultar / editar venda |
| DELETE | `/api/vendas/{id}?devolver=sim\|nao` | excluir venda / devolução |
| GET | `/api/alertas` | alertas de estoque baixo e validade |
| GET | `/api/relatorios/vendas?data=` · `/estoque` · `/faltando` | relatórios |
| POST | `/api/relatorios/salvar` · `/api/backup` | salvar relatórios / backup |

**Esqueci a senha:** feche o sistema, abra `dados/confeitasys.db` no DB Browser for SQLite e apague a linha da tabela `usuario`. Ao abrir de novo, o sistema pede um novo cadastro (vendas, produtos e estoque são mantidos).
