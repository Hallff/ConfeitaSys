package br.com.confeitasys.controle;

import br.com.confeitasys.dto.CredenciaisRequisicao;
import br.com.confeitasys.dto.ErroResposta;
import br.com.confeitasys.dto.OkResposta;
import br.com.confeitasys.dto.StatusResposta;
import br.com.confeitasys.dto.TokenResposta;
import br.com.confeitasys.modelo.Usuario;
import br.com.confeitasys.seguranca.Sessoes;
import br.com.confeitasys.servico.UsuarioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================
 *  GRUPO 1 - ACESSO (login)                       APIs públicas
 * ===================================================================
 *  1.1  POST /api/login    entrar (devolve o token)
 *  1.2  POST /api/logout   sair
 *  1.3  GET  /api/status   situação do sistema (primeiro acesso? logado?)
 *
 *  Frontend: frontend/js/api.js -> API.acesso.*
 *  Regras:   servico/UsuarioServico.java + seguranca/Sessoes.java
 */
@RestController
@Tag(name = "1. Acesso (login)", description = "Situação do sistema, entrar e sair. Rotas públicas (não precisam de token).")
public class AcessoControle {

    private final UsuarioServico usuarios;
    private final Sessoes sessoes;

    public AcessoControle(UsuarioServico usuarios, Sessoes sessoes) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
    }

    /**
     * API 1.1 - ENTRAR
     * Tela: Login -> botão "Entrar"
     * Frontend: API.acesso.login(nome, senha)
     */
    @PostMapping("/api/login")
    @SecurityRequirements
    @Operation(operationId = "entrar", summary = "1.1 Entrar (login)",
            description = "**Tela:** Login → botão **Entrar**  \n"
                    + "**Frontend:** `API.acesso.login()`  \n"
                    + "**Backend:** `controle/AcessoControle.java` → `UsuarioServico.login()` → `UsuarioDAO.buscar()`  \n"
                    + "**Tabela:** usuario  \n\n"
                    + "Confere nome e senha (PBKDF2) e devolve um **token** válido por 12 horas. "
                    + "Após 5 senhas erradas seguidas, o login fica bloqueado por 60 segundos.")
    @ApiResponse(responseCode = "200", description = "Login realizado",
            content = @Content(schema = @Schema(implementation = TokenResposta.class)))
    @ApiResponse(responseCode = "401", description = "Usuário ou senha incorretos",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    @ApiResponse(responseCode = "429", description = "Muitas tentativas erradas",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    public ResponseEntity<Object> login(@RequestBody CredenciaisRequisicao d) {
        long espera = sessoes.segundosBloqueado();
        if (espera > 0)
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new ErroResposta("Muitas tentativas erradas. Aguarde " + espera + " segundos."));
        Usuario u = d == null ? null : usuarios.login(d.nome(), d.senha());
        if (u == null) {
            sessoes.registrarFalha();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErroResposta("Usuário ou senha incorretos."));
        }
        sessoes.registrarSucesso();
        return ResponseEntity.ok(new TokenResposta(true, sessoes.criar(), u.nome));
    }

    /**
     * API 1.2 - SAIR
     * Tela: menu lateral -> "Sair"
     * Frontend: API.acesso.logout()
     */
    @PostMapping("/api/logout")
    @SecurityRequirements
    @Operation(operationId = "sair", summary = "1.2 Sair (logout)",
            description = "**Tela:** menu lateral → **Sair**  \n"
                    + "**Frontend:** `API.acesso.logout()`  \n"
                    + "**Backend:** `controle/AcessoControle.java` → `Sessoes.encerrar()`  \n\n"
                    + "Invalida o token enviado.")
    public OkResposta logout(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
        sessoes.encerrar(Sessoes.tokenDoCabecalho(authorization));
        return OkResposta.OK;
    }

    /**
     * API 1.3 - SITUAÇÃO DO SISTEMA
     * Tela: Login (ao abrir) decide entre "primeiro acesso" e "entrar"; telas internas conferem se o login vale.
     * Frontend: API.acesso.status()
     */
    @GetMapping("/api/status")
    @SecurityRequirements
    @Operation(operationId = "statusSistema", summary = "1.3 Situação do sistema",
            description = "**Tela:** Login (ao abrir) e todas as telas internas (ao carregar).  \n"
                    + "**Frontend:** `API.acesso.status()` em `frontend/js/api.js`  \n"
                    + "**Backend:** `controle/AcessoControle.java` → `UsuarioServico.existeUsuario()` → `UsuarioDAO.buscar()`  \n"
                    + "**Tabela:** usuario  \n\n"
                    + "Informa se já existe usuário (senão é o primeiro acesso) e se o token enviado ainda é válido.")
    public StatusResposta status(
            @Parameter(hidden = true) @RequestHeader(name = "Authorization", required = false) String authorization) {
        boolean logado = sessoes.valido(Sessoes.tokenDoCabecalho(authorization));
        boolean temUsuario = usuarios.existeUsuario();
        String nome = logado && temUsuario ? usuarios.consultar().nome : null;
        return new StatusResposta(temUsuario, logado, nome);
    }
}
