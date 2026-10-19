package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ArquivoResposta(
        boolean ok,
        @Schema(description = "Caminho do arquivo gerado no servidor") String arquivo) {
}
