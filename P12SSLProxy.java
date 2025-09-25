import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * Enhanced SSL Proxy with P12 Certificate Support
 * Supports both server and client P12 certificates
 */
public class P12SSLProxy {
    private static final int PROXY_PORT = 8444;
    private static final int BUFFER_SIZE = 8192;
    private static final String P12_FILE = "badssl.com-client.p12";
    private static final String P12_PASSWORD = "badssl.com";
    
    private final SSLContext sslContext;
    private volatile boolean running = false;
    
    public P12SSLProxy() throws Exception {
        this.sslContext = createSSLContextWithP12Support();
    }
    
    /**
     * Create SSL context with P12 certificate support
     */
    private SSLContext createSSLContextWithP12Support() throws Exception {
        // Load P12 certificate for client authentication
        KeyStore keyStore = null;
        KeyManagerFactory keyManagerFactory = null;
        
        // Try to load P12 certificate if available
        File p12File = new File(P12_FILE);
        if (p12File.exists()) {
            System.out.println("Loading P12 certificate: " + P12_FILE);
            keyStore = KeyStore.getInstance("PKCS12");
            
            try (FileInputStream fis = new FileInputStream(p12File)) {
                keyStore.load(fis, P12_PASSWORD.toCharArray());
                System.out.println("P12 certificate loaded successfully");
                
                // List certificates in keystore
                java.util.Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    System.out.println("Certificate alias: " + alias);
                    if (keyStore.isCertificateEntry(alias)) {
                        X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                        System.out.println("  Subject: " + cert.getSubjectX500Principal());
                    }
                }
                
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, P12_PASSWORD.toCharArray());
            }
        } else {
            System.out.println("P12 file not found: " + P12_FILE + " - running without client certificate");
        }
        
        // Custom trust manager that accepts all certificates
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    if (chain.length > 0) {
                        X509Certificate clientCert = chain[0];
                        System.out.println("Client certificate received: " + clientCert.getSubjectX500Principal());
                        validateClientCertificate(clientCert);
                    }
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    if (chain.length > 0) {
                        System.out.println("Server certificate accepted: " + chain[0].getSubjectX500Principal());
                    }
                }
                
                private void validateClientCertificate(X509Certificate cert) {
                    try {
                        cert.checkValidity();
                        String subject = cert.getSubjectX500Principal().toString();
                        System.out.println("Client certificate validated for: " + subject);
                    } catch (Exception e) {
                        System.err.println("Certificate validation failed: " + e.getMessage());
                    }
                }
            }
        };
        
        SSLContext context = SSLContext.getInstance("TLS");
        KeyManager[] keyManagers = keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null;
        context.init(keyManagers, trustManagers, new SecureRandom());
        return context;
    }
    
    /**
     * Start the P12 SSL proxy
     */
    public void start() throws IOException {
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PROXY_PORT);
        
        // Configure SSL settings
        serverSocket.setWantClientAuth(true);  // Request client certificates
        
        running = true;
        System.out.println("P12 SSL Proxy started on port " + PROXY_PORT);
        System.out.println("Client certificate authentication: OPTIONAL");
        
        while (running) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new Thread(new P12ProxyHandler(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
        
        serverSocket.close();
    }
    
    /**
     * Stop the proxy
     */
    public void stop() {
        running = false;
    }
    
    /**
     * P12 proxy handler
     */
    private class P12ProxyHandler implements Runnable {
        private final SSLSocket clientSocket;
        
        public P12ProxyHandler(SSLSocket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try {
                // Get client certificate information
                SSLSession session = clientSocket.getSession();
                System.out.println("\n=== New SSL Connection ===");
                System.out.println("Protocol: " + session.getProtocol());
                System.out.println("Cipher Suite: " + session.getCipherSuite());
                
                try {
                    java.security.cert.Certificate[] peerCerts = session.getPeerCertificates();
                    if (peerCerts.length > 0) {
                        X509Certificate clientCert = (X509Certificate) peerCerts[0];
                        System.out.println("Client authenticated with certificate: " + 
                                         clientCert.getSubjectX500Principal());
                    }
                } catch (SSLPeerUnverifiedException e) {
                    System.out.println("Client connected without certificate");
                }
                
                handleConnection();
                
            } catch (Exception e) {
                System.err.println("Error in P12 proxy handler: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        private void handleConnection() throws IOException {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true);
            
            // Read the first line of the request
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            
            System.out.println("Request: " + requestLine);
            
            if (requestLine.startsWith("CONNECT")) {
                // Handle HTTPS CONNECT request
                handleConnectRequest(requestLine, writer);
            } else {
                // Handle regular HTTP request
                handleHttpRequest(requestLine, reader, writer);
            }
        }
        
        private void handleConnectRequest(String requestLine, PrintWriter writer) throws IOException {
            // Extract target host and port from CONNECT request
            String[] parts = requestLine.split(" ");
            if (parts.length >= 2) {
                String hostPort = parts[1];
                System.out.println("CONNECT request to: " + hostPort);
                
                // Send 200 Connection established
                writer.println("HTTP/1.1 200 Connection established");
                writer.println();
                writer.flush();
                
                // For demo purposes, we'll just acknowledge the connection
                System.out.println("CONNECT tunnel established for: " + hostPort);
            }
        }
        
        private void handleHttpRequest(String requestLine, BufferedReader reader, PrintWriter writer) 
                throws IOException {
            // Read headers
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                System.out.println("Header: " + line);
            }
            
            // Send a simple response
            writer.println("HTTP/1.1 200 OK");
            writer.println("Content-Type: text/html");
            writer.println("Content-Length: " + getResponseBody().length());
            writer.println();
            writer.println(getResponseBody());
            writer.flush();
            
            System.out.println("Response sent");
        }
        
        private String getResponseBody() {
            return "<html><body>" +
                   "<h1>P12 SSL Proxy Response</h1>" +
                   "<p>Connection successful with P12 certificate support!</p>" +
                   "<p>Timestamp: " + new java.util.Date() + "</p>" +
                   "</body></html>";
        }
    }
    
    public static void main(String[] args) {
        try {
            P12SSLProxy proxy = new P12SSLProxy();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down P12 SSL Proxy...");
                proxy.stop();
            }));
            
            proxy.start();
            
        } catch (Exception e) {
            System.err.println("Failed to start P12 SSL Proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }
}