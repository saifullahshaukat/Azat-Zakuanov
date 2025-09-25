import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Scanner;
import javax.net.ssl.*;

/**
 * Enhanced SSL Client to connect to client.badssl.com
 * Handles client certificate authentication properly
 */
public class BadSSLClient {
    private static final String P12_FILE = "badssl.com-client.p12";
    private static final String P12_PASSWORD = "badssl.com";
    private static final String TARGET_HOST = "client.badssl.com";
    private static final int TARGET_PORT = 443;
    
    private SSLContext sslContext;
    
    public BadSSLClient() throws Exception {
        this.sslContext = createSSLContextWithClientCert();
    }
    
    /**
     * Create SSL context with proper client certificate
     */
    private SSLContext createSSLContextWithClientCert() throws Exception {
        // Load P12 certificate for client authentication
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(P12_FILE)) {
            keyStore.load(fis, P12_PASSWORD.toCharArray());
        }
        
        // Initialize KeyManagerFactory
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, P12_PASSWORD.toCharArray());
        
        // Create a trust manager that accepts the badssl.com certificates
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
                    // For badssl.com testing, we'll accept their certificates
                    if (chain != null && chain.length > 0) {
                        System.out.println("Server certificate received: " + chain[0].getSubjectX500Principal());
                        try {
                            chain[0].checkValidity();
                            System.out.println("✓ Server certificate is valid");
                        } catch (Exception e) {
                            System.out.println("⚠ Server certificate validation: " + e.getMessage());
                        }
                    }
                }
            }
        };
        
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
        return context;
    }
    
    /**
     * Connect to client.badssl.com and make HTTP request
     */
    public void connectToClientBadSSL() {
        System.out.println("=== BadSSL Client Certificate Test ===");
        System.out.println("Target: https://" + TARGET_HOST + "/");
        System.out.println("Using P12 certificate: " + P12_FILE);
        
        try {
            // Create SSL socket
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(TARGET_HOST, TARGET_PORT);
            
            // Configure SSL settings
            socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
            
            System.out.println("\nInitiating SSL handshake...");
            socket.startHandshake();
            
            SSLSession session = socket.getSession();
            System.out.println("✓ SSL handshake successful!");
            System.out.println("Protocol: " + session.getProtocol());
            System.out.println("Cipher Suite: " + session.getCipherSuite());
            
            // Print server certificate info
            try {
                java.security.cert.Certificate[] serverCerts = session.getPeerCertificates();
                if (serverCerts.length > 0) {
                    X509Certificate serverCert = (X509Certificate) serverCerts[0];
                    System.out.println("Server Certificate Subject: " + serverCert.getSubjectX500Principal());
                    System.out.println("Server Certificate Issuer: " + serverCert.getIssuerX500Principal());
                }
            } catch (SSLPeerUnverifiedException e) {
                System.out.println("Could not verify server certificate");
            }
            
            // Send HTTP request
            System.out.println("\nSending HTTP request...");
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Send proper HTTP/1.1 request
            out.println("GET / HTTP/1.1");
            out.println("Host: " + TARGET_HOST);
            out.println("User-Agent: BadSSLClient/1.0");
            out.println("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            out.println("Connection: close");
            out.println(); // Empty line to end headers
            
            // Read response
            System.out.println("\nHTTP Response:");
            System.out.println("==================================================");
            String line;
            int lineCount = 0;
            boolean inHeaders = true;
            StringBuilder response = new StringBuilder();
            
            while ((line = in.readLine()) != null && lineCount < 100) {
                System.out.println(line);
                response.append(line).append("\n");
                lineCount++;
                
                if (inHeaders && line.isEmpty()) {
                    inHeaders = false;
                    System.out.println("--- Response Body ---");
                }
            }
            System.out.println("==================================================");
            
            // Check if we got a proper response
            String responseStr = response.toString();
            if (responseStr.contains("HTTP/1.1 200")) {
                System.out.println("✅ SUCCESS: Connected to client.badssl.com successfully!");
            } else if (responseStr.contains("HTTP/1.1 404")) {
                System.out.println("⚠ 404 Error: The requested page was not found");
                System.out.println("💡 Try different endpoints:");
                System.out.println("   - https://client.badssl.com/");
                System.out.println("   - https://client.badssl.com/dashboard");
                System.out.println("   - https://client.badssl.com/json");
            } else {
                System.out.println("⚠ Unexpected response received");
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test different endpoints on client.badssl.com
     */
    public void testDifferentEndpoints() {
        String[] endpoints = {"/", "/dashboard", "/json", "/api", "/test"};
        
        System.out.println("\n=== Testing Different Endpoints ===");
        
        for (String endpoint : endpoints) {
            System.out.println("\nTesting: https://" + TARGET_HOST + endpoint);
            testEndpoint(endpoint);
        }
    }
    
    private void testEndpoint(String endpoint) {
        try {
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(TARGET_HOST, TARGET_PORT);
            
            socket.startHandshake();
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("GET " + endpoint + " HTTP/1.1");
            out.println("Host: " + TARGET_HOST);
            out.println("User-Agent: BadSSLClient/1.0");
            out.println("Connection: close");
            out.println();
            
            String statusLine = in.readLine();
            System.out.println("  Status: " + statusLine);
            
            if (statusLine != null && statusLine.contains("200")) {
                System.out.println("  ✅ Endpoint available");
            } else if (statusLine != null && statusLine.contains("404")) {
                System.out.println("  ❌ Endpoint not found");
            } else {
                System.out.println("  ⚠ Unexpected response: " + statusLine);
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.out.println("  ❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Interactive mode to test custom URLs
     */
    public void interactiveMode() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n=== Interactive Mode ===");
        System.out.println("Enter endpoints to test (or 'quit' to exit):");
        
        while (true) {
            System.out.print("Endpoint (e.g., /api/status): ");
            String input = scanner.nextLine().trim();
            
            if ("quit".equalsIgnoreCase(input)) {
                break;
            }
            
            if (!input.startsWith("/")) {
                input = "/" + input;
            }
            
            testEndpoint(input);
        }
    }
    
    public static void main(String[] args) {
        try {
            BadSSLClient client = new BadSSLClient();
            
            // Test basic connection
            client.connectToClientBadSSL();
            
            // Test different endpoints
            client.testDifferentEndpoints();
            
            // Interactive mode
            if (args.length > 0 && "interactive".equals(args[0])) {
                client.interactiveMode();
            }
            
        } catch (Exception e) {
            System.err.println("Failed to initialize BadSSL client: " + e.getMessage());
            e.printStackTrace();
        }
    }
}