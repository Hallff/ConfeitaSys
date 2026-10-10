@echo off
chcp 65001 > nul
REM Recompila o ConfeitaSys a partir do codigo-fonte (pasta src) e gera ConfeitaSys.jar
REM Precisa do JDK 17 ou mais novo (que inclui o javac).
cd /d "%~dp0"
if exist out rmdir /s /q out
mkdir out
javac --release 17 -encoding UTF-8 -cp lib\sqlite-jdbc-3.46.1.3.jar -d out src\confeitasys\*.java src\confeitasys\util\*.java src\confeitasys\modelo\*.java src\confeitasys\dao\*.java src\confeitasys\servico\*.java src\confeitasys\controle\*.java
if errorlevel 1 ( echo ERRO NA COMPILACAO & pause & exit /b 1 )
xcopy /e /i /q src\web out\web > nul
xcopy /e /i /q src\db out\db > nul
(echo Main-Class: confeitasys.Main& echo Class-Path: lib/sqlite-jdbc-3.46.1.3.jar)> out\manifesto.txt
jar --create --file ConfeitaSys.jar --manifest out\manifesto.txt -C out confeitasys -C out web -C out db
rmdir /s /q out
echo Pronto: ConfeitaSys.jar gerado.
pause
