package confeitasys.controle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import confeitasys.dao.Conexao;
import confeitasys.modelo.Ingrediente;
import confeitasys.modelo.Produto;
import confeitasys.modelo.Venda;
import confeitasys.servico.IngredienteServico;
import confeitasys.servico.ProdutoServico;
import confeitasys.servico.RelatorioServico;
import confeitasys.servico.UsuarioServico;
import confeitasys.servico.VendaServico;
import confeitasys.util.Json;
import confeitasys.util.RegraException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * CONTROLE - API REST do ConfeitaSys e entrega das telas (HTML/CSS/JS).
 *
 * Recebe as requisições do navegador, confere o login, chama a camada de
 * serviço e devolve a resposta em JSON.
 *
 *   Autenticação  GET /api/status | POST /api/login | POST /api/logout
 *   Usuário       POST /api/usuario (primeiro acesso) | GET | PUT | DELETE /api/usuario
 *   Ingredientes  GET | POST /api/ingredientes      PUT | DELETE /api/ingredientes/{id}
 *   Produtos      GET | POST /api/produtos          GET | PUT | DELETE /api/produtos/{id}
 *   Vendas        GET /api/vendas?data=&busca=      POST /api/vendas
 *                 GET | PUT /api/vendas/{id}        DELETE /api/vendas/{id}?devolver=sim|nao
 *   Alertas       GET /api/alertas
 *   Relatórios    GET /api/relatorios/vendas?data= | /estoque | /faltando   POST /api/relatorios/salvar
 *   Backup        POST /api/backup
 */
public class Servidor {

    private static final String COOKIE = "CONFEITASYS_SESSAO";
    private static final long HORAS_SESSAO = 12;

    private final Conexao banco;
    private final UsuarioServico usuarios;
    private final IngredienteServico ingredientes;
    private final ProdutoServico produtos;
    private final VendaServico vendas;
    private final RelatorioServico relatorios;
    private final Path pastaRelatorios;
    private final HttpServer http;
    private final SecureRandom aleatorio = new SecureRandom();
    private final Map<String, Instant> sessoes = new ConcurrentHashMap<>();
    private int falhasSeguidas = 0;
    private Instant bloqueadoAte = Instant.EPOCH;

