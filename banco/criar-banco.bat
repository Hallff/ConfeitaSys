@echo off
REM ====================================================================
REM  ConfeitaSys - cria o banco PostgreSQL rodando os 3 scripts em ordem
REM  Pede a senha do usuario "postgres" (a que voce definiu ao instalar).
REM ====================================================================
setlocal
chcp 65001 >nul
set PGCLIENTENCODING=UTF8
cd /d "%~dp0"

set PSQL=
for %%V in (18 17 16 15 14 13) do (
  if not defined PSQL if exist "C:\Program Files\PostgreSQL\%%V\bin\psql.exe" set "PSQL=C:\Program Files\PostgreSQL\%%V\bin\psql.exe"
)
if not defined PSQL (
  where psql >nul 2>nul && set PSQL=psql
)
if not defined PSQL (
  echo Nao encontrei o psql.exe. Instale o PostgreSQL ou ajuste o caminho neste arquivo.
  pause
  exit /b 1
)
echo Usando: %PSQL%
echo.

echo [1/3] Criando usuario e banco "confeitasys" (senha do usuario postgres):
"%PSQL%" -h localhost -U postgres -v ON_ERROR_STOP=1 -f 01_criar_banco.sql
if errorlevel 1 (
  echo.
  echo Falhou o passo 1. Se o banco ja existia, pode ignorar e seguir para o passo 2.
  choice /M "Continuar mesmo assim"
  if errorlevel 2 exit /b 1
)

set PGPASSWORD=confeitasys
echo.
echo [2/3] Criando tabelas, indices e views...
"%PSQL%" -h localhost -U confeitasys -d confeitasys -v ON_ERROR_STOP=1 -q -f 02_tabelas.sql
if errorlevel 1 goto erro

echo.
choice /M "[3/3] Inserir dados de exemplo (estoque, cardapio e 3 vendas)"
if errorlevel 2 goto fim
"%PSQL%" -h localhost -U confeitasys -d confeitasys -v ON_ERROR_STOP=1 -q -f 03_dados_exemplo.sql
if errorlevel 1 goto erro

:fim
echo.
echo Banco pronto! Agora inicie o backend (pasta backend).
pause
exit /b 0

:erro
echo.
echo Ocorreu um erro. Leia a mensagem acima.
pause
exit /b 1
