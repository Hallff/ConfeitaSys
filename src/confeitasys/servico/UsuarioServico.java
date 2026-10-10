package confeitasys.servico;

import confeitasys.dao.Conexao;
import confeitasys.dao.UsuarioDAO;
import confeitasys.modelo.Usuario;
import confeitasys.util.RegraException;
import confeitasys.util.Senha;
import confeitasys.util.Util;

import java.util.Map;

/** CADASTRO 1 - Regras do cadastro de usuário (incluir, consultar, alterar, excluir). */
public class UsuarioServico {

    private final Conexao banco;
    private final UsuarioDAO dao = new UsuarioDAO();

    public UsuarioServico(Conexao banco) { this.banco = banco; }

    public boolean existeUsuario() {
        return banco.executar(con -> dao.buscar(con) != null);
    }

    public Usuario consultar() {
        return banco.executar(dao::buscar);
    }

    /** Incluir: só é permitido quando ainda não existe usuário (primeiro acesso). */
    public void cadastrar(Map<String, Object> d) {
        String nome = Util.obrigatorio(d, "nome", "Informe o nome de usuário.");
        String senha = d.get("senha") == null ? "" : d.get("senha").toString();
        validarSenha(senha);
        banco.executar(con -> {
            if (dao.buscar(con) != null) throw new RegraException("Já existe um usuário cadastrado.");
            Usuario u = new Usuario();
            u.nome = nome;
            u.senhaHash = Senha.criptografar(senha);
            u.criadoEm = Util.agora();
            dao.inserir(con, u);
            return null;
        });
    }

    public boolean login(String nome, String senha) {
        Usuario u = consultar();
        if (u == null || nome == null) return false;
        boolean senhaOk = Senha.conferir(senha, u.senhaHash);   // sempre calcula (tempo constante)
        return senhaOk && u.nome.equalsIgnoreCase(nome.trim());
    }

    /** Alterar nome e/ou senha. Exige a senha atual. */
    public Usuario alterar(Map<String, Object> d) {
        String nome = Util.obrigatorio(d, "nome", "Informe o nome de usuário.");
        String atual = d.get("senhaAtual") == null ? "" : d.get("senhaAtual").toString();
        String nova = d.get("novaSenha") == null ? "" : d.get("novaSenha").toString();
        return banco.executar(con -> {
            Usuario u = dao.buscar(con);
            if (u == null) throw new RegraException("Usuário não encontrado.");
            if (!Senha.conferir(atual, u.senhaHash)) throw new RegraException("A senha atual está incorreta.");
            u.nome = nome;
            if (!nova.isEmpty()) {
                validarSenha(nova);
                u.senhaHash = Senha.criptografar(nova);
            }
            dao.alterar(con, u);
            return u;
        });
    }

    /** Excluir: remove o usuário. Na próxima abertura o sistema pede um novo cadastro. */
    public void excluir(Map<String, Object> d) {
        String senha = d.get("senha") == null ? "" : d.get("senha").toString();
        banco.executar(con -> {
            Usuario u = dao.buscar(con);
            if (u == null) throw new RegraException("Usuário não encontrado.");
            if (!Senha.conferir(senha, u.senhaHash)) throw new RegraException("Senha incorreta.");
            dao.excluir(con, u.id);
            return null;
        });
    }

    private static void validarSenha(String senha) {
        if (senha.length() < 6) throw new RegraException("A senha deve ter pelo menos 6 caracteres.");
    }
}
