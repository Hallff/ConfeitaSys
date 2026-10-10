package confeitasys.servico;

import confeitasys.dao.Conexao;
import confeitasys.dao.IngredienteDAO;
import confeitasys.modelo.Ingrediente;
import confeitasys.util.RegraException;
import confeitasys.util.Util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CADASTRO 2 - Regras do estoque de ingredientes + alertas. */
public class IngredienteServico {

    private final Conexao banco;
    private final IngredienteDAO dao = new IngredienteDAO();

    public IngredienteServico(Conexao banco) { this.banco = banco; }

    public List<Ingrediente> listar() {
        return banco.executar(dao::listar);
    }

    public Ingrediente incluir(Map<String, Object> d) {
        return banco.executar(con -> {
            Ingrediente i = new Ingrediente();
            preencher(i, d);
            if (dao.buscarPorNome(con, i.nome) != null)
                throw new RegraException("Já existe um item chamado \"" + i.nome + "\" no estoque.");
            i.id = dao.inserir(con, i);
            return dao.buscar(con, i.id);
        });
    }

    public Ingrediente alterar(int id, Map<String, Object> d) {
        return banco.executar(con -> {
            Ingrediente i = dao.buscar(con, id);
            if (i == null) throw new RegraException("Item de estoque não encontrado.");
            preencher(i, d);
            Ingrediente mesmoNome = dao.buscarPorNome(con, i.nome);
            if (mesmoNome != null && mesmoNome.id != id)
                throw new RegraException("Já existe um item chamado \"" + mesmoNome.nome + "\" no estoque.");
            dao.alterar(con, i);
            return dao.buscar(con, id);
        });
    }

    /** Não deixa excluir ingrediente que faz parte da receita de algum produto. */
    public void excluir(int id) {
        banco.executar(con -> {
            if (dao.buscar(con, id) == null) throw new RegraException("Item de estoque não encontrado.");
            List<String> produtos = dao.produtosQueUsam(con, id);
            if (!produtos.isEmpty())
                throw new RegraException("Não é possível excluir: este item faz parte da receita de " + String.join(", ", produtos)
                        + ". Remova-o da receita primeiro.");
            dao.excluir(con, id);
            return null;
        });
    }

    /**
     * Alertas exibidos em qualquer tela:
     *  - ESTOQUE BAIXO: quantidade <= quantidade de alerta definida pelo usuário (toca sirene)
     *  - VALIDADE: item vencido ou vencendo nos próximos dias
     */
    public Map<String, Object> alertas() {
        List<Object> baixo = new ArrayList<>();
        List<Object> validade = new ArrayList<>();
        for (Ingrediente i : listar()) {
            if (i.estoqueBaixo()) {
                Map<String, Object> m = i.paraTela();
                m.put("mensagem", "O estoque de " + i.nome + " está baixo! Disponível: " + Util.fmtQtd(i.quantidade, i.unidade)
                        + " (alerta em " + Util.fmtQtd(i.quantidadeAlerta, i.unidade) + ").");
                baixo.add(m);
            }
            if (i.vencido() || i.venceLogo()) {
                Map<String, Object> m = i.paraTela();
                long dias = i.diasParaVencer();
                m.put("mensagem", i.vencido() ? i.nome + " está VENCIDO desde " + br(i.validade) + "."
                        : dias == 0 ? i.nome + " vence HOJE." : i.nome + " vence em " + dias + (dias == 1 ? " dia" : " dias") + " (" + br(i.validade) + ").");
                validade.add(m);
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("estoqueBaixo", baixo);
        r.put("validade", validade);
        return r;
    }

    private void preencher(Ingrediente i, Map<String, Object> d) {
        String nome = Util.obrigatorio(d, "nome", "Informe o nome do item.");
        String unidade = Util.texto(d, "unidade");
        if (!"KG".equals(unidade) && !"L".equals(unidade))
            throw new RegraException("Escolha a unidade: quilogramas (kg) ou litros (L).");
        double qtd = Util.numero(d, "quantidade");
        double valor = Util.numero(d, "valorPago");
        double alerta = Util.numero(d, "quantidadeAlerta");
        if (qtd < 0) throw new RegraException("A quantidade não pode ser negativa.");
        if (valor < 0) throw new RegraException("O valor pago não pode ser negativo.");
        if (alerta < 0) throw new RegraException("A quantidade de alerta não pode ser negativa.");
        i.nome = nome;
        i.unidade = unidade;
        i.quantidade = Util.arred(qtd, 3);
        if (qtd > 0) i.custoUnitario = valor / qtd;     // R$ por kg ou L
        i.quantidadeAlerta = Util.arred(alerta, 3);
        i.validade = Util.data(d, "validade");
        i.atualizadoEm = Util.agora();
    }

    private static String br(String iso) {
        String[] p = iso.split("-");
        return p[2] + "/" + p[1] + "/" + p[0];
    }
}
