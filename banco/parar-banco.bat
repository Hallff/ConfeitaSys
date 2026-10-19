@echo off
REM ====================================================================
REM  ConfeitaSys - DESLIGA o banco de dados PostgreSQL
REM  Util para mostrar que o sistema depende do banco: com ele desligado,
REM  a API responde "Nao foi possivel conectar ao banco de dados".
REM  Precisa de administrador: botao direito -> Executar como administrador.
REM ====================================================================
setlocal
set SERVICO=
for /f "tokens=2 delims=: " %%S in ('sc query state^= all ^| findstr /I /R "SERVICE_NAME:.*postgresql"') do if not defined SERVICO set "SERVICO=%%S"
if not defined SERVICO goto sem_servico
net stop "%SERVICO%"
if errorlevel 1 goto sem_permissao
echo Banco de dados DESLIGADO. Para ligar de novo: iniciar-banco.bat
pause
exit /b 0

:sem_servico
echo Nao encontrei o servico do PostgreSQL no Windows.
pause
exit /b 1
:sem_permissao
echo Nao foi possivel desligar. Execute como administrador.
pause
exit /b 1
