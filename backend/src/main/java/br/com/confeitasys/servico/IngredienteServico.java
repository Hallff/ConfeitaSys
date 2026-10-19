package br.com.confeitasys.servico;

import br.com.confeitasys.dao.IngredienteDAO;
import br.com.confeitasys.dto.AlertasResposta;
import br.com.confeitasys.dto.IngredienteRequisicao;
import br.com.confeitasys.dto.IngredienteResposta;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.modelo.Ingrediente;
import br.com.confeitasys.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** CADASTRO 2 - Regras do estoque de ingredientes + alertas. */
@Service
public class IngredienteServico {

    private final IngredienteDAO dao;

    public IngredienteServico(IngredienteDAO dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<Ingrediente> listar() {
        return dao.listar();
    }

    @Transactional
    public Ingrediente incluir(IngredienteRequisicao d) {
        Ingrediente i = new Ingrediente();
        preencher(i, d);
        if (dao.buscarPorNome(i.nome) != null)
            throw new RegraException("Já existe um item chamado \"" + i.nome + "\" no estoque.");
        i.id = dao.inserir(i);
        return dao.buscar(i.id);
    }

    @Transactional
    public Ingrediente alterar(UUID id, IngredienteRequisicao d) {
        Ingrediente i = dao.buscar(id);
        if (i == null) throw new RegraException("Item de estoque não encontrado.");
        preencher(i, d);
        Ingrediente mesmoNome = dao.buscarPorNome(i.nome);
        if (mesmoNome != null && !mesmoNome.id.equals(id))
            throw new RegraException("Já existe um item chamado \"" + mesmoNome.nome + "\" no estoque.");
        dao.alterar(i);
        return dao.buscar(id);
    }

    /** Não deixa excluir ingrediente que faz parte da receita de algum produto. */
    @Transactional
    public void excluir(UUID id) {
        if (dao.buscar(id) == null) throw new RegraException("Item de estoque não encontrado.");
        List<String> produtos = dao.produtosQueUsam(id);
        if (!produtos.isEmpty())
            throw new RegraException("Não é possível excluir: este item faz parte da receita de "
                    + String.join(", ", produtos) + ". Remova-o da receita primeiro.");
        dao.excluir(id);
    }

    /**
     * Alertas exibidos em qualquer tela:
     *  - ESTOQUE BAIXO: quantidade <= quantidade de alerta definida pelo usuário (toca sirene)
     *  - VALIDADE: item vencido ou vencendo nos próximos dias
     */
    @Transactional(readOnly = true)
    public AlertasResposta alertas() {
        List<IngredienteResposta> baixo = new ArrayList<>();
        List<IngredienteResposta> validade = new ArrayList<>();
        for (Ingrediente i : dao.listar()) {
            if (i.estoqueBaixo()) {
                IngredienteResposta r = i.paraResposta();
                r.mensagem = "O estoque de " + i.nome + " está baixo! Disponível: " + Util.fmtQtd(i.quantidade, i.unidade)
                        + " (alerta em " + Util.fmtQtd(i.quantidadeAlerta, i.unidade) + ").";
                baixo.add(r);
            }
            if (i.vencido() || i.venceLogo()) {
                IngredienteResposta r = i.paraResposta();
                long dias = i.diasParaVencer();
                r.mensagem = i.vencido() ? i.nome + " está VENCIDO desde " + Util.br(i.validade) + "."
                        : dias == 0 ? i.nome + " vence HOJE."
                        : i.nome + " vence em " + dias + (dias == 1 ? " dia" : " dias") + " (" + Util.br(i.validade) + ").";
                validade.add(r);
            }
        }
        return new AlertasResposta(baixo, validade);
    }

    private void preencher(Ingrediente i, IngredienteRequisicao d) {
        if (d == null) throw new RegraException("Informe os dados do item.");
        String nome = Util.obrigatorio(d.nome(), "Informe o nome do item.");
        String unidade = Util.texto(d.unidade()).toUpperCase(java.util.Locale.ROOT);
        if (!"KG".equals(unidade) && !"L".equals(unidade))
            throw new RegraException("Escolha a unidade: quilogramas (KG) ou litros (L).");
        double qtd = Util.numero(d.quantidade());
        double valor = Util.numero(d.valorPago());
        double alerta = Util.numero(d.quantidadeAlerta());
        if (qtd < 0) throw new RegraException("A quantidade não pode ser negativa.");
        if (valor < 0) throw new RegraException("O valor pago não pode ser negativo.");
        if (alerta < 0) throw new RegraException("A quantidade de alerta não pode ser negativa.");
        i.nome = nome;
        i.unidade = unidade;
        i.quantidade = Util.arred(qtd, 3);
        if (qtd > 0) i.custoUnitario = valor / qtd;     // R$ por KG ou L
        i.quantidadeAlerta = Util.arred(alerta, 3);
        i.validade = Util.data(d.validade(), "validade");
        i.atualizadoEm = Util.agora();
    }
}
