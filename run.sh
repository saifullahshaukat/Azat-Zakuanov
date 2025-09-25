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
    "client")
        compile_all
        echo "Running P12 SSL Client..."
        java P12SSLClient
        ;;
    "proxy")
        compile_all
        echo "Starting P12 SSL Proxy on port 8444..."
        java P12SSLProxy
        ;;
    "test")
        compile_all
        echo "Running proxy test client..."
        java ProxyTestClient
        ;;
    "suite")
        compile_all
        echo "Running complete test suite..."
        java ProxyTestSuite
        ;;
    *)
        echo "Usage: $0 {compile|clean|client|proxy|test|suite}"
        echo ""
        echo "Commands:"
        echo "  compile  - Compile all Java files"
        echo "  clean    - Remove compiled .class files"
        echo "  client   - Run P12 SSL Client"
        echo "  proxy    - Start P12 SSL Proxy server"
        echo "  test     - Run proxy test client"
        echo "  suite    - Run complete test suite"
        echo ""
        echo "Examples:"
        echo "  ./run.sh compile     # Compile all files"
        echo "  ./run.sh client      # Test SSL client with P12 certificate"
        echo "  ./run.sh proxy       # Start SSL proxy server"
        exit 1
        ;;
esac