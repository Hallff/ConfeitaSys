package br.com.confeitasys.controle;

import br.com.confeitasys.dto.OkResposta;
import br.com.confeitasys.dto.ProdutoRequisicao;
import br.com.confeitasys.dto.ProdutoResposta;
import br.com.confeitasys.modelo.Produto;
import br.com.confeitasys.servico.ProdutoServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ===================================================================
 *  GRUPO 4 - CADASTRO 3: PRODUTOS (cardápio e receitas)
 * ===================================================================
 *  4.1  GET    /api/produtos        listar o cardápio
 *  4.2  POST   /api/produtos        incluir produto com receita
 *  4.3  GET    /api/produtos/{id}   consultar produto
 *  4.4  PUT    /api/produtos/{id}   alterar produto e receita
 *  4.5  DELETE /api/produtos/{id}   excluir produto
 *
 *  Frontend: frontend/js/api.js -> API.produtos.*
 *  Regras:   servico/ProdutoServico.java    SQL: dao/ProdutoDAO.java    Tabelas: produto, produto_ingrediente
 */
@RestController
@Tag(name = "4. Cadastro 3 - Produtos", description = "Produtos do cardápio e suas receitas (ingredientes e quantidades)")
public class ProdutoControle {

    private final ProdutoServico produtos;

    public ProdutoControle(ProdutoServico produtos) {
        this.produtos = produtos;
    }

    /**
     * API 4.1 - LISTAR CARDÁPIO
     * Tela: Produtos (ao abrir); também carrega a lista de produtos na janela de Nova venda.
     * Frontend: API.produtos.listar()
     */
    @GetMapping("/api/produtos")
    @Operation(operationId = "listarProdutos", summary = "4.1 Listar o cardápio",
            description = "**Tela:** Produtos (ao abrir) e janela Nova venda (lista de produtos)  \n"
                    + "**Frontend:** `API.produtos.listar()`  \n"
                    + "**Backend:** `controle/ProdutoControle.java` → `ProdutoServico.listar()` → `ProdutoDAO.listar()`  \n"
                    + "**Tabelas:** produto, produto_ingrediente, ingrediente  \n\n"
                    + "Produtos com a receita, o custo estimado e o lucro estimado.")
    public List<ProdutoResposta> listar() {
        return produtos.listar().stream().map(Produto::paraResposta).toList();
    }

    /**
     * API 4.2 - INCLUIR PRODUTO
     * Tela: Produtos -> "+ Novo produto" -> "Salvar produto"
     * Frontend: API.produtos.incluir(dados)
     */
    @PostMapping("/api/produtos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "incluirProduto", summary = "4.2 Incluir produto com receita",
            description = "**Tela:** Produtos → **+ Novo produto** → **Salvar produto**  \n"
                    + "**Frontend:** `API.produtos.incluir()`  \n"
                    + "**Backend:** `controle/ProdutoControle.java` → `ProdutoServico.incluir()` → `ProdutoDAO.inserir()`  \n"
                    + "**Tabelas:** produto, produto_ingrediente  \n\n"
                    + "Nome + tamanho + recheio não podem repetir. A receita precisa de pelo menos um ingrediente (quantidades em KG/L).")
    public ProdutoResposta incluir(@RequestBody ProdutoRequisicao d) {
        return produtos.incluir(d).paraResposta();
    }

    /**
     * API 4.3 - CONSULTAR PRODUTO
     * Tela: Produtos -> lápis -> "Editar" (carrega os dados na janela)
     * Frontend: API.produtos.consultar(id)
     */
    @GetMapping("/api/produtos/{id}")
    @Operation(operationId = "consultarProduto", summary = "4.3 Consultar produto",
            description = "**Tela:** Produtos → lápis → **Editar** (carrega a janela)  \n"
                    + "**Frontend:** `API.produtos.consultar()`  \n"
                    + "**Backend:** `controle/ProdutoControle.java` → `ProdutoServico.consultar()` → `ProdutoDAO.buscar()`  \n"
                    + "**Tabelas:** produto, produto_ingrediente, ingrediente")
    public ProdutoResposta consultar(@Parameter(description = "Id (UUID) do produto (4.1) - copie da lista") @PathVariable("id") UUID id) {
        return produtos.consultar(id).paraResposta();
    }

    /**
     * API 4.4 - ALTERAR PRODUTO
     * Tela: Produtos -> lápis -> "Editar" -> "Salvar produto"
     * Frontend: API.produtos.alterar(id, dados)
     */
    @PutMapping("/api/produtos/{id}")
    @Operation(operationId = "alterarProduto", summary = "4.4 Alterar produto e receita",
            description = "**Tela:** Produtos → lápis → **Editar** → **Salvar produto**  \n"
                    + "**Frontend:** `API.produtos.alterar()`  \n"
                    + "**Backend:** `controle/ProdutoControle.java` → `ProdutoServico.alterar()` → `ProdutoDAO.alterar()`  \n"
                    + "**Tabelas:** produto, produto_ingrediente (a receita é regravada)")
    public ProdutoResposta alterar(@Parameter(description = "Id (UUID) do produto (4.1) - copie da lista") @PathVariable("id") UUID id,
                                   @RequestBody ProdutoRequisicao d) {
        return produtos.alterar(id, d).paraResposta();
    }

    /**
     * API 4.5 - EXCLUIR PRODUTO
     * Tela: Produtos -> lápis -> "Excluir" -> "Excluir produto"
     * Frontend: API.produtos.excluir(id)
     */
    @DeleteMapping("/api/produtos/{id}")
    @Operation(operationId = "excluirProduto", summary = "4.5 Excluir produto",
            description = "**Tela:** Produtos → lápis → **Excluir** → **Excluir produto**  \n"
                    + "**Frontend:** `API.produtos.excluir()`  \n"
                    + "**Backend:** `controle/ProdutoControle.java` → `ProdutoServico.excluir()` → `ProdutoDAO.excluir()`  \n"
                    + "**Tabelas:** produto (a receita sai junto; as vendas antigas guardam o nome do produto)")
    public OkResposta excluir(@Parameter(description = "Id (UUID) do produto (4.1) - copie da lista") @PathVariable("id") UUID id) {
        produtos.excluir(id);
        return OkResposta.OK;
    }
}
