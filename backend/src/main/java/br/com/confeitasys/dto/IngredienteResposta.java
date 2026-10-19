package br.com.confeitasys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Ingrediente do estoque com os indicadores calculados (valor em estoque,
 * estoque baixo, validade). Nos alertas e no relatório de itens em falta
 * vêm também os campos extras (mensagem, sugestão de compra).
 */
@Schema(description = "Ingrediente do estoque com indicadores calculados")
public class IngredienteResposta {

    @Schema(description = "Id (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") public UUID id;
    @Schema(example = "Farinha de trigo") public String nome;
    @Schema(example = "KG", allowableValues = {"KG", "L"}) public String unidade;
    @Schema(description = "Quantidade em estoque (KG ou L)", example = "6.9") public double quantidade;
    @Schema(description = "Custo por KG ou L (R$)", example = "5.5") public double custoUnitario;
    @Schema(description = "Quantidade de alerta definida para este item", example = "2.0") public double quantidadeAlerta;
    @Schema(description = "Validade AAAA-MM-DD", nullable = true, example = "2026-12-31") public String validade;
    @Schema(example = "2026-10-15T14:30:00") public String atualizadoEm;
    @Schema(description = "quantidade x custoUnitario (R$)", example = "37.95") public double valorEmEstoque;
    @Schema(description = "quantidade <= quantidadeAlerta") public boolean estoqueBaixo;
    @Schema(description = "Dias até vencer (negativo = vencido)", nullable = true, example = "77") public Long diasParaVencer;
    public boolean vencido;
    @Schema(description = "Vence em até 3 dias") public boolean venceLogo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Somente em /api/alertas: texto do alerta",
            example = "O estoque de Chocolate em pó está baixo! Disponível: 400 g (alerta em 500 g).")
    public String mensagem;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Somente no relatório de itens em falta: quanto comprar (KG ou L)")
    public Double sugestaoCompra;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Somente no relatório de itens em falta: custo estimado da compra (R$)")
    public Double custoEstimado;
}
