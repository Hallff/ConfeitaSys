package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login realizado")
public record TokenResposta(
        boolean ok,
        @Schema(description = "Token de acesso: envie no cabeçalho Authorization: Bearer <token>",
                example = "9f2c4b...e81a") String token,
        @Schema(description = "Nome do usuário", example = "gabriel") String nome) {
}
