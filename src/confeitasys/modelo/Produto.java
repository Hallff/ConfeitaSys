package confeitasys.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CADASTRO 3 - Produto do cardápio, com a sua receita (ingredientes). */
public class Produto {
    public int id;
    public String nome;        // ex.: Bolo de chocolate
    public String tamanho;     // ex.: G
    public String recheio;     // ex.: Brigadeiro
    public double precoVenda;  // preço sugerido de venda
    public List<ItemReceita> receita = new ArrayList<>();

    /** Nome completo: "Bolo de chocolate G - recheio Brigadeiro". */
    public String nomeCompleto() {
        StringBuilder sb = new StringBuilder(nome);
        if (tamanho != null && !tamanho.isBlank()) sb.append(" ").append(tamanho);
        if (recheio != null && !recheio.isBlank()) sb.append(" - recheio ").append(recheio);
        return sb.toString();
    }

    public double custoEstimado() {
        double t = 0;
        for (ItemReceita i : receita) t += i.custoEstimado();
        return t;
    }

    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nome", nome);
        m.put("tamanho", tamanho);
        m.put("recheio", recheio);
        m.put("nomeCompleto", nomeCompleto());
        m.put("precoVenda", precoVenda);
        m.put("custoEstimado", custoEstimado());
        m.put("lucroEstimado", precoVenda - custoEstimado());
        List<Object> r = new ArrayList<>();
        for (ItemReceita i : receita) r.add(i.paraTela());
        m.put("receita", r);
        return m;
    }
}
