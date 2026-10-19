package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Relatórios já montados em HTML, para salvar em arquivo")
public record RelatorioSalvarRequisicao(
        @Schema(description = "Data dos relatórios (AAAA-MM-DD)", example = "2026-10-15") String data,
        @Schema(description = "Conteúdo HTML", example = "<html>...</html>") String html) {
}
