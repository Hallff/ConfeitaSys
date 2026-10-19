package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Relatório de vendas do dia")
public record RelatorioVendasResposta(
        @Schema(example = "2026-10-15") String data,
        String geradoEm,
        List<VendaResposta> vendas,
        int quantidadeVendas,
        double faturamento,
        double custo,
        double lucro,
        @Schema(description = "Total de cada ingrediente usado no dia") List<ConsumoResposta> ingredientesUsados) {
}
