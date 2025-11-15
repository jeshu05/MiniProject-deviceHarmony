// AgentDiscovery.java - DISCOVERS CLIENT AGENTS
package com.deviceharmony.agent;

import com.deviceharmony.database.DatabaseManager;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AgentDiscovery {
    private static final int DISCOVERY_PORT = 9876;
    private static final String MULTICAST_GROUP = "230.0.0.0";
    private final DatabaseManager dbManager;
    private MulticastSocket socket;
    private InetAddress group;
    private boolean running = false;
    private ScheduledExecutorService scheduler;
    private Map<String, AgentInfo> discoveredAgents = new ConcurrentHashMap<>();
    
    public AgentDiscovery(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public void start() throws Exception {
        socket = new MulticastSocket(DISCOVERY_PORT);
        group = InetAddress.getByName(MULTICAST_GROUP);
        socket.joinGroup(group);
        running = true;
        
        // Start listener thread for agent broadcasts
        new Thread(this::listenForAgents).start();
        
        // Check agent health periodically
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkAgentHealth, 10, 10, TimeUnit.SECONDS);
    }
    
    private void listenForAgents() {
        byte[] buffer = new byte[1024];
        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("DEVICEHARMONY_AGENT|")) {
                    String[] parts = message.split("\\|");
                    if (parts.length >= 5) {
                        String agentId = parts[1];
                        String agentName = parts[2];
                        String agentIp = parts[3];
                        int agentPort = Integer.parseInt(parts[4]);
                        
                        AgentInfo info = new AgentInfo(agentId, agentName, agentIp, agentPort);
                        discoveredAgents.put(agentId, info);
                        
                        // Register in database
                        dbManager.registerDevice(agentId, agentName, "agent", agentIp, agentPort, null);
                    }
                }
            } catch (Exception e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void checkAgentHealth() {
        try {
            long now = System.currentTimeMillis();
            discoveredAgents.entrySet().removeIf(entry -> {
                if (now - entry.getValue().lastSeen > 15000) { // 15 seconds timeout
                    try {
                        dbManager.updateDeviceStatus(entry.getKey(), "offline");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return true;
                }
                return false;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, AgentInfo> getDiscoveredAgents() {
        return new HashMap<>(discoveredAgents);
    }
    
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
        }
        try {
            if (socket != null) {
                socket.leaveGroup(group);
                socket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static class AgentInfo {
        public final String agentId;
        public final String agentName;
        public final String ipAddress;
        public final int port;
        public long lastSeen;
        
        public AgentInfo(String agentId, String agentName, String ipAddress, int port) {
            this.agentId = agentId;
            this.agentName = agentName;
            this.ipAddress = ipAddress;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
        }
    }
}
	