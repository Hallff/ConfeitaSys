package br.com.confeitasys.dao;

import br.com.confeitasys.modelo.ItemReceita;
import br.com.confeitasys.modelo.Produto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Acesso às tabelas produto e produto_ingrediente (receita). */
@Repository
public class ProdutoDAO {

    private static final String CAMPOS = "id, nome, tamanho, recheio, preco_venda";
    private static final String RECEITA =
            "SELECT pi.produto_id, pi.ingrediente_id, i.nome, i.unidade, pi.quantidade, i.custo_unitario "
                    + "FROM produto_ingrediente pi JOIN ingrediente i ON i.id = pi.ingrediente_id ";

    private static final RowMapper<Produto> MAPA = (rs, n) -> {
        Produto p = new Produto();
        p.id = rs.getObject("id", UUID.class);
        p.nome = rs.getString("nome");
        p.tamanho = rs.getString("tamanho");
        p.recheio = rs.getString("recheio");
        p.precoVenda = rs.getDouble("preco_venda");
        return p;
    };

    private final JdbcTemplate jdbc;

    public ProdutoDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Todos os produtos com suas receitas (2 consultas no total). */
    public List<Produto> listar() {
        List<Produto> lista = jdbc.query(
                "SELECT " + CAMPOS + " FROM produto ORDER BY LOWER(nome), LOWER(tamanho), LOWER(recheio)", MAPA);
        Map<UUID, List<ItemReceita>> receitas = new HashMap<>();
        jdbc.query(RECEITA + "ORDER BY pi.produto_id, LOWER(i.nome)",
                rs -> { receitas.computeIfAbsent(rs.getObject("produto_id", UUID.class), k -> new ArrayList<>()).add(lerItem(rs)); });
        for (Produto p : lista) p.receita = receitas.getOrDefault(p.id, new ArrayList<>());
        return lista;
    }

    public Produto buscar(UUID id) {
        List<Produto> l = jdbc.query("SELECT " + CAMPOS + " FROM produto WHERE id = ?", MAPA, id);
        if (l.isEmpty()) return null;
        Produto p = l.get(0);
        p.receita = jdbc.query(RECEITA + "WHERE pi.produto_id = ? ORDER BY LOWER(i.nome)", (rs, n) -> lerItem(rs), id);
        return p;
    }

    /** Já existe OUTRO produto com o mesmo nome + tamanho + recheio? (p.id nulo = produto novo) */
    public boolean existe(Produto p) {
        Boolean r = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM produto WHERE LOWER(nome) = LOWER(?) AND LOWER(tamanho) = LOWER(?) "
                        + "AND LOWER(recheio) = LOWER(?) AND id IS DISTINCT FROM CAST(? AS UUID))",
                Boolean.class, p.nome, p.tamanho, p.recheio, p.id);
        return Boolean.TRUE.equals(r);
    }

    /** Grava (o banco gera o id UUID) e devolve o id. */
    public UUID inserir(Produto p) {
        p.id = jdbc.queryForObject(
                "INSERT INTO produto (nome, tamanho, recheio, preco_venda) VALUES (?, ?, ?, ?) RETURNING id",
                UUID.class, p.nome, p.tamanho, p.recheio, p.precoVenda);
        gravarReceita(p);
        return p.id;
    }

    public void alterar(Produto p) {
        jdbc.update("UPDATE produto SET nome = ?, tamanho = ?, recheio = ?, preco_venda = ? WHERE id = ?",
                p.nome, p.tamanho, p.recheio, p.precoVenda, p.id);
        gravarReceita(p);
    }

    /** As vendas antigas continuam guardadas (produto_id fica NULL; o nome foi copiado na venda). */
    public void excluir(UUID id) {
        jdbc.update("DELETE FROM produto WHERE id = ?", id);
    }

    /** Apaga a receita antiga e grava a nova. */
    private void gravarReceita(Produto p) {
        jdbc.update("DELETE FROM produto_ingrediente WHERE produto_id = ?", p.id);
        List<Object[]> linhas = new ArrayList<>();
        for (ItemReceita i : p.receita) linhas.add(new Object[] {p.id, i.ingredienteId, i.quantidade});
        if (!linhas.isEmpty())
            jdbc.batchUpdate("INSERT INTO produto_ingrediente (produto_id, ingrediente_id, quantidade) VALUES (?, ?, ?)", linhas);
    }

    private static ItemReceita lerItem(ResultSet rs) throws SQLException {
        ItemReceita i = new ItemReceita();
        i.ingredienteId = rs.getObject("ingrediente_id", UUID.class);
        i.ingredienteNome = rs.getString("nome");
        i.unidade = rs.getString("unidade");
        i.quantidade = rs.getDouble("quantidade");
        i.custoUnitario = rs.getDouble("custo_unitario");
        return i;
    }
}
