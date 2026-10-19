package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.ItemResposta;
import java.util.UUID;

/**
 * Ingrediente usado numa venda. Guarda uma "foto" do nome e do custo no
 * momento da venda, para o histórico não mudar se o ingrediente for alterado.
 */
public class ItemVenda {
    public UUID ingredienteId;     // null se o ingrediente foi excluído depois
    public String nome;
    public String unidade;
    public double quantidade;      // em KG ou L
    public double custo;           // R$

    public ItemResposta paraResposta() {
        return new ItemResposta(ingredienteId, nome, unidade, quantidade, custo);
    }
}
