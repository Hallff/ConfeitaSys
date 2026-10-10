package confeitasys.modelo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro de venda (pedido). */
public class Venda {
    public int id;
    public String dataHora;     // AAAA-MM-DDTHH:MM:SS
    public Integer produtoId;   // null se o produto foi excluído do cardápio depois
    public String produtoNome;
    public double valorVenda;
    public List<ItemVenda> itens = new ArrayList<>();

    public double custo() {
        double t = 0;
        for (ItemVenda i : itens) t += i.custo;
        return t;
    }

    public double lucro() { return valorVenda - custo(); }
    public String data() { return dataHora.substring(0, 10); }
    public String hora() { return dataHora.substring(11, 16); }

    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("dataHora", dataHora);
        m.put("data", data());
        m.put("hora", hora());
        m.put("produtoId", produtoId);
        m.put("produto", produtoNome);
        m.put("valorVenda", valorVenda);
        m.put("custo", custo());
        m.put("lucro", lucro());
        List<Object> l = new ArrayList<>();
        for (ItemVenda i : itens) l.add(i.paraTela());
        m.put("itens", l);
        return m;
    }
}
