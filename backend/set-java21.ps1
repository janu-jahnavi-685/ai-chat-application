# Run this in your terminal before using mvn locally:
#   . .\set-java21.ps1
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.11"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Write-Host "JAVA_HOME set to $env:JAVA_HOME"
java -version