    public Servidor(Conexao banco, Path pastaRelatorios, int porta) throws IOException {
        this.banco = banco;
        this.usuarios = new UsuarioServico(banco);
        this.ingredientes = new IngredienteServico(banco);
        this.produtos = new ProdutoServico(banco);
        this.vendas = new VendaServico(banco);
        this.relatorios = new RelatorioServico(vendas, ingredientes);
        this.pastaRelatorios = pastaRelatorios;
        this.http = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0); // só este computador
        http.setExecutor(Executors.newFixedThreadPool(8));
        http.createContext("/api/", this::tratarApi);
        http.createContext("/", this::tratarArquivo);
    }

    public void iniciar() { http.start(); }

    public int porta() { return http.getAddress().getPort(); }

    // ================================================================ roteamento da API

    private void tratarApi(HttpExchange ex) throws IOException {
        try {
            String metodo = ex.getRequestMethod();
            String[] p = ex.getRequestURI().getPath().substring(1).split("/"); // ["api","vendas","12"]
            String recurso = p.length > 1 ? p[1] : "";
            String sub = p.length > 2 ? p[2] : "";
            Integer id = sub.matches("\\d+") ? Integer.valueOf(sub) : null;
            Map<String, String> q = parametros(ex.getRequestURI().getRawQuery());

            // ---- rotas públicas
            if (recurso.equals("status") && metodo.equals("GET")) {
                Map<String, Object> r = new LinkedHashMap<>();
                boolean logado = logado(ex);
                r.put("temUsuario", usuarios.existeUsuario());
                r.put("logado", logado);
                r.put("usuario", logado && usuarios.consultar() != null ? usuarios.consultar().nome : null);
                responder(ex, 200, r);
                return;
            }
            if (recurso.equals("usuario") && metodo.equals("POST")) {       // primeiro acesso
                usuarios.cadastrar(corpo(ex));
                criarSessao(ex);
                responder(ex, 201, Map.of("ok", true));
                return;
            }
            if (recurso.equals("login") && metodo.equals("POST")) { login(ex); return; }
            if (recurso.equals("logout") && metodo.equals("POST")) {
                encerrarSessao(ex);
                responder(ex, 200, Map.of("ok", true));
                return;
            }

            // ---- daqui para baixo precisa estar logado
            if (!logado(ex)) { responder(ex, 401, Map.of("erro", "Sessão expirada. Faça login novamente.")); return; }

            switch (recurso) {
                case "usuario" -> rotasUsuario(ex, metodo);
                case "ingredientes" -> rotasIngredientes(ex, metodo, id);
                case "produtos" -> rotasProdutos(ex, metodo, id);
                case "vendas" -> rotasVendas(ex, metodo, id, q);
                case "alertas" -> responder(ex, 200, ingredientes.alertas());
                case "relatorios" -> rotasRelatorios(ex, metodo, sub, q);
                case "backup" -> responder(ex, 200, Map.of("ok", true, "arquivo", banco.backup()));
                default -> responder(ex, 404, Map.of("erro", "Rota não encontrada."));
            }
        } catch (RegraException | IllegalStateException e) {
            responder(ex, 400, Map.of("erro", e.getMessage()));
        } catch (IllegalArgumentException | ClassCastException e) {
            responder(ex, 400, Map.of("erro", "Dados inválidos enviados para o servidor."));
        } catch (Exception e) {
            e.printStackTrace();
            responder(ex, 500, Map.of("erro", "Erro interno: " + e.getMessage()));
        } finally {
            ex.close();
        }
    }

    private void rotasUsuario(HttpExchange ex, String metodo) throws IOException {
        switch (metodo) {
            case "GET" -> responder(ex, 200, usuarios.consultar().paraTela());
            case "PUT" -> responder(ex, 200, usuarios.alterar(corpo(ex)).paraTela());
            case "DELETE" -> {
                usuarios.excluir(corpo(ex));
                sessoes.clear();
                encerrarSessao(ex);
                responder(ex, 200, Map.of("ok", true));
            }
            default -> responder(ex, 405, Map.of("erro", "Operação não permitida."));
        }
    }

    private void rotasIngredientes(HttpExchange ex, String metodo, Integer id) throws IOException {
        if (id == null && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Ingrediente i : ingredientes.listar()) l.add(i.paraTela());
            responder(ex, 200, l);
        } else if (id == null && metodo.equals("POST")) responder(ex, 201, ingredientes.incluir(corpo(ex)).paraTela());
        else if (id != null && metodo.equals("PUT")) responder(ex, 200, ingredientes.alterar(id, corpo(ex)).paraTela());
        else if (id != null && metodo.equals("DELETE")) { ingredientes.excluir(id); responder(ex, 200, Map.of("ok", true)); }
        else responder(ex, 405, Map.of("erro", "Operação não permitida."));
    }

    private void rotasProdutos(HttpExchange ex, String metodo, Integer id) throws IOException {
        if (id == null && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Produto p : produtos.listar()) l.add(p.paraTela());
            responder(ex, 200, l);
        } else if (id == null && metodo.equals("POST")) responder(ex, 201, produtos.incluir(corpo(ex)).paraTela());
        else if (id != null && metodo.equals("GET")) responder(ex, 200, produtos.consultar(id).paraTela());
        else if (id != null && metodo.equals("PUT")) responder(ex, 200, produtos.alterar(id, corpo(ex)).paraTela());
        else if (id != null && metodo.equals("DELETE")) { produtos.excluir(id); responder(ex, 200, Map.of("ok", true)); }
        else responder(ex, 405, Map.of("erro", "Operação não permitida."));
    }

    private void rotasVendas(HttpExchange ex, String metodo, Integer id, Map<String, String> q) throws IOException {
        if (id == null && metodo.equals("GET")) {
            List<Object> l = new ArrayList<>();
            for (Venda v : vendas.pesquisar(q.get("data"), q.get("busca"))) l.add(v.paraTela());
            responder(ex, 200, l);
        } else if (id == null && metodo.equals("POST")) responder(ex, 201, vendas.registrar(corpo(ex)).paraTela());
        else if (id != null && metodo.equals("GET")) responder(ex, 200, vendas.consultar(id).paraTela());
        else if (id != null && metodo.equals("PUT")) responder(ex, 200, vendas.editar(id, corpo(ex)).paraTela());
        else if (id != null && metodo.equals("DELETE")) {
            vendas.excluir(id, !"nao".equals(q.get("devolver")));
            responder(ex, 200, Map.of("ok", true));
        } else responder(ex, 405, Map.of("erro", "Operação não permitida."));
    }

    private void rotasRelatorios(HttpExchange ex, String metodo, String tipo, Map<String, String> q) throws IOException {
        String data = q.getOrDefault("data", LocalDate.now().toString());
        switch (tipo) {
            case "vendas" -> responder(ex, 200, relatorios.vendasDoDia(data));
            case "estoque" -> responder(ex, 200, relatorios.estoque());
            case "faltando" -> responder(ex, 200, relatorios.faltando());
            case "salvar" -> {
                if (!metodo.equals("POST")) { responder(ex, 405, Map.of("erro", "Use POST.")); return; }
                Map<String, Object> d = corpo(ex);
                String dia = String.valueOf(d.get("data"));
                if (!dia.matches("\\d{4}-\\d{2}-\\d{2}")) throw new RegraException("Data inválida.");
                Files.createDirectories(pastaRelatorios);
                Path arq = pastaRelatorios.resolve("relatorios-" + dia + ".html");
                Files.writeString(arq, String.valueOf(d.get("html")), StandardCharsets.UTF_8);
                responder(ex, 200, Map.of("ok", true, "arquivo", arq.toAbsolutePath().toString(), "backup", banco.backup()));
            }
            default -> responder(ex, 404, Map.of("erro", "Relatório não encontrado."));
        }
    }

    // ================================================================ login / sessão

    private synchronized void login(HttpExchange ex) throws IOException {
        Instant agora = Instant.now();
        if (agora.isBefore(bloqueadoAte)) {
            long seg = ChronoUnit.SECONDS.between(agora, bloqueadoAte) + 1;
            responder(ex, 429, Map.of("erro", "Muitas tentativas erradas. Aguarde " + seg + " segundos."));
            return;
        }
        Map<String, Object> d = corpo(ex);
        if (usuarios.login(String.valueOf(d.get("nome")), String.valueOf(d.get("senha")))) {
            falhasSeguidas = 0;
            criarSessao(ex);
            responder(ex, 200, Map.of("ok", true));
        } else {
            if (++falhasSeguidas >= 5) { bloqueadoAte = agora.plusSeconds(60); falhasSeguidas = 0; }
            responder(ex, 401, Map.of("erro", "Usuário ou senha incorretos."));
        }
    }

    private void criarSessao(HttpExchange ex) {
        byte[] b = new byte[32];
        aleatorio.nextBytes(b);
        String token = HexFormat.of().formatHex(b);
        sessoes.put(token, Instant.now().plus(HORAS_SESSAO, ChronoUnit.HOURS));
        ex.getResponseHeaders().add("Set-Cookie",
                COOKIE + "=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=" + (HORAS_SESSAO * 3600));
    }

    private void encerrarSessao(HttpExchange ex) {
        String t = token(ex);
        if (t != null) sessoes.remove(t);
        ex.getResponseHeaders().add("Set-Cookie", COOKIE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
    }

    private boolean logado(HttpExchange ex) {
        String t = token(ex);
        if (t == null) return false;
        Instant expira = sessoes.get(t);
        if (expira == null) return false;
        if (Instant.now().isAfter(expira)) { sessoes.remove(t); return false; }
        return true;
    }

    private String token(HttpExchange ex) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String linha : cookies)
            for (String c : linha.split(";")) {
                String[] kv = c.trim().split("=", 2);
                if (kv.length == 2 && kv[0].equals(COOKIE)) return kv[1];
            }
        return null;
    }

    // ================================================================ arquivos das telas

    private void tratarArquivo(HttpExchange ex) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            if (caminho.equals("/")) caminho = "/login.html";
            if (caminho.contains("..")) { ex.sendResponseHeaders(400, -1); return; }
            try (InputStream in = Servidor.class.getResourceAsStream("/web" + caminho)) {
                if (in == null) {
                    byte[] msg = "Página não encontrada".getBytes(StandardCharsets.UTF_8);
                    ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    ex.sendResponseHeaders(404, msg.length);
                    ex.getResponseBody().write(msg);
                    return;
                }
                byte[] dados = in.readAllBytes();
                ex.getResponseHeaders().set("Content-Type", tipo(caminho));
                ex.getResponseHeaders().set("Cache-Control", "no-cache");
                ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                ex.sendResponseHeaders(200, dados.length);
                ex.getResponseBody().write(dados);
            }
        } finally {
            ex.close();
        }
    }

    private static String tipo(String c) {
        if (c.endsWith(".html")) return "text/html; charset=utf-8";
        if (c.endsWith(".css")) return "text/css; charset=utf-8";
        if (c.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (c.endsWith(".svg")) return "image/svg+xml";
        if (c.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    // ================================================================ utilidades HTTP

    private static void responder(HttpExchange ex, int status, Object corpo) throws IOException {
        byte[] dados = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, dados.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(dados); }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> corpo(HttpExchange ex) throws IOException {
        String texto = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Object o = Json.ler(texto);
        if (o == null) return new HashMap<>();
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new IllegalArgumentException("Corpo deve ser um objeto JSON");
    }

    private static Map<String, String> parametros(String query) {
        Map<String, String> m = new HashMap<>();
        if (query == null) return m;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return m;
    }
}
