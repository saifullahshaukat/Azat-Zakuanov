@echo off
REM Java SSL Project - Windows Batch Script

echo.
echo 🔐 Java SSL Project for Windows
echo ================================

REM Function to compile all Java files
if "%1"=="compile" (
    echo Compiling Java files...
    javac *.java
    if %ERRORLEVEL% EQU 0 (
        echo ✅ Compilation successful
    ) else (
        echo ❌ Compilation failed
        exit /b 1
    )
    goto :eof
)

REM Function to clean compiled files
if "%1"=="clean" (
    echo Cleaning compiled files...
    del /Q *.class >nul 2>&1
    echo ✅ Cleaned
    goto :eof
)

REM Function to run BadSSL client test
if "%1"=="badssl" (
    call :compile_all
    echo Running BadSSL Client Test...
    java BadSSLClient
    goto :eof
)

REM Function to run enhanced BadSSL client test
if "%1"=="enhanced" (
    call :compile_all
    echo Running Enhanced BadSSL Client Test...
    java EnhancedBadSSLClient
    goto :eof
)

REM Function to start secure SSL proxy
if "%1"=="secure" (
    call :compile_all
    echo Starting Secure SSL Proxy on port 8444...
    echo Press Ctrl+C to stop the server
    java SecureSSLProxy
    goto :eof
)

REM Function to start dashboard server
if "%1"=="dashboard" (
    call :compile_all
    echo Starting Dashboard Server on port 8080...
    echo Open http://localhost:8080 in your browser
    echo Press Ctrl+C to stop the server
    java DashboardServer
    goto :eof
)

REM Function to start both services
if "%1"=="full" (
    call :compile_all
    echo Starting both Secure SSL Proxy and Dashboard...
    echo Dashboard: http://localhost:8080
    echo SSL Proxy: https://localhost:8444
    echo.
    echo Starting Dashboard Server in background...
    start "Dashboard Server" cmd /c "java DashboardServer"
    timeout /t 3 /nobreak >nul
    echo Starting SSL Proxy Server...
    echo Press Ctrl+C to stop both servers
    java SecureSSLProxy
    goto :eof
)

REM Default help message
echo Usage: %0 {compile^|clean^|badssl^|enhanced^|secure^|dashboard^|full}
echo.
echo Commands:
echo   compile   - Compile all Java files
echo   clean     - Remove compiled .class files
echo   badssl    - Run BadSSL Client Test
echo   enhanced  - Run Enhanced BadSSL Client Test
echo   secure    - Start Secure SSL Proxy server
echo   dashboard - Start Web Dashboard on port 8080
echo   full      - Start both Dashboard and Secure SSL Proxy
echo.
echo Examples:
echo   %0 compile     # Compile all files
echo   %0 secure      # Start secure SSL proxy server
echo   %0 dashboard   # Start web dashboard
echo   %0 full        # Start both dashboard and secure proxy
echo   %0 badssl      # Test BadSSL connection
echo   %0 enhanced    # Run enhanced BadSSL tests
echo.
echo Windows-specific commands:
echo   javac *.java           # Direct compilation
echo   java SecureSSLProxy    # Direct SSL proxy start
echo   java DashboardServer   # Direct dashboard start
echo   java BadSSLClient      # Direct BadSSL test
goto :eof

:compile_all
echo Compiling Java files...
javac *.java
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Compilation failed
    exit /b 1
)
echo ✅ Compilation successful
goto :eof