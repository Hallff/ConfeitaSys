package confeitasys;

import confeitasys.controle.Servidor;
import confeitasys.dao.Conexao;

import java.awt.Desktop;
import java.io.IOException;
import java.net.BindException;
import java.net.URI;
import java.nio.file.Path;

/**
 * Ponto de entrada do ConfeitaSys.
 *
 * Liga o servidor em http://localhost:8080 e abre o navegador na tela de login.
 * Os dados ficam nas pastas ao lado do programa:
 *   dados/       -> banco de dados SQLite (confeitasys.db)
 *   backup/      -> cópias de segurança
 *   relatorios/  -> relatórios de fechamento do dia
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Path base = Path.of(System.getProperty("confeitasys.pasta", ".")).toAbsolutePath().normalize();
        Conexao banco = new Conexao(base.resolve("dados"), base.resolve("backup"));

        int portaInicial = Integer.parseInt(System.getProperty("confeitasys.porta", "8080"));
        Servidor servidor = null;
        for (int porta = portaInicial; porta < portaInicial + 10 && servidor == null; porta++) {
            try {
                servidor = new Servidor(banco, base.resolve("relatorios"), porta);
            } catch (BindException e) {
                System.out.println("Porta " + porta + " ocupada, tentando a próxima...");
            }
        }
        if (servidor == null) throw new IOException("Nenhuma porta livre entre " + portaInicial + " e " + (portaInicial + 9));
        servidor.iniciar();

        String endereco = "http://localhost:" + servidor.porta() + "/";
        System.out.println("==============================================");
        System.out.println("  ConfeitaSys rodando em " + endereco);
        System.out.println("  Banco de dados: " + base.resolve("dados").resolve("confeitasys.db"));
        System.out.println("  Para encerrar, feche esta janela (ou Ctrl+C)");
        System.out.println("==============================================");

        if (!Boolean.getBoolean("confeitasys.semNavegador")) abrirNavegador(endereco);
    }

    private static void abrirNavegador(String endereco) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(endereco));
                return;
            }
        } catch (Exception ignorado) {
            // cai no plano B abaixo
        }
        try {
            String so = System.getProperty("os.name").toLowerCase();
            if (so.contains("win")) new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", endereco).start();
            else if (so.contains("mac")) new ProcessBuilder("open", endereco).start();
            else new ProcessBuilder("xdg-open", endereco).start();
        } catch (Exception e) {
            System.out.println("Abra o navegador e acesse: " + endereco);
        }
    }
}
