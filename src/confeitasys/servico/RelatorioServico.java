package confeitasys.servico;

import confeitasys.modelo.Ingrediente;
import confeitasys.modelo.ItemVenda;
import confeitasys.modelo.Venda;
import confeitasys.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relatórios (opcionais): vendas do dia, estoque e itens em falta. */
public class RelatorioServico {

    private final VendaServico vendas;
    private final IngredienteServico ingredientes;

    public RelatorioServico(VendaServico vendas, IngredienteServico ingredientes) {
        this.vendas = vendas;
        this.ingredientes = ingredientes;
    }

    public Map<String, Object> vendasDoDia(String data) {
        List<Venda> lista = new ArrayList<>(vendas.pesquisar(data, ""));
        lista.sort(Comparator.comparing(v -> v.dataHora));
        double faturamento = 0, custo = 0;
        Map<String, Map<String, Object>> consumo = new LinkedHashMap<>();
        List<Object> linhas = new ArrayList<>();
        for (Venda v : lista) {
            faturamento += v.valorVenda;
            custo += v.custo();
            linhas.add(v.paraTela());
            for (ItemVenda i : v.itens) {
                Map<String, Object> c = consumo.computeIfAbsent(i.nome + "|" + i.unidade, k -> {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("nome", i.nome); n.put("unidade", i.unidade); n.put("quantidade", 0.0); n.put("custo", 0.0);
                    return n;
                });
                c.put("quantidade", (Double) c.get("quantidade") + i.quantidade);
                c.put("custo", (Double) c.get("custo") + i.custo);
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("data", data);
        r.put("geradoEm", Util.agora());
        r.put("vendas", linhas);
        r.put("quantidadeVendas", lista.size());
        r.put("faturamento", faturamento);
        r.put("custo", custo);
        r.put("lucro", faturamento - custo);
        r.put("ingredientesUsados", new ArrayList<>(consumo.values()));
        return r;
    }

    public Map<String, Object> estoque() {
        List<Object> itens = new ArrayList<>();
        double total = 0;
        for (Ingrediente i : ingredientes.listar()) { itens.add(i.paraTela()); total += i.valorEmEstoque(); }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("geradoEm", Util.agora());
        r.put("itens", itens);
        r.put("valorTotal", total);
        return r;
    }

    public Map<String, Object> faltando() {
        List<Object> itens = new ArrayList<>();
        for (Ingrediente i : ingredientes.listar()) {
            if (!i.estoqueBaixo() && !i.vencido()) continue;
            Map<String, Object> m = i.paraTela();
            double comprar = Math.max(0, i.quantidadeAlerta - i.quantidade);
            if (i.vencido()) comprar = Math.max(i.quantidade, i.quantidadeAlerta); // repor o que venceu
            m.put("sugestaoCompra", comprar);
            m.put("custoEstimado", comprar * i.custoUnitario);
            itens.add(m);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("geradoEm", Util.agora());
        r.put("itens", itens);
        return r;
    }
}
