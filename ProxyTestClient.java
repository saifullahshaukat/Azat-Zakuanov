import java.io.*;
import java.net.*;

/**
 * Simple HTTP client to test the SSL Proxy
 */
public class ProxyTestClient {
    
    public static void main(String[] args) {
        ProxyTestClient client = new ProxyTestClient();
        
        System.out.println("=== SSL Proxy Test Client ===");
        
        // Wait a moment for proxy to start if running together
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Test HTTP request through proxy
        client.testHttpRequest();
        
        // Test HTTPS CONNECT request through proxy
        client.testHttpsConnect();
        
        // Test error handling
        client.testErrorHandling();
    }
    
    /**
     * Test basic HTTP request through proxy
     */
    public void testHttpRequest() {
        System.out.println("\n--- Testing HTTP Request ---");
        try {
            // Set proxy settings
            System.setProperty("http.proxyHost", "localhost");
            System.setProperty("http.proxyPort", "8444");
            
            URL url = new URL("http://httpbin.org/get");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("HTTP Response Code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                System.out.println("Response received (first 200 chars):");
                String responseStr = response.toString();
                System.out.println(responseStr.substring(0, Math.min(200, responseStr.length())));
                System.out.println("✓ HTTP test passed");
            } else {
                System.out.println("✗ HTTP test failed with code: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("✗ HTTP test failed: " + e.getMessage());
        } finally {
            // Clear proxy settings
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }
    }
    
    /**
     * Test HTTPS CONNECT request through proxy
     */
    public void testHttpsConnect() {
        System.out.println("\n--- Testing HTTPS CONNECT ---");
        try {
            // Set proxy settings for HTTPS
            System.setProperty("https.proxyHost", "localhost");
            System.setProperty("https.proxyPort", "8444");
            
            URL url = new URL("https://httpbin.org/get");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            
            int responseCode = connection.getResponseCode();
            System.out.println("HTTPS Response Code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                
                System.out.println("Response received (first 200 chars):");
                String responseStr = response.toString();
                System.out.println(responseStr.substring(0, Math.min(200, responseStr.length())));
                System.out.println("✓ HTTPS test passed");
            } else {
                System.out.println("✗ HTTPS test failed with code: " + responseCode);
            }
            
        } catch (Exception e) {
            System.out.println("✗ HTTPS test failed: " + e.getMessage());
        } finally {
            // Clear proxy settings
            System.clearProperty("https.proxyHost");
            System.clearProperty("https.proxyPort");
        }
    }
    
    /**
     * Test error handling with invalid requests
     */
    public void testErrorHandling() {
        System.out.println("\n--- Testing Error Handling ---");
        try {
            Socket socket = new Socket("localhost", 8444);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            
            // Send malformed request
            writer.println("INVALID REQUEST");
            
            String response = reader.readLine();
            System.out.println("Error response: " + response);
            
            if (response != null && response.contains("400")) {
                System.out.println("✓ Error handling test passed");
            } else {
                System.out.println("✗ Error handling test failed");
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.out.println("✗ Error handling test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test direct socket connection to proxy
     */
    public void testDirectConnection() {
        System.out.println("\n--- Testing Direct Connection ---");
        try {
            Socket socket = new Socket("localhost", 8444);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            
            // Send HTTP request
            writer.println("GET http://httpbin.org/get HTTP/1.1");
            writer.println("Host: httpbin.org");
            writer.println("Connection: close");
            writer.println();
            
            // Read response
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null && lineCount < 10) {
                System.out.println(line);
                lineCount++;
            }
            
            socket.close();
            System.out.println("✓ Direct connection test completed");
            
        } catch (Exception e) {
            System.out.println("✗ Direct connection test failed: " + e.getMessage());
        }
    }
}