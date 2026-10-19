package br.com.confeitasys.controle;

import br.com.confeitasys.dto.RelatorioEstoqueResposta;
import br.com.confeitasys.dto.RelatorioFaltandoResposta;
import br.com.confeitasys.dto.RelatorioVendasResposta;
import br.com.confeitasys.servico.RelatorioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================
 *  GRUPO 7 - RELATÓRIOS (opcionais)
 * ===================================================================
 *  7.1  GET /api/relatorios/estoque    estoque atual e valor total
 *  7.2  GET /api/relatorios/faltando   itens em falta com sugestão de compra
 *  7.3  GET /api/relatorios/vendas     vendas do dia (faturamento, custo, lucro)
 *  (salvar os relatórios em arquivo: API 8.2, em BackupControle.java)
 *
 *  Frontend: frontend/js/api.js -> API.relatorios.*
 *  Regras:   servico/RelatorioServico.java
 */
@RestController
@Tag(name = "7. Relatórios", description = "Relatórios opcionais: o usuário não é obrigado a gerar")
public class RelatorioControle {

    private final RelatorioServico relatorios;

    public RelatorioControle(RelatorioServico relatorios) {
        this.relatorios = relatorios;
    }

    /**
     * API 7.1 - RELATÓRIO DO ESTOQUE
     * Tela: Relatórios -> aba "Estoque"
     * Frontend: API.relatorios.estoque()
     */
    @GetMapping("/api/relatorios/estoque")
    @Operation(operationId = "relatorioEstoque", summary = "7.1 Relatório do estoque",
            description = "**Tela:** Relatórios → aba **Estoque**  \n"
                    + "**Frontend:** `API.relatorios.estoque()`  \n"
                    + "**Backend:** `controle/RelatorioControle.java` → `RelatorioServico.estoque()`  \n"
                    + "**Tabela:** ingrediente  \n\n"
                    + "Todos os itens e o valor total em estoque.")
    public RelatorioEstoqueResposta estoque() {
        return relatorios.estoque();
    }

    /**
     * API 7.2 - RELATÓRIO DE ITENS EM FALTA
     * Tela: Relatórios -> aba "Itens em falta"
     * Frontend: API.relatorios.faltando()
     */
    @GetMapping("/api/relatorios/faltando")
    @Operation(operationId = "relatorioFaltando", summary = "7.2 Relatório de itens em falta",
            description = "**Tela:** Relatórios → aba **Itens em falta**  \n"
                    + "**Frontend:** `API.relatorios.faltando()`  \n"
                    + "**Backend:** `controle/RelatorioControle.java` → `RelatorioServico.faltando()`  \n"
                    + "**Tabela:** ingrediente  \n\n"
                    + "Itens com estoque baixo ou vencidos, com sugestão de compra e custo estimado.")
    public RelatorioFaltandoResposta faltando() {
        return relatorios.faltando();
    }

    /**
     * API 7.3 - RELATÓRIO DE VENDAS DO DIA
     * Tela: Relatórios -> aba "Vendas do dia"
     * Frontend: API.relatorios.vendas(data)
     */
    @GetMapping("/api/relatorios/vendas")
    @Operation(operationId = "relatorioVendas", summary = "7.3 Relatório de vendas do dia",
            description = "**Tela:** Relatórios → aba **Vendas do dia**  \n"
                    + "**Frontend:** `API.relatorios.vendas()`  \n"
                    + "**Backend:** `controle/RelatorioControle.java` → `RelatorioServico.vendasDoDia()`  \n"
                    + "**Tabelas:** venda, item_venda  \n\n"
                    + "Vendas do dia, faturamento, custo, lucro e total de cada ingrediente usado.")
    public RelatorioVendasResposta vendas(
            @Parameter(description = "Data AAAA-MM-DD (vazio = hoje)", example = "2026-10-16")
            @RequestParam(name = "data", required = false) String data) {
        return relatorios.vendasDoDia(data);
    }
}
