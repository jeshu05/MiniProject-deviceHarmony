// PrimaryAPIServlet.java
// Location: src/com/deviceharmony/web/PrimaryAPIServlet.java
package com.deviceharmony.web;

import com.deviceharmony.database.DatabaseManager;
import javax.servlet.http.*;
import java.io.*;
import com.google.gson.*;

public class PrimaryAPIServlet extends HttpServlet {
    private DatabaseManager dbManager;
    private Gson gson = new Gson();
    
    public PrimaryAPIServlet(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        
        BufferedReader reader = req.getReader();
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        String deviceId = json.get("deviceId").getAsString();
        
        try {
            dbManager.setPrimaryDevice(deviceId);
            resp.getWriter().write("{\"success\": true}");
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}