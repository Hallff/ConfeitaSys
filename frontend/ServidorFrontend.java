import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ConfeitaSys - servidor das TELAS (frontend).
 *
 * Só entrega os arquivos desta pasta (HTML, CSS, JS, imagens) em
 * http://localhost:5500. Ele NÃO acessa o banco: as telas chamam a API
 * (backend Spring Boot, porta 8080), que é quem fala com o PostgreSQL.
 *
 * Rodar (Java 17, sem compilar):   java ServidorFrontend.java
 * ou dois cliques em iniciar-frontend.bat
 */
public class ServidorFrontend {

    public static void main(String[] args) throws IOException {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 5500;
        Path raiz = Path.of("").toAbsolutePath().normalize();
        HttpServer servidor = HttpServer.create(new InetSocketAddress("localhost", porta), 0);
        servidor.createContext("/", ex -> entregar(ex, raiz));
        servidor.start();
        System.out.println();
        System.out.println("  ConfeitaSys - telas (frontend) no ar: http://localhost:" + porta);
        System.out.println("  Pasta: " + raiz);
        System.out.println("  A API (backend) precisa estar rodando em http://localhost:8080");
        System.out.println("  Para parar: feche esta janela ou aperte Ctrl+C.");
    }

    private static void entregar(HttpExchange ex, Path raiz) throws IOException {
        try {
            String caminho = ex.getRequestURI().getPath();
            if (caminho.equals("/")) caminho = "/index.html";
            Path arquivo = raiz.resolve(caminho.substring(1)).normalize();
            if (!arquivo.startsWith(raiz) || !Files.isRegularFile(arquivo) || caminho.endsWith(".java") || caminho.endsWith(".bat")) {
                byte[] msg = "Página não encontrada".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(404, msg.length);
                ex.getResponseBody().write(msg);
                return;
            }
            byte[] dados = Files.readAllBytes(arquivo);
            ex.getResponseHeaders().set("Content-Type", tipo(caminho));
            ex.getResponseHeaders().set("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, dados.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(dados);
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
}
