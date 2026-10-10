package confeitasys.util;

/** Erro de regra de negócio: a mensagem é mostrada para o usuário na tela. */
public class RegraException extends RuntimeException {
    public RegraException(String mensagem) { super(mensagem); }
}
