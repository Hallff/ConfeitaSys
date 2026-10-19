package br.com.confeitasys.seguranca;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Criptografia da senha do usuário.
 *
 * A senha NUNCA é gravada como texto. Usamos o algoritmo PBKDF2 com HMAC-SHA256
 * (recomendado pelo NIST e pela OWASP para guardar senhas), que já vem dentro
 * do Java (javax.crypto), sem biblioteca externa.
 *
 *  1. Sorteamos um "salt" (16 bytes aleatórios) para cada senha.
 *  2. Passamos senha + salt pelo PBKDF2 210.000 vezes (deixa ataques lentos).
 *  3. Gravamos: pbkdf2_sha256$iterações$salt$hash  (salt e hash em Base64).
 *
 * Para conferir o login, repetimos o cálculo com o mesmo salt e comparamos.
 */
public final class Senha {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_SALT = 16;  // bytes
    private static final int TAMANHO_HASH = 256; // bits
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private Senha() {}

    public static String criptografar(String senha) {
        byte[] salt = new byte[TAMANHO_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] hash = pbkdf2(senha.toCharArray(), salt, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return "pbkdf2_sha256$" + ITERACOES + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
    }

    public static boolean conferir(String senhaDigitada, String guardado) {
        if (senhaDigitada == null || guardado == null) return false;
        String[] partes = guardado.split("\\$");
        if (partes.length != 4 || !partes[0].equals("pbkdf2_sha256")) return false;
        int iteracoes = Integer.parseInt(partes[1]);
        byte[] salt = Base64.getDecoder().decode(partes[2]);
        byte[] esperado = Base64.getDecoder().decode(partes[3]);
        byte[] calculado = pbkdf2(senhaDigitada.toCharArray(), salt, iteracoes);
        return MessageDigest.isEqual(esperado, calculado); // tempo constante
    }

    private static byte[] pbkdf2(char[] senha, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(senha, salt, iteracoes, TAMANHO_HASH);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao criptografar a senha", e);
        }
    }
}
