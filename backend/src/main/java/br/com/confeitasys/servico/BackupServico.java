package br.com.confeitasys.servico;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Backup (cópia de segurança) do banco de dados.
 *
 * Gera um arquivo .sql em backup/ com todos os dados (comandos INSERT).
 * Para restaurar:  psql -U confeitasys -d confeitasys -f backup\confeitasys-AAAAMMDD-HHMMSS.sql
 * (não precisa do pg_dump instalado no PATH).
 */
@Service
public class BackupServico {

    /** Ordem respeita as chaves estrangeiras (pais antes dos filhos). {tabela, ORDER BY} */
    private static final String[][] TABELAS = {
            {"usuario", "criado_em"},
            {"ingrediente", "nome"},
            {"produto", "nome, tamanho, recheio"},
            {"produto_ingrediente", "produto_id, ingrediente_id"},
            {"venda", "numero"},
            {"item_venda", "venda_id, ingrediente_nome"},
    };

    private final JdbcTemplate jdbc;
    private final Path pasta;

    public BackupServico(JdbcTemplate jdbc, @Value("${confeitasys.pasta-backup:backup}") String pasta) {
        this.jdbc = jdbc;
        this.pasta = Path.of(pasta);
    }

    /** Lê tudo numa única transação (foto consistente) e grava o arquivo. Devolve o caminho. */
    @Transactional(readOnly = true)
    public String fazerBackup() {
        List<String> linhas = new ArrayList<>();
        linhas.add("-- ConfeitaSys - backup gerado em " + LocalDateTime.now().withNano(0));
        linhas.add("-- Restaurar: psql -U confeitasys -d confeitasys -f <este arquivo>");
        linhas.add("-- ATENÇÃO: apaga os dados atuais e coloca os deste backup.");
        linhas.add("BEGIN;");
        linhas.add("TRUNCATE item_venda, venda, produto_ingrediente, produto, ingrediente, usuario RESTART IDENTITY;");
        for (String[] t : TABELAS) {
            String tabela = t[0];
            jdbc.query("SELECT * FROM " + tabela + " ORDER BY " + t[1], rs -> {
                ResultSetMetaData md = rs.getMetaData();
                StringBuilder colunas = new StringBuilder();
                for (int c = 1; c <= md.getColumnCount(); c++) colunas.append(c > 1 ? ", " : "").append(md.getColumnName(c));
                StringBuilder valores = new StringBuilder();
                for (int c = 1; c <= md.getColumnCount(); c++) valores.append(c > 1 ? ", " : "").append(literal(rs.getObject(c)));
                linhas.add("INSERT INTO " + tabela + " (" + colunas + ") VALUES (" + valores + ");");
            });
        }
        // Os ids são UUID (vêm prontos no INSERT). Só o nº do pedido usa contador:
        // ajusta para a próxima venda continuar do número certo.
        linhas.add("SELECT setval(pg_get_serial_sequence('venda', 'numero'), COALESCE((SELECT MAX(numero) FROM venda), 0) + 1, false);");
        linhas.add("COMMIT;");

        try {
            Files.createDirectories(pasta);
            String nome = "confeitasys-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".sql";
            Path arq = pasta.resolve(nome);
            Files.write(arq, linhas, StandardCharsets.UTF_8);
            return arq.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha no backup: " + e.getMessage(), e);
        }
    }

    /** Valor Java -> literal SQL. */
    static String literal(Object v) {
        if (v == null) return "NULL";
        if (v instanceof BigDecimal b) return b.toPlainString();
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        // texto, data e UUID vão entre aspas ('3fa85f64-...' é convertido para UUID pelo banco)
        return "'" + v.toString().replace("'", "''") + "'";
    }
}
