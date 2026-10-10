package confeitasys.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Conexão com o banco de dados SQLite (arquivo dados/confeitasys.db).
 *
 * O SQLite é um banco de dados SQL completo que fica dentro de um arquivo,
 * sem precisar instalar servidor. O driver JDBC está em lib/sqlite-jdbc.jar.
 *
 * Toda operação no banco passa pelo método {@link #executar}, que:
 *  - garante que só uma operação aconteça por vez (synchronized);
 *  - abre uma TRANSAÇÃO: se der erro no meio, nada é gravado (rollback).
 */
public class Conexao {

    /** Trabalho a ser feito dentro de uma transação. */
    @FunctionalInterface
    public interface Trabalho<T> {
        T executar(Connection con) throws SQLException;
    }

    private final Path arquivo;
    private final Path pastaBackup;
    private final Connection con;

    public Conexao(Path pastaDados, Path pastaBackup) throws IOException, SQLException {
        Files.createDirectories(pastaDados);
        this.arquivo = pastaDados.resolve("confeitasys.db");
        this.pastaBackup = pastaBackup;
        this.con = DriverManager.getConnection("jdbc:sqlite:" + arquivo.toAbsolutePath());
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");   // liga as chaves estrangeiras
            st.execute("PRAGMA journal_mode = WAL");  // mais seguro contra queda de energia
        }
        criarTabelas();
    }

    /** Executa o script db/schema.sql (CREATE TABLE IF NOT EXISTS ...). */
    private void criarTabelas() throws IOException, SQLException {
        String script;
        try (InputStream in = Conexao.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) throw new IOException("schema.sql não encontrado");
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder semComentarios = new StringBuilder();
        for (String linha : script.split("\n")) {
            int i = linha.indexOf("--");
            semComentarios.append(i >= 0 ? linha.substring(0, i) : linha).append('\n');
        }
        try (Statement st = con.createStatement()) {
            for (String comando : semComentarios.toString().split(";")) {
                if (!comando.isBlank()) st.execute(comando);
            }
        }
    }

    public synchronized <T> T executar(Trabalho<T> trabalho) {
        try {
            con.setAutoCommit(false);
            try {
                T resultado = trabalho.executar(con);
                con.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(traduzir(e), e);
        }
    }

    /** Backup: copia o banco inteiro para outro arquivo (comando VACUUM INTO do SQLite). */
    public synchronized String backup() {
        try {
            Files.createDirectories(pastaBackup);
            String nome = "confeitasys-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".db";
            Path destino = pastaBackup.resolve(nome).toAbsolutePath();
            try (Statement st = con.createStatement()) {
                st.execute("VACUUM INTO '" + destino.toString().replace("'", "''") + "'");
            }
            return destino.toString();
        } catch (IOException | SQLException e) {
            throw new IllegalStateException("Falha no backup: " + e.getMessage(), e);
        }
    }

    private static String traduzir(SQLException e) {
        String msg = String.valueOf(e.getMessage());
        if (msg.contains("FOREIGN KEY")) return "Este registro está sendo usado em outro cadastro.";
        if (msg.contains("UNIQUE")) return "Já existe um registro com esses dados.";
        return "Erro no banco de dados: " + msg;
    }
}
