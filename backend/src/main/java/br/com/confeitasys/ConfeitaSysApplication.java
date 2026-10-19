package br.com.confeitasys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API do ConfeitaSys.
 *
 * Camadas (mesma organização dos diagramas de classes):
 *   controle  -> recebe as requisições HTTP (API REST) e devolve JSON
 *   servico   -> regras de negócio e transações
 *   dao       -> SQL no PostgreSQL (JdbcTemplate)
 *   modelo    -> objetos do domínio (Usuario, Ingrediente, Produto, Venda...)
 *   dto       -> formatos de entrada/saída da API (documentados no Swagger)
 */
@SpringBootApplication
public class ConfeitaSysApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfeitaSysApplication.class, args);
    }
}
