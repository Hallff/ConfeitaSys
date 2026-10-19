package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Nome de usuário e senha (login ou primeiro acesso)")
public record CredenciaisRequisicao(
        @Schema(description = "Nome de usuário", example = "gabriel") String nome,
        @Schema(description = "Senha (mínimo 6 caracteres)", example = "confeita123") String senha) {
}
