package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro (regra de negócio, dados inválidos ou login)")
public record ErroResposta(
        @Schema(description = "Mensagem para o usuário", example = "Estoque insuficiente de Farinha de trigo: precisa de 500 g, mas só há 200 g.") String erro) {
}
