package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dados de um produto do cardápio e sua receita (Cadastro 3)")
public record ProdutoRequisicao(
        @Schema(description = "Nome do produto", example = "Bolo de chocolate") String nome,
        @Schema(description = "Tamanho (opcional)", example = "G") String tamanho,
        @Schema(description = "Recheio (opcional)", example = "Brigadeiro") String recheio,
        @Schema(description = "Preço de venda (R$)", example = "95.00") Double precoVenda,
        @Schema(description = "Ingredientes da receita (pelo menos um)") List<ItemQuantidadeRequisicao> receita) {
}
