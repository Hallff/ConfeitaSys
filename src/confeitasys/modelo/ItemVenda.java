package confeitasys.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ingrediente usado numa venda. Guarda uma "foto" do nome e do custo no
 * momento da venda, para o histórico não mudar se o ingrediente for alterado.
 */
public class ItemVenda {
    public Integer ingredienteId;  // null se o ingrediente foi excluído depois
    public String nome;
    public String unidade;
    public double quantidade;      // em KG ou L
    public double custo;           // R$

    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingredienteId", ingredienteId);
        m.put("nome", nome);
        m.put("unidade", unidade);
        m.put("quantidade", quantidade);
        m.put("custo", custo);
        return m;
    }
}
