@echo off
REM ====================================================================
REM  ConfeitaSys - INICIA TUDO, na ordem certa:
REM    1) banco de dados PostgreSQL (localhost:5432)
REM    2) API / backend Spring Boot (localhost:8080) + Swagger
REM    3) telas / frontend (localhost:5500)
REM  Cada parte abre na sua propria janela. Para parar, feche as janelas.
REM  Primeira vez? Antes rode banco\criar-banco.bat para criar o banco.
REM ====================================================================
setlocal
cd /d "%~dp0"

echo.
echo [1/3] Banco de dados PostgreSQL...
call banco\iniciar-banco.bat nopause
if errorlevel 1 goto erro_banco
cd /d "%~dp0"

echo [2/3] Iniciando a API - backend - em outra janela...
start "ConfeitaSys - API (backend :8080)" cmd /c backend\iniciar-backend.bat

echo [3/3] Iniciando as telas - frontend - em outra janela...
start "ConfeitaSys - Telas (frontend :5500)" /d frontend cmd /c java ServidorFrontend.java

echo.
echo Aguardando a API responder em http://localhost:8080 ...
echo (na primeira vez pode levar alguns minutos)
powershell -NoProfile -Command "for($i=0;$i -lt 300;$i++){ try { $c=New-Object Net.Sockets.TcpClient; $c.Connect('localhost',8080); $c.Close(); exit 0 } catch { Start-Sleep -Seconds 2 } }; exit 1"
if errorlevel 1 goto erro_api

echo.
echo  Tudo no ar!
echo    Swagger (APIs) ..... http://localhost:8080/swagger-ui.html
echo    Sistema (telas) .... http://localhost:5500
start "" http://localhost:8080/swagger-ui.html
start "" http://localhost:5500
echo.
pause
exit /b 0

:erro_banco
echo.
echo O banco de dados nao esta ligado - o sistema nao funciona sem ele.
echo Rode banco\iniciar-banco.bat como administrador e tente de novo.
pause
exit /b 1

:erro_api
echo.
echo A API nao respondeu. Veja a mensagem na janela "ConfeitaSys - API".
pause
exit /b 1
