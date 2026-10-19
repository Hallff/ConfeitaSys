package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Confirmação de senha")
public record SenhaRequisicao(
        @Schema(description = "Senha atual do usuário", example = "confeita123") String senha) {
}
