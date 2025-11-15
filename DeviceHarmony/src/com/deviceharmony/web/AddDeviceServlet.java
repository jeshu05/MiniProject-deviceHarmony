// AddDeviceServlet.java
// Location: src/com/deviceharmony/web/AddDeviceServlet.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import com.google.gson.*;

public class AddDeviceServlet extends HttpServlet {
    private DatabaseManager dbManager;
    private Gson gson = new Gson();
    
    public AddDeviceServlet(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        
        BufferedReader reader = req.getReader();
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        
        String deviceName = json.get("deviceName").getAsString();
        String deviceType = json.get("deviceType").getAsString();
        String sharePath = json.has("sharePath") && !json.get("sharePath").isJsonNull() 
            ? json.get("sharePath").getAsString() : null;
        String ipAddress = json.has("ipAddress") && !json.get("ipAddress").isJsonNull()
            ? json.get("ipAddress").getAsString() : null;
        
        try {
            String deviceId = UUID.randomUUID().toString();
            dbManager.registerDevice(deviceId, deviceName, deviceType, ipAddress, null, sharePath);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("deviceId", deviceId);
            resp.getWriter().write(gson.toJson(response));
        } catch (Exception e) {
            resp.setStatus(500);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            resp.getWriter().write(gson.toJson(response));
        }
    }
}
