package br.com.confeitasys.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Itens em falta (estoque baixo ou vencidos) com sugestão de compra")
public record RelatorioFaltandoResposta(
        String geradoEm,
        List<IngredienteResposta> itens) {
}
