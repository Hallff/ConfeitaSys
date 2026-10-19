package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Usuário do sistema (sem a senha)")
public record UsuarioResposta(
        @Schema(description = "Id (UUID)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6") UUID id,
        @Schema(example = "gabriel") String nome,
        @Schema(example = "2026-10-15T09:00:00") String criadoEm) {
}
