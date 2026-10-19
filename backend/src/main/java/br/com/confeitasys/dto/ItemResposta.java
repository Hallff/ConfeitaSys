package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Ingrediente de uma receita ou de uma venda")
public record ItemResposta(
        @Schema(description = "Id do ingrediente (nulo se ele foi excluído depois)", nullable = true, example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID ingredienteId,
        @Schema(example = "Farinha de trigo") String nome,
        @Schema(example = "KG") String unidade,
        @Schema(description = "Quantidade em KG ou L", example = "0.5") double quantidade,
        @Schema(description = "Custo (R$)", example = "2.75") double custo) {
}
