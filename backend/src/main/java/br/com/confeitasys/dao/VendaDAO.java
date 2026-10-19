package br.com.confeitasys.dao;

import br.com.confeitasys.modelo.ItemVenda;
import br.com.confeitasys.modelo.Venda;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Acesso às tabelas venda e item_venda. */
@Repository
public class VendaDAO {

    private static final String CAMPOS = "id, numero, data_hora, produto_id, produto_nome, valor_venda";
    private static final String ITENS =
            "SELECT iv.venda_id, iv.ingrediente_id, iv.ingrediente_nome, iv.unidade, iv.quantidade, iv.custo FROM item_venda iv ";

    private static final RowMapper<Venda> MAPA = (rs, n) -> {
        Venda v = new Venda();
        v.id = rs.getObject("id", UUID.class);
        v.numero = rs.getInt("numero");
        v.dataHora = rs.getObject("data_hora", LocalDateTime.class);
        v.produtoId = rs.getObject("produto_id", UUID.class);
        v.produtoNome = rs.getString("produto_nome");
        v.valorVenda = rs.getDouble("valor_venda");
        return v;
    };

    private final JdbcTemplate jdbc;

    public VendaDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Vendas de um dia (ou de todos os dias, se data == null), mais recentes primeiro. */
    public List<Venda> listar(LocalDate data) {
        String filtro = data == null ? "" : "WHERE v.data_hora >= ? AND v.data_hora < ? ";
        Object[] args = data == null ? new Object[0] : new Object[] {data.atStartOfDay(), data.plusDays(1).atStartOfDay()};

        List<Venda> lista = jdbc.query("SELECT " + CAMPOS + " FROM venda v " + filtro + "ORDER BY v.data_hora DESC, v.numero DESC",
                MAPA, args);
        Map<UUID, List<ItemVenda>> itens = new HashMap<>();
        jdbc.query(ITENS + "JOIN venda v ON v.id = iv.venda_id " + filtro + "ORDER BY iv.venda_id, iv.ingrediente_nome",
                rs -> { itens.computeIfAbsent(rs.getObject("venda_id", UUID.class), k -> new ArrayList<>()).add(lerItem(rs)); }, args);
        for (Venda v : lista) v.itens = itens.getOrDefault(v.id, new ArrayList<>());
        return lista;
    }

    public Venda buscar(UUID id) {
        return comItens(jdbc.query("SELECT " + CAMPOS + " FROM venda WHERE id = ?", MAPA, id));
    }

    /** Busca pelo número do pedido (o que aparece na nota: #0012). */
    public Venda buscarPorNumero(int numero) {
        return comItens(jdbc.query("SELECT " + CAMPOS + " FROM venda WHERE numero = ?", MAPA, numero));
    }

    private Venda comItens(List<Venda> l) {
        if (l.isEmpty()) return null;
        Venda v = l.get(0);
        v.itens = jdbc.query(ITENS + "WHERE iv.venda_id = ? ORDER BY iv.ingrediente_nome", (rs, n) -> lerItem(rs), v.id);
        return v;
    }

    /** Grava a venda: o banco gera o id (UUID) e o número do pedido. */
    public UUID inserir(Venda v) {
        v.id = jdbc.queryForObject(
                "INSERT INTO venda (data_hora, produto_id, produto_nome, valor_venda) VALUES (?, ?, ?, ?) RETURNING id",
                UUID.class, v.dataHora, v.produtoId, v.produtoNome, v.valorVenda);
        gravarItens(v);
        return v.id;
    }

    public void alterar(Venda v) {
        jdbc.update("UPDATE venda SET produto_id = ?, produto_nome = ?, valor_venda = ? WHERE id = ?",
                v.produtoId, v.produtoNome, v.valorVenda, v.id);
        gravarItens(v);
    }

    /** Os itens são apagados junto (ON DELETE CASCADE). */
    public void excluir(UUID id) {
        jdbc.update("DELETE FROM venda WHERE id = ?", id);
    }

    private void gravarItens(Venda v) {
        jdbc.update("DELETE FROM item_venda WHERE venda_id = ?", v.id);
        List<Object[]> linhas = new ArrayList<>();
        for (ItemVenda i : v.itens)
            linhas.add(new Object[] {v.id, i.ingredienteId, i.nome, i.unidade, i.quantidade, i.custo});
        if (!linhas.isEmpty())
            jdbc.batchUpdate("INSERT INTO item_venda (venda_id, ingrediente_id, ingrediente_nome, unidade, quantidade, custo) "
                    + "VALUES (?, ?, ?, ?, ?, ?)", linhas);
    }

    private static ItemVenda lerItem(ResultSet rs) throws SQLException {
        ItemVenda i = new ItemVenda();
        i.ingredienteId = rs.getObject("ingrediente_id", UUID.class);
        i.nome = rs.getString("ingrediente_nome");
        i.unidade = rs.getString("unidade");
        i.quantidade = rs.getDouble("quantidade");
        i.custo = rs.getDouble("custo");
        return i;
    }
}
