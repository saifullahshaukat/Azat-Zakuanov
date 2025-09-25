#!/bin/bash

# Java SSL Project - Compilation and Run Script

echo "🔐 Java SSL Project"
echo "==================="

# Function to compile all Java files
compile_all() {
    echo "Compiling Java files..."
    javac *.java
    if [ $? -eq 0 ]; then
        echo "✅ Compilation successful"
    else
        echo "❌ Compilation failed"
        exit 1
    fi
}

# Function to clean compiled files
clean() {
    echo "Cleaning compiled files..."
    rm -f *.class
    echo "✅ Cleaned"
}

# Main menu
case "$1" in
    "compile")
        compile_all
        ;;
    "clean")
        clean
        ;;
    "badssl")
        compile_all
        echo "Running BadSSL Client Test..."
        java BadSSLClient
        ;;
    "enhanced")
        compile_all
        echo "Running Enhanced BadSSL Client Test..."
        java EnhancedBadSSLClient
        ;;
    "secure")
        compile_all
        echo "Starting Secure SSL Proxy on port 8444..."
        java SecureSSLProxy
        ;;
    "dashboard")
        compile_all
        echo "Starting Dashboard Server on port 8080..."
        echo "Open http://localhost:8080 in your browser"
        java DashboardServer
        ;;
    "full")
        compile_all
        echo "Starting both Secure SSL Proxy and Dashboard..."
        echo "Dashboard: http://localhost:8080"
        echo "SSL Proxy: https://localhost:8444"
        java DashboardServer &
        sleep 2
        java SecureSSLProxy
        ;;
    *)
        echo "Usage: $0 {compile|clean|badssl|enhanced|secure|dashboard|full}"
        echo ""
        echo "Commands:"
        echo "  compile   - Compile all Java files"
        echo "  clean     - Remove compiled .class files"
        echo "  badssl    - Run BadSSL Client Test"
        echo "  enhanced  - Run Enhanced BadSSL Client Test"
        echo "  secure    - Start Secure SSL Proxy server"
        echo "  dashboard - Start Web Dashboard on port 8080"
        echo "  full      - Start both Dashboard and Secure SSL Proxy"
        echo ""
        echo "Examples:"
        echo "  ./run.sh compile     # Compile all files"
        echo "  ./run.sh secure      # Start secure SSL proxy server"
        echo "  ./run.sh dashboard   # Start web dashboard"
        echo "  ./run.sh full        # Start both dashboard and secure proxy"
        echo "  ./run.sh badssl      # Test BadSSL connection"
        echo "  ./run.sh enhanced    # Run enhanced BadSSL tests"
        exit 1
        ;;
esac