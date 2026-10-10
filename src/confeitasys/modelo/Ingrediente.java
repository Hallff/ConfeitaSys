package confeitasys.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CADASTRO 2 - Ingrediente (item do estoque): farinha, leite, chocolate...
 * A quantidade fica em quilogramas (KG) ou litros (L).
 */
public class Ingrediente {
    public static final int DIAS_AVISO_VALIDADE = 3;

    public int id;
    public String nome;
    public String unidade;          // "KG" ou "L"
    public double quantidade;       // quanto há no estoque agora
    public double custoUnitario;    // R$ por KG ou L
    public double quantidadeAlerta; // definido pelo usuário para cada item
    public String validade;         // AAAA-MM-DD (pode ser vazio)
    public String atualizadoEm;

    public double valorEmEstoque() { return quantidade * custoUnitario; }

    /** Estoque baixo: chegou na quantidade de alerta informada pelo usuário (ou zerou). */
    public boolean estoqueBaixo() {
        return quantidade <= 0 || quantidade <= quantidadeAlerta;
    }

    /** Dias até vencer (negativo = vencido). null se não tem validade. */
    public Long diasParaVencer() {
        if (validade == null || validade.isBlank()) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(validade));
    }

    public boolean vencido() { Long d = diasParaVencer(); return d != null && d < 0; }

    public boolean venceLogo() { Long d = diasParaVencer(); return d != null && d >= 0 && d <= DIAS_AVISO_VALIDADE; }

    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nome", nome);
        m.put("unidade", unidade);
        m.put("quantidade", quantidade);
        m.put("custoUnitario", custoUnitario);
        m.put("quantidadeAlerta", quantidadeAlerta);
        m.put("validade", validade);
        m.put("atualizadoEm", atualizadoEm);
        m.put("valorEmEstoque", valorEmEstoque());
        m.put("estoqueBaixo", estoqueBaixo());
        m.put("diasParaVencer", diasParaVencer());
        m.put("vencido", vencido());
        m.put("venceLogo", venceLogo());
        return m;
    }
}
