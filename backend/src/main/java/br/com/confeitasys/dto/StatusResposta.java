package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Situação do sistema")
public record StatusResposta(
        @Schema(description = "Já existe usuário cadastrado? (false = primeiro acesso)") boolean temUsuario,
        @Schema(description = "O token enviado é válido?") boolean logado,
        @Schema(description = "Nome do usuário logado", nullable = true) String usuario) {
}
