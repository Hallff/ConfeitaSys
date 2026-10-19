@echo off
REM ====================================================================
REM  ConfeitaSys - LIGA o banco de dados PostgreSQL
REM  O sistema so funciona com o banco ligado: a API (backend) grava e
REM  le tudo no PostgreSQL em localhost:5432.
REM  Se pedir permissao: botao direito -> Executar como administrador.
REM ====================================================================
setlocal
cd /d "%~dp0"
set NOPAUSE=
if /i "%~1"=="nopause" set NOPAUSE=1

set BIN=
for %%V in (18 17 16 15 14 13) do if not defined BIN if exist "C:\Program Files\PostgreSQL\%%V\bin\pg_isready.exe" set "BIN=C:\Program Files\PostgreSQL\%%V\bin"
if not defined BIN goto sem_postgres

"%BIN%\pg_isready.exe" -h localhost -p 5432 >nul 2>nul
if not errorlevel 1 goto ligado

echo Ligando o servico do PostgreSQL...
set SERVICO=
for /f "tokens=2 delims=: " %%S in ('sc query state^= all ^| findstr /I /R "SERVICE_NAME:.*postgresql"') do if not defined SERVICO set "SERVICO=%%S"
if not defined SERVICO goto sem_servico
net start "%SERVICO%"
if errorlevel 1 goto sem_permissao
timeout /t 3 /nobreak >nul
"%BIN%\pg_isready.exe" -h localhost -p 5432 >nul 2>nul
if errorlevel 1 goto nao_respondeu

:ligado
echo.
echo  Banco de dados PostgreSQL LIGADO em localhost:5432 - banco "confeitasys"
echo.
if not defined NOPAUSE pause
exit /b 0

:sem_postgres
echo PostgreSQL nao encontrado em C:\Program Files\PostgreSQL. Instale pelo site postgresql.org
goto erro
:sem_servico
echo Nao encontrei o servico do PostgreSQL no Windows. Reinstale o PostgreSQL.
goto erro
:sem_permissao
echo.
echo Nao foi possivel ligar o servico. Clique com o botao direito neste arquivo
echo e escolha "Executar como administrador".
goto erro
:nao_respondeu
echo O servico ligou, mas o banco ainda nao respondeu. Tente de novo em alguns segundos.
goto erro

:erro
if not defined NOPAUSE pause
exit /b 1
