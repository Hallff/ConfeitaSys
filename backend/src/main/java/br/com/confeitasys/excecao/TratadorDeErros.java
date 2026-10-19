package br.com.confeitasys.excecao;

import br.com.confeitasys.dto.ErroResposta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Transforma qualquer erro em JSON {"erro": "mensagem"} com o código HTTP certo,
 * no mesmo formato que o frontend espera. Estas respostas também aparecem no Swagger.
 */
@RestControllerAdvice
public class TratadorDeErros {

    private static final Logger LOG = LoggerFactory.getLogger(TratadorDeErros.class);

    /** Regra de negócio (ex.: estoque insuficiente, campo obrigatório) */
    @ExceptionHandler(RegraException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta regra(RegraException e) {
        return new ErroResposta(e.getMessage());
    }

    /** JSON malformado ou com tipo errado (ex.: texto onde era número) */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta dadosInvalidos(Exception e) {
        if (e instanceof MethodArgumentTypeMismatchException m && m.getRequiredType() == java.util.UUID.class)
            return new ErroResposta("Id inválido: o id é um UUID (ex.: 3fa85f64-5717-4562-b3fc-2c963f66afa6). Copie o id da lista.");
        return new ErroResposta("Dados inválidos enviados para o servidor.");
    }

    /** Violação de UNIQUE, FOREIGN KEY ou CHECK no banco */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErroResposta integridade(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) return new ErroResposta("Já existe um registro com esses dados.");
        String msg = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
        if (msg.contains("foreign key")) return new ErroResposta("Este registro está sendo usado em outro cadastro.");
        if (msg.contains("check constraint")) return new ErroResposta("Valor fora do permitido pelo banco de dados.");
        return new ErroResposta("Os dados não respeitam as regras do banco de dados.");
    }

    /** PostgreSQL desligado, banco inexistente ou senha errada */
    @ExceptionHandler(CannotGetJdbcConnectionException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErroResposta semConexao(CannotGetJdbcConnectionException e) {
        LOG.error("Sem conexão com o PostgreSQL: {}", e.getMostSpecificCause().getMessage());
        return new ErroResposta("Não foi possível conectar ao banco de dados PostgreSQL. Verifique se ele está ligado.");
    }

    /** Tabela inexistente etc. */
    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResposta sql(BadSqlGrammarException e) {
        LOG.error("Erro de SQL", e);
        return new ErroResposta("Erro no banco de dados. As tabelas foram criadas (banco/02_tabelas.sql)? Detalhe: "
                + e.getMostSpecificCause().getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErroResposta metodo(HttpRequestMethodNotSupportedException e) {
        return new ErroResposta("Operação não permitida.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErroResposta naoEncontrado(NoResourceFoundException e) {
        return new ErroResposta("Rota não encontrada.");
    }

    /** Qualquer outro erro */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErroResposta outro(Exception e) {
        LOG.error("Erro inesperado", e);
        return new ErroResposta("Erro interno: " + e.getMessage());
    }
}
