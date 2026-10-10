package confeitasys.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

/** Funções de apoio usadas pelos serviços. */
public final class Util {
    private Util() {}

    public static String agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /** "Pão de Queijo" -> "pao de queijo" (para buscas sem acento e sem maiúsculas). */
    public static String semAcento(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    public static String texto(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        return v == null ? "" : v.toString().trim().replaceAll("\\s+", " ");
    }

    public static String obrigatorio(Map<String, Object> m, String campo, String mensagem) {
        String s = texto(m, campo);
        if (s.isEmpty()) throw new RegraException(mensagem);
        return s;
    }

    /** Lê número aceitando 12.5 ou "12,50". */
    public static double numero(Map<String, Object> m, String campo) {
        Object v = m.get(campo);
        if (v == null || v.toString().isBlank()) return 0;
        if (v instanceof Number n) return n.doubleValue();
        String s = v.toString().trim();
        if (s.contains(",")) s = s.replace(".", "").replace(',', '.');
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new RegraException("Valor numérico inválido no campo " + campo + ".");
        }
    }

    /** Valida data AAAA-MM-DD; devolve null se vazia. */
    public static String data(Map<String, Object> m, String campo) {
        String s = texto(m, campo);
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s).toString();
        } catch (DateTimeParseException e) {
            throw new RegraException("Data inválida no campo " + campo + ".");
        }
    }

    public static double arred(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }

    public static String fmtQtd(double qtdBase, String unidade) {
        Locale br = Locale.forLanguageTag("pt-BR");
        if (qtdBase >= 1) return String.format(br, "%.3f %s", qtdBase, "KG".equals(unidade) ? "kg" : "L").replaceAll(",?0+ ", " ");
        return String.format(br, "%.0f %s", qtdBase * 1000, "KG".equals(unidade) ? "g" : "ml");
    }
}
