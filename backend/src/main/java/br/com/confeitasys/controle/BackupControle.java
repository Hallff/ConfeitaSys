package br.com.confeitasys.controle;

import br.com.confeitasys.dto.ArquivoResposta;
import br.com.confeitasys.dto.RelatorioSalvarRequisicao;
import br.com.confeitasys.dto.RelatorioSalvoResposta;
import br.com.confeitasys.excecao.RegraException;
import br.com.confeitasys.servico.BackupServico;
import br.com.confeitasys.servico.RelatorioServico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * ===================================================================
 *  GRUPO 8 - BACKUP (cópia de segurança)
 * ===================================================================
 *  8.1  POST /api/backup              gerar backup do banco (.sql)
 *  8.2  POST /api/relatorios/salvar   salvar os relatórios em arquivo + backup
 *
 *  Frontend: frontend/js/api.js -> API.backup.*
 *  Regras:   servico/BackupServico.java e servico/RelatorioServico.java
 */
@RestController
@Tag(name = "8. Backup", description = "Cópia de segurança de todos os dados e gravação dos relatórios em arquivo")
public class BackupControle {

    private final BackupServico backup;
    private final RelatorioServico relatorios;

    public BackupControle(BackupServico backup, RelatorioServico relatorios) {
        this.backup = backup;
        this.relatorios = relatorios;
    }

    /**
     * API 8.1 - FAZER BACKUP
     * Tela: menu lateral -> "Backup"
     * Frontend: API.backup.fazer()
     */
    @PostMapping("/api/backup")
    @Operation(operationId = "fazerBackup", summary = "8.1 Fazer backup do banco",
            description = "**Tela:** menu lateral → **Backup**  \n"
                    + "**Frontend:** `API.backup.fazer()`  \n"
                    + "**Backend:** `controle/BackupControle.java` → `BackupServico.fazerBackup()`  \n"
                    + "**Tabelas:** todas  \n\n"
                    + "Gera `backend/backup/confeitasys-AAAAMMDD-HHMMSS.sql` com todos os dados. "
                    + "Para restaurar: `psql -U confeitasys -d confeitasys -f <arquivo>`.")
    public ArquivoResposta fazerBackup() {
        return new ArquivoResposta(true, backup.fazerBackup());
    }

    /**
     * API 8.2 - SALVAR RELATÓRIOS + BACKUP
     * Tela: Relatórios -> botão "Salvar e fazer backup"
     * Frontend: API.backup.salvarRelatorios(data, html)
     */
    @PostMapping("/api/relatorios/salvar")
    @Operation(operationId = "salvarRelatorios", summary = "8.2 Salvar relatórios em arquivo (+ backup)",
            description = "**Tela:** Relatórios → botão **Salvar e fazer backup**  \n"
                    + "**Frontend:** `API.backup.salvarRelatorios()`  \n"
                    + "**Backend:** `controle/BackupControle.java` → `RelatorioServico.salvarHtml()` + `BackupServico.fazerBackup()`  \n\n"
                    + "Grava o HTML em `backend/relatorios/relatorios-AAAA-MM-DD.html` e gera um backup do banco.")
    public RelatorioSalvoResposta salvarRelatorios(@RequestBody RelatorioSalvarRequisicao d) {
        if (d == null) throw new RegraException("Informe a data e o conteúdo do relatório.");
        String arquivo = relatorios.salvarHtml(d.data(), d.html());
        return new RelatorioSalvoResposta(true, arquivo, backup.fazerBackup());
    }
}
