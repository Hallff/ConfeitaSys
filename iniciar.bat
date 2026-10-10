@echo off
chcp 65001 > nul
title ConfeitaSys
cd /d "%~dp0"
where java > nul 2>&1
if errorlevel 1 (
  echo.
  echo  O Java nao esta instalado neste computador.
  echo  Baixe e instale o Java 17 ou mais novo em: https://adoptium.net/
  echo  Depois rode este arquivo de novo.
  echo.
  pause
  exit /b 1
)
echo Iniciando o ConfeitaSys... (nao feche esta janela enquanto usar o sistema)
java -jar ConfeitaSys.jar
pause
