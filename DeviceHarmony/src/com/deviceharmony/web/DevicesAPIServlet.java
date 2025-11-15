// DevicesAPIServlet.java
// Location: src/com/deviceharmony/web/DevicesAPIServlet.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import com.deviceharmony.agent.AgentDiscovery;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import com.google.gson.*;

public class DevicesAPIServlet extends HttpServlet {
    private DatabaseManager dbManager;
    private AgentDiscovery agentDiscovery;
    private Gson gson = new Gson();
    
    public DevicesAPIServlet(DatabaseManager dbManager, AgentDiscovery agentDiscovery) {
        this.dbManager = dbManager;
        this.agentDiscovery = agentDiscovery;
    }
    
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try {
            ResultSet rs = dbManager.getAllDevices();
            List<Map<String, Object>> devices = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> device = new HashMap<>();
                device.put("device_id", rs.getString("device_id"));
                device.put("device_name", rs.getString("device_name"));
                device.put("device_type", rs.getString("device_type"));
                device.put("ip_address", rs.getString("ip_address"));
                device.put("port", rs.getObject("port"));
                device.put("share_path", rs.getString("share_path"));
                device.put("is_primary", rs.getBoolean("is_primary"));
                device.put("status", rs.getString("status"));
                device.put("total_storage", rs.getObject("total_storage"));
                device.put("available_storage", rs.getObject("available_storage"));
                devices.add(device);
            }
            resp.getWriter().write(gson.toJson(devices));
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String deviceId = req.getParameter("id");
        
        try {
            dbManager.deleteDevice(deviceId);
            resp.getWriter().write("{\"success\": true}");
        } catch (SQLException e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}