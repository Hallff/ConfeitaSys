package confeitasys.dao;

import confeitasys.modelo.ItemReceita;
import confeitasys.modelo.Produto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acesso às tabelas produto e produto_ingrediente (receita). */
public class ProdutoDAO {

    public List<Produto> listar(Connection con) throws SQLException {
        List<Produto> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT id, nome, tamanho, recheio, preco_venda FROM produto ORDER BY nome COLLATE NOCASE, tamanho, recheio");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(ler(rs));
        }
        for (Produto p : lista) p.receita = receita(con, p.id);
        return lista;
    }

    public Produto buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, nome, tamanho, recheio, preco_venda FROM produto WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Produto p = ler(rs);
                p.receita = receita(con, id);
                return p;
            }
        }
    }

    public boolean existe(Connection con, Produto p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM produto WHERE nome = ? AND tamanho = ? AND recheio = ? AND id <> ?")) {
            ps.setString(1, p.nome);
            ps.setString(2, p.tamanho);
            ps.setString(3, p.recheio);
            ps.setInt(4, p.id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public int inserir(Connection con, Produto p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO produto (nome, tamanho, recheio, preco_venda) VALUES (?,?,?,?)")) {
            ps.setString(1, p.nome);
            ps.setString(2, p.tamanho);
            ps.setString(3, p.recheio);
            ps.setDouble(4, p.precoVenda);
            ps.executeUpdate();
        }
        p.id = IngredienteDAO.ultimoId(con);
        gravarReceita(con, p);
        return p.id;
    }

    public void alterar(Connection con, Produto p) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE produto SET nome=?, tamanho=?, recheio=?, preco_venda=? WHERE id=?")) {
            ps.setString(1, p.nome);
            ps.setString(2, p.tamanho);
            ps.setString(3, p.recheio);
            ps.setDouble(4, p.precoVenda);
            ps.setInt(5, p.id);
            ps.executeUpdate();
        }
        gravarReceita(con, p);
    }

    public void excluir(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM produto WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /** Apaga a receita antiga e grava a nova. */
    private void gravarReceita(Connection con, Produto p) throws SQLException {
        try (PreparedStatement del = con.prepareStatement("DELETE FROM produto_ingrediente WHERE produto_id = ?")) {
            del.setInt(1, p.id);
            del.executeUpdate();
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO produto_ingrediente (produto_id, ingrediente_id, quantidade) VALUES (?,?,?)")) {
            for (ItemReceita i : p.receita) {
                ps.setInt(1, p.id);
                ps.setInt(2, i.ingredienteId);
                ps.setDouble(3, i.quantidade);
                ps.executeUpdate();
            }
        }
    }

    private List<ItemReceita> receita(Connection con, int produtoId) throws SQLException {
        List<ItemReceita> itens = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT pi.ingrediente_id, i.nome, i.unidade, pi.quantidade, i.custo_unitario " +
                "FROM produto_ingrediente pi JOIN ingrediente i ON i.id = pi.ingrediente_id " +
                "WHERE pi.produto_id = ? ORDER BY i.nome COLLATE NOCASE")) {
            ps.setInt(1, produtoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemReceita i = new ItemReceita();
                    i.ingredienteId = rs.getInt(1);
                    i.ingredienteNome = rs.getString(2);
                    i.unidade = rs.getString(3);
                    i.quantidade = rs.getDouble(4);
                    i.custoUnitario = rs.getDouble(5);
                    itens.add(i);
                }
            }
        }
        return itens;
    }

    private Produto ler(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.id = rs.getInt("id");
        p.nome = rs.getString("nome");
        p.tamanho = rs.getString("tamanho");
        p.recheio = rs.getString("recheio");
        p.precoVenda = rs.getDouble("preco_venda");
        return p;
    }
}
