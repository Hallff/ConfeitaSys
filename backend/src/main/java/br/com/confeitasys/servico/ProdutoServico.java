package br.com.confeitasys.servico;

import br.com.confeitasys.dao.IngredienteDAO;
import br.com.confeitasys.dao.ProdutoDAO;
import br.com.confeitasys.dto.ItemQuantidadeRequisicao;
import br.com.confeitasys.dto.ProdutoRequisicao;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.modelo.ItemReceita;
import br.com.confeitasys.modelo.Produto;
import br.com.confeitasys.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** CADASTRO 3 - Regras do cadastro de produtos do cardápio e suas receitas. */
@Service
public class ProdutoServico {

    private final ProdutoDAO dao;
    private final IngredienteDAO ingredienteDAO;

    public ProdutoServico(ProdutoDAO dao, IngredienteDAO ingredienteDAO) {
        this.dao = dao;
        this.ingredienteDAO = ingredienteDAO;
    }

    @Transactional(readOnly = true)
    public List<Produto> listar() {
        return dao.listar();
    }

    @Transactional(readOnly = true)
    public Produto consultar(UUID id) {
        Produto p = dao.buscar(id);
        if (p == null) throw new RegraException("Produto não encontrado.");
        return p;
    }

    @Transactional
    public Produto incluir(ProdutoRequisicao d) {
        Produto p = new Produto();
        preencher(p, d);
        dao.inserir(p);
        return dao.buscar(p.id);
    }

    @Transactional
    public Produto alterar(UUID id, ProdutoRequisicao d) {
        Produto p = consultar(id);
        preencher(p, d);
        dao.alterar(p);
        return dao.buscar(id);
    }

    /** Excluir produto: as vendas antigas continuam guardadas com o nome do produto. */
    @Transactional
    public void excluir(UUID id) {
        consultar(id);
        dao.excluir(id);
    }

    private void preencher(Produto p, ProdutoRequisicao d) {
        if (d == null) throw new RegraException("Informe os dados do produto.");
        p.nome = Util.obrigatorio(d.nome(), "Informe o nome do produto.");
        p.tamanho = Util.texto(d.tamanho());
        p.recheio = Util.texto(d.recheio());
        p.precoVenda = Util.arred(Util.numero(d.precoVenda()), 2);
        if (p.precoVenda <= 0) throw new RegraException("Informe o preço de venda do produto.");
        if (dao.existe(p)) throw new RegraException("Já existe o produto \"" + p.nomeCompleto() + "\" no cardápio.");

        List<ItemQuantidadeRequisicao> lista = d.receita();
        if (lista == null || lista.isEmpty())
            throw new RegraException("Informe os ingredientes da receita do produto.");
        p.receita.clear();
        Set<UUID> vistos = new HashSet<>();
        for (ItemQuantidadeRequisicao m : lista) {
            if (m == null || m.ingredienteId() == null || ingredienteDAO.buscar(m.ingredienteId()) == null)
                throw new RegraException("Escolha ingredientes válidos do estoque.");
            double qtd = Util.numero(m.quantidade());
            if (qtd <= 0) throw new RegraException("Informe a quantidade de cada ingrediente da receita.");
            if (!vistos.add(m.ingredienteId())) throw new RegraException("O mesmo ingrediente aparece duas vezes na receita.");
            ItemReceita i = new ItemReceita();
            i.ingredienteId = m.ingredienteId();
            i.quantidade = Util.arred(qtd, 4);
            p.receita.add(i);
        }
    }
}
