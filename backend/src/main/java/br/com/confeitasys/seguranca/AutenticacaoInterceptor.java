package br.com.confeitasys.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * Confere o login antes de qualquer rota /api/**.
 * Sem token válido, responde 401 e a requisição não chega ao controle.
 */
@Component
public class AutenticacaoInterceptor implements HandlerInterceptor {

    private static final byte[] NAO_AUTORIZADO =
            "{\"erro\":\"Sessão expirada. Faça login novamente.\"}".getBytes(StandardCharsets.UTF_8);

    private final Sessoes sessoes;

    public AutenticacaoInterceptor(Sessoes sessoes) {
        this.sessoes = sessoes;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String metodo = req.getMethod();
        if ("OPTIONS".equals(metodo) || rotaPublica(metodo, req.getRequestURI())) return true;

        String token = Sessoes.tokenDoCabecalho(req.getHeader("Authorization"));
        if (sessoes.valido(token)) return true;

        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getOutputStream().write(NAO_AUTORIZADO);
        return false;
    }

    /** Rotas que funcionam sem login. */
    private static boolean rotaPublica(String metodo, String caminho) {
        String c = caminho.endsWith("/") ? caminho.substring(0, caminho.length() - 1) : caminho;
        return ("GET".equals(metodo) && c.equals("/api/status"))
                || ("POST".equals(metodo) && (c.equals("/api/login") || c.equals("/api/logout") || c.equals("/api/usuario")));
    }
}
