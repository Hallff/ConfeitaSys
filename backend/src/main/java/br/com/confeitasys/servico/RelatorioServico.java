package br.com.confeitasys.servico;

import br.com.confeitasys.dto.ConsumoResposta;
import br.com.confeitasys.dto.IngredienteResposta;
import br.com.confeitasys.dto.RelatorioEstoqueResposta;
import br.com.confeitasys.dto.RelatorioFaltandoResposta;
import br.com.confeitasys.dto.RelatorioVendasResposta;
import br.com.confeitasys.dto.VendaResposta;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.modelo.Ingrediente;
import br.com.confeitasys.modelo.ItemVenda;
import br.com.confeitasys.modelo.Venda;
import br.com.confeitasys.util.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relatórios (opcionais): vendas do dia, estoque e itens em falta. */
@Service
public class RelatorioServico {

    private final VendaServico vendas;
    private final IngredienteServico ingredientes;
    private final Path pasta;

    public RelatorioServico(VendaServico vendas, IngredienteServico ingredientes,
                            @Value("${confeitasys.pasta-relatorios:relatorios}") String pasta) {
        this.vendas = vendas;
        this.ingredientes = ingredientes;
        this.pasta = Path.of(pasta);
    }

    @Transactional(readOnly = true)
    public RelatorioVendasResposta vendasDoDia(String data) {
        String dia = Util.texto(data).isEmpty() ? LocalDate.now().toString() : Util.data(data, "data").toString();
        List<Venda> lista = new ArrayList<>(vendas.pesquisar(dia, ""));
        lista.sort(Comparator.comparing((Venda v) -> v.dataHora));
        double faturamento = 0, custo = 0;
        Map<String, double[]> consumo = new LinkedHashMap<>();
        Map<String, String[]> nomes = new LinkedHashMap<>();
        List<VendaResposta> linhas = new ArrayList<>();
        for (Venda v : lista) {
            faturamento += v.valorVenda;
            custo += v.custo();
            linhas.add(v.paraResposta());
            for (ItemVenda i : v.itens) {
                String chave = i.nome + "|" + i.unidade;
                nomes.putIfAbsent(chave, new String[] {i.nome, i.unidade});
                double[] t = consumo.computeIfAbsent(chave, k -> new double[2]);
                t[0] += i.quantidade;
                t[1] += i.custo;
            }
        }
        List<ConsumoResposta> usados = new ArrayList<>();
        for (Map.Entry<String, double[]> e : consumo.entrySet()) {
            String[] n = nomes.get(e.getKey());
            usados.add(new ConsumoResposta(n[0], n[1], e.getValue()[0], e.getValue()[1]));
        }
        return new RelatorioVendasResposta(dia, Util.iso(Util.agora()), linhas, lista.size(),
                faturamento, custo, faturamento - custo, usados);
    }

    @Transactional(readOnly = true)
    public RelatorioEstoqueResposta estoque() {
        List<IngredienteResposta> itens = new ArrayList<>();
        double total = 0;
        for (Ingrediente i : ingredientes.listar()) {
            itens.add(i.paraResposta());
            total += i.valorEmEstoque();
        }
        return new RelatorioEstoqueResposta(Util.iso(Util.agora()), itens, total);
    }

    @Transactional(readOnly = true)
    public RelatorioFaltandoResposta faltando() {
        List<IngredienteResposta> itens = new ArrayList<>();
        for (Ingrediente i : ingredientes.listar()) {
            if (!i.estoqueBaixo() && !i.vencido()) continue;
            IngredienteResposta r = i.paraResposta();
            double comprar = Math.max(0, i.quantidadeAlerta - i.quantidade);
            if (i.vencido()) comprar = Math.max(i.quantidade, i.quantidadeAlerta); // repor o que venceu
            r.sugestaoCompra = comprar;
            r.custoEstimado = comprar * i.custoUnitario;
            itens.add(r);
        }
        return new RelatorioFaltandoResposta(Util.iso(Util.agora()), itens);
    }

    /** Salva os relatórios (HTML montado pela tela) em relatorios/relatorios-AAAA-MM-DD.html. */
    public String salvarHtml(String data, String html) {
        String dia = Util.texto(data);
        if (!dia.matches("\\d{4}-\\d{2}-\\d{2}")) throw new RegraException("Data inválida.");
        try {
            Files.createDirectories(pasta);
            Path arq = pasta.resolve("relatorios-" + dia + ".html");
            Files.writeString(arq, html == null ? "" : html, StandardCharsets.UTF_8);
            return arq.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível salvar o relatório: " + e.getMessage(), e);
        }
    }
}
