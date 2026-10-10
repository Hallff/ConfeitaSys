package confeitasys.dao;

import confeitasys.modelo.Ingrediente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acesso à tabela ingrediente (estoque). */
public class IngredienteDAO {

    private static final String CAMPOS =
            "id, nome, unidade, quantidade, custo_unitario, quantidade_alerta, validade, atualizado_em";

    public List<Ingrediente> listar(Connection con) throws SQLException {
        List<Ingrediente> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT " + CAMPOS + " FROM ingrediente ORDER BY nome COLLATE NOCASE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(ler(rs));
        }
        return lista;
    }

    public Ingrediente buscar(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + CAMPOS + " FROM ingrediente WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ler(rs) : null;
            }
        }
    }

    public Ingrediente buscarPorNome(Connection con, String nome) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + CAMPOS + " FROM ingrediente WHERE nome = ? COLLATE NOCASE")) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? ler(rs) : null;
            }
        }
    }

    public int inserir(Connection con, Ingrediente i) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO ingrediente (nome, unidade, quantidade, custo_unitario, quantidade_alerta, validade, atualizado_em) VALUES (?,?,?,?,?,?,?)")) {
            preencher(ps, i);
            ps.executeUpdate();
        }
        return ultimoId(con);
    }

    public void alterar(Connection con, Ingrediente i) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE ingrediente SET nome=?, unidade=?, quantidade=?, custo_unitario=?, quantidade_alerta=?, validade=?, atualizado_em=? WHERE id=?")) {
            preencher(ps, i);
            ps.setInt(8, i.id);
            ps.executeUpdate();
        }
    }

    /** Soma (ou subtrai, se negativo) uma quantidade no estoque. */
    public void movimentar(Connection con, int id, double delta, String quando) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE ingrediente SET quantidade = ROUND(quantidade + ?, 3), atualizado_em = ? WHERE id = ?")) {
            ps.setDouble(1, delta);
            ps.setString(2, quando);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /** Nomes dos produtos cuja receita usa este ingrediente. */
    public List<String> produtosQueUsam(Connection con, int id) throws SQLException {
        List<String> nomes = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT p.nome, p.tamanho FROM produto_ingrediente pi JOIN produto p ON p.id = pi.produto_id WHERE pi.ingrediente_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) nomes.add((rs.getString(1) + " " + rs.getString(2)).trim());
            }
        }
        return nomes;
    }

    public void excluir(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM ingrediente WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private void preencher(PreparedStatement ps, Ingrediente i) throws SQLException {
        ps.setString(1, i.nome);
        ps.setString(2, i.unidade);
        ps.setDouble(3, i.quantidade);
        ps.setDouble(4, i.custoUnitario);
        ps.setDouble(5, i.quantidadeAlerta);
        if (i.validade == null) ps.setNull(6, Types.VARCHAR); else ps.setString(6, i.validade);
        ps.setString(7, i.atualizadoEm);
    }

    private Ingrediente ler(ResultSet rs) throws SQLException {
        Ingrediente i = new Ingrediente();
        i.id = rs.getInt("id");
        i.nome = rs.getString("nome");
        i.unidade = rs.getString("unidade");
        i.quantidade = rs.getDouble("quantidade");
        i.custoUnitario = rs.getDouble("custo_unitario");
        i.quantidadeAlerta = rs.getDouble("quantidade_alerta");
        i.validade = rs.getString("validade");
        i.atualizadoEm = rs.getString("atualizado_em");
        return i;
    }

    static int ultimoId(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT last_insert_rowid()"); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
