package confeitasys.servico;

import confeitasys.dao.Conexao;
import confeitasys.dao.IngredienteDAO;
import confeitasys.dao.ProdutoDAO;
import confeitasys.modelo.ItemReceita;
import confeitasys.modelo.Produto;
import confeitasys.util.RegraException;
import confeitasys.util.Util;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** CADASTRO 3 - Regras do cadastro de produtos do cardápio e suas receitas. */
public class ProdutoServico {

    private final Conexao banco;
    private final ProdutoDAO dao = new ProdutoDAO();
    private final IngredienteDAO ingredienteDAO = new IngredienteDAO();

    public ProdutoServico(Conexao banco) { this.banco = banco; }

    public List<Produto> listar() { return banco.executar(dao::listar); }

    public Produto consultar(int id) {
        return banco.executar(con -> {
            Produto p = dao.buscar(con, id);
            if (p == null) throw new RegraException("Produto não encontrado.");
            return p;
        });
    }

    public Produto incluir(Map<String, Object> d) {
        return banco.executar(con -> {
            Produto p = new Produto();
            preencher(con, p, d);
            dao.inserir(con, p);
            return dao.buscar(con, p.id);
        });
    }

    public Produto alterar(int id, Map<String, Object> d) {
        return banco.executar(con -> {
            Produto p = dao.buscar(con, id);
            if (p == null) throw new RegraException("Produto não encontrado.");
            preencher(con, p, d);
            dao.alterar(con, p);
            return dao.buscar(con, id);
        });
    }

    /** Excluir produto: as vendas antigas continuam guardadas com o nome do produto. */
    public void excluir(int id) {
        banco.executar(con -> {
            if (dao.buscar(con, id) == null) throw new RegraException("Produto não encontrado.");
            dao.excluir(con, id);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private void preencher(java.sql.Connection con, Produto p, Map<String, Object> d) throws java.sql.SQLException {
        p.nome = Util.obrigatorio(d, "nome", "Informe o nome do produto.");
        p.tamanho = Util.texto(d, "tamanho");
        p.recheio = Util.texto(d, "recheio");
        p.precoVenda = Util.arred(Util.numero(d, "precoVenda"), 2);
        if (p.precoVenda <= 0) throw new RegraException("Informe o preço de venda do produto.");
        if (dao.existe(con, p)) throw new RegraException("Já existe o produto \"" + p.nomeCompleto() + "\" no cardápio.");

        Object bruto = d.get("receita");
        if (!(bruto instanceof List<?> lista) || lista.isEmpty())
            throw new RegraException("Informe os ingredientes da receita do produto.");
        p.receita.clear();
        Set<Integer> vistos = new HashSet<>();
        for (Object o : lista) {
            Map<String, Object> m = (Map<String, Object>) o;
            int ingId = (int) Util.numero(m, "ingredienteId");
            double qtd = Util.numero(m, "quantidade");
            if (ingredienteDAO.buscar(con, ingId) == null) throw new RegraException("Escolha ingredientes válidos do estoque.");
            if (qtd <= 0) throw new RegraException("Informe a quantidade de cada ingrediente da receita.");
            if (!vistos.add(ingId)) throw new RegraException("O mesmo ingrediente aparece duas vezes na receita.");
            ItemReceita i = new ItemReceita();
            i.ingredienteId = ingId;
            i.quantidade = Util.arred(qtd, 4);
            p.receita.add(i);
        }
    }
}
