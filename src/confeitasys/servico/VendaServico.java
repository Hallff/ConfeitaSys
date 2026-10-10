package confeitasys.servico;

import confeitasys.dao.Conexao;
import confeitasys.dao.IngredienteDAO;
import confeitasys.dao.ProdutoDAO;
import confeitasys.dao.VendaDAO;
import confeitasys.modelo.Ingrediente;
import confeitasys.modelo.ItemReceita;
import confeitasys.modelo.ItemVenda;
import confeitasys.modelo.Produto;
import confeitasys.modelo.Venda;
import confeitasys.util.RegraException;
import confeitasys.util.Util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** MOVIMENTO - Regras de vendas: registrar, editar, excluir, pesquisar, baixa no estoque. */
public class VendaServico {

    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})\\s*(?:h|:)\\s*(\\d{0,2})$");

    private final Conexao banco;
    private final VendaDAO dao = new VendaDAO();
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    public VendaServico(Conexao banco) { this.banco = banco; }

    /**
     * Pesquisa de vendas.
     * @param data  AAAA-MM-DD (vazio = todas as datas)
     * @param busca nome do produto, nº do pedido (12 ou #12) ou hora (14h, 14:30)
     */
    public List<Venda> pesquisar(String data, String busca) {
        String b = busca == null ? "" : busca.trim();
        String numero = b.startsWith("#") ? b.substring(1).trim() : b;
        if (!numero.isEmpty() && numero.matches("\\d+")) {         // por nº do pedido (ignora a data)
            Venda v = banco.executar(con -> dao.buscar(con, Integer.parseInt(numero)));
            List<Venda> r = new ArrayList<>();
            if (v != null) r.add(v);
            return r;
        }
        String prefixoHora = null;
        Matcher m = HORA.matcher(b.toLowerCase(Locale.ROOT));
        if (m.matches()) {
            prefixoHora = String.format("%02d", Integer.parseInt(m.group(1)));
            if (!m.group(2).isEmpty()) prefixoHora += ":" + m.group(2);
        }
        String termo = Util.semAcento(b);
        List<Venda> resultado = new ArrayList<>();
        for (Venda v : banco.executar(con -> dao.listar(con, data))) {
            if (prefixoHora != null) { if (!v.hora().startsWith(prefixoHora)) continue; }
            else if (!termo.isEmpty() && !Util.semAcento(v.produtoNome).contains(termo)) continue;
            resultado.add(v);
        }
        return resultado;
    }

    public Venda consultar(int id) {
        return banco.executar(con -> {
            Venda v = dao.buscar(con, id);
            if (v == null) throw new RegraException("Pedido nº " + id + " não encontrado.");
            return v;
        });
    }

    /** Registrar venda: grava a venda e dá baixa nos ingredientes do estoque (tudo numa transação). */
    public Venda registrar(Map<String, Object> d) {
        return banco.executar(con -> {
            Venda v = new Venda();
            v.dataHora = Util.agora();
            preencher(con, v, d, List.of());
            dao.inserir(con, v);
            return dao.buscar(con, v.id);
        });
    }

    /** Editar venda: devolve ao estoque o que estava lançado e desconta o novo. */
    public Venda editar(int id, Map<String, Object> d) {
        return banco.executar(con -> {
            Venda v = dao.buscar(con, id);
            if (v == null) throw new RegraException("Pedido nº " + id + " não encontrado.");
            List<ItemVenda> antigos = v.itens;
            preencher(con, v, d, antigos);
            dao.alterar(con, v);
            return dao.buscar(con, id);
        });
    }

    /** Excluir venda / devolução (RF4). Opcionalmente devolve os ingredientes ao estoque. */
    public void excluir(int id, boolean devolverAoEstoque) {
        banco.executar(con -> {
            Venda v = dao.buscar(con, id);
            if (v == null) throw new RegraException("Pedido nº " + id + " não encontrado.");
            if (devolverAoEstoque)
                for (ItemVenda i : v.itens)
                    if (i.ingredienteId != null && ingredienteDAO.buscar(con, i.ingredienteId) != null)
                        ingredienteDAO.movimentar(con, i.ingredienteId, i.quantidade, Util.agora());
            dao.excluir(con, id);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void preencher(Connection con, Venda v, Map<String, Object> d, List<ItemVenda> antigos) throws SQLException {
        int produtoId = (int) Util.numero(d, "produtoId");
        Produto produto = produtoDAO.buscar(con, produtoId);
        if (produto == null) throw new RegraException("Escolha o produto vendido.");
        double valor = Util.numero(d, "valorVenda");
        if (valor <= 0) throw new RegraException("Informe o valor da venda (R$).");

        // Ingredientes: os enviados pela tela (podem ter sido ajustados) ou a receita do produto
        Map<Integer, Double> uso = new LinkedHashMap<>();
        Object bruto = d.get("itens");
        if (bruto instanceof List<?> lista && !lista.isEmpty()) {
            for (Object o : lista) {
                Map<String, Object> m = (Map<String, Object>) o;
                int ingId = (int) Util.numero(m, "ingredienteId");
                double qtd = Util.numero(m, "quantidade");
                if (ingredienteDAO.buscar(con, ingId) == null) throw new RegraException("Escolha ingredientes válidos do estoque.");
                if (qtd <= 0) throw new RegraException("Informe a quantidade usada de cada ingrediente.");
                uso.merge(ingId, qtd, Double::sum);
            }
        } else {
            for (ItemReceita r : produto.receita) uso.merge(r.ingredienteId, r.quantidade, Double::sum);
        }
        if (uso.isEmpty()) throw new RegraException("O produto não tem ingredientes na receita.");

        // Quanto esta venda já tinha descontado (na edição isso volta a ficar disponível)
        Map<Integer, Double> usoAntigo = new HashMap<>();
        for (ItemVenda i : antigos) if (i.ingredienteId != null) usoAntigo.merge(i.ingredienteId, i.quantidade, Double::sum);

        // Confere o estoque ANTES de alterar qualquer coisa
        for (Map.Entry<Integer, Double> e : uso.entrySet()) {
            Ingrediente ing = ingredienteDAO.buscar(con, e.getKey());
            double disponivel = ing.quantidade + usoAntigo.getOrDefault(ing.id, 0.0);
            if (e.getValue() > disponivel + 1e-9)
                throw new RegraException("Estoque insuficiente de " + ing.nome + ": precisa de " + Util.fmtQtd(e.getValue(), ing.unidade)
                        + ", mas só há " + Util.fmtQtd(disponivel, ing.unidade) + ".");
        }

        // Devolve o uso antigo e dá baixa no novo
        String agora = Util.agora();
        for (ItemVenda i : antigos)
            if (i.ingredienteId != null && ingredienteDAO.buscar(con, i.ingredienteId) != null)
                ingredienteDAO.movimentar(con, i.ingredienteId, i.quantidade, agora);
        List<ItemVenda> itens = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : uso.entrySet()) {
            Ingrediente ing = ingredienteDAO.buscar(con, e.getKey());
            ingredienteDAO.movimentar(con, ing.id, -e.getValue(), agora);
            ItemVenda item = new ItemVenda();
            item.ingredienteId = ing.id;
            item.nome = ing.nome;
            item.unidade = ing.unidade;
            item.quantidade = Util.arred(e.getValue(), 4);
            item.custo = Util.arred(e.getValue() * ing.custoUnitario, 2);
            itens.add(item);
        }
        v.produtoId = produto.id;
        v.produtoNome = produto.nomeCompleto();
        v.valorVenda = Util.arred(valor, 2);
        v.itens = itens;
    }
}
