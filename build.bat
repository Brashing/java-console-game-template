@echo off
setlocal enabledelayedexpansion
set ROOT=%~dp0
set OUT=%ROOT%out
rmdir /s /q "%OUT%" 2>nul
mkdir "%OUT%"
dir /s /b "%ROOT%src\*.java" > "%ROOT%.sources"
javac -encoding UTF-8 -d "%OUT%" @%ROOT%.sources
echo Build OK. Run run.bat
