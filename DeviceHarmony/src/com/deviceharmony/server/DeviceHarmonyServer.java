// DeviceHarmonyServer.java - MAIN SERVER APPLICATION
package com.deviceharmony.server;

import com.deviceharmony.database.DatabaseManager;
import com.deviceharmony.web.WebServer;
import com.deviceharmony.agent.AgentDiscovery;
import java.net.InetAddress;
import java.util.Scanner;

public class DeviceHarmonyServer {
    private static WebServer webServer;
    private static AgentDiscovery agentDiscovery;
    private static DatabaseManager dbManager;
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("  DeviceHarmony Server v2.0");
        System.out.println("=================================\n");
        
        try {
            // Initialize database
            System.out.println("✓ Initializing database...");
            dbManager = new DatabaseManager();
            dbManager.initialize();
            
            // Register server as a device
            String serverIp = InetAddress.getLocalHost().getHostAddress();
            String serverName = InetAddress.getLocalHost().getHostName() + " (Server)";
            String serverId = "server-" + serverIp.replace(".", "-");
            dbManager.registerDevice(serverId, serverName, "manual", serverIp, 8080, System.getProperty("user.home"));
            dbManager.setPrimaryDevice(serverId);
            
            // Update storage information for server
            java.io.File root = new java.io.File(System.getProperty("user.home"));
            dbManager.updateDeviceStorage(serverId, root.getTotalSpace(), root.getUsableSpace());
            
            System.out.println("✓ Registered server as primary device");
            
            // Start agent discovery service
            System.out.println("✓ Starting agent discovery...");
            agentDiscovery = new AgentDiscovery(dbManager);
            agentDiscovery.start();
            
            // Start web server
            System.out.println("✓ Starting web server...");
            webServer = new WebServer(8080, dbManager, agentDiscovery);
            webServer.start();
            
            System.out.println("\n========================================");
            System.out.println("✓ DeviceHarmony Server is running!");
            System.out.println("========================================");
            System.out.println("Local Access:   http://localhost:8080");
            System.out.println("Network Access: http://" + serverIp + ":8080");
            System.out.println("\nShare this URL with other devices on your network!");
            System.out.println("\nClient devices can:");
            System.out.println("  1. Access via browser (no installation needed)");
            System.out.println("  2. Run DeviceHarmony Agent for full features");
            System.out.println("  3. Use network shares (Windows/Mac/Linux)");
            System.out.println("\nPress 'q' and Enter to quit\n");
            
            // Wait for quit command
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("q")) {
                    break;
                }
            }
            
            shutdown();
            
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
            shutdown();
        }
    }
    
    private static void shutdown() {
        System.out.println("\nShutting down server...");
        
        if (webServer != null) {
            webServer.stop();
        }
        
        if (agentDiscovery != null) {
            agentDiscovery.stop();
        }
        
        if (dbManager != null) {
            dbManager.close();
        }
        
        System.out.println("✓ Shutdown complete");
        System.exit(0);
    }
}
