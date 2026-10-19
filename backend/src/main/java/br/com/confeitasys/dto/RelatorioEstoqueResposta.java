package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Relatório do estoque atual")
public record RelatorioEstoqueResposta(
        String geradoEm,
        List<IngredienteResposta> itens,
        @Schema(description = "Valor total do estoque (R$)") double valorTotal) {
}
