package br.com.confeitasys.util;

import br.com.confeitasys.excecao.RegraException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Funções de apoio usadas pelos serviços. */
public final class Util {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Locale BR = Locale.forLanguageTag("pt-BR");

    private Util() {}

    /** Data e hora atual, sem frações de segundo. */
    public static LocalDateTime agora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /** 2026-10-15T14:30:00 (formato usado pelo frontend). */
    public static String iso(LocalDateTime dh) {
        return dh == null ? null : dh.format(DATA_HORA);
    }

    /** 2026-10-15 (ou null). */
    public static String iso(LocalDate d) {
        return d == null ? null : d.toString();
    }

    /** "Pão de Queijo" -> "pao de queijo" (para buscas sem acento e sem maiúsculas). */
    public static String semAcento(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    /** Texto sem espaços sobrando ("" se nulo). */
    public static String texto(String s) {
        return s == null ? "" : s.trim().replaceAll("\\s+", " ");
    }

    public static String obrigatorio(String s, String mensagem) {
        String t = texto(s);
        if (t.isEmpty()) throw new RegraException(mensagem);
        return t;
    }

    public static double numero(Double d) {
        return d == null ? 0 : d;
    }

    /** Valida data AAAA-MM-DD; devolve null se vazia. */
    public static LocalDate data(String s, String campo) {
        String t = texto(s);
        if (t.isEmpty()) return null;
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new RegraException("Data inválida no campo " + campo + " (use AAAA-MM-DD).");
        }
    }

    public static double arred(double v, int casas) {
        double f = Math.pow(10, casas);
        return Math.round(v * f) / f;
    }

    /** 1.5 KG -> "1,5 kg"; 0.25 KG -> "250 g"; 0.3 L -> "300 ml". */
    public static String fmtQtd(double qtdBase, String unidade) {
        boolean kg = "KG".equals(unidade);
        if (qtdBase >= 1) return String.format(BR, "%.3f %s", qtdBase, kg ? "kg" : "L").replaceAll(",?0+ ", " ");
        return String.format(BR, "%.0f %s", qtdBase * 1000, kg ? "g" : "ml");
    }

    /** 2026-10-15 -> 15/10/2026 */
    public static String br(LocalDate d) {
        return d == null ? "" : d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
