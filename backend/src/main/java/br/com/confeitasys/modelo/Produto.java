package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.ProdutoResposta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** CADASTRO 3 - Produto do cardápio, com a sua receita (ingredientes). */
public class Produto {
    public UUID id;
    public String nome;        // ex.: Bolo de chocolate
    public String tamanho;     // ex.: G
    public String recheio;     // ex.: Brigadeiro
    public double precoVenda;  // preço sugerido de venda
    public List<ItemReceita> receita = new ArrayList<>();

    /** Nome completo: "Bolo de chocolate G - recheio Brigadeiro". */
    public String nomeCompleto() {
        StringBuilder sb = new StringBuilder(nome);
        if (tamanho != null && !tamanho.isBlank()) sb.append(" ").append(tamanho);
        if (recheio != null && !recheio.isBlank()) sb.append(" - recheio ").append(recheio);
        return sb.toString();
    }

    public double custoEstimado() {
        double t = 0;
        for (ItemReceita i : receita) t += i.custoEstimado();
        return t;
    }

    public ProdutoResposta paraResposta() {
        return new ProdutoResposta(id, nome, tamanho, recheio, nomeCompleto(), precoVenda,
                custoEstimado(), precoVenda - custoEstimado(),
                receita.stream().map(ItemReceita::paraResposta).toList());
    }
}
