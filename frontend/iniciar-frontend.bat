@echo off
REM ====================================================================
REM  ConfeitaSys - inicia as TELAS (frontend) em http://localhost:5500
REM  A API (backend) precisa estar rodando em http://localhost:8080
REM ====================================================================
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java nao encontrado. Instale o Java 17 - adoptium.net
  pause
  exit /b 1
)
start "" http://localhost:5500
java ServidorFrontend.java
pause
