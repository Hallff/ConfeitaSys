package confeitasys.modelo;

import java.util.LinkedHashMap;
import java.util.Map;

/** CADASTRO 1 - Usuário do sistema (dono/gerente da padaria). */
public class Usuario {
    public int id;
    public String nome;
    public String senhaHash;   // senha criptografada (PBKDF2), nunca o texto da senha
    public String criadoEm;

    /** Dados enviados para a tela (sem o hash da senha). */
    public Map<String, Object> paraTela() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("nome", nome);
        m.put("criadoEm", criadoEm);
        return m;
    }
}
