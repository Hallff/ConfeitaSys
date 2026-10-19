package br.com.confeitasys.servico;

import br.com.confeitasys.dao.UsuarioDAO;
import br.com.confeitasys.dto.CredenciaisRequisicao;
import br.com.confeitasys.dto.UsuarioAlteracaoRequisicao;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.modelo.Usuario;
import br.com.confeitasys.seguranca.Senha;
import br.com.confeitasys.util.Util;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CADASTRO 1 - Regras do cadastro de usuário (incluir, consultar, alterar, excluir). */
@Service
public class UsuarioServico {

    private final UsuarioDAO dao;

    public UsuarioServico(UsuarioDAO dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public boolean existeUsuario() {
        return dao.buscar() != null;
    }

    @Transactional(readOnly = true)
    public Usuario consultar() {
        Usuario u = dao.buscar();
        if (u == null) throw new RegraException("Usuário não encontrado.");
        return u;
    }

    /** Incluir: só é permitido quando ainda não existe usuário (primeiro acesso). */
    @Transactional
    public Usuario cadastrar(CredenciaisRequisicao d) {
        String nome = Util.obrigatorio(d.nome(), "Informe o nome de usuário.");
        String senha = d.senha() == null ? "" : d.senha();
        validarSenha(senha);
        if (dao.buscar() != null) throw new RegraException("Já existe um usuário cadastrado.");
        Usuario u = new Usuario();
        u.nome = nome;
        u.senhaHash = Senha.criptografar(senha);
        u.criadoEm = Util.agora();
        u.id = dao.inserir(u);
        return u;
    }

    /** Confere nome e senha. Devolve o usuário, ou null se estiver errado. */
    @Transactional(readOnly = true)
    public Usuario login(String nome, String senha) {
        Usuario u = dao.buscar();
        if (u == null || nome == null) return null;
        boolean senhaOk = Senha.conferir(senha, u.senhaHash);   // sempre calcula (tempo constante)
        return senhaOk && u.nome.equalsIgnoreCase(nome.trim()) ? u : null;
    }

    /** Alterar nome e/ou senha. Exige a senha atual. */
    @Transactional
    public Usuario alterar(UsuarioAlteracaoRequisicao d) {
        String nome = Util.obrigatorio(d.nome(), "Informe o nome de usuário.");
        String atual = d.senhaAtual() == null ? "" : d.senhaAtual();
        String nova = d.novaSenha() == null ? "" : d.novaSenha();
        Usuario u = consultar();
        if (!Senha.conferir(atual, u.senhaHash)) throw new RegraException("A senha atual está incorreta.");
        u.nome = nome;
        if (!nova.isEmpty()) {
            validarSenha(nova);
            u.senhaHash = Senha.criptografar(nova);
        }
        dao.alterar(u);
        return u;
    }

    /** Excluir: remove o usuário. No próximo acesso o sistema pede um novo cadastro. */
    @Transactional
    public void excluir(String senha) {
        Usuario u = consultar();
        if (!Senha.conferir(senha == null ? "" : senha, u.senhaHash)) throw new RegraException("Senha incorreta.");
        dao.excluir(u.id);
    }

    private static void validarSenha(String senha) {
        if (senha.length() < 6) throw new RegraException("A senha deve ter pelo menos 6 caracteres.");
    }
}
