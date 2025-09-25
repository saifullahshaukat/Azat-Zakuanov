import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.*;

/**
 * Enhanced BadSSL Client with comprehensive endpoint testing
 * Tests various BadSSL endpoints to demonstrate different SSL scenarios
 */
public class EnhancedBadSSLClient {
    private static final String P12_FILE = "badssl.com-client.p12";
    private static final String P12_PASSWORD = "badssl.com";
    
    // Various BadSSL test endpoints
    private static final List<TestEndpoint> BADSSL_ENDPOINTS = Arrays.asList(
        new TestEndpoint("client.badssl.com", 443, "/", "Client Certificate Required", true),
        new TestEndpoint("expired.badssl.com", 443, "/", "Expired Certificate", false),
        new TestEndpoint("wrong.host.badssl.com", 443, "/", "Wrong Hostname", false),
        new TestEndpoint("self-signed.badssl.com", 443, "/", "Self-Signed Certificate", false),
        new TestEndpoint("untrusted-root.badssl.com", 443, "/", "Untrusted Root CA", false),
        new TestEndpoint("revoked.badssl.com", 443, "/", "Revoked Certificate", false),
        new TestEndpoint("pinning-test.badssl.com", 443, "/", "Certificate Pinning Test", false),
        new TestEndpoint("no-common-name.badssl.com", 443, "/", "No Common Name", false),
        new TestEndpoint("no-subject.badssl.com", 443, "/", "No Subject", false),
        new TestEndpoint("incomplete-chain.badssl.com", 443, "/", "Incomplete Chain", false),
        new TestEndpoint("sha1-intermediate.badssl.com", 443, "/", "SHA-1 Intermediate", false),
        new TestEndpoint("1000-sans.badssl.com", 443, "/", "1000 Subject Alternative Names", false),
        new TestEndpoint("10000-sans.badssl.com", 443, "/", "10000 Subject Alternative Names", false),
        new TestEndpoint("ecc256.badssl.com", 443, "/", "ECC 256-bit Certificate", false),
        new TestEndpoint("ecc384.badssl.com", 443, "/", "ECC 384-bit Certificate", false),
        new TestEndpoint("rsa2048.badssl.com", 443, "/", "RSA 2048-bit Certificate", false),
        new TestEndpoint("rsa4096.badssl.com", 443, "/", "RSA 4096-bit Certificate", false),
        new TestEndpoint("rsa8192.badssl.com", 443, "/", "RSA 8192-bit Certificate", false),
        new TestEndpoint("extended-validation.badssl.com", 443, "/", "Extended Validation", false),
        new TestEndpoint("mozilla-modern.badssl.com", 443, "/", "Mozilla Modern Compatibility", false),
        new TestEndpoint("mozilla-intermediate.badssl.com", 443, "/", "Mozilla Intermediate Compatibility", false),
        new TestEndpoint("mozilla-old.badssl.com", 443, "/", "Mozilla Old Compatibility", false),
        new TestEndpoint("tls-v1-0.badssl.com", 1010, "/", "TLS 1.0 Only", false),
        new TestEndpoint("tls-v1-1.badssl.com", 1011, "/", "TLS 1.1 Only", false),
        new TestEndpoint("tls-v1-2.badssl.com", 1012, "/", "TLS 1.2 Only", false),
        new TestEndpoint("rc4.badssl.com", 443, "/", "RC4 Cipher", false),
        new TestEndpoint("rc4-md5.badssl.com", 443, "/", "RC4-MD5 Cipher", false),
        new TestEndpoint("3des.badssl.com", 443, "/", "3DES Cipher", false),
        new TestEndpoint("null.badssl.com", 443, "/", "Null Cipher", false)
    );
    
    private SSLContext sslContext;
    
    static class TestEndpoint {
        String host;
        int port;
        String path;
        String description;
        boolean requiresClientCert;
        
        TestEndpoint(String host, int port, String path, String description, boolean requiresClientCert) {
            this.host = host;
            this.port = port;
            this.path = path;
            this.description = description;
            this.requiresClientCert = requiresClientCert;
        }
    }
    
    public EnhancedBadSSLClient() throws Exception {
        this.sslContext = createSSLContextWithClientCert();
    }
    
