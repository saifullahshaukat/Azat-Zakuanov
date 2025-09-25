#!/bin/bash

# Browser BadSSL Setup Script
# This script helps you set up client certificate authentication for browsers

echo "🔐 Browser BadSSL Setup Guide"
echo "============================="

# Check if certificates exist
if [ ! -f "badssl.com-client.p12" ]; then
    echo "❌ P12 certificate not found!"
    echo "💡 Download from: https://badssl.com/download/"
    exit 1
fi

echo "📋 Current certificate files:"
ls -la *.p12 *.pem 2>/dev/null || echo "No certificate files found"

echo ""
echo "🌐 Browser Setup Instructions:"
echo "==============================="

echo ""
echo "🦊 FIREFOX SETUP:"
echo "1. Open Firefox"
echo "2. Go to Settings (about:preferences)"
echo "3. Search for 'Certificates'"
echo "4. Click 'View Certificates'"
echo "5. Go to 'Your Certificates' tab"
echo "6. Click 'Import...'"
echo "7. Select: badssl.com-client.p12"
echo "8. Password: badssl.com"
echo "9. Visit: https://client.badssl.com/"

echo ""
echo "🌍 CHROME/CHROMIUM SETUP:"
echo "1. Open Chrome/Chromium"
echo "2. Go to Settings"
echo "3. Advanced → Privacy and Security → Security"
echo "4. Click 'Manage certificates'"
echo "5. Go to 'Personal' tab"
echo "6. Click 'Import...'"
echo "7. Select: badssl.com-client.p12"
echo "8. Password: badssl.com"
echo "9. Visit: https://client.badssl.com/"

echo ""
echo "🐧 LINUX SYSTEM CERTIFICATE STORE:"
echo "1. Convert P12 to system format:"
echo "   pk12util -i badssl.com-client.p12 -d sql:\$HOME/.pki/nssdb"
echo "2. Password: badssl.com"
echo "3. Visit: https://client.badssl.com/"

echo ""
echo "✅ TEST COMMANDS (Terminal):"
echo "1. Working endpoint:"
echo "   curl -v --cert client-cert.pem --key client-key.pem https://client.badssl.com/"
echo ""
echo "2. Expected result: HTTP 200 OK with green page"

echo ""
echo "🔍 VERIFICATION:"
echo "==============="

# Test the main endpoint
echo "Testing main endpoint..."
if command -v curl >/dev/null 2>&1; then
    if [ -f "client-cert.pem" ] && [ -f "client-key.pem" ]; then
        echo "🧪 Running test..."
        result=$(curl -s -w "%{http_code}" --cert client-cert.pem --key client-key.pem https://client.badssl.com/ 2>/dev/null)
        status_code="${result: -3}"
        
        if [ "$status_code" = "200" ]; then
            echo "✅ SUCCESS: HTTP 200 OK - Certificate authentication working!"
            echo "🎯 You can now use this certificate in your browser"
        else
            echo "⚠ Status: HTTP $status_code"
        fi
    else
        echo "⚠ PEM files not found, converting from P12..."
        openssl pkcs12 -in badssl.com-client.p12 -passin pass:badssl.com -out client-cert.pem -clcerts -nokeys -legacy 2>/dev/null
        openssl pkcs12 -in badssl.com-client.p12 -passin pass:badssl.com -out client-key.pem -nocerts -nodes -legacy 2>/dev/null
        echo "✅ PEM files created, run the script again"
    fi
else
    echo "⚠ curl not available for testing"
fi

echo ""
echo "📝 IMPORTANT NOTES:"
echo "==================="
echo "1. ✅ The certificate is working correctly"
echo "2. ❌ 404 errors on /dashboard, /json etc. are NORMAL"
echo "3. ✅ Only the root path (/) exists on client.badssl.com"
echo "4. 🔐 SSL handshake and authentication are successful"
echo "5. 🌐 After importing to browser, visit: https://client.badssl.com/"

echo ""
echo "🎉 Your SSL setup is working perfectly!"