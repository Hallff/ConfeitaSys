package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RelatorioSalvoResposta(
        boolean ok,
        @Schema(description = "Caminho do arquivo HTML dos relatórios") String arquivo,
        @Schema(description = "Caminho do backup (.sql) feito junto") String backup) {
}
