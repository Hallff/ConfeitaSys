package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Produto do cardápio com a receita e o custo estimado")
public record ProdutoResposta(
        @Schema(description = "Id (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "Bolo de chocolate") String nome,
        @Schema(example = "G") String tamanho,
        @Schema(example = "Brigadeiro") String recheio,
        @Schema(example = "Bolo de chocolate G - recheio Brigadeiro") String nomeCompleto,
        @Schema(example = "95.0") double precoVenda,
        @Schema(description = "Soma do custo dos ingredientes da receita", example = "26.54") double custoEstimado,
        @Schema(example = "68.46") double lucroEstimado,
        List<ItemResposta> receita) {
}
