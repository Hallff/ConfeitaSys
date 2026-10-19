package br.com.confeitasys.controle;

import br.com.confeitasys.dto.AlertasResposta;
import br.com.confeitasys.servico.IngredienteServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================
 *  GRUPO 6 - ALERTAS (estoque baixo e validade)
 * ===================================================================
 *  6.1  GET /api/alertas   itens com estoque baixo e vencidos/vencendo
 *
 *  Frontend: frontend/js/api.js -> API.alertas.consultar()   (chamado a cada 20 s em qualquer tela)
 *  Regras:   servico/IngredienteServico.java (método alertas)    Tabela: ingrediente
 */
@RestController
@Tag(name = "6. Alertas", description = "Estoque baixo (com sirene) e validade, exibidos em qualquer tela")
public class AlertaControle {

    private final IngredienteServico ingredientes;

    public AlertaControle(IngredienteServico ingredientes) {
        this.ingredientes = ingredientes;
    }

    /**
     * API 6.1 - ALERTAS
     * Tela: todas (faixa vermelha no topo + sirene), consultado a cada 20 s e após cada alteração.
     * Frontend: API.alertas.consultar()
     */
    @GetMapping("/api/alertas")
    @Operation(operationId = "consultarAlertas", summary = "6.1 Alertas de estoque baixo e validade",
            description = "**Tela:** todas as telas (faixa vermelha no topo + **sirene**), a cada 20 s e após cada alteração  \n"
                    + "**Frontend:** `API.alertas.consultar()`  \n"
                    + "**Backend:** `controle/AlertaControle.java` → `IngredienteServico.alertas()` → `IngredienteDAO.listar()`  \n"
                    + "**Tabela:** ingrediente  \n\n"
                    + "`estoqueBaixo`: quantidade ≤ quantidade de alerta do item, com a mensagem "
                    + "\"O estoque de X está baixo! Disponível: Y\". `validade`: itens vencidos ou que vencem em até 3 dias.")
    public AlertasResposta alertas() {
        return ingredientes.alertas();
    }
}