    private SSLContext createSSLContextWithClientCert() throws Exception {
        // Load P12 certificate for client authentication
        KeyStore keyStore = null;
        KeyManagerFactory keyManagerFactory = null;
        
        File p12File = new File(P12_FILE);
        if (p12File.exists()) {
            keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(p12File)) {
                keyStore.load(fis, P12_PASSWORD.toCharArray());
            }
            
            keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, P12_PASSWORD.toCharArray());
        }
        
        // Create a trust manager that accepts various certificates for testing
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Not used for client
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // For testing purposes, we'll log certificate info but accept all
                    if (chain != null && chain.length > 0) {
                        System.out.println("Server certificate: " + chain[0].getSubjectX500Principal());
                        try {
                            chain[0].checkValidity();
                            System.out.println("✓ Certificate is valid");
                        } catch (Exception e) {
                            System.out.println("⚠ Certificate issue: " + e.getMessage());
                        }
                    }
                }
            }
        };
        
        SSLContext context = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
        context.init(keyManagers, trustManagers, new SecureRandom());
        return context;
    }
    
    public void testAllBadSSLEndpoints() {
        System.out.println("=== Enhanced BadSSL Endpoint Testing ===");
        System.out.println("Testing " + BADSSL_ENDPOINTS.size() + " different SSL scenarios...\n");
        
        int successCount = 0;
        int expectedFailures = 0;
        int unexpectedResults = 0;
        
        for (TestEndpoint endpoint : BADSSL_ENDPOINTS) {
            System.out.println("🔗 Testing: " + endpoint.host + ":" + endpoint.port + endpoint.path);
            System.out.println("   Description: " + endpoint.description);
            System.out.println("   Client Cert Required: " + (endpoint.requiresClientCert ? "Yes" : "No"));
            
            TestResult result = testEndpoint(endpoint);
            
            switch (result.status) {
                case SUCCESS:
                    System.out.println("   ✅ SUCCESS: " + result.message);
                    successCount++;
                    break;
                case EXPECTED_FAILURE:
                    System.out.println("   ⚠ EXPECTED FAILURE: " + result.message);
                    expectedFailures++;
                    break;
                case UNEXPECTED:
                    System.out.println("   ? UNEXPECTED: " + result.message);
                    unexpectedResults++;
                    break;
                case ERROR:
                    System.out.println("   X ERROR: " + result.message);
                    break;
            }
            System.out.println();
        }
        
        // Summary
        System.out.println("=== Test Summary ===");
        System.out.println("Total Endpoints Tested: " + BADSSL_ENDPOINTS.size());
        System.out.println("OK Successful Connections: " + successCount);
        System.out.println("! Expected Failures: " + expectedFailures);
        System.out.println("? Unexpected Results: " + unexpectedResults);
        System.out.println("\nNote: Many failures are EXPECTED as BadSSL is designed to test various SSL/TLS scenarios.");
    }
    
    private TestResult testEndpoint(TestEndpoint endpoint) {
        try {
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket();
            socket.connect(new InetSocketAddress(endpoint.host, endpoint.port), 10000);
            
            socket.startHandshake();
            
            SSLSession session = socket.getSession();
            String protocol = session.getProtocol();
            String cipher = session.getCipherSuite();
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("GET " + endpoint.path + " HTTP/1.1");
            out.println("Host: " + endpoint.host);
            out.println("User-Agent: EnhancedBadSSLClient/1.0");
            out.println("Connection: close");
            out.println();
            
            String statusLine = in.readLine();
            socket.close();
            
            if (statusLine != null) {
                if (statusLine.contains("200")) {
                    return new TestResult(TestStatus.SUCCESS, 
                        "HTTP 200 OK - Protocol: " + protocol + ", Cipher: " + cipher);
                } else if (statusLine.contains("400") && endpoint.requiresClientCert) {
                    return new TestResult(TestStatus.EXPECTED_FAILURE, 
                        "HTTP 400 - Client certificate required but connection made");
                } else if (statusLine.contains("404")) {
                    return new TestResult(TestStatus.SUCCESS, 
                        "HTTP 404 - Endpoint doesn't exist but SSL connection successful");
                } else {
                    return new TestResult(TestStatus.UNEXPECTED, 
                        statusLine + " - Protocol: " + protocol);
                }
            } else {
                return new TestResult(TestStatus.UNEXPECTED, "No HTTP response received");
            }
            
        } catch (Exception e) {
            String error = e.getMessage();
            
            // Categorize expected vs unexpected errors
            if (error.contains("certificate") || error.contains("trust") || 
                error.contains("verify") || error.contains("chain")) {
                return new TestResult(TestStatus.EXPECTED_FAILURE, 
                    "SSL Certificate Error: " + error);
            } else if (error.contains("timeout") || error.contains("refused")) {
                return new TestResult(TestStatus.EXPECTED_FAILURE, 
                    "Connection Error: " + error);
            } else {
                return new TestResult(TestStatus.ERROR, "Unexpected Error: " + error);
            }
        }
    }
    
    enum TestStatus {
        SUCCESS, EXPECTED_FAILURE, UNEXPECTED, ERROR
    }
    
    static class TestResult {
        TestStatus status;
        String message;
        
        TestResult(TestStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }
    
    public static void main(String[] args) {
        try {
            EnhancedBadSSLClient client = new EnhancedBadSSLClient();
            
            if (args.length > 0 && "full".equals(args[0])) {
                client.testAllBadSSLEndpoints();
            } else {
                // Quick test of main working endpoints
                System.out.println("=== Quick BadSSL Test (Working Endpoints) ===");
                System.out.println("For full test suite, run: java EnhancedBadSSLClient full\n");
                
                TestEndpoint[] quickTests = {
                    new TestEndpoint("client.badssl.com", 443, "/", "Client Certificate Required", true),
                    new TestEndpoint("ecc256.badssl.com", 443, "/", "ECC 256-bit Certificate", false),
                    new TestEndpoint("rsa2048.badssl.com", 443, "/", "RSA 2048-bit Certificate", false),
                    new TestEndpoint("mozilla-modern.badssl.com", 443, "/", "Mozilla Modern", false),
                    new TestEndpoint("extended-validation.badssl.com", 443, "/", "Extended Validation", false)
                };
                
                for (TestEndpoint endpoint : quickTests) {
                    System.out.println("🔗 Testing: " + endpoint.host);
                    System.out.println("   " + endpoint.description);
                    TestResult result = client.testEndpoint(endpoint);
                    System.out.println("   Result: " + result.message);
                    System.out.println();
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to initialize Enhanced BadSSL client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}