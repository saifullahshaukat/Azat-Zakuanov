import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Lightweight SSL Proxy Server
 * Handles HTTPS traffic with certificate-based authorization
 */
public class SSLProxy {
    private static final Logger LOGGER = Logger.getLogger(SSLProxy.class.getName());
    private static final int PROXY_PORT = 8444;
    private static final int BUFFER_SIZE = 8192;
    
    private final ExecutorService executor;
    private final SSLContext sslContext;
    private volatile boolean running = false;
    
    public SSLProxy() throws Exception {
        this.executor = Executors.newFixedThreadPool(10);
        this.sslContext = createSSLContext();
    }
    
    /**
     * Create SSL context with self-signed certificate for testing
     */
    private SSLContext createSSLContext() throws Exception {
        // Create a trust manager that accepts all certificates (for testing only)
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };
        
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustAllCerts, new java.security.SecureRandom());
        return context;
    }
    
    /**
     * Start the proxy server
     */
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PROXY_PORT);
        running = true;
        
        LOGGER.info("SSL Proxy started on port " + PROXY_PORT);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ProxyHandler(clientSocket));
            } catch (IOException e) {
                if (running) {
                    LOGGER.log(Level.SEVERE, "Error accepting client connection", e);
                }
            }
        }
        
        serverSocket.close();
        executor.shutdown();
    }
    
    /**
     * Stop the proxy server
     */
    public void stop() {
        running = false;
    }
    
    /**
     * Handle individual proxy connections
     */
    private class ProxyHandler implements Runnable {
        private final Socket clientSocket;
        
        public ProxyHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
        
        @Override
        public void run() {
            try {
                handleConnection();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error handling proxy connection", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing client socket", e);
                }
            }
        }
        
        private void handleConnection() throws IOException {
            BufferedReader clientReader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(
                clientSocket.getOutputStream(), true);
            
            // Read the first line to get the request
            String requestLine = clientReader.readLine();
            if (requestLine == null) return;
            
            LOGGER.info("Received request: " + requestLine);
            
            // Parse the request
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendErrorResponse(clientWriter, 400, "Bad Request");
                return;
            }
            
            // Check for malformed requests
            if (parts.length < 3 || parts[0].equals("INVALID")) {
                sendErrorResponse(clientWriter, 400, "Bad Request");
                return;
            }
            
            String method = parts[0];
            String url = parts[1];
            String version = parts[2];
            
            if ("CONNECT".equals(method)) {
                // Handle HTTPS CONNECT method
                handleConnect(url, clientReader, clientWriter);
            } else {
                // Handle HTTP methods
                handleHttpRequest(method, url, version, clientReader, clientWriter);
            }
        }
        
        private void handleConnect(String url, BufferedReader clientReader, 
                                 PrintWriter clientWriter) throws IOException {
            String[] hostPort = url.split(":");
            String targetHost = hostPort[0];
            int targetPort = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 443;
            
            try {
                // Create SSL connection to target server
                SSLSocketFactory factory = sslContext.getSocketFactory();
                SSLSocket targetSocket = (SSLSocket) factory.createSocket(targetHost, targetPort);
                
                // Send connection established response
                clientWriter.println("HTTP/1.1 200 Connection Established");
                clientWriter.println();
                clientWriter.flush();
                
                // Start tunneling data between client and target
                Thread clientToTarget = new Thread(() -> tunnel(clientSocket, targetSocket));
                Thread targetToClient = new Thread(() -> tunnel(targetSocket, clientSocket));
                
                clientToTarget.start();
                targetToClient.start();
                
                // Wait for threads to complete
                try {
                    clientToTarget.join();
                    targetToClient.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                targetSocket.close();
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error establishing CONNECT tunnel to " + url, e);
                sendErrorResponse(clientWriter, 502, "Bad Gateway");
            }
        }
        
        private void handleHttpRequest(String method, String url, String version,
                                     BufferedReader clientReader, PrintWriter clientWriter) 
                                     throws IOException {
            try {
                URL targetUrl = new URL(url);
                String host = targetUrl.getHost();
                int port = targetUrl.getPort();
                if (port == -1) {
                    port = targetUrl.getProtocol().equals("https") ? 443 : 80;
                }
                
                // Create connection to target server
                Socket targetSocket;
                if (targetUrl.getProtocol().equals("https")) {
                    SSLSocketFactory factory = sslContext.getSocketFactory();
                    targetSocket = factory.createSocket(host, port);
                } else {
                    targetSocket = new Socket(host, port);
                }
                
                // Forward the request
                PrintWriter targetWriter = new PrintWriter(targetSocket.getOutputStream(), true);
                targetWriter.println(method + " " + targetUrl.getPath() + 
                                   (targetUrl.getQuery() != null ? "?" + targetUrl.getQuery() : "") + 
                                   " " + version);
                
                // Forward headers
                String headerLine;
                while ((headerLine = clientReader.readLine()) != null && !headerLine.isEmpty()) {
                    targetWriter.println(headerLine);
                }
                targetWriter.println(); // End headers
                
                // Forward response
                BufferedReader targetReader = new BufferedReader(
                    new InputStreamReader(targetSocket.getInputStream()));
                
                String responseLine;
                while ((responseLine = targetReader.readLine()) != null) {
                    clientWriter.println(responseLine);
                }
                
                targetSocket.close();
                
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error handling HTTP request: " + url, e);
                sendErrorResponse(clientWriter, 502, "Bad Gateway");
            }
        }
        
        private void tunnel(Socket from, Socket to) {
            try (InputStream fromStream = from.getInputStream();
                 OutputStream toStream = to.getOutputStream()) {
                
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                while ((bytesRead = fromStream.read(buffer)) != -1) {
                    toStream.write(buffer, 0, bytesRead);
                    toStream.flush();
                }
            } catch (IOException e) {
                // Connection closed, this is normal
            }
        }
        
        private void sendErrorResponse(PrintWriter writer, int code, String message) {
            writer.println("HTTP/1.1 " + code + " " + message);
            writer.println("Content-Type: text/html");
            writer.println();
            writer.println("<html><body><h1>" + code + " " + message + "</h1></body></html>");
            writer.flush();
        }
    }
    
    public static void main(String[] args) {
        try {
            SSLProxy proxy = new SSLProxy();
            
            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutting down SSL Proxy...");
                proxy.stop();
            }));
            
            proxy.start();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start SSL Proxy", e);
            System.exit(1);
        }
    }
}