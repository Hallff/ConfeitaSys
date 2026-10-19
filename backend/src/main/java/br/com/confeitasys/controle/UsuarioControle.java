package br.com.confeitasys.controle;

import br.com.confeitasys.dto.CredenciaisRequisicao;
import br.com.confeitasys.dto.OkResposta;
import br.com.confeitasys.dto.SenhaRequisicao;
import br.com.confeitasys.dto.TokenResposta;
import br.com.confeitasys.dto.UsuarioAlteracaoRequisicao;
import br.com.confeitasys.dto.UsuarioResposta;
import br.com.confeitasys.modelo.Usuario;
import br.com.confeitasys.seguranca.Sessoes;
import br.com.confeitasys.servico.UsuarioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================
 *  GRUPO 2 - CADASTRO 1: USUÁRIO
 * ===================================================================
 *  2.1  GET    /api/usuario   consultar
 *  2.2  PUT    /api/usuario   alterar nome/senha
 *  2.3  POST   /api/usuario   cadastrar (primeiro acesso)  - pública
 *  2.4  DELETE /api/usuario   excluir
 *
 *  Frontend: frontend/js/api.js -> API.usuario.*
 *  Regras:   servico/UsuarioServico.java      SQL: dao/UsuarioDAO.java      Tabela: usuario
 */
@RestController
@Tag(name = "2. Cadastro 1 - Usuário", description = "Incluir (primeiro acesso), consultar, alterar e excluir o usuário do sistema")
public class UsuarioControle {

    private final UsuarioServico usuarios;
    private final Sessoes sessoes;

    public UsuarioControle(UsuarioServico usuarios, Sessoes sessoes) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
    }

    /**
     * API 2.1 - CONSULTAR USUÁRIO
     * Tela: Conta (ao abrir)
     * Frontend: API.usuario.consultar()
     */
    @GetMapping("/api/usuario")
    @Operation(operationId = "consultarUsuario", summary = "2.1 Consultar usuário",
            description = "**Tela:** Conta (ao abrir)  \n"
                    + "**Frontend:** `API.usuario.consultar()`  \n"
                    + "**Backend:** `controle/UsuarioControle.java` → `UsuarioServico.consultar()` → `UsuarioDAO.buscar()`  \n"
                    + "**Tabela:** usuario")
    public UsuarioResposta consultar() {
        return usuarios.consultar().paraResposta();
    }

    /**
     * API 2.2 - ALTERAR USUÁRIO
     * Tela: Conta -> botão "Salvar alterações"
     * Frontend: API.usuario.alterar(dados)
     */
    @PutMapping("/api/usuario")
    @Operation(operationId = "alterarUsuario", summary = "2.2 Alterar nome e/ou senha",
            description = "**Tela:** Conta → botão **Salvar alterações**  \n"
                    + "**Frontend:** `API.usuario.alterar()`  \n"
                    + "**Backend:** `controle/UsuarioControle.java` → `UsuarioServico.alterar()` → `UsuarioDAO.alterar()`  \n"
                    + "**Tabela:** usuario  \n\n"
                    + "Exige a senha atual. Deixe `novaSenha` vazia para manter a senha.")
    public UsuarioResposta alterar(@RequestBody UsuarioAlteracaoRequisicao d) {
        return usuarios.alterar(d == null ? new UsuarioAlteracaoRequisicao(null, null, null) : d).paraResposta();
    }

    /**
     * API 2.3 - CADASTRAR USUÁRIO (primeiro acesso)
     * Tela: Login (primeiro acesso) -> botão "Criar usuário e entrar"
     * Frontend: API.usuario.cadastrar(nome, senha)
     */
    @PostMapping("/api/usuario")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(operationId = "cadastrarUsuario", summary = "2.3 Cadastrar usuário (primeiro acesso)",
            description = "**Tela:** Login (primeiro acesso) → botão **Criar usuário e entrar**  \n"
                    + "**Frontend:** `API.usuario.cadastrar()`  \n"
                    + "**Backend:** `controle/UsuarioControle.java` → `UsuarioServico.cadastrar()` → `UsuarioDAO.inserir()`  \n"
                    + "**Tabela:** usuario  \n\n"
                    + "Só funciona enquanto não existe usuário. Senha com no mínimo 6 caracteres (gravada criptografada). "
                    + "Já devolve o token de acesso.")
    public TokenResposta cadastrar(@RequestBody CredenciaisRequisicao d) {
        Usuario u = usuarios.cadastrar(d == null ? new CredenciaisRequisicao(null, null) : d);
        return new TokenResposta(true, sessoes.criar(), u.nome);
    }

    /**
     * API 2.4 - EXCLUIR USUÁRIO
     * Tela: Conta -> botão "Excluir usuário"
     * Frontend: API.usuario.excluir(senha)
     */
    @DeleteMapping("/api/usuario")
    @Operation(operationId = "excluirUsuario", summary = "2.4 Excluir usuário",
            description = "**Tela:** Conta → botão **Excluir usuário**  \n"
                    + "**Frontend:** `API.usuario.excluir()`  \n"
                    + "**Backend:** `controle/UsuarioControle.java` → `UsuarioServico.excluir()` → `UsuarioDAO.excluir()`  \n"
                    + "**Tabela:** usuario  \n\n"
                    + "Pede a senha e encerra todos os logins. No próximo acesso o sistema pede um novo cadastro; "
                    + "vendas, produtos e estoque continuam guardados.")
    public OkResposta excluir(@RequestBody SenhaRequisicao d) {
        usuarios.excluir(d == null ? null : d.senha());
        sessoes.encerrarTodas();
        return OkResposta.OK;
    }
}
