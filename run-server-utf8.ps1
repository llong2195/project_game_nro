[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::InputEncoding = [System.Text.Encoding]::UTF8

$env:JAVA_OPTS = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8 -Djava.awt.headless=true"

Write-Host "Starting server with UTF-8 encoding..." -ForegroundColor Green
Write-Host "JAVA_OPTS: $env:JAVA_OPTS" -ForegroundColor Yellow

java $env:JAVA_OPTS.Split(' ') -jar build/libs/nrotuonglai-1.0.0.jar

Read-Host "Press Enter to exit"
