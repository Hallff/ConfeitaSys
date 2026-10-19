package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Venda (pedido) com custo e lucro")
public record VendaResposta(
        @Schema(description = "Id da venda (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(description = "Número do pedido (vai na nota do cliente)", example = "12") int numero,
        @Schema(example = "2026-10-15T14:30:00") String dataHora,
        @Schema(example = "2026-10-15") String data,
        @Schema(example = "14:30") String hora,
        @Schema(nullable = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID produtoId,
        @Schema(description = "Nome do produto no momento da venda", example = "Bolo de chocolate G - recheio Brigadeiro") String produto,
        @Schema(example = "95.0") double valorVenda,
        @Schema(example = "26.54") double custo,
        @Schema(example = "68.46") double lucro,
        @Schema(description = "Ingredientes baixados do estoque") List<ItemResposta> itens) {
}
