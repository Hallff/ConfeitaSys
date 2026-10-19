package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.IngredienteResposta;
import br.com.confeitasys.util.Util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * CADASTRO 2 - Ingrediente (item do estoque): farinha, leite, chocolate...
 * A quantidade fica em quilogramas (KG) ou litros (L).
 */
public class Ingrediente {
    public static final int DIAS_AVISO_VALIDADE = 3;

    public UUID id;
    public String nome;
    public String unidade;          // "KG" ou "L"
    public double quantidade;       // quanto há no estoque agora
    public double custoUnitario;    // R$ por KG ou L
    public double quantidadeAlerta; // definido pelo usuário para cada item
    public LocalDate validade;      // pode ser nula
    public LocalDateTime atualizadoEm;

    public double valorEmEstoque() { return quantidade * custoUnitario; }

    /** Estoque baixo: chegou na quantidade de alerta informada pelo usuário (ou zerou). */
    public boolean estoqueBaixo() {
        return quantidade <= 0 || quantidade <= quantidadeAlerta;
    }

    /** Dias até vencer (negativo = vencido). null se não tem validade. */
    public Long diasParaVencer() {
        if (validade == null) return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), validade);
    }

    public boolean vencido() { Long d = diasParaVencer(); return d != null && d < 0; }

    public boolean venceLogo() { Long d = diasParaVencer(); return d != null && d >= 0 && d <= DIAS_AVISO_VALIDADE; }

    public IngredienteResposta paraResposta() {
        IngredienteResposta r = new IngredienteResposta();
        r.id = id;
        r.nome = nome;
        r.unidade = unidade;
        r.quantidade = quantidade;
        r.custoUnitario = custoUnitario;
        r.quantidadeAlerta = quantidadeAlerta;
        r.validade = Util.iso(validade);
        r.atualizadoEm = Util.iso(atualizadoEm);
        r.valorEmEstoque = valorEmEstoque();
        r.estoqueBaixo = estoqueBaixo();
        r.diasParaVencer = diasParaVencer();
        r.vencido = vencido();
        r.venceLogo = venceLogo();
        return r;
    }
}
