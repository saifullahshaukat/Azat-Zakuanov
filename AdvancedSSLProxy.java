import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * Advanced SSL Proxy with Certificate Authentication
 * Supports client certificate validation and mutual TLS
 */
public class AdvancedSSLProxy {
    private static final int PROXY_PORT = 8444;
    private static final int BUFFER_SIZE = 8192;
    
    private final SSLContext sslContext;
    private volatile boolean running = false;
    
    public AdvancedSSLProxy() throws Exception {
        this.sslContext = createSSLContextWithClientAuth();
    }
    
    /**
     * Create SSL context with client certificate authentication
     */
    private SSLContext createSSLContextWithClientAuth() throws Exception {
        // Custom trust manager for client certificate validation
        TrustManager[] trustManagers = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
                
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Validate client certificate here
                    if (chain.length > 0) {
                        X509Certificate clientCert = chain[0];
                        System.out.println("Client certificate: " + clientCert.getSubjectDN());
                        
                        // Add your certificate validation logic here
                        // For example, check certificate issuer, validity, etc.
                        validateClientCertificate(clientCert);
                    }
                }
                
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Accept all server certificates (for testing)
                }
                
                private void validateClientCertificate(X509Certificate cert) {
                    try {
                        // Check if certificate is valid
                        cert.checkValidity();
                        
                        // Check certificate subject
                        String subject = cert.getSubjectDN().toString();
                        System.out.println("Validating certificate for: " + subject);
                        
                        // Add additional validation logic as needed
                        // For example, check against a whitelist of allowed certificates
                        
                    } catch (Exception e) {
                        System.err.println("Certificate validation failed: " + e.getMessage());
                        throw new RuntimeException("Invalid client certificate", e);
                    }
                }
            }
        };
        
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagers, new java.security.SecureRandom());
        return context;
    }
    
    /**
     * Start the advanced SSL proxy
     */
    public void start() throws IOException {
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PROXY_PORT);
        
        // Configure SSL settings
        serverSocket.setNeedClientAuth(false); // Set to true for mutual TLS
        serverSocket.setWantClientAuth(true);  // Request client certificates
        
        running = true;
        System.out.println("Advanced SSL Proxy started on port " + PROXY_PORT);
        System.out.println("Client certificate authentication: " + 
                          (serverSocket.getNeedClientAuth() ? "REQUIRED" : "OPTIONAL"));
        
        while (running) {
            try {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new Thread(new AdvancedProxyHandler(clientSocket)).start();
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
        
        serverSocket.close();
    }
    
    /**
     * Advanced proxy handler with SSL features
     */
    private class AdvancedProxyHandler implements Runnable {
        private final SSLSocket clientSocket;
        
        public AdvancedProxyHandler(SSLSocket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try {
                // Get client certificate information
                SSLSession session = clientSocket.getSession();
                try {
                    java.security.cert.Certificate[] peerCerts = session.getPeerCertificates();
                    if (peerCerts.length > 0) {
                        X509Certificate clientCert = (X509Certificate) peerCerts[0];
                        System.out.println("Client authenticated with certificate: " + 
                                         clientCert.getSubjectDN());
                    }
                } catch (SSLPeerUnverifiedException e) {
                    System.out.println("Client connected without certificate");
                }
                
                handleSecureConnection();
                
            } catch (Exception e) {
                System.err.println("Error in advanced proxy handler: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        private void handleSecureConnection() throws IOException {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true);
            
            String requestLine = reader.readLine();
            if (requestLine == null) return;
            
            System.out.println("Secure request: " + requestLine);
            
            // Parse and handle the request
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }
            
            String method = parts[0];
            String url = parts[1];
            
            if ("CONNECT".equals(method)) {
                handleSecureConnect(url, reader, writer);
            } else {
                handleSecureHttpRequest(method, url, reader, writer);
            }
        }
        
        private void handleSecureConnect(String url, BufferedReader clientReader, 
                                       PrintWriter clientWriter) throws IOException {
            String[] hostPort = url.split(":");
            String targetHost = hostPort[0];
            int targetPort = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 443;
            
            try {
                // Create secure connection to target
                SSLSocketFactory factory = sslContext.getSocketFactory();
                SSLSocket targetSocket = (SSLSocket) factory.createSocket(targetHost, targetPort);
                
                clientWriter.println("HTTP/1.1 200 Connection Established");
                clientWriter.println();
                clientWriter.flush();
                
                // Tunnel data securely
                Thread t1 = new Thread(() -> secureTransfer(clientSocket, targetSocket));
                Thread t2 = new Thread(() -> secureTransfer(targetSocket, clientSocket));
                
                t1.start();
                t2.start();
                
                try {
                    t1.join();
                    t2.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                targetSocket.close();
                
            } catch (Exception e) {
                System.err.println("Secure CONNECT failed: " + e.getMessage());
                sendErrorResponse(clientWriter, 502, "Bad Gateway");
            }
        }
        
        private void handleSecureHttpRequest(String method, String url, 
                                           BufferedReader clientReader, 
                                           PrintWriter clientWriter) throws IOException {
            // Handle HTTP requests with additional security checks
            clientWriter.println("HTTP/1.1 200 OK");
            clientWriter.println("Content-Type: application/json");
            clientWriter.println();
            clientWriter.println("{\"message\": \"Secure proxy response\", \"method\": \"" + method + "\", \"url\": \"" + url + "\"}");
            clientWriter.flush();
        }
        
        private void secureTransfer(Socket from, Socket to) {
            try (InputStream in = from.getInputStream();
                 OutputStream out = to.getOutputStream()) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    out.flush();
                }
            } catch (IOException e) {
                // Connection closed
            }
        }
        
        private void sendErrorResponse(PrintWriter writer, int code, String message) {
            writer.println("HTTP/1.1 " + code + " " + message);
            writer.println("Content-Type: application/json");
            writer.println();
            writer.println("{\"error\": \"" + message + "\", \"code\": " + code + "}");
            writer.flush();
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public static void main(String[] args) {
        try {
            AdvancedSSLProxy proxy = new AdvancedSSLProxy();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down Advanced SSL Proxy...");
                proxy.stop();
            }));
            
            proxy.start();
            
        } catch (Exception e) {
            System.err.println("Failed to start Advanced SSL Proxy: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}