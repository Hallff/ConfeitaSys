package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alteração do usuário. A senha atual é obrigatória; deixe novaSenha vazia para manter a senha.")
public record UsuarioAlteracaoRequisicao(
        @Schema(description = "Novo nome de usuário", example = "gabriel") String nome,
        @Schema(description = "Senha atual (obrigatória)", example = "confeita123") String senhaAtual,
        @Schema(description = "Nova senha (opcional, mínimo 6 caracteres)", example = "") String novaSenha) {
}
