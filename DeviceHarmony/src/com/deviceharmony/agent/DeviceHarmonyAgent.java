// DeviceHarmonyAgent.java - LIGHTWEIGHT CLIENT AGENT
package com.deviceharmony.agent;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;

public class DeviceHarmonyAgent {
    private static final int AGENT_PORT = 9877;
    private static final int DISCOVERY_PORT = 9876;
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private static String agentId;
    private static String agentName;
    private static Server webServer;
    private static boolean running = true;
    
    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("  DeviceHarmony Agent v2.0");
        System.out.println("=================================\n");
        
        try {
            agentId = UUID.randomUUID().toString();
            agentName = InetAddress.getLocalHost().getHostName();
            
            // Start mini web server for file serving
            startFileServer();
            
            // Start broadcasting presence
            startBroadcast();
            
            String localIp = getLocalIpAddress();
            System.out.println("✓ Agent running!");
            System.out.println("✓ Agent ID: " + agentId);
            System.out.println("✓ Device Name: " + agentName);
            System.out.println("✓ IP Address: " + localIp);
            System.out.println("✓ Broadcasting to DeviceHarmony server...");
            System.out.println("\nThis device is now discoverable by the server.");
            System.out.println("Access the server dashboard to see this device.\n");
            System.out.println("Press 'q' and Enter to quit\n");
            
            // Wait for quit
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String input = scanner.nextLine();
                if (input.equalsIgnoreCase("q")) {
                    break;
                }
            }
            
            shutdown();
            
        } catch (Exception e) {
            System.err.println("Error starting agent: " + e.getMessage());
            e.printStackTrace();
            shutdown();
        }
    }
    
    private static void startFileServer() throws Exception {
        webServer = new Server(AGENT_PORT);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        webServer.setHandler(context);
        
        // File operations endpoint
        context.addServlet(new ServletHolder(new AgentFileServlet()), "/files/*");
        
        webServer.start();
    }
    
    private static void startBroadcast() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                MulticastSocket socket = new MulticastSocket();
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                
                String message = String.format("DEVICEHARMONY_AGENT|%s|%s|%s|%d",
                    agentId, agentName, getLocalIpAddress(), AGENT_PORT);
                
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, 
                    group, DISCOVERY_PORT);
                socket.send(packet);
                socket.close();
            } catch (Exception e) {
                // Silent fail for broadcast
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
    
    private static String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "127.0.0.1";
    }
    
    private static void shutdown() {
        System.out.println("\nShutting down agent...");
        running = false;
        
        if (webServer != null) {
            try {
                webServer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("✓ Agent stopped");
        System.exit(0);
    }
}

// AgentFileServlet.java - FILE OPERATIONS FOR AGENT
class AgentFileServlet extends HttpServlet {
    private Gson gson = new Gson();
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String action = req.getPathInfo();
        
        if ("/list".equals(action)) {
            listFiles(req, resp);
        } else if ("/download".equals(action)) {
            downloadFile(req, resp);
        } else if ("/info".equals(action)) {
            getStorageInfo(req, resp);
        } else {
            resp.setStatus(404);
        }
    }
    
    private void listFiles(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getParameter("path");
        if (path == null || path.isEmpty()) {
            path = System.getProperty("user.home");
        }
        
        try {
            File dir = new File(path);
            if (!dir.exists() || !dir.isDirectory()) {
                resp.getWriter().write("[]");
                return;
            }
            
            File[] files = dir.listFiles();
            if (files == null) {
                resp.getWriter().write("[]");
                return;
            }
            
            List<Map<String, Object>> fileList = new ArrayList<>();
            for (File file : files) {
                if (file.isHidden()) continue;
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getName());
                fileInfo.put("path", file.getAbsolutePath());
                fileInfo.put("isDirectory", file.isDirectory());
                fileInfo.put("size", file.length());
                fileInfo.put("lastModified", file.lastModified());
                fileInfo.put("canRead", file.canRead());
                fileList.add(fileInfo);
            }
            
            resp.getWriter().write(gson.toJson(fileList));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private void downloadFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String filePath = req.getParameter("path");
        if (filePath == null || filePath.isEmpty()) {
            resp.setStatus(400);
            resp.getWriter().write("{\"error\": \"No file path provided\"}");
            return;
        }
        
        try {
            File file = new File(filePath);
            if (!file.exists() || file.isDirectory() || !file.canRead()) {
                resp.setStatus(404);
                resp.getWriter().write("{\"error\": \"File not found or not accessible\"}");
                return;
            }
            
            resp.setContentType("application/octet-stream");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            resp.setContentLengthLong(file.length());
            
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private void getStorageInfo(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        
        try {
            File root = new File("/");
            Map<String, Object> info = new HashMap<>();
            info.put("totalStorage", root.getTotalSpace());
            info.put("availableStorage", root.getUsableSpace());
            info.put("usedStorage", root.getTotalSpace() - root.getUsableSpace());
            
            resp.getWriter().write(gson.toJson(info));
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}