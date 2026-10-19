package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.VendaResposta;
import br.com.confeitasys.util.Util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** MOVIMENTO - Registro de venda (pedido). id = chave (UUID); numero = nº do pedido na nota. */
public class Venda {
    public UUID id;
    public int numero;          // nº do pedido (1, 2, 3...), gerado pelo banco
    public LocalDateTime dataHora;
    public UUID produtoId;      // null se o produto foi excluído do cardápio depois
    public String produtoNome;
    public double valorVenda;
    public List<ItemVenda> itens = new ArrayList<>();

    public double custo() {
        double t = 0;
        for (ItemVenda i : itens) t += i.custo;
        return t;
    }

    public double lucro() { return valorVenda - custo(); }

    public String data() { return Util.iso(dataHora).substring(0, 10); }

    public String hora() { return Util.iso(dataHora).substring(11, 16); }

    public VendaResposta paraResposta() {
        return new VendaResposta(id, numero, Util.iso(dataHora), data(), hora(), produtoId, produtoNome,
                valorVenda, custo(), lucro(), itens.stream().map(ItemVenda::paraResposta).toList());
    }
}
