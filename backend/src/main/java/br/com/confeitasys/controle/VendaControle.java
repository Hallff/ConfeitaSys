package br.com.confeitasys.controle;

import br.com.confeitasys.dto.OkResposta;
import br.com.confeitasys.dto.VendaRequisicao;
import br.com.confeitasys.dto.VendaResposta;
import br.com.confeitasys.modelo.Venda;
import br.com.confeitasys.servico.VendaServico;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * ===================================================================
 *  GRUPO 5 - VENDAS (movimento)
 * ===================================================================
 *  5.1  GET    /api/vendas          pesquisar vendas (data, produto, nº do pedido, hora)
 *  5.2  POST   /api/vendas          REGISTRAR VENDA (com baixa no estoque)
 *  5.3  GET    /api/vendas/{id}     consultar venda
 *  5.4  PUT    /api/vendas/{id}     editar venda
 *  5.5  DELETE /api/vendas/{id}     excluir venda / devolução
 *
 *  Frontend: frontend/js/api.js -> API.vendas.*
 *  Regras:   servico/VendaServico.java
 *  SQL:      dao/VendaDAO.java + dao/IngredienteDAO.java (baixa)     Tabelas: venda, item_venda, ingrediente
 */
@RestController
@Tag(name = "5. Vendas", description = "Registrar, pesquisar, editar e excluir vendas. Cada venda dá baixa automática nos ingredientes do estoque.")
public class VendaControle {

    private final VendaServico vendas;

    public VendaControle(VendaServico vendas) {
        this.vendas = vendas;
    }

    /**
     * API 5.1 - PESQUISAR VENDAS
     * Tela: Vendas (lista do dia escolhido) e campo de busca no canto superior direito.
     * Frontend: API.vendas.pesquisar(data, busca)
     */
    @GetMapping("/api/vendas")
    @Operation(operationId = "pesquisarVendas", summary = "5.1 Pesquisar vendas",
            description = "**Tela:** Vendas (lista do dia) e **busca** no canto superior direito  \n"
                    + "**Frontend:** `API.vendas.pesquisar()`  \n"
                    + "**Backend:** `controle/VendaControle.java` → `VendaServico.pesquisar()` → `VendaDAO.listar()`  \n"
                    + "**Tabelas:** venda, item_venda  \n\n"
                    + "A busca aceita nome do produto, número do pedido (`12` ou `#12`) ou hora (`14h`, `14:30`).")
    public List<VendaResposta> pesquisar(
            @Parameter(description = "Data AAAA-MM-DD (vazio = todas as datas)", example = "2026-10-16")
            @RequestParam(name = "data", required = false) String data,
            @Parameter(description = "Produto, nº do pedido ou hora", example = "bolo")
            @RequestParam(name = "busca", required = false) String busca) {
        return vendas.pesquisar(data, busca).stream().map(Venda::paraResposta).toList();
    }

    /**
     * API 5.2 - REGISTRAR VENDA
     * Tela: Vendas -> botão "+" no centro do topo (ou tecla N) -> "Salvar venda"
     * Frontend: API.vendas.registrar(dados)
     */
    @PostMapping("/api/vendas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "registrarVenda", summary = "5.2 Registrar venda (com baixa no estoque)",
            description = "**Tela:** Vendas → botão **+** (ou tecla N) → **Salvar venda**  \n"
                    + "**Frontend:** `API.vendas.registrar()`  \n"
                    + "**Backend:** `controle/VendaControle.java` → `VendaServico.registrar()` → "
                    + "`VendaDAO.inserir()` + `IngredienteDAO.movimentar()`  \n"
                    + "**Tabelas:** venda, item_venda, ingrediente (baixa)  \n\n"
                    + "Grava a venda com data/hora, id (UUID) e nº do pedido, calcula custo e lucro e dá baixa nos ingredientes, "
                    + "**tudo numa única transação**. Se faltar estoque de algum ingrediente, nada é gravado. "
                    + "Se `itens` vier vazio, usa a receita do produto.")
    public VendaResposta registrar(@RequestBody VendaRequisicao d) {
        return vendas.registrar(d).paraResposta();
    }

    /**
     * API 5.3 - CONSULTAR VENDA
     * Tela: Vendas -> lápis do pedido -> "Editar" (carrega o pedido na janela)
     * Frontend: API.vendas.consultar(id)
     */
    @GetMapping("/api/vendas/{id}")
    @Operation(operationId = "consultarVenda", summary = "5.3 Consultar venda pelo id",
            description = "**Tela:** Vendas → lápis → **Editar** (carrega o pedido)  \n"
                    + "**Frontend:** `API.vendas.consultar()`  \n"
                    + "**Backend:** `controle/VendaControle.java` → `VendaServico.consultar()` → `VendaDAO.buscar()`  \n"
                    + "**Tabelas:** venda, item_venda")
    public VendaResposta consultar(@Parameter(description = "Id (UUID) da venda - copie da pesquisa (5.1)") @PathVariable("id") UUID id) {
        return vendas.consultar(id).paraResposta();
    }

    /**
     * API 5.4 - EDITAR VENDA
     * Tela: Vendas -> lápis -> "Editar" -> "Salvar venda"
     * Frontend: API.vendas.editar(id, dados)
     */
    @PutMapping("/api/vendas/{id}")
    @Operation(operationId = "editarVenda", summary = "5.4 Editar venda",
            description = "**Tela:** Vendas → lápis → **Editar** → **Salvar venda**  \n"
                    + "**Frontend:** `API.vendas.editar()`  \n"
                    + "**Backend:** `controle/VendaControle.java` → `VendaServico.editar()` → "
                    + "`VendaDAO.alterar()` + `IngredienteDAO.movimentar()`  \n"
                    + "**Tabelas:** venda, item_venda, ingrediente  \n\n"
                    + "Devolve ao estoque o que a venda tinha usado e desconta o novo.")
    public VendaResposta editar(@Parameter(description = "Id (UUID) da venda - copie da pesquisa (5.1)") @PathVariable("id") UUID id,
                                @RequestBody VendaRequisicao d) {
        return vendas.editar(id, d).paraResposta();
    }

    /**
     * API 5.5 - EXCLUIR VENDA / DEVOLUÇÃO
     * Tela: Vendas -> lápis -> "Excluir" -> "Excluir venda" (com a opção de devolver ao estoque)
     * Frontend: API.vendas.excluir(id, devolver)
     */
    @DeleteMapping("/api/vendas/{id}")
    @Operation(operationId = "excluirVenda", summary = "5.5 Excluir venda / devolução",
            description = "**Tela:** Vendas → lápis → **Excluir** → **Excluir venda**  \n"
                    + "**Frontend:** `API.vendas.excluir()`  \n"
                    + "**Backend:** `controle/VendaControle.java` → `VendaServico.excluir()` → "
                    + "`VendaDAO.excluir()` (+ `IngredienteDAO.movimentar()` se devolver)  \n"
                    + "**Tabelas:** venda, item_venda, ingrediente")
    public OkResposta excluir(
            @Parameter(description = "Id (UUID) da venda - copie da pesquisa (5.1)") @PathVariable("id") UUID id,
            @Parameter(description = "Devolver os ingredientes ao estoque? (sim | nao)", example = "sim")
            @RequestParam(name = "devolver", required = false, defaultValue = "sim") String devolver) {
        vendas.excluir(id, !"nao".equalsIgnoreCase(devolver));
        return OkResposta.OK;
    }
}
