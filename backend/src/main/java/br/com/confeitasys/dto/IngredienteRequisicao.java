package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de um ingrediente do estoque (Cadastro 2)")
public record IngredienteRequisicao(
        @Schema(description = "Nome do ingrediente (único)", example = "Farinha de trigo") String nome,
        @Schema(description = "Unidade: KG (quilogramas) ou L (litros)", example = "KG", allowableValues = {"KG", "L"}) String unidade,
        @Schema(description = "Quantidade em estoque, em KG ou L", example = "5") Double quantidade,
        @Schema(description = "Valor pago por essa quantidade (R$). O custo por KG/L é calculado.", example = "27.50") Double valorPago,
        @Schema(description = "Quantidade de alerta: com estoque igual ou abaixo disso o sistema avisa (com sirene)", example = "1.5") Double quantidadeAlerta,
        @Schema(description = "Data de validade AAAA-MM-DD (opcional)", example = "2026-12-31") String validade) {
}
