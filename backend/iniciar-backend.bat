@echo off
REM ====================================================================
REM  ConfeitaSys - inicia a API (backend Spring Boot) em localhost:8080
REM  Swagger: http://localhost:8080/swagger-ui.html
REM
REM  Se o Maven nao estiver instalado, ele e baixado automaticamente para
REM  a pasta backend\.maven (so na primeira vez).
REM  O banco PostgreSQL precisa estar ligado (banco\iniciar-banco.bat).
REM ====================================================================
setlocal
cd /d "%~dp0"

where java >nul 2>nul
if errorlevel 1 goto sem_java

set "MVN=mvn"
where mvn >nul 2>nul
if not errorlevel 1 goto rodar

set "MVN=.maven\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MVN%" goto rodar

echo Baixando o Maven 3.9.9 - apenas na primeira vez...
if not exist ".maven" mkdir ".maven"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '.maven\maven.zip'; Expand-Archive -Path '.maven\maven.zip' -DestinationPath '.maven' -Force; Remove-Item '.maven\maven.zip'"
if not exist "%MVN%" goto sem_maven

:rodar
echo.
echo  Iniciando a API do ConfeitaSys...
echo  Na primeira vez demora alguns minutos: o Maven baixa as bibliotecas do Spring.
echo  Quando aparecer "ConfeitaSys API no ar", abra http://localhost:8080/swagger-ui.html
echo  Para parar: feche esta janela ou aperte Ctrl+C.
echo.
call "%MVN%" -q spring-boot:run
pause
exit /b 0

:sem_java
echo Java nao encontrado. Instale o Java 17 - adoptium.net - marcando "Add to PATH".
pause
exit /b 1
:sem_maven
echo Nao foi possivel baixar o Maven. Verifique a internet ou instale pelo site maven.apache.org
pause
exit /b 1
