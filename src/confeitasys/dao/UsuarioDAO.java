package confeitasys.dao;

import confeitasys.modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Acesso à tabela usuario. */
public class UsuarioDAO {

    public Usuario buscar(Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT id, nome, senha_hash, criado_em FROM usuario ORDER BY id LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            Usuario u = new Usuario();
            u.id = rs.getInt("id");
            u.nome = rs.getString("nome");
            u.senhaHash = rs.getString("senha_hash");
            u.criadoEm = rs.getString("criado_em");
            return u;
        }
    }

    public void inserir(Connection con, Usuario u) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO usuario (nome, senha_hash, criado_em) VALUES (?, ?, ?)")) {
            ps.setString(1, u.nome);
            ps.setString(2, u.senhaHash);
            ps.setString(3, u.criadoEm);
            ps.executeUpdate();
        }
    }

    public void alterar(Connection con, Usuario u) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("UPDATE usuario SET nome = ?, senha_hash = ? WHERE id = ?")) {
            ps.setString(1, u.nome);
            ps.setString(2, u.senhaHash);
            ps.setInt(3, u.id);
            ps.executeUpdate();
        }
    }

    public void excluir(Connection con, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM usuario WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
