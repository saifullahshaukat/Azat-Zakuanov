@echo off
REM Windows Browser Setup Helper for Java SSL Project

echo.
echo 🌐 Browser Certificate Setup for Windows
echo ==========================================

echo.
echo This script will help you set up client certificates for browser testing.
echo.

REM Check if certificate file exists
if not exist "badssl.com-client.p12" (
    echo ❌ Certificate file 'badssl.com-client.p12' not found!
    echo    Make sure you're in the correct project directory.
    pause
    exit /b 1
)

echo ✅ Certificate file found: badssl.com-client.p12
echo.

echo 📋 Manual Browser Setup Instructions:
echo.
echo 🦊 FIREFOX:
echo    1. Open Firefox Settings (about:preferences)
echo    2. Go to Privacy ^& Security section
echo    3. Scroll to "Certificates" and click "View Certificates"
echo    4. Select "Your Certificates" tab
echo    5. Click "Import" button
echo    6. Choose: %CD%\badssl.com-client.p12
echo    7. Enter password: badssl.com
echo    8. Visit: https://client.badssl.com/
echo.
echo 🌐 CHROME/EDGE:
echo    1. Open Settings → Privacy and security → Security
echo    2. Click "Manage certificates"
echo    3. Go to "Personal" tab
echo    4. Click "Import" and select the P12 file
echo    5. Enter password: badssl.com
echo    6. Visit: https://client.badssl.com/
echo.

REM Try to open the directory in Windows Explorer
echo 📁 Opening certificate directory...
start "" "%CD%"

echo.
echo 🚀 After importing the certificate:
echo    • Visit https://client.badssl.com/
echo    • You should see a green page with "client.badssl.com"
echo    • If prompted, select the BadSSL certificate
echo.

pause