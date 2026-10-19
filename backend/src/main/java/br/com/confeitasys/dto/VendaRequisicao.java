package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Registro ou edição de venda. Se 'itens' vier vazio, usa a receita do produto.")
public record VendaRequisicao(
        @Schema(description = "Id (UUID) do produto vendido", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID produtoId,
        @Schema(description = "Valor cobrado (R$)", example = "95.00") Double valorVenda,
        @Schema(description = "Ingredientes usados (opcional; padrão = receita do produto)") List<ItemQuantidadeRequisicao> itens) {
}
