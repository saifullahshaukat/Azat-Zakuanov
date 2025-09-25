import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple HTTP server to serve the SSL Proxy Dashboard
 */
public class DashboardServer {
    private static final int DASHBOARD_PORT = 8080;
    private static final String DOCUMENT_ROOT = ".";
    
    private volatile boolean running = false;
    private ExecutorService threadPool;
    
    public DashboardServer() {
        this.threadPool = Executors.newFixedThreadPool(10);
    }
    
    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(DASHBOARD_PORT);
        running = true;
        
        log("Dashboard Server started on http://localhost:" + DASHBOARD_PORT);
        log("Open http://localhost:" + DASHBOARD_PORT + " in your browser");
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleRequest(clientSocket));
            } catch (IOException e) {
                if (running) {
                    log("Error accepting connection: " + e.getMessage());
                }
            }
        }
        
        serverSocket.close();
        threadPool.shutdown();
    }
    
    private void handleRequest(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
             BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {
            
            String input = in.readLine();
            if (input == null) return;
            
            StringTokenizer parse = new StringTokenizer(input);
            String method = parse.nextToken().toUpperCase();
            String fileRequested = parse.nextToken().toLowerCase();
            
            if (fileRequested.equals("/")) {
                fileRequested = "/index.html";
            }
            
            File file = new File(DOCUMENT_ROOT, fileRequested);
            int fileLength = (int) file.length();
            String content = getContentType(fileRequested);
            
            if (method.equals("GET")) {
                if (file.exists()) {
                    byte[] fileData = readFileData(file, fileLength);
                    
                    // Send HTTP Headers
                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: SSL-Proxy-Dashboard/1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + content);
                    out.println("Content-length: " + fileLength);
                    out.println("Access-Control-Allow-Origin: *");
                    out.println("Cache-Control: no-cache");
                    out.println();
                    out.flush();
                    
                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                    
                    log("GET " + fileRequested + " - 200 OK");
                } else {
                    // File not found
                    String errorMessage = "<html><body><h1>404 File Not Found</h1><p>The file " + fileRequested + " was not found.</p></body></html>";
                    
                    out.println("HTTP/1.1 404 File Not Found");
                    out.println("Server: SSL-Proxy-Dashboard/1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: text/html");
                    out.println("Content-length: " + errorMessage.length());
                    out.println();
                    out.flush();
                    
                    out.println(errorMessage);
                    out.flush();
                    
                    log("GET " + fileRequested + " - 404 Not Found");
                }
            }
            
        } catch (IOException e) {
            log("Error handling request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log("Error closing socket: " + e.getMessage());
            }
        }
    }
    
    private byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];
        
        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null) {
                fileIn.close();
            }
        }
        
        return fileData;
    }
    
    private String getContentType(String fileRequested) {
        if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html")) {
            return "text/html";
        } else if (fileRequested.endsWith(".css")) {
            return "text/css";
        } else if (fileRequested.endsWith(".js")) {
            return "application/javascript";
        } else if (fileRequested.endsWith(".png")) {
            return "image/png";
        } else if (fileRequested.endsWith(".jpg") || fileRequested.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileRequested.endsWith(".gif")) {
            return "image/gif";
        } else if (fileRequested.endsWith(".ico")) {
            return "image/x-icon";
        } else {
            return "application/octet-stream";
        }
    }
    
    public void stop() {
        running = false;
        log("Dashboard Server stopping...");
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        System.out.println("[DASHBOARD " + timestamp + "] " + message);
    }
    
    public static void main(String[] args) {
        try {
            DashboardServer server = new DashboardServer();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down Dashboard Server...");
                server.stop();
            }));
            
            server.start();
            
        } catch (Exception e) {
            System.err.println("Failed to start Dashboard Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}