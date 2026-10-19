package br.com.confeitasys.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI - documentação e teste das APIs.
 * Tela: http://localhost:8080/swagger-ui.html
 *
 * Cada rota tem um NÚMERO (ex.: 5.2 Registrar venda) que é o mesmo no Swagger,
 * no código (comentário "API 5.2" no controle), no frontend (frontend/js/api.js)
 * e no arquivo MAPA-DAS-APIS.md.
 */
@Configuration
public class OpenApiConfig {

    public static final String SEGURANCA = "token";

    private static final String DESCRICAO = """
            API REST do **ConfeitaSys** — controle de vendas e estoque da confeitaria.

            **Arquitetura:** Frontend (HTML/JS) → **esta API** (Java 17 + Spring Boot) → Banco **PostgreSQL**.
            Cada rota tem um número (ex.: **5.2**) que é o mesmo no código (`/** API 5.2 */`),
            no frontend (`frontend/js/api.js`) e no arquivo `MAPA-DAS-APIS.md`.

            | Grupo | O que faz | Arquivo da API (backend/src/main/java/br/com/confeitasys/) |
            |---|---|---|
            | 1. Acesso | entrar, sair, situação do sistema | `controle/AcessoControle.java` |
            | 2. Cadastro 1 - Usuário | incluir, consultar, alterar, excluir | `controle/UsuarioControle.java` |
            | 3. Cadastro 2 - Estoque | ingredientes, quantidade de alerta, validade | `controle/IngredienteControle.java` |
            | 4. Cadastro 3 - Produtos | cardápio e receitas | `controle/ProdutoControle.java` |
            | 5. Vendas | registrar (com baixa no estoque), pesquisar, editar, excluir | `controle/VendaControle.java` |
            | 6. Alertas | estoque baixo (sirene) e validade | `controle/AlertaControle.java` |
            | 7. Relatórios | estoque, itens em falta, vendas do dia | `controle/RelatorioControle.java` |
            | 8. Backup | cópia de segurança e relatórios em arquivo | `controle/BackupControle.java` |

            **Como testar aqui**
            1. Primeiro acesso: **2.3** `POST /api/usuario` com nome e senha. Se o usuário já existe: **1.1** `POST /api/login`.
            2. Copie o `token` da resposta, clique em **Authorize** (cadeado) e cole.
            3. Pronto: as outras rotas funcionam (o token vale 12 horas).

            **Ids:** todas as chaves são **UUID** (ex.: `3fa85f64-5717-4562-b3fc-2c963f66afa6`), geradas pelo banco.
            Para usar uma rota com `{id}`, copie o `id` de uma lista (3.1, 4.1 ou 5.1).
            O nº do pedido da nota (#0012) é o campo `numero` da venda.

            Erros voltam como `{"erro": "mensagem"}`: 400 regra de negócio, 401 sem login, 429 login bloqueado,
            503 banco de dados desligado.
            """;

    @Bean
    public OpenAPI confeitaSysOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConfeitaSys API")
                        .version("1.0.0")
                        .description(DESCRICAO)
                        .contact(new Contact().name("Gabriel Furlan de Souza e Hallef Felipe Ribeiro")))
                .components(new Components().addSecuritySchemes(SEGURANCA, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .description("Token recebido em 1.1 POST /api/login ou 2.3 POST /api/usuario")))
                .addSecurityItem(new SecurityRequirement().addList(SEGURANCA));
    }
}
