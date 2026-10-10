package confeitasys.dao;

import confeitasys.modelo.ItemVenda;
import confeitasys.modelo.Venda;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acesso às tabelas venda e item_venda. */
public class VendaDAO {

    private static final String CAMPOS = "id, data_hora, produto_id, produto_nome, valor_venda";

    /** Vendas de uma data (AAAA-MM-DD) ou de todas as datas (data vazia), mais recentes primeiro. */
    public List<Venda> listar(Connection con, String data) throws SQLException {
        boolean filtra = data != null && !data.isBlank();
        String sql = "SELECT " + CAMPOS + " FROM venda " + (filtra ? "WHERE substr(data_hora, 1, 10) = ? " : "") + "ORDER BY data_hora DESC, id DESC";
        List<Venda> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (filtra) ps.setString(1, data);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(ler(rs));
            }
        }
        for (Venda v : lista) v.itens = itens(con, v.id);
        return lista;
    }

    public Venda buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + CAMPOS + " FROM venda WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Venda v = ler(rs);
                v.itens = itens(con, id);
                return v;
            }
        }
    }

    public int inserir(Connection con, Venda v) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO venda (data_hora, produto_id, produto_nome, valor_venda) VALUES (?,?,?,?)")) {
            ps.setString(1, v.dataHora);
            if (v.produtoId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, v.produtoId);
            ps.setString(3, v.produtoNome);
            ps.setDouble(4, v.valorVenda);
            ps.executeUpdate();
        }
        v.id = IngredienteDAO.ultimoId(con);
        gravarItens(con, v);
        return v.id;
    }

    public void alterar(Connection con, Venda v) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE venda SET produto_id = ?, produto_nome = ?, valor_venda = ? WHERE id = ?")) {
            if (v.produtoId == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, v.produtoId);
            ps.setString(2, v.produtoNome);
            ps.setDouble(3, v.valorVenda);
            ps.setInt(4, v.id);
            ps.executeUpdate();
        }
        gravarItens(con, v);
    }

    public void excluir(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM venda WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void gravarItens(Connection con, Venda v) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM item_venda WHERE venda_id = ?")) {
            del.setInt(1, v.id);
            del.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO item_venda (venda_id, ingrediente_id, ingrediente_nome, unidade, quantidade, custo) VALUES (?,?,?,?,?,?)")) {
            for (ItemVenda i : v.itens) {
                ps.setInt(1, v.id);
                if (i.ingredienteId == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, i.ingredienteId);
                ps.setString(3, i.nome);
                ps.setString(4, i.unidade);
                ps.setDouble(5, i.quantidade);
                ps.setDouble(6, i.custo);
                ps.executeUpdate();
            }
        }
    }

    private List<ItemVenda> itens(Connection con, int vendaId) throws SQLException {
        List<ItemVenda> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT ingrediente_id, ingrediente_nome, unidade, quantidade, custo FROM item_venda WHERE venda_id = ? ORDER BY id")) {
            ps.setInt(1, vendaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemVenda i = new ItemVenda();
                    int ing = rs.getInt(1);
                    i.ingredienteId = rs.wasNull() ? null : ing;
                    i.nome = rs.getString(2);
                    i.unidade = rs.getString(3);
                    i.quantidade = rs.getDouble(4);
                    i.custo = rs.getDouble(5);
                    lista.add(i);
                }
            }
        }
        return lista;
    }

    private Venda ler(ResultSet rs) throws SQLException {
        Venda v = new Venda();
        v.id = rs.getInt("id");
        v.dataHora = rs.getString("data_hora");
        int p = rs.getInt("produto_id");
        v.produtoId = rs.wasNull() ? null : p;
        v.produtoNome = rs.getString("produto_nome");
        v.valorVenda = rs.getDouble("valor_venda");
        return v;
    }
}
