package br.com.confeitasys.dao;

import br.com.confeitasys.modelo.Usuario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Acesso à tabela usuario. */
@Repository
public class UsuarioDAO {

    private final JdbcTemplate jdbc;

    public UsuarioDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Usuario> MAPA = (rs, n) -> {
        Usuario u = new Usuario();
        u.id = rs.getObject("id", UUID.class);
        u.nome = rs.getString("nome");
        u.senhaHash = rs.getString("senha_hash");
        u.criadoEm = rs.getObject("criado_em", LocalDateTime.class);
        return u;
    };

    /** O sistema é monousuário: devolve o único usuário (ou null no primeiro acesso). */
    public Usuario buscar() {
        List<Usuario> l = jdbc.query("SELECT id, nome, senha_hash, criado_em FROM usuario ORDER BY criado_em LIMIT 1", MAPA);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Grava e devolve o id (UUID) gerado pelo banco. */
    public UUID inserir(Usuario u) {
        return jdbc.queryForObject(
                "INSERT INTO usuario (nome, senha_hash, criado_em) VALUES (?, ?, ?) RETURNING id",
                UUID.class, u.nome, u.senhaHash, u.criadoEm);
    }

    public void alterar(Usuario u) {
        jdbc.update("UPDATE usuario SET nome = ?, senha_hash = ? WHERE id = ?", u.nome, u.senhaHash, u.id);
    }

    public void excluir(UUID id) {
        jdbc.update("DELETE FROM usuario WHERE id = ?", id);
    }
}
