package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Alertas exibidos em qualquer tela")
public record AlertasResposta(
        @Schema(description = "Itens com quantidade igual ou abaixo da quantidade de alerta (toca a sirene)") List<IngredienteResposta> estoqueBaixo,
        @Schema(description = "Itens vencidos ou que vencem em até 3 dias") List<IngredienteResposta> validade) {
}
