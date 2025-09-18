@echo off
chcp 65001 >nul
set JAVA_OPTS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djava.awt.headless=true
echo Starting server with UTF-8 encoding...
java %JAVA_OPTS% -jar build/libs/nrotuonglai-1.0.0.jar
pause
