package confeitasys.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/** Um ingrediente da receita de um produto e a quantidade usada (em KG ou L). */
public class ItemReceita {
    public int ingredienteId;
    public String ingredienteNome;
    public String unidade;
    public double quantidade;
    public double custoUnitario;   // custo atual do ingrediente (para estimar o custo do produto)

    public double custoEstimado() { return quantidade * custoUnitario; }

    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ingredienteId", ingredienteId);
        m.put("nome", ingredienteNome);
        m.put("unidade", unidade);
        m.put("quantidade", quantidade);
        m.put("custo", custoEstimado());
        return m;
    }
}
