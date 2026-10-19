package br.com.confeitasys.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controle de login por TOKEN.
 *
 * Ao entrar, o usuário recebe um token aleatório (64 caracteres) que vale
 * algumas horas. O frontend (e o Swagger) envia esse token em toda
 * requisição, no cabeçalho:  Authorization: Bearer &lt;token&gt;
 *
 * Também bloqueia o login por 60 segundos depois de 5 senhas erradas seguidas.
 */
@Component
public class Sessoes {

    private static final int MAX_FALHAS = 5;
    private static final int SEGUNDOS_BLOQUEIO = 60;

    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Instant> tokens = new ConcurrentHashMap<>();
    private final long horas;

    private int falhasSeguidas = 0;
    private Instant bloqueadoAte = Instant.EPOCH;

    public Sessoes(@Value("${confeitasys.sessao.horas:12}") long horas) {
        this.horas = horas;
    }

    /** Cria um token novo, válido pelo número de horas configurado. */
    public String criar() {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        String token = HexFormat.of().formatHex(b);
        tokens.put(token, Instant.now().plus(horas, ChronoUnit.HOURS));
        return token;
    }

    public boolean valido(String token) {
        if (token == null || token.isBlank()) return false;
        Instant expira = tokens.get(token);
        if (expira == null) return false;
        if (Instant.now().isAfter(expira)) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public void encerrar(String token) {
        if (token != null) tokens.remove(token);
    }

    public void encerrarTodas() {
        tokens.clear();
    }

    /** Segundos que ainda faltam de bloqueio (0 = liberado). */
    public synchronized long segundosBloqueado() {
        Instant agora = Instant.now();
        return agora.isBefore(bloqueadoAte) ? ChronoUnit.SECONDS.between(agora, bloqueadoAte) + 1 : 0;
    }

    public synchronized void registrarFalha() {
        if (++falhasSeguidas >= MAX_FALHAS) {
            bloqueadoAte = Instant.now().plusSeconds(SEGUNDOS_BLOQUEIO);
            falhasSeguidas = 0;
        }
    }

    public synchronized void registrarSucesso() {
        falhasSeguidas = 0;
    }

    /** "Bearer abc123" -> "abc123" (ou null). */
    public static String tokenDoCabecalho(String authorization) {
        if (authorization == null) return null;
        String a = authorization.trim();
        if (a.regionMatches(true, 0, "Bearer ", 0, 7)) return a.substring(7).trim();
        return a.isEmpty() ? null : a;
    }
}
