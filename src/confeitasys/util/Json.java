package confeitasys.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Leitor e escritor de JSON bem simples, para o sistema não depender de
 * bibliotecas externas.
 *
 * Conversões:
 *   objeto JSON  -> Map<String, Object>
 *   lista JSON   -> List<Object>
 *   texto        -> String
 *   número       -> Double
 *   true/false   -> Boolean
 *   null         -> null
 */
public final class Json {

    private final String texto;
    private int pos;

    private Json(String texto) {
        this.texto = texto;
    }

    // ------------------------------------------------------------------ leitura

    public static Object ler(String texto) {
        if (texto == null || texto.isBlank()) return null;
        Json j = new Json(texto);
        j.pularEspacos();
        Object valor = j.lerValor();
        j.pularEspacos();
        if (j.pos != texto.length()) throw new IllegalArgumentException("JSON inválido");
        return valor;
    }

    private Object lerValor() {
        pularEspacos();
        if (pos >= texto.length()) throw new IllegalArgumentException("JSON incompleto");
        char c = texto.charAt(pos);
        if (c == '{') return lerObjeto();
        if (c == '[') return lerLista();
        if (c == '"') return lerTexto();
        if (texto.startsWith("true", pos)) { pos += 4; return Boolean.TRUE; }
        if (texto.startsWith("false", pos)) { pos += 5; return Boolean.FALSE; }
        if (texto.startsWith("null", pos)) { pos += 4; return null; }
        return lerNumero();
    }

    private Map<String, Object> lerObjeto() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pos++; // {
        pularEspacos();
        if (texto.charAt(pos) == '}') { pos++; return mapa; }
        while (true) {
            pularEspacos();
            String chave = lerTexto();
            pularEspacos();
            esperar(':');
            mapa.put(chave, lerValor());
            pularEspacos();
            char c = texto.charAt(pos++);
            if (c == '}') return mapa;
            if (c != ',') throw new IllegalArgumentException("JSON inválido: esperado , ou }");
        }
    }

    private List<Object> lerLista() {
        List<Object> lista = new ArrayList<>();
        pos++; // [
        pularEspacos();
        if (texto.charAt(pos) == ']') { pos++; return lista; }
        while (true) {
            lista.add(lerValor());
            pularEspacos();
            char c = texto.charAt(pos++);
            if (c == ']') return lista;
            if (c != ',') throw new IllegalArgumentException("JSON inválido: esperado , ou ]");
        }
    }

    private String lerTexto() {
        esperar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = texto.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = texto.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> sb.append(e); // \" \\ \/
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Double lerNumero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) pos++;
        if (inicio == pos) throw new IllegalArgumentException("JSON inválido");
        return Double.parseDouble(texto.substring(inicio, pos));
    }

    private void esperar(char c) {
        if (pos >= texto.length() || texto.charAt(pos) != c)
            throw new IllegalArgumentException("JSON inválido: esperado " + c);
        pos++;
    }

    private void pularEspacos() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) pos++;
    }

    // ------------------------------------------------------------------ escrita

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(valor, sb);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void escrever(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            escreverTexto(s, sb);
        } else if (v instanceof Double d) {
            if (d.isNaN() || d.isInfinite()) sb.append("0");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append(d.longValue());
            else sb.append(d);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escreverTexto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> it) {
            sb.append('[');
            boolean primeiro = true;
            for (Object o : it) {
                if (!primeiro) sb.append(',');
                primeiro = false;
                escrever(o, sb);
            }
            sb.append(']');
        } else {
            escreverTexto(v.toString(), sb);
        }
    }

    private static void escreverTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '<' -> sb.append("\\u003c");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }
}
