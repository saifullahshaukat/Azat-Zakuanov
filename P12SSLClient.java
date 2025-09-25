import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * SSL Client with P12 Certificate Authentication
 * Uses the badssl.com-client.p12 certificate for client authentication
 */
public class P12SSLClient {
    private static final String P12_FILE = "badssl.com-client.p12";
    private static final String P12_PASSWORD = "badssl.com";
    private static final String TARGET_HOST = "client.badssl.com";
    private static final int TARGET_PORT = 443;
    
    public static void main(String[] args) {
        P12SSLClient client = new P12SSLClient();
        
        try {
            System.out.println("=== P12 SSL Client Test ===");
            client.testSSLConnectionWithP12Certificate();
        } catch (Exception e) {
            System.err.println("SSL connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Test SSL connection using P12 client certificate
     */
    public void testSSLConnectionWithP12Certificate() throws Exception {
        // Load the P12 certificate
        SSLContext sslContext = createSSLContextWithP12Certificate();
        
        // Create SSL socket factory
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();
        
        // Connect to the target server
        System.out.println("Connecting to " + TARGET_HOST + ":" + TARGET_PORT);
        SSLSocket socket = (SSLSocket) socketFactory.createSocket(TARGET_HOST, TARGET_PORT);
        
        // Enable client authentication
        socket.setUseClientMode(true);
        
        try {
            // Perform SSL handshake
            System.out.println("Performing SSL handshake...");
            socket.startHandshake();
            
            // Get session information
            SSLSession session = socket.getSession();
            System.out.println("SSL handshake successful!");
            System.out.println("Protocol: " + session.getProtocol());
            System.out.println("Cipher Suite: " + session.getCipherSuite());
            
            // Get server certificate information
            java.security.cert.Certificate[] serverCerts = session.getPeerCertificates();
            if (serverCerts.length > 0) {
                X509Certificate serverCert = (X509Certificate) serverCerts[0];
                System.out.println("Server Certificate Subject: " + serverCert.getSubjectX500Principal());
                System.out.println("Server Certificate Issuer: " + serverCert.getIssuerX500Principal());
            }
            
            // Send HTTP request
            sendHttpRequest(socket);
            
        } finally {
            socket.close();
        }
    }
    
    /**
     * Create SSL context using P12 certificate for client authentication
     */
    private SSLContext createSSLContextWithP12Certificate() throws Exception {
        // Load the P12 keystore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        FileInputStream p12File = new FileInputStream(P12_FILE);
        
        try {
            keyStore.load(p12File, P12_PASSWORD.toCharArray());
            System.out.println("P12 certificate loaded successfully");
            
            // List aliases in the keystore
            System.out.println("Keystore aliases:");
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                System.out.println("  - " + alias);
                
                if (keyStore.isCertificateEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    System.out.println("    Certificate Subject: " + cert.getSubjectX500Principal());
                } else if (keyStore.isKeyEntry(alias)) {
                    System.out.println("    Private key entry");
                }
            }
            
        } finally {
            p12File.close();
        }
        
        // Initialize KeyManagerFactory with the keystore
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, P12_PASSWORD.toCharArray());
        
        // Create a trust manager that accepts all certificates (for testing)
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Accept all client certificates
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Accept all server certificates (for testing)
                    if (chain.length > 0) {
                        System.out.println("Server certificate accepted: " + chain[0].getSubjectX500Principal());
                    }
                }
            }
        };
        
        // Create and initialize SSL context
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers, new SecureRandom());
        
        return sslContext;
    }
    
    /**
     * Send HTTP request and read response
     */
    private void sendHttpRequest(SSLSocket socket) throws IOException {
        // Send HTTP GET request
        PrintWriter out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream()), true);
        BufferedReader in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()));
        
        System.out.println("Sending HTTP request...");
        out.println("GET / HTTP/1.1");
        out.println("Host: " + TARGET_HOST);
        out.println("User-Agent: P12SSLClient/1.0");
        out.println("Connection: close");
        out.println();
        
        // Read response
        System.out.println("\nHTTP Response:");
        System.out.println("=".repeat(50));
        
        String line;
        int lineCount = 0;
        while ((line = in.readLine()) != null && lineCount < 20) {
            System.out.println(line);
            lineCount++;
        }
        
        if (lineCount == 20) {
            System.out.println("... (response truncated)");
        }
        
        System.out.println("=".repeat(50));
        
        in.close();
        out.close();
    }
    
    /**
     * Test method to verify P12 certificate can be loaded
     */
    public static void testP12Loading() {
        System.out.println("Testing P12 certificate loading...");
        
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            FileInputStream p12File = new FileInputStream(P12_FILE);
            
            keyStore.load(p12File, P12_PASSWORD.toCharArray());
            p12File.close();
            
            System.out.println("✓ P12 certificate loaded successfully");
            
            // List certificate details
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isCertificateEntry(alias)) {
                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                    System.out.println("Certificate found:");
                    System.out.println("  Alias: " + alias);
                    System.out.println("  Subject: " + cert.getSubjectX500Principal());
                    System.out.println("  Issuer: " + cert.getIssuerX500Principal());
                    System.out.println("  Valid from: " + cert.getNotBefore());
                    System.out.println("  Valid until: " + cert.getNotAfter());
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ Failed to load P12 certificate: " + e.getMessage());
            e.printStackTrace();
        }
    }
}