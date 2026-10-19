package br.com.confeitasys.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ao iniciar, confere se o PostgreSQL está acessível e se as tabelas existem,
 * e mostra no console os endereços da API e do Swagger.
 */
@Component
public class VerificacaoInicial implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(VerificacaoInicial.class);

    private final JdbcTemplate jdbc;
    private final int porta;

    public VerificacaoInicial(JdbcTemplate jdbc, @Value("${server.port:8080}") int porta) {
        this.jdbc = jdbc;
        this.porta = porta;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Integer ingredientes = jdbc.queryForObject("SELECT COUNT(*) FROM ingrediente", Integer.class);
            Integer produtos = jdbc.queryForObject("SELECT COUNT(*) FROM produto", Integer.class);
            LOG.info("Banco PostgreSQL conectado: {} ingrediente(s), {} produto(s) cadastrados.", ingredientes, produtos);
            // As chaves primárias são UUID. Um banco criado com a versão antiga (ids numéricos) não funciona.
            String tipoId = jdbc.queryForObject("SELECT data_type FROM information_schema.columns "
                    + "WHERE table_schema = current_schema() AND table_name = 'produto' AND column_name = 'id'", String.class);
            if (!"uuid".equalsIgnoreCase(tipoId))
                LOG.error("\n\n  *** BANCO NA VERSAO ANTIGA (ids numericos) ***\n"
                        + "  Esta versao usa UUID nas chaves primarias. Rode banco/criar-banco.bat de novo\n"
                        + "  (responda S para continuar): as tabelas sao recriadas no formato novo.\n");
        } catch (Exception e) {
            LOG.error("\n\n  *** NAO FOI POSSIVEL USAR O BANCO DE DADOS ***\n"
                    + "  Motivo: {}\n"
                    + "  Confira: 1) o PostgreSQL esta ligado;  2) o banco foi criado (banco/criar-banco.bat);\n"
                    + "           3) usuario/senha em src/main/resources/application.properties.\n", e.getMessage());
        }
        LOG.info("\n\n  ConfeitaSys API no ar!\n"
                + "  API ........ http://localhost:{}/api/status\n"
                + "  Swagger .... http://localhost:{}/swagger-ui.html\n", porta, porta);
    }
}
