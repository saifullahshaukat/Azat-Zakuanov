# Java SSL Proxy Project

A secure SSL proxy server with client certificate authentication and web dashboard for testing SSL/TLS connections with client certificates.

![Java](https://img.shields.io/badge/Java-ED8B00?logo=java&logoColor=white)
![SSL](https://img.shields.io/badge/SSL-TLS-green)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

## Quick Start

### Prerequisites
- Java 8 or higher
- Client certificate file (PKCS#12 format)

### Running the Application

1. **Start Secure SSL Proxy:**
   ```bash
   ./run.sh secure
   ```

2. **Start Web Dashboard:**
   ```bash
   ./run.sh dashboard
   ```

3. **Start Both (Recommended):**
   ```bash
   ./run.sh full
   ```

## Features

- Secure SSL/TLS proxy with client certificate authentication
- Web dashboard with real-time monitoring
- BadSSL.com integration for testing SSL connections
- Comprehensive security features including rate limiting
- IP whitelisting and access control
- Modern Tailwind CSS web interface
- TLS 1.2/1.3 protocol support
- PKCS#12 certificate support
- Multiple SSL testing endpoints

## Project Structure

```
java-ssl/
├── Core Files
│   ├── SecureSSLProxy.java      # Main SSL proxy server
│   ├── DashboardServer.java     # Web dashboard server  
│   └── index.html               # Web dashboard interface
├── Test Clients
│   ├── BadSSLClient.java        # BadSSL test client
│   └── EnhancedBadSSLClient.java # Enhanced BadSSL testing
├── Certificates
│   ├── badssl.com-client.p12    # Client certificate (PKCS#12)
│   ├── client-cert.pem         # PEM certificate
│   └── client-key.pem          # PEM private key
├── Scripts
│   ├── run.sh                  # Build and run script
│   └── browser_setup.sh        # Browser certificate setup
└── Documentation
    └── README.md               # Complete project documentation
```

## Configuration

Environment variables:
- `SSL_CERT_PASSWORD` - Certificate password (default: badssl.com)
- `PROXY_PORT` - SSL proxy port (default: 8444)
- `DASHBOARD_PORT` - Web dashboard port (default: 8080)

## Available Commands

```bash
# Compile all files
./run.sh compile

# Start secure proxy only
./run.sh secure

# Start dashboard only  
./run.sh dashboard

# Start both services
./run.sh full

# Test BadSSL connection
./run.sh badssl

# Run enhanced BadSSL tests
./run.sh enhanced

# Clean compiled files
./run.sh clean
```

## Testing with Curl Commands

### Basic BadSSL Testing
```bash
# Test successful client certificate authentication
curl -v --cert client-cert.pem --key client-key.pem https://client.badssl.com/

# Test with connection timeout (recommended)
curl -v --cert client-cert.pem --key client-key.pem --connect-timeout 10 https://client.badssl.com/

# Test with specific TLS version
curl -v --cert client-cert.pem --key client-key.pem --tlsv1.2 https://client.badssl.com/
curl -v --cert client-cert.pem --key client-key.pem --tlsv1.3 https://client.badssl.com/
```

### Advanced BadSSL Testing
```bash
# Show only response headers
curl -I --cert client-cert.pem --key client-key.pem https://client.badssl.com/

# Test with maximum verbosity and save output
curl -v --cert client-cert.pem --key client-key.pem https://client.badssl.com/ > test_output.html 2>&1

# Test SSL handshake details
curl -v --cert client-cert.pem --key client-key.pem --ssl-reqd https://client.badssl.com/

# Test with custom user agent
curl -v --cert client-cert.pem --key client-key.pem -A "SSL-Test-Client/1.0" https://client.badssl.com/
```

### Testing Other BadSSL Endpoints
```bash
# Test different SSL configurations (these should fail without client cert)
curl -v https://expired.badssl.com/
curl -v https://wrong.host.badssl.com/
curl -v https://self-signed.badssl.com/
curl -v https://untrusted-root.badssl.com/

# Test cipher suites
curl -v --ciphers ECDHE-RSA-AES256-GCM-SHA384 --cert client-cert.pem --key client-key.pem https://client.badssl.com/
```

### Expected Results
```bash
# Successful connection (HTTP 200)
< HTTP/1.1 200 OK
< Server: nginx/1.10.3 (Ubuntu)
< Content-Type: text/html

# SSL handshake information
* SSL connection using TLSv1.2 / ECDHE-RSA-AES256-GCM-SHA384
* Server certificate: *.badssl.com
```

### Java Testing
```bash
# Run BadSSL client test
./run.sh badssl

# Run enhanced tests with multiple endpoints
./run.sh enhanced
```

### Testing SSL Proxy
```bash
# Start the SSL proxy first
./run.sh secure

# Then test through the proxy (in another terminal)
curl -v --cert client-cert.pem --key client-key.pem --proxy https://localhost:8444 https://client.badssl.com/
```

## Browser Setup

To access BadSSL websites in your browser with client certificate authentication:

### Setup Steps

1. **Run the browser setup helper:**
   ```bash
   ./browser_setup.sh
   ```

2. **Firefox (Recommended):**
   - Open Firefox Settings (about:preferences)
   - Go to Privacy & Security section
   - Scroll to "Certificates" and click "View Certificates"
   - Select "Your Certificates" tab
   - Click "Import" button
   - Choose `badssl.com-client.p12` file
   - Enter password: `badssl.com`
   - Visit: https://client.badssl.com/

3. **Chrome/Chromium:**
   - Open Settings → Privacy and security → Security
   - Click "Manage certificates"
   - Go to "Personal" tab
   - Click "Import" and select the P12 file
   - Enter password: `badssl.com`
   - Visit the BadSSL site

### Verification
After importing the certificate, you should see a green page with "client.badssl.com" text when visiting the site.

### Alternative System Certificate Installation (Linux)
```bash
# Install certificate system-wide using NSS database
pk12util -i badssl.com-client.p12 -d sql:$HOME/.pki/nssdb
# Enter password: badssl.com
```

### Browser Troubleshooting
- **400 Bad Request**: Certificate not imported or not selected during connection
- **Connection refused**: Network connectivity issue
- **Certificate selection dialog**: Choose the BadSSL certificate from the list

## Security Features

- Proper certificate validation and chain verification
- TLS 1.2/1.3 protocol support with strong cipher suites
- Rate limiting (10 requests per minute per IP address)
- IP whitelisting and access control
- Secure HTTP headers implementation
- Input validation and sanitization
- Comprehensive error handling and logging
- Real-time monitoring through web dashboard

## Troubleshooting

### Common Issues

1. **Certificate errors**: 
   - Ensure `badssl.com-client.p12` file is present in project directory
   - Verify certificate password is `badssl.com`
   - Check PEM files (`client-cert.pem`, `client-key.pem`) exist

2. **Connection refused**:
   - Check if ports 8444/8080 are available: `netstat -tulpn | grep :8444`
   - Ensure firewall allows connections to these ports
   - Try running with different ports if needed

3. **Browser access issues**:
   - Import certificate following instructions above
   - Clear browser cache and restart browser
   - Check certificate was imported in correct browser profile

4. **404 errors**:
   - Normal behavior for non-existent BadSSL paths (like `/api`, `/dashboard`)
   - 404 responses actually prove SSL authentication is working
   - Only root path (`/`) exists on client.badssl.com

5. **Compilation errors**:
   ```bash
   ./run.sh clean      # Remove old class files
   ./run.sh compile    # Recompile all Java files
   ```

6. **SSL handshake failures**:
   - Verify system time is correct
   - Check if corporate firewall is blocking SSL
   - Try different TLS versions: `--tlsv1.2` or `--tlsv1.3`

### Debug Commands

```bash
# Check certificate details
openssl pkcs12 -in badssl.com-client.p12 -passin pass:badssl.com -info

# Verify PEM files
openssl x509 -in client-cert.pem -text -noout
openssl rsa -in client-key.pem -check

# Test network connectivity
ping badssl.com
nslookup client.badssl.com

# Check Java version
java -version
javac -version
```

## Expected Results

### Successful BadSSL Connection
- **curl**: HTTP 200 OK response with green HTML page content
- **Browser**: Green page displaying "client.badssl.com" in large text
- **Java client**: Successful SSL handshake with certificate authentication

### Understanding 404 Responses
- Paths like `/dashboard`, `/api`, `/json` don't exist on BadSSL server
- These 404 responses actually prove SSL authentication is working correctly
- Only the root path (`/`) contains actual content

### SSL Handshake Success Indicators
```
* SSL connection using TLSv1.2 / ECDHE-RSA-AES256-GCM-SHA384
* Server certificate: *.badssl.com
* SSL certificate verify ok
< HTTP/1.1 200 OK
```

## Quick Setup Guide

1. **Clone and compile:**
   ```bash
   git clone <repository>
   cd java-ssl
   ./run.sh compile
   ```

2. **Test SSL connection:**
   ```bash
   curl -v --cert client-cert.pem --key client-key.pem https://client.badssl.com/
   ```

3. **Start services:**
   ```bash
   ./run.sh full    # Starts both proxy and dashboard
   ```

4. **Access dashboard:**
   - Open browser to: http://localhost:8080

## License

This project is for educational and testing purposes.

Ready to use your secure SSL proxy for development and testing!