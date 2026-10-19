package br.com.confeitasys.dao;

import br.com.confeitasys.modelo.Ingrediente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Acesso à tabela ingrediente (estoque). */
@Repository
public class IngredienteDAO {

    private static final String CAMPOS =
            "id, nome, unidade, quantidade, custo_unitario, quantidade_alerta, validade, atualizado_em";

    private static final RowMapper<Ingrediente> MAPA = (rs, n) -> {
        Ingrediente i = new Ingrediente();
        i.id = rs.getObject("id", UUID.class);
        i.nome = rs.getString("nome");
        i.unidade = rs.getString("unidade");
        i.quantidade = rs.getDouble("quantidade");
        i.custoUnitario = rs.getDouble("custo_unitario");
        i.quantidadeAlerta = rs.getDouble("quantidade_alerta");
        i.validade = rs.getObject("validade", LocalDate.class);
        i.atualizadoEm = rs.getObject("atualizado_em", LocalDateTime.class);
        return i;
    };

    private final JdbcTemplate jdbc;

    public IngredienteDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Ingrediente> listar() {
        return jdbc.query("SELECT " + CAMPOS + " FROM ingrediente ORDER BY LOWER(nome)", MAPA);
    }

    public Ingrediente buscar(UUID id) {
        return primeiro(jdbc.query("SELECT " + CAMPOS + " FROM ingrediente WHERE id = ?", MAPA, id));
    }

    /**
     * Busca e TRAVA a linha até o fim da transação (SELECT ... FOR UPDATE).
     * Usado nas vendas: duas vendas ao mesmo tempo não conseguem
     * gastar o mesmo estoque.
     */
    public Ingrediente buscarParaAtualizar(UUID id) {
        return primeiro(jdbc.query("SELECT " + CAMPOS + " FROM ingrediente WHERE id = ? FOR UPDATE", MAPA, id));
    }

    public Ingrediente buscarPorNome(String nome) {
        return primeiro(jdbc.query("SELECT " + CAMPOS + " FROM ingrediente WHERE LOWER(nome) = LOWER(?)", MAPA, nome));
    }

    /** Grava e devolve o id (UUID) gerado pelo banco. */
    public UUID inserir(Ingrediente i) {
        return jdbc.queryForObject(
                "INSERT INTO ingrediente (nome, unidade, quantidade, custo_unitario, quantidade_alerta, validade, atualizado_em) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id",
                UUID.class,
                i.nome, i.unidade, i.quantidade, i.custoUnitario, i.quantidadeAlerta, i.validade, i.atualizadoEm);
    }

    public void alterar(Ingrediente i) {
        jdbc.update("UPDATE ingrediente SET nome = ?, unidade = ?, quantidade = ?, custo_unitario = ?, "
                        + "quantidade_alerta = ?, validade = ?, atualizado_em = ? WHERE id = ?",
                i.nome, i.unidade, i.quantidade, i.custoUnitario, i.quantidadeAlerta, i.validade, i.atualizadoEm, i.id);
    }

    /** Soma (ou subtrai, se negativo) uma quantidade no estoque. */
    public void movimentar(UUID id, double delta, LocalDateTime quando) {
        jdbc.update("UPDATE ingrediente SET quantidade = quantidade + CAST(? AS NUMERIC), atualizado_em = ? WHERE id = ?",
                delta, quando, id);
    }

    /** Nomes dos produtos cuja receita usa este ingrediente. */
    public List<String> produtosQueUsam(UUID id) {
        return jdbc.query(
                "SELECT TRIM(p.nome || ' ' || p.tamanho) AS nome FROM produto_ingrediente pi "
                        + "JOIN produto p ON p.id = pi.produto_id WHERE pi.ingrediente_id = ? ORDER BY LOWER(p.nome)",
                (rs, n) -> rs.getString("nome"), id);
    }

    public void excluir(UUID id) {
        jdbc.update("DELETE FROM ingrediente WHERE id = ?", id);
    }

    private static <T> T primeiro(List<T> lista) {
        return lista.isEmpty() ? null : lista.get(0);
    }
}
