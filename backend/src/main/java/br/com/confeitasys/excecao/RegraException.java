package br.com.confeitasys.excecao;

/** Erro de regra de negócio. A mensagem é mostrada ao usuário (HTTP 400). */
public class RegraException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RegraException(String mensagem) {
        super(mensagem);
    }
}
