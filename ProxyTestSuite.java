import java.io.*;

/**
 * Comprehensive test suite for        try {
            java.net.Socket socket = new java.net.Socket("localhost", 8444);
            socket.close();
            System.out.println("✅ Basic connection successful");
        } catch (Exception e) {
            System.out.println("❌ Basic connection failed: " + e.getMessage());
        }oxy implementations
 */
public class ProxyTestSuite {
    
    public static void main(String[] args) {
        ProxyTestSuite testSuite = new ProxyTestSuite();
        
        System.out.println("=== SSL Proxy Test Suite ===");
        System.out.println("Starting comprehensive proxy tests...\n");
        
        // Run all tests
        testSuite.runAllTests();
    }
    
    public void runAllTests() {
        System.out.println("🔧 Starting SSL Proxy server in background...");
        
        // Start proxy server in background thread
        Thread proxyThread = new Thread(() -> {
            try {
                SSLProxy proxy = new SSLProxy();
                proxy.start();
            } catch (Exception e) {
                System.err.println("Failed to start proxy: " + e.getMessage());
            }
        });
        proxyThread.setDaemon(true);
        proxyThread.start();
        
        // Wait for proxy to start
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Run test cases
        testBasicConnection();
        testHttpTunneling();
        testErrorHandling();
        testPerformance();
        testConcurrentConnections();
        
        System.out.println("\n=== Test Suite Complete ===");
    }
    
    /**
     * Test basic proxy connection
     */
    public void testBasicConnection() {
        System.out.println("\n🧪 Test 1: Basic Connection");
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 8444);
            socket.close();
            System.out.println("✅ Basic connection test PASSED");
        } catch (Exception e) {
            System.out.println("❌ Basic connection test FAILED: " + e.getMessage());
        }
    }
    
    /**
     * Test HTTP tunneling through proxy
     */
    public void testHttpTunneling() {
        System.out.println("\n🧪 Test 2: HTTP Tunneling");
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 8444);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            
            // Send CONNECT request
            writer.println("CONNECT httpbin.org:80 HTTP/1.1");
            writer.println("Host: httpbin.org:80");
            writer.println();
            
            String response = reader.readLine();
            socket.close();
            
            if (response != null && response.contains("200")) {
                System.out.println("✅ HTTP tunneling test PASSED");
            } else {
                System.out.println("❌ HTTP tunneling test FAILED: " + response);
            }
        } catch (Exception e) {
            System.out.println("❌ HTTP tunneling test FAILED: " + e.getMessage());
        }
    }
    
    /**
     * Test error handling
     */
    public void testErrorHandling() {
        System.out.println("\n🧪 Test 3: Error Handling");
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 8444);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
            
            // Send invalid request
            writer.println("INVALID REQUEST LINE");
            writer.println();
            
            String response = reader.readLine();
            socket.close();
            
            if (response != null && response.contains("400")) {
                System.out.println("✅ Error handling test PASSED");
            } else {
                System.out.println("❌ Error handling test FAILED: " + response);
            }
        } catch (Exception e) {
            System.out.println("❌ Error handling test FAILED: " + e.getMessage());
        }
    }
    
    /**
     * Test proxy performance with timing
     */
    public void testPerformance() {
        System.out.println("\n🧪 Test 4: Performance Testing");
        int requestCount = 10;
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < requestCount; i++) {
            try {
                java.net.Socket socket = new java.net.Socket("localhost", 8444);
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
                
                writer.println("GET http://httpbin.org/get HTTP/1.1");
                writer.println("Host: httpbin.org");
                writer.println("Connection: close");
                writer.println();
                
                String response = reader.readLine();
                socket.close();
                
                if (response != null) {
                    successCount++;
                }
            } catch (Exception e) {
                // Count failures
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Performance Results:");
        System.out.println("  - Total requests: " + requestCount);
        System.out.println("  - Successful: " + successCount);
        System.out.println("  - Failed: " + (requestCount - successCount));
        System.out.println("  - Total time: " + duration + "ms");
        System.out.println("  - Avg time per request: " + (duration / requestCount) + "ms");
        
        if (successCount >= requestCount * 0.8) { // 80% success rate
            System.out.println("✅ Performance test PASSED");
        } else {
            System.out.println("❌ Performance test FAILED");
        }
    }
    
    /**
     * Test concurrent connections
     */
    public void testConcurrentConnections() {
        System.out.println("\n🧪 Test 5: Concurrent Connections");
        int threadCount = 5;
        Thread[] threads = new Thread[threadCount];
        final int[] successCount = {0};
        final Object lock = new Object();
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                try {
                    java.net.Socket socket = new java.net.Socket("localhost", 8444);
                    PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                    
                    writer.println("CONNECT httpbin.org:443 HTTP/1.1");
                    writer.println("Host: httpbin.org:443");
                    writer.println();
                    
                    String response = reader.readLine();
                    socket.close();
                    
                    if (response != null && response.contains("200")) {
                        synchronized (lock) {
                            successCount[0]++;
                        }
                    }
                    
                    System.out.println("  Thread " + threadId + " completed");
                    
                } catch (Exception e) {
                    System.out.println("  Thread " + threadId + " failed: " + e.getMessage());
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Concurrent connection results:");
        System.out.println("  - Total threads: " + threadCount);
        System.out.println("  - Successful: " + successCount[0]);
        
        if (successCount[0] >= threadCount * 0.8) { // 80% success rate
            System.out.println("✅ Concurrent connections test PASSED");
        } else {
            System.out.println("❌ Concurrent connections test FAILED");
        }
    }
    
    /**
     * Generate test report
     */
    public void generateTestReport() {
        System.out.println("\n📊 Generating Test Report...");
        
        try (PrintWriter reportWriter = new PrintWriter(new FileWriter("proxy-test-report.txt"))) {
            reportWriter.println("SSL Proxy Test Report");
            reportWriter.println("Generated: " + new java.util.Date());
            reportWriter.println("=".repeat(50));
            reportWriter.println();
            
            reportWriter.println("Test Environment:");
            reportWriter.println("- Java Version: " + System.getProperty("java.version"));
            reportWriter.println("- OS: " + System.getProperty("os.name"));
            reportWriter.println("- Proxy Port: 8444");
            reportWriter.println();
            
            reportWriter.println("Tests Executed:");
            reportWriter.println("1. Basic Connection Test");
            reportWriter.println("2. HTTP Tunneling Test");
            reportWriter.println("3. Error Handling Test");
            reportWriter.println("4. Performance Test");
            reportWriter.println("5. Concurrent Connections Test");
            reportWriter.println();
            
            reportWriter.println("For detailed results, check console output.");
            
            System.out.println("✅ Test report generated: proxy-test-report.txt");
            
        } catch (IOException e) {
            System.out.println("❌ Failed to generate test report: " + e.getMessage());
        }
    }
}