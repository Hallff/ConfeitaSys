package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Ingrediente e quantidade usada (em KG ou L)")
public record ItemQuantidadeRequisicao(
        @Schema(description = "Id (UUID) do ingrediente no estoque", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID ingredienteId,
        @Schema(description = "Quantidade em KG ou L (ex.: 0.25 = 250 g)", example = "0.5") Double quantidade) {
}
