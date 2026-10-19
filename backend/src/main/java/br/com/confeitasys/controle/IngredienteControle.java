package br.com.confeitasys.controle;

import br.com.confeitasys.dto.IngredienteRequisicao;
import br.com.confeitasys.dto.IngredienteResposta;
import br.com.confeitasys.dto.OkResposta;
import br.com.confeitasys.modelo.Ingrediente;
import br.com.confeitasys.servico.IngredienteServico;
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
 *  GRUPO 3 - CADASTRO 2: ESTOQUE (ingredientes)
 * ===================================================================
 *  3.1  GET    /api/ingredientes        listar o estoque
 *  3.2  POST   /api/ingredientes        incluir ingrediente
 *  3.3  PUT    /api/ingredientes/{id}   alterar ingrediente
 *  3.4  DELETE /api/ingredientes/{id}   excluir ingrediente
 *
 *  Frontend: frontend/js/api.js -> API.estoque.*
 *  Regras:   servico/IngredienteServico.java    SQL: dao/IngredienteDAO.java    Tabela: ingrediente
 */
@RestController
@Tag(name = "3. Cadastro 2 - Estoque", description = "Ingredientes do estoque com quantidade de alerta (individual) e validade")
public class IngredienteControle {

    private final IngredienteServico ingredientes;

    public IngredienteControle(IngredienteServico ingredientes) {
        this.ingredientes = ingredientes;
    }

    /**
     * API 3.1 - LISTAR ESTOQUE
     * Tela: Estoque (ao abrir); também carrega a lista de ingredientes nas janelas de produto e de venda.
     * Frontend: API.estoque.listar()
     */
    @GetMapping("/api/ingredientes")
    @Operation(operationId = "listarIngredientes", summary = "3.1 Listar o estoque",
            description = "**Tela:** Estoque (ao abrir) e janelas de Produto e Nova venda (lista de ingredientes)  \n"
                    + "**Frontend:** `API.estoque.listar()`  \n"
                    + "**Backend:** `controle/IngredienteControle.java` → `IngredienteServico.listar()` → `IngredienteDAO.listar()`  \n"
                    + "**Tabela:** ingrediente  \n\n"
                    + "Todos os ingredientes em ordem alfabética, com estoque baixo, validade e valor em estoque calculados.")
    public List<IngredienteResposta> listar() {
        return ingredientes.listar().stream().map(Ingrediente::paraResposta).toList();
    }

    /**
     * API 3.2 - INCLUIR INGREDIENTE
     * Tela: Estoque -> "+ Novo item no estoque" -> "Salvar"
     * Frontend: API.estoque.incluir(dados)
     */
    @PostMapping("/api/ingredientes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "incluirIngrediente", summary = "3.2 Incluir ingrediente",
            description = "**Tela:** Estoque → **+ Novo item no estoque** → **Salvar**  \n"
                    + "**Frontend:** `API.estoque.incluir()`  \n"
                    + "**Backend:** `controle/IngredienteControle.java` → `IngredienteServico.incluir()` → `IngredienteDAO.inserir()`  \n"
                    + "**Tabela:** ingrediente  \n\n"
                    + "Unidade KG ou L. O custo por KG/L é calculado (valorPago ÷ quantidade). "
                    + "A **quantidadeAlerta** define quando aparece o alerta de estoque baixo. O nome não pode repetir.")
    public IngredienteResposta incluir(@RequestBody IngredienteRequisicao d) {
        return ingredientes.incluir(d).paraResposta();
    }

    /**
     * API 3.3 - ALTERAR INGREDIENTE
     * Tela: Estoque -> lápis (Editar) -> "Salvar"
     * Frontend: API.estoque.alterar(id, dados)
     */
    @PutMapping("/api/ingredientes/{id}")
    @Operation(operationId = "alterarIngrediente", summary = "3.3 Alterar ingrediente",
            description = "**Tela:** Estoque → lápis (**Editar**) → **Salvar**  \n"
                    + "**Frontend:** `API.estoque.alterar()`  \n"
                    + "**Backend:** `controle/IngredienteControle.java` → `IngredienteServico.alterar()` → `IngredienteDAO.alterar()`  \n"
                    + "**Tabela:** ingrediente")
    public IngredienteResposta alterar(@Parameter(description = "Id (UUID) do ingrediente (3.1) - copie da lista") @PathVariable("id") UUID id,
                                       @RequestBody IngredienteRequisicao d) {
        return ingredientes.alterar(id, d).paraResposta();
    }

    /**
     * API 3.4 - EXCLUIR INGREDIENTE
     * Tela: Estoque -> lixeira (Excluir) -> "Excluir item"
     * Frontend: API.estoque.excluir(id)
     */
    @DeleteMapping("/api/ingredientes/{id}")
    @Operation(operationId = "excluirIngrediente", summary = "3.4 Excluir ingrediente",
            description = "**Tela:** Estoque → lixeira → **Excluir item**  \n"
                    + "**Frontend:** `API.estoque.excluir()`  \n"
                    + "**Backend:** `controle/IngredienteControle.java` → `IngredienteServico.excluir()` → `IngredienteDAO.excluir()`  \n"
                    + "**Tabelas:** ingrediente (confere produto_ingrediente)  \n\n"
                    + "Não é permitido excluir um ingrediente que faz parte da receita de algum produto.")
    public OkResposta excluir(@Parameter(description = "Id (UUID) do ingrediente (3.1) - copie da lista") @PathVariable("id") UUID id) {
        ingredientes.excluir(id);
        return OkResposta.OK;
    }
}
