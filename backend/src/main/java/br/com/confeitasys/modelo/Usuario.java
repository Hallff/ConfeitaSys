package br.com.confeitasys.modelo;

import br.com.confeitasys.dto.UsuarioResposta;
import br.com.confeitasys.util.Util;

import java.time.LocalDateTime;
import java.util.UUID;

/** CADASTRO 1 - Usuário do sistema (dono/gerente da confeitaria). */
public class Usuario {
    public UUID id;
    public String nome;
    public String senhaHash;   // senha criptografada (PBKDF2), nunca o texto da senha
    public LocalDateTime criadoEm;

    /** Dados enviados para a tela (sem o hash da senha). */
    public UsuarioResposta paraResposta() {
        return new UsuarioResposta(id, nome, Util.iso(criadoEm));
    }
}
