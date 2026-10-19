package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.ItemResposta;
import java.util.UUID;

/** Um ingrediente da receita de um produto e a quantidade usada (em KG ou L). */
public class ItemReceita {
    public UUID ingredienteId;
    public String ingredienteNome;
    public String unidade;
    public double quantidade;
    public double custoUnitario;   // custo atual do ingrediente (para estimar o custo do produto)

    public double custoEstimado() { return quantidade * custoUnitario; }

    public ItemResposta paraResposta() {
        return new ItemResposta(ingredienteId, ingredienteNome, unidade, quantidade, custoEstimado());
    }
}
