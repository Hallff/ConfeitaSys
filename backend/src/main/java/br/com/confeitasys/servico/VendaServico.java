package br.com.confeitasys.servico;

import br.com.confeitasys.dao.IngredienteDAO;
import br.com.confeitasys.dao.ProdutoDAO;
import br.com.confeitasys.dao.VendaDAO;
import br.com.confeitasys.dto.ItemQuantidadeRequisicao;
import br.com.confeitasys.dto.VendaRequisicao;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.modelo.Ingrediente;
import br.com.confeitasys.modelo.ItemReceita;
import br.com.confeitasys.modelo.ItemVenda;
import br.com.confeitasys.modelo.Produto;
import br.com.confeitasys.modelo.Venda;
import br.com.confeitasys.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** MOVIMENTO - Regras de vendas: registrar, editar, excluir, pesquisar e baixa no estoque. */
@Service
public class VendaServico {

    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})\\s*(?:h|:)\\s*(\\d{0,2})$");

    private final VendaDAO dao;
    private final ProdutoDAO produtoDAO;
    private final IngredienteDAO ingredienteDAO;

    public VendaServico(VendaDAO dao, ProdutoDAO produtoDAO, IngredienteDAO ingredienteDAO) {
        this.dao = dao;
        this.produtoDAO = produtoDAO;
        this.ingredienteDAO = ingredienteDAO;
    }

    /**
     * Pesquisa de vendas.
     * @param data  AAAA-MM-DD (vazio = todas as datas)
     * @param busca nome do produto, nº do pedido (12 ou #12) ou hora (14h, 14:30)
     */
    @Transactional(readOnly = true)
    public List<Venda> pesquisar(String data, String busca) {
        String b = Util.texto(busca);
        String numero = b.startsWith("#") ? b.substring(1).trim() : b;
        if (!numero.isEmpty() && numero.matches("\\d{1,9}")) {        // por nº do pedido (ignora a data)
            Venda v = dao.buscarPorNumero(Integer.parseInt(numero));
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
        LocalDate dia = Util.data(data, "data");
        List<Venda> resultado = new ArrayList<>();
        for (Venda v : dao.listar(dia)) {
            if (prefixoHora != null) { if (!v.hora().startsWith(prefixoHora)) continue; }
            else if (!termo.isEmpty() && !Util.semAcento(v.produtoNome).contains(termo)) continue;
            resultado.add(v);
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public Venda consultar(UUID id) {
        Venda v = dao.buscar(id);
        if (v == null) throw new RegraException("Pedido não encontrado.");
        return v;
    }

    /** Registrar venda: grava a venda e dá baixa nos ingredientes do estoque (tudo numa transação). */
    @Transactional
    public Venda registrar(VendaRequisicao d) {
        Venda v = new Venda();
        v.dataHora = Util.agora();
        preencher(v, d, List.of());
        dao.inserir(v);
        return dao.buscar(v.id);
    }

    /** Editar venda: devolve ao estoque o que estava lançado e desconta o novo. */
    @Transactional
    public Venda editar(UUID id, VendaRequisicao d) {
        Venda v = consultar(id);
        List<ItemVenda> antigos = v.itens;
        preencher(v, d, antigos);
        dao.alterar(v);
        return dao.buscar(id);
    }

    /** Excluir venda / devolução. Opcionalmente devolve os ingredientes ao estoque. */
    @Transactional
    public void excluir(UUID id, boolean devolverAoEstoque) {
        Venda v = consultar(id);
        if (devolverAoEstoque) {
            LocalDateTime agora = Util.agora();
            for (ItemVenda i : v.itens)
                if (i.ingredienteId != null && ingredienteDAO.buscarParaAtualizar(i.ingredienteId) != null)
                    ingredienteDAO.movimentar(i.ingredienteId, i.quantidade, agora);
        }
        dao.excluir(id);
    }

    private void preencher(Venda v, VendaRequisicao d, List<ItemVenda> antigos) {
        if (d == null || d.produtoId() == null) throw new RegraException("Escolha o produto vendido.");
        Produto produto = produtoDAO.buscar(d.produtoId());
        if (produto == null) throw new RegraException("Escolha o produto vendido.");
        double valor = Util.numero(d.valorVenda());
        if (valor <= 0) throw new RegraException("Informe o valor da venda (R$).");

        // Ingredientes: os enviados pela tela (podem ter sido ajustados) ou a receita do produto
        Map<UUID, Double> uso = new LinkedHashMap<>();
        if (d.itens() != null && !d.itens().isEmpty()) {
            for (ItemQuantidadeRequisicao m : d.itens()) {
                if (m == null || m.ingredienteId() == null) throw new RegraException("Escolha ingredientes válidos do estoque.");
                double qtd = Util.numero(m.quantidade());
                if (qtd <= 0) throw new RegraException("Informe a quantidade usada de cada ingrediente.");
                uso.merge(m.ingredienteId(), qtd, Double::sum);
            }
        } else {
            for (ItemReceita r : produto.receita) uso.merge(r.ingredienteId, r.quantidade, Double::sum);
        }
        if (uso.isEmpty()) throw new RegraException("O produto não tem ingredientes na receita.");

        // Quanto esta venda já tinha descontado (na edição isso volta a ficar disponível)
        Map<UUID, Double> usoAntigo = new HashMap<>();
        for (ItemVenda i : antigos) if (i.ingredienteId != null) usoAntigo.merge(i.ingredienteId, i.quantidade, Double::sum);

        // Trava os ingredientes envolvidos (sempre na mesma ordem, para evitar deadlock)
        TreeSet<UUID> ids = new TreeSet<>(uso.keySet());
        ids.addAll(usoAntigo.keySet());
        Map<UUID, Ingrediente> travados = new HashMap<>();
        for (UUID id : ids) {
            Ingrediente ing = ingredienteDAO.buscarParaAtualizar(id);
            if (ing != null) travados.put(id, ing);
        }

        // Confere o estoque ANTES de alterar qualquer coisa
        for (Map.Entry<UUID, Double> e : uso.entrySet()) {
            Ingrediente ing = travados.get(e.getKey());
            if (ing == null) throw new RegraException("Escolha ingredientes válidos do estoque.");
            double disponivel = ing.quantidade + usoAntigo.getOrDefault(ing.id, 0.0);
            if (e.getValue() > disponivel + 1e-9)
                throw new RegraException("Estoque insuficiente de " + ing.nome + ": precisa de " + Util.fmtQtd(e.getValue(), ing.unidade)
                        + ", mas só há " + Util.fmtQtd(disponivel, ing.unidade) + ".");
        }

        // Devolve o uso antigo e dá baixa no novo
        LocalDateTime agora = Util.agora();
        for (Map.Entry<UUID, Double> e : usoAntigo.entrySet())
            if (travados.containsKey(e.getKey())) ingredienteDAO.movimentar(e.getKey(), e.getValue(), agora);
        List<ItemVenda> itens = new ArrayList<>();
        for (Map.Entry<UUID, Double> e : uso.entrySet()) {
            Ingrediente ing = travados.get(e.getKey());
            ingredienteDAO.movimentar(ing.id, -e.getValue(), agora);
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
